/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.data.bigtable.repository.query;

import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Filters.ChainFilter;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntity;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentProperty;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * {@link RepositoryQuery} implementation executing derived queries using {@link PartTree}.
 * Supports prefix scans, range scans, row key lookups, and property filter queries.
 *
 * @param <T> the domain type
 */
public class BigtablePartTreeQuery<T> implements RepositoryQuery {

  private final BigtableQueryMethod queryMethod;
  private final BigtableOperations bigtableOperations;
  private final BigtableMappingContext mappingContext;
  private final Class<T> domainType;
  private final PartTree tree;

  @SuppressWarnings("unchecked")
  public BigtablePartTreeQuery(
      BigtableQueryMethod queryMethod,
      BigtableOperations bigtableOperations,
      BigtableMappingContext mappingContext) {
    Assert.notNull(queryMethod, "QueryMethod must not be null");
    Assert.notNull(bigtableOperations, "BigtableOperations must not be null");
    Assert.notNull(mappingContext, "BigtableMappingContext must not be null");

    this.queryMethod = queryMethod;
    this.bigtableOperations = bigtableOperations;
    this.mappingContext = mappingContext;
    this.domainType = (Class<T>) queryMethod.getMetadata().getDomainType();
    this.tree = new PartTree(queryMethod.getName(), this.domainType);
  }

  @Override
  public BigtableQueryMethod getQueryMethod() {
    return this.queryMethod;
  }

  @Override
  public Object execute(Object[] parameters) {
    List<T> results = runQuery(parameters);

    if (this.tree.isCountProjection()) {
      return results != null ? (long) results.size() : 0L;
    }

    if (this.tree.isExistsProjection()) {
      return results != null && !results.isEmpty();
    }

    if (this.tree.isDelete()) {
      if (results != null && !results.isEmpty()) {
        this.bigtableOperations.deleteAll(results);
      }
      Class<?> returnType = this.queryMethod.getReturnedObjectType();
      if (returnType == void.class) {
        return null;
      }
      if (Number.class.isAssignableFrom(returnType) || returnType == int.class || returnType == long.class) {
        return results != null ? results.size() : 0;
      }
      return results;
    }

    Class<?> returnType = this.queryMethod.getReturnedObjectType();
    if (Optional.class.isAssignableFrom(returnType)) {
      return Optional.ofNullable(results == null || results.isEmpty() ? null : results.get(0));
    }

    if (this.queryMethod.isCollectionQuery()) {
      return results != null ? results : Collections.emptyList();
    }

    return results == null || results.isEmpty() ? null : results.get(0);
  }

  private List<T> runQuery(Object[] parameters) {
    ParameterAccessor accessor =
        new ParametersParameterAccessor(this.queryMethod.getParameters(), parameters);
    Iterator<Object> iterator = accessor.iterator();

    List<Part> parts = new ArrayList<>();
    for (PartTree.OrPart orPart : this.tree.get()) {
      for (Part part : orPart) {
        parts.add(part);
      }
    }

    // Direct check for prefix query: method name contains Prefix or Part is STARTING_WITH
    String methodName = this.queryMethod.getName();
    if (parts.size() == 1 && (parts.get(0).getType() == Part.Type.STARTING_WITH || methodName.contains("Prefix"))) {
      Object param = iterator.hasNext() ? iterator.next() : null;
      String prefix = param != null ? String.valueOf(param) : "";
      return this.bigtableOperations.findByRowKeyPrefix(prefix, this.domainType);
    }

    // Direct check for range query: method name contains Range or Part is BETWEEN
    if ((parts.size() == 1 && (parts.get(0).getType() == Part.Type.BETWEEN || methodName.contains("Range")))
        || (parts.size() == 2 && parts.get(0).getType() == Part.Type.GREATER_THAN_EQUAL && parts.get(1).getType() == Part.Type.LESS_THAN)) {
      Object start = iterator.hasNext() ? iterator.next() : null;
      Object end = iterator.hasNext() ? iterator.next() : null;
      String startKey = start != null ? String.valueOf(start) : null;
      String endKey = end != null ? String.valueOf(end) : null;
      return this.bigtableOperations.findByRowKeyRange(startKey, endKey, this.domainType);
    }

    // If no predicate parts (e.g. findAll / countAll), return all
    if (parts.isEmpty()) {
      return this.bigtableOperations.findAll(this.domainType);
    }

    // General query construction using BigtableOperations.query(Query, Class)
    BigtablePersistentEntity<?> entity = this.mappingContext.getPersistentEntity(this.domainType);
    if (entity == null) {
      return Collections.emptyList();
    }

    Query query = Query.create(entity.getTableName());
    ChainFilter chainFilter = Filters.FILTERS.chain();
    boolean hasFilter = false;

    for (Part part : parts) {
      if (!iterator.hasNext()) {
        break;
      }
      Object val = iterator.next();
      String propName = part.getProperty();

      // Check if this property is a row key property
      boolean isRowKey = false;
      for (BigtablePersistentProperty rowKeyProp : entity.getRowKeyProperties()) {
        if (rowKeyProp.getName().equalsIgnoreCase(propName)) {
          isRowKey = true;
          break;
        }
      }

      if (isRowKey || propName.equalsIgnoreCase("rowKey") || propName.equalsIgnoreCase("id")) {
        if (part.getType() == Part.Type.STARTING_WITH) {
          query.prefix(val != null ? String.valueOf(val) : "");
        } else if (val != null) {
          ByteString encodedKey = this.bigtableOperations.getEntityConverter().encodeId(this.domainType, val);
          query.rowKey(encodedKey);
        }
      } else {
        // Property is a column property
        BigtablePersistentProperty columnProp = entity.getPersistentProperty(propName);
        if (columnProp != null && columnProp.isColumn()) {
          String family = columnProp.getFamilyName();
          String qualifier = columnProp.getColumnQualifier();
          chainFilter.filter(Filters.FILTERS.family().exactMatch(family));
          chainFilter.filter(Filters.FILTERS.qualifier().exactMatch(qualifier));
          if (val != null) {
            ByteString valBytes = ByteString.copyFromUtf8(String.valueOf(val));
            chainFilter.filter(Filters.FILTERS.value().exactMatch(valBytes));
          }
          hasFilter = true;
        }
      }
    }

    if (hasFilter) {
      query.filter(chainFilter);
    }

    return this.bigtableOperations.query(query, this.domainType);
  }
}

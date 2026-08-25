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

package com.google.cloud.spring.data.bigtable.core;

import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.BulkMutation;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.bigtable.data.v2.models.RowMutationEntry;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.convert.MappingBigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableDataException;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntity;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link BigtableOperations} executing operations on Cloud Bigtable
 * via {@link BigtableDataClient}, converting entities using {@link BigtableEntityConverter},
 * and resolving schema metadata via {@link BigtableMappingContext}.
 */
public class BigtableTemplate implements BigtableOperations {

  private static final int DELETE_ALL_BATCH_SIZE = 1000;

  private final BigtableDataClient dataClient;
  private final BigtableMappingContext mappingContext;
  private final BigtableEntityConverter entityConverter;

  public BigtableTemplate(
      BigtableDataClient dataClient,
      BigtableMappingContext mappingContext,
      BigtableEntityConverter entityConverter) {
    Assert.notNull(dataClient, "BigtableDataClient must not be null");
    Assert.notNull(mappingContext, "BigtableMappingContext must not be null");
    Assert.notNull(entityConverter, "BigtableEntityConverter must not be null");
    this.dataClient = dataClient;
    this.mappingContext = mappingContext;
    this.entityConverter = entityConverter;
  }

  public BigtableTemplate(BigtableDataClient dataClient, BigtableMappingContext mappingContext) {
    this(dataClient, mappingContext, new MappingBigtableEntityConverter(mappingContext));
  }

  @Override
  public BigtableMappingContext getMappingContext() {
    return this.mappingContext;
  }

  @Override
  public BigtableEntityConverter getEntityConverter() {
    return this.entityConverter;
  }

  @Override
  public BigtableDataClient getDataClient() {
    return this.dataClient;
  }

  @Override
  public <R> R execute(BigtableDataClientCallback<R> action) {
    Assert.notNull(action, "BigtableDataClientCallback must not be null");
    try {
      return action.doInBigtable(this.dataClient);
    } catch (Exception e) {
      throw translateException("Failed to execute Bigtable callback", e);
    }
  }

  @Override
  public <T> T findById(Object id, Class<T> entityClass) {
    Assert.notNull(id, "Id must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      ByteString rowKey = this.entityConverter.encodeId(entityClass, id);
      Row row = this.dataClient.readRow(tableName, rowKey);
      return this.entityConverter.read(entityClass, row);
    } catch (Exception e) {
      throw translateException(
          "Failed to find entity " + entityClass.getName() + " by id: " + id, e);
    }
  }

  @Override
  public <T> boolean existsById(Object id, Class<T> entityClass) {
    Assert.notNull(id, "Id must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      ByteString rowKey = this.entityConverter.encodeId(entityClass, id);
      Row row = this.dataClient.readRow(tableName, rowKey);
      return row != null;
    } catch (Exception e) {
      throw translateException(
          "Failed to check existence for entity " + entityClass.getName() + " by id: " + id, e);
    }
  }

  @Override
  public <T> List<T> findAllById(Iterable<?> ids, Class<T> entityClass) {
    Assert.notNull(ids, "Ids must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    String tableName = getTableName(entityClass);
    Query query = Query.create(tableName);
    boolean hasKeys = false;
    for (Object id : ids) {
      if (id != null) {
        ByteString rowKey = this.entityConverter.encodeId(entityClass, id);
        query.rowKey(rowKey);
        hasKeys = true;
      }
    }
    if (!hasKeys) {
      return Collections.emptyList();
    }
    return query(query, entityClass);
  }

  @Override
  public <T> List<T> findAll(Class<T> entityClass) {
    Assert.notNull(entityClass, "Entity class must not be null");
    String tableName = getTableName(entityClass);
    Query query = Query.create(tableName);
    return query(query, entityClass);
  }

  @Override
  public <T> List<T> findByRowKeyPrefix(String prefix, Class<T> entityClass) {
    Assert.notNull(prefix, "Row key prefix must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    String tableName = getTableName(entityClass);
    Query query = Query.create(tableName).prefix(prefix);
    return query(query, entityClass);
  }

  @Override
  public <T> List<T> findByRowKeyRange(String startKey, String endKey, Class<T> entityClass) {
    Assert.notNull(entityClass, "Entity class must not be null");
    String tableName = getTableName(entityClass);
    Query query = Query.create(tableName).range(startKey, endKey);
    return query(query, entityClass);
  }

  @Override
  public <T> List<T> query(Query query, Class<T> entityClass) {
    Assert.notNull(query, "Query must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      ServerStream<Row> serverStream = this.dataClient.readRows(query);
      List<T> results = new ArrayList<>();
      for (Row row : serverStream) {
        T entity = this.entityConverter.read(entityClass, row);
        if (entity != null) {
          results.add(entity);
        }
      }
      return results;
    } catch (Exception e) {
      throw translateException(
          "Failed to execute query for entity " + entityClass.getName(), e);
    }
  }

  @Override
  public <T> T save(T entity) {
    Assert.notNull(entity, "Entity must not be null");
    try {
      String tableName = getTableName(entity.getClass());
      ByteString rowKey = this.entityConverter.toRowKey(entity);
      RowMutation mutation = RowMutation.create(tableName, rowKey);
      this.entityConverter.write(entity, mutation);
      this.dataClient.mutateRow(mutation);
      return entity;
    } catch (Exception e) {
      throw translateException(
          "Failed to save entity of type " + entity.getClass().getName(), e);
    }
  }

  @Override
  public <T> Iterable<T> saveAll(Iterable<T> entities) {
    Assert.notNull(entities, "Entities must not be null");
    List<T> resultList = new ArrayList<>();
    Map<String, BulkMutation> mutationsByTable = new LinkedHashMap<>();
    try {
      for (T entity : entities) {
        if (entity != null) {
          String tableName = getTableName(entity.getClass());
          ByteString rowKey = this.entityConverter.toRowKey(entity);
          RowMutationEntry entry = RowMutationEntry.create(rowKey);
          this.entityConverter.write(entity, entry);
          mutationsByTable.computeIfAbsent(tableName, BulkMutation::create).add(entry);
          resultList.add(entity);
        }
      }
      for (BulkMutation bulkMutation : mutationsByTable.values()) {
        if (bulkMutation.getEntryCount() > 0) {
          this.dataClient.bulkMutateRows(bulkMutation);
        }
      }
      return resultList;
    } catch (Exception e) {
      throw translateException("Failed to saveAll entities", e);
    }
  }

  @Override
  public <T> void delete(T entity) {
    Assert.notNull(entity, "Entity to delete must not be null");
    try {
      String tableName = getTableName(entity.getClass());
      ByteString rowKey = this.entityConverter.toRowKey(entity);
      RowMutation mutation = RowMutation.create(tableName, rowKey).deleteRow();
      this.dataClient.mutateRow(mutation);
    } catch (Exception e) {
      throw translateException(
          "Failed to delete entity of type " + entity.getClass().getName(), e);
    }
  }

  @Override
  public <T> void deleteById(Object id, Class<T> entityClass) {
    Assert.notNull(id, "Id must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      ByteString rowKey = this.entityConverter.encodeId(entityClass, id);
      RowMutation mutation = RowMutation.create(tableName, rowKey).deleteRow();
      this.dataClient.mutateRow(mutation);
    } catch (Exception e) {
      throw translateException(
          "Failed to delete entity " + entityClass.getName() + " by id: " + id, e);
    }
  }

  @Override
  public <T> void deleteAllById(Iterable<?> ids, Class<T> entityClass) {
    Assert.notNull(ids, "Ids must not be null");
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      BulkMutation bulkMutation = BulkMutation.create(tableName);
      boolean hasEntries = false;
      for (Object id : ids) {
        if (id != null) {
          ByteString rowKey = this.entityConverter.encodeId(entityClass, id);
          bulkMutation.add(RowMutationEntry.create(rowKey).deleteRow());
          hasEntries = true;
        }
      }
      if (hasEntries) {
        this.dataClient.bulkMutateRows(bulkMutation);
      }
    } catch (Exception e) {
      throw translateException(
          "Failed to delete entities of type " + entityClass.getName() + " by ids", e);
    }
  }

  @Override
  public <T> void deleteAll(Iterable<? extends T> entities) {
    Assert.notNull(entities, "Entities must not be null");
    try {
      Map<String, BulkMutation> mutationsByTable = new LinkedHashMap<>();
      for (T entity : entities) {
        if (entity != null) {
          String tableName = getTableName(entity.getClass());
          ByteString rowKey = this.entityConverter.toRowKey(entity);
          mutationsByTable
              .computeIfAbsent(tableName, BulkMutation::create)
              .add(RowMutationEntry.create(rowKey).deleteRow());
        }
      }
      for (BulkMutation bulkMutation : mutationsByTable.values()) {
        if (bulkMutation.getEntryCount() > 0) {
          this.dataClient.bulkMutateRows(bulkMutation);
        }
      }
    } catch (Exception e) {
      throw translateException("Failed to deleteAll entities", e);
    }
  }

  @Override
  public <T> void deleteAll(Class<T> entityClass) {
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      Query query =
          Query.create(tableName)
              .filter(
                  Filters.FILTERS
                      .chain()
                      .filter(Filters.FILTERS.limit().cellsPerRow(1))
                      .filter(Filters.FILTERS.value().strip()));
      ServerStream<Row> serverStream = this.dataClient.readRows(query);
      BulkMutation bulkMutation = BulkMutation.create(tableName);
      int count = 0;
      for (Row row : serverStream) {
        bulkMutation.add(RowMutationEntry.create(row.getKey()).deleteRow());
        count++;
        if (count % DELETE_ALL_BATCH_SIZE == 0) {
          this.dataClient.bulkMutateRows(bulkMutation);
          bulkMutation = BulkMutation.create(tableName);
        }
      }
      if (bulkMutation.getEntryCount() > 0) {
        this.dataClient.bulkMutateRows(bulkMutation);
      }
    } catch (Exception e) {
      throw translateException(
          "Failed to deleteAll for entity " + entityClass.getName(), e);
    }
  }

  @Override
  public <T> long count(Class<T> entityClass) {
    Assert.notNull(entityClass, "Entity class must not be null");
    try {
      String tableName = getTableName(entityClass);
      Query query =
          Query.create(tableName)
              .filter(
                  Filters.FILTERS
                      .chain()
                      .filter(Filters.FILTERS.limit().cellsPerRow(1))
                      .filter(Filters.FILTERS.value().strip()));
      ServerStream<Row> serverStream = this.dataClient.readRows(query);
      long count = 0;
      for (Row ignored : serverStream) {
        count++;
      }
      return count;
    } catch (Exception e) {
      throw translateException(
          "Failed to count rows for entity " + entityClass.getName(), e);
    }
  }

  private String getTableName(Class<?> entityClass) {
    BigtablePersistentEntity<?> persistentEntity =
        this.mappingContext.getPersistentEntityOrFail(entityClass);
    return persistentEntity.getTableName();
  }

  private RuntimeException translateException(String message, Throwable t) {
    if (t instanceof DataAccessException dae) {
      return dae;
    }
    return new BigtableDataException(message + ": " + t.getMessage(), t);
  }
}

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

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import java.lang.reflect.Method;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.Assert;

/**
 * Determines and creates query executions for Cloud Bigtable query methods.
 */
public class BigtableQueryLookupStrategy implements QueryLookupStrategy {

  private final BigtableMappingContext bigtableMappingContext;
  private final BigtableOperations bigtableOperations;
  private final ValueExpressionDelegate valueExpressionDelegate;

  public BigtableQueryLookupStrategy(
      BigtableMappingContext bigtableMappingContext,
      BigtableOperations bigtableOperations,
      ValueExpressionDelegate valueExpressionDelegate) {
    Assert.notNull(bigtableMappingContext, "A valid BigtableMappingContext is required.");
    Assert.notNull(bigtableOperations, "A valid BigtableOperations is required.");
    this.bigtableMappingContext = bigtableMappingContext;
    this.bigtableOperations = bigtableOperations;
    this.valueExpressionDelegate = valueExpressionDelegate;
  }

  @Override
  public RepositoryQuery resolveQuery(
      Method method,
      RepositoryMetadata metadata,
      ProjectionFactory factory,
      NamedQueries namedQueries) {
    BigtableQueryMethod queryMethod = new BigtableQueryMethod(method, metadata, factory);
    return new BigtablePartTreeQuery<>(queryMethod, this.bigtableOperations, this.bigtableMappingContext);
  }
}

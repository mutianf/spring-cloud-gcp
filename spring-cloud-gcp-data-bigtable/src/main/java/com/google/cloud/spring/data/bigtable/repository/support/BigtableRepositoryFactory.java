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

package com.google.cloud.spring.data.bigtable.repository.support;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntity;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntityInformation;
import com.google.cloud.spring.data.bigtable.repository.query.BigtableQueryLookupStrategy;
import java.util.Optional;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory for creating Bigtable repository instances and resolving query lookup strategies.
 */
public class BigtableRepositoryFactory extends RepositoryFactorySupport
    implements ApplicationContextAware {

  private final BigtableMappingContext bigtableMappingContext;
  private final BigtableOperations bigtableOperations;
  private ApplicationContext applicationContext;

  public BigtableRepositoryFactory(
      BigtableMappingContext bigtableMappingContext, BigtableOperations bigtableOperations) {
    Assert.notNull(bigtableMappingContext, "A valid BigtableMappingContext is required.");
    Assert.notNull(bigtableOperations, "A valid BigtableOperations object is required.");
    this.bigtableMappingContext = bigtableMappingContext;
    this.bigtableOperations = bigtableOperations;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, I> EntityInformation<T, I> getEntityInformation(Class<T> domainClass) {
    BigtablePersistentEntity<?> entity =
        this.bigtableMappingContext.getPersistentEntity(domainClass);

    if (entity == null) {
      throw new MappingException(
          String.format(
              "Could not lookup mapping metadata for domain class %s!", domainClass.getName()));
    }

    return (EntityInformation<T, I>) new BigtablePersistentEntityInformation<>(entity);
  }

  @Override
  public Object getTargetRepository(RepositoryInformation metadata) {
    return getTargetRepositoryViaReflection(
        metadata, this.bigtableOperations, metadata.getDomainType());
  }

  @Override
  public Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
    return SimpleBigtableRepository.class;
  }

  @Override
  public Optional<QueryLookupStrategy> getQueryLookupStrategy(
      @Nullable Key key, ValueExpressionDelegate valueExpressionDelegate) {
    return Optional.of(
        new BigtableQueryLookupStrategy(
            this.bigtableMappingContext, this.bigtableOperations, valueExpressionDelegate));
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}

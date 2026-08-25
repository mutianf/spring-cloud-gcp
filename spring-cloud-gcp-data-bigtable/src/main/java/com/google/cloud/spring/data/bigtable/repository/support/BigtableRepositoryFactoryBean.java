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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * {@link org.springframework.beans.factory.FactoryBean} creating {@link BigtableRepositoryFactory} instances.
 *
 * @param <T> the repository type
 * @param <S> the entity type
 * @param <I> the identifier type
 */
public class BigtableRepositoryFactoryBean<T extends Repository<S, I>, S, I>
    extends RepositoryFactoryBeanSupport<T, S, I> implements ApplicationContextAware {

  private BigtableMappingContext bigtableMappingContext;
  private BigtableOperations bigtableOperations;
  private ApplicationContext applicationContext;

  public BigtableRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  public void setBigtableOperations(BigtableOperations bigtableOperations) {
    this.bigtableOperations = bigtableOperations;
  }

  public void setBigtableTemplate(BigtableOperations bigtableOperations) {
    this.bigtableOperations = bigtableOperations;
  }

  public void setBigtableMappingContext(BigtableMappingContext mappingContext) {
    super.setMappingContext(mappingContext);
    this.bigtableMappingContext = mappingContext;
  }

  @Override
  protected RepositoryFactorySupport createRepositoryFactory() {
    BigtableRepositoryFactory factory =
        new BigtableRepositoryFactory(this.bigtableMappingContext, this.bigtableOperations);
    factory.setApplicationContext(this.applicationContext);
    return factory;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}

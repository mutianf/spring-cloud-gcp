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

package com.google.cloud.spring.data.bigtable.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.repository.support.BigtableRepositoryFactory;
import com.google.cloud.spring.data.bigtable.repository.support.BigtableRepositoryFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Unit tests for {@link BigtableRepositoryFactoryBean}.
 */
class BigtableRepositoryFactoryBeanTest {

  private BigtableRepositoryFactoryBean<BigtableRepository<Object, String>, Object, String> factoryBean;
  private BigtableMappingContext mappingContext;
  private BigtableOperations operations;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    this.mappingContext = new BigtableMappingContext();
    this.operations = mock(BigtableOperations.class);
    this.factoryBean = new BigtableRepositoryFactoryBean(BigtableRepository.class);
    this.factoryBean.setBigtableMappingContext(this.mappingContext);
    this.factoryBean.setBigtableOperations(this.operations);
    this.factoryBean.setBigtableTemplate(this.operations);
  }

  @Test
  void testCreateRepositoryFactory() {
    TestRepositoryFactoryBean<BigtableRepository<Object, String>, Object, String> testBean =
        new TestRepositoryFactoryBean<>(BigtableRepository.class);
    testBean.setBigtableMappingContext(this.mappingContext);
    testBean.setBigtableOperations(this.operations);

    RepositoryFactorySupport factory = testBean.invokeCreateRepositoryFactory();
    assertThat(factory).isInstanceOf(BigtableRepositoryFactory.class);
  }

  @Test
  void testGetObjectAndType() {
    this.factoryBean.afterPropertiesSet();
    assertThat(this.factoryBean.getObjectType()).isEqualTo(BigtableRepository.class);
    assertThat(this.factoryBean.isSingleton()).isTrue();
    assertThat(this.factoryBean.getObject()).isNotNull();
  }

  @Test
  void testSetApplicationContext() {
    ApplicationContext context = mock(ApplicationContext.class);
    this.factoryBean.setApplicationContext(context);
    // Verifies no exception
  }

  private static class TestRepositoryFactoryBean<T extends BigtableRepository<S, I>, S, I>
      extends BigtableRepositoryFactoryBean<T, S, I> {
    TestRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
      super(repositoryInterface);
    }

    RepositoryFactorySupport invokeCreateRepositoryFactory() {
      return createRepositoryFactory();
    }
  }
}

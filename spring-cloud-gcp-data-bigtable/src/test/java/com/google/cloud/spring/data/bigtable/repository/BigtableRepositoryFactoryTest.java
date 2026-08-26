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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import com.google.cloud.spring.data.bigtable.repository.query.BigtableQueryLookupStrategy;
import com.google.cloud.spring.data.bigtable.repository.support.BigtableRepositoryFactory;
import com.google.cloud.spring.data.bigtable.repository.support.SimpleBigtableRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Unit tests for {@link BigtableRepositoryFactory}.
 */
class BigtableRepositoryFactoryTest {

  private BigtableMappingContext mappingContext;
  private BigtableOperations bigtableOperations;
  private BigtableRepositoryFactory factory;

  @BeforeEach
  void setUp() {
    this.mappingContext = new BigtableMappingContext();
    this.bigtableOperations = mock(BigtableOperations.class);
    this.factory = new BigtableRepositoryFactory(this.mappingContext, this.bigtableOperations);
  }

  @Test
  void testConstructorValidation() {
    assertThatThrownBy(() -> new BigtableRepositoryFactory(null, this.bigtableOperations))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("BigtableMappingContext");

    assertThatThrownBy(() -> new BigtableRepositoryFactory(this.mappingContext, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("BigtableOperations");
  }

  @Test
  void testGetEntityInformation() {
    EntityInformation<TestOrder, String> entityInformation =
        this.factory.getEntityInformation(TestOrder.class);

    assertThat(entityInformation.getJavaType()).isEqualTo(TestOrder.class);
    assertThat(entityInformation.getIdType()).isEqualTo(String.class);

    TestOrder order = new TestOrder("ord#123", 99.5);
    assertThat(entityInformation.getId(order)).isEqualTo("ord#123");
    assertThat(entityInformation.isNew(order)).isFalse();
    assertThat(entityInformation.isNew(new TestOrder())).isTrue();
  }

  @Test
  void testGetEntityInformation_NotAvailableThrowsException() {
    BigtableRepositoryFactory mockFactory =
        new BigtableRepositoryFactory(mock(BigtableMappingContext.class), this.bigtableOperations);

    assertThatThrownBy(() -> mockFactory.getEntityInformation(TestOrder.class))
        .isInstanceOf(MappingException.class)
        .hasMessageContaining("Could not lookup mapping metadata");
  }

  @Test
  void testGetTargetRepository() {
    RepositoryInformation repoInfo = mock(RepositoryInformation.class);
    when(repoInfo.getRepositoryBaseClass()).thenAnswer(inv -> SimpleBigtableRepository.class);
    when(repoInfo.getDomainType()).thenAnswer(inv -> TestOrder.class);

    Object repo = this.factory.getTargetRepository(repoInfo);
    assertThat(repo).isInstanceOf(SimpleBigtableRepository.class);

    SimpleBigtableRepository<?, ?> simpleRepo = (SimpleBigtableRepository<?, ?>) repo;
    assertThat(simpleRepo.getBigtableOperations()).isSameAs(this.bigtableOperations);
  }

  @Test
  void testGetRepositoryBaseClass() {
    Class<?> baseClass = this.factory.getRepositoryBaseClass(null);
    assertThat(baseClass).isEqualTo(SimpleBigtableRepository.class);
  }

  @Test
  void testGetQueryLookupStrategy() {
    Optional<QueryLookupStrategy> strategy =
        this.factory.getQueryLookupStrategy(null, mock(ValueExpressionDelegate.class));

    assertThat(strategy).isPresent();
    assertThat(strategy.get()).isInstanceOf(BigtableQueryLookupStrategy.class);
  }

  @Test
  void testSetApplicationContext() {
    ApplicationContext context = mock(ApplicationContext.class);
    this.factory.setApplicationContext(context);
    // Verifies no exception thrown
  }

  @Test
  void testGetRepositoryProxy() {
    TestOrderRepository repository = this.factory.getRepository(TestOrderRepository.class);
    assertThat(repository).isNotNull();
  }

  interface TestOrderRepository extends BigtableRepository<TestOrder, String> {}

  @BigtableTable(name = "orders")
  private static class TestOrder {
    @RowKey
    private String orderId;

    @Column(family = "details")
    private Double total;

    TestOrder() {}

    TestOrder(String orderId, Double total) {
      this.orderId = orderId;
      this.total = total;
    }

    public String getOrderId() {
      return orderId;
    }

    public Double getTotal() {
      return total;
    }
  }
}

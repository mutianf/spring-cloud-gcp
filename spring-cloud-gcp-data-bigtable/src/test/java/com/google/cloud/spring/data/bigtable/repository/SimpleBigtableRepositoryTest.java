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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import com.google.cloud.spring.data.bigtable.repository.support.SimpleBigtableRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimpleBigtableRepository}.
 */
class SimpleBigtableRepositoryTest {

  private BigtableOperations bigtableOperations;
  private SimpleBigtableRepository<TestCustomer, String> repository;

  @BeforeEach
  void setUp() {
    this.bigtableOperations = mock(BigtableOperations.class);
    this.repository = new SimpleBigtableRepository<>(this.bigtableOperations, TestCustomer.class);
  }

  @Test
  void testConstructorValidation() {
    assertThatThrownBy(() -> new SimpleBigtableRepository<>(null, TestCustomer.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("BigtableOperations");

    assertThatThrownBy(() -> new SimpleBigtableRepository<>(this.bigtableOperations, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("entity type");
  }

  @Test
  void testGetBigtableOperations() {
    assertThat(this.repository.getBigtableOperations()).isSameAs(this.bigtableOperations);
  }

  @Test
  void testSave() {
    TestCustomer customer = new TestCustomer("c1", "Alice");
    when(this.bigtableOperations.save(customer)).thenReturn(customer);

    TestCustomer saved = this.repository.save(customer);
    assertThat(saved).isSameAs(customer);
    verify(this.bigtableOperations).save(customer);
  }

  @Test
  void testSave_NullEntityThrowsException() {
    assertThatThrownBy(() -> this.repository.save(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Entity must not be null");
  }

  @Test
  void testSaveAll() {
    TestCustomer c1 = new TestCustomer("c1", "Alice");
    TestCustomer c2 = new TestCustomer("c2", "Bob");
    List<TestCustomer> customers = Arrays.asList(c1, c2);
    when(this.bigtableOperations.saveAll(customers)).thenReturn(customers);

    Iterable<TestCustomer> saved = this.repository.saveAll(customers);
    assertThat(saved).isEqualTo(customers);
    verify(this.bigtableOperations).saveAll(customers);
  }

  @Test
  void testSaveAll_NullEntitiesThrowsException() {
    assertThatThrownBy(() -> this.repository.saveAll(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Entities must not be null");
  }

  @Test
  void testFindById_Found() {
    TestCustomer customer = new TestCustomer("c1", "Alice");
    when(this.bigtableOperations.findById("c1", TestCustomer.class)).thenReturn(customer);

    Optional<TestCustomer> result = this.repository.findById("c1");
    assertThat(result).contains(customer);
    verify(this.bigtableOperations).findById("c1", TestCustomer.class);
  }

  @Test
  void testFindById_NotFound() {
    when(this.bigtableOperations.findById("c99", TestCustomer.class)).thenReturn(null);

    Optional<TestCustomer> result = this.repository.findById("c99");
    assertThat(result).isEmpty();
    verify(this.bigtableOperations).findById("c99", TestCustomer.class);
  }

  @Test
  void testFindById_NullIdThrowsException() {
    assertThatThrownBy(() -> this.repository.findById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ID must not be null");
  }

  @Test
  void testExistsById() {
    when(this.bigtableOperations.existsById("c1", TestCustomer.class)).thenReturn(true);
    when(this.bigtableOperations.existsById("c2", TestCustomer.class)).thenReturn(false);

    assertThat(this.repository.existsById("c1")).isTrue();
    assertThat(this.repository.existsById("c2")).isFalse();

    verify(this.bigtableOperations).existsById("c1", TestCustomer.class);
    verify(this.bigtableOperations).existsById("c2", TestCustomer.class);
  }

  @Test
  void testExistsById_NullIdThrowsException() {
    assertThatThrownBy(() -> this.repository.existsById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ID must not be null");
  }

  @Test
  void testFindAll() {
    TestCustomer c1 = new TestCustomer("c1", "Alice");
    List<TestCustomer> customers = Collections.singletonList(c1);
    when(this.bigtableOperations.findAll(TestCustomer.class)).thenReturn(customers);

    Iterable<TestCustomer> result = this.repository.findAll();
    assertThat(result).isEqualTo(customers);
    verify(this.bigtableOperations).findAll(TestCustomer.class);
  }

  @Test
  void testFindAllById() {
    List<String> ids = Arrays.asList("c1", "c2");
    TestCustomer c1 = new TestCustomer("c1", "Alice");
    TestCustomer c2 = new TestCustomer("c2", "Bob");
    List<TestCustomer> customers = Arrays.asList(c1, c2);
    when(this.bigtableOperations.findAllById(ids, TestCustomer.class)).thenReturn(customers);

    Iterable<TestCustomer> result = this.repository.findAllById(ids);
    assertThat(result).isEqualTo(customers);
    verify(this.bigtableOperations).findAllById(ids, TestCustomer.class);
  }

  @Test
  void testFindAllById_NullIdsThrowsException() {
    assertThatThrownBy(() -> this.repository.findAllById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IDs must not be null");
  }

  @Test
  void testCount() {
    when(this.bigtableOperations.count(TestCustomer.class)).thenReturn(42L);

    assertThat(this.repository.count()).isEqualTo(42L);
    verify(this.bigtableOperations).count(TestCustomer.class);
  }

  @Test
  void testDelete() {
    TestCustomer customer = new TestCustomer("c1", "Alice");
    this.repository.delete(customer);
    verify(this.bigtableOperations).delete(customer);
  }

  @Test
  void testDelete_NullEntityThrowsException() {
    assertThatThrownBy(() -> this.repository.delete(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Entity must not be null");
  }

  @Test
  void testDeleteById() {
    this.repository.deleteById("c1");
    verify(this.bigtableOperations).deleteById("c1", TestCustomer.class);
  }

  @Test
  void testDeleteById_NullIdThrowsException() {
    assertThatThrownBy(() -> this.repository.deleteById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ID must not be null");
  }

  @Test
  void testDeleteAllById() {
    List<String> ids = Arrays.asList("c1", "c2");
    this.repository.deleteAllById(ids);
    verify(this.bigtableOperations).deleteAllById(ids, TestCustomer.class);
  }

  @Test
  void testDeleteAllById_NullIdsThrowsException() {
    assertThatThrownBy(() -> this.repository.deleteAllById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IDs must not be null");
  }

  @Test
  void testDeleteAllEntities() {
    List<TestCustomer> customers = Collections.singletonList(new TestCustomer("c1", "Alice"));
    this.repository.deleteAll(customers);
    verify(this.bigtableOperations).deleteAll(customers);
  }

  @Test
  void testDeleteAllEntities_NullEntitiesThrowsException() {
    assertThatThrownBy(() -> this.repository.deleteAll((Iterable<? extends TestCustomer>) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Entities must not be null");
  }

  @Test
  void testDeleteAll() {
    this.repository.deleteAll();
    verify(this.bigtableOperations).deleteAll(TestCustomer.class);
  }

  @Test
  void testFindByRowKeyPrefix() {
    TestCustomer c1 = new TestCustomer("cust#100", "Alice");
    List<TestCustomer> customers = Collections.singletonList(c1);
    when(this.bigtableOperations.findByRowKeyPrefix("cust#", TestCustomer.class)).thenReturn(customers);

    List<TestCustomer> result = this.repository.findByRowKeyPrefix("cust#");
    assertThat(result).isEqualTo(customers);
    verify(this.bigtableOperations).findByRowKeyPrefix("cust#", TestCustomer.class);
  }

  @Test
  void testFindByRowKeyPrefix_NullPrefixThrowsException() {
    assertThatThrownBy(() -> this.repository.findByRowKeyPrefix(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Prefix must not be null");
  }

  @Test
  void testFindByRowKeyRange() {
    TestCustomer c1 = new TestCustomer("cust#100", "Alice");
    List<TestCustomer> customers = Collections.singletonList(c1);
    when(this.bigtableOperations.findByRowKeyRange("cust#100", "cust#200", TestCustomer.class))
        .thenReturn(customers);

    List<TestCustomer> result = this.repository.findByRowKeyRange("cust#100", "cust#200");
    assertThat(result).isEqualTo(customers);
    verify(this.bigtableOperations).findByRowKeyRange("cust#100", "cust#200", TestCustomer.class);
  }

  @BigtableTable(name = "customers")
  private static class TestCustomer {
    @RowKey
    private String customerId;

    @Column(family = "info")
    private String name;

    TestCustomer() {}

    TestCustomer(String customerId, String name) {
      this.customerId = customerId;
      this.name = name;
    }

    public String getCustomerId() {
      return customerId;
    }

    public String getName() {
      return name;
    }
  }
}

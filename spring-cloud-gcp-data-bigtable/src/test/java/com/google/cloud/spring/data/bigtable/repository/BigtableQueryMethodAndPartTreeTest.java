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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import com.google.cloud.spring.data.bigtable.repository.query.BigtablePartTreeQuery;
import com.google.cloud.spring.data.bigtable.repository.query.BigtableQueryLookupStrategy;
import com.google.cloud.spring.data.bigtable.repository.query.BigtableQueryMethod;
import com.google.cloud.spring.data.bigtable.repository.query.PartTreeBigtableQuery;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;

/**
 * Unit tests for derived query methods: {@link BigtableQueryMethod},
 * {@link BigtablePartTreeQuery}, {@link PartTreeBigtableQuery}, and {@link BigtableQueryLookupStrategy}.
 */
class BigtableQueryMethodAndPartTreeTest {

  private BigtableMappingContext mappingContext;
  private BigtableOperations bigtableOperations;
  private BigtableEntityConverter entityConverter;
  private RepositoryMetadata metadata;
  private ProjectionFactory projectionFactory;
  private BigtableQueryLookupStrategy lookupStrategy;

  @BeforeEach
  void setUp() {
    this.mappingContext = new BigtableMappingContext();
    this.bigtableOperations = mock(BigtableOperations.class);
    this.entityConverter = mock(BigtableEntityConverter.class);
    when(this.bigtableOperations.getEntityConverter()).thenReturn(this.entityConverter);

    this.metadata = mock(RepositoryMetadata.class);
    when(this.metadata.getDomainType()).thenAnswer(inv -> TestUser.class);
    when(this.metadata.getReturnedDomainClass(any())).thenAnswer(inv -> TestUser.class);

    this.projectionFactory = mock(ProjectionFactory.class);
    this.lookupStrategy = new BigtableQueryLookupStrategy(
        this.mappingContext, this.bigtableOperations, mock(ValueExpressionDelegate.class));
  }

  @Test
  void testQueryMethodMetadata() throws Exception {
    Method method = TestUserRepository.class.getMethod("findByRowKeyStartingWith", String.class);
    BigtableQueryMethod queryMethod = new BigtableQueryMethod(method, this.metadata, this.projectionFactory);

    assertThat(queryMethod.getMethod()).isEqualTo(method);
    assertThat(queryMethod.getName()).isEqualTo("findByRowKeyStartingWith");
    assertThat(queryMethod.isCollectionQuery()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPrefixQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("findByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser user = new TestUser("usr#123", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyPrefix("usr#", TestUser.class))
        .thenReturn(Collections.singletonList(user));

    Object result = query.execute(new Object[] {"usr#"});
    assertThat(result).isInstanceOf(List.class);
    List<TestUser> list = (List<TestUser>) result;
    assertThat(list).containsExactly(user);
    verify(this.bigtableOperations).findByRowKeyPrefix("usr#", TestUser.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRangeQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("findByRowKeyBetween", String.class, String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser u1 = new TestUser("usr#100", "ACTIVE");
    TestUser u2 = new TestUser("usr#200", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyRange("usr#100", "usr#300", TestUser.class))
        .thenReturn(Arrays.asList(u1, u2));

    Object result = query.execute(new Object[] {"usr#100", "usr#300"});
    assertThat(result).isInstanceOf(List.class);
    List<TestUser> list = (List<TestUser>) result;
    assertThat(list).containsExactly(u1, u2);
    verify(this.bigtableOperations).findByRowKeyRange("usr#100", "usr#300", TestUser.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRangeQueryExecution_GreaterThanEqualAndLessThan() throws Exception {
    Method method = TestUserRepository.class.getMethod(
        "findByRowKeyGreaterThanEqualAndRowKeyLessThan", String.class, String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser u1 = new TestUser("usr#100", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyRange("usr#100", "usr#300", TestUser.class))
        .thenReturn(Collections.singletonList(u1));

    Object result = query.execute(new Object[] {"usr#100", "usr#300"});
    assertThat(result).isInstanceOf(List.class);
    List<TestUser> list = (List<TestUser>) result;
    assertThat(list).containsExactly(u1);
    verify(this.bigtableOperations).findByRowKeyRange("usr#100", "usr#300", TestUser.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testColumnFilterQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("findByStatus", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser user = new TestUser("usr#1", "ACTIVE");
    when(this.bigtableOperations.query(any(Query.class), eq(TestUser.class)))
        .thenReturn(Collections.singletonList(user));

    Object result = query.execute(new Object[] {"ACTIVE"});
    assertThat(result).isInstanceOf(List.class);
    List<TestUser> list = (List<TestUser>) result;
    assertThat(list).containsExactly(user);
    verify(this.bigtableOperations).query(any(Query.class), eq(TestUser.class));
  }

  @Test
  void testCountQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("countByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser u1 = new TestUser("usr#1", "ACTIVE");
    TestUser u2 = new TestUser("usr#2", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyPrefix("usr#", TestUser.class))
        .thenReturn(Arrays.asList(u1, u2));

    Object result = query.execute(new Object[] {"usr#"});
    assertThat(result).isEqualTo(2L);
  }

  @Test
  void testExistsQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("existsByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    when(this.bigtableOperations.findByRowKeyPrefix("usr#", TestUser.class))
        .thenReturn(Collections.singletonList(new TestUser("usr#1", "ACTIVE")));

    Object result = query.execute(new Object[] {"usr#"});
    assertThat(result).isEqualTo(true);

    when(this.bigtableOperations.findByRowKeyPrefix("absent#", TestUser.class))
        .thenReturn(Collections.emptyList());

    Object absentResult = query.execute(new Object[] {"absent#"});
    assertThat(absentResult).isEqualTo(false);
  }

  @Test
  void testDeleteQueryExecution() throws Exception {
    Method method = TestUserRepository.class.getMethod("deleteByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser u1 = new TestUser("usr#1", "ACTIVE");
    List<TestUser> toDelete = Collections.singletonList(u1);
    when(this.bigtableOperations.findByRowKeyPrefix("usr#", TestUser.class)).thenReturn(toDelete);

    Object result = query.execute(new Object[] {"usr#"});
    assertThat(result).isEqualTo(1);
    verify(this.bigtableOperations).deleteAll(toDelete);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testOptionalReturnType_Found() throws Exception {
    Method method = TestUserRepository.class.getMethod("findOptionalByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    TestUser user = new TestUser("usr#1", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyPrefix("usr#1", TestUser.class))
        .thenReturn(Collections.singletonList(user));

    Object result = query.execute(new Object[] {"usr#1"});
    assertThat(result).isInstanceOf(Optional.class);
    Optional<TestUser> opt = (Optional<TestUser>) result;
    assertThat(opt).contains(user);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testOptionalReturnType_NotFound() throws Exception {
    Method method = TestUserRepository.class.getMethod("findOptionalByRowKeyStartingWith", String.class);
    RepositoryQuery query = this.lookupStrategy.resolveQuery(method, this.metadata, this.projectionFactory, null);

    when(this.bigtableOperations.findByRowKeyPrefix("usr#none", TestUser.class))
        .thenReturn(Collections.emptyList());

    Object result = query.execute(new Object[] {"usr#none"});
    assertThat(result).isInstanceOf(Optional.class);
    Optional<TestUser> opt = (Optional<TestUser>) result;
    assertThat(opt).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPartTreeBigtableQueryAlias() throws Exception {
    Method method = TestUserRepository.class.getMethod("findByRowKeyStartingWith", String.class);
    BigtableQueryMethod queryMethod = new BigtableQueryMethod(method, this.metadata, this.projectionFactory);
    PartTreeBigtableQuery<TestUser> query =
        new PartTreeBigtableQuery<>(queryMethod, this.bigtableOperations, this.mappingContext);

    TestUser user = new TestUser("usr#1", "ACTIVE");
    when(this.bigtableOperations.findByRowKeyPrefix("usr#", TestUser.class))
        .thenReturn(Collections.singletonList(user));

    Object result = query.execute(new Object[] {"usr#"});
    assertThat(result).isInstanceOf(List.class);
    List<TestUser> list = (List<TestUser>) result;
    assertThat(list).containsExactly(user);
    assertThat(query.getQueryMethod()).isSameAs(queryMethod);
  }

  interface TestUserRepository extends BigtableRepository<TestUser, String> {
    List<TestUser> findByRowKeyStartingWith(String prefix);
    List<TestUser> findByRowKeyBetween(String startKey, String endKey);
    List<TestUser> findByRowKeyGreaterThanEqualAndRowKeyLessThan(String startKey, String endKey);
    List<TestUser> findByStatus(String status);
    long countByRowKeyStartingWith(String prefix);
    boolean existsByRowKeyStartingWith(String prefix);
    int deleteByRowKeyStartingWith(String prefix);
    Optional<TestUser> findOptionalByRowKeyStartingWith(String prefix);
  }

  @BigtableTable(name = "users")
  private static class TestUser {
    @RowKey
    private String rowKey;

    @Column(family = "profile")
    private String status;

    TestUser() {}

    TestUser(String rowKey, String status) {
      this.rowKey = rowKey;
      this.status = status;
    }

    public String getRowKey() {
      return rowKey;
    }

    public String getStatus() {
      return status;
    }
  }
}

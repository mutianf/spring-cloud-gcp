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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.BulkMutation;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.spring.data.bigtable.core.convert.MappingBigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableDataException;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;

/**
 * Unit tests for {@link BigtableTemplate}.
 */
class BigtableTemplateTest {

  private BigtableDataClient dataClient;
  private BigtableMappingContext mappingContext;
  private MappingBigtableEntityConverter entityConverter;
  private BigtableTemplate template;

  @BeforeEach
  void setUp() {
    this.dataClient = mock(BigtableDataClient.class);
    this.mappingContext = new BigtableMappingContext();
    this.entityConverter = new MappingBigtableEntityConverter(this.mappingContext);
    this.template =
        new BigtableTemplate(this.dataClient, this.mappingContext, this.entityConverter);
  }

  @Test
  void testAccessors() {
    assertThat(this.template.getMappingContext()).isSameAs(this.mappingContext);
    assertThat(this.template.getEntityConverter()).isSameAs(this.entityConverter);
    assertThat(this.template.getDataClient()).isSameAs(this.dataClient);
  }

  @Test
  void testConstructorWithDefaultConverter() {
    BigtableTemplate defaultTemplate = new BigtableTemplate(this.dataClient, this.mappingContext);
    assertThat(defaultTemplate.getEntityConverter()).isNotNull();
    assertThat(defaultTemplate.getMappingContext()).isSameAs(this.mappingContext);
    assertThat(defaultTemplate.getDataClient()).isSameAs(this.dataClient);
  }

  @Test
  void testFindById_Found() {
    ByteString key = ByteString.copyFromUtf8("c1");
    RowCell nameCell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("Alice"));
    RowCell emailCell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("email"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("alice@example.com"));
    Row row = Row.create(key, Arrays.asList(nameCell, emailCell));

    when(this.dataClient.readRow("customers", key)).thenReturn(row);

    TestCustomer customer = this.template.findById("c1", TestCustomer.class);

    assertThat(customer).isNotNull();
    assertThat(customer.getId()).isEqualTo("c1");
    assertThat(customer.getName()).isEqualTo("Alice");
    assertThat(customer.getEmail()).isEqualTo("alice@example.com");

    verify(this.dataClient).readRow("customers", key);
  }

  @Test
  void testFindById_NotFound() {
    ByteString key = ByteString.copyFromUtf8("missing");
    when(this.dataClient.readRow("customers", key)).thenReturn(null);

    TestCustomer customer = this.template.findById("missing", TestCustomer.class);

    assertThat(customer).isNull();
    verify(this.dataClient).readRow("customers", key);
  }

  @Test
  void testFindById_NullArgumentsThrowsException() {
    assertThatThrownBy(() -> this.template.findById(null, TestCustomer.class))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> this.template.findById("c1", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testExistsById_True() {
    ByteString key = ByteString.copyFromUtf8("c1");
    Row row = Row.create(key, Collections.emptyList());
    when(this.dataClient.readRow("customers", key)).thenReturn(row);

    boolean exists = this.template.existsById("c1", TestCustomer.class);

    assertThat(exists).isTrue();
    verify(this.dataClient).readRow("customers", key);
  }

  @Test
  void testExistsById_False() {
    ByteString key = ByteString.copyFromUtf8("c1");
    when(this.dataClient.readRow("customers", key)).thenReturn(null);

    boolean exists = this.template.existsById("c1", TestCustomer.class);

    assertThat(exists).isFalse();
    verify(this.dataClient).readRow("customers", key);
  }

  @Test
  void testSave() {
    TestCustomer customer = new TestCustomer("c1", "Bob", "bob@example.com");

    TestCustomer saved = this.template.save(customer);

    assertThat(saved).isSameAs(customer);
    verify(this.dataClient).mutateRow(any(RowMutation.class));
  }

  @Test
  void testSaveAll() {
    TestCustomer c1 = new TestCustomer("c1", "Bob", "bob@example.com");
    TestCustomer c2 = new TestCustomer("c2", "Charlie", "charlie@example.com");

    Iterable<TestCustomer> saved = this.template.saveAll(Arrays.asList(c1, c2));

    assertThat(saved).containsExactly(c1, c2);
    ArgumentCaptor<BulkMutation> captor = ArgumentCaptor.forClass(BulkMutation.class);
    verify(this.dataClient).bulkMutateRows(captor.capture());
    assertThat(captor.getValue().getEntryCount()).isEqualTo(2);
  }

  @Test
  void testSaveAll_Empty() {
    Iterable<TestCustomer> saved = this.template.saveAll(Collections.emptyList());

    assertThat(saved).isEmpty();
    verify(this.dataClient, never()).bulkMutateRows(any(BulkMutation.class));
  }

  @Test
  void testDelete() {
    TestCustomer customer = new TestCustomer("c1", "Bob", "bob@example.com");

    this.template.delete(customer);

    verify(this.dataClient).mutateRow(any(RowMutation.class));
  }

  @Test
  void testDeleteById() {
    this.template.deleteById("c1", TestCustomer.class);

    verify(this.dataClient).mutateRow(any(RowMutation.class));
  }

  @Test
  void testDeleteById_CompositeKey() {
    this.template.deleteById(new Object[] {"cust_1", "ord_99"}, TestOrder.class);

    verify(this.dataClient).mutateRow(any(RowMutation.class));
  }

  @Test
  void testDeleteAllById() {
    this.template.deleteAllById(Arrays.asList("c1", "c2", "c3"), TestCustomer.class);

    ArgumentCaptor<BulkMutation> captor = ArgumentCaptor.forClass(BulkMutation.class);
    verify(this.dataClient).bulkMutateRows(captor.capture());
    assertThat(captor.getValue().getEntryCount()).isEqualTo(3);
  }

  @Test
  void testDeleteAllById_Empty() {
    this.template.deleteAllById(Collections.emptyList(), TestCustomer.class);

    verify(this.dataClient, never()).bulkMutateRows(any(BulkMutation.class));
  }

  @Test
  void testDeleteAllEntities() {
    TestCustomer c1 = new TestCustomer("c1", "A", "a@a.com");
    TestCustomer c2 = new TestCustomer("c2", "B", "b@b.com");

    this.template.deleteAll(Arrays.asList(c1, c2));

    ArgumentCaptor<BulkMutation> captor = ArgumentCaptor.forClass(BulkMutation.class);
    verify(this.dataClient).bulkMutateRows(captor.capture());
    assertThat(captor.getValue().getEntryCount()).isEqualTo(2);
  }

  @Test
  void testDeleteAllClass() {
    Row r1 = Row.create(ByteString.copyFromUtf8("c1"), Collections.emptyList());
    Row r2 = Row.create(ByteString.copyFromUtf8("c2"), Collections.emptyList());

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Arrays.asList(r1, r2).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    this.template.deleteAll(TestCustomer.class);

    ArgumentCaptor<BulkMutation> captor = ArgumentCaptor.forClass(BulkMutation.class);
    verify(this.dataClient).bulkMutateRows(captor.capture());
    assertThat(captor.getValue().getEntryCount()).isEqualTo(2);
  }

  @Test
  void testFindAllById() {
    RowCell nameCell1 =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("User1"));
    Row r1 = Row.create(ByteString.copyFromUtf8("c1"), Collections.singletonList(nameCell1));

    RowCell nameCell2 =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("User2"));
    Row r2 = Row.create(ByteString.copyFromUtf8("c2"), Collections.singletonList(nameCell2));

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Arrays.asList(r1, r2).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    List<TestCustomer> customers =
        this.template.findAllById(Arrays.asList("c1", "c2"), TestCustomer.class);

    assertThat(customers).hasSize(2);
    assertThat(customers.get(0).getName()).isEqualTo("User1");
    assertThat(customers.get(1).getName()).isEqualTo("User2");
  }

  @Test
  void testFindAllById_Empty() {
    List<TestCustomer> customers =
        this.template.findAllById(Collections.emptyList(), TestCustomer.class);

    assertThat(customers).isEmpty();
    verify(this.dataClient, never()).readRows(any(Query.class));
  }

  @Test
  void testFindAll() {
    RowCell cell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("User1"));
    Row r1 = Row.create(ByteString.copyFromUtf8("c1"), Collections.singletonList(cell));

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Collections.singletonList(r1).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    List<TestCustomer> customers = this.template.findAll(TestCustomer.class);

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getId()).isEqualTo("c1");
    assertThat(customers.get(0).getName()).isEqualTo("User1");
  }

  @Test
  void testFindByRowKeyPrefix() {
    RowCell cell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("PrefixUser"));
    Row r1 = Row.create(ByteString.copyFromUtf8("pref_1"), Collections.singletonList(cell));

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Collections.singletonList(r1).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    List<TestCustomer> customers = this.template.findByRowKeyPrefix("pref_", TestCustomer.class);

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getName()).isEqualTo("PrefixUser");
  }

  @Test
  void testFindByRowKeyRange() {
    RowCell cell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("RangeUser"));
    Row r1 = Row.create(ByteString.copyFromUtf8("range_1"), Collections.singletonList(cell));

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Collections.singletonList(r1).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    List<TestCustomer> customers =
        this.template.findByRowKeyRange("range_0", "range_9", TestCustomer.class);

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getName()).isEqualTo("RangeUser");
  }

  @Test
  void testQuery() {
    RowCell cell =
        RowCell.create(
            "info",
            ByteString.copyFromUtf8("name"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("QueryUser"));
    Row r1 = Row.create(ByteString.copyFromUtf8("q_1"), Collections.singletonList(cell));

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Collections.singletonList(r1).iterator());

    Query customQuery = Query.create("customers").limit(10);
    when(this.dataClient.readRows(customQuery)).thenReturn(stream);

    List<TestCustomer> customers = this.template.query(customQuery, TestCustomer.class);

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getName()).isEqualTo("QueryUser");
  }

  @Test
  void testCount() {
    Row r1 = Row.create(ByteString.copyFromUtf8("c1"), Collections.emptyList());
    Row r2 = Row.create(ByteString.copyFromUtf8("c2"), Collections.emptyList());
    Row r3 = Row.create(ByteString.copyFromUtf8("c3"), Collections.emptyList());

    @SuppressWarnings("unchecked")
    ServerStream<Row> stream = mock(ServerStream.class);
    when(stream.iterator()).thenReturn(Arrays.asList(r1, r2, r3).iterator());
    when(this.dataClient.readRows(any(Query.class))).thenReturn(stream);

    long count = this.template.count(TestCustomer.class);

    assertThat(count).isEqualTo(3);
  }

  @Test
  void testExecute_Callback() {
    String result = this.template.execute(client -> "custom-result");

    assertThat(result).isEqualTo("custom-result");
  }

  @Test
  void testExceptionTranslation_ReadRow() {
    ApiException apiException = mock(ApiException.class);
    when(apiException.getMessage()).thenReturn("Simulated RPC failure");
    when(this.dataClient.readRow(anyString(), any(ByteString.class))).thenThrow(apiException);

    assertThatThrownBy(() -> this.template.findById("c1", TestCustomer.class))
        .isInstanceOf(BigtableDataException.class)
        .isInstanceOf(DataAccessException.class)
        .hasCause(apiException)
        .hasMessageContaining("Simulated RPC failure");
  }

  @Test
  void testExceptionTranslation_MutateRow() {
    ApiException apiException = mock(ApiException.class);
    when(apiException.getMessage()).thenReturn("Simulated mutation failure");
    doThrow(apiException).when(this.dataClient).mutateRow(any(RowMutation.class));

    assertThatThrownBy(() -> this.template.save(new TestCustomer("c1", "A", "a@a.com")))
        .isInstanceOf(BigtableDataException.class)
        .isInstanceOf(DataAccessException.class)
        .hasCause(apiException)
        .hasMessageContaining("Simulated mutation failure");
  }

  @Test
  void testExceptionTranslation_BulkMutateRows() {
    ApiException apiException = mock(ApiException.class);
    when(apiException.getMessage()).thenReturn("Simulated bulk failure");
    doThrow(apiException).when(this.dataClient).bulkMutateRows(any(BulkMutation.class));

    assertThatThrownBy(
            () ->
                this.template.saveAll(
                    Collections.singletonList(new TestCustomer("c1", "A", "a@a.com"))))
        .isInstanceOf(BigtableDataException.class)
        .isInstanceOf(DataAccessException.class)
        .hasCause(apiException)
        .hasMessageContaining("Simulated bulk failure");
  }

  @Test
  void testExceptionTranslation_ReadRows() {
    ApiException apiException = mock(ApiException.class);
    when(apiException.getMessage()).thenReturn("Simulated stream failure");
    when(this.dataClient.readRows(any(Query.class))).thenThrow(apiException);

    assertThatThrownBy(() -> this.template.findAll(TestCustomer.class))
        .isInstanceOf(BigtableDataException.class)
        .isInstanceOf(DataAccessException.class)
        .hasCause(apiException)
        .hasMessageContaining("Simulated stream failure");
  }

  @Test
  void testExceptionTranslation_Execute() {
    assertThatThrownBy(
            () ->
                this.template.execute(
                    client -> {
                      throw new RuntimeException("Callback error");
                    }))
        .isInstanceOf(BigtableDataException.class)
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Callback error");
  }

  @BigtableTable(name = "customers")
  static class TestCustomer {

    @RowKey
    private String id;

    @Column(family = "info", qualifier = "name")
    private String name;

    @Column(family = "info", qualifier = "email")
    private String email;

    public TestCustomer() {}

    public TestCustomer(String id, String name, String email) {
      this.id = id;
      this.name = name;
      this.email = email;
    }

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return this.email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TestCustomer that = (TestCustomer) o;
      return Objects.equals(this.id, that.id)
          && Objects.equals(this.name, that.name)
          && Objects.equals(this.email, that.email);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.id, this.name, this.email);
    }
  }

  @BigtableTable(name = "orders", rowKeyDelimiter = "#")
  static class TestOrder {

    @RowKey(order = 0)
    private String customerId;

    @RowKey(order = 1)
    private String orderId;

    @Column(family = "details", qualifier = "amount")
    private Long amount;

    public TestOrder() {}

    public TestOrder(String customerId, String orderId, Long amount) {
      this.customerId = customerId;
      this.orderId = orderId;
      this.amount = amount;
    }

    public String getCustomerId() {
      return this.customerId;
    }

    public void setCustomerId(String customerId) {
      this.customerId = customerId;
    }

    public String getOrderId() {
      return this.orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    public Long getAmount() {
      return this.amount;
    }

    public void setAmount(Long amount) {
      this.amount = amount;
    }
  }
}

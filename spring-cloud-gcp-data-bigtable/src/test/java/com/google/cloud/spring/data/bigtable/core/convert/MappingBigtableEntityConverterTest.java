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

package com.google.cloud.spring.data.bigtable.core.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableDataException;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.DynamicColumns;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MappingBigtableEntityConverter}.
 */
class MappingBigtableEntityConverterTest {

  private BigtableMappingContext mappingContext;
  private MappingBigtableEntityConverter converter;

  @BeforeEach
  void setUp() {
    this.mappingContext = new BigtableMappingContext();
    this.converter = new MappingBigtableEntityConverter(this.mappingContext);
  }

  @Test
  void testSingleRowKeySerializationAndDeserialization() {
    UserEntity user = new UserEntity("user123", "alice@example.com", 28);
    ByteString key = this.converter.toRowKey(user);
    assertThat(key.toStringUtf8()).isEqualTo("user123");

    RowCell emailCell =
        RowCell.create(
            "user_info",
            ByteString.copyFromUtf8("email"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("alice@example.com"));
    RowCell ageCell =
        RowCell.create(
            "user_info",
            ByteString.copyFromUtf8("age"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("28"));

    Row row = Row.create(key, Arrays.asList(emailCell, ageCell));
    UserEntity readUser = this.converter.read(UserEntity.class, row);

    assertThat(readUser).isNotNull();
    assertThat(readUser.getId()).isEqualTo("user123");
    assertThat(readUser.getEmail()).isEqualTo("alice@example.com");
    assertThat(readUser.getAge()).isEqualTo(28);
  }

  @Test
  void testCompositeRowKeyDelimiterFormattingAndSplitting() {
    OrderEntity order = new OrderEntity("cust_42", "ord_999", 199.99, true);
    ByteString key = this.converter.toRowKey(order);
    assertThat(key.toStringUtf8()).isEqualTo("cust_42#ord_999");

    RowCell amountCell =
        RowCell.create(
            "order_details",
            ByteString.copyFromUtf8("amount"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("199.99"));
    RowCell activeCell =
        RowCell.create(
            "order_details",
            ByteString.copyFromUtf8("active"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("true"));

    Row row = Row.create(key, Arrays.asList(amountCell, activeCell));
    OrderEntity readOrder = this.converter.read(OrderEntity.class, row);

    assertThat(readOrder).isNotNull();
    assertThat(readOrder.getCustomerId()).isEqualTo("cust_42");
    assertThat(readOrder.getOrderId()).isEqualTo("ord_999");
    assertThat(readOrder.getAmount()).isEqualTo(199.99);
    assertThat(readOrder.isActive()).isTrue();
  }

  @Test
  void testCompositeRowKeyCustomDelimiter() {
    CustomDelimiterEntity entity = new CustomDelimiterEntity("regionA", "zoneB", "data");
    ByteString key = this.converter.toRowKey(entity);
    assertThat(key.toStringUtf8()).isEqualTo("regionA:zoneB");

    RowCell cell =
        RowCell.create(
            "cf",
            ByteString.copyFromUtf8("payload"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("data"));
    Row row = Row.create(key, Collections.singletonList(cell));
    CustomDelimiterEntity readEntity = this.converter.read(CustomDelimiterEntity.class, row);

    assertThat(readEntity.getRegion()).isEqualTo("regionA");
    assertThat(readEntity.getZone()).isEqualTo("zoneB");
    assertThat(readEntity.getPayload()).isEqualTo("data");
  }

  @Test
  void testCompositeRowKeyDelimiterCollisionThrowsException() {
    OrderEntity collisionOrder = new OrderEntity("cust#42", "ord_999", 50.0, true);
    assertThatThrownBy(() -> this.converter.toRowKey(collisionOrder))
        .isInstanceOf(BigtableDataException.class)
        .hasMessageContaining("delimiter collision");
  }

  @Test
  void testCompositeRowKeyTokenMismatchThrowsException() {
    ByteString invalidKey = ByteString.copyFromUtf8("only_one_part");
    Row row = Row.create(invalidKey, Collections.emptyList());

    assertThatThrownBy(() -> this.converter.read(OrderEntity.class, row))
        .isInstanceOf(BigtableDataException.class)
        .hasMessageContaining("expects 2 components");
  }

  @Test
  void testColumnSerializationAndDeserialization() {
    CustomQualifierEntity entity = new CustomQualifierEntity("key1", "val1", "val2");
    RowMutation mutation = RowMutation.create("custom_table", "key1");
    this.converter.write(entity, mutation);

    RowCell cell1 =
        RowCell.create(
            "data_cf",
            ByteString.copyFromUtf8("renamed_col"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("val1"));
    RowCell cell2 =
        RowCell.create(
            "data_cf",
            ByteString.copyFromUtf8("defaultCol"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("val2"));

    Row row = Row.create(ByteString.copyFromUtf8("key1"), Arrays.asList(cell1, cell2));
    CustomQualifierEntity read = this.converter.read(CustomQualifierEntity.class, row);

    assertThat(read.getId()).isEqualTo("key1");
    assertThat(read.getSpecialCol()).isEqualTo("val1");
    assertThat(read.getDefaultCol()).isEqualTo("val2");
  }

  @Test
  void testDynamicColumnsSerializationAndDeserialization() {
    DynamicColumnsEntity entity = new DynamicColumnsEntity("device_01");
    Map<String, String> dynamicProps = new LinkedHashMap<>();
    dynamicProps.put("os_version", "14.2");
    dynamicProps.put("carrier", "Verizon");
    dynamicProps.put("battery", "85%");
    entity.setProperties(dynamicProps);

    RowMutation mutation = RowMutation.create("devices", "device_01");
    this.converter.write(entity, mutation);

    RowCell cell1 =
        RowCell.create(
            "metrics",
            ByteString.copyFromUtf8("os_version"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("14.2"));
    RowCell cell2 =
        RowCell.create(
            "metrics",
            ByteString.copyFromUtf8("carrier"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("Verizon"));
    RowCell cell3 =
        RowCell.create(
            "metrics",
            ByteString.copyFromUtf8("battery"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("85%"));

    Row row = Row.create(ByteString.copyFromUtf8("device_01"), Arrays.asList(cell1, cell2, cell3));
    DynamicColumnsEntity read = this.converter.read(DynamicColumnsEntity.class, row);

    assertThat(read.getId()).isEqualTo("device_01");
    assertThat(read.getProperties())
        .containsEntry("os_version", "14.2")
        .containsEntry("carrier", "Verizon")
        .containsEntry("battery", "85%");
  }

  @Test
  void testReverseChronologicalLastWriteWinsDeduplication() {
    RowCell newestCell =
        RowCell.create(
            "user_info",
            ByteString.copyFromUtf8("email"),
            3000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("latest@example.com"));
    RowCell middleCell =
        RowCell.create(
            "user_info",
            ByteString.copyFromUtf8("email"),
            2000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("middle@example.com"));
    RowCell oldestCell =
        RowCell.create(
            "user_info",
            ByteString.copyFromUtf8("email"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("oldest@example.com"));

    Row row =
        Row.create(
            ByteString.copyFromUtf8("user_lww"),
            Arrays.asList(newestCell, middleCell, oldestCell));
    UserEntity user = this.converter.read(UserEntity.class, row);

    assertThat(user.getId()).isEqualTo("user_lww");
    assertThat(user.getEmail()).isEqualTo("latest@example.com");
  }

  @Test
  void testDynamicColumnsLastWriteWinsDeduplication() {
    RowCell dynNewest =
        RowCell.create(
            "metrics",
            ByteString.copyFromUtf8("status"),
            2000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("ONLINE"));
    RowCell dynOldest =
        RowCell.create(
            "metrics",
            ByteString.copyFromUtf8("status"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("OFFLINE"));

    Row row =
        Row.create(
            ByteString.copyFromUtf8("device_lww"),
            Arrays.asList(dynNewest, dynOldest));
    DynamicColumnsEntity read = this.converter.read(DynamicColumnsEntity.class, row);

    assertThat(read.getProperties()).containsEntry("status", "ONLINE");
  }

  @Test
  void testCustomTypeConversions() {
    UUID uuid = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-25T21:00:00Z");
    byte[] rawBytes = "sample-bytes".getBytes(StandardCharsets.UTF_8);
    ByteString bs = ByteString.copyFromUtf8("proto-bytes");

    TypesEntity entity = new TypesEntity(uuid, now, rawBytes, bs, 42L);
    RowMutation mutation = RowMutation.create("types_table", uuid.toString());
    this.converter.write(entity, mutation);

    RowCell cell1 =
        RowCell.create(
            "cf",
            ByteString.copyFromUtf8("timestamp"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("2026-08-25T21:00:00Z"));
    RowCell cell2 =
        RowCell.create(
            "cf",
            ByteString.copyFromUtf8("rawBytes"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFrom(rawBytes));
    RowCell cell3 =
        RowCell.create(
            "cf",
            ByteString.copyFromUtf8("protoBytes"),
            1000L,
            Collections.emptyList(),
            bs);
    RowCell cell4 =
        RowCell.create(
            "cf",
            ByteString.copyFromUtf8("counter"),
            1000L,
            Collections.emptyList(),
            ByteString.copyFromUtf8("42"));

    Row row =
        Row.create(
            ByteString.copyFromUtf8(uuid.toString()),
            Arrays.asList(cell1, cell2, cell3, cell4));
    TypesEntity read = this.converter.read(TypesEntity.class, row);

    assertThat(read.getId()).isEqualTo(uuid);
    assertThat(read.getTimestamp()).isEqualTo(now);
    assertThat(read.getRawBytes()).isEqualTo(rawBytes);
    assertThat(read.getProtoBytes()).isEqualTo(bs);
    assertThat(read.getCounter()).isEqualTo(42L);
  }

  @Test
  void testEncodeId() {
    ByteString strId = this.converter.encodeId(UserEntity.class, "user_alpha");
    assertThat(strId.toStringUtf8()).isEqualTo("user_alpha");

    ByteString byteStringId =
        this.converter.encodeId(UserEntity.class, ByteString.copyFromUtf8("user_beta"));
    assertThat(byteStringId.toStringUtf8()).isEqualTo("user_beta");

    ByteString arrayId =
        this.converter.encodeId(OrderEntity.class, new Object[] {"cust_1", "ord_2"});
    assertThat(arrayId.toStringUtf8()).isEqualTo("cust_1#ord_2");

    ByteString listId =
        this.converter.encodeId(OrderEntity.class, Arrays.asList("cust_3", "ord_4"));
    assertThat(listId.toStringUtf8()).isEqualTo("cust_3#ord_4");

    assertThatThrownBy(
            () ->
                this.converter.encodeId(
                    OrderEntity.class, new Object[] {"cust_1"}))
        .isInstanceOf(BigtableDataException.class)
        .hasMessageContaining("does not match row key component count");
  }

  @Test
  void testNullRowReturnsNull() {
    assertThat(this.converter.read(UserEntity.class, null)).isNull();
  }

  @Test
  void testNullRowKeyThrowsException() {
    UserEntity user = new UserEntity(null, "null@key.com", 20);
    assertThatThrownBy(() -> this.converter.toRowKey(user))
        .isInstanceOf(BigtableDataException.class)
        .hasMessageContaining("cannot be null");
  }

  @Test
  void testCreateRowMutation() {
    UserEntity user = new UserEntity("usr_77", "usr77@test.com", 25);
    RowMutation mutation = this.converter.createRowMutation(user);

    assertThat(mutation).isNotNull();
  }

  // --- Test Entities ---

  @BigtableTable(name = "users")
  public static class UserEntity {
    @RowKey private String id;
    @Column(family = "user_info") private String email;
    @Column(family = "user_info") private Integer age;

    public UserEntity() {}

    public UserEntity(String id, String email, Integer age) {
      this.id = id;
      this.email = email;
      this.age = age;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
  }

  @BigtableTable(name = "orders", rowKeyDelimiter = "#")
  public static class OrderEntity {
    @RowKey(order = 0) private String customerId;
    @RowKey(order = 1) private String orderId;
    @Column(family = "order_details") private Double amount;
    @Column(family = "order_details") private boolean active;

    public OrderEntity() {}

    public OrderEntity(String customerId, String orderId, Double amount, boolean active) {
      this.customerId = customerId;
      this.orderId = orderId;
      this.amount = amount;
      this.active = active;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
  }

  @BigtableTable(name = "custom_delim", rowKeyDelimiter = ":")
  public static class CustomDelimiterEntity {
    @RowKey(order = 0) private String region;
    @RowKey(order = 1) private String zone;
    @Column(family = "cf") private String payload;

    public CustomDelimiterEntity() {}

    public CustomDelimiterEntity(String region, String zone, String payload) {
      this.region = region;
      this.zone = zone;
      this.payload = payload;
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
  }

  @BigtableTable(name = "custom_table")
  public static class CustomQualifierEntity {
    @RowKey private String id;
    @Column(family = "data_cf", qualifier = "renamed_col") private String specialCol;
    @Column(family = "data_cf") private String defaultCol;

    public CustomQualifierEntity() {}

    public CustomQualifierEntity(String id, String specialCol, String defaultCol) {
      this.id = id;
      this.specialCol = specialCol;
      this.defaultCol = defaultCol;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSpecialCol() { return specialCol; }
    public void setSpecialCol(String specialCol) { this.specialCol = specialCol; }
    public String getDefaultCol() { return defaultCol; }
    public void setDefaultCol(String defaultCol) { this.defaultCol = defaultCol; }
  }

  @BigtableTable(name = "devices")
  public static class DynamicColumnsEntity {
    @RowKey private String id;
    @DynamicColumns(family = "metrics") private Map<String, String> properties;

    public DynamicColumnsEntity() {}

    public DynamicColumnsEntity(String id) {
      this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
  }

  @BigtableTable(name = "types_table")
  public static class TypesEntity {
    @RowKey private UUID id;
    @Column(family = "cf") private Instant timestamp;
    @Column(family = "cf") private byte[] rawBytes;
    @Column(family = "cf") private ByteString protoBytes;
    @Column(family = "cf") private Long counter;

    public TypesEntity() {}

    public TypesEntity(UUID id, Instant timestamp, byte[] rawBytes, ByteString protoBytes, Long counter) {
      this.id = id;
      this.timestamp = timestamp;
      this.rawBytes = rawBytes;
      this.protoBytes = protoBytes;
      this.counter = counter;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public byte[] getRawBytes() { return rawBytes; }
    public void setRawBytes(byte[] rawBytes) { this.rawBytes = rawBytes; }
    public ByteString getProtoBytes() { return protoBytes; }
    public void setProtoBytes(ByteString protoBytes) { this.protoBytes = protoBytes; }
    public Long getCounter() { return counter; }
    public void setCounter(Long counter) { this.counter = counter; }
  }
}

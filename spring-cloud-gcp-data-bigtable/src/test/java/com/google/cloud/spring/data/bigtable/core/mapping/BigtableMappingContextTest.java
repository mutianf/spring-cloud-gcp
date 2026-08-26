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

package com.google.cloud.spring.data.bigtable.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.FieldNamingStrategy;

/**
 * Unit tests for {@link BigtableMappingContext}, {@link BigtablePersistentEntity},
 * and {@link BigtablePersistentProperty}.
 */
class BigtableMappingContextTest {

  private BigtableMappingContext mappingContext;

  @BeforeEach
  void setUp() {
    this.mappingContext = new BigtableMappingContext();
  }

  @Test
  void testSingleKeyEntity() {
    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(SingleKeyUser.class);

    assertThat(entity).isNotNull();
    assertThat(entity.getTableName()).isEqualTo("users");
    assertThat(entity.getRowKeyDelimiter()).isEqualTo("#");
    assertThat(entity.hasCompositeRowKey()).isFalse();
    assertThat(entity.hasDynamicColumns()).isFalse();

    List<BigtablePersistentProperty> rowKeys = entity.getRowKeyProperties();
    assertThat(rowKeys).hasSize(1);
    BigtablePersistentProperty rowKeyProp = rowKeys.get(0);
    assertThat(rowKeyProp.getName()).isEqualTo("userId");
    assertThat(rowKeyProp.isRowKey()).isTrue();
    assertThat(rowKeyProp.getRowKeyOrder()).isEqualTo(0);
    assertThat(entity.getIdProperty()).isSameAs(rowKeyProp);

    List<BigtablePersistentProperty> columns = entity.getColumnProperties();
    assertThat(columns).hasSize(2);

    BigtablePersistentProperty nameProp = entity.getPersistentProperty("fullName");
    assertThat(nameProp).isNotNull();
    assertThat(nameProp.isColumn()).isTrue();
    assertThat(nameProp.getFamilyName()).isEqualTo("cf1");
    assertThat(nameProp.getColumnQualifier()).isEqualTo("name");

    BigtablePersistentProperty emailProp = entity.getPersistentProperty("email");
    assertThat(emailProp).isNotNull();
    assertThat(emailProp.isColumn()).isTrue();
    assertThat(emailProp.getFamilyName()).isEqualTo("cf1");
    assertThat(emailProp.getColumnQualifier()).isEqualTo("email");
  }

  @Test
  void testCompositeKeyOrdering() {
    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(CompositeKeyOrder.class);

    assertThat(entity).isNotNull();
    assertThat(entity.getTableName()).isEqualTo("orders");
    assertThat(entity.getRowKeyDelimiter()).isEqualTo(":");
    assertThat(entity.hasCompositeRowKey()).isTrue();

    List<BigtablePersistentProperty> rowKeys = entity.getRowKeyProperties();
    assertThat(rowKeys).hasSize(3);
    assertThat(rowKeys.get(0).getName()).isEqualTo("region");
    assertThat(rowKeys.get(0).getRowKeyOrder()).isEqualTo(0);
    assertThat(rowKeys.get(1).getName()).isEqualTo("customerId");
    assertThat(rowKeys.get(1).getRowKeyOrder()).isEqualTo(1);
    assertThat(rowKeys.get(2).getName()).isEqualTo("orderId");
    assertThat(rowKeys.get(2).getRowKeyOrder()).isEqualTo(2);
  }

  @Test
  void testDynamicColumnsDetection() {
    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(DynamicColumnsEntity.class);

    assertThat(entity).isNotNull();
    assertThat(entity.hasDynamicColumns()).isTrue();
    BigtablePersistentProperty dynamicProp = entity.getDynamicColumnsProperty();
    assertThat(dynamicProp).isNotNull();
    assertThat(dynamicProp.getName()).isEqualTo("attributes");
    assertThat(dynamicProp.isDynamicColumns()).isTrue();
    assertThat(dynamicProp.getFamilyName()).isEqualTo("dyn_cf");
  }

  @Test
  void testSpelTableNameResolution() {
    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.getBeanFactory().registerSingleton("suffix", "prod");
    applicationContext.refresh();

    this.mappingContext.setApplicationContext(applicationContext);
    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(SpelTableEntity.class);

    assertThat(entity).isNotNull();
    assertThat(entity.getTableName()).isEqualTo("table_prod");
  }

  @Test
  void testPropertyIterationHandlers() {
    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(SingleKeyUser.class);

    List<String> visitedColumns = new ArrayList<>();
    entity.doWithColumnProperties(prop -> visitedColumns.add(prop.getName()));
    assertThat(visitedColumns).containsExactlyInAnyOrder("fullName", "email");

    List<String> visitedKeys = new ArrayList<>();
    entity.doWithRowKeyProperties(prop -> visitedKeys.add(prop.getName()));
    assertThat(visitedKeys).containsExactly("userId");
  }

  @Test
  void testFieldNamingStrategy() {
    FieldNamingStrategy customStrategy = property -> property.getName().toUpperCase();
    this.mappingContext.setFieldNamingStrategy(customStrategy);
    assertThat(this.mappingContext.getFieldNamingStrategy()).isSameAs(customStrategy);

    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntity(SingleKeyUser.class);
    BigtablePersistentProperty emailProp = entity.getPersistentProperty("email");
    assertThat(emailProp).isNotNull();
    assertThat(emailProp.getColumnQualifier()).isEqualTo("EMAIL");
  }

  @Test
  void testGetPersistentEntityOrFail() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntityOrFail(null))
        .isInstanceOf(IllegalArgumentException.class);

    BigtablePersistentEntity<?> entity =
        this.mappingContext.getPersistentEntityOrFail(SingleKeyUser.class);
    assertThat(entity).isNotNull();
  }

  @Test
  void testMissingTableAnnotationThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithoutTable.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "missing required @BigtableTable"));
  }

  @Test
  void testEmptyTableNameThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithEmptyTable.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "Table name for entity"));
  }

  @Test
  void testMissingRowKeyThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithoutRowKey.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "must define at least one @RowKey"));
  }

  @Test
  void testDuplicateRowKeyOrderThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithDuplicateRowKeyOrder.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "Duplicate @RowKey order"));
  }

  @Test
  void testNonConsecutiveRowKeyOrderThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithNonConsecutiveRowKeyOrder.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "consecutive orders"));
  }

  @Test
  void testEmptyRowKeyDelimiterThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithEmptyDelimiter.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "Row key delimiter"));
  }

  @Test
  void testDuplicateColumnCoordinatesThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithDuplicateColumnCoordinates.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "Duplicate column mapping"));
  }

  @Test
  void testMultipleDynamicColumnsThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithMultipleDynamicColumns.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "cannot have more than one @DynamicColumns"));
  }

  @Test
  void testDynamicColumnsOnNonMapThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithInvalidDynamicColumnsType.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "must be of Map type"));
  }

  @Test
  void testFamilyCollisionThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithFamilyCollision.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "cannot be used for both @DynamicColumns and @Column"));
  }

  @Test
  void testPropertyWithBothRowKeyAndColumnThrowsException() {
    assertThatThrownBy(() -> this.mappingContext.getPersistentEntity(EntityWithRowKeyAndColumn.class))
        .satisfies(t -> assertMappingExceptionContainsMessage(t, "cannot be annotated with both @RowKey and @Column"));
  }

  private void assertMappingExceptionContainsMessage(Throwable throwable, String messageSnippet) {
    Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
    assertThat(cause).isInstanceOf(MappingException.class);
    assertThat(cause.getMessage()).contains(messageSnippet);
  }

  // --- Test Entity Models ---

  @BigtableTable(name = "users")
  private static class SingleKeyUser {
    @RowKey
    private String userId;

    @Column(family = "cf1", qualifier = "name")
    private String fullName;

    @Column(family = "cf1")
    private String email;
  }

  @BigtableTable(name = "orders", rowKeyDelimiter = ":")
  private static class CompositeKeyOrder {
    @RowKey(order = 1)
    private String customerId;

    @RowKey(order = 2)
    private String orderId;

    @RowKey(order = 0)
    private String region;

    @Column(family = "cf")
    private String amount;
  }

  @BigtableTable(name = "dynamic_table")
  private static class DynamicColumnsEntity {
    @RowKey
    private String id;

    @DynamicColumns(family = "dyn_cf")
    private Map<String, String> attributes;
  }

  @BigtableTable(name = "#{'table_'.concat(suffix)}")
  private static class SpelTableEntity {
    @RowKey
    private String id;
  }

  private static class EntityWithoutTable {
    @RowKey
    private String id;
  }

  @BigtableTable(name = "")
  private static class EntityWithEmptyTable {
    @RowKey
    private String id;
  }

  @BigtableTable(name = "no_key")
  private static class EntityWithoutRowKey {
    @Column(family = "cf")
    private String data;
  }

  @BigtableTable(name = "dup_keys")
  private static class EntityWithDuplicateRowKeyOrder {
    @RowKey(order = 0)
    private String key1;

    @RowKey(order = 0)
    private String key2;
  }

  @BigtableTable(name = "gap_keys")
  private static class EntityWithNonConsecutiveRowKeyOrder {
    @RowKey(order = 0)
    private String key1;

    @RowKey(order = 2)
    private String key2;
  }

  @BigtableTable(name = "empty_delim", rowKeyDelimiter = "")
  private static class EntityWithEmptyDelimiter {
    @RowKey(order = 0)
    private String key1;

    @RowKey(order = 1)
    private String key2;
  }

  @BigtableTable(name = "dup_cols")
  private static class EntityWithDuplicateColumnCoordinates {
    @RowKey
    private String id;

    @Column(family = "cf", qualifier = "col1")
    private String first;

    @Column(family = "cf", qualifier = "col1")
    private String second;
  }

  @BigtableTable(name = "multi_dyn")
  private static class EntityWithMultipleDynamicColumns {
    @RowKey
    private String id;

    @DynamicColumns(family = "dyn1")
    private Map<String, String> dyn1;

    @DynamicColumns(family = "dyn2")
    private Map<String, String> dyn2;
  }

  @BigtableTable(name = "invalid_dyn_type")
  private static class EntityWithInvalidDynamicColumnsType {
    @RowKey
    private String id;

    @DynamicColumns(family = "dyn")
    private String invalid;
  }

  @BigtableTable(name = "family_collision")
  private static class EntityWithFamilyCollision {
    @RowKey
    private String id;

    @Column(family = "shared_family", qualifier = "c1")
    private String staticCol;

    @DynamicColumns(family = "shared_family")
    private Map<String, String> dynamicCols;
  }

  @BigtableTable(name = "rowkey_and_col")
  private static class EntityWithRowKeyAndColumn {
    @RowKey
    @Column(family = "cf")
    private String both;
  }
}

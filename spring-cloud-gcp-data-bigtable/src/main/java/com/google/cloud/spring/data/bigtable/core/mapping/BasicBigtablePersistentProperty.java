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

import java.util.Map;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

/**
 * Basic implementation of {@link BigtablePersistentProperty}.
 */
public class BasicBigtablePersistentProperty
    extends AnnotationBasedPersistentProperty<BigtablePersistentProperty>
    implements BigtablePersistentProperty {

  private final FieldNamingStrategy fieldNamingStrategy;

  /**
   * Creates a new persistent property, validating its Bigtable mapping annotations.
   *
   * @param property the property to create the persistent property for
   * @param owner the entity declaring the property
   * @param simpleTypeHolder the type holder deciding which types are simple
   * @param fieldNamingStrategy the strategy used to derive a column qualifier from the property
   *     name, or null to use {@link PropertyNameFieldNamingStrategy#INSTANCE}
   * @throws MappingException if the property's mapping annotations are inconsistent
   */
  public BasicBigtablePersistentProperty(
      Property property,
      PersistentEntity<?, BigtablePersistentProperty> owner,
      SimpleTypeHolder simpleTypeHolder,
      FieldNamingStrategy fieldNamingStrategy) {
    super(property, owner, simpleTypeHolder);
    this.fieldNamingStrategy =
        (fieldNamingStrategy != null)
            ? fieldNamingStrategy
            : PropertyNameFieldNamingStrategy.INSTANCE;
    validateProperty();
  }

  /**
   * Creates a new persistent property using {@link PropertyNameFieldNamingStrategy#INSTANCE}.
   *
   * @param property the property to create the persistent property for
   * @param owner the entity declaring the property
   * @param simpleTypeHolder the type holder deciding which types are simple
   * @throws MappingException if the property's mapping annotations are inconsistent
   */
  public BasicBigtablePersistentProperty(
      Property property,
      PersistentEntity<?, BigtablePersistentProperty> owner,
      SimpleTypeHolder simpleTypeHolder) {
    this(property, owner, simpleTypeHolder, PropertyNameFieldNamingStrategy.INSTANCE);
  }

  private void validateProperty() {
    if (isRowKey()) {
      checkMapping(
          !isColumn() && !isDynamicColumns(),
          "%s cannot combine @RowKey (or @Id) with @Column or @DynamicColumns.",
          describe());
      return;
    }

    if (isColumn()) {
      checkMapping(
          !isDynamicColumns(),
          "%s cannot be annotated with both @Column and @DynamicColumns.",
          describe());
      Column column = findAnnotation(Column.class);
      checkMapping(
          StringUtils.hasText(column.family()), "Column family for %s cannot be empty.", describe());
      checkMapping(
          StringUtils.hasText(column.qualifier()),
          "Column qualifier for %s cannot be empty.",
          describe());
      return;
    }

    if (isDynamicColumns()) {
      checkMapping(
          StringUtils.hasText(findAnnotation(DynamicColumns.class).family()),
          "DynamicColumns family for %s cannot be empty.",
          describe());
      checkMapping(
          Map.class.isAssignableFrom(getType()),
          "%s annotated with @DynamicColumns must be of Map type, but found %s.",
          describe(),
          getType().getName());
    }
  }

  private static void checkMapping(boolean expression, String message, Object... args) {
    if (!expression) {
      throw new MappingException(String.format(message, args));
    }
  }

  private String describe() {
    return "Property '" + getName() + "' in " + getOwner().getType().getSimpleName();
  }

  /**
   * {@inheritDoc}
   *
   * <p>A property is a row key component if it carries {@link RowKey} or any other
   * {@link org.springframework.data.annotation.Id} meta-annotated annotation. An entity may
   * declare several such properties; they form a composite key ordered by {@link
   * #getRowKeyOrder()}.
   */
  @Override
  public boolean isRowKey() {
    return isIdProperty() || isAnnotationPresent(RowKey.class);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns 0 for a property annotated with plain {@code @Id} rather than {@link RowKey},
   * which is the correct order for a single-component key.
   */
  @Override
  public int getRowKeyOrder() {
    RowKey rowKey = findAnnotation(RowKey.class);
    return rowKey != null ? rowKey.order() : 0;
  }

  @Override
  public boolean isColumn() {
    return isAnnotationPresent(Column.class);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Reads {@link Column#family()} or {@link DynamicColumns#family()}, whichever is present.
   */
  @Override
  public String getFamilyName() {
    Column col = findAnnotation(Column.class);
    if (col != null) {
      return col.family();
    }
    DynamicColumns dynamic = findAnnotation(DynamicColumns.class);
    if (dynamic != null) {
      return dynamic.family();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@link Column#qualifier()} is mandatory and validated to be non-blank, so the configured
   * {@link FieldNamingStrategy} only applies to properties that carry no {@link Column}.
   */
  @Override
  public String getColumnQualifier() {
    Column col = findAnnotation(Column.class);
    if (col != null && StringUtils.hasText(col.qualifier())) {
      return col.qualifier();
    }
    return this.fieldNamingStrategy.getFieldName(this);
  }

  @Override
  public boolean isDynamicColumns() {
    return isAnnotationPresent(DynamicColumns.class);
  }

  @Override
  public boolean isMapped() {
    return isRowKey() || isColumn() || isDynamicColumns();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Bigtable entities have no associations, so this returns a self-referencing
   * {@link Association} to satisfy the Spring Data contract.
   */
  @Override
  protected Association<BigtablePersistentProperty> createAssociation() {
    return new Association<>(this, null);
  }
}

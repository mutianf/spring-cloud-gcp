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

  public BasicBigtablePersistentProperty(
      Property property,
      PersistentEntity<?, BigtablePersistentProperty> owner,
      SimpleTypeHolder simpleTypeHolder) {
    this(property, owner, simpleTypeHolder, PropertyNameFieldNamingStrategy.INSTANCE);
  }

  private void validateProperty() {
    if (isRowKey()) {
      if (isColumn() || isDynamicColumns()) {
        throw new MappingException(
            describe() + " cannot combine @RowKey (or @Id) with @Column or @DynamicColumns.");
      }
      return;
    }

    if (isColumn()) {
      if (isDynamicColumns()) {
        throw new MappingException(
            describe() + " cannot be annotated with both @Column and @DynamicColumns.");
      }
      Column col = findAnnotation(Column.class);
      if (col != null && !StringUtils.hasText(col.family())) {
        throw new MappingException("Column family for " + describe() + " cannot be empty.");
      }
      if (col != null && !StringUtils.hasText(col.qualifier())) {
        throw new MappingException("Column qualifier for " + describe() + " cannot be empty.");
      }
      return;
    }

    if (isDynamicColumns()) {
      DynamicColumns dynamic = findAnnotation(DynamicColumns.class);
      if (dynamic != null && !StringUtils.hasText(dynamic.family())) {
        throw new MappingException("DynamicColumns family for " + describe() + " cannot be empty.");
      }
      if (!Map.class.isAssignableFrom(getType())) {
        throw new MappingException(
            describe() + " annotated with @DynamicColumns must be of Map type, but found "
                + getType().getName());
      }
    }
  }

  private String describe() {
    return "Property '" + getName() + "' in " + getOwner().getType().getSimpleName();
  }

  @Override
  public boolean isRowKey() {
    return isIdProperty() || isAnnotationPresent(RowKey.class);
  }

  @Override
  public int getRowKeyOrder() {
    RowKey rowKey = findAnnotation(RowKey.class);
    return rowKey != null ? rowKey.order() : 0;
  }

  @Override
  public boolean isColumn() {
    return isAnnotationPresent(Column.class);
  }

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

  @Override
  protected Association<BigtablePersistentProperty> createAssociation() {
    return new Association<>(this, null);
  }
}

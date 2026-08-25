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
    boolean isRowKey = isRowKey();
    boolean isColumn = isColumn();
    boolean isDynamic = isDynamicColumns();

    if (isRowKey && isColumn) {
      throw new MappingException(
          "Property '" + getName() + "' in " + getOwner().getType().getSimpleName()
              + " cannot be annotated with both @RowKey and @Column.");
    }
    if (isRowKey && isDynamic) {
      throw new MappingException(
          "Property '" + getName() + "' in " + getOwner().getType().getSimpleName()
              + " cannot be annotated with both @RowKey and @DynamicColumns.");
    }
    if (isColumn && isDynamic) {
      throw new MappingException(
          "Property '" + getName() + "' in " + getOwner().getType().getSimpleName()
              + " cannot be annotated with both @Column and @DynamicColumns.");
    }
    if (isColumn) {
      Column col = findAnnotation(Column.class);
      if (col != null && !StringUtils.hasText(col.family())) {
        throw new MappingException(
            "Column family for property '" + getName() + "' in "
                + getOwner().getType().getSimpleName() + " cannot be empty.");
      }
    }
    if (isDynamic) {
      DynamicColumns dynamic = findAnnotation(DynamicColumns.class);
      if (dynamic != null && !StringUtils.hasText(dynamic.family())) {
        throw new MappingException(
            "DynamicColumns family for property '" + getName() + "' in "
                + getOwner().getType().getSimpleName() + " cannot be empty.");
      }
      if (!Map.class.isAssignableFrom(getType())) {
        throw new MappingException(
            "Property '" + getName() + "' annotated with @DynamicColumns in "
                + getOwner().getType().getSimpleName() + " must be of Map type, but found "
                + getType().getName());
      }
    }
  }

  @Override
  public boolean isRowKey() {
    return isIdProperty() || isAnnotationPresent(RowKey.class);
  }

  @Override
  public int getRowKeyOrder() {
    RowKey rowKey = findAnnotation(RowKey.class);
    if (rowKey != null) {
      return rowKey.order() != 0 ? rowKey.order() : rowKey.value();
    }
    return 0;
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

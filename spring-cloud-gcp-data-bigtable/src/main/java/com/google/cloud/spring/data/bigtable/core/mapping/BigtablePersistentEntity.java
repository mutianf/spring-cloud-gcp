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

import java.util.List;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Cloud Bigtable persistent entity metadata interface.
 *
 * @param <T> the persistent entity type
 */
public interface BigtablePersistentEntity<T>
    extends MutablePersistentEntity<T, BigtablePersistentProperty>, ApplicationContextAware {

  /**
   * Returns the Cloud Bigtable table name for this entity.
   *
   * @return the table name
   */
  String getTableName();

  /**
   * Returns the row key delimiter for composite keys. Defaults to "#".
   *
   * @return the row key delimiter
   */
  String getRowKeyDelimiter();

  /**
   * Returns the ordered list of row key properties, sorted by their order index.
   *
   * @return ordered list of row key properties
   */
  List<BigtablePersistentProperty> getRowKeyProperties();

  /**
   * Returns true if this entity has a composite row key (more than one @RowKey property).
   *
   * @return true if composite row key
   */
  boolean hasCompositeRowKey();

  /**
   * Returns all properties annotated with {@link Column}.
   *
   * @return list of column properties
   */
  List<BigtablePersistentProperty> getColumnProperties();

  /**
   * Returns the property annotated with {@link DynamicColumns}, or null if not present.
   *
   * @return dynamic columns property or null
   */
  BigtablePersistentProperty getDynamicColumnsProperty();

  /**
   * Returns true if this entity has a {@link DynamicColumns} property.
   *
   * @return true if entity has dynamic columns
   */
  boolean hasDynamicColumns();

  /**
   * Applies the given {@link PropertyHandler} to all properties annotated with {@link Column}.
   *
   * @param handler the property handler
   */
  void doWithColumnProperties(PropertyHandler<BigtablePersistentProperty> handler);

  /**
   * Applies the given {@link PropertyHandler} to all row key properties.
   *
   * @param handler the property handler
   */
  void doWithRowKeyProperties(PropertyHandler<BigtablePersistentProperty> handler);
}

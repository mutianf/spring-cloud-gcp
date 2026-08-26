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

import org.springframework.data.mapping.PersistentProperty;

/**
 * Persistent property interface for Cloud Bigtable entity properties.
 */
public interface BigtablePersistentProperty extends PersistentProperty<BigtablePersistentProperty> {

  /**
   * Returns true if this property is annotated with {@link RowKey} or {@link org.springframework.data.annotation.Id}.
   *
   * @return true if row key property
   */
  boolean isRowKey();

  /**
   * Returns the order of this row key component within a composite key. Defaults to 0.
   *
   * @return the row key order
   */
  int getRowKeyOrder();

  /**
   * Returns true if this property is annotated with {@link Column}.
   *
   * @return true if column property
   */
  boolean isColumn();

  /**
   * Returns the column family name defined by {@link Column} or {@link DynamicColumns}.
   *
   * @return the column family name, or null if unmapped
   */
  String getFamilyName();

  /**
   * Returns the column qualifier name. Uses {@link Column#qualifier()} if provided,
   * otherwise defaults to the field naming strategy result (or property name).
   *
   * @return the column qualifier
   */
  String getColumnQualifier();

  /**
   * Returns true if this property is annotated with {@link DynamicColumns}.
   *
   * @return true if dynamic columns property
   */
  boolean isDynamicColumns();

  /**
   * Returns true if this property is mapped to Bigtable (RowKey, Column, or DynamicColumns).
   *
   * @return true if mapped
   */
  boolean isMapped();
}

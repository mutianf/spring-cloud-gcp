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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Persistent property implementation for Cloud Bigtable.
 */
public class BigtablePersistentPropertyImpl extends BasicBigtablePersistentProperty {

  /**
   * Creates a new persistent property.
   *
   * @param property the property to create the persistent property for
   * @param owner the entity declaring the property
   * @param simpleTypeHolder the type holder deciding which types are simple
   * @param fieldNamingStrategy the strategy used to derive a column qualifier from the property
   *     name
   */
  public BigtablePersistentPropertyImpl(
      Property property,
      PersistentEntity<?, BigtablePersistentProperty> owner,
      SimpleTypeHolder simpleTypeHolder,
      FieldNamingStrategy fieldNamingStrategy) {
    super(property, owner, simpleTypeHolder, fieldNamingStrategy);
  }

  /**
   * Creates a new persistent property using the default field naming strategy.
   *
   * @param property the property to create the persistent property for
   * @param owner the entity declaring the property
   * @param simpleTypeHolder the type holder deciding which types are simple
   */
  public BigtablePersistentPropertyImpl(
      Property property,
      PersistentEntity<?, BigtablePersistentProperty> owner,
      SimpleTypeHolder simpleTypeHolder) {
    super(property, owner, simpleTypeHolder);
  }
}

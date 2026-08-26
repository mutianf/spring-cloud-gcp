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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Mapping context for Cloud Bigtable that creates and caches persistent entities and properties.
 */
public class BigtableMappingContext
    extends AbstractMappingContext<BigtablePersistentEntity<?>, BigtablePersistentProperty>
    implements ApplicationContextAware {

  private FieldNamingStrategy fieldNamingStrategy = PropertyNameFieldNamingStrategy.INSTANCE;
  private ApplicationContext applicationContext;

  public BigtableMappingContext() {}

  /**
   * Sets the field naming strategy for column qualifiers.
   *
   * @param fieldNamingStrategy the naming strategy
   */
  public void setFieldNamingStrategy(@Nullable FieldNamingStrategy fieldNamingStrategy) {
    this.fieldNamingStrategy =
        (fieldNamingStrategy != null) ? fieldNamingStrategy : PropertyNameFieldNamingStrategy.INSTANCE;
  }

  /**
   * Returns the field naming strategy in use.
   *
   * @return the naming strategy
   */
  public FieldNamingStrategy getFieldNamingStrategy() {
    return this.fieldNamingStrategy;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  protected <T> BigtablePersistentEntity<T> createPersistentEntity(
      TypeInformation<T> typeInformation) {
    BasicBigtablePersistentEntity<T> entity = new BasicBigtablePersistentEntity<>(typeInformation);
    if (this.applicationContext != null) {
      entity.setApplicationContext(this.applicationContext);
    }
    return entity;
  }

  @Override
  protected BigtablePersistentProperty createPersistentProperty(
      Property property,
      BigtablePersistentEntity<?> owner,
      SimpleTypeHolder simpleTypeHolder) {
    return new BasicBigtablePersistentProperty(
        property, owner, simpleTypeHolder, this.fieldNamingStrategy);
  }

  /**
   * Returns the persistent entity for the given type, throwing an exception if not found.
   *
   * @param entityClass the entity class
   * @return the persistent entity
   * @throws BigtableDataException if entity cannot be resolved
   */
  public BigtablePersistentEntity<?> getPersistentEntityOrFail(Class<?> entityClass) {
    Assert.notNull(entityClass, "Entity class must not be null");
    BigtablePersistentEntity<?> entity = getPersistentEntity(entityClass);
    if (entity == null) {
      throw new BigtableDataException(
          "The provided entity class cannot be converted to a Bigtable Persistent Entity: "
              + entityClass.getName());
    }
    return entity;
  }
}

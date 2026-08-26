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

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 * Holds entity and ID type information about a Bigtable persistent entity.
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 */
public class BigtablePersistentEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {

  private final BigtablePersistentEntity<T> persistentEntity;

  public BigtablePersistentEntityInformation(BigtablePersistentEntity<T> persistentEntity) {
    super(persistentEntity.getType());
    this.persistentEntity = persistentEntity;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ID getId(T entity) {
    if (entity == null) {
      return null;
    }
    BigtablePersistentProperty idProperty = this.persistentEntity.getIdProperty();
    if (idProperty != null) {
      PersistentPropertyAccessor<?> accessor = this.persistentEntity.getPropertyAccessor(entity);
      if (accessor != null) {
        return (ID) accessor.getProperty(idProperty);
      }
      try {
        if (idProperty.getField() != null) {
          idProperty.getField().setAccessible(true);
          return (ID) idProperty.getField().get(entity);
        } else if (idProperty.getGetter() != null) {
          idProperty.getGetter().setAccessible(true);
          return (ID) idProperty.getGetter().invoke(entity);
        }
      } catch (Exception e) {
        throw new BigtableDataException("Failed to access ID property for entity " + entity.getClass().getName(), e);
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<ID> getIdType() {
    if (this.persistentEntity.getIdProperty() != null) {
      return (Class<ID>) this.persistentEntity.getIdProperty().getType();
    }
    return (Class<ID>) Object.class;
  }
}

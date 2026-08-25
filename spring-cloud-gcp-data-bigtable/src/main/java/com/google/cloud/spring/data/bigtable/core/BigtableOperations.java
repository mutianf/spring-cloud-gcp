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

package com.google.cloud.spring.data.bigtable.core;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import java.util.List;

/**
 * Interface that specifies a basic set of Cloud Bigtable operations on domain entities.
 */
public interface BigtableOperations {

  /**
   * Returns the mapping context used by this operations abstraction.
   *
   * @return the mapping context
   */
  BigtableMappingContext getMappingContext();

  /**
   * Returns the entity converter used by this operations abstraction.
   *
   * @return the entity converter
   */
  BigtableEntityConverter getEntityConverter();

  /**
   * Returns the underlying {@link BigtableDataClient}.
   *
   * @return the Bigtable data client
   */
  BigtableDataClient getDataClient();

  /**
   * Executes the given callback action using the underlying {@link BigtableDataClient}.
   *
   * @param action callback action
   * @param <R> the return type
   * @return the result returned by the callback
   */
  <R> R execute(BigtableDataClientCallback<R> action);

  /**
   * Retrieves an entity by its identifier.
   *
   * @param id the entity identifier
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return the entity, or {@code null} if not found
   */
  <T> T findById(Object id, Class<T> entityClass);

  /**
   * Returns whether an entity with the given identifier exists.
   *
   * @param id the entity identifier
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return {@code true} if an entity exists, {@code false} otherwise
   */
  <T> boolean existsById(Object id, Class<T> entityClass);

  /**
   * Returns all instances of the type with the given IDs.
   *
   * @param ids the entity identifiers
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return list of matching entities
   */
  <T> List<T> findAllById(Iterable<?> ids, Class<T> entityClass);

  /**
   * Returns all instances of the given entity type.
   *
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return list of all entities in the table
   */
  <T> List<T> findAll(Class<T> entityClass);

  /**
   * Finds entities whose row key starts with the given prefix.
   *
   * @param prefix the row key prefix
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return list of matching entities
   */
  <T> List<T> findByRowKeyPrefix(String prefix, Class<T> entityClass);

  /**
   * Finds entities within the specified row key range [startKey, endKey).
   *
   * @param startKey inclusive start row key, or null for unbounded start
   * @param endKey exclusive end row key, or null for unbounded end
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return list of matching entities
   */
  <T> List<T> findByRowKeyRange(String startKey, String endKey, Class<T> entityClass);

  /**
   * Executes a custom {@link Query} and maps the resulting rows to entities of the given type.
   *
   * @param query the query to execute
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return list of entities matching the query
   */
  <T> List<T> query(Query query, Class<T> entityClass);

  /**
   * Saves a given entity.
   *
   * @param entity the entity to save
   * @param <T> the entity type
   * @return the saved entity
   */
  <T> T save(T entity);

  /**
   * Saves all given entities.
   *
   * @param entities the entities to save
   * @param <T> the entity type
   * @return the saved entities
   */
  <T> Iterable<T> saveAll(Iterable<T> entities);

  /**
   * Deletes a given entity.
   *
   * @param entity the entity to delete
   * @param <T> the entity type
   */
  <T> void delete(T entity);

  /**
   * Deletes the entity with the given id.
   *
   * @param id the entity identifier
   * @param entityClass the entity class
   * @param <T> the entity type
   */
  <T> void deleteById(Object id, Class<T> entityClass);

  /**
   * Deletes all entities with the given IDs.
   *
   * @param ids the entity identifiers
   * @param entityClass the entity class
   * @param <T> the entity type
   */
  <T> void deleteAllById(Iterable<?> ids, Class<T> entityClass);

  /**
   * Deletes the given entities.
   *
   * @param entities the entities to delete
   * @param <T> the entity type
   */
  <T> void deleteAll(Iterable<? extends T> entities);

  /**
   * Deletes all entities of the given class.
   *
   * @param entityClass the entity class
   * @param <T> the entity type
   */
  <T> void deleteAll(Class<T> entityClass);

  /**
   * Returns the number of entities in the table associated with the given class.
   *
   * @param entityClass the entity class
   * @param <T> the entity type
   * @return the number of entities
   */
  <T> long count(Class<T> entityClass);
}

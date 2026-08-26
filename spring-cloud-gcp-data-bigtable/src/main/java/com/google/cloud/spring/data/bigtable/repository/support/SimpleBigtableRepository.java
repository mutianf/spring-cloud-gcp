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

package com.google.cloud.spring.data.bigtable.repository.support;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.repository.BigtableRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link BigtableRepository}, delegating operations
 * to {@link BigtableOperations}.
 *
 * @param <T> the domain entity type
 * @param <ID> the entity identifier type
 */
public class SimpleBigtableRepository<T, ID> implements BigtableRepository<T, ID> {

  private final BigtableOperations bigtableOperations;
  private final Class<T> entityType;

  public SimpleBigtableRepository(BigtableOperations bigtableOperations, Class<T> entityType) {
    Assert.notNull(bigtableOperations, "A valid BigtableOperations object is required.");
    Assert.notNull(entityType, "A valid entity type is required.");
    this.bigtableOperations = bigtableOperations;
    this.entityType = entityType;
  }

  @Override
  public BigtableOperations getBigtableOperations() {
    return this.bigtableOperations;
  }

  @Override
  public <S extends T> S save(S entity) {
    Assert.notNull(entity, "Entity must not be null.");
    return this.bigtableOperations.save(entity);
  }

  @Override
  public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
    Assert.notNull(entities, "Entities must not be null.");
    return this.bigtableOperations.saveAll(entities);
  }

  @Override
  public Optional<T> findById(ID id) {
    Assert.notNull(id, "ID must not be null.");
    return Optional.ofNullable(this.bigtableOperations.findById(id, this.entityType));
  }

  @Override
  public boolean existsById(ID id) {
    Assert.notNull(id, "ID must not be null.");
    return this.bigtableOperations.existsById(id, this.entityType);
  }

  @Override
  public Iterable<T> findAll() {
    return this.bigtableOperations.findAll(this.entityType);
  }

  @Override
  public Iterable<T> findAllById(Iterable<ID> ids) {
    Assert.notNull(ids, "IDs must not be null.");
    return this.bigtableOperations.findAllById(ids, this.entityType);
  }

  @Override
  public long count() {
    return this.bigtableOperations.count(this.entityType);
  }

  @Override
  public void deleteById(ID id) {
    Assert.notNull(id, "ID must not be null.");
    this.bigtableOperations.deleteById(id, this.entityType);
  }

  @Override
  public void delete(T entity) {
    Assert.notNull(entity, "Entity must not be null.");
    this.bigtableOperations.delete(entity);
  }

  @Override
  public void deleteAllById(Iterable<? extends ID> ids) {
    Assert.notNull(ids, "IDs must not be null.");
    this.bigtableOperations.deleteAllById(ids, this.entityType);
  }

  @Override
  public void deleteAll(Iterable<? extends T> entities) {
    Assert.notNull(entities, "Entities must not be null.");
    this.bigtableOperations.deleteAll(entities);
  }

  @Override
  public void deleteAll() {
    this.bigtableOperations.deleteAll(this.entityType);
  }

  @Override
  public List<T> findByRowKeyPrefix(String prefix) {
    Assert.notNull(prefix, "Prefix must not be null.");
    return this.bigtableOperations.findByRowKeyPrefix(prefix, this.entityType);
  }

  @Override
  public List<T> findByRowKeyRange(String startKey, String endKey) {
    return this.bigtableOperations.findByRowKeyRange(startKey, endKey, this.entityType);
  }
}

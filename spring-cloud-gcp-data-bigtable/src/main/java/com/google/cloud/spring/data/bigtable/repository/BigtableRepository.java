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

package com.google.cloud.spring.data.bigtable.repository;

import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Spring Data repository interface for Cloud Bigtable, providing CRUD operations,
 * prefix scans, and range scans.
 *
 * @param <T> the domain entity type
 * @param <ID> the entity identifier type
 */
@NoRepositoryBean
public interface BigtableRepository<T, ID> extends CrudRepository<T, ID> {

  /**
   * Scans rows whose row key begins with the given prefix.
   *
   * @param prefix row key prefix
   * @return list of matching entities
   */
  List<T> findByRowKeyPrefix(String prefix);

  /**
   * Scans rows within the row key range [startKey, endKey).
   *
   * @param startKey inclusive start key, or null for unbounded start
   * @param endKey exclusive end key, or null for unbounded end
   * @return list of matching entities
   */
  List<T> findByRowKeyRange(String startKey, String endKey);

  /**
   * Returns the underlying {@link BigtableOperations} used by this repository.
   *
   * @return the Bigtable operations abstraction
   */
  BigtableOperations getBigtableOperations();
}

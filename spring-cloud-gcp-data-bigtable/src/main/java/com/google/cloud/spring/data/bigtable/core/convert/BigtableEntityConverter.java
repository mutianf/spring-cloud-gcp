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

package com.google.cloud.spring.data.bigtable.core.convert;

import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.protobuf.ByteString;

/**
 * Public interface for converting domain objects to Bigtable {@link RowMutation} instances
 * and deserializing Bigtable {@link Row} instances into domain objects.
 */
public interface BigtableEntityConverter {

  /**
   * Serializes the persistent properties of the given entity into the provided {@link RowMutation}.
   *
   * @param entity the domain entity to serialize
   * @param mutation the Bigtable RowMutation sink to populate
   */
  void write(Object entity, RowMutation mutation);

  /**
   * Deserializes a Bigtable {@link Row} into an instance of the specified domain entity class.
   *
   * @param type the entity class to deserialize into
   * @param row the Bigtable Row to read from
   * @param <T> the entity type
   * @return deserialized domain entity instance, or null if the row is null
   */
  <T> T read(Class<T> type, Row row);

  /**
   * Serializes and formats the row key from the entity's {@code @RowKey} annotated field(s).
   * Supports both single keys and composite keys joined by the entity's row key delimiter.
   *
   * @param entity the domain entity
   * @return the serialized row key as {@link ByteString}
   */
  ByteString toRowKey(Object entity);

  /**
   * Encodes an ID object for the given entity type into a Bigtable row key.
   *
   * @param entityType the entity class
   * @param id the id value or composite key object
   * @return the encoded row key as {@link ByteString}
   */
  ByteString encodeId(Class<?> entityType, Object id);
}

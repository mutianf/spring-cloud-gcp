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

/**
 * Callback interface for code that operates directly on the {@link BigtableDataClient}.
 *
 * @param <T> the result type
 */
@FunctionalInterface
public interface BigtableDataClientCallback<T> {

  /**
   * Performs operations on the provided {@link BigtableDataClient}.
   *
   * @param client the Bigtable data client
   * @return a result object, or {@code null}
   * @throws Exception if an error occurs
   */
  T doInBigtable(BigtableDataClient client) throws Exception;
}

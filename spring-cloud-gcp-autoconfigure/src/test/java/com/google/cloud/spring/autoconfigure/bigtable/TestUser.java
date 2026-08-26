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

package com.google.cloud.spring.autoconfigure.bigtable;

import com.google.cloud.spring.data.bigtable.core.mapping.BigtableTable;
import com.google.cloud.spring.data.bigtable.core.mapping.Column;
import com.google.cloud.spring.data.bigtable.core.mapping.RowKey;
import java.util.Objects;

@BigtableTable(name = "test_users")
public class TestUser {

  @RowKey
  private String id;

  @Column(family = "cf", qualifier = "name")
  private String name;

  public TestUser() {}

  public TestUser(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestUser testUser = (TestUser) o;
    return Objects.equals(id, testUser.id) && Objects.equals(name, testUser.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}

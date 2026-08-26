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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Id;

/**
 * Identifies a field or method as part of the Cloud Bigtable row key.
 * Meta-annotated with {@link Id} from Spring Data Commons.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Id
public @interface RowKey {

  /**
   * The 0-based ordering of this property within a composite row key.
   * Defaults to 0 for single row keys or the first component.
   *
   * @return the row key order
   */
  @AliasFor("value")
  int order() default 0;

  /**
   * Alias for {@link #order()}.
   *
   * @return the row key order
   */
  @AliasFor("order")
  int value() default 0;
}

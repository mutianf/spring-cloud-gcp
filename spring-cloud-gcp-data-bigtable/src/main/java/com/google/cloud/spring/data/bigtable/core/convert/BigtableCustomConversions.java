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

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Value object capturing custom type conversion rules for Cloud Bigtable.
 */
public class BigtableCustomConversions extends CustomConversions {

  public static final Set<Class<?>> BIGTABLE_SIMPLE_TYPES =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(ByteString.class, byte[].class, String.class)));

  public static final SimpleTypeHolder SIMPLE_TYPE_HOLDER =
      new SimpleTypeHolder(BIGTABLE_SIMPLE_TYPES, true);

  private static final StoreConversions STORE_CONVERSIONS;
  private static final List<Converter<?, ?>> STORE_CONVERTERS;

  static {
    List<Converter<?, ?>> converters = new ArrayList<>(BigtableConverters.getConvertersToRegister());
    STORE_CONVERTERS = Collections.unmodifiableList(converters);
    STORE_CONVERSIONS = StoreConversions.of(SIMPLE_TYPE_HOLDER, STORE_CONVERTERS);
  }

  /**
   * Creates a new instance of {@link BigtableCustomConversions} with default store converters.
   */
  public BigtableCustomConversions() {
    this(Collections.emptyList());
  }

  /**
   * Creates a new instance of {@link BigtableCustomConversions} with custom converters added.
   *
   * @param converters custom user converters to register
   */
  public BigtableCustomConversions(Collection<?> converters) {
    super(STORE_CONVERSIONS, converters != null ? converters : Collections.emptyList());
  }
}

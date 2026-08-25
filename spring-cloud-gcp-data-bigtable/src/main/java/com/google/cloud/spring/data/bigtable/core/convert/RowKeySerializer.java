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

import com.google.cloud.spring.data.bigtable.core.mapping.BigtableDataException;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntity;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentProperty;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Helper class responsible for serializing and deserializing Bigtable single and composite row keys,
 * enforcing delimiter collision detection and token count validation.
 */
final class RowKeySerializer {

  ByteString toRowKey(
      Object entity,
      BigtablePersistentEntity<?> persistentEntity,
      ConversionService conversionService,
      PersistentPropertyAccessor<?> accessor) {
    Assert.notNull(entity, "Entity must not be null");
    List<BigtablePersistentProperty> keyProps = persistentEntity.getRowKeyProperties();
    if (keyProps.isEmpty()) {
      throw new BigtableDataException(
          "No @RowKey properties found on entity " + persistentEntity.getType().getName());
    }

    if (keyProps.size() == 1) {
      BigtablePersistentProperty prop = keyProps.get(0);
      Object val = accessor.getProperty(prop);
      if (val == null) {
        throw new BigtableDataException(
            "Row key property '" + prop.getName() + "' cannot be null for entity "
                + persistentEntity.getType().getName());
      }
      return convertToByteString(val, conversionService);
    }

    // Composite row key
    String delimiter = persistentEntity.getRowKeyDelimiter();
    if (!StringUtils.hasText(delimiter)) {
      throw new BigtableDataException(
          "Row key delimiter cannot be empty for composite key in entity "
              + persistentEntity.getType().getName());
    }

    String[] parts = new String[keyProps.size()];
    for (int i = 0; i < keyProps.size(); i++) {
      BigtablePersistentProperty prop = keyProps.get(i);
      Object val = accessor.getProperty(prop);
      if (val == null) {
        throw new BigtableDataException(
            "Composite row key component '" + prop.getName() + "' (order " + prop.getRowKeyOrder()
                + ") cannot be null for entity " + persistentEntity.getType().getName());
      }
      String strVal = convertToString(val, conversionService);
      if (strVal.contains(delimiter)) {
        throw new BigtableDataException(
            "Row key component '" + prop.getName() + "' value '" + strVal
                + "' contains delimiter '" + delimiter
                + "', causing delimiter collision and preventing unambiguous decoding.");
      }
      parts[i] = strVal;
    }

    return ByteString.copyFromUtf8(String.join(delimiter, parts));
  }

  void hydrateRowKey(
      PersistentPropertyAccessor<?> accessor,
      ByteString rawKey,
      BigtablePersistentEntity<?> persistentEntity,
      ConversionService conversionService) {
    Assert.notNull(rawKey, "Row key must not be null");
    List<BigtablePersistentProperty> keyProps = persistentEntity.getRowKeyProperties();
    if (keyProps.isEmpty()) {
      return;
    }

    if (keyProps.size() == 1) {
      BigtablePersistentProperty prop = keyProps.get(0);
      Object val = convertFromByteString(rawKey, prop.getType(), conversionService);
      accessor.setProperty(prop, val);
      return;
    }

    // Composite row key
    String delimiter = persistentEntity.getRowKeyDelimiter();
    String keyStr = rawKey.toStringUtf8();
    String[] tokens = keyStr.split(Pattern.quote(delimiter), -1);

    if (tokens.length != keyProps.size()) {
      throw new BigtableDataException(
          "Row key '" + keyStr + "' contains " + tokens.length + " tokens with delimiter '"
              + delimiter + "', but entity " + persistentEntity.getType().getSimpleName()
              + " expects " + keyProps.size() + " components.");
    }

    for (int i = 0; i < keyProps.size(); i++) {
      BigtablePersistentProperty prop = keyProps.get(i);
      Object converted = convertFromString(tokens[i], prop.getType(), conversionService);
      accessor.setProperty(prop, converted);
    }
  }

  ByteString encodeId(
      Object id,
      BigtablePersistentEntity<?> persistentEntity,
      ConversionService conversionService) {
    Assert.notNull(id, "Row key ID cannot be null");
    if (id instanceof ByteString bs) {
      return bs;
    }
    if (id instanceof byte[] bytes) {
      return ByteString.copyFrom(bytes);
    }
    if (id instanceof String s) {
      return ByteString.copyFromUtf8(s);
    }

    List<BigtablePersistentProperty> keyProps = persistentEntity.getRowKeyProperties();
    String delimiter = persistentEntity.getRowKeyDelimiter();

    if (id instanceof Object[] arr) {
      if (arr.length != keyProps.size()) {
        throw new BigtableDataException(
            "Array length " + arr.length + " does not match row key component count "
                + keyProps.size() + " for entity " + persistentEntity.getType().getSimpleName());
      }
      return formatCompositeTokens(arr, delimiter, keyProps, conversionService);
    }

    if (id instanceof Iterable<?> iterable) {
      List<Object> list = new ArrayList<>();
      iterable.forEach(list::add);
      if (list.size() != keyProps.size()) {
        throw new BigtableDataException(
            "Iterable size " + list.size() + " does not match row key component count "
                + keyProps.size() + " for entity " + persistentEntity.getType().getSimpleName());
      }
      return formatCompositeTokens(list.toArray(), delimiter, keyProps, conversionService);
    }

    return convertToByteString(id, conversionService);
  }

  private ByteString formatCompositeTokens(
      Object[] parts,
      String delimiter,
      List<BigtablePersistentProperty> keyProps,
      ConversionService conversionService) {
    String[] tokens = new String[parts.length];
    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        throw new BigtableDataException(
            "Composite row key component at index " + i + " cannot be null");
      }
      String strVal = convertToString(parts[i], conversionService);
      if (strVal.contains(delimiter)) {
        throw new BigtableDataException(
            "Row key component '" + keyProps.get(i).getName() + "' value '" + strVal
                + "' contains delimiter '" + delimiter
                + "', causing delimiter collision.");
      }
      tokens[i] = strVal;
    }
    return ByteString.copyFromUtf8(String.join(delimiter, tokens));
  }

  private ByteString convertToByteString(Object value, ConversionService conversionService) {
    if (value instanceof ByteString bs) {
      return bs;
    }
    if (value instanceof byte[] bytes) {
      return ByteString.copyFrom(bytes);
    }
    if (value instanceof String s) {
      return ByteString.copyFromUtf8(s);
    }
    if (conversionService.canConvert(value.getClass(), ByteString.class)) {
      return conversionService.convert(value, ByteString.class);
    }
    return ByteString.copyFromUtf8(value.toString());
  }

  private String convertToString(Object value, ConversionService conversionService) {
    if (value instanceof ByteString bs) {
      return bs.toStringUtf8();
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    if (value instanceof String s) {
      return s;
    }
    if (conversionService.canConvert(value.getClass(), String.class)) {
      return conversionService.convert(value, String.class);
    }
    return value.toString();
  }

  @SuppressWarnings("unchecked")
  private <T> T convertFromByteString(
      ByteString byteString, Class<T> targetType, ConversionService conversionService) {
    if (ByteString.class.isAssignableFrom(targetType)) {
      return (T) byteString;
    }
    if (byte[].class.isAssignableFrom(targetType)) {
      return (T) byteString.toByteArray();
    }
    if (String.class.isAssignableFrom(targetType)) {
      return (T) byteString.toStringUtf8();
    }
    if (conversionService.canConvert(ByteString.class, targetType)) {
      return conversionService.convert(byteString, targetType);
    }
    return convertFromString(byteString.toStringUtf8(), targetType, conversionService);
  }

  @SuppressWarnings("unchecked")
  private <T> T convertFromString(
      String str, Class<T> targetType, ConversionService conversionService) {
    if (String.class.isAssignableFrom(targetType)) {
      return (T) str;
    }
    if (conversionService.canConvert(String.class, targetType)) {
      return conversionService.convert(str, targetType);
    }
    throw new BigtableDataException(
        "Cannot convert string '" + str + "' to target type " + targetType.getName());
  }
}

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
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableDataException;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentEntity;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtablePersistentProperty;
import com.google.protobuf.ByteString;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Default implementation of {@link BigtableEntityConverter} that maps domain entities to Cloud Bigtable
 * mutations and reconstructs entities from Bigtable rows using {@link BigtableMappingContext}.
 *
 * <p>Supports:
 * <ul>
 *   <li>Single and composite row keys formatted and parsed with the entity's delimiter</li>
 *   <li>Static {@code @Column} properties mapped to family and qualifier</li>
 *   <li>Dynamic wide columns ({@code @DynamicColumns}) mapped to {@code Map<String, ?>}</li>
 *   <li>Native Last-Write-Wins (LWW) deduplication over reverse-chronologically sorted cells</li>
 *   <li>Extensible type conversions via {@link BigtableCustomConversions}</li>
 * </ul>
 */
public class MappingBigtableEntityConverter implements BigtableEntityConverter {

  private final BigtableMappingContext mappingContext;
  private final BigtableCustomConversions customConversions;
  private final GenericConversionService conversionService;
  private final RowKeySerializer rowKeySerializer;

  public MappingBigtableEntityConverter(BigtableMappingContext mappingContext) {
    this(mappingContext, new BigtableCustomConversions());
  }

  public MappingBigtableEntityConverter(
      BigtableMappingContext mappingContext, BigtableCustomConversions customConversions) {
    Assert.notNull(mappingContext, "BigtableMappingContext must not be null");
    Assert.notNull(customConversions, "BigtableCustomConversions must not be null");
    this.mappingContext = mappingContext;
    this.customConversions = customConversions;
    this.conversionService = new DefaultConversionService();
    this.customConversions.registerConvertersIn(this.conversionService);
    this.rowKeySerializer = new RowKeySerializer();
  }

  public BigtableMappingContext getMappingContext() {
    return this.mappingContext;
  }

  public BigtableCustomConversions getCustomConversions() {
    return this.customConversions;
  }

  public ConversionService getConversionService() {
    return this.conversionService;
  }

  @Override
  public void write(Object entity, RowMutation mutation) {
    Assert.notNull(entity, "Entity to write must not be null");
    Assert.notNull(mutation, "RowMutation target must not be null");

    BigtablePersistentEntity<?> persistentEntity =
        this.mappingContext.getPersistentEntityOrFail(entity.getClass());
    PersistentPropertyAccessor<?> accessor = getPropertyAccessor(entity, persistentEntity);

    // 1. Write static @Column fields
    for (BigtablePersistentProperty property : persistentEntity.getColumnProperties()) {
      Object value = accessor.getProperty(property);
      if (value != null) {
        String family = property.getFamilyName();
        ByteString qualifier = ByteString.copyFromUtf8(property.getColumnQualifier());
        ByteString byteValue = convertToByteString(value);
        mutation.setCell(family, qualifier, byteValue);
      }
    }

    // 2. Write dynamic columns wide map
    if (persistentEntity.hasDynamicColumns()) {
      BigtablePersistentProperty dynamicProperty = persistentEntity.getDynamicColumnsProperty();
      Object mapVal = accessor.getProperty(dynamicProperty);
      if (mapVal instanceof Map<?, ?> map) {
        String family = dynamicProperty.getFamilyName();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (entry.getKey() != null && entry.getValue() != null) {
            ByteString qualifier = ByteString.copyFromUtf8(entry.getKey().toString());
            ByteString byteValue = convertToByteString(entry.getValue());
            mutation.setCell(family, qualifier, byteValue);
          }
        }
      }
    }
  }

  @Override
  public <T> T read(Class<T> type, Row row) {
    if (row == null) {
      return null;
    }
    Assert.notNull(type, "Target entity class must not be null");

    @SuppressWarnings("unchecked")
    BigtablePersistentEntity<T> persistentEntity =
        (BigtablePersistentEntity<T>) this.mappingContext.getPersistentEntityOrFail(type);

    T instance = instantiateEntity(persistentEntity);
    PersistentPropertyAccessor<T> accessor = getPropertyAccessor(instance, persistentEntity);

    // 1. Hydrate row key
    this.rowKeySerializer.hydrateRowKey(
        accessor, row.getKey(), persistentEntity, this.conversionService);

    // 2. Native Last-Write-Wins (LWW) deduplication:
    // Row.getCells() returns cells in reverse-chronological order (newest timestamp first).
    // Using putIfAbsent retains only the newest cell version for each (family, qualifier) coordinate.
    Map<CellCoordinate, RowCell> latestCells = new LinkedHashMap<>();
    for (RowCell cell : row.getCells()) {
      latestCells.putIfAbsent(new CellCoordinate(cell.getFamily(), cell.getQualifier()), cell);
    }

    // 3. Hydrate static @Column properties
    for (BigtablePersistentProperty property : persistentEntity.getColumnProperties()) {
      String family = property.getFamilyName();
      ByteString qualifier = ByteString.copyFromUtf8(property.getColumnQualifier());
      RowCell cell = latestCells.get(new CellCoordinate(family, qualifier));
      if (cell != null) {
        Object convertedValue = convertFromByteString(cell.getValue(), property.getType());
        accessor.setProperty(property, convertedValue);
      }
    }

    // 4. Hydrate dynamic columns wide map
    if (persistentEntity.hasDynamicColumns()) {
      BigtablePersistentProperty dynamicProp = persistentEntity.getDynamicColumnsProperty();
      String family = dynamicProp.getFamilyName();
      Map<String, Object> dynamicMap = new LinkedHashMap<>();
      Class<?> valueType = resolveDynamicColumnValueType(dynamicProp);

      for (Map.Entry<CellCoordinate, RowCell> entry : latestCells.entrySet()) {
        if (entry.getKey().family().equals(family)) {
          String colName = entry.getKey().qualifier().toStringUtf8();
          ByteString cellValue = entry.getValue().getValue();
          Object convertedValue = convertFromByteString(cellValue, valueType);
          dynamicMap.put(colName, convertedValue);
        }
      }
      accessor.setProperty(dynamicProp, dynamicMap);
    }

    return instance;
  }

  @Override
  public ByteString toRowKey(Object entity) {
    Assert.notNull(entity, "Entity must not be null");
    BigtablePersistentEntity<?> persistentEntity =
        this.mappingContext.getPersistentEntityOrFail(entity.getClass());
    PersistentPropertyAccessor<?> accessor = getPropertyAccessor(entity, persistentEntity);
    return this.rowKeySerializer.toRowKey(
        entity, persistentEntity, this.conversionService, accessor);
  }

  @Override
  public ByteString encodeId(Class<?> entityType, Object id) {
    Assert.notNull(entityType, "Entity type must not be null");
    Assert.notNull(id, "Row key ID must not be null");
    BigtablePersistentEntity<?> persistentEntity =
        this.mappingContext.getPersistentEntityOrFail(entityType);
    return this.rowKeySerializer.encodeId(id, persistentEntity, this.conversionService);
  }

  /**
   * Helper to create and populate a {@link RowMutation} directly from an entity instance.
   *
   * @param entity the entity to create a mutation for
   * @return populated {@link RowMutation}
   */
  public RowMutation createRowMutation(Object entity) {
    Assert.notNull(entity, "Entity must not be null");
    BigtablePersistentEntity<?> persistentEntity =
        this.mappingContext.getPersistentEntityOrFail(entity.getClass());
    ByteString key = toRowKey(entity);
    RowMutation mutation = RowMutation.create(persistentEntity.getTableName(), key);
    write(entity, mutation);
    return mutation;
  }

  private ByteString convertToByteString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof ByteString bs) {
      return bs;
    }
    if (value instanceof byte[] bytes) {
      return ByteString.copyFrom(bytes);
    }
    if (value instanceof String s) {
      return ByteString.copyFromUtf8(s);
    }
    if (this.conversionService.canConvert(value.getClass(), ByteString.class)) {
      return this.conversionService.convert(value, ByteString.class);
    }
    if (this.conversionService.canConvert(value.getClass(), String.class)) {
      String str = this.conversionService.convert(value, String.class);
      return ByteString.copyFromUtf8(str != null ? str : "");
    }
    return ByteString.copyFromUtf8(value.toString());
  }

  @SuppressWarnings("unchecked")
  private <V> V convertFromByteString(ByteString byteString, Class<V> targetType) {
    if (byteString == null) {
      return null;
    }
    if (ByteString.class.isAssignableFrom(targetType)) {
      return (V) byteString;
    }
    if (byte[].class.isAssignableFrom(targetType)) {
      return (V) byteString.toByteArray();
    }
    if (String.class.isAssignableFrom(targetType)) {
      return (V) byteString.toStringUtf8();
    }
    if (this.conversionService.canConvert(ByteString.class, targetType)) {
      return this.conversionService.convert(byteString, targetType);
    }
    String str = byteString.toStringUtf8();
    if (this.conversionService.canConvert(String.class, targetType)) {
      return this.conversionService.convert(str, targetType);
    }
    throw new BigtableDataException(
        "Cannot convert ByteString to target type " + targetType.getName());
  }

  private Class<?> resolveDynamicColumnValueType(BigtablePersistentProperty property) {
    Field field = property.getField();
    if (field != null && field.getGenericType() instanceof ParameterizedType pt) {
      Type[] args = pt.getActualTypeArguments();
      if (args.length > 1 && args[1] instanceof Class<?> clazz && !clazz.equals(Object.class)) {
        return clazz;
      }
    }
    return String.class;
  }

  private <T> T instantiateEntity(BigtablePersistentEntity<T> persistentEntity) {
    try {
      Constructor<T> ctor = persistentEntity.getType().getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor.newInstance();
    } catch (Exception e) {
      throw new BigtableDataException(
          "Failed to instantiate entity " + persistentEntity.getType().getName()
              + ". Ensure a default no-argument constructor is declared.",
          e);
    }
  }

  @SuppressWarnings("unchecked")
  private <B> PersistentPropertyAccessor<B> getPropertyAccessor(
      B bean, BigtablePersistentEntity<?> entity) {
    PersistentPropertyAccessor<B> accessor = entity.getPropertyAccessor(bean);
    if (accessor != null) {
      return accessor;
    }
    return new ReflectionPropertyAccessor<>(bean);
  }

  private record CellCoordinate(String family, ByteString qualifier) {}

  private static class ReflectionPropertyAccessor<T> implements PersistentPropertyAccessor<T> {
    private final T bean;

    ReflectionPropertyAccessor(T bean) {
      this.bean = bean;
    }

    @Override
    public void setProperty(PersistentProperty<?> property, Object value) {
      Field field = ReflectionUtils.findField(bean.getClass(), property.getName());
      if (field != null) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, value);
      }
    }

    @Override
    public Object getProperty(PersistentProperty<?> property) {
      Field field = ReflectionUtils.findField(bean.getClass(), property.getName());
      if (field != null) {
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, bean);
      }
      return null;
    }

    @Override
    public T getBean() {
      return bean;
    }
  }
}

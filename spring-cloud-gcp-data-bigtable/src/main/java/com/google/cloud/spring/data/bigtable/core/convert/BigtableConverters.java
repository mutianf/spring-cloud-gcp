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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Built-in converters between standard Java types and Cloud Bigtable {@link ByteString} cells.
 */
public final class BigtableConverters {

  private BigtableConverters() {}

  @WritingConverter
  public enum StringToByteStringConverter implements Converter<String, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull String source) {
      return ByteString.copyFromUtf8(source);
    }
  }

  @ReadingConverter
  public enum ByteStringToStringConverter implements Converter<ByteString, String> {
    INSTANCE;

    @Override
    public String convert(@NonNull ByteString source) {
      return source.toStringUtf8();
    }
  }

  @WritingConverter
  public enum ByteArrayToByteStringConverter implements Converter<byte[], ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull byte[] source) {
      return ByteString.copyFrom(source);
    }
  }

  @ReadingConverter
  public enum ByteStringToByteArrayConverter implements Converter<ByteString, byte[]> {
    INSTANCE;

    @Override
    public byte[] convert(@NonNull ByteString source) {
      return source.toByteArray();
    }
  }

  @WritingConverter
  public enum IntegerToByteStringConverter implements Converter<Integer, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Integer source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToIntegerConverter implements Converter<ByteString, Integer> {
    INSTANCE;

    @Override
    public Integer convert(@NonNull ByteString source) {
      return Integer.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum LongToByteStringConverter implements Converter<Long, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Long source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToLongConverter implements Converter<ByteString, Long> {
    INSTANCE;

    @Override
    public Long convert(@NonNull ByteString source) {
      return Long.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum DoubleToByteStringConverter implements Converter<Double, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Double source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToDoubleConverter implements Converter<ByteString, Double> {
    INSTANCE;

    @Override
    public Double convert(@NonNull ByteString source) {
      return Double.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum FloatToByteStringConverter implements Converter<Float, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Float source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToFloatConverter implements Converter<ByteString, Float> {
    INSTANCE;

    @Override
    public Float convert(@NonNull ByteString source) {
      return Float.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum BooleanToByteStringConverter implements Converter<Boolean, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Boolean source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToBooleanConverter implements Converter<ByteString, Boolean> {
    INSTANCE;

    @Override
    public Boolean convert(@NonNull ByteString source) {
      return Boolean.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum ShortToByteStringConverter implements Converter<Short, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Short source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToShortConverter implements Converter<ByteString, Short> {
    INSTANCE;

    @Override
    public Short convert(@NonNull ByteString source) {
      return Short.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum ByteToByteStringConverter implements Converter<Byte, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Byte source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToByteConverter implements Converter<ByteString, Byte> {
    INSTANCE;

    @Override
    public Byte convert(@NonNull ByteString source) {
      return Byte.valueOf(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum BigIntegerToByteStringConverter implements Converter<BigInteger, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull BigInteger source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToBigIntegerConverter implements Converter<ByteString, BigInteger> {
    INSTANCE;

    @Override
    public BigInteger convert(@NonNull ByteString source) {
      return new BigInteger(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum BigDecimalToByteStringConverter implements Converter<BigDecimal, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull BigDecimal source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToBigDecimalConverter implements Converter<ByteString, BigDecimal> {
    INSTANCE;

    @Override
    public BigDecimal convert(@NonNull ByteString source) {
      return new BigDecimal(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum UUIDToByteStringConverter implements Converter<UUID, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull UUID source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToUUIDConverter implements Converter<ByteString, UUID> {
    INSTANCE;

    @Override
    public UUID convert(@NonNull ByteString source) {
      return UUID.fromString(source.toStringUtf8());
    }
  }

  @WritingConverter
  public enum InstantToByteStringConverter implements Converter<Instant, ByteString> {
    INSTANCE;

    @Override
    public ByteString convert(@NonNull Instant source) {
      return ByteString.copyFromUtf8(source.toString());
    }
  }

  @ReadingConverter
  public enum ByteStringToInstantConverter implements Converter<ByteString, Instant> {
    INSTANCE;

    @Override
    public Instant convert(@NonNull ByteString source) {
      return Instant.parse(source.toStringUtf8());
    }
  }

  /**
   * Returns a list of default converters to register for Bigtable.
   *
   * @return list of converters
   */
  public static Collection<Converter<?, ?>> getConvertersToRegister() {
    List<Converter<?, ?>> converters = new ArrayList<>();
    converters.add(StringToByteStringConverter.INSTANCE);
    converters.add(ByteStringToStringConverter.INSTANCE);
    converters.add(ByteArrayToByteStringConverter.INSTANCE);
    converters.add(ByteStringToByteArrayConverter.INSTANCE);
    converters.add(IntegerToByteStringConverter.INSTANCE);
    converters.add(ByteStringToIntegerConverter.INSTANCE);
    converters.add(LongToByteStringConverter.INSTANCE);
    converters.add(ByteStringToLongConverter.INSTANCE);
    converters.add(DoubleToByteStringConverter.INSTANCE);
    converters.add(ByteStringToDoubleConverter.INSTANCE);
    converters.add(FloatToByteStringConverter.INSTANCE);
    converters.add(ByteStringToFloatConverter.INSTANCE);
    converters.add(BooleanToByteStringConverter.INSTANCE);
    converters.add(ByteStringToBooleanConverter.INSTANCE);
    converters.add(ShortToByteStringConverter.INSTANCE);
    converters.add(ByteStringToShortConverter.INSTANCE);
    converters.add(ByteToByteStringConverter.INSTANCE);
    converters.add(ByteStringToByteConverter.INSTANCE);
    converters.add(BigIntegerToByteStringConverter.INSTANCE);
    converters.add(ByteStringToBigIntegerConverter.INSTANCE);
    converters.add(BigDecimalToByteStringConverter.INSTANCE);
    converters.add(ByteStringToBigDecimalConverter.INSTANCE);
    converters.add(UUIDToByteStringConverter.INSTANCE);
    converters.add(ByteStringToUUIDConverter.INSTANCE);
    converters.add(InstantToByteStringConverter.INSTANCE);
    converters.add(ByteStringToInstantConverter.INSTANCE);
    return Collections.unmodifiableList(converters);
  }
}

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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link BigtableCustomConversions} and {@link BigtableConverters}.
 */
class BigtableCustomConversionsTest {

  @Test
  void testSimpleTypes() {
    assertThat(BigtableCustomConversions.SIMPLE_TYPE_HOLDER.isSimpleType(ByteString.class)).isTrue();
    assertThat(BigtableCustomConversions.SIMPLE_TYPE_HOLDER.isSimpleType(byte[].class)).isTrue();
    assertThat(BigtableCustomConversions.SIMPLE_TYPE_HOLDER.isSimpleType(String.class)).isTrue();
  }

  @Test
  void testDefaultConvertersRegistration() {
    BigtableCustomConversions conversions = new BigtableCustomConversions();
    DefaultConversionService service = new DefaultConversionService();
    conversions.registerConvertersIn(service);

    // String <-> ByteString
    assertThat(service.convert("hello", ByteString.class)).isEqualTo(ByteString.copyFromUtf8("hello"));
    assertThat(service.convert(ByteString.copyFromUtf8("world"), String.class)).isEqualTo("world");

    // byte[] <-> ByteString
    byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
    assertThat(service.convert(bytes, ByteString.class)).isEqualTo(ByteString.copyFrom(bytes));
    assertThat(service.convert(ByteString.copyFrom(bytes), byte[].class)).isEqualTo(bytes);

    // Integer <-> ByteString
    assertThat(service.convert(42, ByteString.class)).isEqualTo(ByteString.copyFromUtf8("42"));
    assertThat(service.convert(ByteString.copyFromUtf8("42"), Integer.class)).isEqualTo(42);

    // Long <-> ByteString
    assertThat(service.convert(123456789L, ByteString.class))
        .isEqualTo(ByteString.copyFromUtf8("123456789"));
    assertThat(service.convert(ByteString.copyFromUtf8("123456789"), Long.class))
        .isEqualTo(123456789L);

    // Double <-> ByteString
    assertThat(service.convert(3.14159, ByteString.class))
        .isEqualTo(ByteString.copyFromUtf8("3.14159"));
    assertThat(service.convert(ByteString.copyFromUtf8("3.14159"), Double.class))
        .isEqualTo(3.14159);

    // Boolean <-> ByteString
    assertThat(service.convert(true, ByteString.class)).isEqualTo(ByteString.copyFromUtf8("true"));
    assertThat(service.convert(ByteString.copyFromUtf8("true"), Boolean.class)).isTrue();

    // UUID <-> ByteString
    UUID uuid = UUID.randomUUID();
    assertThat(service.convert(uuid, ByteString.class))
        .isEqualTo(ByteString.copyFromUtf8(uuid.toString()));
    assertThat(service.convert(ByteString.copyFromUtf8(uuid.toString()), UUID.class))
        .isEqualTo(uuid);

    // Instant <-> ByteString
    Instant now = Instant.parse("2026-08-25T20:00:00Z");
    assertThat(service.convert(now, ByteString.class))
        .isEqualTo(ByteString.copyFromUtf8("2026-08-25T20:00:00Z"));
    assertThat(service.convert(ByteString.copyFromUtf8("2026-08-25T20:00:00Z"), Instant.class))
        .isEqualTo(now);

    // BigInteger <-> ByteString
    BigInteger bi = new BigInteger("999999999999999999");
    assertThat(service.convert(bi, ByteString.class)).isEqualTo(ByteString.copyFromUtf8(bi.toString()));
    assertThat(service.convert(ByteString.copyFromUtf8(bi.toString()), BigInteger.class)).isEqualTo(bi);

    // BigDecimal <-> ByteString
    BigDecimal bd = new BigDecimal("12345.6789");
    assertThat(service.convert(bd, ByteString.class)).isEqualTo(ByteString.copyFromUtf8(bd.toString()));
    assertThat(service.convert(ByteString.copyFromUtf8(bd.toString()), BigDecimal.class)).isEqualTo(bd);
  }

  @Test
  void testUserCustomConvertersRegistration() {
    Converter<Point, ByteString> pointWriter =
        new Converter<Point, ByteString>() {
          @Override
          public ByteString convert(Point source) {
            return ByteString.copyFromUtf8(source.x() + "," + source.y());
          }
        };
    Converter<ByteString, Point> pointReader =
        new Converter<ByteString, Point>() {
          @Override
          public Point convert(ByteString source) {
            String[] parts = source.toStringUtf8().split(",");
            return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
          }
        };

    BigtableCustomConversions conversions =
        new BigtableCustomConversions(java.util.Arrays.asList(pointWriter, pointReader));
    DefaultConversionService service = new DefaultConversionService();
    conversions.registerConvertersIn(service);

    Point point = new Point(10, 20);
    ByteString bs = service.convert(point, ByteString.class);
    assertThat(bs).isEqualTo(ByteString.copyFromUtf8("10,20"));

    Point readPoint = service.convert(bs, Point.class);
    assertThat(readPoint).isEqualTo(point);
  }

  private record Point(int x, int y) {}
}

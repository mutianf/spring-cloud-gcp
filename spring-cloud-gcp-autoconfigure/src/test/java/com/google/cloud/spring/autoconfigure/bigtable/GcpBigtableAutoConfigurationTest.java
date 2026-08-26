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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.spring.autoconfigure.TestUtils;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.BigtableTemplate;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableCustomConversions;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for Cloud Bigtable autoconfiguration.
 */
class GcpBigtableAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  GcpContextAutoConfiguration.class,
                  GcpBigtableAutoConfiguration.class))
          .withUserConfiguration(TestConfiguration.class)
          .withPropertyValues(
              "spring.cloud.gcp.bigtable.project-id=test-project",
              "spring.cloud.gcp.bigtable.instance-id=test-instance");

  @Test
  void testDefaultBeanCreation() {
    this.contextRunner.run(
        context -> {
          assertThat(context.getBean(BigtableDataSettings.class)).isNotNull();
          assertThat(context.getBean(BigtableDataClient.class)).isNotNull();
          assertThat(context.getBean(BigtableMappingContext.class)).isNotNull();
          assertThat(context.getBean(BigtableCustomConversions.class)).isNotNull();
          assertThat(context.getBean(BigtableEntityConverter.class)).isNotNull();
          assertThat(context.getBean(BigtableTemplate.class)).isNotNull();
          assertThat(context.getBean(BigtableOperations.class)).isNotNull();
        });
  }

  @Test
  void testBigtableDataSettingsWithEmulatorHost() {
    this.contextRunner
        .withPropertyValues("spring.cloud.gcp.bigtable.emulator-host=localhost:8086")
        .run(
            context -> {
              BigtableDataSettings settings = context.getBean(BigtableDataSettings.class);
              assertThat(settings.getProjectId()).isEqualTo("test-project");
              assertThat(settings.getInstanceId()).isEqualTo("test-instance");
              assertThat(settings.getStubSettings().getEndpoint()).isEqualTo("localhost:8086");
            });
  }

  @Test
  void testBigtableDataSettingsWithNestedEmulatorProperties() {
    this.contextRunner
        .withPropertyValues(
            "spring.cloud.gcp.bigtable.emulator.enabled=true",
            "spring.cloud.gcp.bigtable.emulator.host=localhost",
            "spring.cloud.gcp.bigtable.emulator.port=9000")
        .run(
            context -> {
              GcpBigtableProperties properties = context.getBean(GcpBigtableProperties.class);
              assertThat(properties.getEmulator().isEnabled()).isTrue();
              assertThat(properties.getEmulatorHost()).isEqualTo("localhost:9000");

              BigtableDataSettings settings = context.getBean(BigtableDataSettings.class);
              assertThat(settings.getStubSettings().getEndpoint()).isEqualTo("localhost:9000");
            });
  }

  @Test
  void testBigtableDataSettingsWithCustomProperties() {
    this.contextRunner
        .withPropertyValues(
            "spring.cloud.gcp.bigtable.project-id=custom-project",
            "spring.cloud.gcp.bigtable.instance-id=custom-instance",
            "spring.cloud.gcp.bigtable.app-profile-id=custom-profile")
        .run(
            context -> {
              GcpBigtableProperties properties = context.getBean(GcpBigtableProperties.class);
              assertThat(properties.getProjectId()).isEqualTo("custom-project");
              assertThat(properties.getInstanceId()).isEqualTo("custom-instance");
              assertThat(properties.getAppProfileId()).isEqualTo("custom-profile");

              BigtableDataSettings settings = context.getBean(BigtableDataSettings.class);
              assertThat(settings.getProjectId()).isEqualTo("custom-project");
              assertThat(settings.getInstanceId()).isEqualTo("custom-instance");
              assertThat(settings.getAppProfileId()).isEqualTo("custom-profile");
            });
  }

  @Test
  void testDisabledProperty() {
    this.contextRunner
        .withPropertyValues("spring.cloud.gcp.bigtable.enabled=false")
        .run(
            context -> {
              assertThat(context.getBeansOfType(BigtableTemplate.class)).isEmpty();
              assertThat(context.getBeansOfType(BigtableMappingContext.class)).isEmpty();
              assertThat(context.getBeansOfType(BigtableEntityConverter.class)).isEmpty();
              assertThat(context.getBeansOfType(BigtableDataSettings.class)).isEmpty();
            });
  }

  @Test
  void testConditionalOnMissingBeanOverrides() {
    BigtableTemplate customTemplate = mock(BigtableTemplate.class);
    BigtableMappingContext customMappingContext = new BigtableMappingContext();
    BigtableCustomConversions customConversions = new BigtableCustomConversions();
    BigtableEntityConverter customConverter = mock(BigtableEntityConverter.class);

    this.contextRunner
        .withBean(BigtableOperations.class, () -> customTemplate)
        .withBean(BigtableMappingContext.class, () -> customMappingContext)
        .withBean(BigtableCustomConversions.class, () -> customConversions)
        .withBean(BigtableEntityConverter.class, () -> customConverter)
        .run(
            context -> {
              assertThat(context.getBean(BigtableOperations.class)).isSameAs(customTemplate);
              assertThat(context.getBean(BigtableMappingContext.class)).isSameAs(customMappingContext);
              assertThat(context.getBean(BigtableCustomConversions.class)).isSameAs(customConversions);
              assertThat(context.getBean(BigtableEntityConverter.class)).isSameAs(customConverter);
            });
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    public CredentialsProvider credentialsProvider() {
      return () -> TestUtils.MOCK_CREDENTIALS;
    }

    @Bean
    public BigtableDataClient bigtableDataClient() {
      return mock(BigtableDataClient.class);
    }
  }
}

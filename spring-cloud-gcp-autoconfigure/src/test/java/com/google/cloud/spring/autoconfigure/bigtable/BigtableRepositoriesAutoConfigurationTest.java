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
import com.google.cloud.spring.autoconfigure.TestUtils;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.data.bigtable.repository.BigtableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for Bigtable repositories autoconfiguration.
 */
class BigtableRepositoriesAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  GcpContextAutoConfiguration.class,
                  GcpBigtableAutoConfiguration.class,
                  BigtableRepositoriesAutoConfiguration.class))
          .withUserConfiguration(TestConfiguration.class)
          .withPropertyValues(
              "spring.cloud.gcp.bigtable.project-id=test-project",
              "spring.cloud.gcp.bigtable.instance-id=test-instance");

  @Test
  void testRepositoryCreated() {
    this.contextRunner.run(
        context -> {
          assertThat(context.getBean(TestUserRepository.class)).isNotNull();
          assertThat(context.getBean(TestUserRepository.class)).isInstanceOf(BigtableRepository.class);
        });
  }

  @Test
  void testRepositoriesDisabled() {
    this.contextRunner
        .withPropertyValues("spring.cloud.gcp.bigtable.enabled=false")
        .run(
            context -> {
              assertThat(context.getBeansOfType(TestUserRepository.class)).isEmpty();
            });
  }

  @AutoConfigurationPackage
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

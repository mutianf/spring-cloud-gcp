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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.data.bigtable.core.BigtableOperations;
import com.google.cloud.spring.data.bigtable.core.BigtableTemplate;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableCustomConversions;
import com.google.cloud.spring.data.bigtable.core.convert.BigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.convert.MappingBigtableEntityConverter;
import com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext;
import java.io.IOException;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Spring Data Cloud Bigtable.
 */
@AutoConfiguration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnClass({BigtableDataClient.class, BigtableTemplate.class})
@ConditionalOnProperty(value = "spring.cloud.gcp.bigtable.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GcpBigtableProperties.class)
public class GcpBigtableAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public BigtableDataSettings bigtableDataSettings(
      GcpBigtableProperties properties,
      Optional<GcpProjectIdProvider> projectIdProvider,
      Optional<CredentialsProvider> credentialsProvider)
      throws IOException {

    String projectId =
        properties.getProjectId() != null
            ? properties.getProjectId()
            : projectIdProvider.map(GcpProjectIdProvider::getProjectId).orElse(null);

    String instanceId = properties.getInstanceId();

    BigtableDataSettings.Builder builder;

    String emulatorHost = properties.getEmulatorHost();
    if (emulatorHost != null && !emulatorHost.trim().isEmpty()) {
      emulatorHost = emulatorHost.trim();
      if (emulatorHost.contains(":")) {
        String[] parts = emulatorHost.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        builder = BigtableDataSettings.newBuilderForEmulator(host, port);
      } else {
        int port = Integer.parseInt(emulatorHost);
        builder = BigtableDataSettings.newBuilderForEmulator(port);
      }
      if (projectId != null) {
        builder.setProjectId(projectId);
      }
      if (instanceId != null) {
        builder.setInstanceId(instanceId);
      }
      if (properties.getAppProfileId() != null) {
        builder.setAppProfileId(properties.getAppProfileId());
      }
    } else {
      builder = BigtableDataSettings.newBuilder();
      if (projectId != null) {
        builder.setProjectId(projectId);
      }
      if (instanceId != null) {
        builder.setInstanceId(instanceId);
      }
      if (properties.getAppProfileId() != null) {
        builder.setAppProfileId(properties.getAppProfileId());
      }
      CredentialsProvider creds =
          properties.getCredentials().hasKey()
              ? new DefaultCredentialsProvider(properties)
              : credentialsProvider.orElse(null);
      if (creds != null) {
        builder.setCredentialsProvider(creds);
      }
    }

    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean
  public BigtableDataClient bigtableDataClient(BigtableDataSettings settings) throws IOException {
    return BigtableDataClient.create(settings);
  }

  @Bean
  @ConditionalOnMissingBean
  public BigtableMappingContext bigtableMappingContext() {
    return new BigtableMappingContext();
  }

  @Bean
  @ConditionalOnMissingBean
  public BigtableCustomConversions bigtableCustomConversions() {
    return new BigtableCustomConversions();
  }

  @Bean
  @ConditionalOnMissingBean
  public BigtableEntityConverter bigtableEntityConverter(
      BigtableMappingContext mappingContext, BigtableCustomConversions customConversions) {
    return new MappingBigtableEntityConverter(mappingContext, customConversions);
  }

  @Bean
  @ConditionalOnMissingBean(BigtableOperations.class)
  public BigtableTemplate bigtableTemplate(
      BigtableDataClient bigtableDataClient,
      BigtableMappingContext mappingContext,
      BigtableEntityConverter entityConverter) {
    return new BigtableTemplate(bigtableDataClient, mappingContext, entityConverter);
  }
}

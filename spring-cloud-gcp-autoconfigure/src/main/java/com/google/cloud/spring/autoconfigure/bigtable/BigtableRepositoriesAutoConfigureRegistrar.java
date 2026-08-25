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

import com.google.cloud.spring.data.bigtable.repository.config.BigtableRepositoryConfigurationExtension;
import com.google.cloud.spring.data.bigtable.repository.config.EnableBigtableRepositories;
import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar} used to
 * auto-configure Spring Data Cloud Bigtable Repositories.
 */
public class BigtableRepositoriesAutoConfigureRegistrar
    extends AbstractRepositoryConfigurationSourceSupport {

  @Override
  protected Class<? extends Annotation> getAnnotation() {
    return EnableBigtableRepositories.class;
  }

  @Override
  protected Class<?> getConfiguration() {
    return EnableBigtableRepositoriesConfiguration.class;
  }

  @Override
  protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
    return new BigtableRepositoryConfigurationExtension();
  }

  @EnableBigtableRepositories
  private static class EnableBigtableRepositoriesConfiguration {}
}

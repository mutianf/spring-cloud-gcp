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

package com.google.cloud.spring.data.bigtable.repository.config;

import com.google.cloud.spring.data.bigtable.repository.support.BigtableRepositoryFactoryBean;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

/**
 * Annotation that enables the instantiation of Cloud Bigtable repositories.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(BigtableRepositoriesRegistrar.class)
public @interface EnableBigtableRepositories {

  /**
   * Alias for the {@link #basePackages()} attribute.
   *
   * @return base packages to scan
   */
  String[] value() default {};

  /**
   * Specifies which types are eligible for component scanning.
   *
   * @return include filters
   */
  Filter[] includeFilters() default {};

  /**
   * Specifies which types are not eligible for component scanning.
   *
   * @return exclude filters
   */
  Filter[] excludeFilters() default {};

  /**
   * Base packages to scan for annotated components.
   *
   * @return array of package names
   */
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying packages to scan.
   *
   * @return array of classes
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * Configure the repository base class to be used to create repository proxies.
   *
   * @return the repository base class
   */
  Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

  /**
   * Configures whether nested repository interfaces should be discovered.
   *
   * @return true if nested repositories should be considered
   */
  boolean considerNestedRepositories() default false;

  /**
   * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be used for each repository.
   *
   * @return the repository factory bean class
   */
  Class<?> repositoryFactoryBeanClass() default BigtableRepositoryFactoryBean.class;

  /**
   * Configures the location of where to read Spring Data named queries properties file.
   *
   * @return the named queries properties file location
   */
  String namedQueriesLocation() default "";

  /**
   * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}.
   *
   * @return the implementation postfix
   */
  String repositoryImplementationPostfix() default "";

  /**
   * Configures the bean name of the {@link com.google.cloud.spring.data.bigtable.core.BigtableOperations}
   * to be used by default with repositories detected.
   *
   * @return the name of the Bigtable template / operations bean
   */
  String bigtableTemplateRef() default "bigtableTemplate";

  /**
   * Configures the bean name of the {@link com.google.cloud.spring.data.bigtable.core.mapping.BigtableMappingContext}
   * to be used by default with repositories detected.
   *
   * @return the name of the Bigtable mapping context bean
   */
  String bigtableMappingContextRef() default "bigtableMappingContext";
}

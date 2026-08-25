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

package com.google.cloud.spring.data.bigtable.core.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Basic implementation of {@link BigtablePersistentEntity}.
 *
 * @param <T> the persistent entity type
 */
public class BasicBigtablePersistentEntity<T>
    extends BasicPersistentEntity<T, BigtablePersistentProperty>
    implements BigtablePersistentEntity<T> {

  private static final ExpressionParser PARSER = new SpelExpressionParser();

  private final BigtableTable tableAnnotation;
  private final Expression tableNameExpression;
  private final StandardEvaluationContext evaluationContext;
  private String tableName;
  private final List<BigtablePersistentProperty> rowKeyProperties = new ArrayList<>();
  private final List<BigtablePersistentProperty> columnProperties = new ArrayList<>();
  private BigtablePersistentProperty dynamicColumnsProperty;

  public BasicBigtablePersistentEntity(TypeInformation<T> information) {
    super(information);
    this.evaluationContext = new StandardEvaluationContext();
    this.tableAnnotation = findAnnotation(BigtableTable.class);
    this.tableNameExpression = detectExpression();
  }

  @Nullable
  private Expression detectExpression() {
    if (this.tableAnnotation == null) {
      return null;
    }
    String name = StringUtils.hasText(this.tableAnnotation.name())
        ? this.tableAnnotation.name()
        : this.tableAnnotation.value();
    if (!StringUtils.hasText(name)) {
      return null;
    }
    Expression expression = PARSER.parseExpression(name, ParserContext.TEMPLATE_EXPRESSION);
    return (expression instanceof LiteralExpression) ? null : expression;
  }

  @Override
  public void addPersistentProperty(BigtablePersistentProperty property) {
    super.addPersistentProperty(property);
    if (property.isRowKey()) {
      this.rowKeyProperties.add(property);
    } else if (property.isColumn()) {
      this.columnProperties.add(property);
    } else if (property.isDynamicColumns()) {
      this.dynamicColumnsProperty = property;
    }
  }

  @Override
  protected BigtablePersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
      BigtablePersistentProperty property) {
    if (!property.isIdProperty()) {
      return null;
    }
    if (this.getIdProperty() != null) {
      // Multiple @RowKey properties represent a composite key; allow them without failing here.
      return null;
    }
    return property;
  }

  @Override
  public String getTableName() {
    if (this.tableName == null) {
      if (this.tableAnnotation != null) {
        String rawName = StringUtils.hasText(this.tableAnnotation.name())
            ? this.tableAnnotation.name()
            : this.tableAnnotation.value();
        if (StringUtils.hasText(rawName)) {
          if (this.tableNameExpression != null) {
            this.tableName = this.tableNameExpression.getValue(this.evaluationContext, String.class);
          } else {
            this.tableName = rawName;
          }
        } else {
          this.tableName = "";
        }
      } else {
        this.tableName = "";
      }
    }
    return this.tableName;
  }

  @Override
  public String getRowKeyDelimiter() {
    return (this.tableAnnotation != null && this.tableAnnotation.rowKeyDelimiter() != null)
        ? this.tableAnnotation.rowKeyDelimiter()
        : "#";
  }

  @Override
  public List<BigtablePersistentProperty> getRowKeyProperties() {
    List<BigtablePersistentProperty> sorted = new ArrayList<>(this.rowKeyProperties);
    sorted.sort(Comparator.comparingInt(BigtablePersistentProperty::getRowKeyOrder));
    return Collections.unmodifiableList(sorted);
  }

  @Override
  public boolean hasCompositeRowKey() {
    return this.rowKeyProperties.size() > 1;
  }

  @Override
  public List<BigtablePersistentProperty> getColumnProperties() {
    return Collections.unmodifiableList(this.columnProperties);
  }

  @Override
  public BigtablePersistentProperty getDynamicColumnsProperty() {
    return this.dynamicColumnsProperty;
  }

  @Override
  public boolean hasDynamicColumns() {
    return this.dynamicColumnsProperty != null;
  }

  @Override
  public void doWithColumnProperties(PropertyHandler<BigtablePersistentProperty> handler) {
    for (BigtablePersistentProperty property : this.columnProperties) {
      handler.doWithPersistentProperty(property);
    }
  }

  @Override
  public void doWithRowKeyProperties(PropertyHandler<BigtablePersistentProperty> handler) {
    for (BigtablePersistentProperty property : getRowKeyProperties()) {
      handler.doWithPersistentProperty(property);
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.evaluationContext.addPropertyAccessor(new BeanFactoryAccessor());
    this.evaluationContext.setBeanResolver(new BeanFactoryResolver(applicationContext));
    this.evaluationContext.setRootObject(applicationContext);
  }

  @Override
  public void verify() {
    super.verify();

    if (this.tableAnnotation == null) {
      throw new MappingException(
          "Entity class " + getType().getName() + " is missing required @BigtableTable annotation.");
    }

    String name = getTableName();
    if (!StringUtils.hasText(name)) {
      throw new MappingException(
          "Table name for entity " + getType().getName() + " cannot be empty.");
    }

    List<BigtablePersistentProperty> rowKeys = getRowKeyProperties();
    if (rowKeys.isEmpty()) {
      throw new MappingException(
          "Entity class " + getType().getName() + " must define at least one @RowKey property.");
    }

    Set<Integer> seenOrders = new HashSet<>();
    for (BigtablePersistentProperty rk : rowKeys) {
      int order = rk.getRowKeyOrder();
      if (!seenOrders.add(order)) {
        throw new MappingException(
            "Duplicate @RowKey order " + order + " in entity " + getType().getName()
                + " for property '" + rk.getName() + "'.");
      }
    }

    if (rowKeys.size() > 1) {
      int minOrder = rowKeys.get(0).getRowKeyOrder();
      for (int i = 0; i < rowKeys.size(); i++) {
        if (rowKeys.get(i).getRowKeyOrder() != minOrder + i) {
          throw new MappingException(
              "The @RowKey properties in " + getType().getName()
                  + " must have consecutive orders without gaps. Expected order "
                  + (minOrder + i) + " but found " + rowKeys.get(i).getRowKeyOrder()
                  + " for property '" + rowKeys.get(i).getName() + "'.");
        }
      }
      if (!StringUtils.hasLength(getRowKeyDelimiter())) {
        throw new MappingException(
            "Row key delimiter for entity " + getType().getName() + " cannot be empty.");
      }
    }

    Set<String> columnCoordinates = new HashSet<>();
    for (BigtablePersistentProperty prop : getColumnProperties()) {
      String coord = prop.getFamilyName() + ":" + prop.getColumnQualifier();
      if (!columnCoordinates.add(coord)) {
        throw new MappingException(
            "Duplicate column mapping detected in entity " + getType().getName()
                + " for family '" + prop.getFamilyName() + "' and qualifier '"
                + prop.getColumnQualifier() + "'.");
      }
    }

    List<BigtablePersistentProperty> dynamicProps = new ArrayList<>();
    doWithProperties((PropertyHandler<BigtablePersistentProperty>) prop -> {
      if (prop.isDynamicColumns()) {
        dynamicProps.add(prop);
      }
    });
    if (dynamicProps.size() > 1) {
      throw new MappingException(
          "Entity " + getType().getName() + " cannot have more than one @DynamicColumns property.");
    }
    if (!dynamicProps.isEmpty()) {
      String dynamicFamily = dynamicProps.get(0).getFamilyName();
      for (BigtablePersistentProperty colProp : getColumnProperties()) {
        if (dynamicFamily.equals(colProp.getFamilyName())) {
          throw new MappingException(
              "Family '" + dynamicFamily + "' in entity " + getType().getName()
                  + " cannot be used for both @DynamicColumns and @Column.");
        }
      }
    }
  }
}

/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jboss.subsystem;

import static org.modeshape.jboss.subsystem.ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;

/**
 * Attributes used in setting up ModeShape configurations. To mark an attribute as required, mark it as not allowing null.
 */
public class ModelAttributes {

    private static final ParameterValidator ROLE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, false, true) {
        @Override
        public void validateParameter( String parameterName,
                                       ModelNode value ) throws OperationFailedException {
            super.validateParameter(parameterName, value); // checks null
            String str = value.asString().toLowerCase();
            if (!StringUtil.isBlank(str) &&
                !ModeShapeRoles.ADMIN.equals(str) &&
                !ModeShapeRoles.READONLY.equals(str) &&
                !ModeShapeRoles.READWRITE.equals(str)) {
                throw new OperationFailedException("Invalid anonymous role name: '" + str + "'");
            }
        }
    };
    private static final ParameterValidator WORKSPACE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, false,
                                                                                              true);
    private static final ParameterValidator NODE_TYPE_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, false, true);
    private static final ParameterValidator INITIAL_CONTENT_VALIDATOR = new ModelTypeValidator(ModelType.PROPERTY, false, false,
                                                                                               true);
    private static final ParameterValidator DEFAULT_INITIAL_CONTENT_VALIDATOR = new ModelTypeValidator(ModelType.STRING, true,
                                                                                                       false, true);
    private static final ParameterValidator PATH_EXPRESSION_VALIDATOR = new PathExpressionValidator(false);

    private static final ParameterValidator PROJECTION_VALIDATOR = new ProjectionValidator(false);

    private static final ParameterValidator COLUMNS_VALIDATOR = new IndexColumnsValidator(false);

    private static final ParameterValidator INDEX_KIND_VALIDATOR = new EnumValidator<>(IndexKind.class, false, true);

    public static final SimpleAttributeDefinition ALLOW_WORKSPACE_CREATION = new MappedAttributeDefinitionBuilder(
                                                                                                                  ModelKeys.ALLOW_WORKSPACE_CREATION,
                                                                                                                  ModelType.BOOLEAN).setXmlName(Attribute.ALLOW_WORKSPACE_CREATION.getLocalName())
                                                                                                                                    .setAllowExpression(true)
                                                                                                                                    .setAllowNull(true)
                                                                                                                                    .setDefaultValue(new ModelNode().set(true))
                                                                                                                                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                    .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,
                                                                                                                                                                           FieldName.ALLOW_CREATION)
                                                                                                                                    .build();

    public static final SimpleAttributeDefinition WORKSPACES_CACHE_CONTAINER = new MappedAttributeDefinitionBuilder(
                                                                                                                    ModelKeys.WORKSPACES_CACHE_CONTAINER,
                                                                                                                    ModelType.STRING).setXmlName(Attribute.CACHE_CONTAINER.getLocalName())
                                                                                                                                     .setAllowExpression(true)
                                                                                                                                     .setAllowNull(true)
                                                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                     .build();

    public static final ListAttributeDefinition ANONYMOUS_ROLES = MappedListAttributeDefinition.Builder.of(ModelKeys.ANONYMOUS_ROLES,
                                                                                                           new MappedAttributeDefinitionBuilder(
                                                                                                                                                ModelKeys.ANONYMOUS_ROLE,
                                                                                                                                                ModelType.STRING).setAllowExpression(true)
                                                                                                                                                                 .setAllowNull(true)
                                                                                                                                                                 .setDefaultValue(new ModelNode().add(new ModelNode().set(ModeShapeRoles.READONLY)))
                                                                                                                                                                 .setValidator(ROLE_NAME_VALIDATOR)
                                                                                                                                                                 .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                                 .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                                                                                                                                 .build())
                                                                                                       .setAllowNull(true)
                                                                                                       .setMinSize(0)
                                                                                                       .setMaxSize(100)
                                                                                                       .setFieldPathInRepositoryConfiguration(
                                                                                                               FieldName.SECURITY,
                                                                                                               FieldName.ANONYMOUS,
                                                                                                               FieldName.ANONYMOUS_ROLES)
                                                                                                       .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                                                                       .build();

    public static final SimpleAttributeDefinition ANONYMOUS_USERNAME = new MappedAttributeDefinitionBuilder(
                                                                                                            ModelKeys.ANONYMOUS_USERNAME,
                                                                                                            ModelType.STRING).setXmlName(Attribute.ANONYMOUS_USERNAME.getLocalName())
                                                                                                                             .setAllowExpression(true)
                                                                                                                             .setAllowNull(true)
                                                                                                                             .setDefaultValue(new ModelNode().set("<anonymous>"))
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                             .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                                                                                             .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                                                    FieldName.ANONYMOUS,
                                                                                                                                                                    FieldName.ANONYMOUS_USERNAME)
                                                                                                                             .build();

    public static final SimpleAttributeDefinition AUTHENTICATOR_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                                 ModelKeys.AUTHENTICATOR_CLASSNAME,
                                                                                                                 ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                                  .setAllowExpression(false)
                                                                                                                                  .setAllowNull(true)
                                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                  .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                                                         FieldName.PROVIDERS,
                                                                                                                                                                         FieldName.CLASSNAME)
                                                                                                                                  .build();

    public static final SimpleAttributeDefinition CACHE_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.CACHE_NAME,
                                                                                                    ModelType.STRING).setXmlName(Attribute.CACHE_NAME.getLocalName())
                                                                                                                     .setAllowExpression(false)
                                                                                                                     .setAllowNull(true)
                                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                     .build();

    public static final SimpleAttributeDefinition CACHE_CONTAINER = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.CACHE_CONTAINER,
                                                                                                         ModelType.STRING).setXmlName(Attribute.CACHE_CONTAINER.getLocalName())
                                                                                                                          .setAllowExpression(false)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition CLASSNAME = new MappedAttributeDefinitionBuilder(ModelKeys.CLASSNAME,
                                                                                                   ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                    .setAllowExpression(false)
                                                                                                                    .setAllowNull(true)
                                                                                                                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                    .build();

    public static final SimpleAttributeDefinition DATA_CACHE_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.DATA_CACHE_NAME,
                                                                                                         ModelType.STRING).setXmlName(Attribute.DATA_CACHE_NAME.getLocalName())
                                                                                                                          .setAllowExpression(false)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition DATA_SOURCE_JNDI_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                               ModelKeys.DATA_SOURCE_JNDI_NAME,
                                                                                                               ModelType.STRING).setXmlName(Attribute.DATA_SOURCE_JNDI_NAME.getLocalName())
                                                                                                                                .setAllowExpression(true)
                                                                                                                                .setAllowNull(false)
                                                                                                                                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                                .build();

    public static final SimpleAttributeDefinition DEFAULT_WORKSPACE = new MappedAttributeDefinitionBuilder(
                                                                                                           ModelKeys.DEFAULT_WORKSPACE,
                                                                                                           ModelType.STRING).setXmlName(Attribute.DEFAULT_WORKSPACE.getLocalName())
                                                                                                                            .setAllowExpression(true)
                                                                                                                            .setAllowNull(true)
                                                                                                                            .setDefaultValue(new ModelNode().set("default"))
                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                            .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,
                                                                                                                                                                   FieldName.DEFAULT)
                                                                                                                            .build();

    public static final SimpleAttributeDefinition ENABLE_MONITORING = new MappedAttributeDefinitionBuilder(
                                                                                                           ModelKeys.ENABLE_MONITORING,
                                                                                                           ModelType.BOOLEAN).setXmlName(Attribute.ENABLE_MONITORING.getLocalName())
                                                                                                                             .setAllowNull(true)
                                                                                                                             .setAllowExpression(true)
                                                                                                                             .setDefaultValue(new ModelNode().set(true))
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                             .setFieldPathInRepositoryConfiguration(FieldName.MONITORING,
                                                                                                                                                                    FieldName.MONITORING_ENABLED)
                                                                                                                             .build();

    public static final SimpleAttributeDefinition GARBAGE_COLLECTION_THREAD_POOL = new MappedAttributeDefinitionBuilder(
                                                                                                                        ModelKeys.GARBAGE_COLLECTION_THREAD_POOL,
                                                                                                                        ModelType.STRING).setXmlName(Attribute.GARBAGE_COLLECTION_THREAD_POOL.getLocalName())
                                                                                                                                         .setAllowExpression(true)
                                                                                                                                         .setAllowNull(true)
                                                                                                                                         .setDefaultValue(new ModelNode().set("modeshape-gc"))
                                                                                                                                         .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                         .setFieldPathInRepositoryConfiguration(FieldName.GARBAGE_COLLECTION,
                                                                                                                                                                                FieldName.THREAD_POOL)
                                                                                                                                         .build();
    public static final SimpleAttributeDefinition GARBAGE_COLLECTION_INITIAL_TIME = new MappedAttributeDefinitionBuilder(
                                                                                                                         ModelKeys.GARBAGE_COLLECTION_INITIAL_TIME,
                                                                                                                         ModelType.STRING).setXmlName(Attribute.GARBAGE_COLLECTION_INITIAL_TIME.getLocalName())
                                                                                                                                          .setAllowExpression(true)
                                                                                                                                          .setAllowNull(true)
                                                                                                                                          .setDefaultValue(new ModelNode().set("00:00"))
                                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                          .setFieldPathInRepositoryConfiguration(FieldName.GARBAGE_COLLECTION,
                                                                                                                                                                                 FieldName.INITIAL_TIME)
                                                                                                                                          .build();
    public static final SimpleAttributeDefinition GARBAGE_COLLECTION_INTERVAL = new MappedAttributeDefinitionBuilder(
                                                                                                                     ModelKeys.GARBAGE_COLLECTION_INTERVAL,
                                                                                                                     ModelType.INT).setXmlName(Attribute.GARBAGE_COLLECTION_INTERVAL.getLocalName())
                                                                                                                                   .setAllowExpression(true)
                                                                                                                                   .setAllowNull(true)
                                                                                                                                   .setDefaultValue(new ModelNode().set(24))
                                                                                                                                   .setMeasurementUnit(MeasurementUnit.HOURS)
                                                                                                                                   .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                   .setFieldPathInRepositoryConfiguration(FieldName.GARBAGE_COLLECTION,
                                                                                                                                                                          FieldName.INTERVAL_IN_HOURS)
                                                                                                                                   .build();
    public static final SimpleAttributeDefinition DOCUMENT_OPTIMIZATION_THREAD_POOL = new MappedAttributeDefinitionBuilder(
                                                                                                                           ModelKeys.DOCUMENT_OPTIMIZATION_THREAD_POOL,
                                                                                                                           ModelType.STRING).setXmlName(Attribute.DOCUMENT_OPTIMIZATION_THREAD_POOL.getLocalName())
                                                                                                                                            .setAllowExpression(true)
                                                                                                                                            .setAllowNull(true)
                                                                                                                                            .setDefaultValue(new ModelNode().set("modeshape-opt"))
                                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                            .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                                   FieldName.DOCUMENT_OPTIMIZATION,
                                                                                                                                                                                   FieldName.THREAD_POOL)
                                                                                                                                            .build();
    public static final SimpleAttributeDefinition DOCUMENT_OPTIMIZATION_INITIAL_TIME = new MappedAttributeDefinitionBuilder(
                                                                                                                            ModelKeys.DOCUMENT_OPTIMIZATION_INITIAL_TIME,
                                                                                                                            ModelType.STRING).setXmlName(Attribute.DOCUMENT_OPTIMIZATION_INITIAL_TIME.getLocalName())
                                                                                                                                             .setAllowExpression(true)
                                                                                                                                             .setAllowNull(true)
                                                                                                                                             .setDefaultValue(new ModelNode().set("00:00"))
                                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                             .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                                    FieldName.DOCUMENT_OPTIMIZATION,
                                                                                                                                                                                    FieldName.INITIAL_TIME)
                                                                                                                                             .build();
    public static final SimpleAttributeDefinition DOCUMENT_OPTIMIZATION_INTERVAL = new MappedAttributeDefinitionBuilder(
                                                                                                                        ModelKeys.DOCUMENT_OPTIMIZATION_INTERVAL,
                                                                                                                        ModelType.INT).setXmlName(Attribute.DOCUMENT_OPTIMIZATION_INTERVAL.getLocalName())
                                                                                                                                      .setAllowExpression(true)
                                                                                                                                      .setAllowNull(true)
                                                                                                                                      .setDefaultValue(new ModelNode().set(24))
                                                                                                                                      .setMeasurementUnit(MeasurementUnit.HOURS)
                                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                      .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                             FieldName.DOCUMENT_OPTIMIZATION,
                                                                                                                                                                             FieldName.INTERVAL_IN_HOURS)
                                                                                                                                      .build();
    public static final SimpleAttributeDefinition DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET = new MappedAttributeDefinitionBuilder(
                                                                                                                                  ModelKeys.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET,
                                                                                                                                  ModelType.INT).setXmlName(Attribute.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET.getLocalName())
                                                                                                                                                .setAllowExpression(true)
                                                                                                                                                .setAllowNull(true)
                                                                                                                                                .setMeasurementUnit(MeasurementUnit.NONE)
                                                                                                                                                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                                       FieldName.DOCUMENT_OPTIMIZATION,
                                                                                                                                                                                       FieldName.OPTIMIZATION_CHILD_COUNT_TARGET)
                                                                                                                                                .build();
    public static final SimpleAttributeDefinition DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE = new MappedAttributeDefinitionBuilder(
                                                                                                                                     ModelKeys.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE,
                                                                                                                                     ModelType.INT).setXmlName(Attribute.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE.getLocalName())
                                                                                                                                                   .setAllowExpression(true)
                                                                                                                                                   .setAllowNull(true)
                                                                                                                                                   .setMeasurementUnit(MeasurementUnit.NONE)
                                                                                                                                                   .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                   .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                                          FieldName.DOCUMENT_OPTIMIZATION,
                                                                                                                                                                                          FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE)
                                                                                                                                                   .build();

    public static final SimpleAttributeDefinition INDEX_KIND = new MappedAttributeDefinitionBuilder(ModelKeys.INDEX_KIND,
                                                                                                    ModelType.STRING).setXmlName(Attribute.INDEX_KIND.getLocalName())
                                                                                                                     .setAllowExpression(true)
                                                                                                                     .setAllowNull(true)
                                                                                                                     .setDefaultValue(new ModelNode().set(IndexKind.DUPLICATES.toString()))
                                                                                                                     .setValidator(INDEX_KIND_VALIDATOR)
                                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                     .build();

    public static final SimpleAttributeDefinition INDEX_PROVIDER_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                                  ModelKeys.INDEX_PROVIDER_CLASSNAME,
                                                                                                                  ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                                   .setAllowExpression(false)
                                                                                                                                   .setAllowNull(true)
                                                                                                                                   .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                   .setFieldPathInRepositoryConfiguration(FieldName.INDEX_PROVIDERS,
                                                                                                                                                                          FieldName.CLASSNAME)
                                                                                                                                   .build();
    public static final SimpleAttributeDefinition JNDI_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.JNDI_NAME,
                                                                                                   ModelType.STRING).setXmlName(Attribute.JNDI_NAME.getLocalName())
                                                                                                                    .setAllowExpression(false)
                                                                                                                    .setAllowNull(true)
                                                                                                                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                    .build();

    public static final SimpleAttributeDefinition LOCK_CACHE_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.LOCK_CACHE_NAME,
                                                                                                         ModelType.STRING).setXmlName(Attribute.LOCK_CACHE_NAME.getLocalName())
                                                                                                                          .setAllowExpression(false)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition METADATA_CACHE_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.METADATA_CACHE_NAME,
                                                                                                             ModelType.STRING).setXmlName(Attribute.META_CACHE_NAME.getLocalName())
                                                                                                                              .setAllowExpression(false)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                              .build();
    public static final SimpleAttributeDefinition CHUNK_SIZE = new MappedAttributeDefinitionBuilder(ModelKeys.CHUNK_SIZE,
                                                                                                    ModelType.INT).setXmlName(Attribute.CHUNK_SIZE.getLocalName())
                                                                                                                  .setAllowExpression(false)
                                                                                                                  .setAllowNull(true)
                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                  .build();

    public static final SimpleAttributeDefinition MINIMUM_BINARY_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.MINIMUM_BINARY_SIZE,
                                                                                                             ModelType.INT).setXmlName(Attribute.MIN_VALUE_SIZE.getLocalName())
                                                                                                                           .setAllowExpression(false)
                                                                                                                           .setAllowNull(true)
                                                                                                                           .setMeasurementUnit(MeasurementUnit.BYTES)
                                                                                                                           .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                           .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                  FieldName.BINARY_STORAGE,
                                                                                                                                                                  FieldName.MINIMUM_BINARY_SIZE_IN_BYTES)
                                                                                                                           .build();

    public static final SimpleAttributeDefinition MINIMUM_STRING_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.MINIMUM_STRING_SIZE,
                                                                                                             ModelType.INT).setXmlName(Attribute.MIN_STRING_SIZE.getLocalName())
                                                                                                                           .setAllowExpression(false)
                                                                                                                           .setAllowNull(true)
                                                                                                                           .setMeasurementUnit(MeasurementUnit.NONE)
                                                                                                                           .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                           .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,
                                                                                                                                                                  FieldName.BINARY_STORAGE,
                                                                                                                                                                  FieldName.MINIMUM_STRING_SIZE)
                                                                                                                           .build();

    public static final SimpleAttributeDefinition MODULE = new MappedAttributeDefinitionBuilder(ModelKeys.MODULE,
                                                                                                ModelType.STRING).setXmlName(Attribute.MODULE.getLocalName())
                                                                                                                 .setAllowExpression(false)
                                                                                                                 .setAllowNull(true)
                                                                                                                 .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                 .build();

    public static final SimpleAttributeDefinition NAME = new MappedAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING).setXmlName(Attribute.NAME.getLocalName())
                                                                                                                               .setAllowExpression(false)
                                                                                                                               .setAllowNull(false)
                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                               .build();

    public static final SimpleAttributeDefinition NODE_TYPE_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.NODE_TYPE_NAME,
                                                                                                        ModelType.STRING).setXmlName(Attribute.NODE_TYPE.getLocalName())
                                                                                                                         .setAllowExpression(true)
                                                                                                                         .setAllowNull(false)
                                                                                                                         .setValidator(NODE_TYPE_VALIDATOR)
                                                                                                                         .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                         .build();

    public static final SimpleAttributeDefinition INDEX_COLUMNS = new MappedAttributeDefinitionBuilder(ModelKeys.INDEX_COLUMNS,
                                                                                                       ModelType.STRING).setXmlName(Attribute.COLUMNS.getLocalName())
                                                                                                                        .setAllowExpression(true)
                                                                                                                        .setAllowNull(false)
                                                                                                                        .setValidator(COLUMNS_VALIDATOR)
                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                        .build();

    public static final SimpleAttributeDefinition PROVIDER_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.PROVIDER_NAME,
                                                                                                       ModelType.STRING).setXmlName(Attribute.PROVIDER_NAME.getLocalName())
                                                                                                                        .setAllowExpression(true)
                                                                                                                        .setAllowNull(true)
                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                        .build();

    public static final SimpleAttributeDefinition PATH = new MappedAttributeDefinitionBuilder(ModelKeys.PATH, ModelType.STRING).setXmlName(Attribute.PATH.getLocalName())
                                                                                                                               .setAllowExpression(true)
                                                                                                                               .setAllowNull(true)
                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                               .build();

    public static final ListAttributeDefinition PATH_EXPRESSIONS = MappedListAttributeDefinition.Builder.of(ModelKeys.PATH_EXPRESSIONS,
                                                                                                            new MappedAttributeDefinitionBuilder(
                                                                                                                                                 ModelKeys.PATH_EXPRESSION,
                                                                                                                                                 ModelType.STRING).setAllowExpression(true)
                                                                                                                                                                  .setAllowNull(false)
                                                                                                                                                                  .setValidator(PATH_EXPRESSION_VALIDATOR)
                                                                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                                  .build())
                                                                                                        .setAllowNull(false)
                                                                                                        .setMinSize(1)
                                                                                                        .setFieldPathInRepositoryConfiguration(FieldName.SEQUENCING,
                                                                                                                                               FieldName.SEQUENCERS,
                                                                                                                                               FieldName.PATH_EXPRESSIONS)
                                                                                                        .build();

    public static final ListAttributeDefinition PROJECTIONS = MappedListAttributeDefinition.Builder.of(ModelKeys.PROJECTIONS,
                                                                                                       new MappedAttributeDefinitionBuilder(
                                                                                                                                            ModelKeys.PROJECTION,
                                                                                                                                            ModelType.STRING).setAllowExpression(true)
                                                                                                                                                             .setAllowNull(false)
                                                                                                                                                             .setValidator(PROJECTION_VALIDATOR)
                                                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                             .build())
                                                                                                   .setAllowNull(true)
                                                                                                   .setMinSize(1)
                                                                                                   .build();

    public static final SimpleAttributeDefinition CONNECTOR_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.CONNECTOR_CLASSNAME,
                                                                                                             ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                              .setAllowExpression(false)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                              .build();

    public static final SimpleAttributeDefinition CACHE_TTL_SECONDS = new MappedAttributeDefinitionBuilder(
                                                                                                           ModelKeys.CACHE_TTL_SECONDS,
                                                                                                           ModelType.INT).setXmlName(Attribute.CACHE_TTL_SECONDS.getLocalName())
                                                                                                                         .setAllowExpression(false)
                                                                                                                         .setAllowNull(true)
                                                                                                                         .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                         .build();

    public static final SimpleAttributeDefinition QUERYABLE = new MappedAttributeDefinitionBuilder(ModelKeys.QUERYABLE,
                                                                                                   ModelType.BOOLEAN).setXmlName(Attribute.QUERYABLE.getLocalName())
                                                                                                                     .setAllowExpression(false)
                                                                                                                     .setAllowNull(true)
                                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                     .build();

    public static final SimpleAttributeDefinition READONLY = new MappedAttributeDefinitionBuilder(ModelKeys.READONLY,
                                                                                                  ModelType.BOOLEAN).setXmlName(Attribute.READONLY.getLocalName())
                                                                                                                    .setAllowExpression(false)
                                                                                                                    .setAllowNull(true)
                                                                                                                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                    .setDefaultValue(new ModelNode(
                                                                                                                                                   false))
                                                                                                                    .build();

    public static final ListAttributeDefinition PREDEFINED_WORKSPACE_NAMES = MappedListAttributeDefinition.Builder.of(ModelKeys.PREDEFINED_WORKSPACE_NAMES,
                                                                                                                      new MappedAttributeDefinitionBuilder(
                                                                                                                                                           ModelKeys.PREDEFINED_WORKSPACE_NAME,
                                                                                                                                                           ModelType.STRING).setAllowExpression(true)
                                                                                                                                                                            .setAllowNull(false)
                                                                                                                                                                            .setValidator(WORKSPACE_NAME_VALIDATOR)
                                                                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                                            .build())
                                                                                                                  .setAllowNull(true)
                                                                                                                  .setMinSize(0)
                                                                                                                  .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,
                                                                                                                                                         FieldName.PREDEFINED)
                                                                                                                  .build();

    public static final SimpleAttributeDefinition DEFAULT_INITIAL_CONTENT = new MappedAttributeDefinitionBuilder(
                                                                                                                 ModelKeys.DEFAULT_INITIAL_CONTENT,
                                                                                                                 ModelType.STRING).setAllowExpression(false)
                                                                                                                                  .setAllowNull(true)
                                                                                                                                  .setValidator(DEFAULT_INITIAL_CONTENT_VALIDATOR)
                                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                  .build();

    public static final ListAttributeDefinition WORKSPACES_INITIAL_CONTENT = MappedListAttributeDefinition.Builder.of(ModelKeys.WORKSPACES_INITIAL_CONTENT,
                                                                                                                      new MappedAttributeDefinitionBuilder(
                                                                                                                                                           ModelKeys.INITIAL_CONTENT,
                                                                                                                                                           ModelType.PROPERTY).setAllowNull(false)
                                                                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                                              .setValidator(INITIAL_CONTENT_VALIDATOR)
                                                                                                                                                                              .build())
                                                                                                                  .setAllowNull(true)
                                                                                                                  .setMinSize(0)
                                                                                                                  .build();

    public static final ListAttributeDefinition NODE_TYPES = MappedListAttributeDefinition.Builder.of(ModelKeys.NODE_TYPES,
                                                                                                      new MappedAttributeDefinitionBuilder(
                                                                                                                                           ModelKeys.NODE_TYPE,
                                                                                                                                           ModelType.STRING).setAllowExpression(true)
                                                                                                                                                            .setAllowNull(false)
                                                                                                                                                            .setValidator(NODE_TYPE_VALIDATOR)
                                                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                            .build())
                                                                                                  .setAllowNull(true)
                                                                                                  .setMinSize(0)
                                                                                                  .build();

    public static final SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY,
                                                                                           ModelType.PROPERTY, true);
    public static final SimpleListAttributeDefinition PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES,
                                                                                                            PROPERTY)
                                                                                                        .setAllowNull(true)
                                                                                                        .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new MappedAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO,
                                                                                                     ModelType.STRING).setXmlName(Attribute.RELATIVE_TO.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode().set(JBOSS_DATA_DIR_VARIABLE))
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                      .build();

    public static final SimpleAttributeDefinition SEQUENCER_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.SEQUENCER_CLASSNAME,
                                                                                                             ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                              .setAllowExpression(false)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                              .setFieldPathInRepositoryConfiguration(FieldName.SEQUENCING,
                                                                                                                                                                     FieldName.SEQUENCERS,
                                                                                                                                                                     FieldName.CLASSNAME)
                                                                                                                              .build();
    public static final SimpleAttributeDefinition STORE_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.STORE_NAME,
                                                                                                    ModelType.STRING).setXmlName(Attribute.STORE_NAME.getLocalName())
                                                                                                                     .setAllowExpression(false)
                                                                                                                     .setAllowNull(true)
                                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                     .build();

    public static final ListAttributeDefinition NESTED_STORES = MappedListAttributeDefinition.Builder.of(ModelKeys.NESTED_STORES,
                                                                                                         new MappedAttributeDefinitionBuilder(
                                                                                                                                              ModelKeys.STORE_NAME,
                                                                                                                                              ModelType.STRING).setAllowExpression(false)
                                                                                                                                                               .setAllowNull(false)
                                                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                               .build())
                                                                                                     .setAllowNull(false)
                                                                                                     .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                     .build();

    public static final SimpleAttributeDefinition TEXT_EXTRACTOR_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                                  ModelKeys.TEXT_EXTRACTOR_CLASSNAME,
                                                                                                                  ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                                   .setAllowExpression(false)
                                                                                                                                   .setAllowNull(true)
                                                                                                                                   .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                   .setFieldPathInRepositoryConfiguration(FieldName.TEXT_EXTRACTION,
                                                                                                                                                                          FieldName.EXTRACTORS,
                                                                                                                                                                          FieldName.CLASSNAME)
                                                                                                                                   .build();

    public static final SimpleAttributeDefinition SECURITY_DOMAIN = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.SECURITY_DOMAIN,
                                                                                                         ModelType.STRING).setXmlName(Attribute.SECURITY_DOMAIN.getLocalName())
                                                                                                                          .setAllowExpression(true)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setDefaultValue(new ModelNode().set("modeshape-security"))
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                          .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                                                                                          .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                                                 FieldName.JAAS,
                                                                                                                                                                 FieldName.JAAS_POLICY_NAME)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition SOURCE_PATH = new MappedAttributeDefinitionBuilder(ModelKeys.SOURCE_PATH,
                                                                                                     ModelType.STRING).setXmlName(Attribute.SOURCE_PATH.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(false)
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                      .build();

    public static final SimpleAttributeDefinition SOURCE_RELATIVE_TO = new MappedAttributeDefinitionBuilder(
                                                                                                            ModelKeys.SOURCE_RELATIVE_TO,
                                                                                                            ModelType.STRING).setXmlName(Attribute.SOURCE_RELATIVE_TO.getLocalName())
                                                                                                                             .setAllowExpression(true)
                                                                                                                             .setAllowNull(true)
                                                                                                                             .setDefaultValue(new ModelNode().set(JBOSS_DATA_DIR_VARIABLE))
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                             .build();

    public static final SimpleAttributeDefinition USE_ANONYMOUS_IF_AUTH_FAILED = new MappedAttributeDefinitionBuilder(
                                                                                                                      ModelKeys.USE_ANONYMOUS_IF_AUTH_FAILED,
                                                                                                                      ModelType.BOOLEAN).setXmlName(Attribute.USE_ANONYMOUS_IF_AUTH_FAILED.getLocalName())
                                                                                                                                        .setAllowExpression(true)
                                                                                                                                        .setAllowNull(true)
                                                                                                                                        .setDefaultValue(new ModelNode().set(false))
                                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                                                                                                        .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                                                               FieldName.ANONYMOUS,
                                                                                                                                                                               FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS)
                                                                                                                                        .build();

    public static final SimpleAttributeDefinition EXPLODED = new MappedAttributeDefinitionBuilder(ModelKeys.EXPLODED,
                                                                                                  ModelType.BOOLEAN).setXmlName(Attribute.EXPLODED.getLocalName())
                                                                                                                    .setAllowExpression(false)
                                                                                                                    .setAllowNull(true)
                                                                                                                    .setDefaultValue(new ModelNode().set(false))
                                                                                                                    .build();

    public static final SimpleAttributeDefinition JOURNALING = new MappedAttributeDefinitionBuilder(ModelKeys.JOURNALING,
                                                                                                    ModelType.BOOLEAN).setXmlName(Attribute.JOURNALING.getLocalName())
                                                                                                                      .setAllowExpression(false)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode(
                                                                                                                                                     false))
                                                                                                                      .build();

    public static final SimpleAttributeDefinition JOURNAL_PATH = new MappedAttributeDefinitionBuilder(ModelKeys.JOURNAL_PATH,
                                                                                                      ModelType.STRING).setXmlName(Attribute.JOURNAL_PATH.getLocalName())
                                                                                                                       .setAllowExpression(true)
                                                                                                                       .setAllowNull(true)
                                                                                                                       .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                       .build();

    public static final SimpleAttributeDefinition JOURNAL_RELATIVE_TO = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.JOURNAL_RELATIVE_TO,
                                                                                                             ModelType.STRING).setXmlName(Attribute.JOURNAL_RELATIVE_TO.getLocalName())
                                                                                                                              .setAllowExpression(true)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                              .build();

    public static final SimpleAttributeDefinition MAX_DAYS_TO_KEEP_RECORDS = new MappedAttributeDefinitionBuilder(
                                                                                                                  ModelKeys.MAX_DAYS_TO_KEEP_RECORDS,
                                                                                                                  ModelType.INT).setXmlName(Attribute.MAX_DAYS_TO_KEEP_RECORDS.getLocalName())
                                                                                                                                .setAllowExpression(false)
                                                                                                                                .setAllowNull(true)
                                                                                                                                .setDefaultValue(new ModelNode(
                                                                                                                                                               -1))
                                                                                                                                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                                .build();

    public static final SimpleAttributeDefinition ASYNC_WRITES = new MappedAttributeDefinitionBuilder(ModelKeys.ASYNC_WRITES,
                                                                                                      ModelType.BOOLEAN).setXmlName(Attribute.ASYNC_WRITES.getLocalName())
                                                                                                                        .setAllowExpression(false)
                                                                                                                        .setAllowNull(true)
                                                                                                                        .setDefaultValue(new ModelNode(
                                                                                                                                                       false))
                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                        .build();

    public static final SimpleAttributeDefinition JOURNAL_GC_THREAD_POOL = new MappedAttributeDefinitionBuilder(
                                                                                                                ModelKeys.JOURNAL_GC_THREAD_POOL,
                                                                                                                ModelType.STRING).setXmlName(Attribute.JOURNAL_GC_THREAD_POOL.getLocalName())
                                                                                                                                 .setAllowExpression(true)
                                                                                                                                 .setAllowNull(true)
                                                                                                                                 .setDefaultValue(new ModelNode().set("modeshape-journaling-gc"))
                                                                                                                                 .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                 .build();

    public static final SimpleAttributeDefinition JOURNAL_GC_INITIAL_TIME = new MappedAttributeDefinitionBuilder(
                                                                                                                 ModelKeys.JOURNAL_GC_INITIAL_TIME,
                                                                                                                 ModelType.STRING).setXmlName(Attribute.JOURNAL_GC_INITIAL_TIME.getLocalName())
                                                                                                                                  .setAllowExpression(true)
                                                                                                                                  .setAllowNull(true)
                                                                                                                                  .setDefaultValue(new ModelNode().set("00:00"))
                                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                  .build();

    public static final AttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {};

    public static final AttributeDefinition[] WEBAPP_ATTRIBUTES = {EXPLODED};

    public static final AttributeDefinition[] REPOSITORY_ATTRIBUTES = {CACHE_NAME, CACHE_CONTAINER, JNDI_NAME, ENABLE_MONITORING,
        SECURITY_DOMAIN, ANONYMOUS_ROLES, ANONYMOUS_USERNAME, USE_ANONYMOUS_IF_AUTH_FAILED, NODE_TYPES, DEFAULT_WORKSPACE,
        PREDEFINED_WORKSPACE_NAMES, ALLOW_WORKSPACE_CREATION, WORKSPACES_CACHE_CONTAINER, DEFAULT_INITIAL_CONTENT,
        WORKSPACES_INITIAL_CONTENT, MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE, GARBAGE_COLLECTION_THREAD_POOL,
        GARBAGE_COLLECTION_INITIAL_TIME, GARBAGE_COLLECTION_INTERVAL, DOCUMENT_OPTIMIZATION_THREAD_POOL,
        DOCUMENT_OPTIMIZATION_INITIAL_TIME, DOCUMENT_OPTIMIZATION_INTERVAL, DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET,
        DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE, JOURNAL_PATH, JOURNAL_RELATIVE_TO, MAX_DAYS_TO_KEEP_RECORDS,
        JOURNAL_GC_INITIAL_TIME, JOURNAL_GC_THREAD_POOL, ASYNC_WRITES, JOURNALING};

    public static final AttributeDefinition[] FILE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE, PATH,
        RELATIVE_TO, STORE_NAME};

    public static final AttributeDefinition[] CACHE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        CHUNK_SIZE, DATA_CACHE_NAME, METADATA_CACHE_NAME, CACHE_CONTAINER, STORE_NAME};

    public static final AttributeDefinition[] DATABASE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        DATA_SOURCE_JNDI_NAME, STORE_NAME};

    public static final AttributeDefinition[] COMPOSITE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        NESTED_STORES};

    public static final AttributeDefinition[] CUSTOM_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        CLASSNAME, MODULE, STORE_NAME};

    public static final AttributeDefinition[] INDEX_DEFINITION_ATTRIBUTES = {INDEX_KIND, PROVIDER_NAME, NODE_TYPE_NAME,
        INDEX_COLUMNS, PROPERTIES};

    public static final AttributeDefinition[] INDEX_PROVIDER_ATTRIBUTES = {CLASSNAME, MODULE, PROPERTIES};

    public static final AttributeDefinition[] SEQUENCER_ATTRIBUTES = {PATH_EXPRESSIONS, SEQUENCER_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] SOURCE_ATTRIBUTES = {PROJECTIONS, CONNECTOR_CLASSNAME, READONLY, CACHE_TTL_SECONDS,
        QUERYABLE, MODULE, PROPERTIES};
    public static final AttributeDefinition[] TEXT_EXTRACTOR_ATTRIBUTES = {TEXT_EXTRACTOR_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] AUTHENTICATOR_ATTRIBUTES = {AUTHENTICATOR_CLASSNAME, MODULE, PROPERTIES};
}

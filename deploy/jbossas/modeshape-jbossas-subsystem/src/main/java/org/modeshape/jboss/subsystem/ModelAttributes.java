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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FileSystemAccessType;
import org.modeshape.jcr.RepositoryConfiguration.FileSystemLockingStrategy;
import org.modeshape.jcr.RepositoryConfiguration.IndexReaderStrategy;
import org.modeshape.jcr.RepositoryConfiguration.IndexingMode;
import org.modeshape.jcr.RepositoryConfiguration.QueryRebuild;

/**
 * Attributes used in setting up ModeShape configurations. To mark an attribute as required, mark it as not allowing null.
 */
public class ModelAttributes {

    private static final ParameterValidator ROLE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, false, true) {
        @Override
        public void validateParameter( String parameterName,
                                       ModelNode value ) throws OperationFailedException {
            super.validateParameter(parameterName, value); // checks null
            String str = value.asString();
            if (!ModeShapeRoles.ADMIN.equals(str) && !ModeShapeRoles.READONLY.equals(str)
                && !ModeShapeRoles.READWRITE.equals(str)) {
                throw new OperationFailedException("Invalid anonymous role name: '" + str + "'");
            }
        }
    };
    private static final ParameterValidator INDEX_STORAGE_TYPE_VALIDATOR = new ModelTypeValidator(ModelType.STRING,
                                                                                                  false,
                                                                                                  false,
                                                                                                  true) {
        @Override
        public void validateParameter( String parameterName,
                                       ModelNode value ) throws OperationFailedException {
            super.validateParameter(parameterName, value); // checks null
            String str = value.asString();
            if (!ModelKeys.RAM_INDEX_STORAGE.equals(str) && !ModelKeys.LOCAL_FILE_INDEX_STORAGE.equals(str)
                && !ModelKeys.MASTER_FILE_INDEX_STORAGE.equals(str) && !ModelKeys.SLAVE_FILE_INDEX_STORAGE.equals(str)
                && !ModelKeys.CUSTOM_INDEX_STORAGE.equals(str)) {
                throw new OperationFailedException("Invalid index storage type: '" + str + "'");
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
    private static final ParameterValidator INDEX_FORMAT_VALIDATOR = new RegexValidator("LUCENE_(3[0-9]{1,2}|CURRENT)", true);
    private static final ParameterValidator REBUILD_INDEXES_VALIDATOR = new EnumValidator<QueryRebuild>(QueryRebuild.class,
                                                                                                        false, true);
    private static final ParameterValidator ACCESS_TYPE_VALIDATOR = new EnumValidator<FileSystemAccessType>(
                                                                                                            FileSystemAccessType.class,
                                                                                                            false, true);
    private static final ParameterValidator LOCKING_STRATEGY_VALIDATOR = new EnumValidator<FileSystemLockingStrategy>(
                                                                                                                      FileSystemLockingStrategy.class,
                                                                                                                      false, true);
    private static final ParameterValidator READER_STRATEGY_VALIDATOR = new EnumValidator<IndexReaderStrategy>(
                                                                                                               IndexReaderStrategy.class,
                                                                                                               false, true);
    private static final ParameterValidator INDEXING_MODE_VALIDATOR = new EnumValidator<IndexingMode>(IndexingMode.class, false,
                                                                                                      true);
    private static final ParameterValidator PATH_EXPRESSION_VALIDATOR = new PathExpressionValidator(false);

    private static final ParameterValidator PROJECTION_VALIDATOR = new ProjectionValidator(false);

    public static final SimpleAttributeDefinition ACCESS_TYPE = new MappedAttributeDefinitionBuilder(ModelKeys.ACCESS_TYPE,
                                                                                                     ModelType.STRING).setXmlName(Attribute.ACCESS_TYPE.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode().set(FileSystemAccessType.AUTO.toString()))
                                                                                                                      .setValidator(ACCESS_TYPE_VALIDATOR)
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                      .build();

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

    public static final SimpleAttributeDefinition ANALYZER_CLASSNAME = new MappedAttributeDefinitionBuilder(
                                                                                                            ModelKeys.ANALYZER_CLASSNAME,
                                                                                                            ModelType.STRING).setXmlName(Attribute.ANALYZER_CLASSNAME.getLocalName())
                                                                                                                             .setAllowExpression(false)
                                                                                                                             .setAllowNull(true)
                                                                                                                             .setDefaultValue(new ModelNode().set(StandardAnalyzer.class.getName()))
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                             .build();

    public static final SimpleAttributeDefinition ANALYZER_MODULE = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.ANALYZER_MODULE,
                                                                                                         ModelType.STRING).setXmlName(Attribute.ANALYZER_MODULE.getLocalName())
                                                                                                                          .setAllowExpression(false)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final ListAttributeDefinition ANONYMOUS_ROLES = MappedListAttributeDefinition.Builder.of(ModelKeys.ANONYMOUS_ROLES,
                                                                                                           new MappedAttributeDefinitionBuilder(
                                                                                                                                                ModelKeys.ANONYMOUS_ROLE,
                                                                                                                                                ModelType.STRING).setAllowExpression(true)
                                                                                                                                                                 .setAllowNull(true)
                                                                                                                                                                 .setDefaultValue(new ModelNode().add(new ModelNode().set(ModeShapeRoles.ADMIN))
                                                                                                                                                                                                 .add(new ModelNode().set(ModeShapeRoles.READONLY))
                                                                                                                                                                                                 .add(new ModelNode().set(ModeShapeRoles.READWRITE)))
                                                                                                                                                                 .setValidator(ROLE_NAME_VALIDATOR)
                                                                                                                                                                 .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                                 .build())
                                                                                                       .setAllowNull(true)
                                                                                                       .setMinSize(0)
                                                                                                       .setMaxSize(100)
                                                                                                       .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                              FieldName.ANONYMOUS,
                                                                                                                                              FieldName.ANONYMOUS_ROLES)
                                                                                                       .build();

    public static final SimpleAttributeDefinition ANONYMOUS_USERNAME = new MappedAttributeDefinitionBuilder(
                                                                                                            ModelKeys.ANONYMOUS_USERNAME,
                                                                                                            ModelType.STRING).setXmlName(Attribute.ANONYMOUS_USERNAME.getLocalName())
                                                                                                                             .setAllowExpression(true)
                                                                                                                             .setAllowNull(true)
                                                                                                                             .setDefaultValue(new ModelNode().set("<anonymous>"))
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                             .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,
                                                                                                                                                                    FieldName.ANONYMOUS,
                                                                                                                                                                    FieldName.ANONYMOUS_USERNAME)
                                                                                                                             .build();

    public static final SimpleAttributeDefinition ASYNC_MAX_QUEUE_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                              ModelKeys.ASYNC_MAX_QUEUE_SIZE,
                                                                                                              ModelType.INT).setXmlName(Attribute.ASYNC_MAX_QUEUE_SIZE.getLocalName())
                                                                                                                            .setAllowExpression(true)
                                                                                                                            .setAllowNull(true)
                                                                                                                            .setDefaultValue(new ModelNode().set(1))
                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                            .build();

    public static final SimpleAttributeDefinition ASYNC_THREAD_POOL_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                                ModelKeys.ASYNC_THREAD_POOL_SIZE,
                                                                                                                ModelType.INT).setXmlName(Attribute.ASYNC_THREAD_POOL_SIZE.getLocalName())
                                                                                                                              .setAllowExpression(true)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setDefaultValue(new ModelNode().set(1))
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
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

    public static final SimpleAttributeDefinition BATCH_SIZE = new MappedAttributeDefinitionBuilder(ModelKeys.BATCH_SIZE,
                                                                                                    ModelType.INT).setXmlName(Attribute.BATCH_SIZE.getLocalName())
                                                                                                                  .setAllowExpression(true)
                                                                                                                  .setAllowNull(true)
                                                                                                                  .setDefaultValue(new ModelNode().set(-1))
                                                                                                                  .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
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

    public static final SimpleAttributeDefinition CLUSTER_NAME = new MappedAttributeDefinitionBuilder(ModelKeys.CLUSTER_NAME,
                                                                                                      ModelType.STRING).setXmlName(Attribute.CLUSTER_NAME.getLocalName())
                                                                                                                       .setAllowExpression(true)
                                                                                                                       .setAllowNull(true)
                                                                                                                       .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                       .build();

    public static final SimpleAttributeDefinition CLUSTER_STACK = new MappedAttributeDefinitionBuilder(ModelKeys.CLUSTER_STACK,
                                                                                                       ModelType.STRING).setXmlName(Attribute.CLUSTER_STACK.getLocalName())
                                                                                                                        .setAllowExpression(true)
                                                                                                                        .setAllowNull(true)
                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                        .build();

    public static final SimpleAttributeDefinition CLASSNAME = new MappedAttributeDefinitionBuilder(ModelKeys.CLASSNAME,
                                                                                                   ModelType.STRING).setXmlName(Attribute.CLASSNAME.getLocalName())
                                                                                                                    .setAllowExpression(false)
                                                                                                                    .setAllowNull(true)
                                                                                                                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                    .build();

    public static final SimpleAttributeDefinition CONNECTION_FACTORY_JNDI_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                                      ModelKeys.CONNECTION_FACTORY_JNDI_NAME,
                                                                                                                      ModelType.STRING).setXmlName(Attribute.CONNECTION_FACTORY_JNDI_NAME.getLocalName())
                                                                                                                                       .setAllowExpression(true)
                                                                                                                                       .setAllowNull(true)
                                                                                                                                       .setDefaultValue(new ModelNode().set("/ConnectionFactory"))
                                                                                                                                       .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                                       .build();

    public static final SimpleAttributeDefinition COPY_BUFFER_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                          ModelKeys.COPY_BUFFER_SIZE,
                                                                                                          ModelType.INT).setXmlName(Attribute.COPY_BUFFER_SIZE.getLocalName())
                                                                                                                        .setAllowExpression(true)
                                                                                                                        .setAllowNull(true)
                                                                                                                        .setDefaultValue(new ModelNode().set(16))
                                                                                                                        .setMeasurementUnit(MeasurementUnit.MEGABYTES)
                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                        .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                               FieldName.INDEX_STORAGE,
                                                                                                                                                               FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES)
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

    public static final SimpleAttributeDefinition ENABLE_QUERIES = new MappedAttributeDefinitionBuilder(ModelKeys.ENABLE_QUERIES,
                                                                                                        ModelType.BOOLEAN).setXmlName(Attribute.ENABLE_QUERIES.getLocalName())
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setAllowExpression(true)
                                                                                                                          .setDefaultValue(new ModelNode().set(true))
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                          .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                                 FieldName.QUERY_ENABLED)
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
    public static final SimpleAttributeDefinition INDEX_FORMAT = new MappedAttributeDefinitionBuilder(ModelKeys.INDEX_FORMAT,
                                                                                                      ModelType.STRING).setXmlName(Attribute.FORMAT.getLocalName())
                                                                                                                       .setAllowExpression(true)
                                                                                                                       .setAllowNull(true)
                                                                                                                       .setValidator(INDEX_FORMAT_VALIDATOR)
                                                                                                                       .setDefaultValue(new ModelNode().set("LUCENE_CURRENT"))
                                                                                                                       .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                       .build();

    public static final SimpleAttributeDefinition INDEX_STORAGE_TYPE = new MappedAttributeDefinitionBuilder(
                                                                                                            ModelKeys.INDEX_STORAGE_TYPE,
                                                                                                            ModelType.STRING).setAllowExpression(false)
                                                                                                                             .setAllowNull(false)
                                                                                                                             .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                             .setDefaultValue(new ModelNode().set(ModelKeys.LOCAL_FILE_INDEX_STORAGE))
                                                                                                                             .setValidator(INDEX_STORAGE_TYPE_VALIDATOR)
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

    public static final SimpleAttributeDefinition LOCKING_STRATEGY = new MappedAttributeDefinitionBuilder(
                                                                                                          ModelKeys.LOCKING_STRATEGY,
                                                                                                          ModelType.STRING).setXmlName(Attribute.LOCKING_STRATEGY.getLocalName())
                                                                                                                           .setAllowExpression(true)
                                                                                                                           .setAllowNull(true)
                                                                                                                           .setDefaultValue(new ModelNode().set(FileSystemLockingStrategy.NATIVE.toString()))
                                                                                                                           .setValidator(LOCKING_STRATEGY_VALIDATOR)
                                                                                                                           .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                           .build();

    public static final SimpleAttributeDefinition METADATA_CACHE_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.METADATA_CACHE_NAME,
                                                                                                             ModelType.STRING).setXmlName(Attribute.META_CACHE_NAME.getLocalName())
                                                                                                                              .setAllowExpression(false)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                              .build();
    public static final SimpleAttributeDefinition CHUNK_SIZE = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.CHUNK_SIZE,
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

    public static final SimpleAttributeDefinition MODE = new MappedAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING).setXmlName(Attribute.MODE.getLocalName())
                                                                                                                               .setAllowExpression(true)
                                                                                                                               .setAllowNull(true)
                                                                                                                               .setDefaultValue(new ModelNode().set(IndexingMode.SYNC.toString()))
                                                                                                                               .setValidator(INDEXING_MODE_VALIDATOR)
                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                               .build();

    /**
     * @deprecated use the REBUILD_INDEXES_UPON_STARTUP,REBUILD_INDEXES_UPON_STARTUP_MODE,
     *             REBUILD_INDEXES_UPON_INCLUDE_SYSTEM_CONTENT attributes
     */
    @Deprecated
    public static final SimpleAttributeDefinition SYSTEM_CONTENT_MODE = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.SYSTEM_CONTENT_MODE,
                                                                                                             ModelType.STRING).setXmlName(Attribute.SYSTEM_CONTENT_MODE.getLocalName())
                                                                                                                              .setAllowExpression(true)
                                                                                                                              .setAllowNull(true)
                                                                                                                              .setDefaultValue(new ModelNode().set(IndexingMode.DISABLED.toString()))
                                                                                                                              .setValidator(INDEXING_MODE_VALIDATOR)
                                                                                                                              .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
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
                                                                                                            (AttributeDefinition)PROPERTY)
                                                                                                        .setAllowNull(true)
                                                                                                        .build();
    public static final SimpleAttributeDefinition QUEUE_JNDI_NAME = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.QUEUE_JNDI_NAME,
                                                                                                         ModelType.STRING).setXmlName(Attribute.QUEUE_JNDI_NAME.getLocalName())
                                                                                                                          .setAllowExpression(true)
                                                                                                                          .setAllowNull(false)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition READER_STRATEGY = new MappedAttributeDefinitionBuilder(
                                                                                                         ModelKeys.READER_STRATEGY,
                                                                                                         ModelType.STRING).setXmlName(Attribute.READER_STRATEGY.getLocalName())
                                                                                                                          .setAllowExpression(true)
                                                                                                                          .setAllowNull(true)
                                                                                                                          .setDefaultValue(new ModelNode().set(IndexReaderStrategy.SHARED.toString()))
                                                                                                                          .setValidator(READER_STRATEGY_VALIDATOR)
                                                                                                                          .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                          .build();

    public static final SimpleAttributeDefinition REBUILD_INDEXES_UPON_STARTUP = new MappedAttributeDefinitionBuilder(
                                                                                                                      ModelKeys.REBUILD_INDEXES_UPON_STARTUP,
                                                                                                                      ModelType.STRING).setXmlName(Attribute.REBUILD_UPON_STARTUP.getLocalName())
                                                                                                                                       .setAllowExpression(true)
                                                                                                                                       .setAllowNull(true)
                                                                                                                                       .setDefaultValue(new ModelNode().set(QueryRebuild.IF_MISSING.toString()))
                                                                                                                                       .setValidator(REBUILD_INDEXES_VALIDATOR)
                                                                                                                                       .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                       .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                                              FieldName.INDEXING,
                                                                                                                                                                              FieldName.REBUILD_ON_STARTUP,
                                                                                                                                                                              FieldName.REBUILD_WHEN)
                                                                                                                                       .build();

    public static final SimpleAttributeDefinition REBUILD_INDEXES_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT = new MappedAttributeDefinitionBuilder(
                                                                                                                                             ModelKeys.REBUILD_INDEXES_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT,
                                                                                                                                             ModelType.BOOLEAN).setXmlName(Attribute.REBUILD_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT.getLocalName())
                                                                                                                                                               .setAllowExpression(false)
                                                                                                                                                               .setAllowNull(true)
                                                                                                                                                               .setDefaultValue(new ModelNode().set(false))
                                                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                                               .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                                                                      FieldName.INDEXING,
                                                                                                                                                                                                      FieldName.REBUILD_ON_STARTUP,
                                                                                                                                                                                                      FieldName.REBUILD_INCLUDE_SYSTEM_CONTENT)
                                                                                                                                                               .build();

    public static final SimpleAttributeDefinition REBUILD_INDEXES_UPON_STARTUP_MODE = new MappedAttributeDefinitionBuilder(
                                                                                                                           ModelKeys.REBUILD_INDEXES_UPON_STARTUP_MODE,
                                                                                                                           ModelType.STRING).setXmlName(Attribute.REBUILD_UPON_STARTUP_MODE.getLocalName())
                                                                                                                                            .setAllowExpression(true)
                                                                                                                                            .setAllowNull(true)
                                                                                                                                            .setDefaultValue(new ModelNode().set(IndexingMode.ASYNC.toString()))
                                                                                                                                            .setValidator(INDEXING_MODE_VALIDATOR)
                                                                                                                                            .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                                            .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                                                   FieldName.INDEXING,
                                                                                                                                                                                   FieldName.REBUILD_ON_STARTUP,
                                                                                                                                                                                   FieldName.REBUILD_MODE)
                                                                                                                                            .build();

    public static final SimpleAttributeDefinition REFRESH_PERIOD = new MappedAttributeDefinitionBuilder(ModelKeys.REFRESH_PERIOD,
                                                                                                        ModelType.INT).setXmlName(Attribute.REFRESH_PERIOD.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode().set(3600))
                                                                                                                      .setMeasurementUnit(MeasurementUnit.SECONDS)
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                                                                                      .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                             FieldName.INDEX_STORAGE,
                                                                                                                                                             FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS)
                                                                                                                      .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new MappedAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO,
                                                                                                     ModelType.STRING).setXmlName(Attribute.RELATIVE_TO.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode().set(JBOSS_DATA_DIR_VARIABLE))
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                      .build();

    public static final SimpleAttributeDefinition RETRY_INITIALIZE_PERIOD = new MappedAttributeDefinitionBuilder(
                                                                                                                 ModelKeys.RETRY_INITIALIZE_PERIOD,
                                                                                                                 ModelType.INT).setXmlName(Attribute.RETRY_INIT_PERIOD.getLocalName())
                                                                                                                               .setAllowExpression(true)
                                                                                                                               .setAllowNull(true)
                                                                                                                               .setDefaultValue(new ModelNode().set(0))
                                                                                                                               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                               .build();

    public static final SimpleAttributeDefinition RETRY_MARKER_LOOKUP = new MappedAttributeDefinitionBuilder(
                                                                                                             ModelKeys.RETRY_MARKER_LOOKUP,
                                                                                                             ModelType.INT).setXmlName(Attribute.RETRY_MARKER_LOOKUP.getLocalName())
                                                                                                                           .setAllowExpression(true)
                                                                                                                           .setAllowNull(true)
                                                                                                                           .setDefaultValue(new ModelNode().set(0))
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
                                                                                                                                   .setFieldPathInRepositoryConfiguration(FieldName.QUERY,
                                                                                                                                                                          FieldName.TEXT_EXTRACTING,
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

    public static final SimpleAttributeDefinition THREAD_POOL = new MappedAttributeDefinitionBuilder(ModelKeys.THREAD_POOL,
                                                                                                     ModelType.STRING).setXmlName(Attribute.THREAD_POOL.getLocalName())
                                                                                                                      .setAllowExpression(true)
                                                                                                                      .setAllowNull(true)
                                                                                                                      .setDefaultValue(new ModelNode().set("modeshape-indexing-workers"))
                                                                                                                      .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                                                                                      .build();

    public static final SimpleAttributeDefinition USE_ANONYMOUS_IF_AUTH_FAILED = new MappedAttributeDefinitionBuilder(
                                                                                                                      ModelKeys.USE_ANONYMOUS_IF_AUTH_FAILED,
                                                                                                                      ModelType.BOOLEAN).setXmlName(Attribute.USE_ANONYMOUS_IF_AUTH_FAILED.getLocalName())
                                                                                                                                        .setAllowExpression(true)
                                                                                                                                        .setAllowNull(true)
                                                                                                                                        .setDefaultValue(new ModelNode().set(false))
                                                                                                                                        .setFlags(AttributeAccess.Flag.RESTART_NONE)
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
                                                                                                    ModelType.BOOLEAN)
            .setXmlName(Attribute.JOURNALING.getLocalName())
            .setAllowExpression(false)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final SimpleAttributeDefinition JOURNAL_PATH = new MappedAttributeDefinitionBuilder(ModelKeys.JOURNAL_PATH,
                                                                                                      ModelType.STRING)
            .setXmlName(Attribute.JOURNAL_PATH.getLocalName())
            .setAllowExpression(true)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition JOURNAL_RELATIVE_TO = new MappedAttributeDefinitionBuilder(ModelKeys.JOURNAL_RELATIVE_TO,
                                                                                                             ModelType.STRING)
            .setXmlName(Attribute.JOURNAL_RELATIVE_TO.getLocalName())
            .setAllowExpression(true)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MAX_DAYS_TO_KEEP_RECORDS = new MappedAttributeDefinitionBuilder(ModelKeys.MAX_DAYS_TO_KEEP_RECORDS,
                                                                                                                  ModelType.INT)
            .setXmlName(Attribute.MAX_DAYS_TO_KEEP_RECORDS.getLocalName())
            .setAllowExpression(false)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(-1))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ASYNC_WRITES = new MappedAttributeDefinitionBuilder(ModelKeys.ASYNC_WRITES,
                                                                                                      ModelType.BOOLEAN)
            .setXmlName(Attribute.ASYNC_WRITES.getLocalName())
            .setAllowExpression(false)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
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
        ENABLE_QUERIES, SECURITY_DOMAIN, ANONYMOUS_ROLES, ANONYMOUS_USERNAME, USE_ANONYMOUS_IF_AUTH_FAILED, NODE_TYPES,
        DEFAULT_WORKSPACE, PREDEFINED_WORKSPACE_NAMES, ALLOW_WORKSPACE_CREATION, WORKSPACES_CACHE_CONTAINER,
        DEFAULT_INITIAL_CONTENT, WORKSPACES_INITIAL_CONTENT, MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE, THREAD_POOL, BATCH_SIZE,
        READER_STRATEGY, MODE, SYSTEM_CONTENT_MODE, ASYNC_THREAD_POOL_SIZE, ASYNC_MAX_QUEUE_SIZE, ANALYZER_CLASSNAME,
        ANALYZER_MODULE, REBUILD_INDEXES_UPON_STARTUP, REBUILD_INDEXES_UPON_STARTUP_MODE,
        REBUILD_INDEXES_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT, CLUSTER_NAME, CLUSTER_STACK, GARBAGE_COLLECTION_THREAD_POOL,
        GARBAGE_COLLECTION_INITIAL_TIME, GARBAGE_COLLECTION_INTERVAL, DOCUMENT_OPTIMIZATION_THREAD_POOL,
        DOCUMENT_OPTIMIZATION_INITIAL_TIME, DOCUMENT_OPTIMIZATION_INTERVAL, DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET,
        DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE, JOURNAL_PATH, JOURNAL_RELATIVE_TO, MAX_DAYS_TO_KEEP_RECORDS, JOURNAL_GC_INITIAL_TIME,
        JOURNAL_GC_THREAD_POOL, ASYNC_WRITES, JOURNALING};

    public static final AttributeDefinition[] RAM_INDEX_STORAGE_ATTRIBUTES = {INDEX_STORAGE_TYPE,};

    public static final AttributeDefinition[] LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES = {INDEX_STORAGE_TYPE, INDEX_FORMAT, PATH,
        RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY,};

    public static final AttributeDefinition[] MASTER_FILE_INDEX_STORAGE_ATTRIBUTES = {INDEX_STORAGE_TYPE, INDEX_FORMAT, PATH,
        RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY, REFRESH_PERIOD, SOURCE_PATH, SOURCE_RELATIVE_TO,
        CONNECTION_FACTORY_JNDI_NAME, QUEUE_JNDI_NAME,};

    public static final AttributeDefinition[] SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES = {INDEX_STORAGE_TYPE, INDEX_FORMAT, PATH,
        RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY, REFRESH_PERIOD, SOURCE_PATH, SOURCE_RELATIVE_TO,
        CONNECTION_FACTORY_JNDI_NAME, QUEUE_JNDI_NAME, COPY_BUFFER_SIZE, RETRY_MARKER_LOOKUP, RETRY_INITIALIZE_PERIOD};

    public static final AttributeDefinition[] CUSTOM_INDEX_STORAGE_ATTRIBUTES = {INDEX_STORAGE_TYPE, INDEX_FORMAT, CLASSNAME,
        MODULE,};

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

    public static final AttributeDefinition[] SEQUENCER_ATTRIBUTES = {PATH_EXPRESSIONS, SEQUENCER_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] SOURCE_ATTRIBUTES = {PROJECTIONS, CONNECTOR_CLASSNAME, READONLY, CACHE_TTL_SECONDS,
        QUERYABLE, MODULE, PROPERTIES};
    public static final AttributeDefinition[] TEXT_EXTRACTOR_ATTRIBUTES = {TEXT_EXTRACTOR_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] AUTHENTICATOR_ATTRIBUTES = {AUTHENTICATOR_CLASSNAME, MODULE, PROPERTIES};
}

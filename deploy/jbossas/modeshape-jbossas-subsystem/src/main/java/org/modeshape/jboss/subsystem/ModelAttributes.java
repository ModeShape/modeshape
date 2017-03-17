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
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.persistence.file.FileDbProvider;
import org.modeshape.persistence.relational.RelationalDbConfig;

/**
 * Attributes used in setting up ModeShape configurations. To mark an attribute as required, mark it as not allowing null.
 */
public class ModelAttributes {

    private static final ParameterValidator ROLE_NAME_VALIDATOR = new StringSetValidator(false,
                                                                                         false,
                                                                                         "",
                                                                                         ModeShapeRoles.ADMIN,
                                                                                         ModeShapeRoles.READONLY,
                                                                                         ModeShapeRoles.READWRITE);
    private static final ParameterValidator WORKSPACE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, false,
                                                                                              true);
    private static final ParameterValidator NODE_TYPE_VALIDATOR = new ModelTypeValidator(ModelType.STRING, false, true, true);
    private static final ParameterValidator INITIAL_CONTENT_VALIDATOR = new ModelTypeValidator(ModelType.PROPERTY, false, true,
                                                                                               true);
    private static final ParameterValidator DEFAULT_INITIAL_CONTENT_VALIDATOR = new ModelTypeValidator(ModelType.STRING, true,
                                                                                                       false, true);
    private static final ParameterValidator PATH_EXPRESSION_VALIDATOR = new PathExpressionValidator(false);

    private static final ParameterValidator PROJECTION_VALIDATOR = new ProjectionValidator(false);

    private static final ParameterValidator COLUMNS_VALIDATOR = new IndexColumnsValidator(false);

    private static final ParameterValidator INDEX_KIND_VALIDATOR = new StringSetValidator(true,
                                                                                          true,
                                                                                          RepositoryConfiguration.FieldValue.KIND_ENUMERATED,
                                                                                          RepositoryConfiguration.FieldValue.KIND_NODE_TYPE,
                                                                                          RepositoryConfiguration.FieldValue.KIND_TEXT,
                                                                                          RepositoryConfiguration.FieldValue.KIND_UNIQUE,
                                                                                          RepositoryConfiguration.FieldValue.KIND_VALUE);
    private static final ParameterValidator CLUSTER_LOCKING_VALIDATOR = new StringSetValidator(true,
                                                                                               true,
                                                                                               RepositoryConfiguration.FieldValue.LOCKING_JGROUPS,
                                                                                               RepositoryConfiguration.FieldValue.LOCKING_DB);
    private static final ParameterValidator REINDEXING_MODE_VALIDATOR = new StringSetValidator(true,
                                                                                          true,
                                                                                          RepositoryConfiguration.ReindexingMode.IF_MISSING.name(),
                                                                                          RepositoryConfiguration.ReindexingMode.INCREMENTAL.name());
    private static final ParameterValidator MIME_TYPE_DETECTION_VALIDATOR = new StringSetValidator(true,
                                                                                          true,
                                                                                          RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_CONTENT,
                                                                                          RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_NAME,
                                                                                          RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_NONE);

    public static final SimpleAttributeDefinition ALLOW_WORKSPACE_CREATION =
            new MappedAttributeDefinitionBuilder(ModelKeys.ALLOW_WORKSPACE_CREATION, ModelType.BOOLEAN,
                                                 FieldName.WORKSPACES,
                                                 FieldName.ALLOW_CREATION)
                    .setXmlName(Attribute.ALLOW_WORKSPACE_CREATION.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition WORKSPACES_CACHE_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.WORKSPACES_CACHE_SIZE, ModelType.INT)
                    .setXmlName(Attribute.CACHE_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setValidator(new IntRangeValidator(1))
                    .build();

    public static final ListAttributeDefinition ANONYMOUS_ROLES =
            MappedListAttributeDefinition.Builder.of(ModelKeys.ANONYMOUS_ROLES,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.ANONYMOUS_ROLE,
                                                                                          ModelType.STRING)
                                                             .setAllowExpression(true)
                                                             .setAllowNull(true)
                                                             .setDefaultValue(new ModelNode().add(new ModelNode().set(
                                                                             ModeShapeRoles.READONLY)))
                                                             .setValidator(ROLE_NAME_VALIDATOR)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .setAccessConstraints(
                                                                     SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setMinSize(0)
                                                 .setMaxSize(100)
                                                 .setFieldPathInRepositoryConfiguration(
                                                         FieldName.SECURITY,
                                                         FieldName.ANONYMOUS,
                                                         FieldName.ANONYMOUS_ROLES)
                                                 .setAccessConstraints(
                                                         SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                                                 .build();

    public static final SimpleAttributeDefinition ANONYMOUS_USERNAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.ANONYMOUS_USERNAME, ModelType.STRING, 
                                                 FieldName.SECURITY, FieldName.ANONYMOUS, FieldName.ANONYMOUS_USERNAME)
                    .setXmlName(Attribute.ANONYMOUS_USERNAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("<anonymous>"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .build();

    public static final MappedSimpleAttributeDefinition AUTHENTICATOR_CLASSNAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.AUTHENTICATOR_CLASSNAME, ModelType.STRING, 
                                                 FieldName.SECURITY, FieldName.PROVIDERS, FieldName.CLASSNAME)
                    .setXmlName(Attribute.CLASSNAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition CLASSNAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLASSNAME, ModelType.STRING)
                    .setXmlName(Attribute.CLASSNAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition REPOSITORY_MODULE_DEPENDENCIES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.REPOSITORY_MODULE_DEPENDENCIES,
                                                 ModelType.STRING)
                    .setXmlName(Attribute.REPOSITORY_MODULE_DEPENDENCIES.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition DATA_SOURCE_JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DATA_SOURCE_JNDI_NAME, ModelType.STRING)
                    .setXmlName(Attribute.DATA_SOURCE_JNDI_NAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition DEFAULT_WORKSPACE =
            new MappedAttributeDefinitionBuilder(
                    ModelKeys.DEFAULT_WORKSPACE, ModelType.STRING, FieldName.WORKSPACES, FieldName.DEFAULT)
                    .setXmlName(Attribute.DEFAULT_WORKSPACE.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("default"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition ENABLE_MONITORING =
            new MappedAttributeDefinitionBuilder(
                    ModelKeys.ENABLE_MONITORING, ModelType.BOOLEAN, FieldName.MONITORING, FieldName.MONITORING_ENABLED)
                    .setXmlName(Attribute.ENABLE_MONITORING.getLocalName())
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition CLUSTER_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER_NAME, ModelType.STRING)
                    .setXmlName(Attribute.CLUSTER_NAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition CLUSTER_STACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER_STACK, ModelType.STRING)
                    .setXmlName(Attribute.CLUSTER_STACK.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition CLUSTER_CONFIG =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER_CONFIG, ModelType.STRING)
                    .setXmlName(Attribute.CLUSTER_CONFIG.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final SimpleAttributeDefinition CLUSTER_LOCKING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER_LOCKING, ModelType.STRING)
                    .setXmlName(Attribute.CLUSTER_LOCKING.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(CLUSTER_LOCKING_VALIDATOR)
                    .build();

    public static final MappedSimpleAttributeDefinition GARBAGE_COLLECTION_THREAD_POOL =
            new MappedAttributeDefinitionBuilder(ModelKeys.GARBAGE_COLLECTION_THREAD_POOL, ModelType.STRING, 
                                                 FieldName.GARBAGE_COLLECTION, FieldName.THREAD_POOL)
                    .setXmlName(Attribute.GARBAGE_COLLECTION_THREAD_POOL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("modeshape-gc"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
   
    public static final MappedSimpleAttributeDefinition GARBAGE_COLLECTION_INITIAL_TIME =
            new MappedAttributeDefinitionBuilder(ModelKeys.GARBAGE_COLLECTION_INITIAL_TIME, ModelType.STRING,
                                                 FieldName.GARBAGE_COLLECTION, FieldName.INITIAL_TIME)
                    .setXmlName(Attribute.GARBAGE_COLLECTION_INITIAL_TIME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("00:00"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    
    public static final MappedSimpleAttributeDefinition GARBAGE_COLLECTION_INTERVAL =
            new MappedAttributeDefinitionBuilder(ModelKeys.GARBAGE_COLLECTION_INTERVAL, ModelType.INT,
                                                 FieldName.GARBAGE_COLLECTION, FieldName.INTERVAL_IN_HOURS)
                    .setXmlName(Attribute.GARBAGE_COLLECTION_INTERVAL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(24))
                    .setMeasurementUnit(MeasurementUnit.HOURS)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition DOCUMENT_OPTIMIZATION_THREAD_POOL =
            new MappedAttributeDefinitionBuilder(ModelKeys.DOCUMENT_OPTIMIZATION_THREAD_POOL, ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.DOCUMENT_OPTIMIZATION, FieldName.THREAD_POOL)
                    .setXmlName(Attribute.DOCUMENT_OPTIMIZATION_THREAD_POOL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("modeshape-opt"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    
    public static final MappedSimpleAttributeDefinition DOCUMENT_OPTIMIZATION_INITIAL_TIME =
            new MappedAttributeDefinitionBuilder(ModelKeys.DOCUMENT_OPTIMIZATION_INITIAL_TIME, ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.DOCUMENT_OPTIMIZATION, FieldName.INITIAL_TIME)
                    .setXmlName(Attribute.DOCUMENT_OPTIMIZATION_INITIAL_TIME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("00:00"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition DOCUMENT_OPTIMIZATION_INTERVAL =
            new MappedAttributeDefinitionBuilder(ModelKeys.DOCUMENT_OPTIMIZATION_INTERVAL, ModelType.INT,
                                                 FieldName.STORAGE, FieldName.DOCUMENT_OPTIMIZATION, FieldName.INTERVAL_IN_HOURS)
                    .setXmlName(Attribute.DOCUMENT_OPTIMIZATION_INTERVAL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(24))
                    .setMeasurementUnit(MeasurementUnit.HOURS)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET =
            new MappedAttributeDefinitionBuilder(ModelKeys.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET, ModelType.INT,
                                                 FieldName.STORAGE, FieldName.DOCUMENT_OPTIMIZATION, FieldName.OPTIMIZATION_CHILD_COUNT_TARGET)
                    .setXmlName(Attribute.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE =
            new MappedAttributeDefinitionBuilder(ModelKeys.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE, ModelType.INT,
                                                 FieldName.STORAGE, FieldName.DOCUMENT_OPTIMIZATION, 
                                                 FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE)
                    .setXmlName(Attribute.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition EVENT_BUS_SIZE = 
            new MappedAttributeDefinitionBuilder(ModelKeys.EVENT_BUS_SIZE, ModelType.INT,  FieldName.EVENT_BUS_SIZE)
                    .setXmlName(Attribute.EVENT_BUS_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    
    public static final MappedSimpleAttributeDefinition LOCK_TIMEOUT_MILLIS = 
            new MappedAttributeDefinitionBuilder(ModelKeys.LOCK_TIMEOUT_MILLIS, ModelType.INT, FieldName.LOCK_TIMEOUT_MILLIS)
                    .setXmlName(Attribute.LOCK_TIMEOUT_MILLIS.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition REINDEXING_ASYNC =
            new MappedAttributeDefinitionBuilder(ModelKeys.REINDEXING_ASYNC, ModelType.BOOLEAN, 
                                                 FieldName.REINDEXING, FieldName.REINDEXING_ASYNC)
                    .setXmlName(Attribute.REINDEXING_ASNC.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition REINDEXING_MODE = 
            new MappedAttributeDefinitionBuilder(ModelKeys.REINDEXING_MODE, ModelType.STRING, 
                                                 FieldName.REINDEXING, FieldName.REINDEXING_MODE)
                    .setXmlName(Attribute.REINDEXING_MODE.getLocalName())
                    .setAllowExpression(false)
                    .setValidator(REINDEXING_MODE_VALIDATOR)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition INDEX_KIND =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INDEX_KIND, ModelType.STRING)
                    .setXmlName(Attribute.INDEX_KIND.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(IndexKind.VALUE.toString()))
                    .setValidator(INDEX_KIND_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition SYNCHRONOUS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SYNCHRONOUS, ModelType.BOOLEAN)
                    .setXmlName(Attribute.SYNCHRONOUS.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(Boolean.TRUE))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING)
                    .setXmlName(Attribute.JNDI_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition MINIMUM_BINARY_SIZE =
            new MappedAttributeDefinitionBuilder(ModelKeys.MINIMUM_BINARY_SIZE, ModelType.INT,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE,
                                                 FieldName.MINIMUM_BINARY_SIZE_IN_BYTES)
                    .setXmlName(Attribute.MIN_VALUE_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.BYTES)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition MINIMUM_STRING_SIZE =
            new MappedAttributeDefinitionBuilder(ModelKeys.MINIMUM_STRING_SIZE, ModelType.INT,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.MINIMUM_STRING_SIZE)
                    .setXmlName(Attribute.MIN_STRING_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition MIME_TYPE_DETECTION =
            new MappedAttributeDefinitionBuilder(ModelKeys.MIME_TYPE_DETECTION, ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.MIMETYPE_DETECTION)
                    .setXmlName(Attribute.MIME_TYPE_DETECTION.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setMeasurementUnit(MeasurementUnit.NONE)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setValidator(MIME_TYPE_DETECTION_VALIDATOR)
                    .build();

    public static final SimpleAttributeDefinition MODULE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING)
                    .setXmlName(Attribute.MODULE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition NODE_TYPE_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.NODE_TYPE_NAME, ModelType.STRING)
                    .setXmlName(Attribute.NODE_TYPE.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode("nt:base"))
                    .setValidator(NODE_TYPE_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition INDEX_COLUMNS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INDEX_COLUMNS, ModelType.STRING)
                    .setXmlName(Attribute.COLUMNS.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setValidator(COLUMNS_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition PROVIDER_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PROVIDER_NAME, ModelType.STRING)
                    .setXmlName(Attribute.PROVIDER_NAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition WORKSPACES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.WORKSPACES, ModelType.STRING)
                    .setXmlName(Attribute.WORKSPACES.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PATH, ModelType.STRING)
                    .setXmlName(Attribute.PATH.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition TRASH =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TRASH, ModelType.STRING)
                    .setXmlName(Attribute.TRASH.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final ListAttributeDefinition PATH_EXPRESSIONS =
            MappedListAttributeDefinition.Builder.of(ModelKeys.PATH_EXPRESSIONS,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.PATH_EXPRESSION,
                                                                                          ModelType.STRING)
                                                             .setAllowExpression(true)
                                                             .setAllowNull(false)
                                                             .setValidator(PATH_EXPRESSION_VALIDATOR)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setMinSize(0)
                                                 .setFieldPathInRepositoryConfiguration(FieldName.SEQUENCING,
                                                                                        FieldName.SEQUENCERS,
                                                                                        FieldName.PATH_EXPRESSIONS)
                                                 .build();

    public static final ListAttributeDefinition PROJECTIONS =
            MappedListAttributeDefinition.Builder.of(ModelKeys.PROJECTIONS,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.PROJECTION,
                                                                                          ModelType.STRING)
                                                             .setAllowExpression(true)
                                                             .setAllowNull(false)
                                                             .setValidator(PROJECTION_VALIDATOR)
                                                             .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setMinSize(1)
                                                 .build();

    public static final SimpleAttributeDefinition CONNECTOR_CLASSNAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CONNECTOR_CLASSNAME, ModelType.STRING)
                    .setXmlName(Attribute.CLASSNAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition CACHEABLE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CACHEABLE, ModelType.BOOLEAN)
                    .setXmlName(Attribute.CACHEABLE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition QUERYABLE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.QUERYABLE, ModelType.BOOLEAN)
                    .setXmlName(Attribute.QUERYABLE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition READONLY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.READONLY, ModelType.BOOLEAN)
                    .setXmlName(Attribute.READONLY.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    public static final SimpleAttributeDefinition EXPOSE_AS_WORKSPACE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.EXPOSE_AS_WORKSPACE, ModelType.STRING)
                    .setXmlName(Attribute.EXPOSE_AS_WORKSPACE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final ListAttributeDefinition PREDEFINED_WORKSPACE_NAMES =
            MappedListAttributeDefinition.Builder.of(ModelKeys.PREDEFINED_WORKSPACE_NAMES,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.PREDEFINED_WORKSPACE_NAME,
                                                                                          ModelType.STRING)
                                                             .setAllowExpression(true)
                                                             .setAllowNull(false)
                                                             .setValidator(WORKSPACE_NAME_VALIDATOR)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setMinSize(0)
                                                 .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,
                                                                                        FieldName.PREDEFINED)
                                                 .build();

    public static final SimpleAttributeDefinition DEFAULT_INITIAL_CONTENT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_INITIAL_CONTENT, ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setValidator(DEFAULT_INITIAL_CONTENT_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final ListAttributeDefinition WORKSPACES_INITIAL_CONTENT =
            MappedListAttributeDefinition.Builder.of(ModelKeys.WORKSPACES_INITIAL_CONTENT,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.INITIAL_CONTENT, 
                                                                                          ModelType.PROPERTY)
                                                             .setAllowNull(false)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .setValidator(INITIAL_CONTENT_VALIDATOR)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setMinSize(0)
                                                 .build();

    public static final ListAttributeDefinition NODE_TYPES =
            MappedListAttributeDefinition.Builder.of(ModelKeys.NODE_TYPES,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.NODE_TYPE, ModelType.STRING)
                                                             .setAllowExpression(true)
                                                             .setAllowNull(false)
                                                             .setValidator(NODE_TYPE_VALIDATOR)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .build())
                                                 .setAllowNull(true)
                                                 .setAllowExpression(true)
                                                 .setMinSize(0)
                                                 .build();

    public static final SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, 
                                                                                           ModelType.PROPERTY, true);
    public static final SimpleListAttributeDefinition PROPERTIES =
            SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES, PROPERTY)
                                                 .setAllowNull(true)
                                                 .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                                                 .build();

    public static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO, ModelType.STRING)
                    .setXmlName(Attribute.RELATIVE_TO.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(JBOSS_DATA_DIR_VARIABLE))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition SEQUENCER_CLASSNAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.SEQUENCER_CLASSNAME, ModelType.STRING,
                                                 FieldName.SEQUENCING, FieldName.SEQUENCERS, FieldName.CLASSNAME)
                    .setXmlName(Attribute.CLASSNAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition SEQUENCER_THREAD_POOL_NAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.SEQUENCERS_THREAD_POOL_NAME, ModelType.STRING,
                                                 FieldName.SEQUENCING, FieldName.SEQUENCERS, FieldName.THREAD_POOL)
                    .setXmlName(Attribute.THREAD_POOL_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setDefaultValue(new ModelNode().set(RepositoryConfiguration.Default.SEQUENCING_POOL))
                    .build();

    public static final MappedSimpleAttributeDefinition SEQUENCER_MAX_POOL_SIZE =
            new MappedAttributeDefinitionBuilder(ModelKeys.SEQUENCERS_MAX_POOL_SIZE, ModelType.STRING,
                                                 FieldName.SEQUENCING, FieldName.SEQUENCERS, FieldName.MAX_POOL_SIZE)
                    .setXmlName(Attribute.MAX_POOL_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setDefaultValue(new ModelNode().set(RepositoryConfiguration.Default.SEQUENCING_MAX_POOL_SIZE))
                    .build();

    public static final SimpleAttributeDefinition STORE_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STORE_NAME, ModelType.STRING)
                    .setXmlName(Attribute.STORE_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final ListAttributeDefinition NESTED_STORES =
            MappedListAttributeDefinition.Builder.of(ModelKeys.NESTED_STORES,
                                                     new SimpleAttributeDefinitionBuilder(ModelKeys.STORE_NAME,
                                                                                          ModelType.STRING)
                                                             .setAllowExpression(false)
                                                             .setAllowNull(false)
                                                             .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                             .build())
                                                 .setAllowNull(false)
                                                 .setFlags(AttributeAccess.Flag.RESTART_NONE)
                                                 .build();

    public static final MappedSimpleAttributeDefinition TEXT_EXTRACTOR_CLASSNAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.TEXT_EXTRACTOR_CLASSNAME, ModelType.STRING,
                                                 FieldName.TEXT_EXTRACTION, FieldName.EXTRACTORS, FieldName.CLASSNAME)
                    .setXmlName(Attribute.CLASSNAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition TEXT_EXTRACTOR_THREAD_POOL_NAME =
            new MappedAttributeDefinitionBuilder(ModelKeys.TEXT_EXTRACTORS_THREAD_POOL_NAME, ModelType.STRING,
                                                 FieldName.TEXT_EXTRACTION, FieldName.EXTRACTORS, FieldName.THREAD_POOL)
                    .setXmlName(Attribute.THREAD_POOL_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setDefaultValue(new ModelNode().set(RepositoryConfiguration.Default.TEXT_EXTRACTION_POOL))
                    .build();

    public static final MappedSimpleAttributeDefinition TEXT_EXTRACTOR_MAX_POOL_SIZE =
            new MappedAttributeDefinitionBuilder(ModelKeys.TEXT_EXTRACTORS_MAX_POOL_SIZE, ModelType.STRING,
                                                 FieldName.TEXT_EXTRACTION, FieldName.EXTRACTORS, FieldName.MAX_POOL_SIZE                                                 )
                    .setXmlName(Attribute.MAX_POOL_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setDefaultValue(new ModelNode().set(RepositoryConfiguration.Default.TEXT_EXTRACTION_MAX_POOL_SIZE))
                    .build();


    public static final MappedSimpleAttributeDefinition SECURITY_DOMAIN =
            new MappedAttributeDefinitionBuilder(ModelKeys.SECURITY_DOMAIN, ModelType.STRING,
                                                 FieldName.SECURITY, FieldName.JAAS, FieldName.JAAS_POLICY_NAME)
                    .setXmlName(Attribute.SECURITY_DOMAIN.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("modeshape-security"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .build();

    public static final MappedSimpleAttributeDefinition USE_ANONYMOUS_IF_AUTH_FAILED =
            new MappedAttributeDefinitionBuilder(ModelKeys.USE_ANONYMOUS_IF_AUTH_FAILED, ModelType.BOOLEAN,
                                                 FieldName.SECURITY, FieldName.ANONYMOUS,
                                                 FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS)
                    .setXmlName(Attribute.USE_ANONYMOUS_IF_AUTH_FAILED.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(false))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .build();

    public static final SimpleAttributeDefinition EXPLODED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.EXPLODED, ModelType.BOOLEAN)
                    .setXmlName(Attribute.EXPLODED.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();

    public static final SimpleAttributeDefinition JOURNALING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNALING, ModelType.BOOLEAN)
                    .setXmlName(Attribute.JOURNALING.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    public static final SimpleAttributeDefinition JOURNAL_PATH =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNAL_PATH, ModelType.STRING)
                    .setXmlName(Attribute.JOURNAL_PATH.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition JOURNAL_ENABLED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNAL_ENABLED, ModelType.BOOLEAN)
                    .setXmlName(Attribute.JOURNAL_ENABLED.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition JOURNAL_RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNAL_RELATIVE_TO, ModelType.STRING)
                    .setXmlName(Attribute.JOURNAL_RELATIVE_TO.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition MAX_DAYS_TO_KEEP_RECORDS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MAX_DAYS_TO_KEEP_RECORDS, ModelType.INT)
                    .setXmlName(Attribute.MAX_DAYS_TO_KEEP_RECORDS.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode(-1))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition ASYNC_WRITES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ASYNC_WRITES, ModelType.BOOLEAN)
                    .setXmlName(Attribute.ASYNC_WRITES.getLocalName())
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode(false))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition JOURNAL_GC_THREAD_POOL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNAL_GC_THREAD_POOL, ModelType.STRING)
                    .setXmlName(Attribute.JOURNAL_GC_THREAD_POOL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("modeshape-journaling-gc"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final SimpleAttributeDefinition JOURNAL_GC_INITIAL_TIME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JOURNAL_GC_INITIAL_TIME, ModelType.STRING)
                    .setXmlName(Attribute.JOURNAL_GC_INITIAL_TIME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode().set("00:00"))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    public static final MappedSimpleAttributeDefinition TABLE_NAME =
            new MappedAttributeDefinitionBuilder(Attribute.TABLE_NAME.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.TABLE_NAME)
                    .setXmlName(Attribute.TABLE_NAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition CREATE_ON_START = 
            new MappedAttributeDefinitionBuilder(Attribute.CREATE_ON_START.getLocalName(), ModelType.BOOLEAN,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.CREATE_ON_START)
                    .setXmlName(Attribute.CREATE_ON_START.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition DROP_ON_EXIT =
            new MappedAttributeDefinitionBuilder(Attribute.DROP_ON_EXIT.getLocalName(), ModelType.BOOLEAN,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.DROP_ON_EXIT)
                    .setXmlName(Attribute.DROP_ON_EXIT.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition DB_COMPRESS =
            new MappedAttributeDefinitionBuilder(Attribute.COMPRESS.getLocalName(), ModelType.BOOLEAN,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.COMPRESS)
                    .setXmlName(Attribute.COMPRESS.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build(); 
    
    public static final MappedSimpleAttributeDefinition FETCH_SIZE = 
            new MappedAttributeDefinitionBuilder(Attribute.FETCH_SIZE.getLocalName(), ModelType.INT,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.COMPRESS)
                    .setXmlName(Attribute.FETCH_SIZE.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build(); 
    
    public static final MappedSimpleAttributeDefinition POOL_SIZE =
            new MappedAttributeDefinitionBuilder(Attribute.POOL_SIZE.getLocalName(), ModelType.INT,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.POOL_SIZE)
                    .setXmlName(Attribute.POOL_SIZE.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
 
    public static final MappedSimpleAttributeDefinition CONNECTION_URL =
            new MappedAttributeDefinitionBuilder(Attribute.URL.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.CONNECTION_URL)
                    .setXmlName(Attribute.URL.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition DRIVER = 
            new MappedAttributeDefinitionBuilder(Attribute.DRIVER.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.DRIVER)
                    .setXmlName(Attribute.DRIVER.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition USERNAME =
            new MappedAttributeDefinitionBuilder(Attribute.USERNAME.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.USERNAME)
                    .setXmlName(Attribute.USERNAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition PASSWORD = 
            new MappedAttributeDefinitionBuilder(Attribute.PASSWORD.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, RelationalDbConfig.PASSWORD)
                    .setXmlName(Attribute.PASSWORD.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition PERSISTENCE_DS_JNDI_NAME =
            new MappedAttributeDefinitionBuilder(Attribute.DATA_SOURCE_JNDI_NAME.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE,
                                                 RelationalDbConfig.DATASOURCE_JNDI_NAME)
                    .setXmlName(Attribute.DATA_SOURCE_JNDI_NAME.getLocalName())
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition FS_PATH =
            new MappedAttributeDefinitionBuilder(Attribute.PATH.getLocalName(), ModelType.STRING, 
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, FileDbProvider.PATH_FIELD)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition FS_COMPRESS =
            new MappedAttributeDefinitionBuilder(Attribute.COMPRESS.getLocalName(), ModelType.BOOLEAN,
                                                 FieldName.STORAGE, FieldName.PERSISTENCE, FileDbProvider.COMPRESS_FIELD)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition CASSANDRA_HOST =
            new MappedAttributeDefinitionBuilder(Attribute.HOST.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.ADDRESS)
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition MONGO_HOST =
            new MappedAttributeDefinitionBuilder(Attribute.HOST.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.HOST)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition MONGO_PORT =
            new MappedAttributeDefinitionBuilder(Attribute.PORT.getLocalName(), ModelType.INT,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.PORT)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition MONGO_DATABASE =
            new MappedAttributeDefinitionBuilder(Attribute.DATABASE.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.DATABASE)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition MONGO_USERNAME =
            new MappedAttributeDefinitionBuilder(Attribute.USERNAME.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.USER_NAME)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition MONGO_PASSWORD =
            new MappedAttributeDefinitionBuilder(Attribute.PASSWORD.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.USER_PASSWORD)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
    
    public static final MappedSimpleAttributeDefinition MONGO_HOST_ADDRESSES =
            new MappedAttributeDefinitionBuilder(Attribute.HOST_ADDRESSES.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.HOST_ADDRESSES)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final MappedSimpleAttributeDefinition S3_USERNAME =
            new MappedAttributeDefinitionBuilder(Attribute.USERNAME.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.USER_NAME)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final MappedSimpleAttributeDefinition S3_PASSWORD =
            new MappedAttributeDefinitionBuilder(Attribute.PASSWORD.getLocalName(), ModelType.STRING,
                                                 FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.USER_PASSWORD)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final MappedSimpleAttributeDefinition S3_BUCKET_NAME =
        new MappedAttributeDefinitionBuilder(Attribute.BUCKET_NAME.getLocalName(), ModelType.STRING,
                                             FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.BUCKET_NAME)
            .setAllowExpression(true)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final MappedSimpleAttributeDefinition S3_ENDPOINT_URL =
        new MappedAttributeDefinitionBuilder(Attribute.ENDPOINT_URL.getLocalName(), ModelType.STRING,
                                             FieldName.STORAGE, FieldName.BINARY_STORAGE, FieldName.ENDPOINT_URL)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final AttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {};

    public static final AttributeDefinition[] WEBAPP_ATTRIBUTES = {EXPLODED};

    public static final AttributeDefinition[] REPOSITORY_ATTRIBUTES = {JNDI_NAME, ENABLE_MONITORING,
        CLUSTER_NAME, CLUSTER_STACK, CLUSTER_CONFIG, CLUSTER_LOCKING, REPOSITORY_MODULE_DEPENDENCIES,
        SECURITY_DOMAIN, ANONYMOUS_ROLES, ANONYMOUS_USERNAME, USE_ANONYMOUS_IF_AUTH_FAILED, NODE_TYPES, DEFAULT_WORKSPACE,
        PREDEFINED_WORKSPACE_NAMES, ALLOW_WORKSPACE_CREATION, WORKSPACES_CACHE_SIZE, DEFAULT_INITIAL_CONTENT,
        WORKSPACES_INITIAL_CONTENT, GARBAGE_COLLECTION_THREAD_POOL,
        GARBAGE_COLLECTION_INITIAL_TIME, GARBAGE_COLLECTION_INTERVAL, DOCUMENT_OPTIMIZATION_THREAD_POOL,
        DOCUMENT_OPTIMIZATION_INITIAL_TIME, DOCUMENT_OPTIMIZATION_INTERVAL, DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET,
        DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE, JOURNAL_PATH, JOURNAL_RELATIVE_TO, MAX_DAYS_TO_KEEP_RECORDS,
        JOURNAL_GC_INITIAL_TIME, JOURNAL_GC_THREAD_POOL, ASYNC_WRITES, JOURNALING, JOURNAL_ENABLED, SEQUENCER_THREAD_POOL_NAME, SEQUENCER_MAX_POOL_SIZE, 
        TEXT_EXTRACTOR_THREAD_POOL_NAME, TEXT_EXTRACTOR_MAX_POOL_SIZE, EVENT_BUS_SIZE, REINDEXING_ASYNC, REINDEXING_MODE,
        LOCK_TIMEOUT_MILLIS};

    public static final AttributeDefinition[] TRANSIENT_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE, 
                                                                                     MIME_TYPE_DETECTION}; 
    
    public static final AttributeDefinition[] FILE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE, PATH,
        TRASH, RELATIVE_TO, STORE_NAME, MIME_TYPE_DETECTION};

    public static final AttributeDefinition[] DATABASE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        DATA_SOURCE_JNDI_NAME, STORE_NAME, MIME_TYPE_DETECTION};  
    
    public static final AttributeDefinition[] CASSANDRA_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        MIME_TYPE_DETECTION, CASSANDRA_HOST };

    public static final AttributeDefinition[] S3_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        MIME_TYPE_DETECTION, S3_USERNAME, S3_PASSWORD, S3_BUCKET_NAME, S3_ENDPOINT_URL};

    public static final AttributeDefinition[] MONGO_BINARY_STORAGE_ATTRIBUTES = { MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
                                                                                  MIME_TYPE_DETECTION, MONGO_HOST, MONGO_PORT, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD, MONGO_HOST_ADDRESSES };

    public static final AttributeDefinition[] COMPOSITE_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        NESTED_STORES, MIME_TYPE_DETECTION};

    public static final AttributeDefinition[] CUSTOM_BINARY_STORAGE_ATTRIBUTES = {MINIMUM_BINARY_SIZE, MINIMUM_STRING_SIZE,
        CLASSNAME, MODULE, STORE_NAME, MIME_TYPE_DETECTION};

    public static final AttributeDefinition[] INDEX_DEFINITION_ATTRIBUTES = {INDEX_KIND, PROVIDER_NAME, NODE_TYPE_NAME,
        SYNCHRONOUS ,INDEX_COLUMNS, WORKSPACES, PROPERTIES};

    public static final AttributeDefinition[] INDEX_PROVIDER_ATTRIBUTES = {CLASSNAME, MODULE, RELATIVE_TO, PATH, PROPERTIES};

    public static final AttributeDefinition[] SEQUENCER_ATTRIBUTES = {PATH_EXPRESSIONS, SEQUENCER_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] PERSISTENCE_DB_ATTRIBUTES = { TABLE_NAME, CREATE_ON_START, DROP_ON_EXIT,
                                                                            FETCH_SIZE, CONNECTION_URL, DRIVER, USERNAME, PASSWORD,
                                                                            PERSISTENCE_DS_JNDI_NAME, DB_COMPRESS, POOL_SIZE, 
                                                                            PROPERTIES}; 
    public static final AttributeDefinition[] PERSISTENCE_FS_ATTRIBUTES = { FS_PATH, FS_COMPRESS};
    public static final AttributeDefinition[] SOURCE_ATTRIBUTES = { PROJECTIONS, CONNECTOR_CLASSNAME, READONLY, CACHEABLE,
                                                                    QUERYABLE, MODULE, PROPERTIES, EXPOSE_AS_WORKSPACE};
    public static final AttributeDefinition[] TEXT_EXTRACTOR_ATTRIBUTES = {TEXT_EXTRACTOR_CLASSNAME, MODULE, PROPERTIES};
    public static final AttributeDefinition[] AUTHENTICATOR_ATTRIBUTES = {AUTHENTICATOR_CLASSNAME, MODULE, PROPERTIES};
}

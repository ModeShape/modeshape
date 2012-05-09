/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jboss.subsystem;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
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

    private static final ParameterValidator ROLE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING,false,false,true) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);  // checks null
            String str = value.asString();
            if ( !ModeShapeRoles.ADMIN.equals(str) &&
                 !ModeShapeRoles.READONLY.equals(str) &&
                 !ModeShapeRoles.READWRITE.equals(str) &&
                 !ModeShapeRoles.CONNECT.equals(str) ) {
                throw new OperationFailedException("Invalid anonymous role name: '" + str + "'");
            }
        }
    };
    private static final ParameterValidator BINARY_STORAGE_TYPE_VALIDATOR = new ModelTypeValidator(ModelType.STRING,false,false,true) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);  // checks null
            String str = value.asString();
            if ( !ModelKeys.FILE_BINARY_STORAGE.equals(str) &&
                 !ModelKeys.DB_BINARY_STORAGE.equals(str) &&
                 !ModelKeys.CACHE_BINARY_STORAGE.equals(str) &&
                 !ModelKeys.CUSTOM_BINARY_STORAGE.equals(str) ) {
                throw new OperationFailedException("Invalid binary storage type: '" + str + "'");
            }
        }
    };
    private static final ParameterValidator INDEX_STORAGE_TYPE_VALIDATOR = new ModelTypeValidator(ModelType.STRING,false,false,true) {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);  // checks null
            String str = value.asString();
            if ( !ModelKeys.RAM_INDEX_STORAGE.equals(str) &&
                 !ModelKeys.LOCAL_FILE_INDEX_STORAGE.equals(str) &&
                 !ModelKeys.MASTER_FILE_INDEX_STORAGE.equals(str) &&
                 !ModelKeys.SLAVE_FILE_INDEX_STORAGE.equals(str) &&
                 !ModelKeys.CACHE_INDEX_STORAGE.equals(str) &&
                 !ModelKeys.CUSTOM_INDEX_STORAGE.equals(str) ) {
                throw new OperationFailedException("Invalid index storage type: '" + str + "'");
            }
        }
    };
    private static final ParameterValidator WORKSPACE_NAME_VALIDATOR = new ModelTypeValidator(ModelType.STRING,false,false,true);
    private static final ParameterValidator INDEX_FORMAT_VALIDATOR = new RegexValidator("LUCENE_(3[0-9]{1,2}|CURRENT)", true);
    private static final ParameterValidator REBUILD_INDEXES_VALIDATOR = new EnumValidator<QueryRebuild>(QueryRebuild.class, false, true);
    private static final ParameterValidator ACCESS_TYPE_VALIDATOR = new EnumValidator<FileSystemAccessType>(FileSystemAccessType.class, false, true);
    private static final ParameterValidator LOCKING_STRATEGY_VALIDATOR = new EnumValidator<FileSystemLockingStrategy>(FileSystemLockingStrategy.class, false, true);
    private static final ParameterValidator READER_STRATEGY_VALIDATOR = new EnumValidator<IndexReaderStrategy>(IndexReaderStrategy.class, false, true);
    private static final ParameterValidator INDEXING_MODE_VALIDATOR = new EnumValidator<IndexingMode>(IndexingMode.class, false, true);
    private static final ParameterValidator PATH_EXPRESSION_VALIDATOR = new PathExpressionValidator(false);

    public static final SimpleAttributeDefinition ACCESS_TYPE =
        new MappedAttributeDefinitionBuilder(ModelKeys.ACCESS_TYPE, ModelType.STRING)
                .setXmlName(Attribute.ACCESS_TYPE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(FileSystemAccessType.AUTO.toString()))
                .setValidator(ACCESS_TYPE_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition ALLOW_WORKSPACE_CREATION =
        new MappedAttributeDefinitionBuilder(ModelKeys.ALLOW_WORKSPACE_CREATION, ModelType.BOOLEAN)
                .setXmlName(Attribute.ALLOW_WORKSPACE_CREATION.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(true))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,FieldName.ALLOW_CREATION)
                .build();

    public static final SimpleAttributeDefinition ANALYZER_CLASSNAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.ANALYZER_CLASSNAME, ModelType.STRING)
                .setXmlName(Attribute.ANALYZER_CLASSNAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(StandardAnalyzer.class.getName()))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition ANALYZER_MODULE =
        new MappedAttributeDefinitionBuilder(ModelKeys.ANALYZER_MODULE, ModelType.STRING)
                .setXmlName(Attribute.ANALYZER_MODULE.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final ListAttributeDefinition ANONYMOUS_ROLES = 
        MappedListAttributeDefinition.Builder.of(ModelKeys.ANONYMOUS_ROLES,
            new MappedAttributeDefinitionBuilder(ModelKeys.ANONYMOUS_ROLE, ModelType.STRING)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode()
                                 .add(new ModelNode().set(ModeShapeRoles.CONNECT))
                                 .add(new ModelNode().set(ModeShapeRoles.ADMIN))
                                 .add(new ModelNode().set(ModeShapeRoles.READONLY))
                                 .add(new ModelNode().set(ModeShapeRoles.READWRITE)))
                .setValidator(ROLE_NAME_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .build()
        )
        .setAllowNull(true)
        .setMinSize(0)
        .setMaxSize(100)
        .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,FieldName.ANONYMOUS,FieldName.ANONYMOUS_ROLES)
        .build();

    public static final SimpleAttributeDefinition ANONYMOUS_USERNAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.ANONYMOUS_USERNAME, ModelType.STRING)
                .setXmlName(Attribute.ANONYMOUS_USERNAME.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("<anonymous>"))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,FieldName.ANONYMOUS,FieldName.ANONYMOUS_USERNAME)
                .build();

    public static final SimpleAttributeDefinition ASYNC_MAX_QUEUE_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.ASYNC_MAX_QUEUE_SIZE, ModelType.INT)
                .setXmlName(Attribute.ASYNC_MAX_QUEUE_SIZE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(0))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition ASYNC_THREAD_POOL_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.ASYNC_THREAD_POOL_SIZE, ModelType.INT)
                .setXmlName(Attribute.ASYNC_THREAD_POOL_SIZE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(1))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition BATCH_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.BATCH_SIZE, ModelType.INT)
                .setXmlName(Attribute.BATCH_SIZE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(-1))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition BINARY_STORAGE_TYPE =
        new MappedAttributeDefinitionBuilder(ModelKeys.BINARY_STORAGE_TYPE, ModelType.STRING)
                .setAllowExpression(false)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setDefaultValue(new ModelNode().set(ModelKeys.FILE_BINARY_STORAGE))
                .setValidator(BINARY_STORAGE_TYPE_VALIDATOR)
                .build();

    public static final SimpleAttributeDefinition CACHE_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.CACHE_NAME, ModelType.STRING)
                .setXmlName(Attribute.CACHE_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CACHE_CONTAINER =
        new MappedAttributeDefinitionBuilder(ModelKeys.CACHE_CONTAINER, ModelType.STRING)
                .setXmlName(Attribute.CACHE_CONTAINER.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CACHE_CONTAINER_JNDI_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.CACHE_CONTAINER_JNDI_NAME, ModelType.STRING)
                .setXmlName(Attribute.CACHE_CONTAINER_JNDI_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CLUSTER_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.CLUSTER_NAME, ModelType.STRING)
                .setXmlName(Attribute.CLUSTER_NAME.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CLUSTER_STACK =
        new MappedAttributeDefinitionBuilder(ModelKeys.CLUSTER_STACK, ModelType.STRING)
                .setXmlName(Attribute.CLUSTER_STACK.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CHUNK_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.CHUNK_SIZE, ModelType.INT)
                .setXmlName(Attribute.CHUNK_SIZE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(16834))
                .setMeasurementUnit(MeasurementUnit.BYTES)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.QUERY,FieldName.INDEX_STORAGE,FieldName.INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES)
                .build();

    public static final SimpleAttributeDefinition CLASSNAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.CLASSNAME, ModelType.STRING)
                .setXmlName(Attribute.CLASSNAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition CONNECTION_FACTORY_JNDI_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.CONNECTION_FACTORY_JNDI_NAME, ModelType.STRING)
                .setXmlName(Attribute.CONNECTION_FACTORY_JNDI_NAME.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("/ConnectionFactory"))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition COPY_BUFFER_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.COPY_BUFFER_SIZE, ModelType.INT)
                .setXmlName(Attribute.COPY_BUFFER_SIZE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(16))
                .setMeasurementUnit(MeasurementUnit.MEGABYTES)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.QUERY,FieldName.INDEX_STORAGE,FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES)
                .build();

    public static final SimpleAttributeDefinition DATA_CACHE_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.DATA_CACHE_NAME, ModelType.STRING)
                .setXmlName(Attribute.DATA_CACHE_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition DATA_SOURCE_JNDI_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.DATA_SOURCE_JNDI_NAME, ModelType.STRING)
                .setXmlName(Attribute.DATA_SOURCE_JNDI_NAME.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition DEFAULT_WORKSPACE =
        new MappedAttributeDefinitionBuilder(ModelKeys.DEFAULT_WORKSPACE, ModelType.STRING)
                .setXmlName(Attribute.DEFAULT_WORKSPACE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("default"))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,FieldName.DEFAULT)
                .build();

    public static final SimpleAttributeDefinition ENABLE_MONITORING =
        new MappedAttributeDefinitionBuilder(ModelKeys.ENABLE_MONITORING, ModelType.BOOLEAN)
                .setXmlName(Attribute.ENABLE_MONITORING.getLocalName())
                .setAllowNull(true)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode().set(true))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.MONITORING,FieldName.MONITORING_ENABLED)
                .build();

    public static final SimpleAttributeDefinition INDEX_FORMAT =
        new MappedAttributeDefinitionBuilder(ModelKeys.INDEX_FORMAT, ModelType.STRING)
                .setXmlName(Attribute.FORMAT.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setValidator(INDEX_FORMAT_VALIDATOR)
                .setDefaultValue(new ModelNode().set("LUCENE_CURRENT"))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition INDEX_STORAGE_TYPE =
        new MappedAttributeDefinitionBuilder(ModelKeys.INDEX_STORAGE_TYPE, ModelType.STRING)
                .setAllowExpression(false)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setDefaultValue(new ModelNode().set(ModelKeys.LOCAL_FILE_INDEX_STORAGE))
                .setValidator(INDEX_STORAGE_TYPE_VALIDATOR)
                .build();

    public static final SimpleAttributeDefinition JNDI_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING)
                .setXmlName(Attribute.JNDI_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition LOCK_CACHE_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.LOCK_CACHE_NAME, ModelType.STRING)
                .setXmlName(Attribute.LOCK_CACHE_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition LOCKING_STRATEGY =
        new MappedAttributeDefinitionBuilder(ModelKeys.LOCKING_STRATEGY, ModelType.STRING)
                .setXmlName(Attribute.LOCKING_STRATEGY.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(FileSystemLockingStrategy.NATIVE.toString()))
                .setValidator(LOCKING_STRATEGY_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition METADATA_CACHE_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.METADATA_CACHE_NAME, ModelType.STRING)
                .setXmlName(Attribute.META_CACHE_NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();
    
    public static final SimpleAttributeDefinition MINIMUM_BINARY_SIZE =
        new MappedAttributeDefinitionBuilder(ModelKeys.MINIMUM_BINARY_SIZE, ModelType.INT)
                .setXmlName(Attribute.MIN_VALUE_SIZE.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(4096))
                .setMeasurementUnit(MeasurementUnit.BYTES)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.STORAGE,FieldName.BINARY_STORAGE,FieldName.MINIMUM_BINARY_SIZE_IN_BYTES)
                .build();

    public static final SimpleAttributeDefinition MODE =
        new MappedAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING)
                .setXmlName(Attribute.MODE.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(IndexingMode.SYNC.toString()))
                .setValidator(INDEXING_MODE_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition MODULE =
        new MappedAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING)
                .setXmlName(Attribute.MODULE.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING)
                .setXmlName(Attribute.NAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition PATH =
        new MappedAttributeDefinitionBuilder(ModelKeys.PATH, ModelType.STRING)
                .setXmlName(Attribute.PATH.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final ListAttributeDefinition PATH_EXPRESSIONS = 
        MappedListAttributeDefinition.Builder.of(ModelKeys.PATH_EXPRESSIONS,
            new MappedAttributeDefinitionBuilder(ModelKeys.PATH_EXPRESSION, ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setValidator(PATH_EXPRESSION_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build()
        )
        .setAllowNull(false)
        .setMinSize(1)
        .setFieldPathInRepositoryConfiguration(FieldName.SEQUENCING,FieldName.SEQUENCERS,FieldName.PATH_EXPRESSIONS)
        .build();

    public static final ListAttributeDefinition PREDEFINED_WORKSPACE_NAMES = 
        MappedListAttributeDefinition.Builder.of(ModelKeys.PREDEFINED_WORKSPACE_NAMES,
            new MappedAttributeDefinitionBuilder(ModelKeys.PREDEFINED_WORKSPACE_NAME, ModelType.STRING)
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setValidator(WORKSPACE_NAME_VALIDATOR)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build()
        )
        .setAllowNull(true)
        .setMinSize(0)
        .setFieldPathInRepositoryConfiguration(FieldName.WORKSPACES,FieldName.PREDEFINED)
        .build();

    public static final SimpleAttributeDefinition QUEUE_JNDI_NAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.QUEUE_JNDI_NAME, ModelType.STRING)
                .setXmlName(Attribute.QUEUE_JNDI_NAME.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition READER_STRATEGY =
        new MappedAttributeDefinitionBuilder(ModelKeys.READER_STRATEGY, ModelType.STRING)
                .setXmlName(Attribute.READER_STRATEGY.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(IndexReaderStrategy.SHARED.toString()))
                .setValidator(READER_STRATEGY_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition REBUILD_INDEXES_UPON_STARTUP =
        new MappedAttributeDefinitionBuilder(ModelKeys.REBUILD_INDEXES_UPON_STARTUP, ModelType.STRING)
                .setXmlName(Attribute.REBUILD_UPON_STARTUP.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(QueryRebuild.IF_MISSING.toString()))
                .setValidator(REBUILD_INDEXES_VALIDATOR)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.QUERY,FieldName.REBUILD_UPON_STARTUP)
                .build();

    public static final SimpleAttributeDefinition REFRESH_PERIOD =
        new MappedAttributeDefinitionBuilder(ModelKeys.REFRESH_PERIOD, ModelType.INT)
                .setXmlName(Attribute.REFRESH_PERIOD.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(3600))
                .setMeasurementUnit(MeasurementUnit.SECONDS)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.QUERY,FieldName.INDEX_STORAGE,FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS)
                .build();

    public static final SimpleAttributeDefinition RELATIVE_TO =
        new MappedAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO, ModelType.STRING)
                .setXmlName(Attribute.RELATIVE_TO.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition RETRY_INITIALIZE_PERIOD =
        new MappedAttributeDefinitionBuilder(ModelKeys.RETRY_INITIALIZE_PERIOD, ModelType.INT)
                .setXmlName(Attribute.RETRY_INIT_PERIOD.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(0))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();
    
    public static final SimpleAttributeDefinition RETRY_MARKER_LOOKUP =
        new MappedAttributeDefinitionBuilder(ModelKeys.RETRY_MARKER_LOOKUP, ModelType.INT)
                .setXmlName(Attribute.RETRY_MARKER_LOOKUP.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(0))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition SEQUENCER_CLASSNAME =
        new MappedAttributeDefinitionBuilder(ModelKeys.SEQUENCER_CLASSNAME, ModelType.STRING)
                .setXmlName(Attribute.CLASSNAME.getLocalName())
                .setAllowExpression(false)
                .setAllowNull(true)
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.SEQUENCING,FieldName.SEQUENCERS,FieldName.TYPE)
                .build();

    public static final SimpleAttributeDefinition SECURITY_DOMAIN =
        new MappedAttributeDefinitionBuilder(ModelKeys.SECURITY_DOMAIN, ModelType.STRING)
                .setXmlName(Attribute.SECURITY_DOMAIN.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("modeshape-security"))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,FieldName.JAAS,FieldName.JAAS_POLICY_NAME)
                .build();

    public static final SimpleAttributeDefinition SOURCE_PATH =
        new MappedAttributeDefinitionBuilder(ModelKeys.SOURCE_PATH, ModelType.STRING)
                .setXmlName(Attribute.SOURCE_PATH.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition SOURCE_RELATIVE_TO =
        new MappedAttributeDefinitionBuilder(ModelKeys.SOURCE_RELATIVE_TO, ModelType.STRING)
                .setXmlName(Attribute.SOURCE_RELATIVE_TO.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition THREAD_POOL =
        new MappedAttributeDefinitionBuilder(ModelKeys.THREAD_POOL, ModelType.STRING)
                .setXmlName(Attribute.THREAD_POOL.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set("modeshape-workers"))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .build();

    public static final SimpleAttributeDefinition USE_ANONYMOUS_IF_AUTH_FAILED =
        new MappedAttributeDefinitionBuilder(ModelKeys.USE_ANONYMOUS_IF_AUTH_FAILED, ModelType.BOOLEAN)
                .setXmlName(Attribute.USE_ANONYMOUS_IF_AUTH_FAILED.getLocalName())
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setFlags(AttributeAccess.Flag.RESTART_NONE)
                .setFieldPathInRepositoryConfiguration(FieldName.SECURITY,FieldName.ANONYMOUS,FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS)
                .build();

    public static final AttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {};
    
    public static final AttributeDefinition[] REPOSITORY_ATTRIBUTES = {
        CACHE_NAME, CACHE_CONTAINER, JNDI_NAME, ENABLE_MONITORING,
        SECURITY_DOMAIN, ANONYMOUS_ROLES, ANONYMOUS_USERNAME, USE_ANONYMOUS_IF_AUTH_FAILED,
        DEFAULT_WORKSPACE, PREDEFINED_WORKSPACE_NAMES, ALLOW_WORKSPACE_CREATION,
        MINIMUM_BINARY_SIZE,
        THREAD_POOL, BATCH_SIZE, READER_STRATEGY,
        MODE, ASYNC_THREAD_POOL_SIZE, ASYNC_MAX_QUEUE_SIZE,
        ANALYZER_CLASSNAME, ANALYZER_MODULE,
        REBUILD_INDEXES_UPON_STARTUP,
        CLUSTER_NAME, CLUSTER_STACK,
    };

    public static final AttributeDefinition[] RAM_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE,
    };

    public static final AttributeDefinition[] LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE, INDEX_FORMAT,
        PATH, RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY,
    };

    public static final AttributeDefinition[] MASTER_FILE_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE, INDEX_FORMAT,
        PATH, RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY,
        REFRESH_PERIOD, SOURCE_PATH, SOURCE_RELATIVE_TO, CONNECTION_FACTORY_JNDI_NAME, QUEUE_JNDI_NAME,
    };

    public static final AttributeDefinition[] SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE, INDEX_FORMAT,
        PATH, RELATIVE_TO, ACCESS_TYPE, LOCKING_STRATEGY,
        REFRESH_PERIOD, SOURCE_PATH, SOURCE_RELATIVE_TO, CONNECTION_FACTORY_JNDI_NAME, QUEUE_JNDI_NAME,
        COPY_BUFFER_SIZE, RETRY_MARKER_LOOKUP, RETRY_INITIALIZE_PERIOD
    };

    public static final AttributeDefinition[] CACHE_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE, INDEX_FORMAT,
        DATA_CACHE_NAME, METADATA_CACHE_NAME, LOCK_CACHE_NAME, CACHE_CONTAINER_JNDI_NAME, CHUNK_SIZE,
    };

    public static final AttributeDefinition[] CUSTOM_INDEX_STORAGE_ATTRIBUTES = {
        INDEX_STORAGE_TYPE, INDEX_FORMAT,
        CLASSNAME, MODULE,
    };
    
    public static final AttributeDefinition[] FILE_BINARY_STORAGE_ATTRIBUTES = {
        BINARY_STORAGE_TYPE, MINIMUM_BINARY_SIZE, 
        PATH, RELATIVE_TO,
    };
    
    public static final AttributeDefinition[] CACHE_BINARY_STORAGE_ATTRIBUTES = {
        BINARY_STORAGE_TYPE, MINIMUM_BINARY_SIZE,
        DATA_CACHE_NAME, METADATA_CACHE_NAME, CACHE_CONTAINER,
    };
    
    public static final AttributeDefinition[] DATABASE_BINARY_STORAGE_ATTRIBUTES = {
        BINARY_STORAGE_TYPE, MINIMUM_BINARY_SIZE,
        DATA_SOURCE_JNDI_NAME,
    };
    
    public static final AttributeDefinition[] CUSTOM_BINARY_STORAGE_ATTRIBUTES = {
        BINARY_STORAGE_TYPE, MINIMUM_BINARY_SIZE,
        CLASSNAME, MODULE,
    };
    
    

    public static final AttributeDefinition[] SEQUENCER_ATTRIBUTES = {
        PATH_EXPRESSIONS, SEQUENCER_CLASSNAME, MODULE,
    };
}

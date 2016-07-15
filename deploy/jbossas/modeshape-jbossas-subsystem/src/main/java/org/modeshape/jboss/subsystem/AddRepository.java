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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.metric.ModelMetrics;
import org.modeshape.jboss.metric.MonitorService;
import org.modeshape.jboss.security.JBossDomainAuthenticationProvider;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jboss.service.ReferenceFactoryService;
import org.modeshape.jboss.service.RepositoryService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.wildfly.clustering.jgroups.ChannelFactory;

public class AddRepository extends AbstractAddStepHandler {

    public static final AddRepository INSTANCE = new AddRepository();

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(AddRepository.class.getPackage()
                                                                                                              .getName());

    private AddRepository() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        // attributes
        for (AttributeDefinition attribute : ModelAttributes.REPOSITORY_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }

        // metrics
        for (final AttributeDefinition metric : ModelMetrics.ALL_METRICS) {
            metric.validateAndSet(operation, model);
        }
    }

    private ModelNode attribute( OperationContext context,
                                 ModelNode model,
                                 AttributeDefinition defn ) throws OperationFailedException {
        assert defn.getDefaultValue() != null && defn.getDefaultValue().isDefined();
        return defn.resolveModelAttribute(context, model);
    }

    private String attribute( OperationContext context,
                              ModelNode model,
                              AttributeDefinition defn,
                              String defaultValue ) throws OperationFailedException {
        ModelNode value = defn.resolveModelAttribute(context, model);
        return value.isDefined() ? value.asString() : defaultValue;
    }

    private Integer intAttribute( OperationContext context,
                                  ModelNode model,
                                  AttributeDefinition defn,
                                  Integer defaultValue ) throws OperationFailedException {
        ModelNode value = defn.resolveModelAttribute(context, model);
        if (value == null || !value.isDefined()) return defaultValue;
        return value.asInt();
    }

    private Long longAttribute(OperationContext context,
                               ModelNode model,
                               AttributeDefinition defn,
                               Long defaultValue) throws OperationFailedException {
        ModelNode value = defn.resolveModelAttribute(context, model);
        if (value == null || !value.isDefined()) {
            return defaultValue;
        }
        return value.asLong();
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model) throws OperationFailedException {

        final ServiceTarget target = context.getServiceTarget();
        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String clusterName = attribute(context, model, ModelAttributes.CLUSTER_NAME, null);
        final boolean enableMonitoring = attribute(context, model, ModelAttributes.ENABLE_MONITORING).asBoolean();
        final String gcThreadPool = attribute(context, model, ModelAttributes.GARBAGE_COLLECTION_THREAD_POOL, null);
        final String gcInitialTime = attribute(context, model, ModelAttributes.GARBAGE_COLLECTION_INITIAL_TIME, null);
        final int gcIntervalInHours = attribute(context, model, ModelAttributes.GARBAGE_COLLECTION_INTERVAL).asInt();
        final String optThreadPool = attribute(context, model, ModelAttributes.DOCUMENT_OPTIMIZATION_THREAD_POOL, null);
        final String optInitialTime = attribute(context, model, ModelAttributes.DOCUMENT_OPTIMIZATION_INITIAL_TIME, null);
        final int optIntervalInHours = attribute(context, model, ModelAttributes.DOCUMENT_OPTIMIZATION_INTERVAL).asInt();
        final Integer optTarget = intAttribute(context, model, ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET, null);
        final Integer eventBusSize = intAttribute(context, model, ModelAttributes.EVENT_BUS_SIZE, null);
        final Integer optTolerance = intAttribute(context, model,
                                                  ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE,
                                                  null);
        final Long lockTimeoutMillis = longAttribute(context, model, ModelAttributes.LOCK_TIMEOUT_MILLIS, null);

        // Create a document for the repository configuration ...
        EditableDocument configDoc = Schematic.newDocument();
        configDoc.set(FieldName.NAME, repositoryName);

        // Determine the JNDI name ...
        configDoc.set(FieldName.JNDI_NAME, "");// always set to empty string, since we'll register in JNDI here ...
        final String jndiName = ModeShapeJndiNames.JNDI_BASE_NAME + repositoryName;
        String jndiAlias = ModeShapeJndiNames.jndiNameFrom(model, repositoryName);
        if (jndiName.equals(jndiAlias)) {
            jndiAlias = null;
        }
        
        if (eventBusSize != null) {
            configDoc.setNumber(FieldName.EVENT_BUS_SIZE, eventBusSize);
        }    
        if (lockTimeoutMillis != null) {
            configDoc.setNumber(FieldName.LOCK_TIMEOUT_MILLIS, lockTimeoutMillis);
        }
        
        List<String> additionalClasspathEntries = new ArrayList<>();
        
        // Always set whether monitoring is enabled ...
        enableMonitoring(enableMonitoring, configDoc);

        // Initial node-types if configured
        parseCustomNodeTypes(model, configDoc, additionalClasspathEntries);

        // Workspace information is on the repository model node (unlike the XML) ...
        EditableDocument workspacesDoc = parseWorkspaces(context, model, configDoc, additionalClasspathEntries);
        
        // security
        parseSecurity(context, model, configDoc);

        // Now create the repository service that manages the lifecycle of the JcrRepository instance ...
        RepositoryConfiguration repositoryConfig = new RepositoryConfiguration(configDoc, repositoryName);
        
        String additionalModuleDependencies = attribute(context, model, ModelAttributes.REPOSITORY_MODULE_DEPENDENCIES, null);
        RepositoryService repositoryService = new RepositoryService(repositoryConfig, additionalModuleDependencies);
        ServiceName repositoryServiceName = ModeShapeServiceNames.repositoryServiceName(repositoryName);

        // Sequencing
        parseSequencing(model, configDoc);

        // Text Extraction
        parseTextExtraction(model, configDoc);
        
        // Reindexing
        parseReindexing(model, configDoc);
        
        // Journaling
        parseJournaling(repositoryService, context, model, configDoc);

        // Add the EngineService's dependencies ...
        ServiceBuilder<JcrRepository> repositoryServiceBuilder = target.addService(repositoryServiceName, repositoryService);

        // Add dependency to the ModeShape engine service ...
        repositoryServiceBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                               ModeShapeEngine.class,
                                               repositoryService.getEngineInjector());
        repositoryServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Add garbage collection information ...
        if (gcThreadPool != null) {
            configDoc.getOrCreateDocument(FieldName.GARBAGE_COLLECTION).setString(FieldName.THREAD_POOL, gcThreadPool);
        }
        if (gcInitialTime != null) {
            configDoc.getOrCreateDocument(FieldName.GARBAGE_COLLECTION).setString(FieldName.INITIAL_TIME, gcInitialTime);
        }
        configDoc.getOrCreateDocument(FieldName.GARBAGE_COLLECTION).setNumber(FieldName.INTERVAL_IN_HOURS, gcIntervalInHours);

        // Add document optimization information ...
        if (optTarget != null) {
            EditableDocument docOpt = configDoc.getOrCreateDocument(FieldName.STORAGE)
                                               .getOrCreateDocument(FieldName.DOCUMENT_OPTIMIZATION);
            if (optThreadPool != null) {
                docOpt.setString(FieldName.THREAD_POOL, optThreadPool);
            }
            if (optInitialTime != null) {
                docOpt.setString(FieldName.INITIAL_TIME, optInitialTime);
            }
            docOpt.setNumber(FieldName.INTERVAL_IN_HOURS, optIntervalInHours);
            docOpt.setNumber(FieldName.OPTIMIZATION_CHILD_COUNT_TARGET, optTarget.intValue());
            if (optTolerance != null) {
                docOpt.setNumber(FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE, optTolerance.intValue());
            }
        }
        
        if (!StringUtil.isBlank(clusterName)) {
            final String clusterConfig = attribute(context, model, ModelAttributes.CLUSTER_CONFIG, null);
            final String clusterLocking = attribute(context, model, ModelAttributes.CLUSTER_LOCKING, null);
            parseClustering(clusterName, clusterConfig, clusterLocking, configDoc);
            final String clusterStackName = attribute(context, model, ModelAttributes.CLUSTER_STACK, null);
            if (!StringUtil.isBlank(clusterStackName)) {
                repositoryServiceBuilder.addDependency(ServiceName.JBOSS.append("jgroups", "factory", clusterStackName),
                                      ChannelFactory.class, repositoryService.getChannelFactoryInjector());
            }
        }

        // Add the dependency to the Security Manager
        repositoryServiceBuilder.addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                                               repositoryService.getSecurityManagementServiceInjector());

        repositoryServiceBuilder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER,
                                               ModuleLoader.class,
                                               repositoryService.getModuleLoaderInjector());
        
        // Set up the JNDI binder service ...
        final ReferenceFactoryService<JcrRepository> referenceFactoryService = new ReferenceFactoryService<JcrRepository>();
        ServiceName referenceFactoryServiceName = ModeShapeServiceNames.referenceFactoryServiceName(repositoryName);
        final ServiceBuilder<?> referenceBuilder = target.addService(referenceFactoryServiceName, referenceFactoryService);
        referenceBuilder.addDependency(repositoryServiceName, JcrRepository.class, referenceFactoryService.getInjector());
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder);
        if (jndiAlias != null) {
            ContextNames.BindInfo aliasInfo = ContextNames.bindInfoFor(jndiAlias);
            ServiceName alias = aliasInfo.getBinderServiceName();
            binderBuilder.addAliases(alias);
            LOG.debugv("Binding repository {0} to JNDI name {1} and {2}",
                       repositoryName,
                       bindInfo.getAbsoluteJndiName(),
                       aliasInfo.getAbsoluteJndiName());
        } else {
            LOG.debugv("Binding repository {0} to JNDI name {1}", repositoryName, bindInfo.getAbsoluteJndiName());
        }
        binderBuilder.addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binder.getManagedObjectInjector());
        binderBuilder.addDependency(bindInfo.getParentContextServiceName(),
                                    ServiceBasedNamingStore.class,
                                    binder.getNamingStoreInjector());
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Add dependency to the data directory ...
        ServiceName dataDirServiceName = ModeShapeServiceNames.dataDirectoryServiceName(repositoryName);
        RelativePathService.addService(dataDirServiceName, "modeshape/" + repositoryName, 
                                       ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE, target);
        repositoryServiceBuilder.addDependency(dataDirServiceName, String.class, repositoryService.getDataDirectoryPathInjector());

        // Add the default binary storage service which will provide the binary configuration
        BinaryStorageService defaultBinaryService = BinaryStorageService.createDefault();
        ServiceName defaultBinaryStorageServiceName = ModeShapeServiceNames.binaryStorageDefaultServiceName(repositoryName);
        ServiceBuilder<BinaryStorage> binaryStorageBuilder = target.addService(defaultBinaryStorageServiceName,
                                                                               defaultBinaryService);
        binaryStorageBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        // Add dependency to the binaries storage service, which captures the properties for the binaries storage
        repositoryServiceBuilder.addDependency(defaultBinaryStorageServiceName,
                                               BinaryStorage.class,
                                               repositoryService.getBinaryStorageInjector());
        
        // Add monitor service
        final MonitorService monitorService = new MonitorService();
        final ServiceBuilder<RepositoryMonitor> monitorBuilder = target.addService(ModeShapeServiceNames.monitorServiceName(repositoryName),
                                                                                   monitorService);
        monitorBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                     JcrRepository.class,
                                     monitorService.getJcrRepositoryInjector());
        monitorBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Now add the controller for the RepositoryService ...
        repositoryServiceBuilder.install();
        referenceBuilder.install();
        binderBuilder.install();
        binaryStorageBuilder.install();
        monitorBuilder.install();
    }

    private void parseClustering(String clusterName, String clusterConfig, String clusterLocking, EditableDocument configDoc)  {
        EditableDocument clustering = configDoc.getOrCreateDocument(FieldName.CLUSTERING);
        clustering.setString(FieldName.CLUSTER_NAME, clusterName);
        if (!StringUtil.isBlank(clusterConfig)) {
            clustering.setString(FieldName.CLUSTER_CONFIGURATION, clusterConfig);                        
        }
        if (!StringUtil.isBlank(clusterLocking)) {
            clustering.setString(FieldName.CLUSTER_LOCKING, clusterLocking);
        }
    }

    private void parseTextExtraction( ModelNode model, EditableDocument configDoc ) {
        if (model.hasDefined(ModelKeys.TEXT_EXTRACTORS_THREAD_POOL_NAME)) {
            EditableDocument extractors = configDoc.getOrCreateDocument(FieldName.TEXT_EXTRACTION);
            String poolName = model.get(ModelKeys.TEXT_EXTRACTORS_THREAD_POOL_NAME).asString();
            extractors.set(FieldName.THREAD_POOL, poolName);
        }
        if (model.hasDefined(ModelKeys.TEXT_EXTRACTORS_MAX_POOL_SIZE)) {
            EditableDocument sequencing = configDoc.getOrCreateDocument(FieldName.TEXT_EXTRACTION);
            int maxPoolSize = model.get(ModelKeys.TEXT_EXTRACTORS_MAX_POOL_SIZE).asInt();
            sequencing.set(FieldName.MAX_POOL_SIZE, maxPoolSize);
        }
    }
   
    private void parseReindexing( ModelNode model, EditableDocument configDoc ) {
        if (model.hasDefined(ModelKeys.REINDEXING_ASYNC)) {
            EditableDocument reindexing = configDoc.getOrCreateDocument(FieldName.REINDEXING);
            boolean async = model.get(ModelKeys.REINDEXING_ASYNC).asBoolean();
            reindexing.set(FieldName.REINDEXING_ASYNC, async);
        }
        if (model.hasDefined(ModelKeys.REINDEXING_MODE)) {
            EditableDocument reindexing = configDoc.getOrCreateDocument(FieldName.REINDEXING);
            String mode = model.get(ModelKeys.REINDEXING_MODE).asString();
            reindexing.set(FieldName.REINDEXING_MODE, mode);
        }
    }

    private void parseSequencing( ModelNode model,
                                  EditableDocument configDoc ) {
        if (model.hasDefined(ModelKeys.SEQUENCERS_THREAD_POOL_NAME)) {
            EditableDocument sequencing = configDoc.getOrCreateDocument(FieldName.SEQUENCING);
            String sequencingThreadPool = model.get(ModelKeys.SEQUENCERS_THREAD_POOL_NAME).asString();
            sequencing.set(FieldName.THREAD_POOL, sequencingThreadPool);
        }            
        if (model.hasDefined(ModelKeys.SEQUENCERS_MAX_POOL_SIZE)) {
            EditableDocument sequencing = configDoc.getOrCreateDocument(FieldName.SEQUENCING);
            int maxPoolSize = model.get(ModelKeys.SEQUENCERS_MAX_POOL_SIZE).asInt();
            sequencing.set(FieldName.MAX_POOL_SIZE, maxPoolSize);
        }            
    }

    private void parseSecurity( OperationContext context,
                                ModelNode model,
                                EditableDocument configDoc ) throws OperationFailedException {
        EditableDocument security = configDoc.getOrCreateDocument(FieldName.SECURITY);

        // Anonymous ...
        EditableDocument anon = security.getOrCreateDocument(FieldName.ANONYMOUS);
        String anonUsername = attribute(context, model, ModelAttributes.ANONYMOUS_USERNAME).asString();
        boolean useAnonIfFailed = attribute(context, model, ModelAttributes.USE_ANONYMOUS_IF_AUTH_FAILED).asBoolean();
        anon.set(FieldName.ANONYMOUS_USERNAME, anonUsername);
        anon.set(FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS, useAnonIfFailed);
        List<ModelNode> modelNodes = model.hasDefined(ModelKeys.ANONYMOUS_ROLES) ?
                                     model.get(ModelKeys.ANONYMOUS_ROLES).asList():
                                     ModelAttributes.ANONYMOUS_ROLES.getDefaultValue().asList();
        for (ModelNode roleNode : modelNodes) {
            EditableArray anonymousRolesArray = anon.getOrCreateArray(FieldName.ANONYMOUS_ROLES);
            String roleName = roleNode.asString();
            if (!StringUtil.isBlank(roleName)) {
                anonymousRolesArray.addString(roleName);
            }
        }

        EditableArray providers = security.getOrCreateArray(FieldName.PROVIDERS);
        
        // JBoss authenticator ...
        String securityDomain = attribute(context, model, ModelAttributes.SECURITY_DOMAIN).asString();
        EditableDocument jboss = Schematic.newDocument();
        jboss.set(FieldName.CLASSNAME, JBossDomainAuthenticationProvider.class.getName());
        jboss.set(FieldName.SECURITY_DOMAIN, securityDomain);
        providers.add(jboss);

        // Servlet authenticator ...
        EditableDocument servlet = Schematic.newDocument();
        servlet.set(FieldName.CLASSNAME, "servlet");
        providers.add(servlet);
    }

    private EditableDocument parseWorkspaces(OperationContext context,
                                             ModelNode model,
                                             EditableDocument configDoc, List<String> additionalClasspathEntries) throws OperationFailedException {
        EditableDocument workspacesDoc = configDoc.getOrCreateDocument(FieldName.WORKSPACES);
        boolean allowWorkspaceCreation = attribute(context, model, ModelAttributes.ALLOW_WORKSPACE_CREATION).asBoolean();
        String defaultWorkspaceName = attribute(context, model, ModelAttributes.DEFAULT_WORKSPACE).asString();
        workspacesDoc.set(FieldName.ALLOW_CREATION, allowWorkspaceCreation);
        workspacesDoc.set(FieldName.DEFAULT, defaultWorkspaceName);
        if (model.hasDefined(ModelKeys.WORKSPACES_CACHE_SIZE)) {
            workspacesDoc.set(FieldName.WORKSPACE_CACHE_SIZE, model.get(ModelKeys.WORKSPACES_CACHE_SIZE).asInt());
        }
        if (model.hasDefined(ModelKeys.PREDEFINED_WORKSPACE_NAMES)) {
            for (ModelNode name : model.get(ModelKeys.PREDEFINED_WORKSPACE_NAMES).asList()) {
                workspacesDoc.getOrCreateArray(FieldName.PREDEFINED).add(name.asString());
            }

            if (model.hasDefined(ModelKeys.WORKSPACES_INITIAL_CONTENT)) {
                EditableDocument initialContentDocument = workspacesDoc.getOrCreateDocument(FieldName.INITIAL_CONTENT);
                List<ModelNode> workspacesInitialContent = model.get(ModelKeys.WORKSPACES_INITIAL_CONTENT).asList();
                for (ModelNode initialContent : workspacesInitialContent) {
                    Property initialContentProperty = initialContent.asProperty();
                    initialContentDocument.set(initialContentProperty.getName(), initialContentProperty.getValue().asString());
                }
            }
        }
        if (model.hasDefined(ModelKeys.DEFAULT_INITIAL_CONTENT)) {
            EditableDocument initialContentDocument = workspacesDoc.getOrCreateDocument(FieldName.INITIAL_CONTENT);
            initialContentDocument.set(FieldName.DEFAULT_INITIAL_CONTENT, model.get(ModelKeys.DEFAULT_INITIAL_CONTENT).asString());
        }
        return workspacesDoc;
    }

    private void parseJournaling( RepositoryService repositoryService,
                                  OperationContext context,
                                  ModelNode model,
                                  EditableDocument configDoc ) throws OperationFailedException {
        if (model.hasDefined(ModelKeys.JOURNALING)) {
            EditableDocument journaling = configDoc.getOrCreateDocument(FieldName.JOURNALING);
            
            if (model.hasDefined(ModelKeys.JOURNAL_ENABLED)) {
                boolean enabled = attribute(context, model, ModelAttributes.JOURNAL_ENABLED).asBoolean();
                journaling.setBoolean(FieldName.JOURNAL_ENABLED, enabled);
            }

            // set it temporarily on the repository service because the final location needs to be resolved later
            if (model.hasDefined(ModelKeys.JOURNAL_RELATIVE_TO)) {
                String relativeTo = attribute(context, model, ModelAttributes.JOURNAL_RELATIVE_TO).asString();
                repositoryService.setJournalRelativeTo(relativeTo);
            }

            // set it temporarily on the repository service because the final location needs to be resolved later
            if (model.hasDefined(ModelKeys.JOURNAL_PATH)) {
                String path = attribute(context, model, ModelAttributes.JOURNAL_PATH).asString();
                repositoryService.setJournalPath(path);
            }

            int maxDaysToKeepRecords = attribute(context, model, ModelAttributes.MAX_DAYS_TO_KEEP_RECORDS).asInt();
            journaling.setNumber(FieldName.MAX_DAYS_TO_KEEP_RECORDS, maxDaysToKeepRecords);

            boolean asyncWrites = attribute(context, model, ModelAttributes.ASYNC_WRITES).asBoolean();
            journaling.setBoolean(FieldName.ASYNC_WRITES_ENABLED, asyncWrites);

            String gcThreadPool = attribute(context, model, ModelAttributes.JOURNAL_GC_THREAD_POOL).asString();
            journaling.setString(FieldName.THREAD_POOL, gcThreadPool);

            String gcInitialTime = attribute(context, model, ModelAttributes.JOURNAL_GC_INITIAL_TIME).asString();
            journaling.setString(FieldName.INITIAL_TIME, gcInitialTime);
        }
    }

    private void parseCustomNodeTypes(ModelNode model,
                                      EditableDocument configDoc, List<String> additionalClasspathEntries) {
        if (model.hasDefined(ModelKeys.NODE_TYPES)) {
            EditableArray nodeTypesArray = configDoc.getOrCreateArray(FieldName.NODE_TYPES);
            for (ModelNode nodeType : model.get(ModelKeys.NODE_TYPES).asList()) {
                nodeTypesArray.add(nodeType.asString());
            }
        }
    }

    private void enableMonitoring( boolean enableMonitoring,
                                   EditableDocument configDoc ) {
        EditableDocument monitoring = configDoc.getOrCreateDocument(FieldName.MONITORING);
        monitoring.set(FieldName.MONITORING_ENABLED, enableMonitoring);
    }
}

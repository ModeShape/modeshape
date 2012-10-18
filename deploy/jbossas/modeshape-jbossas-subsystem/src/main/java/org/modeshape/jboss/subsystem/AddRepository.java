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

import javax.transaction.TransactionManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.transaction.lookup.JBossTransactionManagerLookup;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.common.logging.Logger;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jboss.service.IndexStorage;
import org.modeshape.jboss.service.IndexStorageService;
import org.modeshape.jboss.service.ReferenceFactoryService;
import org.modeshape.jboss.service.RepositoryService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import java.util.List;

public class AddRepository extends AbstractAddStepHandler {

    public static final AddRepository INSTANCE = new AddRepository();

    private AddRepository() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.REPOSITORY_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
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

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler,
                                   final List<ServiceController<?>> newControllers ) throws OperationFailedException {

        final ServiceTarget target = context.getServiceTarget();
        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getLastElement().getValue();
        final String cacheName = attribute(context, model, ModelAttributes.CACHE_NAME, repositoryName);
        final String clusterChannelName = attribute(context, model, ModelAttributes.CLUSTER_NAME, null);
        final String clusterStackName = attribute(context, model, ModelAttributes.CLUSTER_STACK, null);
        final boolean enableMonitoring = attribute(context, model, ModelAttributes.ENABLE_MONITORING).asBoolean();

        // Figure out which cache container to use (by default we'll use Infinispan subsystem's default cache container) ...
        String namedContainer = attribute(context, model, ModelAttributes.CACHE_CONTAINER, "modeshape");

        // Create a document for the repository configuration ...
        EditableDocument configDoc = Schematic.newDocument();
        configDoc.set(FieldName.NAME, repositoryName);

        // Determine the JNDI name ...
        configDoc.set(FieldName.JNDI_NAME, "");// always set to empty string, since we'll register in JNDI here ...
        final String jndiName = ModeShapeJndiNames.JNDI_BASE_NAME + repositoryName;
        String jndiAlias = ModeShapeJndiNames.jndiNameFrom(model, repositoryName);
        if (jndiName.equals(jndiAlias)) jndiAlias = null;

        // Always enable monitoring ...
        EditableDocument monitoring = configDoc.getOrCreateDocument(FieldName.MONITORING);
        monitoring.set(FieldName.MONITORING_ENABLED, enableMonitoring);

        //Initial node-types if configured
        if (model.hasDefined(ModelKeys.NODE_TYPES)) {
            EditableArray nodeTypesArray = configDoc.getOrCreateArray(FieldName.NODE_TYPES);
            for (ModelNode nodeType : model.get(ModelKeys.NODE_TYPES).asList()) {
                nodeTypesArray.add(nodeType.asString());
            }
        }

        // Workspace information is on the repository model node (unlike the XML) ...
        EditableDocument workspacesDoc = configDoc.getOrCreateDocument(FieldName.WORKSPACES);
        boolean allowWorkspaceCreation = attribute(context, model, ModelAttributes.ALLOW_WORKSPACE_CREATION).asBoolean();
        String defaultWorkspaceName = attribute(context, model, ModelAttributes.DEFAULT_WORKSPACE).asString();
        workspacesDoc.set(FieldName.ALLOW_CREATION, allowWorkspaceCreation);
        workspacesDoc.set(FieldName.DEFAULT, defaultWorkspaceName);
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

        // Set the storage information (that was set on the repository ModelNode) ...
        EditableDocument storage = configDoc.getOrCreateDocument(FieldName.STORAGE);
        storage.set(FieldName.CACHE_NAME, cacheName);
        storage.set(FieldName.CACHE_TRANSACTION_MANAGER_LOOKUP, JBossTransactionManagerLookup.class.getName());
        // The proper container will be injected into the RepositoryService, so use the fixed container name ...
        storage.set(FieldName.CACHE_CONFIGURATION, RepositoryService.CONTENT_CONTAINER_NAME);

        EditableDocument security = configDoc.getOrCreateDocument(FieldName.SECURITY);

        // Indexing ...
        EditableDocument query = configDoc.getOrCreateDocument(FieldName.QUERY);
        EditableDocument indexing = query.getOrCreateDocument(FieldName.INDEXING);
        String analyzerClassname = ModelAttributes.ANALYZER_CLASSNAME.resolveModelAttribute(context, model).asString();
        String analyzerClasspath = ModelAttributes.ANALYZER_MODULE.resolveModelAttribute(context, model).asString();
        String indexThreadPool = ModelAttributes.THREAD_POOL.resolveModelAttribute(context, model).asString();
        int indexBatchSize = ModelAttributes.BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        String indexReaderStrategy = ModelAttributes.READER_STRATEGY.resolveModelAttribute(context, model).asString();
        String indexMode = ModelAttributes.MODE.resolveModelAttribute(context, model).asString();
        String systemContentIndexingMode = ModelAttributes.SYSTEM_CONTENT_MODE.resolveModelAttribute(context, model).asString();
        int indexAsyncThreadPoolSize = ModelAttributes.ASYNC_THREAD_POOL_SIZE.resolveModelAttribute(context, model).asInt();
        int indexAsyncMaxQueueSize = ModelAttributes.ASYNC_MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();

        indexing.set(FieldName.INDEXING_ANALYZER, analyzerClassname);
        if (analyzerClasspath != null) {
            indexing.set(FieldName.INDEXING_ANALYZER_CLASSPATH, analyzerClasspath);
        }
        indexing.set(FieldName.THREAD_POOL, indexThreadPool);
        indexing.set(FieldName.INDEXING_BATCH_SIZE, indexBatchSize);
        indexing.set(FieldName.INDEXING_READER_STRATEGY, indexReaderStrategy);
        indexing.set(FieldName.INDEXING_MODE, indexMode);
        indexing.set(FieldName.INDEXING_MODE_SYSTEM_CONTENT, systemContentIndexingMode);
        indexing.set(FieldName.INDEXING_ASYNC_THREAD_POOL_SIZE, indexAsyncThreadPoolSize);
        indexing.set(FieldName.INDEXING_ASYNC_MAX_QUEUE_SIZE, indexAsyncMaxQueueSize);
        for (String key : model.keys()) {
            if (key.startsWith("hibernate")) {
                indexing.set(key, model.get(key).asString());
            }
        }

        // JAAS ...
        EditableDocument jaas = security.getOrCreateDocument(FieldName.JAAS);
        String securityDomain = attribute(context, model, ModelAttributes.SECURITY_DOMAIN).asString();
        jaas.set(FieldName.JAAS_POLICY_NAME, securityDomain);

        // Anonymous ...
        EditableDocument anon = security.getOrCreateDocument(FieldName.ANONYMOUS);
        String anonUsername = attribute(context, model, ModelAttributes.ANONYMOUS_USERNAME).asString();
        boolean useAnonIfFailed = attribute(context, model, ModelAttributes.USE_ANONYMOUS_IF_AUTH_FAILED).asBoolean();
        anon.set(FieldName.ANONYMOUS_USERNAME, anonUsername);
        anon.set(FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS, useAnonIfFailed);
        if (model.hasDefined(ModelKeys.ANONYMOUS_ROLES)) {
            for (ModelNode roleNode : model.get(ModelKeys.ANONYMOUS_ROLES).asList()) {
                anon.getOrCreateArray(FieldName.ANONYMOUS_ROLES).addString(roleNode.asString());
            }
        }

        // Servlet authenticator ...
        EditableArray providers = security.getOrCreateArray(FieldName.PROVIDERS);
        EditableDocument servlet = Schematic.newDocument();
        servlet.set(FieldName.CLASSNAME, "servlet");
        servlet.set(FieldName.NAME, "Authenticator that uses the Servlet context");
        providers.addDocument(servlet);

        // Custom authenticators ...
        if (model.hasDefined(ModelKeys.AUTHENTICATORS)) {
            ModelNode authenticators = model.get(ModelKeys.AUTHENTICATORS);
            for (String name : authenticators.keys()) {
                ModelNode authenticator = authenticators.get(name);
                EditableDocument authDoc = Schematic.newDocument();
                for (String key : authenticator.keys()) {
                    String value = authenticator.get(key).asString();
                    if (key.equals(ModelKeys.CLASSNAME)) {
                        key = FieldName.CLASSNAME;
                    } else if (key.equals(ModelKeys.MODULE)) {
                        key = FieldName.CLASSLOADER;
                    }
                    authDoc.set(key, value);
                }
                providers.add(authDoc);
            }
        }

        // Clustering and the JGroups channel ...
        if (clusterChannelName != null) {
            EditableDocument clustering = configDoc.getOrCreateDocument(FieldName.CLUSTERING);
            clustering.setString(FieldName.CLUSTER_NAME, clusterChannelName);
        }

        // Now create the repository service that manages the lifecycle of the JcrRepository instance ...
        RepositoryConfiguration repositoryConfig = new RepositoryConfiguration(configDoc, repositoryName);
        RepositoryService repositoryService = new RepositoryService(repositoryConfig);
        ServiceName repositoryServiceName = ModeShapeServiceNames.repositoryServiceName(repositoryName);

        // Add the EngineService's dependencies ...
        ServiceBuilder<JcrRepository> builder = target.addService(repositoryServiceName, repositoryService);

        // Add dependency to the ModeShape engine service ...
        builder.addDependency(ModeShapeServiceNames.ENGINE, ModeShapeEngine.class, repositoryService.getEngineInjector());

        // Add dependency to the JGroups channel (used for events) ...
        if (clusterStackName != null) {
            builder.addDependency(ServiceName.JBOSS.append("jgroups", "stack", clusterStackName),
                                  ChannelFactory.class,
                                  repositoryService.getChannelFactoryInjector());
        }

        // Add dependency to the transaction manager ...
        builder.addDependency(ServiceName.JBOSS.append("txn", "TransactionManager"),
                              TransactionManager.class,
                              repositoryService.getTransactionManagerInjector());

        // Add dependency to the Infinispan cache container used for content ...
        builder.addDependency(ServiceName.JBOSS.append("infinispan", namedContainer),
                              CacheContainer.class,
                              repositoryService.getCacheManagerInjector());

        // Add dependency, if necessary, to the workspaces cache container
        String workspacesCacheContainer = attribute(context, model, ModelAttributes.WORKSPACES_CACHE_CONTAINER, null);
        if (workspacesCacheContainer != null && !workspacesCacheContainer.toLowerCase().equalsIgnoreCase(namedContainer)) {
            //there is a different ISPN container configured for the ws caches
            builder.addDependency(ServiceName.JBOSS.append("infinispan", workspacesCacheContainer),
                                  CacheContainer.class,
                                  repositoryService.getWorkspacesCacheContainerInjector());
            //the name is a constant which will be resolved later by the RepositoryService
            workspacesDoc.set(FieldName.WORKSPACE_CACHE_CONFIGURATION, RepositoryService.WORKSPACES_CONTAINER_NAME);
        }

        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER,
                              ModuleLoader.class,
                              repositoryService.getModuleLoaderInjector());

        // Add dependency to the index storage service, which captures the properties for the index storage
        builder.addDependency(ModeShapeServiceNames.indexStorageServiceName(repositoryName),
                              IndexStorage.class,
                              repositoryService.getIndexStorageConfigInjector());

        // Add dependency to the binaries storage service, which captures the properties for the binaries storage
        builder.addDependency(ModeShapeServiceNames.binaryStorageServiceName(repositoryName),
                              BinaryStorage.class,
                              repositoryService.getBinaryStorageInjector());

        // Set up the JNDI binder service ...
        final ReferenceFactoryService<JcrRepository> referenceFactoryService = new ReferenceFactoryService<JcrRepository>();
        final ServiceName referenceFactoryServiceName = repositoryServiceName.append("reference-factory");
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
            Logger.getLogger(getClass()).debug("Binding repository '{0}' to JNDI name '{1}' and '{2}'",
                                               repositoryName,
                                               bindInfo.getAbsoluteJndiName(),
                                               aliasInfo.getAbsoluteJndiName());
        } else {
            Logger.getLogger(getClass()).debug("Binding repository '{0}' to JNDI name '{1}'",
                                               repositoryName,
                                               bindInfo.getAbsoluteJndiName());
        }
        binderBuilder.addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binder.getManagedObjectInjector());
        binderBuilder.addDependency(bindInfo.getParentContextServiceName(),
                                    ServiceBasedNamingStore.class,
                                    binder.getNamingStoreInjector());
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Add dependency to the data directory ...
        ServiceName dataDirServiceName = ModeShapeServiceNames.dataDirectoryServiceName(repositoryName);
        newControllers.add(RelativePathService.addService(dataDirServiceName,
                                                          "modeshape/" + repositoryName,
                                                          ModeShapeExtension.DATA_DIR_VARIABLE,
                                                          target));
        builder.addDependency(dataDirServiceName, String.class, repositoryService.getDataDirectoryPathInjector());

        // Add the default index storage service which will provide the indexing configuration
        IndexStorageService defaultIndexService = new IndexStorageService(repositoryName);
        ServiceBuilder<IndexStorage> indexBuilder = target.addService(ModeShapeServiceNames.indexStorageServiceName(
                repositoryName), defaultIndexService);
        indexBuilder.addDependency(dataDirServiceName, String.class, defaultIndexService.getDataDirectoryPathInjector());
        indexBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Add the default binary storage service which will provide the binary configuration
        BinaryStorageService defaultBinaryService = new BinaryStorageService(repositoryName);
        ServiceBuilder<BinaryStorage> binaryStorageBuilder = target.addService(ModeShapeServiceNames.binaryStorageServiceName(
                repositoryName), defaultBinaryService);
        binaryStorageBuilder.addDependency(dataDirServiceName, String.class, defaultBinaryService.getDataDirectoryPathInjector());
        binaryStorageBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        // Now add the controller for the RepositoryService ...
        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(builder.install());
        newControllers.add(referenceBuilder.install());
        newControllers.add(binderBuilder.install());
        newControllers.add(indexBuilder.install());
        newControllers.add(binaryStorageBuilder.install());
    }

    protected void writeIndexingBackendConfiguration( final OperationContext context,
                                                      final ModelNode storage,
                                                      EditableDocument backend,
                                                      String type ) throws OperationFailedException {
        // Set the type of indexing backend ...
        backend.set(FieldName.TYPE, type);
        backend.set(FieldName.TYPE, FieldValue.INDEXING_BACKEND_TYPE_JMS_MASTER);
        String connJndi = attribute(context, storage, ModelAttributes.CONNECTION_FACTORY_JNDI_NAME).asString();
        String queueJndi = attribute(context, storage, ModelAttributes.QUEUE_JNDI_NAME).asString();
        backend.set(FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME, connJndi);
        backend.set(FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME, queueJndi);
    }
}

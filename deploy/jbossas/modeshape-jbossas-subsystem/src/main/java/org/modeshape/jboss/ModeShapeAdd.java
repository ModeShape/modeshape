/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.modeshape.jboss;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.EngineService;
import org.modeshape.jboss.subsystem.JBossManagedI18n;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;

class ModeShapeAdd extends AbstractAddStepHandler implements DescriptionProvider {

	private static Element[] attributes = {
	};
	
	final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();
	
	@Override
	public ModelNode getModelDescription(Locale locale) {
        final ResourceBundle bundle = JBossManagedI18n.getResourceBundle(locale);
        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("modeshape.add")); //$NON-NLS-1$
        
        describeModeShape(node, REQUEST_PROPERTIES,  bundle); 
		
        return node;
	}

	static void describeModeShape(final ModelNode node, String type, final ResourceBundle bundle) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].describe(node, type, bundle);
		}	
	}
	
	@Override
	protected void populateModel(ModelNode operation, ModelNode model)	throws OperationFailedException {
		populate(operation, model);
	}

	static void populate(ModelNode operation, ModelNode model) {
		for (int i = 0; i < attributes.length; i++) {
			attributes[i].populate(operation, model);
		}
	}
	

	@Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(Module.getCallerModule().getClassLoader());
			initializeModeShapeEngine(context, operation, newControllers);
		} finally {
			Thread.currentThread().setContextClassLoader(classloader);
		}
	}

	private void initializeModeShapeEngine(final OperationContext context, final ModelNode operation, final List<ServiceController<?>> newControllers)
			throws OperationFailedException {
		ServiceTarget target = context.getServiceTarget();
		
		final JBossLifeCycleListener shutdownListener = new JBossLifeCycleListener();
		
//		// Sequencing service
//    	final TranslatorRepository translatorRepo = new TranslatorRepository();
//    	ValueService<TranslatorRepository> translatorService = new ValueService<TranslatorRepository>(new org.jboss.msc.value.Value<TranslatorRepository>() {
//			@Override
//			public TranslatorRepository getValue() throws IllegalStateException, IllegalArgumentException {
//				return translatorRepo;
//			}
//    	});
//    	ServiceController<TranslatorRepository> service = target.addService(TeiidServiceNames.TRANSLATOR_REPO, translatorService).install();
//    	newControllers.add(service);
    	
    	
    	// Jcr Engine
    	final EngineService engine = buildModeShapeEngine(operation);
    	
        ServiceBuilder<JcrEngine> engineBuilder = target.addService(ModeShapeServiceNames.ENGINE, engine);
        engineBuilder.addDependency(ModeShapeServiceNames.REPOSITORY, JcrRepository.class, engine.getJcrRepositoryInjector());
 //       engineBuilder.addDependency(DependencyType.OPTIONAL, TeiidServiceNames.OBJECT_REPLICATOR, ObjectReplicator.class, engine.getObjectReplicatorInjector());
        
        engineBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrEngine> controller = engineBuilder.install(); 
        newControllers.add(controller);
        ServiceContainer container =  controller.getServiceContainer();
        container.addTerminateListener(shutdownListener);
     
    	    	
    	// Register Sequencer deployer
        context.addStep(new AbstractDeploymentChainStep() {
			@Override
			public void execute(DeploymentProcessorTarget processorTarget) {
				
			//  sequencer deployers
			//	processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_JDBC_DRIVER|0x0001,new TranslatorStructureDeployer());
			//	processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE|0x0001, new TranslatorDependencyDeployer());
			//	processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JDBC_DRIVER|0x0001, new TranslatorDeployer());
			}
        	
        }, OperationContext.Stage.RUNTIME);
	}
	
  
//	
//		
//    private BufferServiceImpl  buildBufferManager(ModelNode node) {
//    	BufferServiceImpl bufferManger = new BufferServiceImpl();
//    	
//    	if (node == null) {
//    		return bufferManger;
//    	}
//    	
//    	if (Element.USE_DISK_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setUseDisk(Element.USE_DISK_ATTRIBUTE.asBoolean(node));
//    	}	                	
//    	if (Element.PROCESSOR_BATCH_SIZE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setProcessorBatchSize(Element.PROCESSOR_BATCH_SIZE_ATTRIBUTE.asInt(node));
//    	}	
//    	if (Element.CONNECTOR_BATCH_SIZE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setConnectorBatchSize(Element.CONNECTOR_BATCH_SIZE_ATTRIBUTE.asInt(node));
//    	}	
//    	if (Element.MAX_PROCESSING_KB_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxProcessingKb(Element.MAX_PROCESSING_KB_ATTRIBUTE.asInt(node));
//    	}
//    	if (Element.MAX_RESERVED_KB_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxReserveKb(Element.MAX_RESERVED_KB_ATTRIBUTE.asInt(node));
//    	}
//    	if (Element.MAX_FILE_SIZE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxFileSize(Element.MAX_FILE_SIZE_ATTRIBUTE.asLong(node));
//    	}
//    	if (Element.MAX_BUFFER_SPACE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxBufferSpace(Element.MAX_BUFFER_SPACE_ATTRIBUTE.asLong(node));
//    	}
//    	if (Element.MAX_OPEN_FILES_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxOpenFiles(Element.MAX_OPEN_FILES_ATTRIBUTE.asInt(node));
//    	}	    
//    	if (Element.MEMORY_BUFFER_SPACE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMemoryBufferSpace(Element.MEMORY_BUFFER_SPACE_ATTRIBUTE.asInt(node));
//    	}  
//    	if (Element.MEMORY_BUFFER_OFFHEAP_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMemoryBufferOffHeap(Element.MEMORY_BUFFER_OFFHEAP_ATTRIBUTE.asBoolean(node));
//    	} 
//    	if (Element.MAX_STORAGE_OBJECT_SIZE_ATTRIBUTE.isDefined(node)) {
//    		bufferManger.setMaxStorageObjectSize(Element.MAX_STORAGE_OBJECT_SIZE_ATTRIBUTE.asInt(node));
//    	}
//    	if (Element.INLINE_LOBS.isDefined(node)) {
//    		bufferManger.setInlineLobs(Element.INLINE_LOBS.asBoolean(node));
//    	}     	
//    	
//    	return bufferManger;
//    }	
//
//    private SessionAwareCache<CachedResults> buildResultsetCache(ModelNode node, BufferManager bufferManager) {
//
//    	CacheConfiguration cacheConfig = new CacheConfiguration();
//    	// these settings are not really used; they are defined by infinispan
//    	cacheConfig.setMaxEntries(1024);
//   		cacheConfig.setMaxAgeInSeconds(7200);
//   		cacheConfig.setType(Policy.EXPIRATION.name());
//    	cacheConfig.setLocation("resultset"); //$NON-NLS-1$
//    	cacheConfig.setMaxStaleness(60);
//    	
//    	if (Element.RSC_ENABLE_ATTRIBUTE.isDefined(node)) {
//    		if (!Element.RSC_ENABLE_ATTRIBUTE.asBoolean(node)) {
//    			return null;
//    		}
//    	}    	
//    	
//    	ClusterableCacheFactory cacheFactory = null;
//
//    	if (Element.RSC_CONTAINER_NAME_ELEMENT.isDefined(node)) {
//    		cacheFactory = new ClusterableCacheFactory();
//    		cacheFactory.setCacheManager(Element.RSC_CONTAINER_NAME_ELEMENT.asString(node));
//    	}
//    	else {
//    		SessionAwareCache<CachedResults> resultsetCache = new SessionAwareCache<CachedResults>(new DefaultCacheFactory(), SessionAwareCache.Type.RESULTSET, cacheConfig);
//        	resultsetCache.setBufferManager(bufferManager);
//        	return resultsetCache;    		
//    	}
//    	
//    	if (Element.RSC_NAME_ELEMENT.isDefined(node)) {
//    		cacheFactory.setResultsetCacheName(Element.RSC_NAME_ELEMENT.asString(node));
//    	}	 
//    	else {
//    		cacheFactory.setResultsetCacheName("resultset"); //$NON-NLS-1$
//    	}
//
//   		if (Element.RSC_MAX_STALENESS_ELEMENT.isDefined(node)) {
//    		cacheConfig.setMaxStaleness(Element.RSC_MAX_STALENESS_ELEMENT.asInt(node));
//    	}
//
//   		SessionAwareCache<CachedResults> resultsetCache = new SessionAwareCache<CachedResults>(cacheFactory, SessionAwareCache.Type.RESULTSET, cacheConfig);
//    	resultsetCache.setBufferManager(bufferManager);
//    	return resultsetCache;
//	}	      
//    
//    
//    private SessionAwareCache<PreparedPlan> buildPreparedPlanCache(ModelNode node, BufferManager bufferManager) {
//    	CacheConfiguration cacheConfig = new CacheConfiguration();
//    	if (Element.PPC_MAX_ENTRIES_ATTRIBUTE.isDefined(node)) {
//    		cacheConfig.setMaxEntries(Element.PPC_MAX_ENTRIES_ATTRIBUTE.asInt(node));
//    	}
//    	else {
//    		cacheConfig.setMaxEntries(512);
//    	}
//    	
//    	if (Element.PPC_MAX_AGE_IN_SECS_ATTRIBUTE.isDefined(node)) {
//    		cacheConfig.setMaxAgeInSeconds(Element.PPC_MAX_AGE_IN_SECS_ATTRIBUTE.asInt(node));
//    	}
//    	else {
//    		cacheConfig.setMaxAgeInSeconds(28800);
//    	}
//    	
//		cacheConfig.setType(Policy.LRU.name());
//    	
//    	cacheConfig.setLocation("prepared"); //$NON-NLS-1$
//    	SessionAwareCache<PreparedPlan> cache = new SessionAwareCache<PreparedPlan>(new DefaultCacheFactory(), SessionAwareCache.Type.PREPAREDPLAN, cacheConfig);
//    	cache.setBufferManager(bufferManager);
//    	
//    	return cache;
//	}	    
//    
//    
	private EngineService buildModeShapeEngine(ModelNode node) {
		EngineService engine = new EngineService(new JcrEngine());
//		
//		engine.
//    	
//    	if (Element.MAX_THREADS_ELEMENT.isDefined(node)) {
//    		engine.setMaxThreads(Element.MAX_THREADS_ELEMENT.asInt(node));
//    	}
//    	if (Element.MAX_ACTIVE_PLANS_ELEMENT.isDefined(node)) {
//    		engine.setMaxActivePlans(Element.MAX_ACTIVE_PLANS_ELEMENT.asInt(node));
//    	}
//    	if (Element.USER_REQUEST_SOURCE_CONCURRENCY_ELEMENT.isDefined(node)) {
//    		engine.setUserRequestSourceConcurrency(Element.USER_REQUEST_SOURCE_CONCURRENCY_ELEMENT.asInt(node));
//    	}	
//    	if (Element.TIME_SLICE_IN_MILLI_ELEMENT.isDefined(node)) {
//    		engine.setTimeSliceInMilli(Element.TIME_SLICE_IN_MILLI_ELEMENT.asInt(node));
//    	}
//    	if (Element.MAX_ROWS_FETCH_SIZE_ELEMENT.isDefined(node)) {
//    		engine.setMaxRowsFetchSize(Element.MAX_ROWS_FETCH_SIZE_ELEMENT.asInt(node));
//    	}
//    	if (Element.LOB_CHUNK_SIZE_IN_KB_ELEMENT.isDefined(node)) {
//    		engine.setLobChunkSizeInKB(Element.LOB_CHUNK_SIZE_IN_KB_ELEMENT.asInt(node));
//    	}
//    	if (Element.QUERY_THRESHOLD_IN_SECS_ELEMENT.isDefined(node)) {
//    		engine.setQueryThresholdInSecs(Element.QUERY_THRESHOLD_IN_SECS_ELEMENT.asInt(node));
//    	}
//    	if (Element.MAX_SOURCE_ROWS_ELEMENT.isDefined(node)) {
//    		engine.setMaxSourceRows(Element.MAX_SOURCE_ROWS_ELEMENT.asInt(node));
//    	}
//    	if (Element.EXCEPTION_ON_MAX_SOURCE_ROWS_ELEMENT.isDefined(node)) {
//    		engine.setExceptionOnMaxSourceRows(Element.EXCEPTION_ON_MAX_SOURCE_ROWS_ELEMENT.asBoolean(node));
//    	}
//    	if (Element.DETECTING_CHANGE_EVENTS_ELEMENT.isDefined(node)) {
//    		engine.setDetectingChangeEvents(Element.DETECTING_CHANGE_EVENTS_ELEMENT.asBoolean(node));
//    	}	 
//    	if (Element.QUERY_TIMEOUT.isDefined(node)) {
//    		engine.setQueryTimeout(Element.QUERY_TIMEOUT.asLong(node));
//    	}
		return engine;
	}    
//	
//	static class VDBStatusCheckerExecutorService extends VDBStatusChecker{
//		final InjectedValue<Executor> executorInjector = new InjectedValue<Executor>();
//		
//		public VDBStatusCheckerExecutorService(VDBRepository vdbRepository) {
//			super(vdbRepository);
//		}
//		
//		@Override
//		public Executor getExecutor() {
//			return this.executorInjector.getValue();
//		}    		
//	}
}

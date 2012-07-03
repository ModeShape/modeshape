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
package org.modeshape.jboss.service;

import javax.jcr.RepositoryException;
import javax.transaction.TransactionManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.DelegatingClassLoader;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.subsystem.MappedAttributeDefinition;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.jcr.RepositoryConfiguration.QueryRebuild;
import java.util.ArrayList;
import java.util.List;

/**
 * A <code>RepositoryService</code> instance is the service responsible for initializing a {@link JcrRepository} in the ModeShape
 * engine using the information from the configuration.
 */
public class RepositoryService implements Service<JcrRepository>, Environment {

    public static final String CONTENT_CONTAINER_NAME = "content";
    public static final String BINARY_STORAGE_CONTAINER_NAME = "binaries";

    private static final Logger LOG = Logger.getLogger(RepositoryService.class.getPackage().getName());

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<ModeShapeEngine>();
    private final InjectedValue<CacheContainer> cacheManagerInjector = new InjectedValue<CacheContainer>();
    private final InjectedValue<TransactionManager> txnMgrInjector = new InjectedValue<TransactionManager>();
    private final InjectedValue<ChannelFactory> channelFactoryInjector = new InjectedValue<ChannelFactory>();
    private final InjectedValue<IndexStorage> indexStorageConfigInjector = new InjectedValue<IndexStorage>();
    private final InjectedValue<BinaryStorage> binaryStorageInjector = new InjectedValue<BinaryStorage>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<String>();
    private final InjectedValue<ModuleLoader> moduleLoaderInjector = new InjectedValue<ModuleLoader>();

    private RepositoryConfiguration repositoryConfiguration;

    public RepositoryService( RepositoryConfiguration repositoryConfiguration ) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

    @Override
    public JcrRepository getValue() throws IllegalStateException, IllegalArgumentException {
        try {
            return getEngine().getRepository(repositoryName());
        } catch (NoSuchRepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModeShapeEngine getEngine() {
        return engineInjector.getValue();
    }

    @Override
    public CacheContainer getCacheContainer( String name ) {
        CacheContainer container = null;
        if (BINARY_STORAGE_CONTAINER_NAME.equals(name)) {
            BinaryStorage storage = binaryStorageInjector.getValue();
            container = storage.getCacheContainer();
        }
        if (container == null) {
            container = cacheManagerInjector.getValue();
        }
        return container;
    }

    @Override
    public Channel getChannel( String name ) throws Exception {
        return channelFactoryInjector.getValue().createChannel(name);
    }

    @Override
    public ClassLoader getClassLoader( ClassLoader fallbackLoader,
                                       String... classpathEntries ) {
        List<ClassLoader> delegatingLoaders = new ArrayList<ClassLoader>();
        if (classpathEntries != null) {
            //each classpath entry is interpreted as a module identifier
            for (String moduleIdString : classpathEntries) {
                if (!StringUtil.isBlank(moduleIdString)) {
                    try {
                        ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(moduleIdString);
                        delegatingLoaders.add(moduleLoader().loadModule(moduleIdentifier).getClassLoader());
                    } catch (IllegalArgumentException e) {
                        LOG.warnv("The string (classpath entry) is not a valid module identifier: {0}", moduleIdString);
                    }
                    catch (ModuleLoadException e) {
                        LOG.warnv("Cannot load module from (from classpath entry) with identifier: {0}", moduleIdString);
                    }
                }
            }
        }
        if (fallbackLoader != null) {
            delegatingLoaders.add(fallbackLoader);
        }

        ClassLoader parentLoader = getClass().getClassLoader();
        return delegatingLoaders.isEmpty() ? parentLoader : new DelegatingClassLoader(parentLoader, delegatingLoaders);
    }

    @Override
    public void shutdown() {
        // Do nothing; this is the Environment's shutdown method
    }

    public final String repositoryName() {
        return repositoryConfiguration.getName();
    }

    @Override
    public void start( StartContext arg0 ) throws StartException {
        ModeShapeEngine engine = getEngine();

        try {
            final String repositoryName = repositoryName();

            // Get the index storage configuration ...
            IndexStorage indexStorageConfig = indexStorageConfigInjector.getValue();
            Document queryConfig = null;
            if (indexStorageConfig != null) {
                queryConfig = indexStorageConfig.getQueryConfiguration();
            } else {
                // We'll use the default index storage, but this will be overwritten by the *IndexStorageAdd operation
                // (that we're dependent upon). The default for non-AS7 ModeShape repositories is to use
                // RAM index storage, but in AS7 we want to by default store the indexes on the filesystem in the
                // AS7 data directory.
                // We'll do this by setting a path relative to the data directory, and then injecting
                // the "${jboss.server.data.dir}/modeshape" path into the repository service
                // (which will then update the configuration prior to deployment) ...
                EditableDocument query = Schematic.newDocument();
                EditableDocument indexing = query.getOrCreateDocument(FieldName.INDEXING);
                EditableDocument indexStorage = query.getOrCreateDocument(FieldName.INDEX_STORAGE);
                EditableDocument backend = indexing.getOrCreateDocument(FieldName.INDEXING_BACKEND);
                query.set(FieldName.REBUILD_UPON_STARTUP, QueryRebuild.IF_MISSING.toString().toLowerCase());
                backend.set(FieldName.TYPE, FieldValue.INDEXING_BACKEND_TYPE_LUCENE);
                indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_FILESYSTEM);
                String dataDirPath = dataDirectoryPathInjector.getValue();
                indexStorage.set(FieldName.INDEX_STORAGE_LOCATION, dataDirPath + "/" + repositoryName + "/indexes");
                queryConfig = query;
            }
            assert queryConfig != null;

            // Get the binary storage configuration ...
            Document binaryConfig = null;
            BinaryStorage binaryStorageConfig = binaryStorageInjector.getValue();
            if (binaryStorageConfig != null) {
                binaryConfig = binaryStorageConfig.getBinaryConfiguration();
            } else {
                // By default, store the binaries in the data directory ...
                EditableDocument binaries = Schematic.newDocument();
                binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_FILE);
                String dataDirPath = dataDirectoryPathInjector.getValue();
                binaries.set(FieldName.DIRECTORY, dataDirPath + "/" + repositoryName + "/binaries");
                binaryConfig = binaries;
            }

            // Create a new configuration document ...
            EditableDocument config = Schematic.newDocument(repositoryConfiguration.getDocument());
            config.setDocument(FieldName.QUERY, queryConfig);
            config.getOrCreateDocument(FieldName.STORAGE).setDocument(FieldName.BINARY_STORAGE, binaryConfig);

            if (LOG.isDebugEnabled()) {
                LOG.debugv("ModeShape configuration for '{0}' repository: {1}", repositoryName, config);
                Problems problems = repositoryConfiguration.validate();
                if (problems.isEmpty()) {
                    LOG.debugv("Problems with configuration for '{0}' repository: {1}", repositoryName, problems);
                }
            }

            // Create a new (updated) configuration ...
            repositoryConfiguration = new RepositoryConfiguration(config, repositoryName);

            // Deploy the repository and use this as the environment ...
            engine.deploy(repositoryConfiguration.with(this));
        } catch (ConfigurationException e) {
            throw new StartException(e);
        } catch (RepositoryException e) {
            throw new StartException(e);
        }

    }

    @Override
    public void stop( StopContext context ) {
        ModeShapeEngine engine = getEngine();
        if (engine != null) {
            try {
                // Undeploy the repository ...
                engine.undeploy(repositoryName());
            } catch (NoSuchRepositoryException e) {
                // The repository doesn't exist, so no worries ...
            }
        }
    }

    /**
     * Immediately change and apply the specified field in the current repository configuration to the new value.
     *
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeField( MappedAttributeDefinition defn,
                             ModelNode newValue ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the Document containing the field ...
        EditableDocument fieldContainer = editor;
        for (String fieldName : defn.getPathToContainerOfField()) {
            fieldContainer = editor.getOrCreateDocument(fieldName);
        }

        // Get the raw value from the model node ...
        Object rawValue = defn.getTypedValue(newValue);

        // Change the field ...
        String fieldName = defn.getFieldName();
        fieldContainer.set(fieldName, rawValue);

        // Apply the changes to the current configuration ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified sequencer field in the current repository configuration to the new value.
     *
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param sequencerName the name of the sequencer
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeSequencerField( MappedAttributeDefinition defn,
                                      ModelNode newValue,
                                      String sequencerName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument sequencing = editor.getOrCreateDocument(pathToContainer.get(0));
        EditableDocument sequencers = sequencing.getOrCreateArray(pathToContainer.get(1));

        // The container should be an array ...
        for (String configuredSequencerName : sequencers.keySet()) {
            // Look for the entry with a name that matches our sequencer name ...
            if (sequencerName.equals(configuredSequencerName)) {
                // All these entries should be nested documents ...
                EditableDocument sequencer = (EditableDocument)sequencers.get(configuredSequencerName);

                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                sequencer.set(fieldName, rawValue);
                break;
            }
        }

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified extractor field in the current repository configuration to the new value.
     *
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param extractorName the name of the sequencer
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeTextExtractorField( MappedAttributeDefinition defn,
                                          ModelNode newValue,
                                          String extractorName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument textExtracting = editor.getOrCreateDocument(pathToContainer.get(1));
        EditableDocument extractors = textExtracting.getOrCreateDocument(pathToContainer.get(2));

        // The container should be an array ...
        for (String configuredExtractorName : extractors.keySet()) {
            // Look for the entry with a name that matches our extractor name ...
            if (extractorName.equals(configuredExtractorName)) {
                // All these entries should be nested documents ...
                EditableDocument extractor = (EditableDocument)extractors.get(configuredExtractorName);
                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                extractor.set(fieldName, rawValue);
                break;
            }
        }

        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * @return the injector used to set the configuration details for the index storage
     */
    public InjectedValue<IndexStorage> getIndexStorageConfigInjector() {
        return indexStorageConfigInjector;
    }

    /**
     * @return the injector used to set the configuration details for the binaries storage
     */
    public InjectedValue<BinaryStorage> getBinaryStorageInjector() {
        return binaryStorageInjector;
    }

    /**
     * @return the injector used to set the {@link ModeShapeEngine} reference
     */
    public InjectedValue<ModeShapeEngine> getEngineInjector() {
        return engineInjector;
    }

    /**
     * @return the injector used to set the TransactionManager reference
     */
    public InjectedValue<TransactionManager> getTransactionManagerInjector() {
        return txnMgrInjector;
    }

    /**
     * @return the injector used to set the CacheContainer reference used for content storage
     */
    public InjectedValue<CacheContainer> getCacheManagerInjector() {
        return cacheManagerInjector;
    }

    /**
     * @return the injector used to set the JGroups' ChannelFactory
     */
    public InjectedValue<ChannelFactory> getChannelFactoryInjector() {
        return channelFactoryInjector;
    }

    /**
     * @return the injector used to set the data directory for this repository
     */
    public InjectedValue<String> getDataDirectoryPathInjector() {
        return dataDirectoryPathInjector;
    }

    /**
     * @return the injector used to set the jboss module loader
     */
    public InjectedValue<ModuleLoader> getModuleLoaderInjector() {
        return moduleLoaderInjector;
    }

    private ModuleLoader moduleLoader() {
        return moduleLoaderInjector.getValue();
    }
}

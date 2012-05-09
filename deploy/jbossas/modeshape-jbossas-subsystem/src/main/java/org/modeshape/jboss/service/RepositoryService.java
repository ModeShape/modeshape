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

import java.util.List;
import javax.jcr.RepositoryException;
import javax.transaction.TransactionManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.Logger;
import org.modeshape.jboss.subsystem.MappedAttributeDefinition;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.jcr.RepositoryConfiguration.QueryRebuild;

/**
 * A <code>RepositoryService</code> instance is the service responsible for initializing a {@link JcrRepository} in the ModeShape
 * engine using the information from the configuration.
 */
public class RepositoryService implements Service<JcrRepository>, Environment {

    public static final String CONTENT_CONTAINER_NAME = "content";
    public static final String BINARY_STORAGE_CONTAINER_NAME = "binaries";

    private final InjectedValue<JcrEngine> engineInjector = new InjectedValue<JcrEngine>();
    private final InjectedValue<CacheContainer> cacheManagerInjector = new InjectedValue<CacheContainer>();
    private final InjectedValue<TransactionManager> txnMgrInjector = new InjectedValue<TransactionManager>();
    private final InjectedValue<ChannelFactory> channelFactoryInjector = new InjectedValue<ChannelFactory>();
    private final InjectedValue<IndexStorage> indexStorageConfigInjector = new InjectedValue<IndexStorage>();
    private final InjectedValue<BinaryStorage> binaryStorageInjector = new InjectedValue<BinaryStorage>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<String>();

    private RepositoryConfiguration repositoryConfiguration;

    public RepositoryService( RepositoryConfiguration repositoryConfiguration ) {
        this.repositoryConfiguration = repositoryConfiguration;
    }

    @Override
    public JcrRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private JcrEngine getEngine() {
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
    public void shutdown() {
        // Do nothing; this is the Environment's shutdown method
    }

    public final String repositoryName() {
        return repositoryConfiguration.getName();
    }

    @Override
    public void start( StartContext arg0 ) throws StartException {
        JcrEngine engine = getEngine();
        Logger logger = Logger.getLogger(getClass());
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

            if (logger.isDebugEnabled()) {
                logger.debug("ModeShape configuration for '{0}' repository: {1}", repositoryName, config);
                Problems problems = repositoryConfiguration.validate();
                if (problems.isEmpty()) {
                    logger.debug("Problems with configuration for '{0}' repository: {1}", repositoryName, problems);
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
        JcrEngine engine = getEngine();
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
        JcrEngine engine = getEngine();
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
        JcrEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument sequencing = editor.getOrCreateDocument(pathToContainer.get(0));
        EditableArray sequencers = sequencing.getOrCreateArray(pathToContainer.get(1));

        // The container should be an array ...
        for (int i = 0; i != sequencers.size(); ++i) {
            // All these entries should be nested documents ...
            EditableDocument sequencer = (EditableDocument)sequencers.get(i);
            // Look for the entry with a name that matches our sequencer name ...
            if (sequencerName.equals(sequencer.getString(FieldName.NAME))) {
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
     * @return the injector used to set the JcrEngine reference
     */
    public InjectedValue<JcrEngine> getEngineInjector() {
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

}

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
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

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

    private final RepositoryConfiguration repositoryConfiguration;

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
        // do nothing
    }

    @Override
    public void start( StartContext arg0 ) throws StartException {
        JcrEngine jcr = getEngine();
        try {
            final String repositoryName = repositoryConfiguration.getName();

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
                query.set(FieldName.REBUILD_UPON_STARTUP, "if_needed");
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

            // Now update the configuration ...
            Editor editor = repositoryConfiguration.edit();
            editor.setDocument(FieldName.QUERY, queryConfig);
            editor.getOrCreateDocument(FieldName.STORAGE).setDocument(FieldName.BINARY_STORAGE, binaryConfig);

            // Apply the changes to the configuration ...
            editor.apply(editor.getChanges());

            // Deploy the repository and use this as the environment ...
            jcr.deploy(repositoryConfiguration.with(this));
        } catch (ConfigurationException e) {
            throw new StartException(e);
        } catch (RepositoryException e) {
            throw new StartException(e);
        }

    }

    @Override
    public void stop( StopContext arg0 ) {

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

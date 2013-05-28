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
package org.modeshape.jcr.query.lucene.basic;

import java.util.Properties;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.jcr.query.lucene.LuceneSearchConfiguration;

/**
 * The Hibernate Search {@link SearchConfiguration} implementation that specifies how Hibernate Search should be configured.
 */
public class BasicLuceneConfiguration extends LuceneSearchConfiguration {

    /**
     * Create a configuration for the {@link BasicLuceneSchema}.
     * 
     * @param repositoryName the name of the repository; may not be null
     * @param backend the {@link RepositoryConfiguration repository configuration} properties for the Hibernate Search backend;
     *        may not be null
     * @param indexing the {@link RepositoryConfiguration repository configuration} properties for the indexing options; may not
     *        be null
     * @param storage the {@link RepositoryConfiguration repository configuration} properties for index storage options; may not
     *        be null
     */
    public BasicLuceneConfiguration( String repositoryName,
                                     Properties backend,
                                     Properties indexing,
                                     Properties storage ) {
        super(NodeInfo.class, DynamicFieldBridge.class);

        String storageType = initStorageProperties(repositoryName, storage);
        initIndexingProperties(indexing);
        initBackendProperties(storageType, backend);
    }

    private String initStorageProperties( String repositoryName,
                                          Properties storage ) {
        // ------------------------------
        // Set the storage properties ...
        // ------------------------------
        String storageType = storage.getProperty(FieldName.TYPE);
        if (storageType.equals(FieldValue.INDEX_STORAGE_RAM)) {
            // RAM directory provider ...
            setProperty("hibernate.search.default.directory_provider", "ram");
        } else if (storageType.equals(FieldValue.INDEX_STORAGE_FILESYSTEM)) {
            // Filesystem directory provider ...
            String indexBase = storage.getProperty(FieldName.INDEX_STORAGE_LOCATION);
            String lockingStrategy = storage.getProperty(FieldName.INDEX_STORAGE_LOCKING_STRATEGY);
            String accessType = storage.getProperty(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE);
            setProperty("hibernate.search.default.directory_provider", "filesystem");
            setProperty("hibernate.search.default.indexBase", indexBase);
            setProperty("hibernate.search.default.locking_strategy", lockingStrategy);
            setProperty("hibernate.search.default.filesystem_access_type", accessType);
        } else if (storageType.equals(FieldValue.INDEX_STORAGE_FILESYSTEM_MASTER)) {
            // Filesystem-master directory provider ...
            String indexBase = storage.getProperty(FieldName.INDEX_STORAGE_LOCATION);
            String sourceBase = storage.getProperty(FieldName.INDEX_STORAGE_SOURCE_LOCATION);
            String lockingStrategy = storage.getProperty(FieldName.INDEX_STORAGE_LOCKING_STRATEGY);
            String accessType = storage.getProperty(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE);
            String refresh = storage.getProperty(FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS);
            String bufferSize = storage.getProperty(FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES);
            setProperty("hibernate.search.default.directory_provider", "filesystem-master");
            setProperty("hibernate.search.default.indexBase", indexBase);
            setProperty("hibernate.search.default.sourceBase", sourceBase);
            setProperty("hibernate.search.default.refresh", refresh);
            setProperty("hibernate.search.default.buffer_size_on_copy", bufferSize);
            setProperty("hibernate.search.default.locking_strategy", lockingStrategy);
            setProperty("hibernate.search.default.filesystem_access_type", accessType);
        } else if (storageType.equals(FieldValue.INDEX_STORAGE_FILESYSTEM_SLAVE)) {
            // Filesystem-master directory provider ...
            String indexBase = storage.getProperty(FieldName.INDEX_STORAGE_LOCATION);
            String sourceBase = storage.getProperty(FieldName.INDEX_STORAGE_SOURCE_LOCATION);
            String lockingStrategy = storage.getProperty(FieldName.INDEX_STORAGE_LOCKING_STRATEGY);
            String accessType = storage.getProperty(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE);
            String refresh = storage.getProperty(FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS);
            String bufferSize = storage.getProperty(FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES);
            String retryMarkerLookup = storage.getProperty(FieldName.INDEX_STORAGE_RETRY_MARKER_LOOKUP);
            String retryInitializePeriod = storage.getProperty(FieldName.INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS);
            setProperty("hibernate.search.default.directory_provider", "filesystem-slave");
            setProperty("hibernate.search.default.indexBase", indexBase);
            setProperty("hibernate.search.default.sourceBase", sourceBase);
            setProperty("hibernate.search.default.refresh", refresh);
            setProperty("hibernate.search.default.buffer_size_on_copy", bufferSize);
            setProperty("hibernate.search.default.retry_marker_lookup", retryMarkerLookup);
            setProperty("hibernate.search.default.retry_initialize_period", retryInitializePeriod);
            setProperty("hibernate.search.default.locking_strategy", lockingStrategy);
            setProperty("hibernate.search.default.filesystem_access_type", accessType);
        } else if (storageType.equals(FieldValue.INDEX_STORAGE_CUSTOM)) {
            storageType = storage.getProperty(FieldName.CLASSNAME);
            setProperty("hibernate.search.default.directory_provider", storageType);
            // Now set the extra properties ...
            for (Object keyObj : storage.keySet()) {
                String key = keyObj.toString();
                if (key.equals(FieldName.CLASSNAME) || key.equals(FieldName.CLASSLOADER) || key.equals(FieldName.TYPE)) continue;
                setProperty("hibernate.search.default." + key, storage.getProperty(key));
            }
        }
        return storageType;
    }

    private void initIndexingProperties( Properties indexing ) {
        // ----------------------------------
        // Define the indexing properties ...
        // ----------------------------------
        String analyzer = indexing.getProperty(FieldName.INDEXING_ANALYZER);
        if (!StandardAnalyzer.class.getName().equals(analyzer)) {
            // Hibernate Search defaults to StandardAnalyzer, which is also our default. But there's an issue loading
            // the class when running in AS7, because ConfigContext uses org.hibernate.annotations.common.util.ReflectHelper,
            // which apparently uses the Hibernate module's classpath (which does not contain the Lucene library).
            setProperty(Environment.ANALYZER_CLASS, analyzer);
        }

        String similarity = indexing.getProperty(FieldName.INDEXING_SIMILARITY);
        setProperty(Environment.SIMILARITY_CLASS, similarity);

        String batchSize = indexing.getProperty(FieldName.INDEXING_BATCH_SIZE);
        setProperty(Environment.QUEUEINGPROCESSOR_BATCHSIZE, batchSize);

        String indexFormat = indexing.getProperty(FieldName.INDEXING_INDEX_FORMAT);
        setProperty(Environment.LUCENE_MATCH_VERSION, indexFormat);

        String readerStrategy = indexing.getProperty(FieldName.INDEXING_READER_STRATEGY);
        setProperty("hibernate.search.reader.strategy", readerStrategy);

        String mode = indexing.getProperty(FieldName.INDEXING_MODE);
        setProperty("hibernate.search.default.worker.execution", mode); // sync or async

        String asyncThreadPoolSize = indexing.getProperty(FieldName.INDEXING_ASYNC_THREAD_POOL_SIZE);
        setProperty("hibernate.search.default.worker.thread_pool.size", asyncThreadPoolSize);

        String asyncMaxQueueSize = indexing.getProperty(FieldName.INDEXING_ASYNC_MAX_QUEUE_SIZE);
        setProperty("hibernate.search.default.worker.buffer_queue.max", asyncMaxQueueSize);

        for (Object keyObj : indexing.keySet()) {
            String key = keyObj.toString();
            if (key.startsWith("hibernate.search.")) {
                String value = indexing.getProperty(key);
                setProperty(key, value);
            }
        }
    }

    private String initBackendProperties( String storageType,
                                          Properties backend ) {
        // ---------------------------------
        // Define the backend properties ...
        // ---------------------------------

        //jms master is ignored, because that should be standard lucene configuration
        String backendType = backend.getProperty(FieldName.TYPE);
        if (backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_LUCENE)) {
            setProperty("hibernate.search.default.worker.backend", "lucene");
        } else if (backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_BLACKHOLE)) {
            setProperty("hibernate.search.default.worker.backend", "blackhole");
        } else if (backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_JMS_SLAVE)) {
            String queueJndiName = backend.getProperty(FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME);
            String factoryJndiName = backend.getProperty(FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME);
            setProperty("hibernate.search.default.worker.backend", "jms");
            setProperty("hibernate.search.default.worker.jms.queue", queueJndiName);
            setProperty("hibernate.search.default.worker.jms.connection_factory", factoryJndiName);
            // Now set the extra properties ...
            for (Object keyObj : backend.keySet()) {
                String key = keyObj.toString();
                if (key.equals(FieldName.TYPE) || key.equals(FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME)
                        || key.equals(FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME)) continue;
                setProperty("hibernate.search.default.worker." + key, backend.getProperty(key));
            }
        } else if (backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_JGROUPS_MASTER)
                || backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_JGROUPS_SLAVE)) {
            String type = backendType.equals(FieldValue.INDEXING_BACKEND_TYPE_JGROUPS_MASTER) ? "jgroupsMaster" : "jgroupsSlave";
            String channel = backend.getProperty(FieldName.INDEXING_BACKEND_JGROUPS_CHANNEL_NAME);
            String config = backend.getProperty(FieldName.INDEXING_BACKEND_JGROUPS_CHANNEL_CONFIGURATION);
            setProperty("hibernate.search.default.worker.backend", type);
            setProperty(JGroupsChannelProvider.CLUSTER_NAME, channel);
            setProperty(JGroupsChannelProvider.CONFIGURATION_FILE, config);
        } else if (storageType.equals(FieldValue.INDEX_STORAGE_CUSTOM)) {
            String classname = backend.getProperty(FieldName.CLASSNAME);
            setProperty("hibernate.search.default.worker.backend", classname);
        }
        return backendType;
    }

    @Override
    public boolean isIndexMetadataComplete() {
        return true;
    }
}

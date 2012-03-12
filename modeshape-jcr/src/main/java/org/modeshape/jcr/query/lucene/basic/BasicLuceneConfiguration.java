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

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
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

        // ------------------------------
        // Set the storage properties ...
        // ------------------------------
        String storageType = storage.getProperty(FieldName.TYPE);
        if (storageType.equals(FieldName.INDEX_STORAGE_RAM)) {
            // RAM directory provider ...
            setProperty("hibernate.search.default.directory_provider", "ram");
        } else if (storageType.equals(FieldName.INDEX_STORAGE_FILESYSTEM)) {
            // Filesystem directory provider ...
            String indexBase = storage.getProperty(FieldName.INDEX_STORAGE_LOCATION);
            String lockingStrategy = storage.getProperty(FieldName.INDEX_STORAGE_LOCKING_STRATEGY);
            String accessType = storage.getProperty(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE);
            setProperty("hibernate.search.default.directory_provider", "filesystem");
            setProperty("hibernate.search.default.indexBase", indexBase);
            setProperty("hibernate.search.default.locking_strategy", lockingStrategy);
            setProperty("hibernate.search.default.filesystem_access_type", accessType);
        } else if (storageType.equals(FieldName.INDEX_STORAGE_FILESYSTEM_MASTER)) {
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
        } else if (storageType.equals(FieldName.INDEX_STORAGE_FILESYSTEM_SLAVE)) {
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
        } else if (storageType.equals(FieldName.INDEX_STORAGE_INFINISPAN)) {
            // Filesystem-master directory provider ...
            String lockCacheName = repositoryName + "-index-lock";
            String dataCacheName = repositoryName + "-index-data";
            String metaCacheName = repositoryName + "-index-meta";
            String chunkSize = storage.getProperty(FieldName.INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES);
            setProperty("hibernate.search.default.directory_provider", "infinispan");
            setProperty("hibernate.search.default.locking_cachename", lockCacheName);
            setProperty("hibernate.search.default.data_cachename", dataCacheName);
            setProperty("hibernate.search.default.metadata_cachename", metaCacheName);
            setProperty("hibernate.search.default.chunk_size", chunkSize);
        } else if (storageType.equals(FieldName.INDEX_STORAGE_CUSTOM)) {
            storageType = storage.getProperty(FieldName.CLASSNAME);
            setProperty("hibernate.search.default.directory_provider", storageType);
            // Now set the extra properties ...
            for (Object keyObj : storage.keySet()) {
                String key = keyObj.toString();
                if (key.equals(FieldName.CLASSNAME) || key.equals(FieldName.CLASSPATH) || key.equals(FieldName.TYPE)) continue;
                setProperty("hibernate.search.default." + key, storage.getProperty(key));
            }
        }

        // ----------------------------------
        // Define the indexing properties ...
        // ----------------------------------
        String analyzer = indexing.getProperty(FieldName.INDEXING_ANALYZER);
        String similarity = indexing.getProperty(FieldName.INDEXING_SIMILARITY);
        String batchSize = indexing.getProperty(FieldName.INDEXING_BATCH_SIZE);
        String indexFormat = indexing.getProperty(FieldName.INDEXING_INDEX_FORMAT);
        String readerStrategy = indexing.getProperty(FieldName.INDEXING_READER_STRATEGY);
        String mode = indexing.getProperty(FieldName.INDEXING_MODE);
        String asyncThreadPoolSize = indexing.getProperty(FieldName.INDEXING_ASYNC_THREAD_POOL_SIZE);
        String asyncMaxQueueSize = indexing.getProperty(FieldName.INDEXING_ASYNC_MAX_QUEUE_SIZE);

        setProperty("hibernate.search.analyzer", analyzer);
        setProperty("hibernate.search.similarity", similarity);
        setProperty("hibernate.search.worker.batch_size", batchSize);
        setProperty("hibernate.search.lucene_version", indexFormat);
        setProperty("hibernate.search.reader.strategy", readerStrategy);
        setProperty("hibernate.search.worker.execution", mode); // sync or async
        setProperty("hibernate.search.worker.thread_pool.size", asyncThreadPoolSize);
        setProperty("hibernate.search.worker.buffer_queue.max", asyncMaxQueueSize);

        for (Object keyObj : indexing.keySet()) {
            String key = keyObj.toString();
            if (key.startsWith("hibernate.search.")) {
                String value = indexing.getProperty(key);
                setProperty(key, value);
            }
        }

        // ---------------------------------
        // Define the backend properties ...
        // ---------------------------------
        String backendType = backend.getProperty(FieldName.TYPE);
        if (backendType.equals(FieldName.INDEXING_BACKEND_TYPE_LUCENE)) {
            setProperty("hibernate.search.default.worker.backend", "lucene");
        } else if (backendType.equals(FieldName.INDEXING_BACKEND_TYPE_BLACKHOLE)) {
            setProperty("hibernate.search.default.worker.backend", "blackhole");
        } else if (backendType.equals(FieldName.INDEXING_BACKEND_TYPE_JMS_MASTER)) {
            String queueJndiName = backend.getProperty(FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME);
            String factoryJndiName = backend.getProperty(FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME);
            setProperty("hibernate.search.default.worker.backend", "lucene");
            // Now create a component that pulls from the JMS object and writes to the local Lucene backend ...
            // TODO: Query (JMS)
        } else if (backendType.equals(FieldName.INDEXING_BACKEND_TYPE_JMS_SLAVE)) {
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
                setProperty("hibernate.search.default.worker.jndi." + key, backend.getProperty(key));
            }
        } else if (backendType.equals(FieldName.INDEXING_BACKEND_TYPE_JGROUPS_MASTER)
                   || backendType.equals(FieldName.INDEXING_BACKEND_TYPE_JGROUPS_SLAVE)) {
            String type = backendType.equals(FieldName.INDEXING_BACKEND_TYPE_JGROUPS_MASTER) ? "jgroupsMaster" : "jgroupsSlave";
            String channel = backend.getProperty(FieldName.INDEXING_BACKEND_JGROUPS_CHANNEL_NAME);
            String config = backend.getProperty(FieldName.INDEXING_BACKEND_JGROUPS_CHANNEL_CONFIGURATION);
            setProperty("hibernate.search.default.worker.backend", type);
            setProperty("hibernate.search.default.worker.jgroups.clusterName", channel);
            if (isFileOrClasspath(config)) {
                setProperty("hibernate.search.default.worker.backend.jgroups.configurationFile", config);
            } else if (isJGroupsXml(config)) {
                setProperty("hibernate.search.default.worker.backend.jgroups.configurationXml", config);
            } else {
                setProperty("hibernate.search.default.worker.backend.jgroups.configurationString", config);
            }
        } else if (storageType.equals(FieldName.INDEX_STORAGE_CUSTOM)) {
            String classname = backend.getProperty(FieldName.CLASSNAME);
            setProperty("hibernate.search.default.worker.backend", classname);
        }

    }

    protected static boolean isFileOrClasspath( String value ) {
        if (value == null) return false;
        value = value.trim();
        try {
            // Try a file ...
            File file = new File(value);
            if (file.exists() && file.isFile()) return true;
        } catch (Throwable t) {
            // ignore ...
        }
        try {
            // Try a classpath resource ...
            URL url = BasicLuceneConfiguration.class.getClassLoader().getResource(value);
            if (url != null) return true;
        } catch (Throwable t) {
            // ignore ...
        }
        return false;
    }

    protected static boolean isJGroupsXml( String value ) {
        if (value == null) return false;
        value = value.trim();
        if (value.contains("<config") && value.contains("xmlns=\"urn:org:jgroups\"")) return true;
        return false;
    }
}

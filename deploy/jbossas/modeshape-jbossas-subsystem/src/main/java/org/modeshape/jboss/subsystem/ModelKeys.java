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

/**
 * Constants used in the ModeShape subsystem model.
 */
public class ModelKeys {

    static final String REPOSITORY = "repository";
    static final String WEBAPP = "webapp";

    static final String ACCESS_TYPE = "access-type";
    static final String ALLOW_WORKSPACE_CREATION = "allow-workspace-creation";
    static final String WORKSPACES_CACHE_CONTAINER = "workspaces-cache-container";
    static final String ANALYZER_CLASSNAME = "indexing-analyzer-classname";
    static final String ANALYZER_MODULE = "indexing-analyzer-module";
    static final String ANONYMOUS_ROLE = "anonymous-role";
    static final String ANONYMOUS_ROLES = "anonymous-roles";
    static final String ANONYMOUS_USERNAME = "anonymous-username";
    static final String ASYNC_MAX_QUEUE_SIZE = "indexing-async-max-queue-size";
    static final String ASYNC_THREAD_POOL_SIZE = "indexing-async-thread-pool-size";
    static final String BATCH_SIZE = "indexing-batch-size";
    static final String STORAGE_TYPE = "storage-type";
    static final String NESTED_STORAGE_TYPE_FILE = "nested-storage-type-file";
    static final String NESTED_STORAGE_TYPE_CACHE = "nested-storage-type-cache";
    static final String NESTED_STORAGE_TYPE_DB = "nested-storage-type-db";
    static final String NESTED_STORAGE_TYPE_CUSTOM = "nested-storage-type-custom";
    static final String CACHE_CONTAINER = "cache-container";
    static final String CACHE_NAME = "cache-name";
    static final String CLUSTER_NAME = "cluster-name";
    static final String CLUSTER_STACK = "cluster-stack";
    static final String CLASSNAME = "classname";
    static final String CONNECTION_FACTORY_JNDI_NAME = "connection-factory-jndi-name";
    static final String COPY_BUFFER_SIZE = "copy-buffer-size";
    static final String DATA_CACHE_NAME = "data-cache-name";
    static final String DATA_SOURCE_JNDI_NAME = "data-source-jndi-name";
    static final String DEFAULT_WORKSPACE = "default-workspace";
    static final String DOCUMENT_OPTIMIZATION_THREAD_POOL = "document-optimization-thread-pool";
    static final String DOCUMENT_OPTIMIZATION_INITIAL_TIME = "document-optimization-initial-time";
    static final String DOCUMENT_OPTIMIZATION_INTERVAL = "document-optimization-interval";
    static final String DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET = "document-optimization-child-count-target";
    static final String DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE = "document-optimization-child-count-tolerance";
    static final String ENABLE_MONITORING = "enable-monitoring";
    static final String ENABLE_QUERIES = "enable-queries";
    static final String GARBAGE_COLLECTION_THREAD_POOL = "garbage-collection-thread-pool";
    static final String GARBAGE_COLLECTION_INITIAL_TIME = "garbage-collection-initial-time";
    static final String GARBAGE_COLLECTION_INTERVAL = "garbage-collection-interval";
    static final String INDEX_FORMAT = "index-format";
    static final String INDEX_STORAGE_TYPE = "index-storage-type";
    static final String JNDI_NAME = "jndi-name";
    static final String LOCK_CACHE_NAME = "lock-cache-name";
    static final String LOCKING_STRATEGY = "locking-strategy";
    static final String METADATA_CACHE_NAME = "metadata-cache-name";
    static final String CHUNK_SIZE = "chunk-size";
    static final String MINIMUM_BINARY_SIZE = "minimum-binary-size";
    static final String MINIMUM_STRING_SIZE = "minimum-string-size";
    static final String MODE = "indexing-mode";
    /**
     * @deprecated use the REBUILD_INDEXES_UPON_STARTUP, REBUILD_INDEXES_UPON_INCLUDE_SYSTEM_CONTENT and
     *             REBUILD_INDEXES_UPON_STARTUP_MODE attributes
     */
    @Deprecated
    static final String SYSTEM_CONTENT_MODE = "system-content-indexing-mode";
    static final String MODULE = "module";
    static final String NAME = "name";
    static final String PATH = "path";
    static final String PATH_EXPRESSION = "path-expression";
    static final String PATH_EXPRESSIONS = "path-expressions";
    static final String PROPERTY = "property";
    static final String PROPERTIES = "properties";
    static final String PREDEFINED_WORKSPACE_NAME = "predefined-workspace-name";
    static final String PREDEFINED_WORKSPACE_NAMES = "predefined-workspace-names";
    static final String QUEUE_JNDI_NAME = "queue-jndi-name";
    static final String READER_STRATEGY = "indexing-reader-strategy";
    static final String REBUILD_INDEXES_UPON_STARTUP = "rebuild-upon-startup";
    static final String REBUILD_INDEXES_UPON_STARTUP_MODE = "rebuild-upon-startup-mode";
    static final String REBUILD_INDEXES_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT = "rebuild-upon-startup-include-system-content";
    static final String REFRESH_PERIOD = "refresh-period";
    static final String RELATIVE_TO = "relative-to";
    static final String RETRY_INITIALIZE_PERIOD = "retry-initialize-period";
    static final String RETRY_MARKER_LOOKUP = "retry-marker-lookup";
    static final String SECURITY_DOMAIN = "security-domain";
    static final String SEQUENCER_CLASSNAME = "classname";
    static final String TEXT_EXTRACTOR_CLASSNAME = "classname";
    static final String SOURCE_PATH = "source-path";
    static final String SOURCE_RELATIVE_TO = "source-relative-to";
    static final String STORE_NAME = "store-name";
    static final String NESTED_STORES = "nested-stores";
    static final String THREAD_POOL = "indexing-thread-pool";
    static final String USE_ANONYMOUS_IF_AUTH_FAILED = "use-anonymous-upon-failed-authentication";

    static final String AUTHENTICATOR = "authenticator";
    static final String AUTHENTICATOR_CLASSNAME = "classname";

    static final String BINARY_STORAGE = "binary-storage";
    static final String FILE_BINARY_STORAGE = "file-binary-storage";
    static final String DB_BINARY_STORAGE = "db-binary-storage";
    static final String COMPOSITE_BINARY_STORAGE = "composite-binary-storage";
    static final String CACHE_BINARY_STORAGE = "cache-binary-storage";
    static final String CUSTOM_BINARY_STORAGE = "custom-binary-storage";

    static final String SEQUENCER = "sequencer";
    static final String SOURCE = "source";
    static final String TEXT_EXTRACTOR = "text-extractor";

    static final String CONFIGURATION = "configuration";
    static final String INDEX_STORAGE = "index-storage";
    static final String RAM_INDEX_STORAGE = "ram-index-storage";
    static final String LOCAL_FILE_INDEX_STORAGE = "local-file-index-storage";
    static final String MASTER_FILE_INDEX_STORAGE = "master-file-index-storage";
    static final String SLAVE_FILE_INDEX_STORAGE = "slave-file-index-storage";
    static final String CUSTOM_INDEX_STORAGE = "custom-index-storage";
    static final String ADD_RAM_INDEX_STORAGE = "add-ram-index-storage";
    static final String ADD_LOCAL_FILE_INDEX_STORAGE = "add-local-file-index-storage";
    static final String ADD_MASTER_FILE_INDEX_STORAGE = "add-master-file-index-storage";
    static final String ADD_SLAVE_FILE_INDEX_STORAGE = "add-slave-file-index-storage";

    static final String DEFAULT_INITIAL_CONTENT = "default-initial-content";
    static final String WORKSPACES_INITIAL_CONTENT = "workspaces-initial-content";
    static final String INITIAL_CONTENT = "initial-content";

    static final String NODE_TYPES = "node-types";
    static final String NODE_TYPE = "node-type";

    static final String PROJECTIONS = "projections";
    static final String PROJECTION = "projection";
    static final String CONNECTOR_CLASSNAME = "classname";
    static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";
    static final String QUERYABLE = "queryable";
    static final String READONLY = "readonly";

    static final String EXPLODED = "exploded";

    static final String MAX_DAYS_TO_KEEP_RECORDS = "journal-max-days-to-keep-records";
    static final String ASYNC_WRITES = "journal-async-writes";
    static final String JOURNALING = "journaling";
    static final String JOURNAL_GC_THREAD_POOL = "journal-gc-thread-pool";
    static final String JOURNAL_GC_INITIAL_TIME = "journal-gc-initial-time";
    static final String JOURNAL_PATH = "journal-path";
    static final String JOURNAL_RELATIVE_TO = "journal-relative-to";
}

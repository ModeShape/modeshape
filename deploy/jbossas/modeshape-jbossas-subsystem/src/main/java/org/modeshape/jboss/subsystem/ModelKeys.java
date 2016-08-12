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

    static final String ALLOW_WORKSPACE_CREATION = "allow-workspace-creation";
    static final String WORKSPACES_CACHE_SIZE = "cache-size";
    static final String ANONYMOUS_ROLE = "anonymous-role";
    static final String ANONYMOUS_ROLES = "anonymous-roles";
    static final String ANONYMOUS_USERNAME = "anonymous-username";
    static final String STORAGE_TYPE = "storage-type";
    static final String NESTED_STORAGE_TYPE_FILE = "nested-storage-type-file";
    static final String NESTED_STORAGE_TYPE_DB = "nested-storage-type-db";
    static final String NESTED_STORAGE_TYPE_CUSTOM = "nested-storage-type-custom";
    static final String CLASSNAME = "classname";
    static final String REPOSITORY_MODULE_DEPENDENCIES = "depends-on";
    static final String DATA_SOURCE_JNDI_NAME = "data-source-jndi-name";
    static final String DEFAULT_WORKSPACE = "default-workspace";
    static final String DOCUMENT_OPTIMIZATION_THREAD_POOL = "document-optimization-thread-pool";
    static final String DOCUMENT_OPTIMIZATION_INITIAL_TIME = "document-optimization-initial-time";
    static final String DOCUMENT_OPTIMIZATION_INTERVAL = "document-optimization-interval";
    static final String DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET = "document-optimization-child-count-target";
    static final String DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE = "document-optimization-child-count-tolerance";
    static final String EVENT_BUS_SIZE = "event-bus-size";
    static final String LOCK_TIMEOUT_MILLIS = "lock-timeout-millis";
    static final String ENABLE_MONITORING = "enable-monitoring";
    static final String CLUSTER_NAME = "cluster-name";
    static final String CLUSTER_STACK = "cluster-stack";
    static final String CLUSTER_CONFIG = "cluster-config";
    static final String CLUSTER_LOCKING = "cluster-locking";
    static final String GARBAGE_COLLECTION_THREAD_POOL = "garbage-collection-thread-pool";
    static final String GARBAGE_COLLECTION_INITIAL_TIME = "garbage-collection-initial-time";
    static final String GARBAGE_COLLECTION_INTERVAL = "garbage-collection-interval";
    static final String JNDI_NAME = "jndi-name";
    static final String MINIMUM_BINARY_SIZE = "minimum-binary-size";
    static final String MINIMUM_STRING_SIZE = "minimum-string-size";
    static final String MIME_TYPE_DETECTION = "mime-type-detection";
    static final String MODULE = "module";
    static final String NAME = "name";
    static final String PATH = "path";
    static final String TRASH = "trash";
    static final String PATH_EXPRESSION = "path-expression";
    static final String PATH_EXPRESSIONS = "path-expressions";
    static final String PROPERTY = "property";
    static final String PROPERTIES = "properties";
    static final String PREDEFINED_WORKSPACE_NAME = "predefined-workspace-name";
    static final String PREDEFINED_WORKSPACE_NAMES = "predefined-workspace-names";
    static final String RELATIVE_TO = "relative-to";
    static final String SECURITY_DOMAIN = "security-domain";
    static final String SEQUENCER_CLASSNAME = "classname";
    static final String TEXT_EXTRACTOR_CLASSNAME = "classname";
    static final String SOURCE_PATH = "source-path";
    static final String SOURCE_RELATIVE_TO = "source-relative-to";
    static final String STORE_NAME = "store-name";
    static final String NESTED_STORES = "nested-stores";
    static final String USE_ANONYMOUS_IF_AUTH_FAILED = "use-anonymous-upon-failed-authentication";
    static final String SEQUENCERS_THREAD_POOL_NAME = "sequencers-thread-pool-name";
    static final String SEQUENCERS_MAX_POOL_SIZE = "sequencers-max-pool-size";
    static final String TEXT_EXTRACTORS_THREAD_POOL_NAME = "text-extractors-thread-pool-name";
    static final String TEXT_EXTRACTORS_MAX_POOL_SIZE = "text-extractors-max-pool-size";

    static final String AUTHENTICATOR = "authenticator";
    static final String AUTHENTICATOR_CLASSNAME = "classname";

    static final String BINARY_STORAGE = "binary-storage";
    static final String TRANSIENT_BINARY_STORAGE = "transient-binary-storage";
    static final String FILE_BINARY_STORAGE = "file-binary-storage";
    static final String DB_BINARY_STORAGE = "db-binary-storage";
    static final String CASSANDRA_BINARY_STORAGE = "cassandra-binary-storage";
    static final String MONGO_BINARY_STORAGE = "mongo-binary-storage";
    static final String S3_BINARY_STORAGE = "s3-binary-storage";
    static final String COMPOSITE_BINARY_STORAGE = "composite-binary-storage";
    static final String CUSTOM_BINARY_STORAGE = "custom-binary-storage";

    static final String SEQUENCER = "sequencer";
    static final String SEQUENCERS = "sequencers";
    static final String SOURCE = "source";
    static final String EXTRACTORS = "text-extractors";
    static final String TEXT_EXTRACTOR = "text-extractor";

    static final String INDEX = "index";
    static final String INDEX_KIND = "index-kind";
    static final String SYNCHRONOUS = "synchronous";
    static final String PROVIDER_NAME = "provider-name";
    static final String NODE_TYPE_NAME = "node-type-name";
    static final String INDEX_COLUMNS = "index-columns";
    static final String WORKSPACES = "workspaces";

    static final String INDEX_PROVIDER = "index-provider";

    static final String CONFIGURATION = "configuration";

    static final String DEFAULT_INITIAL_CONTENT = "default-initial-content";
    static final String WORKSPACES_INITIAL_CONTENT = "workspaces-initial-content";
    static final String INITIAL_CONTENT = "initial-content";

    static final String NODE_TYPES = "node-types";
    static final String NODE_TYPE = "node-type";

    static final String PROJECTIONS = "projections";
    static final String PROJECTION = "projection";
    static final String CONNECTOR_CLASSNAME = "classname";
    static final String CACHEABLE = "cacheable";
    static final String QUERYABLE = "queryable";
    static final String READONLY = "readonly";
    static final String EXPOSE_AS_WORKSPACE = "exposeAsWorkspace";
    static final String EXPLODED = "exploded";

    static final String MAX_DAYS_TO_KEEP_RECORDS = "journal-max-days-to-keep-records";
    static final String ASYNC_WRITES = "journal-async-writes";
    static final String JOURNALING = "journaling";
    static final String JOURNAL_GC_THREAD_POOL = "journal-gc-thread-pool";
    static final String JOURNAL_GC_INITIAL_TIME = "journal-gc-initial-time";
    static final String JOURNAL_PATH = "journal-path";
    static final String JOURNAL_ENABLED = "journal-enabled";
    static final String JOURNAL_RELATIVE_TO = "journal-relative-to";
    
    static final String REINDEXING_ASYNC = "reindexing-async";
    static final String REINDEXING_MODE = "reindexing-mode";
}

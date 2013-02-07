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

/**
 * Constants used in the ModeShape subsystem model.
 */
public class ModelKeys {

    static final String REPOSITORY = "repository";

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
    static final String BINARY_STORAGE_TYPE = "binary-storage-type";
    static final String STORAGE_TYPE = "storage-type";
    static final String CACHE_CONTAINER = "cache-container";
    static final String CACHE_NAME = "cache-name";
    static final String CLUSTER_NAME = "cluster-name";
    static final String CLUSTER_STACK = "cluster-stack";
    static final String CHUNK_SIZE = "chunk-size";
    static final String CLASSNAME = "classname";
    static final String CONNECTION_FACTORY_JNDI_NAME = "connection-factory-jndi-name";
    static final String COPY_BUFFER_SIZE = "copy-buffer-size";
    static final String DATA_CACHE_NAME = "data-cache-name";
    static final String DATA_SOURCE_JNDI_NAME = "data-source-jndi-name";
    static final String DEFAULT_WORKSPACE = "default-workspace";
    static final String ENABLE_MONITORING = "enable-monitoring";
    static final String INDEX_FORMAT = "index-format";
    static final String INDEX_STORAGE_TYPE = "index-storage-type";
    static final String JNDI_NAME = "jndi-name";
    static final String LOCK_CACHE_NAME = "lock-cache-name";
    static final String LOCKING_STRATEGY = "locking-strategy";
    static final String METADATA_CACHE_NAME = "metadata-cache-name";
    static final String MINIMUM_BINARY_SIZE = "minimum-binary-size";
    static final String MINIMUM_STRING_SIZE = "minimum-string-size";
    static final String MODE = "indexing-mode";
    /**
     * @deprecated use the REBUILD_INDEXES_UPON_STARTUP, REBUILD_INDEXES_UPON_INCLUDE_SYSTEM_CONTENT and REBUILD_INDEXES_UPON_STARTUP_MODE attributes
     */
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
    static final String REBUILD_INDEXES_UPON_STARTUP = "rebuild-indexes-upon-startup";
    static final String REBUILD_INDEXES_UPON_STARTUP_MODE = "rebuild-indexes-upon-startup-mode";
    static final String REBUILD_INDEXES_UPON_STARTUP_INCLUDE_SYSTEM_CONTENT = "rebuild-indexes-upon-startup-include-system-content";
    static final String REFRESH_PERIOD = "refresh-period";
    static final String RELATIVE_TO = "relative-to";
    static final String RETRY_INITIALIZE_PERIOD = "retry-initialize-period";
    static final String RETRY_MARKER_LOOKUP = "retry-marker-lookup";
    static final String SECURITY_DOMAIN = "security-domain";
    static final String SEQUENCER_CLASSNAME = "classname";
    static final String TEXT_EXTRACTOR_CLASSNAME = "classname";
    static final String SOURCE_PATH = "source-path";
    static final String SOURCE_RELATIVE_TO = "source-relative-to";
    static final String THREAD_POOL = "indexing-thread-pool";
    static final String USE_ANONYMOUS_IF_AUTH_FAILED = "use-anonymous-upon-failed-authentication";

    static final String AUTHENTICATOR = "authenticator";
    static final String AUTHENTICATOR_CLASSNAME = "classname";

    static final String BINARY_STORAGE = "binary-storage";
    static final String FILE_BINARY_STORAGE = "file-binary-storage";
    static final String DB_BINARY_STORAGE = "db-binary-storage";
    static final String CACHE_BINARY_STORAGE = "cache-binary-storage";
    static final String CUSTOM_BINARY_STORAGE = "custom-binary-storage";
    static final String REMOVE_BINARY_STORAGE = "remove-binary-storage";

    static final String SEQUENCER = "sequencer";
    static final String SOURCE = "source";
    static final String TEXT_EXTRACTOR = "text-extractor";

    static final String CONFIGURATION = "configuration";
    static final String INDEX_STORAGE = "index-storage";
    static final String RAM_INDEX_STORAGE = "ram-index-storage";
    static final String LOCAL_FILE_INDEX_STORAGE = "local-file-index-storage";
    static final String MASTER_FILE_INDEX_STORAGE = "master-file-index-storage";
    static final String SLAVE_FILE_INDEX_STORAGE = "slave-file-index-storage";
    static final String CACHE_INDEX_STORAGE = "cache-index-storage";
    static final String CUSTOM_INDEX_STORAGE = "custom-index-storage";
    static final String ADD_RAM_INDEX_STORAGE = "add-ram-index-storage";
    static final String ADD_LOCAL_FILE_INDEX_STORAGE = "add-local-file-index-storage";
    static final String ADD_MASTER_FILE_INDEX_STORAGE = "add-master-file-index-storage";
    static final String ADD_SLAVE_FILE_INDEX_STORAGE = "add-slave-file-index-storage";
    static final String ADD_CACHE_INDEX_STORAGE = "add-cache-index-storage";
    static final String ADD_CUSTOM_INDEX_STORAGE = "add-custom-index-storage";
    static final String REMOVE_INDEX_STORAGE = "remove-index-storage";

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
}

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
package org.modeshape.jcr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.util.Version;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.SchemaLibrary.Problem;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.ParsingException;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.infinispan.util.FileLookup;
import org.infinispan.util.FileLookupFactory;
import org.infinispan.util.ReflectionUtil;
import org.infinispan.util.Util;
import org.jgroups.Channel;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.clustering.DefaultChannelProvider;
import org.modeshape.jcr.security.AnonymousProvider;
import org.modeshape.jcr.security.JaasProvider;
import org.modeshape.jcr.value.binary.AbstractBinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.DatabaseBinaryStore;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;
import org.modeshape.jcr.value.binary.TransientBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.modeshape.sequencer.cnd.CndSequencer;

/**
 * A representation of the configuration for a {@link JcrRepository JCR Repository}.
 * <p>
 * Each repository configuration is loaded from a JSON document. A {@link #validate() valid} repository configuration requires
 * that the JSON document validates using the ModeShape repository configuration JSON Schema.
 * </p>
 * <p>
 * Variables may appear anywhere within the document's string field values. If a variable is to be used within a non-string field,
 * simply use a string field within the JSON document. When a RepositoryConfiguration instance is created from a JSON document,
 * these variables will be replaced with the System properties of the same name, and any resulting fields that are expected to be
 * non-string values will be converted into the expected field type. As expected, use {@link #validate()} to ensure the
 * configuration is valid.
 * </p>
 * <p>
 * Variables take the form:
 * 
 * <pre>
 *    variable := '${' variableNames [ ':' defaultValue ] '}'
 * 
 *    variableNames := variableName [ ',' variableNames ]
 * 
 *    variableName := /* any characters except ',' and ':' and '}'
 * 
 *    defaultValue := /* any characters except
 * </pre>
 * 
 * Note that <i>variableName</i> is the name used to look up a System property via {@link System#getProperty(String)}.
 * </p>
 * Notice that the syntax supports multiple <i>variables</i>. The logic will process the <i>variables</i> from let to right, until
 * an existing System property is found. And at that point, it will stop and will not attempt to find values for the other
 * <i>variables</i>.
 * <p>
 */
@Immutable
public class RepositoryConfiguration {

    /**
     * The standard identifier of the root node is '{@value} '.
     */
    public static final String ROOT_NODE_ID = "/";

    /**
     * The name of the 'system' workspace.
     */
    public static final String SYSTEM_WORKSPACE_NAME = "system";

    /**
     * The default JNDI location for repositories is "java:jcr/local/&lt;name>", where "&lt;name>" is the name of the repository.
     */
    public static final String DEFAULT_JNDI_PREFIX_OF_NAME = "java:jcr/local/";

    /**
     * The regexp pattern used to parse & validate projection path expressions. Expects [workspaceName]:/[projectedPath] =>
     * [externalPath] expressions. The expression is:
     * 
     * <pre>
     * (\w+):((([/]([^/=]|(\\.))+)+)|[/])\s*=>\s*((([/]([^/]|(\\.))+)+)|[/])
     * </pre>
     * 
     * where:
     * <ul>
     * <li>Group 1 is the workspace name</li>
     * <li>Group 2 is the path of the existing node</li>
     * <li>Group 7 is the path in the external source</li>
     * </ul>
     */
    private final static String PROJECTION_PATH_EXPRESSION_STRING = "(\\w+):((([/]([^/=]|(\\\\.))+)+)|[/])\\s*=>\\s*((([/]([^/]|(\\\\.))+)+)|[/])";
    public final static Pattern PROJECTION_PATH_EXPRESSION_PATTERN = Pattern.compile(PROJECTION_PATH_EXPRESSION_STRING);

    final static TimeUnit GARBAGE_COLLECTION_SWEEP_PERIOD_UNIT = TimeUnit.MINUTES;

    /**
     * The process of garbage collecting locks and binary values runs periodically, and this value controls how often it runs. The
     * value is currently set to 5 minutes.
     */
    final static int GARBAGE_COLLECTION_SWEEP_PERIOD = (int)TimeUnit.MILLISECONDS.convert(5, GARBAGE_COLLECTION_SWEEP_PERIOD_UNIT);

    /**
     * Each time the garbage collection process runs, session-scoped locks that are still used by active sessions will have their
     * expiry times extended by this amount of time. Each repository instance in the ModeShape cluster will run its own cleanup
     * process, which will extend the expiry times of its own locks. As soon as a repository is no longer running the cleanup
     * process, we know that there can be no active sessions.
     * <p>
     * The extension interval is generally twice the length of the period that the garbage collection runs, ensuring that any
     * slight deviation in the period does not cause locks to be expired prematurely.
     * </p>
     */
    final static int LOCK_EXTENSION_INTERVAL_IN_MILLIS = (int)TimeUnit.MILLISECONDS.convert(GARBAGE_COLLECTION_SWEEP_PERIOD * 2,
                                                                                            GARBAGE_COLLECTION_SWEEP_PERIOD_UNIT);

    /**
     * The amount of time that a lock may be expired before being removed. The sweep process will extend the locks for active
     * sessions, so only unused locks will have an unmodified expiry time. The value is currently twice the sweep period.
     */
    final static int LOCK_EXPIRY_AGE_IN_MILLIS = (int)TimeUnit.MILLISECONDS.convert(GARBAGE_COLLECTION_SWEEP_PERIOD * 2,
                                                                                    GARBAGE_COLLECTION_SWEEP_PERIOD_UNIT);

    /**
     * As binary values are no longer used, they are quarantined in the binary store. When the garbage collection process runs,
     * any binary values that have been quarantined longer than this duration will be removed.
     * <p>
     * The age is generally twice the length of the period that the garbage collection process runs, ensuring that any slight
     * deviation in the period does not cause binary values to be removed prematurely.
     * </p>
     */
    final static int UNUSED_BINARY_VALUE_AGE_IN_MILLIS = (int)TimeUnit.MILLISECONDS.convert(GARBAGE_COLLECTION_SWEEP_PERIOD * 2,
                                                                                            GARBAGE_COLLECTION_SWEEP_PERIOD_UNIT);

    protected static final Document EMPTY = Schematic.newDocument();

    protected static final Map<String, String> PROVIDER_ALIASES;
    protected static final Map<String, String> SEQUENCER_ALIASES;
    protected static final Map<String, String> EXTRACTOR_ALIASES;
    protected static final Map<String, String> CONNECTOR_ALIASES;
    protected static SchemaLibrary SCHEMA_LIBRARY;

    public static final String JSON_SCHEMA_URI = "http://modeshape.org/3.0/repository-config#";
    public static final String JSON_SCHEMA_RESOURCE_PATH = "org/modeshape/jcr/repository-config-schema.json";

    private static final Logger LOGGER = Logger.getLogger(RepositoryConfiguration.class);

    public static class FieldName {
        /**
         * The name for the field specifying the repository's name.
         */
        public static final String NAME = "name";
        /**
         * The name for the field specifying a description.
         */
        public static final String DESCRIPTION = "description";

        /**
         * The name for the optional field specifying where in JNDI this repository should be registered.
         */
        public static final String JNDI_NAME = "jndiName";

        /**
         * The specification of whether the repository should expect and detect whether JCR clients modify the content within
         * transactions. The default value of 'auto' will automatically detect the use of both user- and container-managed
         * transactions and also works when the JCR client does not use transactions; this will work in most situations. The value
         * of 'none' specifies that the repository should not attempt to detect existing transactions; this setting is an
         * optimization that should be used *only* if JCR clients will never use transactions to change the repository content.
         */
        public static final String TRANSACTION_MODE = "transactionMode";

        /**
         * The name for the field whose value is a document containing the monitoring information.
         */
        public static final String MONITORING = "monitoring";

        /**
         * The name for the optional field specifying whether the monitoring system is enabled or disabled.
         */
        public static final String MONITORING_ENABLED = "enabled";

        /**
         * The name for the field whose value is a document containing the Infinispan storage information.
         */
        public static final String STORAGE = "storage";

        /**
         * The name for the field containing the name of the Infinispan cache that this repository should use. If not specified,
         * the repository's name is used as the Infinispan cache name.
         */
        public static final String CACHE_NAME = "cacheName";

        /**
         * The name for the field containing the name of the Infinispan configuration file. If a file could not be found (on the
         * thread context classloader, on the application's classpath, or on the system classpath), then the name is used to look
         * in JNDI for an Infinispan CacheContainer instance. If no such container is found, then a default Infinispan
         * configuration (a basic, local mode, non-clustered cache) will be used.
         */
        public static final String CACHE_CONFIGURATION = "cacheConfiguration";

        /**
         * The name for the field containing the name of the Infinispan transaction manager lookup class. This is only used if no
         * {@link #CACHE_CONFIGURATION cacheConfiguration} value is specified and ModeShape needs to instantiate the Infinispan
         * {@link CacheContainer}. By default, the {@link GenericTransactionManagerLookup} class is used.
         */
        public static final String CACHE_TRANSACTION_MANAGER_LOOKUP = "transactionManagerLookup";

        /**
         * The size threshold that dictates whether binary values should be stored in the binary store. Binary values smaller than
         * this value are stored with the node, whereas binary values with a size equal to or greater than this limit will be
         * stored separately from the node and in the binary store, keyed by the SHA-1 hash of the value. This is a space and
         * performance optimization that stores each unique large value only once. The default value is '4096' bytes, or 4
         * kilobytes.
         */
        public static final String MINIMUM_BINARY_SIZE_IN_BYTES = "minimumBinarySizeInBytes";

        /**
         * The size threshold that dictates whether String should be stored in the binary store. String value shorter than this
         * value are stored with the node, whereas string values with a length equal to or greater than this limit will be stored
         * separately from the node and in the binary store, keyed by the SHA-1 hash of the value. This is a space and performance
         * optimization that stores each unique large value only once. By default, the {@link #MINIMUM_BINARY_SIZE_IN_BYTES} value
         * will be used.
         */
        public static final String MINIMUM_STRING_LENGTH = "minimumStringSizeInBytes";

        /**
         * The name for the field whose value is a document containing workspace information.
         */
        public static final String WORKSPACES = "workspaces";

        /**
         * The name for the field under "workspaces" specifying the array of names for the predefined (existing) workspaces.
         */
        public static final String PREDEFINED = "predefined";

        /**
         * The name for the field under "workspaces" specifying whether users can create additional workspaces beyond the
         * predefined, system, and default workspaces.
         */
        public static final String ALLOW_CREATION = "allowCreation";

        /**
         * The name of the field under which initial content can be specified for workspaces
         */
        public static final String INITIAL_CONTENT = "initialContent";

        /**
         * The name of the field using which initial cnd files can be specified
         */
        public static final String NODE_TYPES = "node-types";

        /**
         * The default value which symbolizes "all" the workspaces, meaning the initial content should be imported for each of the
         * new workspaces.
         */
        public static final String DEFAULT_INITIAL_CONTENT = "*";

        /**
         * The name for the field under "workspaces" specifying the name of the workspace that should be used by default when
         * creating sessions where the workspace is not specified.
         */
        public static final String DEFAULT = "default";

        /**
         * The name for the field containing the name of the file defining the Infinispan configuration for the repository's
         * workspace caches. If a file could not be found (on the thread context classloader, on the application's classpath, or
         * on the system classpath), then the name is used to look in JNDI for an Infinispan CacheContainer instance. If no such
         * container is found, then a value of "org/modeshape/jcr/deafult-workspace-cache-config.xml" is used, which is the
         * default configuration provided by ModeShape.
         */
        public static final String WORKSPACE_CACHE_CONFIGURATION = "cacheConfiguration";

        /**
         * The name for the field whose value is a document containing binary storage information.
         */
        public static final String BINARY_STORAGE = "binaryStorage";

        /**
         * The name for the field whose value is a document containing security information.
         */
        public static final String SECURITY = "security";

        /**
         * The name for the field under "security" specifying the optional JAAS configuration.
         */
        public static final String JAAS = "jaas";

        /**
         * The name for the field under "security/jaas" specifying the JAAS policy that should be used. An empty string value
         * implies that JAAS should not be used.
         */
        public static final String JAAS_POLICY_NAME = "policyName";

        /**
         * The name for the field under "security" specifying the optional anonymous security configuration.
         */
        public static final String ANONYMOUS = "anonymous";

        /**
         * The name for the field under "security/anonymous" specifying the roles that should be granted to anonymous users. By
         * default, anonymous users are granted the "admin" role, but this can be completely disabled by providing an empty array.
         */
        public static final String ANONYMOUS_ROLES = "roles";

        /**
         * The name for the field under "security/anonymous" specifying the username that should be used for anonymous users. The
         * default is "&lt;anonymous>";
         */
        public static final String ANONYMOUS_USERNAME = "username";

        /**
         * The name for the field under "security" specifying the username.
         */
        public static final String USER_NAME = "username";

        /**
         * The name for the field under "security" specifying the user's password.
         */
        public static final String USER_PASSWORD = "password";

        /**
         * The name for the field under "security/anonymous" specifying whether clients that fail authentication should instead be
         * granted anonymous credentials.
         */
        public static final String USE_ANONYMOUS_ON_FAILED_LOGINS = "useOnFailedLogin";

        public static final String PROVIDERS = "providers";
        public static final String TYPE = "type";
        public static final String DIRECTORY = "directory";
        public static final String CLASSLOADER = "classloader";
        public static final String CLASSNAME = "classname";
        public static final String DATA_SOURCE_JNDI_NAME = "dataSourceJndiName";
        public static final String DATA_CACHE_NAME = "dataCacheName";
        public static final String FULL_TEXT_SEARCH_ENABLED = "enableFullTextSearch";
        public static final String METADATA_CACHE_NAME = "metadataCacheName";
        public static final String QUERY = "query";
        public static final String QUERY_ENABLED = "enabled";
        public static final String INDEX_STORAGE = "indexStorage";
        public static final String INDEXING = "indexing";
        public static final String INDEXING_BACKEND = "backend";
        public static final String TABLES_INCLUDE_INHERITED_COLUMNS = "tablesIncludeInheritedColumns";
        public static final String TEXT_EXTRACTING = "textExtracting";
        public static final String EXTRACTORS = "extractors";
        public static final String SEQUENCING = "sequencing";
        public static final String SEQUENCERS = "sequencers";
        public static final String EXTERNAL_SOURCES = "externalSources";
        public static final String PROJECTIONS = "projections";
        public static final String PATH_EXPRESSION = "pathExpression";
        public static final String PATH_EXPRESSIONS = "pathExpressions";
        public static final String JDBC_DRIVER_CLASS = "driverClass";
        public static final String CONNECTION_URL = "url";
        /**
         * The name for the field (under "sequencing" and "query") specifying the thread pool that should be used for sequencing.
         * By default, all repository instances will use the same thread pool within the engine. To use a dedicated thread pool
         * for a single repository, simply use a name that is unique from all other repositories.
         */
        public static final String THREAD_POOL = "threadPool";
        public static final String REMOVE_DERIVED_CONTENT_WITH_ORIGINAL = "removeDerivedContentWithOriginal";

        public static final String INDEXING_ANALYZER = "analyzer";
        public static final String INDEXING_ANALYZER_CLASSPATH = "analyzerClasspath";
        public static final String INDEXING_SIMILARITY = "similarity";
        public static final String INDEXING_BATCH_SIZE = "batchSize";
        public static final String INDEXING_INDEX_FORMAT = "indexFormat";
        public static final String INDEXING_READER_STRATEGY = "readerStrategy";
        public static final String INDEXING_MODE = "mode";
        public static final String INDEXING_ASYNC_THREAD_POOL_SIZE = "asyncThreadPoolSize";
        public static final String INDEXING_ASYNC_MAX_QUEUE_SIZE = "asyncMaxQueueSize";

        public static final String INDEX_STORAGE_LOCATION = "location";
        public static final String INDEX_STORAGE_SOURCE_LOCATION = "sourceLocation";
        public static final String INDEX_STORAGE_LOCKING_STRATEGY = "lockingStrategy";
        public static final String INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE = "fileSystemAccessType";
        public static final String INDEX_STORAGE_REFRESH_IN_SECONDS = "refreshInSeconds";
        public static final String INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES = "copyBufferSizeInMegabytes";
        public static final String INDEX_STORAGE_RETRY_MARKER_LOOKUP = "retryMarkerLookup";
        public static final String INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS = "retryInitializePeriodInSeconds";
        public static final String INDEX_STORAGE_INFINISPAN_LOCK_CACHE = "lockCacheName";
        public static final String INDEX_STORAGE_INFINISPAN_DATA_CACHE = "dataCacheName";
        public static final String INDEX_STORAGE_INFINISPAN_META_CACHE = "metadataCacheName";
        public static final String INDEX_STORAGE_INFINISPAN_CONTAINER = "cacheConfiguration";
        public static final String INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES = "chunkSizeInBytes";

        public static final String INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME = "connectionFactoryJndiName";
        public static final String INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME = "queueJndiName";
        public static final String INDEXING_BACKEND_JGROUPS_CHANNEL_NAME = "channelName";
        public static final String INDEXING_BACKEND_JGROUPS_CHANNEL_CONFIGURATION = "channelConfiguration";

        /**
         * @deprecated use REBUILD_ON_STARTUP document
         */
        public static final String REBUILD_UPON_STARTUP = "rebuildUponStartup";

        /**
         * @deprecated use REBUILD_ON_STARTUP document
         */
        public static final String INDEXING_MODE_SYSTEM_CONTENT = "systemContentMode";

        public static final String REBUILD_ON_STARTUP = "rebuildOnStartup";
        public static final String REBUILD_WHEN = "when";
        public static final String REBUILD_INCLUDE_SYSTEM_CONTENT = "includeSystemContent";
        public static final String REBUILD_MODE = "mode";


        /**
         * The name of the clustering top-level configuration document
         */
        public static final String CLUSTERING = "clustering";

        /**
         * The name of the cluster as used by JChannel.connect
         */
        public static final String CLUSTER_NAME = "clusterName";

        /**
         * The fully qualified name of the {@link org.modeshape.jcr.clustering.ChannelProvider} implementation which will provide
         * the JChannel instance
         */
        public static final String CHANNEL_PROVIDER = "channelProvider";

        /**
         * The optional string representing a valid JGroups channel configuration object
         */
        public static final String CHANNEL_CONFIGURATION = "channelConfiguration";
    }

    public static class Default {
        /**
         * The default value of the {@link FieldName#MINIMUM_BINARY_SIZE_IN_BYTES} field is '{@value} ' (4 kilobytes).
         */
        public static final long MINIMUM_BINARY_SIZE_IN_BYTES = 4 * 1024L;

        /**
         * The default value of the {@link FieldName#ALLOW_CREATION} field is '{@value} '.
         */
        public static final boolean ALLOW_CREATION = true;

        /**
         * The default value of the {@link FieldName#DEFAULT} field is '{@value} '.
         */
        public static final String DEFAULT = "default";

        /**
         * The default value of the {@link FieldName#TRANSACTION_MODE} field is '{@value} '.
         */
        public static final TransactionMode TRANSACTION_MODE = TransactionMode.AUTO;

        /**
         * The default value of the {@link FieldName#CACHE_TRANSACTION_MANAGER_LOOKUP} field is
         * "org.infinispan.transaction.lookup.GenericTransactionManagerLookup".
         */
        public static final String CACHE_TRANSACTION_MANAGER_LOOKUP = GenericTransactionManagerLookup.class.getName();

        /**
         * The default value of the {@link FieldName#JAAS_POLICY_NAME} field is '{@value} '.
         */
        public static final String JAAS_POLICY_NAME = "modeshape-jcr";

        /**
         * The default value of the {@link FieldName#ANONYMOUS_ROLES} field is a list with 'admin' as the role.
         */
        public static final Set<String> ANONYMOUS_ROLES = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                          Arrays.asList(new String[] {ModeShapeRoles.ADMIN})));

        /**
         * The default value of the {@link FieldName#WORKSPACE_CACHE_CONFIGURATION} field is '{@value} '.
         */
        public static final String WORKSPACE_CACHE_CONFIGURATION = "org/modeshape/jcr/default-workspace-cache-config.xml";

        /**
         * The default value of the {@link FieldName#USE_ANONYMOUS_ON_FAILED_LOGINS} field is '{@value} '.
         */
        public static final boolean USE_ANONYMOUS_ON_FAILED_LOGINS = false;

        public static final String ANONYMOUS_USERNAME = "<anonymous>";

        public static final boolean QUERY_ENABLED = true;
        public static final boolean FULL_TEXT_SEARCH_ENABLED = true;

        public static final boolean MONITORING_ENABLED = true;

        public static final boolean REMOVE_DERIVED_CONTENT_WITH_ORIGINAL = true;

        public static final String SEQUENCING_POOL = "modeshape-sequencer";
        public static final String QUERY_THREAD_POOL = "modeshape-indexer";

        public static final String INDEXING_ANALYZER = StandardAnalyzer.class.getName();
        public static final String INDEXING_SIMILARITY = DefaultSimilarity.class.getName();
        public static final String INDEXING_BATCH_SIZE = "-1";
        @SuppressWarnings( "deprecation" )
        public static final String INDEXING_INDEX_FORMAT = Version.LUCENE_CURRENT.name();
        public static final IndexReaderStrategy INDEXING_READER_STRATEGY = IndexReaderStrategy.SHARED;
        public static final IndexingMode INDEXING_MODE = IndexingMode.SYNC;
        public static final IndexingMode INDEXING_MODE_SYSTEM_CONTENT = IndexingMode.DISABLED;
        public static final String INDEXING_ASYNC_THREAD_POOL_SIZE = "1";
        public static final String INDEXING_ASYNC_MAX_QUEUE_SIZE = "1";

        public static final FileSystemLockingStrategy INDEX_STORAGE_LOCKING_STRATEGY = FileSystemLockingStrategy.NATIVE;
        public static final FileSystemAccessType INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE = FileSystemAccessType.AUTO;
        public static final String INDEX_STORAGE_REFRESH_IN_SECONDS = "3600";
        public static final String INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES = "16";
        public static final String INDEX_STORAGE_RETRY_MARKER_LOOKUP = "0";
        public static final String INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS = "0";
        public static final String INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES = "16834";

        public static final String INDEXING_BACKEND_TYPE = "lucene";

        public static final String CLUSTER_NAME = "ModeShape-JCR";
        public static final String CHANNEL_PROVIDER = DefaultChannelProvider.class.getName();
    }

    public static final class FieldValue {
        public static final String INDEX_STORAGE_RAM = "ram";
        public static final String INDEX_STORAGE_FILESYSTEM = "filesystem";
        public static final String INDEX_STORAGE_FILESYSTEM_MASTER = "filesystem-master";
        public static final String INDEX_STORAGE_FILESYSTEM_SLAVE = "filesystem-slave";
        public static final String INDEX_STORAGE_INFINISPAN = "infinispan";
        public static final String INDEX_STORAGE_CUSTOM = "custom";

        public static final String INDEXING_BACKEND_TYPE_LUCENE = "lucene";
        public static final String INDEXING_BACKEND_TYPE_JMS_MASTER = "jms-master";
        public static final String INDEXING_BACKEND_TYPE_JMS_SLAVE = "jms-slave";
        public static final String INDEXING_BACKEND_TYPE_JGROUPS_MASTER = "jgroups-master";
        public static final String INDEXING_BACKEND_TYPE_JGROUPS_SLAVE = "jgroups-slave";
        public static final String INDEXING_BACKEND_TYPE_BLACKHOLE = "blackhole";
        public static final String INDEXING_BACKEND_TYPE_CUSTOM = "custom";

        public static final String BINARY_STORAGE_TYPE_FILE = "file";
        public static final String BINARY_STORAGE_TYPE_CACHE = "cache";
        public static final String BINARY_STORAGE_TYPE_DATABASE = "database";
        public static final String BINARY_STORAGE_TYPE_CUSTOM = "custom";

    }

    public enum IndexingMode {
        SYNC,
        ASYNC,
        DISABLED;
    }

    public enum IndexReaderStrategy {
        SHARED,
        NOT_SHARED;
    }

    public enum FileSystemLockingStrategy {
        SIMPLE,
        NATIVE,
        SINGLE,
        NONE;
    }

    public enum FileSystemAccessType {
        AUTO,
        SIMPLE,
        NIO,
        MMAP;
    }

    /**
     * The set of field names that should be skipped when {@link Component#createInstance(ClassLoader) instantiating a component}.
     */
    protected static final Set<String> COMPONENT_SKIP_PROPERTIES;

    /**
     * Flag which is used to determine whether clustering should be enabled or not
     */
    protected static final boolean JGROUPS_PRESENT = isJGroupsInClasspath();

    static {
        Set<String> skipProps = new HashSet<String>();
        skipProps.add(FieldName.CLASSLOADER);
        skipProps.add(FieldName.CLASSNAME);
        skipProps.add(FieldName.PROJECTIONS);
        COMPONENT_SKIP_PROPERTIES = Collections.unmodifiableSet(skipProps);

        String jaasProvider = "org.modeshape.jcr.security.JaasProvider";
        String servletProvider = "org.modeshape.jcr.security.ServletProvider";

        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("jaas", jaasProvider);
        aliases.put("jaasprovider", jaasProvider);
        aliases.put("servlet", servletProvider);
        aliases.put("servlets", servletProvider);
        aliases.put("servletprovider", servletProvider);
        PROVIDER_ALIASES = Collections.unmodifiableMap(aliases);

        String cndSequencer = CndSequencer.class.getName();
        String classfileSequencer = "org.modeshape.sequencer.classfile.ClassFileSequencer";
        String ddlSequencer = "org.modeshape.sequencer.ddl.DdlSequencer";
        String imageSequencer = "org.modeshape.sequencer.image.ImageMetadataSequencer";
        String javaSequencer = "org.modeshape.sequencer.javafile.JavaFileSequencer";
        String modelSequencer = "org.modeshape.sequencer.teiid.model.ModelSequencer";
        String vdbSequencer = "org.modeshape.sequencer.teiid.VdbSequencer";
        String msofficeSequencer = "org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer";
        String wsdlSequencer = "org.modeshape.sequencer.wsdl.WsdlSequencer";
        String xsdSequencer = "org.modeshape.sequencer.xsd.XsdSequencer";
        String xmlSequencer = "org.modeshape.sequencer.xml.XmlSequencer";
        String zipSequencer = "org.modeshape.sequencer.zip.ZipSequencer";
        String mp3Sequencer = "org.modeshape.sequencer.mp3.Mp3MetadataSequencer";
        String fixedWidthTextSequencer = "org.modeshape.sequencer.text.FixedWidthTextSequencer";
        String delimitedTextSequencer = "org.modeshape.sequencer.text.DelimitedTextSequencer";

        aliases = new HashMap<String, String>();
        aliases.put("cnd", cndSequencer);
        aliases.put("cndsequencer", cndSequencer);
        aliases.put("class", classfileSequencer);
        aliases.put("classfile", classfileSequencer);
        aliases.put("classsequencer", classfileSequencer);
        aliases.put("classfilesequencer", classfileSequencer);
        aliases.put("ddl", ddlSequencer);
        aliases.put("ddlsequencer", ddlSequencer);
        aliases.put("image", imageSequencer);
        aliases.put("imagesequencer", imageSequencer);
        aliases.put("java", javaSequencer);
        aliases.put("javasource", javaSequencer);
        aliases.put("javasequencer", javaSequencer);
        aliases.put("javasourcesequencer", javaSequencer);
        aliases.put("model", modelSequencer);
        aliases.put("modelsequencer", modelSequencer);
        aliases.put("vdb", vdbSequencer);
        aliases.put("vdbsequencer", vdbSequencer);
        aliases.put("msoffice", msofficeSequencer);
        aliases.put("msofficesequencer", msofficeSequencer);
        aliases.put("wsdl", wsdlSequencer);
        aliases.put("wsdlsequencer", wsdlSequencer);
        aliases.put("xsd", xsdSequencer);
        aliases.put("xsdsequencer", xsdSequencer);
        aliases.put("xml", xmlSequencer);
        aliases.put("xmlsequencer", xmlSequencer);
        aliases.put("zip", zipSequencer);
        aliases.put("zipsequencer", zipSequencer);
        aliases.put("mp3", mp3Sequencer);
        aliases.put("mp3sequencer", mp3Sequencer);
        aliases.put("fixedwidthtext", fixedWidthTextSequencer);
        aliases.put("fixedwidthtextsequencer", fixedWidthTextSequencer);
        aliases.put("delimitedtext", delimitedTextSequencer);
        aliases.put("delimitedtextsequencer", delimitedTextSequencer);

        SEQUENCER_ALIASES = Collections.unmodifiableMap(aliases);

        String fileSystemConnector = FileSystemConnector.class.getName();
        String gitConnector = "org.modeshape.connector.git.GitConnector";

        aliases = new HashMap<String, String>();
        aliases.put("files", fileSystemConnector);
        aliases.put("filesystem", fileSystemConnector);
        aliases.put("filesystemconnector", fileSystemConnector);
        aliases.put("git", gitConnector);
        aliases.put("gitconnector", gitConnector);

        CONNECTOR_ALIASES = Collections.unmodifiableMap(aliases);

        String tikaExtractor = "org.modeshape.extractor.tika.TikaTextExtractor";
        String vdbExtractor = "org.modeshape.extractor.teiid.TeiidVdbTextExtractor";

        aliases = new HashMap<String, String>();
        aliases.put("tika", tikaExtractor);
        aliases.put("tikaextractor", tikaExtractor);
        aliases.put("tikatextextractor", tikaExtractor);
        aliases.put("vdb", vdbExtractor);
        aliases.put("vdbextractor", vdbExtractor);
        aliases.put("vdbtextextractor", vdbExtractor);
        EXTRACTOR_ALIASES = Collections.unmodifiableMap(aliases);

        SCHEMA_LIBRARY = Schematic.createSchemaLibrary("ModeShape Repository Configuration Schemas");
        FileLookup factory = FileLookupFactory.newInstance();
        InputStream configStream = factory.lookupFile(JSON_SCHEMA_RESOURCE_PATH, RepositoryConfiguration.class.getClassLoader());
        if (configStream == null) {
            LOGGER.error(JcrI18n.unableToFindRepositoryConfigurationSchema, JSON_SCHEMA_RESOURCE_PATH);
        }
        try {
            Document configDoc = Json.read(configStream);
            SCHEMA_LIBRARY.put(JSON_SCHEMA_URI, configDoc);
        } catch (IOException e) {
            LOGGER.error(e, JcrI18n.unableToLoadRepositoryConfigurationSchema, JSON_SCHEMA_RESOURCE_PATH);
        }
    }

    /**
     * Utility method to replace all system property variables found within the specified document.
     * 
     * @param doc the document; may not be null
     * @return the modified document if system property variables were found, or the <code>doc</code> instance if no such
     *         variables were found
     */
    protected static Document replaceSystemPropertyVariables( Document doc ) {
        if (doc.isEmpty()) return doc;
        Document modified = doc.withVariablesReplacedWithSystemProperties();
        if (modified == doc) return doc;

        // Otherwise, we changed some values. Note that the system properties can only be used in
        // string values, whereas the schema may expect non-string values. Therefore, we need to validate
        // the document against the schema and possibly perform some conversions of values ...
        return SCHEMA_LIBRARY.convertValues(modified, JSON_SCHEMA_URI);
    }

    /**
     * Resolve the supplied URL to a JSON document, read the contents, and parse into a {@link RepositoryConfiguration}.
     * 
     * @param url the URL; may not be null
     * @return the parsed repository configuration; never null
     * @throws ParsingException if the content could not be parsed as a valid JSON document
     */
    public static RepositoryConfiguration read( URL url ) throws ParsingException {
        Document doc = Json.read(url);
        return new RepositoryConfiguration(doc, withoutExtension(url.getFile()));
    }

    /**
     * Read the supplied JSON file and parse into a {@link RepositoryConfiguration}.
     * 
     * @param file the file; may not be null
     * @return the parsed repository configuration; never null
     * @throws ParsingException if the content could not be parsed as a valid JSON document
     * @throws FileNotFoundException if the file could not be found
     */
    public static RepositoryConfiguration read( File file ) throws ParsingException, FileNotFoundException {
        Document doc = Json.read(new FileInputStream(file));
        return new RepositoryConfiguration(doc, withoutExtension(file.getName()));
    }

    /**
     * Read the supplied stream containing a JSON file, and parse into a {@link RepositoryConfiguration}.
     * 
     * @param stream the file; may not be null
     * @param name the name of the resource; may not be null
     * @return the parsed repository configuration; never null
     * @throws ParsingException if the content could not be parsed as a valid JSON document
     * @throws FileNotFoundException if the file could not be found
     */
    public static RepositoryConfiguration read( InputStream stream,
                                                String name ) throws ParsingException, FileNotFoundException {
        Document doc = Json.read(stream);
        return new RepositoryConfiguration(doc, withoutExtension(name));
    }

    /**
     * Read the repository configuration given by the supplied path to a file on the file system, the path a classpath resource
     * file, or a string containg the actual JSON content.
     * 
     * @param resourcePathOrJsonContentString the path to a file on the file system, the path to a classpath resource file or the
     *        JSON content string; may not be null
     * @return the parsed repository configuration; never null
     * @throws ParsingException if the content could not be parsed as a valid JSON document
     * @throws FileNotFoundException if the file could not be found
     */
    public static RepositoryConfiguration read( String resourcePathOrJsonContentString )
        throws ParsingException, FileNotFoundException {
        FileLookup factory = FileLookupFactory.newInstance();
        InputStream stream = factory.lookupFile(resourcePathOrJsonContentString, Thread.currentThread().getContextClassLoader());
        if (stream == null) {
            stream = factory.lookupFile(resourcePathOrJsonContentString, RepositoryConfiguration.class.getClassLoader());
        }
        if (stream != null) {
            Document doc = Json.read(stream);
            return new RepositoryConfiguration(doc, withoutExtension(resourcePathOrJsonContentString));
        }
        // Try a file ...
        File file = new File(resourcePathOrJsonContentString);
        if (file.exists() && file.isFile()) {
            return read(file);
        }
        String content = resourcePathOrJsonContentString.trim();
        if (content.startsWith("{")) {
            // Try to parse the document ...
            Document doc = Json.read(content);
            return new RepositoryConfiguration(doc, null);
        }
        throw new FileNotFoundException(resourcePathOrJsonContentString);
    }

    private static String withoutExtension( String name ) {
        int index = name.lastIndexOf('.');
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }

    private static boolean isEmpty( String str ) {
        return str == null || str.trim().length() == 0;
    }

    private static Document ensureNamed( Document document,
                                         String documentName ) {
        String name = document.getString(FieldName.NAME);
        if (isEmpty(name) && documentName != null && documentName.trim().length() != 0) {
            EditableDocument doc = Schematic.newDocument(document);
            doc.setString(FieldName.NAME, documentName);
            document = doc;
        }
        return document;
    }

    private static boolean isJGroupsInClasspath() {
        List<String> requiredJGroupsClasses = Arrays.asList("org.jgroups.JChannel",
                                                            "org.jgroups.ReceiverAdapter",
                                                            "org.jgroups.ChannelListener");
        try {
            ClassLoader classLoader = RepositoryConfiguration.class.getClassLoader();

            for (String jGroupsClass : requiredJGroupsClasses) {
                Class.forName(jGroupsClass, false, classLoader);
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private final String docName;
    private final Document doc;
    private transient Environment environment = new LocalEnvironment();
    private volatile Problems problems = null;

    public RepositoryConfiguration() {
        this(Schematic.newDocument(), null);
    }

    public RepositoryConfiguration( String name ) {
        this(Schematic.newDocument(), name);
    }

    public RepositoryConfiguration( Document document,
                                    String documentName ) {
        Document replaced = replaceSystemPropertyVariables(document);
        this.doc = ensureNamed(replaced, documentName);
        this.docName = documentName;
    }

    public RepositoryConfiguration( String name,
                                    Environment environment ) {
        this(Schematic.newDocument(), name != null ? name : Default.DEFAULT);
        this.environment = environment;
    }

    public RepositoryConfiguration( Document document,
                                    String documentName,
                                    Environment environment ) {
        Document replaced = replaceSystemPropertyVariables(document);
        this.doc = ensureNamed(replaced, documentName);
        this.docName = documentName;
        this.environment = environment;
    }

    protected Environment environment() {
        return this.environment;
    }

    public String getName() {
        return doc.getString(FieldName.NAME, docName);
    }

    public Document getDocument() {
        return doc;
    }

    public String getJndiName() {
        return doc.getString(FieldName.JNDI_NAME, DEFAULT_JNDI_PREFIX_OF_NAME + getName());
    }

    public String getStoreName() {
        return getCacheName();
    }

    public String getCacheName() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage != null) {
            return storage.getString(FieldName.CACHE_NAME, getName());
        }
        return getName();
    }

    public String getCacheConfiguration() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage != null) {
            return storage.getString(FieldName.CACHE_CONFIGURATION);
        }
        return null;
    }

    public String getCacheTransactionManagerLookupClassName() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage != null) {
            return storage.getString(FieldName.CACHE_TRANSACTION_MANAGER_LOOKUP, Default.CACHE_TRANSACTION_MANAGER_LOOKUP);
        }
        return Default.CACHE_TRANSACTION_MANAGER_LOOKUP;
    }

    public String getWorkspaceCacheConfiguration() {
        Document storage = doc.getDocument(FieldName.WORKSPACES);
        if (storage != null) {
            return storage.getString(FieldName.WORKSPACE_CACHE_CONFIGURATION, Default.WORKSPACE_CACHE_CONFIGURATION);
        }
        return Default.WORKSPACE_CACHE_CONFIGURATION;
    }

    CacheContainer getContentCacheContainer() throws IOException, NamingException {
        return getCacheContainer(null);
    }

    CacheContainer getWorkspaceContentCacheContainer() throws IOException, NamingException {
        String config = getWorkspaceCacheConfiguration();
        return getCacheContainer(config);
    }

    protected CacheContainer getCacheContainer( String config ) throws IOException, NamingException {
        if (config == null) config = getCacheConfiguration();
        return environment.getCacheContainer(config);
    }

    @SuppressWarnings( "unchecked" )
    protected Class<? extends TransactionManagerLookup> getCacheTransactionManagerLookupClass() {
        String txnMgrLookupClassName = getCacheTransactionManagerLookupClassName();
        try {
            return (Class<TransactionManagerLookup>)getClass().getClassLoader().loadClass(txnMgrLookupClassName);
        } catch (ClassNotFoundException e) {
            return GenericTransactionManagerLookup.class;
        }
    }

    public BinaryStorage getBinaryStorage() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage == null) {
            storage = Schematic.newDocument();
        }
        return new BinaryStorage(storage.getDocument(FieldName.BINARY_STORAGE));
    }

    public Clustering getClustering() {
        return new Clustering(doc.getDocument(FieldName.CLUSTERING));
    }

    /**
     * Returns the initial content configuration for this repository configuration
     * 
     * @return a {@code non-null} {@link InitialContent}
     */
    public InitialContent getInitialContent() {
        return new InitialContent(doc.getDocument(FieldName.WORKSPACES));
    }

    /**
     * Returns a list with the cnd files which should be loaded at startup.
     * 
     * @return a {@code non-null} string list
     */
    public List<String> getNodeTypes() {
        List<String> result = new ArrayList<String>();

        List<?> configuredNodeTypes = doc.getArray(FieldName.NODE_TYPES);
        if (configuredNodeTypes != null) {
            for (Object configuredNodeType : configuredNodeTypes) {
                result.add(configuredNodeType.toString());
            }
        }

        return result;
    }

    /**
     * Returns a fully qualified built-in sequencer class name mapped to the given alias, or {@code null} if there isn't such a
     * mapping
     * 
     * @param alias the alias
     * @return the name of the sequencer class, or null if the alias did not correspond to a built-in class
     */
    public static String getBuiltInSequencerClassName( String alias ) {
        return SEQUENCER_ALIASES.get(alias);
    }

    /**
     * Returns a fully qualified built-in text extractor class name mapped to the given alias, or {@code null} if there isn't such
     * a mapping
     * 
     * @param alias the alias
     * @return the name of the text extractor class, or null if the alias did not correspond to a built-in class
     */
    public static String getBuiltInTextExtractorClassName( String alias ) {
        return EXTRACTOR_ALIASES.get(alias);
    }

    /**
     * Returns a fully qualified built-in authentication provider class name mapped to the given alias, or {@code null} if there
     * isn't such a mapping
     * 
     * @param alias the alias
     * @return the name of the authentication provider class, or null if the alias did not correspond to a built-in class
     */
    public static String getBuiltInAuthenticationProviderClassName( String alias ) {
        return PROVIDER_ALIASES.get(alias);
    }

    @Immutable
    public class InitialContent {
        private final Map<String, String> workspacesInitialContentFiles;
        private String defaultInitialContentFile = "";

        public InitialContent( Document workspaces ) {
            workspacesInitialContentFiles = new HashMap<String, String>();
            if (workspaces != null) {
                Document initialContent = workspaces.getDocument(FieldName.INITIAL_CONTENT);
                if (initialContent != null) {
                    parseInitialContent(initialContent);
                }
            }
        }

        @SuppressWarnings( "synthetic-access" )
        private void parseInitialContent( Document initialContent ) {

            for (String workspaceName : initialContent.keySet()) {
                Object value = initialContent.get(workspaceName);
                if (value == null) value = "";
                if (!(value instanceof String)) {
                    LOGGER.warn(JcrI18n.invalidInitialContentValue, value.toString(), workspaceName);
                } else {
                    String initialContentFilePath = ((String)value).trim();
                    if (FieldName.DEFAULT_INITIAL_CONTENT.equals(workspaceName)) {
                        defaultInitialContentFile = initialContentFilePath;
                    } else {
                        workspacesInitialContentFiles.put(workspaceName, initialContentFilePath);
                    }
                }
            }
        }

        /**
         * Checks if there is an initial content file configured for the given workspace.
         * 
         * @param workspaceName a non-null {@link String} representing the name of a workspace
         * @return {@code true} if either there's an initial file configured specifically for the workspace or there's a default
         *         file which applies to all the workspaces.
         */
        public boolean hasInitialContentFile( String workspaceName ) {
            if (workspacesInitialContentFiles.containsKey(workspaceName)) {
                return !StringUtil.isBlank(workspacesInitialContentFiles.get(workspaceName));
            }
            return !StringUtil.isBlank(defaultInitialContentFile);
        }

        /**
         * Returns the initial content file configured for the workspace with the given name.
         * 
         * @param workspaceName a non-null {@link String} representing the name of a workspace
         * @return either a {@link String} representing the initial content file for the workspace, or an empty string indicating
         *         that explicitly no file has been configured for this workspace.
         */
        public String getInitialContentFile( String workspaceName ) {
            if (workspacesInitialContentFiles.containsKey(workspaceName)) {
                return workspacesInitialContentFiles.get(workspaceName);
            }
            return defaultInitialContentFile;
        }
    }

    /**
     * The binary-storage-related configuration information.
     */
    @Immutable
    public class BinaryStorage {
        private String classname;
        private String classPath;

        private final Document binaryStorage;
        private final Set<String> excludeList = new HashSet<String>();

        protected BinaryStorage( Document binaryStorage ) {
            this.binaryStorage = binaryStorage != null ? binaryStorage : EMPTY;
            excludeList.add(FieldName.TYPE);
            excludeList.add(FieldName.CLASSNAME);
            excludeList.add(FieldName.CLASSLOADER);
        }

        public long getMinimumBinarySizeInBytes() {
            return binaryStorage.getLong(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, Default.MINIMUM_BINARY_SIZE_IN_BYTES);
        }

        public long getMinimumStringSize() {
            return binaryStorage.getLong(FieldName.MINIMUM_STRING_LENGTH, getMinimumBinarySizeInBytes());
        }

        public AbstractBinaryStore getBinaryStore() throws Exception {
            String type = binaryStorage.getString(FieldName.TYPE, "transient");
            AbstractBinaryStore store = null;
            if (type.equalsIgnoreCase("transient")) {
                store = TransientBinaryStore.get();
            } else if (type.equalsIgnoreCase("file")) {
                String directory = binaryStorage.getString(FieldName.DIRECTORY);
                assert directory != null;
                File dir = new File(directory);
                store = FileSystemBinaryStore.create(dir);
            } else if (type.equalsIgnoreCase("database")) {
                String driverClass = binaryStorage.getString(FieldName.JDBC_DRIVER_CLASS);
                String connectionURL = binaryStorage.getString(FieldName.CONNECTION_URL);
                String username = binaryStorage.getString(FieldName.USER_NAME);
                String password = binaryStorage.getString(FieldName.USER_PASSWORD);
                String dataSourceJndi = binaryStorage.getString(FieldName.DATA_SOURCE_JNDI_NAME);
                if (StringUtil.isBlank(dataSourceJndi)) {
                    // Use the connection properties ...
                    store = new DatabaseBinaryStore(driverClass, connectionURL, username, password);
                } else {
                    // Use the DataSource in JNDI ...
                    store = new DatabaseBinaryStore(dataSourceJndi);
                }
            } else if (type.equalsIgnoreCase("cache")) {
                String metadataCacheName = binaryStorage.getString(FieldName.METADATA_CACHE_NAME, getName());
                String blobCacheName = binaryStorage.getString(FieldName.DATA_CACHE_NAME, getName());
                String cacheConfiguration = binaryStorage.getString(FieldName.CACHE_CONFIGURATION); // may be null
                boolean dedicatedCacheContainer = false;
                if (cacheConfiguration == null) {
                    cacheConfiguration = getCacheConfiguration();
                } else {
                    dedicatedCacheContainer = true;
                }
                CacheContainer cacheContainer = getCacheContainer(cacheConfiguration);

                // String cacheTransactionManagerLookupClass = binaryStorage.getString(FieldName.CACHE_TRANSACTION_MANAGER_LOOKUP,
                // Default.CACHE_TRANSACTION_MANAGER_LOOKUP);
                store = new InfinispanBinaryStore(cacheContainer, dedicatedCacheContainer, metadataCacheName, blobCacheName);
            } else if (type.equalsIgnoreCase("custom")) {
                classname = binaryStorage.getString(FieldName.CLASSNAME);
                classPath = binaryStorage.getString(FieldName.CLASSLOADER);

                // class name is mandatory
                if (StringUtil.isBlank(classname)) {
                    throw new BinaryStoreException(JcrI18n.missingVariableValue.text("classname"));
                }

                store = createInstance();
                setTypeFields(store, binaryStorage);
            }
            if (store == null) store = TransientBinaryStore.get();
            store.setMinimumBinarySizeInBytes(getMinimumBinarySizeInBytes());
            return store;
        }

        /*
         * Instantiates custom binary store.
         */
        private AbstractBinaryStore createInstance() throws Exception {
            ClassLoader classLoader = environment().getClassLoader(getClass().getClassLoader(), classPath);
            return (AbstractBinaryStore)classLoader.loadClass(classname).newInstance();
        }

        @SuppressWarnings( "synthetic-access" )
        private void setTypeFields( Object instance,
                                    Document document ) {
            for (Field field : document.fields()) {
                String fieldName = field.getName();
                Object fieldValue = field.getValue();
                if (excludeList.contains(fieldName)) {
                    continue;
                }
                try {
                    // locate the field instance on which the value will be set
                    java.lang.reflect.Field instanceField = findField(instance.getClass(), fieldName);
                    if (instanceField == null) {
                        LOGGER.warn(JcrI18n.missingFieldOnInstance, fieldName, classname);
                        continue;
                    }

                    Object convertedFieldValue = convertValueToType(instanceField.getType(), fieldValue);

                    // if the value is a document, means there is a nested bean
                    if (convertedFieldValue instanceof Document) {
                        // only no-arg constructors are supported
                        Object innerInstance = instanceField.getType().newInstance();
                        setTypeFields(innerInstance, (Document)convertedFieldValue);
                        convertedFieldValue = innerInstance;
                    }

                    // this is very ! tricky because it does not throw an exception - ever
                    ReflectionUtil.setValue(instance, fieldName, convertedFieldValue);
                } catch (Throwable e) {
                    LOGGER.error(e, JcrI18n.unableToSetFieldOnInstance, fieldName, fieldValue, classname);
                }
            }
        }

        /**
         * Attempts "its best" to convert a generic Object value (coming from a Document) to a value which can be set on the field
         * of a component. Note: thanks to type erasure, generics are not supported.
         * 
         * @param expectedType the {@link Class} of the field on which the value should be set
         * @param value a generic value coming from a document. Can be a simple value, another {@link Document} or {@link Array}
         * @return the converted value, which should be compatible with the expected type.
         * @throws Exception if anything will fail during the conversion process
         */
        private Object convertValueToType( Class<?> expectedType,
                                           Object value ) throws Exception {
            // lists are converted to ArrayList
            if (List.class.isAssignableFrom(expectedType)) {
                return valueToCollection(value, new ArrayList<Object>());
            }
            // sets are converted to HashSet
            if (Set.class.isAssignableFrom(expectedType)) {
                return valueToCollection(value, new HashSet<Object>());
            }
            // arrays are converted as-is
            if (expectedType.isArray()) {
                return valueToArray(expectedType.getComponentType(), value);
            }

            // maps are converted to hashmap
            if (Map.class.isAssignableFrom(expectedType)) {
                // only string keys are supported atm
                return ((Document)value).toMap();
            }

            // Strings can be parsed into numbers ...
            if (value instanceof String) {
                String strValue = (String)value;
                // Try the smallest ranges first ...
                if (Short.TYPE.isAssignableFrom(expectedType) || Short.class.isAssignableFrom(expectedType)) {
                    try {
                        return Short.parseShort(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Integer.TYPE.isAssignableFrom(expectedType) || Integer.class.isAssignableFrom(expectedType)) {
                    try {
                        return Integer.parseInt(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Long.TYPE.isAssignableFrom(expectedType) || Long.class.isAssignableFrom(expectedType)) {
                    try {
                        return Long.parseLong(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Boolean.TYPE.isAssignableFrom(expectedType) || Boolean.class.isAssignableFrom(expectedType)) {
                    try {
                        return Boolean.parseBoolean(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Float.TYPE.isAssignableFrom(expectedType) || Float.class.isAssignableFrom(expectedType)) {
                    try {
                        return Float.parseFloat(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Double.TYPE.isAssignableFrom(expectedType) || Double.class.isAssignableFrom(expectedType)) {
                    try {
                        return Double.parseDouble(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
            }

            // return value as it is
            return value;
        }

        private Object valueToArray( Class<?> arrayComponentType,
                                     Object value ) throws Exception {
            if (value instanceof Array) {
                Array valueArray = (Array)value;
                int arraySize = valueArray.size();
                Object newArray = java.lang.reflect.Array.newInstance(arrayComponentType, arraySize);
                for (int i = 0; i < ((Array)value).size(); i++) {
                    Object element = valueArray.get(i);
                    element = convertValueToType(arrayComponentType, element);
                    java.lang.reflect.Array.set(newArray, i, element);
                }
                return newArray;
            } else if (value instanceof String) {
                // Parse the string into a comma-separated set of values (this works if it's just a single value) ...
                String strValue = (String)value;
                if (strValue.length() > 0) {
                    String[] stringValues = strValue.split(",");
                    Object newArray = java.lang.reflect.Array.newInstance(arrayComponentType, stringValues.length);
                    for (int i = 0; i < stringValues.length; i++) {
                        Object element = convertValueToType(arrayComponentType, stringValues[i]);
                        java.lang.reflect.Array.set(newArray, i, element);
                    }
                    return newArray;
                }
            }

            // Otherwise, just initialize it to an empty array ...
            return java.lang.reflect.Array.newInstance(arrayComponentType, 0);
        }

        private Collection<?> valueToCollection( Object value,
                                                 Collection<Object> collection ) throws Exception {
            if (value instanceof Array) {
                collection.addAll((List<?>)value);
            } else {
                collection.add(value);
            }
            return collection;
        }

        public java.lang.reflect.Field findField( Class<?> typeClass,
                                                  String fieldName ) {
            java.lang.reflect.Field field = null;
            if (typeClass != null) {
                try {
                    field = typeClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    field = findField(typeClass.getSuperclass(), fieldName);
                }
            }
            return field;
        }
    }

    public boolean isCreatingWorkspacesAllowed() {
        Document workspaces = doc.getDocument(FieldName.WORKSPACES);
        if (workspaces != null) {
            return workspaces.getBoolean(FieldName.ALLOW_CREATION, Default.ALLOW_CREATION);
        }
        return Default.ALLOW_CREATION;
    }

    public TransactionMode getTransactionMode() {
        String mode = doc.getString(FieldName.TRANSACTION_MODE);
        return mode != null ? TransactionMode.valueOf(mode.trim().toUpperCase()) : Default.TRANSACTION_MODE;
    }

    /**
     * Get the name of the workspace that should be used for sessions where the client does not specify the name of the workspace.
     * 
     * @return the default workspace name; never null
     */
    public String getDefaultWorkspaceName() {
        Document workspaces = doc.getDocument(FieldName.WORKSPACES);
        if (workspaces != null) {
            return workspaces.getString(FieldName.DEFAULT, Default.DEFAULT);
        }
        return Default.DEFAULT;
    }

    /**
     * Obtain the names of the workspaces that were listed as being predefined. This includes the name
     * {@link #getDefaultWorkspaceName() default workspace}.
     * 
     * @return the set of predefined (non-system) workspace names; never null
     */
    public Set<String> getPredefinedWorkspaceNames() {
        Set<String> names = new HashSet<String>();
        Document workspaces = doc.getDocument(FieldName.WORKSPACES);
        if (workspaces != null) {
            List<?> predefined = workspaces.getArray(FieldName.PREDEFINED);
            if (predefined != null) {
                for (Object value : predefined) {
                    if (value instanceof String) names.add((String)value);
                }
            }
        }
        names.add(getDefaultWorkspaceName());
        return names;
    }

    /**
     * Obtain all of the workspace names specified by this repository, including the {@link #getPredefinedWorkspaceNames()
     * predefined workspaces} and the {@link #getDefaultWorkspaceName() default workspace}. The result does <i>not</i> contain the
     * names of any dynamically-created workspaces (e.g., those not specified in the configuration).
     * 
     * @return the set of all workspace names defined by the configuration; never null
     */
    public Set<String> getAllWorkspaceNames() {
        Set<String> names = getPredefinedWorkspaceNames();
        names.add(getDefaultWorkspaceName());
        return names;
    }

    /**
     * Get the configuration for the security-related aspects of this repository.
     * 
     * @return the security configuration; never null
     */
    public Security getSecurity() {
        return new Security(doc.getDocument(FieldName.SECURITY));
    }

    /**
     * The security-related configuration information.
     */
    @Immutable
    public class Security {
        private final Document security;

        protected Security( Document security ) {
            this.security = security != null ? security : EMPTY;
        }

        /**
         * Get the configuration information for the JAAS provider.
         * 
         * @return the JAAS provider configuration information; null if JAAS is not configured
         */
        public JaasSecurity getJaas() {
            if (isIncludedInCustomProviders(JaasProvider.class.getName())) {
                // It's in the custom provider, so don't expose it.
                // (this enables easily turning off JAAS without setting a blank policy name) ...
                return null;
            }
            Document jaas = security.getDocument(FieldName.JAAS);
            return jaas != null ? new JaasSecurity(jaas) : null;
        }

        /**
         * Get the configuration information for the anonymous authentication provider.
         * 
         * @return the anonymous provider configuration information; null if anonymous users are not allowed
         */
        public AnonymousSecurity getAnonymous() {
            Document anonymous = security.getDocument(FieldName.ANONYMOUS);
            if (anonymous != null && anonymous.size() == 1) {
                // Check the 'roleNames' field ...
                List<?> roles = anonymous.getArray(FieldName.ANONYMOUS_ROLES);
                if (roles != null && roles.isEmpty()) {
                    // Specified empty roles, so this is disabling anonymous logins ...
                    return null;
                }
            }
            if (anonymous == null) anonymous = Schematic.newDocument();
            return new AnonymousSecurity(anonymous);
        }

        /**
         * Get the ordered list of custom authentication providers. Note that the JAAS and anonymous provider specified via
         * {@link #getJaas()} and {@link #getAnonymous()} are not included in this list. However, should the JAAS and/or anonymous
         * providers be specified in this list (to change the ordering), the {@link #getJaas()} and/or {@link #getAnonymous()}
         * configuration components will be null.
         * 
         * @return the immutable list of custom providers; never null but possibly empty
         */
        public List<Component> getCustomProviders() {
            Problems problems = new SimpleProblems();
            List<Component> components = readComponents(security,
                                                        FieldName.PROVIDERS,
                                                        FieldName.CLASSNAME,
                                                        PROVIDER_ALIASES,
                                                        problems);
            assert !problems.hasErrors();
            return components;
        }

        protected void validateCustomProviders( Problems problems ) {
            readComponents(security, FieldName.PROVIDERS, FieldName.CLASSNAME, PROVIDER_ALIASES, problems);
        }

        private boolean isIncludedInCustomProviders( String classname ) {
            for (Component component : getCustomProviders()) {
                if (classname.equals(component.getClassname())) return true;
            }
            return false;
        }
    }

    /**
     * The configuration of the use of the built-in JAAS authentication and authorization provider. Note that this is <i>not</i>
     * used if the JAAS provider is specified in the '{@link FieldName#PROVIDERS providers}' field.
     */
    @Immutable
    public class JaasSecurity {
        private final Document jaas;

        protected JaasSecurity( Document jaas ) {
            assert jaas != null;
            this.jaas = jaas;
        }

        /**
         * Get the name of the JAAS policy.
         * 
         * @return the policy name; never null and '{@value Default#JAAS_POLICY_NAME}' by default.
         */
        public String getPolicyName() {
            String policy = jaas.getString(FieldName.JAAS_POLICY_NAME, Default.JAAS_POLICY_NAME);
            return policy != null && policy.trim().length() == 0 ? null : policy;
        }
    }

    /**
     * The configuration of the use of the built-in anonymous authentication and authorization provider. Note that this is
     * <i>not</i> used if the anonymous provider is specified in the '{@link FieldName#PROVIDERS providers}' field.
     */
    @Immutable
    public class AnonymousSecurity {
        private final Document anonymous;

        protected AnonymousSecurity( Document anonymous ) {
            assert anonymous != null;
            this.anonymous = anonymous;
        }

        /**
         * Get the name of the ModeShape authorization roles that each anonymous user should be assigned.
         * 
         * @return the set of role names; never null or empty, and '{@value Default#ANONYMOUS_ROLES}' by default.
         */
        public Set<String> getAnonymousRoles() {
            Set<String> names = new HashSet<String>();
            Collection<?> roles = anonymous.getArray(FieldName.ANONYMOUS_ROLES);
            if (roles == null) roles = Default.ANONYMOUS_ROLES;
            if (roles != null) {
                for (Object value : roles) {
                    if (value instanceof String) {
                        names.add(((String)value).trim().toLowerCase());
                    }
                }
            }
            return names;
        }

        /**
         * Get the username that each anonymous user should be assigned.
         * 
         * @return the anonymous username; never null and '{@value Default#ANONYMOUS_USERNAME}' by default.
         */
        public String getAnonymousUsername() {
            return anonymous.getString(FieldName.ANONYMOUS_USERNAME, Default.ANONYMOUS_USERNAME);
        }

        /**
         * Determine whether users that fail all other authentication should be automatically logged in as an anonymous user.
         * 
         * @return true if non-authenticated users should be given anonymous sessions, or false if authenication should fail; the
         *         default is '{@value Default#USE_ANONYMOUS_ON_FAILED_LOGINS}'.
         */
        public boolean useAnonymousOnFailedLogings() {
            return anonymous.getBoolean(FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS, Default.USE_ANONYMOUS_ON_FAILED_LOGINS);
        }
    }

    /**
     * Get the configuration for the monitoring-related aspects of this repository.
     * 
     * @return the monitoring configuration; never null
     */
    public MonitoringSystem getMonitoring() {
        return new MonitoringSystem(doc.getDocument(FieldName.MONITORING));
    }

    /**
     * The query-related configuration information.
     */
    @Immutable
    public class MonitoringSystem {
        private final Document monitoring;

        protected MonitoringSystem( Document monitoring ) {
            this.monitoring = monitoring != null ? monitoring : EMPTY;
        }

        /**
         * Determine whether monitoring is enabled. The default is to enable monitoring, but this can be used to turn off support
         * for monitoring should it not be necessary.
         * 
         * @return true if monitoring is enabled, or false if it is disabled
         */
        public boolean enabled() {
            return monitoring.getBoolean(FieldName.MONITORING_ENABLED, Default.MONITORING_ENABLED);
        }
    }

    /**
     * Possible options for rebuilding the indexes upon startup.
     */
    public enum QueryRebuild {
        ALWAYS,
        NEVER,
        IF_MISSING;
    }

    /**
     * Possible options for rebuilding the indexes upon startup.
     */
    public enum TransactionMode {
        AUTO,
        NONE;
    }

    /**
     * Get the configuration for the query-related aspects of this repository.
     * 
     * @return the query configuration; never null
     */
    public QuerySystem getQuery() {
        return new QuerySystem(doc.getDocument(FieldName.QUERY));
    }

    /**
     * The query-related configuration information.
     */
    @Immutable
    public class QuerySystem {
        private final Document query;

        protected QuerySystem( Document query ) {
            this.query = query != null ? query : EMPTY;
        }

        /**
         * Determine whether queries and searches are enabled. The default is to enable queries, but this can be used to turn off
         * support for queries and improve performance.
         * 
         * @return true if queries are enabled, or false if they are disabled
         * @see #fullTextSearchEnabled()
         */
        public boolean queriesEnabled() {
            return query.getBoolean(FieldName.QUERY_ENABLED, Default.QUERY_ENABLED);
        }

        /**
         * Get whether full-text searching is enabled for this repository. Note that full-text search requires that
         * {@link #queriesEnabled() queries are enabled}, so this method returns false if queries are disabled.
         * 
         * @return true if full-text searching is enabled, or false otherwise
         * @see #queriesEnabled()
         */
        public boolean fullTextSearchEnabled() {
            return queriesEnabled() && query.getBoolean(FieldName.FULL_TEXT_SEARCH_ENABLED, Default.FULL_TEXT_SEARCH_ENABLED);
        }

        /**
         * Get the name of the thread pool that should be used for indexing work.
         * 
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return query.getString(FieldName.THREAD_POOL, Default.QUERY_THREAD_POOL);
        }

        /**
         * Returns the options that should be used for rebuilding indexes when the repository starts up.
         *
         * @return a {@code non-null} {@link IndexRebuildOptions} instance.
         */
        public IndexRebuildOptions getIndexRebuildOptions() {
            return new IndexRebuildOptions(query);
        }

        /**
         * Get the configuration properties for the storage of indexes. The values of the properties will consist of string
         * values.
         * 
         * @return the configuration properties; never null
         */
        public Properties getIndexStorageProperties() {
            Properties props = new Properties();
            Document doc = query.getDocument(FieldName.INDEX_STORAGE);
            if (doc != null) {
                for (Field field : doc.fields()) {
                    String name = field.getName();
                    String value = field.getValue().toString(); // schema only allows strings and integers
                    props.setProperty(name, value);
                }
            }

            // Set the defaults ...
            setDefProp(props, FieldName.TYPE, FieldValue.INDEX_STORAGE_RAM);
            String type = props.getProperty(FieldName.TYPE);
            if (FieldValue.INDEX_STORAGE_FILESYSTEM.equalsIgnoreCase(type)) {
                setDefProp(props, FieldName.INDEX_STORAGE_LOCKING_STRATEGY, Default.INDEX_STORAGE_LOCKING_STRATEGY.toString()
                                                                                                                  .toLowerCase());
                setDefProp(props,
                           FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE,
                           Default.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE.toString().toLowerCase());
            } else if (FieldValue.INDEX_STORAGE_FILESYSTEM_MASTER.equalsIgnoreCase(type)) {
                setDefProp(props, FieldName.INDEX_STORAGE_LOCKING_STRATEGY, Default.INDEX_STORAGE_LOCKING_STRATEGY.toString()
                                                                                                                  .toLowerCase());
                setDefProp(props,
                           FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE,
                           Default.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE.toString().toLowerCase());
                setDefProp(props, FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS, Default.INDEX_STORAGE_REFRESH_IN_SECONDS);
            } else if (FieldValue.INDEX_STORAGE_FILESYSTEM_SLAVE.equalsIgnoreCase(type)) {
                setDefProp(props, FieldName.INDEX_STORAGE_LOCKING_STRATEGY, Default.INDEX_STORAGE_LOCKING_STRATEGY.toString()
                                                                                                                  .toLowerCase());
                setDefProp(props,
                           FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE,
                           Default.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE.toString().toLowerCase());
                setDefProp(props, FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS, Default.INDEX_STORAGE_REFRESH_IN_SECONDS);
                setDefProp(props,
                           FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES,
                           Default.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES);
                setDefProp(props, FieldName.INDEX_STORAGE_RETRY_MARKER_LOOKUP, Default.INDEX_STORAGE_RETRY_MARKER_LOOKUP);
                setDefProp(props,
                           FieldName.INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS,
                           Default.INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS);
            } else if (FieldValue.INDEX_STORAGE_INFINISPAN.equalsIgnoreCase(type)) {
                setDefProp(props,
                           FieldName.INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES,
                           Default.INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES);
                setDefProp(props, FieldName.CACHE_CONFIGURATION, getCacheConfiguration());
                // The cache names will be set when the Hibernate Search configuration is created; that way we don't have
                // the repository name hard-coded in the properties ...
            }
            return props;
        }

        /**
         * Get the configuration properties for the indexing section. The values of the properties will consist of string values,
         * and they may contain properties of the form "hibernate.search.*".
         * 
         * @return the configuration properties; never null
         */
        public Properties getIndexingProperties() {
            Properties props = new Properties();
            Document doc = query.getDocument(FieldName.INDEXING);
            if (doc != null) {
                for (Field field : doc.fields()) {
                    String name = field.getName();
                    if (FieldName.INDEXING_BACKEND.equals(name)) continue;
                    if (FieldName.THREAD_POOL.equals(name)) continue;
                    String value = field.getValue().toString(); // schema only allows strings and integers
                    props.setProperty(name, value);
                }
            }
            // Set the defaults ...
            setDefProp(props, FieldName.INDEXING_ANALYZER, Default.INDEXING_ANALYZER);
            setDefProp(props, FieldName.INDEXING_SIMILARITY, Default.INDEXING_SIMILARITY);
            setDefProp(props, FieldName.INDEXING_BATCH_SIZE, Default.INDEXING_BATCH_SIZE);
            setDefProp(props, FieldName.INDEXING_INDEX_FORMAT, Default.INDEXING_INDEX_FORMAT);
            setDefProp(props, FieldName.INDEXING_READER_STRATEGY, Default.INDEXING_READER_STRATEGY.toString().toLowerCase());
            setDefProp(props, FieldName.INDEXING_MODE, Default.INDEXING_MODE.toString().toLowerCase());
            setDefProp(props, FieldName.INDEXING_ASYNC_THREAD_POOL_SIZE, Default.INDEXING_ASYNC_THREAD_POOL_SIZE);
            setDefProp(props, FieldName.INDEXING_ASYNC_MAX_QUEUE_SIZE, Default.INDEXING_ASYNC_MAX_QUEUE_SIZE);
            return props;
        }

        /**
         * Get the configuration properties for the indexing backend. The values of the properties will consist of string values.
         * 
         * @return the configuration properties; never null
         */
        public Properties getIndexingBackendProperties() {
            Properties props = new Properties();
            Document doc = query.getDocument(FieldName.INDEXING);
            if (doc != null) {
                doc = doc.getDocument(FieldName.INDEXING_BACKEND);
                if (doc != null) {
                    for (Field field : doc.fields()) {
                        String name = field.getName();
                        String value = field.getValue().toString(); // schema only allows strings and integers
                        props.setProperty(name, value);
                    }
                }
            }
            // Set the defaults ...
            setDefProp(props, FieldName.TYPE, Default.INDEXING_BACKEND_TYPE);
            return props;
        }

        public TextExtracting getTextExtracting() {
            return new TextExtracting(query.getDocument(FieldName.TEXT_EXTRACTING));
        }

        protected void setDefProp( Properties props,
                                   String name,
                                   String defaultValue ) {
            if (!props.containsKey(name) && defaultValue != null) {
                props.setProperty(name, defaultValue);
            }
        }
    }

    @Immutable
    public class IndexRebuildOptions {

        private final QueryRebuild when;
        private final Boolean includeSystemContent;
        private final IndexingMode mode;

        private IndexRebuildOptions( Document query ) {
            assert query != null;

            //first parse the deprecated fields (we need to avoid breaking client compatibility)
            QueryRebuild deprecatedQueryRebuild = QueryRebuild.IF_MISSING;
            if (query.containsField(FieldName.REBUILD_UPON_STARTUP)) {
                deprecatedQueryRebuild = QueryRebuild.valueOf(query.getString(FieldName.REBUILD_UPON_STARTUP).toUpperCase());
            }

            Boolean deprecatedIncludeSystemContent = false;
            IndexingMode deprecatedIndexingMode = IndexingMode.SYNC;
            if (query.containsField(FieldName.INDEXING_MODE_SYSTEM_CONTENT)) {
                deprecatedIndexingMode = IndexingMode.valueOf(query.getString(FieldName.INDEXING_MODE_SYSTEM_CONTENT).toUpperCase());
                switch (deprecatedIndexingMode) {
                    case SYNC: {
                        deprecatedIncludeSystemContent = true;
                        break;
                    }
                    case ASYNC: {
                        deprecatedIncludeSystemContent = true;
                        break;
                    }
                    case DISABLED: {
                        //we don't support disabled in the new indexing mode, so fallback to the default
                        deprecatedIndexingMode = IndexingMode.SYNC;
                        break;
                    }
                }
            }

            //look for the new document structure
            Document rebuildOnStartupDocument = null;
            if (query.containsField(FieldName.INDEXING)) {
                Document indexingDocument = query.getDocument(FieldName.INDEXING);
                rebuildOnStartupDocument = indexingDocument.getDocument(FieldName.REBUILD_ON_STARTUP);
            }

            if (rebuildOnStartupDocument == null) {
                //there isn't the newer version of the rebuildOnStartupDocument present, so we need to use the old values
                this.when = deprecatedQueryRebuild;
                this.includeSystemContent = deprecatedIncludeSystemContent;
                this.mode = deprecatedIndexingMode;
            } else {
                String when = rebuildOnStartupDocument.getString(FieldName.REBUILD_WHEN, deprecatedQueryRebuild.name()).toUpperCase();
                this.when = QueryRebuild.valueOf(when);
                this.includeSystemContent = rebuildOnStartupDocument.getBoolean(FieldName.REBUILD_INCLUDE_SYSTEM_CONTENT, deprecatedIncludeSystemContent.booleanValue());
                String mode = rebuildOnStartupDocument.getString(FieldName.REBUILD_MODE, deprecatedIndexingMode.name()).toUpperCase();
                this.mode = IndexingMode.valueOf(mode);
            }
        }

        /**
         * Returns the value of the flag that controls whether system content should be reindexed or not at startup.
         *
         * @return {@code true} if system content should be reindexed at startup; {@code false} otherwise
         */
        public boolean includeSystemContent() {
            return includeSystemContent;
        }

        /**
         * Returns the mode in which indexes should be rebuilt.
         *
         * @return a {@code non-null} {@link IndexingMode} instance
         */
        public IndexingMode getMode() {
            return mode;
        }

        /**
         * Returns the strategy used for rebuilding indexes.
         *
         * @return a {@code non-null} {@link QueryRebuild} instance
         */
        public QueryRebuild getWhen() {
            return when;
        }
    }

    @Immutable
    public class TextExtracting {
        private final Document textExtracting;

        public TextExtracting( Document textExtracting ) {
            this.textExtracting = textExtracting != null ? textExtracting : EMPTY;
        }

        /**
         * Get the name of the thread pool that should be used for sequencing work.
         * 
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return textExtracting.getString(FieldName.THREAD_POOL, "modeshape-text-extractor");
        }

        /**
         * Get the ordered list of text extractors. All text extractors are configured with this list.
         * 
         * @return the immutable list of text extractors; never null but possibly empty
         */
        public List<Component> getTextExtractors() {
            Problems problems = new SimpleProblems();
            List<Component> components = readComponents(textExtracting,
                                                        FieldName.EXTRACTORS,
                                                        FieldName.CLASSNAME,
                                                        EXTRACTOR_ALIASES,
                                                        problems);
            assert !problems.hasErrors();
            return components;
        }

        protected void validateTextExtractors( Problems problems ) {
            readComponents(textExtracting, FieldName.EXTRACTORS, FieldName.CLASSNAME, EXTRACTOR_ALIASES, problems);
        }
    }

    /**
     * Get the configuration for the sequencing-related aspects of this repository.
     * 
     * @return the sequencing configuration; never null
     */
    public Sequencing getSequencing() {
        return new Sequencing(doc.getDocument(FieldName.SEQUENCING));
    }

    /**
     * Get the configuration for the sequencing-related aspects of this repository.
     * 
     * @return the sequencing configuration; never null
     */
    public Federation getFederation() {
        return new Federation(doc);
    }

    /**
     * The security-related configuration information.
     */
    @Immutable
    public class Sequencing {
        private final Document sequencing;

        protected Sequencing( Document sequencing ) {
            this.sequencing = sequencing != null ? sequencing : EMPTY;
        }

        /**
         * Determine whether the derived content originally produced by a sequencer upon sequencing some specific input should be
         * removed if that input is updated and the sequencer re-run.
         * 
         * @return true if the original derived content should be removed upon subsequent sequencing of the same input.
         */
        public boolean removeDerivedContentWithOriginal() {
            return sequencing.getBoolean(FieldName.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL,
                                         Default.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL);
        }

        /**
         * Get the name of the thread pool that should be used for sequencing work.
         * 
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return sequencing.getString(FieldName.THREAD_POOL, Default.SEQUENCING_POOL);
        }

        /**
         * Get the ordered list of sequencers. All sequencers are configured with this list.
         * 
         * @return the immutable list of sequencers; never null but possibly empty
         */
        public List<Component> getSequencers() {
            Problems problems = new SimpleProblems();
            List<Component> components = readComponents(sequencing,
                                                        FieldName.SEQUENCERS,
                                                        FieldName.CLASSNAME,
                                                        SEQUENCER_ALIASES,
                                                        problems);
            assert !problems.hasErrors();
            return components;
        }

        /**
         * Get the ordered list of sequencers. All sequencers are configured with this list.
         * 
         * @param problems the container for problems reading the sequencer information; may not be null
         */
        protected void validateSequencers( Problems problems ) {
            readComponents(sequencing, FieldName.SEQUENCERS, FieldName.CLASSNAME, SEQUENCER_ALIASES, problems);
        }
    }

    /**
     * The federation-related configuration information.
     */
    @Immutable
    public class Federation {
        private final Document federation;

        protected Federation( Document federation ) {
            this.federation = federation != null ? federation : EMPTY;
        }

        /**
         * Get the list of connector configurations.
         * 
         * @return the immutable list of connectors; never null but possibly empty
         */
        public List<Component> getConnectors() {
            Problems problems = new SimpleProblems();
            List<Component> components = readComponents(federation,
                                                        FieldName.EXTERNAL_SOURCES,
                                                        FieldName.CLASSNAME,
                                                        CONNECTOR_ALIASES,
                                                        problems);
            assert !problems.hasErrors();
            return components;
        }

        /**
         * Returns the [sourceName, list(projections)] configured for each source.
         * 
         * @return a {@link Map} instance, never null.
         */
        public Map<String, List<ProjectionConfiguration>> getProjections() {
            Map<String, List<ProjectionConfiguration>> result = new HashMap<String, List<ProjectionConfiguration>>();
            if (!federation.containsField(FieldName.EXTERNAL_SOURCES)) {
                return result;
            }
            Document externalSources = federation.getDocument(FieldName.EXTERNAL_SOURCES);
            for (String sourceName : externalSources.keySet()) {
                Document externalSource = externalSources.getDocument(sourceName);
                if (!externalSource.containsField(FieldName.PROJECTIONS)) {
                    continue;
                }
                List<ProjectionConfiguration> projectionConfigurations = new ArrayList<ProjectionConfiguration>();
                for (Object projectionExpression : externalSource.getArray(FieldName.PROJECTIONS)) {
                    projectionConfigurations.add(new ProjectionConfiguration(projectionExpression.toString()));
                }
                result.put(sourceName, projectionConfigurations);
            }
            return result;
        }

        /**
         * Validate the list of connector configurations.
         * 
         * @param problems the container for problems reading the configuration information; may not be null
         */
        protected void validateConnectors( Problems problems ) {
            readComponents(federation, FieldName.EXTERNAL_SOURCES, FieldName.CLASSNAME, CONNECTOR_ALIASES, problems);
        }

        /**
         * Object representation of a projection configuration within an external source
         */
        public class ProjectionConfiguration {
            private final String workspaceName;
            private final String externalPath;

            private String projectedPath;

            /**
             * Creates a new projection using a string expression
             * 
             * @param pathExpression a {@code non-null} String
             */
            public ProjectionConfiguration( String pathExpression ) {
                Matcher expressionMatcher = PROJECTION_PATH_EXPRESSION_PATTERN.matcher(pathExpression);
                // should be validated by the repository schema
                if (expressionMatcher.matches()){

                	workspaceName = expressionMatcher.group(1);
                	projectedPath = expressionMatcher.group(2);
                	if (projectedPath.endsWith("/") && projectedPath.length() > 1) {
                		projectedPath = projectedPath.substring(0, projectedPath.length() - 1);
                	}
                	externalPath = expressionMatcher.group(7);
                }else{
                	throw new IllegalStateException(JcrI18n.invalidProjectionExpression.text(pathExpression));
                }
            }

            /**
             * Returns the projection's external path.
             * 
             * @return a {@code non-null} String
             */
            public String getExternalPath() {
                return externalPath;
            }

            /**
             * Returns the projected path
             * 
             * @return a {@code non-null} String
             */
            public String getProjectedPath() {
                return projectedPath;
            }

            /**
             * Returns the projection's workspace name
             * 
             * @return a {@code non-null} String
             */
            public String getWorkspaceName() {
                return workspaceName;
            }

            /**
             * Returns the alias of a projection.
             * 
             * @return a {@code non-null} String
             */
            public String getAlias() {
                return projectedPath.substring(projectedPath.lastIndexOf("/") + 1);
            }

            /**
             * Returns the repository path
             * 
             * @return a {@code non-null} String
             */
            public String getRepositoryPath() {
                return projectedPath.substring(0, projectedPath.lastIndexOf("/") + 1);
            }
        }
    }

    /**
     * Class holding the clustering configuration for a repository.
     */
    @Immutable
    public class Clustering {

        private final Document clusteringDoc;

        public Clustering( Document clusteringDoc ) {
            this.clusteringDoc = (clusteringDoc != null && JGROUPS_PRESENT) ? clusteringDoc : EMPTY;
        }

        /**
         * Checks whether clustering is enabled or not, based on a) JGroups being in the classpath and b) a clustering
         * configuration having been provided.
         * 
         * @return true if clustering is enabled, or false otherwise
         */
        public boolean isEnabled() {
            return this.clusteringDoc != EMPTY;
        }

        public String getChannelProviderClassName() {
            return clusteringDoc.getString(FieldName.CHANNEL_PROVIDER, Default.CHANNEL_PROVIDER);
        }

        public String getClusterName() {
            return clusteringDoc.getString(FieldName.CLUSTER_NAME, Default.CLUSTER_NAME);
        }

        public String getChannelConfiguration() {
            return clusteringDoc.getString(FieldName.CHANNEL_CONFIGURATION);
        }

        public Document getDocument() {
            return clusteringDoc;
        }

        /**
         * Attempt to get the correct channel from the environment, if the environment has specified one.
         * 
         * @return the environment's channel, if defined; may be null if the environment doesn't provide a channel and the
         *         {@link #getChannelProviderClassName() channel provider} should be used
         * @throws Exception if there is a problem getting the channel from the environment
         */
        public Channel getChannel() throws Exception {
            Environment env = environment();
            if (env == null) return null;
            Channel channel = env.getChannel(getClusterName());
            return channel;
        }
    }

    protected List<Component> readComponents( Document doc,
                                              String fieldName,
                                              String aliasFieldName,
                                              Map<String, String> classnamesByAlias,
                                              Problems problems ) {
        List<Component> results = new ArrayList<Component>();
        Document components = doc.getDocument(fieldName);
        if (components != null) {
            boolean isArray = components instanceof List;
            for (Field field : components.fields()) {
                Object value = field.getValue();
                if (value instanceof Document) {
                    Document component = (Document)value;
                    String classname = component.getString(FieldName.CLASSNAME);
                    String classpath = component.getString(FieldName.CLASSLOADER); // optional
                    String name = isArray ? component.getString(FieldName.NAME) : field.getName();
                    if (classname != null) {
                        String resolvedClassname = classnamesByAlias.get(classname.toLowerCase());
                        if (resolvedClassname != null) classname = resolvedClassname;
                    } else {
                        String aliases = aliasesStringFrom(classnamesByAlias);
                        problems.addError(JcrI18n.missingComponentType, aliases);
                    }
                    if (classname != null) {
                        if (name == null) name = classname;
                        results.add(new Component(name, classname, classpath, component));
                    }
                }
            }
        }
        return Collections.unmodifiableList(results);
    }

    private String aliasesStringFrom( Map<String, String> classnamesByAlias ) {
        StringBuilder aliases = new StringBuilder();
        boolean first = true;
        for (String validAlias : classnamesByAlias.keySet()) {
            if (first) first = false;
            else aliases.append(", ");
            aliases.append('"').append(validAlias).append('"');
        }
        return aliases.toString();
    }

    protected Map<String, Object> readProperties( Document document,
                                                  String... skipFieldNames ) {
        Map<String, Object> props = new HashMap<String, Object>();
        Set<String> skipFields = new HashSet<String>(Arrays.asList(skipFieldNames));
        for (Field field : document.fields()) {
            String name = field.getName();
            if (skipFields.contains(name)) continue;
            props.put(name, field.getValue());
        }
        return props;
    }

    protected Context jndiContext() throws NamingException {
        return new InitialContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Json.write(doc);
    }

    /**
     * Make a clone of this configuration and return an editor for changing that clone. As the editor is used to alter the cloned
     * configuration, the editor records the {@link Changes changes}. After all changes are completed, the editor (which
     * represents the newly modified configuration) can be used to create a
     * {@link RepositoryConfiguration#RepositoryConfiguration(Document, String) new RepositoryConfiguration}, or the
     * {@link Editor#getChanges() editor's changes} can be used to update an already deployed (and running) repository.
     * <p>
     * For example, the following code shows how an existing RepositoryConfiguration instance can be used to create a second
     * configuration that is a slightly-modified copy of the original.
     * 
     * <pre>
     * </pre>
     * </p>
     * <p>
     * Also, the following code shows how an existing RepositoryConfiguration instance for a deployed repository can be updated:
     * 
     * <pre>
     *   ModeShapeEngine engine = ...
     *   Repository deployed = engine.getRepository("repo");
     *   RepositoryConfiguration deployedConfig = deployed.getConfiguration();
     * 
     *   // Create an editor ...
     *   Editor editor = deployedConfig.edit();
     * 
     *   // Modify the copy of the configuration (we'll do something trivial here) ...
     *   editor.setNumber(FieldName.LARGE_VALUE_SIZE_IN_BYTES,8096);
     * 
     *   // Get the changes and validate them ...
     *   Changes changes = editor.getChanges();
     *   Results validationResults = deployedConfig.validate(changes);
     *   if ( validationResults.hasErrors() ) {
     *       // do something
     *   } else {
     *       // Update the deployed repository's configuration with these changes ...
     *       engine.update("repo",changes);
     *   }
     * </pre>
     * 
     * </p>
     * 
     * @return an editor for modifying a copy of this repository configuration.
     * @see #validate(Changes)
     */
    public Editor edit() {
        return Schematic.editDocument(this.doc, true);
    }

    /***
     * Validate this configuration against the JSON Schema.
     * 
     * @return the validation results; never null
     * @see #validate(Changes)
     */
    public Problems validate() {
        if (problems == null) {
            SimpleProblems problems = new SimpleProblems();
            Results results = SCHEMA_LIBRARY.validate(doc, JSON_SCHEMA_URI);
            for (Problem problem : results) {
                switch (problem.getType()) {
                    case ERROR:
                        problems.addError(JcrI18n.configurationError, problem.getPath(), problem.getReason());
                        break;
                    case WARNING:
                        problems.addWarning(JcrI18n.configurationWarning, problem.getPath(), problem.getReason());
                        break;
                }
            }
            // Validate the components ...
            getSecurity().validateCustomProviders(problems);
            getSequencing().validateSequencers(problems);
            getQuery().getTextExtracting().validateTextExtractors(problems);
            this.problems = problems;
        }
        return problems;
    }

    /***
     * Validate this configuration if the supplied changes were made to this. Note that this does <i>not</i> actually change this
     * configuration.
     * 
     * @param changes the proposed changes to this configuration's underlying document; never null
     * @return the validation results; never null
     * @see #edit()
     * @see #validate()
     */
    public Problems validate( Changes changes ) {
        // Create a copy of this configuration ...
        Editor copy = edit();
        copy.apply(changes);
        RepositoryConfiguration updated = new RepositoryConfiguration(copy, this.getName());
        return updated.validate();
    }

    /**
     * Create a copy of this configuration that uses the supplied Infinispan {@link CacheContainer} instance.
     * 
     * @param environment the environment that should be used for the repository; may be null
     * @return the new configuration; never null
     */
    public RepositoryConfiguration with( Environment environment ) {
        return new RepositoryConfiguration(doc.clone(), docName, environment);
    }

    /**
     * Create a copy of this configuration that uses the supplied document name.
     * 
     * @param docName the new document name; may be null
     * @return the new configuration; never null
     */
    public RepositoryConfiguration withName( String docName ) {
        return new RepositoryConfiguration(doc.clone(), docName, environment);
    }

    @Immutable
    public class Component {
        private final String name;
        private final String classname;
        private final String classpath;
        private final Document document;

        protected Component( String name,
                             String classname,
                             String classpath,
                             Document document ) {
            assert classname != null;
            this.classname = classname;
            this.classpath = classpath;
            this.name = name != null ? name : classname;
            this.document = document;
        }

        /**
         * Get the component's name.
         * 
         * @return the name of this component; never null
         */
        public String getName() {
            return name;
        }

        /**
         * @return classname
         */
        public String getClassname() {
            return classname;
        }

        /**
         * @return classpath
         */
        public String getClasspath() {
            return classpath;
        }

        /**
         * @return document
         */
        public Document getDocument() {
            return document;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Component) {
                Component that = (Component)obj;
                if (!this.getClassname().equals(that.getClassname())) return false;
                if (!this.getName().equals(that.getName())) return false;
                if (!ObjectUtil.isEqualWithNulls(this.getClasspath(), that.getClasspath())) return false;
                if (!this.getDocument().equals(that.getDocument())) return false;
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return document.toString();
        }

        /**
         * Create an instance of this class.
         * 
         * @param <Type>
         * @param fallbackLoader the fallback class loader that should be used for
         *        {@link Environment#getClassLoader(ClassLoader, String...)}
         * @return the new instance, with all {@link #getDocument() document fields} set on it; never null
         * @see #getClasspath()
         * @throws Exception if anything fails
         */
        @SuppressWarnings( "unchecked" )
        public <Type> Type createInstance( ClassLoader fallbackLoader ) throws Exception {
            // Handle some of the built-in providers in a special way ...
            String classname = getClassname();
            if (AnonymousProvider.class.getName().equals(classname)) {
                return (Type)createAnonymousProvider();
            } else if (JaasProvider.class.getName().equals(classname)) {
                return (Type)createJaasProvider();
            }
            ClassLoader classLoader = environment().getClassLoader(fallbackLoader, classpath);
            return (Type)createGenericComponent(classLoader);
        }

        @SuppressWarnings( {"unchecked", "cast"} )
        private <Type> Type createGenericComponent( ClassLoader classLoader ) {
            // Create the instance ...
            Type instance = (Type)Util.getInstance(getClassname(), classLoader);
            if (ReflectionUtil.getField("name", instance.getClass()) != null) {
                // Always try to set the name (if there is such a field). The name may be set based upon
                // the value in the document, but a name field in documents is not required ...
                ReflectionUtil.setValue(instance, "name", getName());
            }
            setTypeFields(instance, getDocument());
            return (Type)instance;
        }

        @SuppressWarnings( "unchecked" )
        private <Type> Type createJaasProvider() throws LoginException {
            Object value = this.document.get(FieldName.JAAS_POLICY_NAME);
            String policyName = value instanceof String ? value.toString() : Default.JAAS_POLICY_NAME;
            return (Type)new JaasProvider(policyName);
        }

        @SuppressWarnings( "unchecked" )
        private <Type> Type createAnonymousProvider() {
            Object roles = this.document.get(FieldName.ANONYMOUS_ROLES);
            Set<String> roleNames = new HashSet<String>();
            if (roles instanceof Array) {
                Array roleValues = (Array)roles;
                for (Object roleName : roleValues) {
                    if (roleName instanceof String) {
                        roleNames.add(roleName.toString());
                    }
                }
            }
            Object usernameValue = this.document.get(FieldName.ANONYMOUS_USERNAME);
            String username = usernameValue instanceof String ? usernameValue.toString() : Default.ANONYMOUS_USERNAME;
            return (Type)new AnonymousProvider(username, roleNames);
        }

        @SuppressWarnings( "synthetic-access" )
        private void setTypeFields( Object instance,
                                    Document document ) {
            for (Field field : document.fields()) {
                String fieldName = field.getName();
                Object fieldValue = field.getValue();
                if (COMPONENT_SKIP_PROPERTIES.contains(fieldName)) {
                    continue;
                }
                try {
                    // locate the field instance on which the value will be set
                    java.lang.reflect.Field instanceField = findField(instance.getClass(), fieldName);
                    if (instanceField == null) {
                        LOGGER.warn(JcrI18n.missingFieldOnInstance, fieldName, getClassname());
                        continue;
                    }

                    Object convertedFieldValue = convertValueToType(instanceField.getType(), fieldValue);

                    // if the value is a document, means there is a nested bean
                    if (convertedFieldValue instanceof Document) {
                        // only no-arg constructors are supported
                        Object innerInstance = instanceField.getType().newInstance();
                        setTypeFields(innerInstance, (Document)convertedFieldValue);
                        convertedFieldValue = innerInstance;
                    }

                    // this is very ! tricky because it does not throw an exception - ever
                    ReflectionUtil.setValue(instance, fieldName, convertedFieldValue);
                } catch (Throwable e) {
                    LOGGER.error(e, JcrI18n.unableToSetFieldOnInstance, fieldName, fieldValue, getClassname());
                }
            }
        }

        /**
         * Attempts "its best" to convert a generic Object value (coming from a Document) to a value which can be set on the field
         * of a component. Note: thanks to type erasure, generics are not supported.
         * 
         * @param expectedType the {@link Class} of the field on which the value should be set
         * @param value a generic value coming from a document. Can be a simple value, another {@link Document} or {@link Array}
         * @return the converted value, which should be compatible with the expected type.
         * @throws Exception if anything will fail during the conversion process
         */
        private Object convertValueToType( Class<?> expectedType,
                                           Object value ) throws Exception {
            // lists are converted to ArrayList
            if (List.class.isAssignableFrom(expectedType)) {
                return valueToCollection(value, new ArrayList<Object>());
            }
            // sets are converted to HashSet
            if (Set.class.isAssignableFrom(expectedType)) {
                return valueToCollection(value, new HashSet<Object>());
            }
            // arrays are converted as-is
            if (expectedType.isArray()) {
                return valueToArray(expectedType.getComponentType(), value);
            }

            // maps are converted to hashmap
            if (Map.class.isAssignableFrom(expectedType)) {
                // only string keys are supported atm
                return ((Document)value).toMap();
            }

            // Strings can be parsed into numbers ...
            if (value instanceof String) {
                String strValue = (String)value;
                // Try the smallest ranges first ...
                if (Short.TYPE.isAssignableFrom(expectedType) || Short.class.isAssignableFrom(expectedType)) {
                    try {
                        return Short.parseShort(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Integer.TYPE.isAssignableFrom(expectedType) || Integer.class.isAssignableFrom(expectedType)) {
                    try {
                        return Integer.parseInt(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Long.TYPE.isAssignableFrom(expectedType) || Long.class.isAssignableFrom(expectedType)) {
                    try {
                        return Long.parseLong(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Boolean.TYPE.isAssignableFrom(expectedType) || Boolean.class.isAssignableFrom(expectedType)) {
                    try {
                        return Boolean.parseBoolean(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Float.TYPE.isAssignableFrom(expectedType) || Float.class.isAssignableFrom(expectedType)) {
                    try {
                        return Float.parseFloat(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
                if (Double.TYPE.isAssignableFrom(expectedType) || Double.class.isAssignableFrom(expectedType)) {
                    try {
                        return Double.parseDouble(strValue);
                    } catch (NumberFormatException e) {
                        // ignore and continue ...
                    }
                }
            }

            // return value as it is
            return value;
        }

        private Object valueToArray( Class<?> arrayComponentType,
                                     Object value ) throws Exception {
            if (value instanceof Array) {
                Array valueArray = (Array)value;
                int arraySize = valueArray.size();
                Object newArray = java.lang.reflect.Array.newInstance(arrayComponentType, arraySize);
                for (int i = 0; i < ((Array)value).size(); i++) {
                    Object element = valueArray.get(i);
                    element = convertValueToType(arrayComponentType, element);
                    java.lang.reflect.Array.set(newArray, i, element);
                }
                return newArray;
            } else if (value instanceof String) {
                // Parse the string into a comma-separated set of values (this works if it's just a single value) ...
                String strValue = (String)value;
                if (strValue.length() > 0) {
                    String[] stringValues = strValue.split(",");
                    Object newArray = java.lang.reflect.Array.newInstance(arrayComponentType, stringValues.length);
                    for (int i = 0; i < stringValues.length; i++) {
                        Object element = convertValueToType(arrayComponentType, stringValues[i]);
                        java.lang.reflect.Array.set(newArray, i, element);
                    }
                    return newArray;
                }
            }

            // Otherwise, just initialize it to an empty array ...
            return java.lang.reflect.Array.newInstance(arrayComponentType, 0);
        }

        private Collection<?> valueToCollection( Object value,
                                                 Collection<Object> collection ) throws Exception {
            if (value instanceof Array) {
                collection.addAll((List<?>)value);
            } else {
                collection.add(value);
            }
            return collection;
        }

        private java.lang.reflect.Field findField( Class<?> typeClass,
                                                   String fieldName ) {
            java.lang.reflect.Field field = null;
            if (typeClass != null) {
                try {
                    field = typeClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    field = findField(typeClass.getSuperclass(), fieldName);
                }
            }
            return field;
        }
    }

}

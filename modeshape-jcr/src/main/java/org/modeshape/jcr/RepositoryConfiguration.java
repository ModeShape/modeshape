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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
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
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.collection.ring.RingBufferBuilder;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.common.util.Reflection;
import org.modeshape.common.util.ResourceLookup;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.index.local.LocalIndexProvider;
import org.modeshape.jcr.mimetype.ContentDetector;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.NameOnlyDetector;
import org.modeshape.jcr.mimetype.NullMimeTypeDetector;
import org.modeshape.jcr.security.AnonymousProvider;
import org.modeshape.jcr.security.JaasProvider;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.binary.AbstractBinaryStore;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;
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

    /**
     * The regexp pattern used to parse & validate index column definitions. The expression for a single column definition is of
     * the form "{@code name(type)}" where "{@code name}" is the name of the property and "{@code type}" is the type of that
     * property. The expression is:
     *
     * <pre>
     * (([^:/]+):)?([^:]*)(\(\w{1,}\))
     * </pre>
     *
     * The full expression really represents a comma-separated list of one or more of these index definitions. where:
     * <ul>
     * <li>Group 1 is the workspace name</li>
     * <li>Group 2 is the path of the existing node</li>
     * <li>Group 7 is the path in the external source</li>
     * </ul>
     */
    private final static String INDEX_COLUMN_DEFINITION_STRING = "(([^:/]+):)?([^:]*)(\\(\\w{1,}\\))";
    private final static String INDEX_COLUMN_DEFINITIONS_STRING = INDEX_COLUMN_DEFINITION_STRING + "(,"
                                                                  + INDEX_COLUMN_DEFINITION_STRING + ")*";
    public final static Pattern INDEX_COLUMN_DEFINITIONS_PATTERN = Pattern.compile(INDEX_COLUMN_DEFINITIONS_STRING);

    /**
     * As binary values are no longer used, they are quarantined in the binary store. When the garbage collection process runs,
     * any binary values that have been quarantined longer than this duration will be removed.
     * <p>
     * The age is 1 hour, to ensure that binary values are not removed prematurely (e.g., when one session removes a binary value
     * from a property while another session shortly thereafter reuses it).
     * </p>
     */
    final static int UNUSED_BINARY_VALUE_AGE_IN_MILLIS = (int)TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

    final static String INITIAL_TIME_REGEX = "(\\d\\d):(\\d\\d)";
    final static Pattern INITIAL_TIME_PATTERN = Pattern.compile(INITIAL_TIME_REGEX);

    protected static final Document EMPTY = Schematic.newDocument();

    protected static final Map<String, String> PROVIDER_ALIASES;
    protected static final Map<String, String> SEQUENCER_ALIASES;
    protected static final Map<String, String> INDEX_PROVIDER_ALIASES;
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
         * The name for the field whose value is the size of the event buffer
         */
        public static final String EVENT_BUS_SIZE = "eventBusSize";

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
         *
         * @deprecated The transaction manager lookup class should be specified in the Infinispan cache configuration (or in a
         *             custom Environment subclass for default caches)
         */
        @Deprecated
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
        public static final String MINIMUM_STRING_SIZE = "minimumStringSize";

        /**
         * The name attribute which can be set on a binary store. It's only used when a {@link CompositeBinaryStore} is
         * configured.
         */
        public static final String BINARY_STORE_NAME = "storeName";

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
         * The name for the field whose value is a document containing binary storage information.
         */
        public static final String COMPOSITE_STORE_NAMED_BINARY_STORES = "namedStores";
        
        public static final String MIMETYPE_DETECTION = "mimeTypeDetection";

        /**
         * The name for the field whose value is a document containing security information.
         */
        public static final String SECURITY = "security";

        /**
         * The name of the security domain used together with a {@link org.modeshape.jcr.security.EnvironmentAuthenticationProvider}
         */
        public static final String SECURITY_DOMAIN = "securityDomain";

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

        public static final String INDEX_PROVIDERS = "indexProviders";
        public static final String PROVIDERS = "providers";
        public static final String PROVIDER_NAME = "provider";
        public static final String KIND = "kind";
        public static final String SYNCHRONOUS = "synchronous";
        public static final String NODE_TYPE = "nodeType";
        public static final String COLUMNS = "columns";
        public static final String TYPE = "type";
        public static final String DIRECTORY = "directory";
        public static final String CLASSLOADER = "classloader";
        public static final String CLASSNAME = "classname";
        public static final String DATA_SOURCE_JNDI_NAME = "dataSourceJndiName";
        public static final String DATA_CACHE_NAME = "dataCacheName";
        public static final String INDEXES = "indexes";
        public static final String METADATA_CACHE_NAME = "metadataCacheName";
        public static final String CHUNK_SIZE = "chunkSize";
        public static final String TEXT_EXTRACTION = "textExtraction";
        public static final String EXTRACTORS = "extractors";
        public static final String SEQUENCING = "sequencing";
        public static final String SEQUENCERS = "sequencers";
        public static final String EXTERNAL_SOURCES = "externalSources";
        public static final String EXPOSE_AS_WORKSPACE = "exposeAsWorkspace";
        public static final String PROJECTIONS = "projections";
        public static final String PATH_EXPRESSION = "pathExpression";
        public static final String PATH_EXPRESSIONS = "pathExpressions";
        public static final String JDBC_DRIVER_CLASS = "driverClass";
        public static final String CONNECTION_URL = "url";
        public static final String REINDEXING = "reindexing";
        public static final String REINDEXING_ASYNC = "async";
        public static final String REINDEXING_MODE = "mode";

        public static final String GARBAGE_COLLECTION = "garbageCollection";
        public static final String INITIAL_TIME = "initialTime";
        public static final String INTERVAL_IN_HOURS = "intervalInHours";

        public static final String DOCUMENT_OPTIMIZATION = "documentOptimization";
        public static final String OPTIMIZATION_CHILD_COUNT_TARGET = "childCountTarget";
        public static final String OPTIMIZATION_CHILD_COUNT_TOLERANCE = "childCountTolerance";

        /**
         * The name for the field (under "sequencing" and "textExtraction") specifying the thread pool that should be used for sequencing.
         */
        public static final String THREAD_POOL = "threadPool";

        /**
         * The name of the field which allows the configuration of the maximum number of threads that can be spawned by a pool
         */
        public static final String MAX_POOL_SIZE = "maxPoolSize";
        
        /**
         * The name of the journaling schema field.
         */
        public static final String JOURNALING = "journaling";

        /**
         * The location where the journal should be kept
         */
        public static final String JOURNAL_LOCATION = "location";

        /**
         * The maximum number of days journal entries should be stored on disk
         */
        public static final String MAX_DAYS_TO_KEEP_RECORDS = "maxDaysToKeepRecords";

        /**
         * Whether asynchronous writes into the journal should be enabled or not.
         */
        public static final String ASYNC_WRITES_ENABLED = "asyncWritesEnabled";
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
         * The default value of the {@link FieldName#EVENT_BUS_SIZE} field is '{@value}'
         */
        public static final int EVENT_BUS_SIZE = RingBufferBuilder.DEFAULT_BUFFER_SIZE;

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

        public static final boolean MONITORING_ENABLED = true;

        public static final String SEQUENCING_POOL = "modeshape-sequencer";
        public static final String TEXT_EXTRACTION_POOL = "modeshape-text-extractor";
        public static final String GARBAGE_COLLECTION_POOL = "modeshape-gc";
        public static final String OPTIMIZATION_POOL = "modeshape-opt";
        public static final String JOURNALING_POOL = "modeshape-journaling-gc";

        public static final String GARBAGE_COLLECTION_INITIAL_TIME = "00:00";
        public static final int GARBAGE_COLLECTION_INTERVAL_IN_HOURS = 24;

        public static final String OPTIMIZATION_INITIAL_TIME = "02:00";
        public static final int OPTIMIZATION_INTERVAL_IN_HOURS = 24;

        public static final String JOURNAL_LOCATION = "modeshape/journal";
        // by default journal entries are kept indefinitely
        public static final int MAX_DAYS_TO_KEEP_RECORDS = -1;
        public static final boolean ASYNC_WRITES_ENABLED = false;

        public static final String KIND = IndexKind.VALUE.name();
        public static final String NODE_TYPE = "nt:base";
        public static final boolean SYNCHRONOUS = true;
        public static final String WORKSPACES = "*";

        public static final int SEQUENCING_MAX_POOL_SIZE = 10;
        public static final int TEXT_EXTRACTION_MAX_POOL_SIZE = 5;
    }

    public static final class FieldValue {
        public static final String BINARY_STORAGE_TYPE_TRANSIENT = "transient";
        public static final String BINARY_STORAGE_TYPE_FILE = "file";
        public static final String BINARY_STORAGE_TYPE_CACHE = "cache";
        public static final String BINARY_STORAGE_TYPE_DATABASE = "database";
        public static final String BINARY_STORAGE_TYPE_COMPOSITE = "composite";
        public static final String BINARY_STORAGE_TYPE_CUSTOM = "custom";

        public static final String KIND_VALUE = "value";
        public static final String KIND_UNIQUE = "unique";
        public static final String KIND_ENUMERATED = "enumerated";
        public static final String KIND_TEXT = "text";
        public static final String KIND_NODE_TYPE = "nodetype";
        
        public static final String MIMETYPE_DETECTION_NONE = "none";
        public static final String MIMETYPE_DETECTION_NAME = "name";
        public static final String MIMETYPE_DETECTION_CONTENT = "content";
    }

    protected static final Set<List<String>> DEPRECATED_FIELDS;

    /**
     * The set of field names that should be skipped when {@link Component#createInstance(ClassLoader) instantiating a component}.
     */
    protected static final Set<String> COMPONENT_SKIP_PROPERTIES;

    static {
        Set<String> skipProps = new HashSet<String>();
        skipProps.add(FieldName.CLASSLOADER);
        skipProps.add(FieldName.CLASSNAME);
        skipProps.add(FieldName.PROJECTIONS);
        skipProps.add(FieldName.EXPOSE_AS_WORKSPACE);
        
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

        String localIndexProvider = LocalIndexProvider.class.getName();
        aliases = new HashMap<String, String>();
        aliases.put("local", localIndexProvider);
        aliases.put("lucene", "org.modeshape.jcr.index.lucene.LuceneIndexProvider");

        INDEX_PROVIDER_ALIASES = Collections.unmodifiableMap(aliases);

        String fileSystemConnector = FileSystemConnector.class.getName();
        String gitConnector = "org.modeshape.connector.git.GitConnector";
        String cmisConnector = "org.modeshape.connector.cmis.CmisConnector";

        aliases = new HashMap<String, String>();
        aliases.put("files", fileSystemConnector);
        aliases.put("filesystem", fileSystemConnector);
        aliases.put("filesystemconnector", fileSystemConnector);
        aliases.put("git", gitConnector);
        aliases.put("gitconnector", gitConnector);
        aliases.put("cmis", cmisConnector);
        aliases.put("cmisconnector", cmisConnector);

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
        InputStream configStream = ResourceLookup.read(JSON_SCHEMA_RESOURCE_PATH, RepositoryConfiguration.class, false);
        if (configStream == null) {
            LOGGER.error(JcrI18n.unableToFindRepositoryConfigurationSchema, JSON_SCHEMA_RESOURCE_PATH);
        }
        try {
            Document configDoc = Json.read(configStream);
            SCHEMA_LIBRARY.put(JSON_SCHEMA_URI, configDoc);
        } catch (IOException e) {
            LOGGER.error(e, JcrI18n.unableToLoadRepositoryConfigurationSchema, JSON_SCHEMA_RESOURCE_PATH);
        }

        Set<List<String>> deprecatedFieldNames = new HashSet<List<String>>();
        deprecatedFieldNames.add(Collections.unmodifiableList(Arrays.asList(new String[] {FieldName.STORAGE,
            FieldName.CACHE_TRANSACTION_MANAGER_LOOKUP})));
        DEPRECATED_FIELDS = Collections.unmodifiableSet(deprecatedFieldNames);
    }

    /**
     * The regular expression used to capture the index column definition property name and type. The expression is "
     * <code>([^(,]+)[(]([^),]+)[)]</code>".
     */
    protected static final String COLUMN_DEFN_PATTERN_STRING = "([^(,]+)[(]([^),]+)[)]";
    protected static final Pattern COLUMN_DEFN_PATTERN = Pattern.compile(COLUMN_DEFN_PATTERN_STRING);
    protected static final Set<String> INDEX_PROVIDER_FIELDS = org.modeshape.common.collection.Collections.unmodifiableSet(FieldName.PROVIDER_NAME,
                                                                                                                           FieldName.DESCRIPTION,
                                                                                                                           FieldName.NODE_TYPE,
                                                                                                                           FieldName.KIND,
                                                                                                                           FieldName.COLUMNS,
                                                                                                                           FieldName.WORKSPACES);

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
        CheckArg.isNotNull(url, "url");
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
        CheckArg.isNotNull(file, "file");
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
        CheckArg.isNotNull(stream, "stream");
        CheckArg.isNotNull(name, "name");
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
        CheckArg.isNotNull(resourcePathOrJsonContentString, "resourcePathOrJsonContentString");
        InputStream stream = ResourceLookup.read(resourcePathOrJsonContentString, RepositoryConfiguration.class, true);

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

    public BinaryStorage getBinaryStorage() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage == null) {
            storage = Schematic.newDocument();
        }
        return new BinaryStorage(storage.getDocument(FieldName.BINARY_STORAGE));
    }

    /**
     * Returns the journaling configuration
     *
     * @return a {@link Journaling} instance, never {@code null}
     */
    public Journaling getJournaling() {
        return new Journaling(doc.getDocument(FieldName.JOURNALING));
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
     * Returns the reindexing configuration.
     * 
     * @return a {@link org.modeshape.jcr.RepositoryConfiguration.Reindexing} instance, never {@code null}.
     */
    public Reindexing getReindexing() {
        return new Reindexing(doc.getDocument(FieldName.REINDEXING));
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
     * Returns a fully qualified built-in index provider class name mapped to the given alias, or {@code null} if there isn't such
     * a mapping
     *
     * @param alias the alias
     * @return the name of the index provider class, or null if the alias did not correspond to a built-in class
     */
    public static String getBuiltInIndexProviderClassName( String alias ) {
        return INDEX_PROVIDER_ALIASES.get(alias);
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
            return binaryStorage.getLong(FieldName.MINIMUM_STRING_SIZE, getMinimumBinarySizeInBytes());
        }

        public BinaryStore getBinaryStore() throws Exception {
            String type = getType();
            BinaryStore store = null;
            if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_TRANSIENT)) {
                store = TransientBinaryStore.get();
            } else if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_FILE)) {
                String directory = binaryStorage.getString(FieldName.DIRECTORY);
                assert directory != null;
                File dir = new File(directory);
                store = FileSystemBinaryStore.create(dir);
            } else if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_DATABASE)) {
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
            } else if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_CACHE)) {
                String metadataCacheName = binaryStorage.getString(FieldName.METADATA_CACHE_NAME, getName());
                String blobCacheName = binaryStorage.getString(FieldName.DATA_CACHE_NAME, getName());
                String cacheConfiguration = binaryStorage.getString(FieldName.CACHE_CONFIGURATION); // may be null
                int chunkSize = binaryStorage.getInteger(FieldName.CHUNK_SIZE, InfinispanBinaryStore.DEFAULT_CHUNK_SIZE);
                boolean dedicatedCacheContainer = false;
                if (cacheConfiguration == null) {
                    cacheConfiguration = getCacheConfiguration();
                } else {
                    dedicatedCacheContainer = true;
                }
                CacheContainer cacheContainer = getCacheContainer(cacheConfiguration);

                // String cacheTransactionManagerLookupClass = binaryStorage.getString(FieldName.CACHE_TRANSACTION_MANAGER_LOOKUP,
                // Default.CACHE_TRANSACTION_MANAGER_LOOKUP);
                store = new InfinispanBinaryStore(cacheContainer, dedicatedCacheContainer, metadataCacheName, blobCacheName,
                                                  chunkSize);
            } else if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_COMPOSITE)) {

                Map<String, BinaryStore> binaryStores = new LinkedHashMap<String, BinaryStore>();

                Document binaryStoresConfiguration = binaryStorage.getDocument(FieldName.COMPOSITE_STORE_NAMED_BINARY_STORES);

                for (String sourceName : binaryStoresConfiguration.keySet()) {
                    Document binaryStoreConfig = binaryStoresConfiguration.getDocument(sourceName);
                    binaryStores.put(sourceName, new BinaryStorage(binaryStoreConfig).getBinaryStore());
                }

                // must have at least one named store
                if (binaryStores.isEmpty()) {
                    throw new BinaryStoreException(JcrI18n.missingVariableValue.text("namedStores"));
                }

                store = new CompositeBinaryStore(binaryStores);

            } else if (type.equalsIgnoreCase(FieldValue.BINARY_STORAGE_TYPE_CUSTOM)) {
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

        /**
         * Returns the type of the configured binary store.
         *
         * @return the type of the configured binary store, never {@code null}
         */
        public String getType() {
            return binaryStorage.getString(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_TRANSIENT);
        }
        
        protected MimeTypeDetector getMimeTypeDetector(Environment environment) {
            String mimeTypeDetection = binaryStorage.getString(FieldName.MIMETYPE_DETECTION, FieldValue.MIMETYPE_DETECTION_CONTENT);
            switch (mimeTypeDetection.toLowerCase()) {
                case FieldValue.MIMETYPE_DETECTION_CONTENT: {
                    return new ContentDetector(environment);
                }
                case FieldValue.MIMETYPE_DETECTION_NAME: {
                    return new NameOnlyDetector(environment);
                }
                case FieldValue.MIMETYPE_DETECTION_NONE: {
                    return NullMimeTypeDetector.INSTANCE;
                }
                default: {
                    throw new IllegalArgumentException("Unknown mime-type detector setting: " + mimeTypeDetection);
                }
            }
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
                    Reflection.setValue(instance, fieldName, convertedFieldValue);
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
    
    public int getEventBusSize() {
        return doc.getInteger(FieldName.EVENT_BUS_SIZE, Default.EVENT_BUS_SIZE);
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
        protected List<Component> getCustomProviders() {
            Problems problems = new SimpleProblems();
            List<Component> components = getCustomProviders(problems);
            assert !problems.hasErrors();
            return components;
        }

        protected List<Component> getCustomProviders( Problems problems ) {
            return readComponents(security, FieldName.PROVIDERS, FieldName.CLASSNAME, PROVIDER_ALIASES, problems);

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
    public enum TransactionMode {
        AUTO,
        NONE
    }

    /**
     * Get the ordered list of index providers defined in the configuration.
     *
     * @return the immutable list of provider components; never null but possibly empty
     */
    public List<Component> getIndexProviders() {
        Problems problems = new SimpleProblems();
        List<Component> components = readComponents(doc, FieldName.INDEX_PROVIDERS, FieldName.CLASSNAME, INDEX_PROVIDER_ALIASES,
                                                    problems);
        assert !problems.hasErrors();
        return components;
    }

    protected void validateIndexProviders( Problems problems ) {
        readComponents(doc, FieldName.INDEX_PROVIDERS, FieldName.CLASSNAME, PROVIDER_ALIASES, problems);
    }

    /**
     * Get the configuration for the indexes used by this repository.
     *
     * @return the index-related configuration; never null
     */
    public Indexes getIndexes() {
        return new Indexes(doc.getDocument(FieldName.INDEXES));
    }

    /**
     * The index-related configuration information.
     */
    @Immutable
    public class Indexes {
        private final Document indexes;

        protected Indexes( Document indexes ) {
            this.indexes = indexes;
        }

        /**
         * Determine whether there are indexes defined in this configuration.
         *
         * @return true if there are no indexes, or false otherwise
         */
        public boolean isEmpty() {
            return indexes == null ? true : indexes.isEmpty();
        }

        /**
         * Get the names of the indexes defined in this configuration.
         *
         * @return the index names; never null but possibly empty
         * @see #getIndex(String)
         * @see #getRawIndex(String)
         */
        public Set<String> getIndexNames() {
            if (indexes == null) return Collections.emptySet();
            return indexes.keySet();
        }

        /**
         * Get the document representing the single named index definition.
         *
         * @param name the index name
         * @return the representation of the index definition; or null if there is no index definition with the supplied name
         * @see #getIndexNames()
         */
        public Document getRawIndex( String name ) {
            return indexes == null ? null : indexes.getDocument(name);
        }

        public IndexDefinition getIndex( final String name ) {
            if (name == null) return null;
            final Document doc = getRawIndex(name);
            if (doc == null) return null;
            return new IndexDefinition() {
                private List<IndexColumnDefinition> columns;
                private Map<String, Object> properties;

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getProviderName() {
                    return doc.getString(FieldName.PROVIDER_NAME);
                }

                @Override
                public String getDescription() {
                    return doc.getString(FieldName.DESCRIPTION);
                }

                @Override
                public IndexKind getKind() {
                    String kindStr = doc.getString(FieldName.KIND, Default.KIND);
                    if (FieldValue.KIND_VALUE.equalsIgnoreCase(kindStr)) {
                        return IndexKind.VALUE;
                    }
                    if (FieldValue.KIND_UNIQUE.equalsIgnoreCase(kindStr)) {
                        return IndexKind.UNIQUE_VALUE;
                    }
                    if (FieldValue.KIND_ENUMERATED.equalsIgnoreCase(kindStr)) {
                        return IndexKind.ENUMERATED_VALUE;
                    }
                    if (FieldValue.KIND_TEXT.equalsIgnoreCase(kindStr)) {
                        return IndexKind.TEXT;
                    }
                    if (FieldValue.KIND_NODE_TYPE.equalsIgnoreCase(kindStr)) {
                        return IndexKind.NODE_TYPE;
                    }
                    return IndexKind.VALUE;
                }

                @Override
                public String getNodeTypeName() {
                    return doc.getString(FieldName.NODE_TYPE, Default.NODE_TYPE);
                }

                @Override
                public WorkspaceMatchRule getWorkspaceMatchRule() {
                    String rule = doc.getString(FieldName.WORKSPACES, Default.WORKSPACES);
                    return RepositoryIndexDefinition.workspaceMatchRule(rule);
                }

                @Override
                public boolean isSynchronous() {
                    return doc.getBoolean(FieldName.SYNCHRONOUS, Default.SYNCHRONOUS);
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public int size() {
                    return columns().size();
                }

                @Override
                public boolean appliesToProperty( String propertyName ) {
                    for (IndexColumnDefinition defn : columns()) {
                        if (defn.getPropertyName().equals(propertyName)) return true;
                    }
                    return false;
                }

                @Override
                public IndexColumnDefinition getColumnDefinition( int position ) throws NoSuchElementException {
                    return columns().get(position);
                }

                @Override
                public boolean hasSingleColumn() {
                    return size() == 1;
                }

                @Override
                public Iterator<IndexColumnDefinition> iterator() {
                    return columns.iterator();
                }

                @Override
                public Object getIndexProperty( String propertyName ) {
                    return doc.get(name);
                }

                @Override
                public Map<String, Object> getIndexProperties() {
                    if (properties == null) {
                        // Read in the properties ...
                        properties = new HashMap<>();
                        for (Field field : doc.fields()) {
                            if (INDEX_PROVIDER_FIELDS.contains(field.getName())) continue;
                            properties.put(field.getName(), field.getValue());
                        }
                    }
                    return properties;
                }

                protected List<IndexColumnDefinition> columns() {
                    if (columns == null) {
                        columns = new ArrayList<>();
                        String columnDefnsStr = doc.getString(FieldName.COLUMNS);
                        if (columnDefnsStr != null) {
                            for (String columnDefn : columnDefnsStr.split(",")) {
                                if (columnDefn.trim().length() == 0) continue;
                                try {
                                    Matcher matcher = COLUMN_DEFN_PATTERN.matcher(columnDefn);
                                    if (matcher.find()) {
                                        final String propertyName = matcher.group(1).trim();
                                        String typeStr = matcher.group(2).trim();
                                        final PropertyType type = PropertyType.valueFor(typeStr);
                                        columns.add(new IndexColumnDefinition() {

                                            @Override
                                            public String getPropertyName() {
                                                return propertyName;
                                            }

                                            @Override
                                            public int getColumnType() {
                                                return type.jcrType();
                                            }
                                        });
                                    }
                                } catch (RuntimeException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (columns == null) columns = Collections.emptyList();
                    }
                    return columns;
                }
            };
        }

        protected void validateIndexDefinitions( Problems problems ) {
            for (String indexName : getIndexNames()) {
                IndexDefinition defn = getIndex(indexName);
                // Make sure the index has a valid provider ...
                if (defn.getProviderName() == null) {
                    problems.addError(JcrI18n.indexProviderNameRequired, indexName);
                } else if (!hasIndexProvider(defn.getProviderName())) {
                    problems.addWarning(JcrI18n.indexProviderNameMustMatchProvider, indexName, defn.getProviderName());
                }
            }
        }
    }

    protected boolean hasIndexProvider( String name ) {
        for (Component component : getIndexProviders()) {
            if (component.getName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Possible reindexing modes.
     */
    public enum ReindexingMode {
        IF_MISSING,
        INCREMENTAL
    }
    
    /**
     * The reindexing configuration information.
     */
    @Immutable
    public class Reindexing {
        private final Document reindexing;

        protected Reindexing( Document reindexing ) {
            this.reindexing = reindexing;
        }

        /**
         * Get whether the reindexing should be done synchronously or asynchronously
         * 
         * @return {@code true} if the reindexing should be performed asynchronously, {@code false} otherwise
         */
        public boolean isAsync() {
            return reindexing == null || reindexing.getBoolean(FieldName.REINDEXING_ASYNC, true);
        }

        /**
         * Gets the way reindexing should be performed.
         * 
         * @return a {@link ReindexingMode} instance, never {@code null}
         */
        public ReindexingMode mode() {
            String defaultMode = ReindexingMode.IF_MISSING.name();
            String reindexingMode = reindexing == null ? defaultMode : reindexing.getString(FieldName.REINDEXING_MODE, defaultMode);
            return ReindexingMode.valueOf(reindexingMode.toUpperCase());
        }
    }

    /**
     * Get the configuration for the text extraction aspects of this repository.
     *
     * @return the text extraction configuration; never null
     */
    public TextExtraction getTextExtraction() {
        return new TextExtraction(doc.getDocument(FieldName.TEXT_EXTRACTION));
    }

    @Immutable
    public class TextExtraction {
        private final Document textExtracting;

        public TextExtraction( Document textExtracting ) {
            this.textExtracting = textExtracting != null ? textExtracting : EMPTY;
        }

        /**
         * Get the name of the thread pool that should be used for sequencing work.
         *
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return textExtracting.getString(FieldName.THREAD_POOL, Default.TEXT_EXTRACTION_POOL);
        }

        /**
         * Get the maximum number of threads that can be spawned for sequencing at the same time
         *
         * @return the max number of threads
         */
        public int getMaxPoolSize() {
            return textExtracting.getInteger(FieldName.MAX_POOL_SIZE, Default.TEXT_EXTRACTION_MAX_POOL_SIZE);
        }


        /**
         * Get the ordered list of text extractors. All text extractors are configured with this list.
         *
         * @param problems the container with which should be recorded any problems during component initialization
         * @return the immutable list of text extractors; never null but possibly empty
         */
        protected List<Component> getTextExtractors( Problems problems ) {
            return readComponents(textExtracting, FieldName.EXTRACTORS, FieldName.CLASSNAME, EXTRACTOR_ALIASES, problems);
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
     * Get the configuration for the federation-related aspects of this repository.
     *
     * @return the federation configuration; never null
     */
    public Federation getFederation() {
        return new Federation(doc);
    }

    /**
     * Get the configuration for the garbage collection aspects of this repository.
     *
     * @return the garbage collection configuration; never null
     */
    public GarbageCollection getGarbageCollection() {
        return new GarbageCollection(doc.getDocument(FieldName.GARBAGE_COLLECTION));
    }

    @Immutable
    public class GarbageCollection {
        private final Document gc;

        protected GarbageCollection( Document garbageCollection ) {
            this.gc = garbageCollection != null ? garbageCollection : EMPTY;
        }

        /**
         * Get the name of the thread pool that should be used for garbage collection work.
         *
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return gc.getString(FieldName.THREAD_POOL, Default.GARBAGE_COLLECTION_POOL);
        }

        /**
         * Get the time that the first garbage collection process should be run.
         *
         * @return the initial time; never null
         */
        public String getInitialTimeExpression() {
            return gc.getString(FieldName.INITIAL_TIME, Default.GARBAGE_COLLECTION_INITIAL_TIME);
        }

        /**
         * Get the garbage collection interval in hours.
         *
         * @return the interval; never null
         */
        public int getIntervalInHours() {
            return gc.getInteger(FieldName.INTERVAL_IN_HOURS, Default.GARBAGE_COLLECTION_INTERVAL_IN_HOURS);
        }
        
        /**
         * Get the garbage collection interval in milliseconds.
         *
         * @return the interval; never null
         */
        public long getIntervalInMillis() {
            return TimeUnit.MILLISECONDS.convert(getIntervalInHours(), TimeUnit.HOURS);
        }
    }

    /**
     * Get the configuration for the document optimization for this repository.
     *
     * @return the document optimization configuration; never null
     */
    public DocumentOptimization getDocumentOptimization() {
        Document storage = doc.getDocument(FieldName.STORAGE);
        if (storage == null) {
            storage = Schematic.newDocument();
        }
        return new DocumentOptimization(storage.getDocument(FieldName.DOCUMENT_OPTIMIZATION));
    }

    @Immutable
    public class DocumentOptimization {
        private final Document optimization;

        protected DocumentOptimization( Document optimization ) {
            this.optimization = optimization != null ? optimization : EMPTY;
        }

        /**
         * Determine if document optimization is enabled. At this time, optimization is DISABLED by default and must be enabled by
         * defining the "{@value FieldName#OPTIMIZATION_CHILD_COUNT_TARGET}" and "
         * {@value FieldName#OPTIMIZATION_CHILD_COUNT_TOLERANCE}" fields.
         *
         * @return true if enabled, or false otherwise
         */
        public boolean isEnabled() {
            return !this.optimization.isEmpty() && getChildCountTarget() != Integer.MAX_VALUE;
        }

        /**
         * Get the name of the thread pool that should be used for optimization work.
         *
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return optimization.getString(FieldName.THREAD_POOL, Default.OPTIMIZATION_POOL);
        }

        /**
         * Get the time that the first optimization process should be run.
         *
         * @return the initial time; never null
         */
        public String getInitialTimeExpression() {
            return optimization.getString(FieldName.INITIAL_TIME, Default.OPTIMIZATION_INITIAL_TIME);
        }

        /**
         * Get the optimization interval in hours.
         *
         * @return the interval; never null
         */
        public int getIntervalInHours() {
            return optimization.getInteger(FieldName.INTERVAL_IN_HOURS, Default.OPTIMIZATION_INTERVAL_IN_HOURS);
        }

        /**
         * Get the target for the number of children in a single persisted node document.
         *
         * @return the child count target
         */
        public int getChildCountTarget() {
            Integer result = optimization.getInteger(FieldName.OPTIMIZATION_CHILD_COUNT_TARGET);
            return result == null ? Integer.MAX_VALUE : result.intValue();
        }

        /**
         * Get the tolerance for the number of children in a single persisted node document. Generally, the documents are
         * optimized only when the actual number of children differs from the target by the tolerance.
         *
         * @return the child count tolerance
         */
        public int getChildCountTolerance() {
            Integer result = optimization.getInteger(FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE);
            return result == null ? 0 : result.intValue();
        }
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
         * Get the name of the thread pool that should be used for sequencing work.
         *
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return sequencing.getString(FieldName.THREAD_POOL, Default.SEQUENCING_POOL);
        }

        /**
         * Get the maximum number of threads that can be spawned for sequencing at the same time
         * 
         * @return the max number of threads
         */
        public int getMaxPoolSize() { 
            return sequencing.getInteger(FieldName.MAX_POOL_SIZE, Default.SEQUENCING_MAX_POOL_SIZE);
        }

        /**
         * Get the ordered list of sequencers. All sequencers are configured with this list.
         *
         * @return the immutable list of sequencers; never null but possibly empty
         */
        public List<Component> getSequencers() {
            Problems problems = new SimpleProblems();
            List<Component> components = getSequencers(problems);
            assert !problems.hasErrors();
            return components;
        }

        protected List<Component> getSequencers( Problems problems ) {
            return readComponents(sequencing, FieldName.SEQUENCERS, FieldName.CLASSNAME, SEQUENCER_ALIASES, problems);
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
         * @param problems the container with which should be recorded any problems during component initialization
         * @return the immutable list of connectors; never null but possibly empty
         */
        public List<Component> getConnectors(Problems problems) {
            List<Component> components = readComponents(federation, FieldName.EXTERNAL_SOURCES, FieldName.CLASSNAME,
                    CONNECTOR_ALIASES, problems);
            assert !problems.hasErrors();
            return components;
        }

        /**
         * Returns converged names of all configured external sources.
         * 
         * Converged name is in following format:
         * 'connector-name':'expsed-workspace-name'
         *
         * @return a {@link Set} instance or null if there is not any external
         * source.
         */
        public Set<String> getExternalSources() {
            Document externalSources = federation.getDocument(FieldName.EXTERNAL_SOURCES);
            Set<String> list = new HashSet<>();
            
            //no external sources? nothing to do
            if (externalSources == null) {
                return null;
            }
            
            //checking each external source definition
            Set<String> names = externalSources.keySet();
            for (String name : names) {
                //take ext source definition and see exposeAsWorkspace filed
                Document extSource = externalSources.getDocument(name);
                String extWsName = extSource.getString(FieldName.EXPOSE_AS_WORKSPACE);
                
                //ext source is not going to expose content as workspace
                //ignore it
                if (extWsName == null) {
                    continue;
                }
                
                //ext source saying do not expose me as workspace
                if (extWsName.equalsIgnoreCase("false")) {
                    continue;
                }
                
                //ext source exposes content as workspace and the same name
                if (extWsName.equalsIgnoreCase("true")) {
                    list.add(name + ":" + name);
                }
                
                //ext source saying the name of the workspace
                list.add(name + ":" + extWsName);
            }
            
            return list;
        }

        /**
         * Returns the [workspaceName, list(projections)] of projections configured for each workspace.
         *
         * @return a {@link Map} instance, never null.
         */
        public Map<String, List<ProjectionConfiguration>> getProjectionsByWorkspace() {
            Map<String, List<ProjectionConfiguration>> projectionsByWorkspace = new HashMap<String, List<ProjectionConfiguration>>();
            if (!federation.containsField(FieldName.EXTERNAL_SOURCES)) {
                return projectionsByWorkspace;
            }
            Document externalSources = federation.getDocument(FieldName.EXTERNAL_SOURCES);
            for (String sourceName : externalSources.keySet()) {
                Document externalSource = externalSources.getDocument(sourceName);
                if (!externalSource.containsField(FieldName.PROJECTIONS)) {
                    continue;
                }

                for (Object projectionExpression : externalSource.getArray(FieldName.PROJECTIONS)) {
                    ProjectionConfiguration projectionConfiguration = new ProjectionConfiguration(sourceName,
                                                                                                  projectionExpression.toString());
                    String workspaceName = projectionConfiguration.getWorkspaceName();

                    List<ProjectionConfiguration> projectionsInWorkspace = projectionsByWorkspace.get(workspaceName);
                    if (projectionsInWorkspace == null) {
                        projectionsInWorkspace = new ArrayList<ProjectionConfiguration>();
                        projectionsByWorkspace.put(workspaceName, projectionsInWorkspace);
                    }
                    projectionsInWorkspace.add(projectionConfiguration);
                }
            }
            return projectionsByWorkspace;
        }
    }

    /**
     * Object representation of a projection configuration within an external source
     */
    @Immutable
    public class ProjectionConfiguration {
        private final String workspaceName;
        private final String externalPath;
        private final String sourceName;
        private final String pathExpression;

        private String projectedPath;

        /**
         * Creates a new projection using a string expression
         *
         * @param sourceName the source name
         * @param pathExpression a {@code non-null} String
         */
        public ProjectionConfiguration( String sourceName,
                                        String pathExpression ) {
            Matcher expressionMatcher = PROJECTION_PATH_EXPRESSION_PATTERN.matcher(pathExpression);
            // should be validated by the repository schema
            if (expressionMatcher.matches()) {
                this.pathExpression = pathExpression;
                this.sourceName = sourceName;
                workspaceName = expressionMatcher.group(1);
                projectedPath = expressionMatcher.group(2);
                projectedPath = expressionMatcher.group(2);
                if (projectedPath.endsWith("/") && projectedPath.length() > 1) {
                    projectedPath = projectedPath.substring(0, projectedPath.length() - 1);
                }
                externalPath = expressionMatcher.group(7);
            } else {
                throw new IllegalArgumentException(JcrI18n.invalidProjectionExpression.text(pathExpression));
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

        /**
         * Returns the name of the source for which the projection is configured
         *
         * @return a {@code non-null} String
         */
        public String getSourceName() {
            return sourceName;
        }

        @Override
        public String toString() {
            return pathExpression;
        }
    }

    @Immutable
    public class Journaling {

        private final Document journalingDoc;

        protected Journaling( Document journalingDoc ) {
            this.journalingDoc = journalingDoc != null ? journalingDoc : EMPTY;
        }

        /**
         * Checks whether journaling is enabled or not, based on a journaling configuration having been provided.
         *
         * @return true if journaling is enabled, or false otherwise
         */
        public boolean isEnabled() {
            return this.journalingDoc != EMPTY;
        }

        /**
         * The location of the journal
         *
         * @return a {@code non-null} String
         */
        public String location() {
            return this.journalingDoc.getString(FieldName.JOURNAL_LOCATION, Default.JOURNAL_LOCATION);
        }

        /**
         * The maximum number of days journal entries should be kept on disk
         *
         * @return the number of days
         */
        public int maxDaysToKeepRecords() {
            return this.journalingDoc.getInteger(FieldName.MAX_DAYS_TO_KEEP_RECORDS, Default.MAX_DAYS_TO_KEEP_RECORDS);
        }

        /**
         * Whether asynchronous writes shoudl be enabled or not.
         *
         * @return true if anyschronos writes should be enabled.
         */
        public boolean asyncWritesEnabled() {
            return this.journalingDoc.getBoolean(FieldName.ASYNC_WRITES_ENABLED, Default.ASYNC_WRITES_ENABLED);
        }

        /**
         * Get the name of the thread pool that should be used for garbage collection journal entries.
         *
         * @return the thread pool name; never null
         */
        public String getThreadPoolName() {
            return journalingDoc.getString(FieldName.THREAD_POOL, Default.JOURNALING_POOL);
        }

        /**
         * Get the time that the first GC process should be run.
         *
         * @return the initial time; never null
         */
        public String getInitialTimeExpression() {
            return journalingDoc.getString(FieldName.INITIAL_TIME, Default.GARBAGE_COLLECTION_INITIAL_TIME);
        }

        /**
         * Get the GC interval in hours.
         *
         * @return the interval; never null
         */
        public int getIntervalInHours() {
            return journalingDoc.getInteger(FieldName.INTERVAL_IN_HOURS, Default.GARBAGE_COLLECTION_INTERVAL_IN_HOURS);
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
            warnUseOfDeprecatedFields(problems);
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
            getTextExtraction().validateTextExtractors(problems);
            validateIndexProviders(problems);
            getIndexes().validateIndexDefinitions(problems);
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
        RepositoryConfiguration updated = new RepositoryConfiguration(copy.unwrap(), this.getName());
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

    protected void warnUseOfDeprecatedFields( SimpleProblems problems ) {
        for (List<String> path : DEPRECATED_FIELDS) {
            Document nested = this.doc;
            Object value = null;
            for (String segment : path) {
                value = nested.get(segment);
                if (value == null) break;
                if (value instanceof Document) nested = (Document)value; // or array
            }
            if (value != null) {
                String p = StringUtil.join(path, ".");
                LOGGER.warn(JcrI18n.repositoryConfigurationContainsDeprecatedField, p, this.doc);
                problems.addWarning(JcrI18n.repositoryConfigurationContainsDeprecatedField, p, this.doc);
            }
        }
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
            Type instance = Reflection.getInstance(getClassname(), classLoader);
            if (Reflection.getField("name", instance.getClass()) != null) {
                // Always try to set the name (if there is such a field). The name may be set based upon
                // the value in the document, but a name field in documents is not required ...
                Reflection.setValue(instance, "name", getName());
            }
            setTypeFields(instance, getDocument());
            return instance;
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
                // Convert the field name from dash-separated to camel case ...
                fieldName = Inflector.getInstance().lowerCamelCase(fieldName, '-', '_');

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
                    Reflection.setValue(instance, fieldName, convertedFieldValue);
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

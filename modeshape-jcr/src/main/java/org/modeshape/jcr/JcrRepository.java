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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.UnmodifiableProperties;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JaasSecurityContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.federation.FederatedRepositorySource;
import org.modeshape.graph.connector.federation.Projection;
import org.modeshape.graph.connector.federation.ProjectionParser;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observable;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.GraphNamespaceRegistry;
import org.modeshape.graph.query.parse.QueryParsers;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.jcr.RepositoryQueryManager.PushDown;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.xpath.XPathQueryParser;

/**
 * Creates JCR {@link Session sessions} to an underlying repository (which may be a federated repository).
 * <p>
 * This JCR repository must be configured with the ability to connect to a repository via a supplied
 * {@link RepositoryConnectionFactory repository connection factory} and repository source name. An {@link ExecutionContext
 * execution context} must also be supplied to enable working with the underlying ModeShape graph implementation to which this JCR
 * implementation delegates.
 * </p>
 * <p>
 * If {@link Credentials credentials} are used to login, implementations <em>must</em> also implement one of the following
 * methods:
 * 
 * <pre>
 * public {@link AccessControlContext} getAccessControlContext();
 * public {@link LoginContext} getLoginContext();
 * </pre>
 * 
 * Note, {@link Session#getAttributeNames() attributes} on credentials are not supported. JCR {@link SimpleCredentials} are also
 * not supported.
 * </p>
 */
@ThreadSafe
public class JcrRepository implements Repository {

    /**
     * A flag that controls whether the repository uses a shared repository (or workspace) for the "/jcr:system" content in all of
     * the workspaces. In production, this needs to be "true" for proper JCR functionality, but in some debugging cases it can be
     * set to false to simplify the architecture by removing the federated connector layer.
     * <p>
     * This should be changed to 'false' only in advanced situations, and never for production. Note that this also disables query
     * execution.
     * </p>
     */
    static final boolean WORKSPACES_SHARE_SYSTEM_BRANCH = true;

    /**
     * The user name for anonymous sessions
     * 
     * @see Option#ANONYMOUS_USER_ROLES
     */
    static final String ANONYMOUS_USER_NAME = "<anonymous>";
    private static final Logger LOGGER = Logger.getLogger(JcrRepository.class);
    private static Properties bundleProperties = null;

    /**
     * The available options for the {@code JcrRepository}.
     */
    public enum Option {

        /**
         * Flag that defines whether or not the node types should be exposed as content under the "{@code
         * /jcr:system/jcr:nodeTypes}" node. Value is either "<code>true</code>" or "<code>false</code>" (default).
         * 
         * @see DefaultOption#PROJECT_NODE_TYPES
         */
        PROJECT_NODE_TYPES,
        /**
         * The {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name} that specifies which
         * login modules should be used to validate credentials.
         */
        JAAS_LOGIN_CONFIG_NAME,

        /**
         * The name of the source (and optionally the workspace in the source) where the "/jcr:system" branch should be stored.
         * The format is "<code>name of workspace@name of source</code>", or simply "<code>name of source</code>" if the default
         * workspace is to be used. If this option is not used, a transient in-memory source will be used.
         * <p>
         * Note that all leading and trailing whitespace is removed for both the source name and workspace name. Thus, a value of
         * "<code>@</code>" implies a zero-length workspace name and zero-length source name.
         * </p>
         * <p>
         * Also, any use of the '@' character in source and workspace names must be escaped with a preceding backslash.
         * </p>
         */
        SYSTEM_SOURCE_NAME,

        /**
         * The depth of the subgraphs that should be loaded the connectors. The default value is 1.
         */
        READ_DEPTH,

        /**
         * A comma-delimited list of default roles provided for anonymous access. A null or empty value for this option means that
         * anonymous access is disabled.
         */
        ANONYMOUS_USER_ROLES,

        /**
         * The query system represents node types as tables that can be queried, but there are two ways to define the columns for
         * each of those tables. One approach is that each table only has columns representing the (single-valued) property
         * definitions explicitly defined by the node type. The other approach also adds columns for each of the (single-valued)
         * property definitions inherited by the node type from all of the {@link javax.jcr.nodetype.NodeType#getSupertypes()}.
         * <p>
         * The default value is 'true'.
         * </p>
         */
        TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES,

        /**
         * A boolean flag that specifies whether this repository is expected to execute searches and queries. If client
         * applications will never perform searches or queries, then maintaining the query indexes is an unncessary overhead, and
         * can be disabled. Note that this is merely a hint, and that searches and queries might still work when this is set to
         * 'false'.
         * <p>
         * The default is 'true', meaning that clients can execute searches and queries.
         * </p>
         */
        QUERY_EXECUTION_ENABLED,

        /**
         * The system may maintain a set of indexes that improve the performance of searching and querying the content. These size
         * of these indexes depend upon the size of the content being stored, and thus may consume a significant amount of space.
         * This option defines a location on the file system where this repository may (if needed) store indexes so they don't
         * consume large amounts of memory.
         * <p>
         * If specified, the value must be a valid path to a writable directory on the file system. If the path specifies a
         * non-existant location, the repository may attempt to create the missing directories. The path may be absolute or
         * relative to the location where this VM was started. If the specified location is not a readable and writable directory
         * (or cannot be created as such), then this will generate an exception when the repository is created.
         * </p>
         * <p>
         * The default value is null, meaning the search indexes may not be stored on the local file system and, if needed, will
         * be stored within memory.
         * </p>
         */
        QUERY_INDEX_DIRECTORY,

        /**
         * A boolean flag that specifies whether updates to the indexes (if used) should be made synchronously, meaning that a
         * call to {@link Session#save()} will not return until the search indexes have been completely updated. The benefit of
         * synchronous updates is that a search or query performed immediately after a <code>save()</code> will operate upon
         * content that was just changed. The downside is that the <code>save()</code> operation will take longer.
         * <p>
         * With asynchronous updates, however, the only work done during a <code>save()</code> invocation is that required to
         * persist the changes in the underlying repository source, while changes to the search indexes are made in a different
         * thread that may not run immediately. In this case, there may be an indeterminate lag before searching or querying after
         * a <code>save()</code> will operate upon the changed content.
         * </p>
         * <p>
         * The default is value 'false', meaning the updates are performed <i>asynchronously</i>.
         * </p>
         */
        QUERY_INDEXES_UPDATED_SYNCHRONOUSLY,

        /**
         * A boolean flag that specifies whether referential integrity checks should be performed upon {@link Session#save()}. By
         * default, this option is enabled, meaning that referential integrity checks <i>are</i> performed to ensure that nodes
         * referenced by other nodes cannot be removed.
         * <p>
         * If no {@link PropertyType#REFERENCE} properties are used within your content, these referential integrity checks will
         * never find referring nodes. In these cases, you may be able to improve performance by skipping these checks.
         * </p>
         * <p>
         * The default value is 'true', meaning that these checks are performed.
         * </p>
         */
        PERFORM_REFERENTIAL_INTEGRITY_CHECKS;

        /**
         * Determine the option given the option name. This does more than {@link Option#valueOf(String)}, since this method first
         * tries to match the supplied string to the option's {@link Option#name() name}, then the uppercase version of the
         * supplied string to the option's name, and finally if the supplied string is a camel-case version of the name (e.g.,
         * "projectNodeTypes").
         * 
         * @param option the string version of the option's name
         * @return the matching Option instance, or null if an option could not be matched using the supplied value
         */
        public static Option findOption( String option ) {
            if (option == null) return null;
            try {
                return Option.valueOf(option);
            } catch (IllegalArgumentException e) {
                // Try an uppercased version ...
                try {
                    return Option.valueOf(option.toUpperCase());
                } catch (IllegalArgumentException e2) {
                    // Try a camel-case version ...
                    String underscored = Inflector.getInstance().underscore(option, '_');
                    if (underscored == null) {
                        throw e2;
                    }
                    return Option.valueOf(underscored.toUpperCase());
                }
            }
        }
    }

    /**
     * The default values for each of the {@link Option}.
     */
    public static class DefaultOption {
        /**
         * The default value for the {@link Option#PROJECT_NODE_TYPES} option is {@value} .
         */
        public static final String PROJECT_NODE_TYPES = Boolean.FALSE.toString();

        /**
         * The default value for the {@link Option#JAAS_LOGIN_CONFIG_NAME} option is {@value} .
         */
        public static final String JAAS_LOGIN_CONFIG_NAME = "modeshape-jcr";

        /**
         * The default value for the {@link Option#READ_DEPTH} option is {@value} .
         */
        public static final String READ_DEPTH = "1";

        /**
         * The default value for the {@link Option#ANONYMOUS_USER_ROLES} option is {@value} .
         */
        public static final String ANONYMOUS_USER_ROLES = ModeShapeRoles.ADMIN;

        /**
         * The default value for the {@link Option#PROJECT_NODE_TYPES} option is {@value} .
         */
        public static final String TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#QUERY_EXECUTION_ENABLED} option is {@value} .
         */
        public static final String QUERY_EXECUTION_ENABLED = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#QUERY_INDEXES_UPDATED_SYNCHRONOUSLY} option is {@value} .
         */
        public static final String QUERY_INDEXES_UPDATED_SYNCHRONOUSLY = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#QUERY_INDEX_DIRECTORY} option is {@value} .
         */
        public static final String QUERY_INDEX_DIRECTORY = null;

        /**
         * The default value for the {@link Option#QUERY_INDEX_DIRECTORY} option is {@value} .
         */
        public static final String PERFORM_REFERENTIAL_INTEGRITY_CHECKS = Boolean.TRUE.toString();

    }

    /**
     * The static unmodifiable map of default options, which are initialized in the static initializer.
     */
    protected static final Map<Option, String> DEFAULT_OPTIONS;

    /**
     * The set of supported query language string constants.
     * 
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     * @see javax.jcr.query.QueryManager#createQuery(String, String)
     */
    public static final class QueryLanguage {
        /**
         * The standard JCR 1.0 XPath query language.
         */
        public static final String XPATH = Query.XPATH;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL query language defined by the JCR 1.0.1
         * specification.
         */
        public static final String JCR_SQL = Query.SQL;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL2 query language defined by the JCR 2.0
         * specification.
         */
        public static final String JCR_SQL2 = JcrSql2QueryParser.LANGUAGE;
        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL2 query language defined by the JCR 2.0
         * specification.
         * 
         * @deprecated use {@link #JCR_SQL2} instead
         */
        @Deprecated
        public static final String SQL = JCR_SQL2;
        /**
         * The full-text search language defined as part of the abstract query model, in Section 6.7.19 of the JCR 2.0
         * specification.
         */
        public static final String SEARCH = FullTextSearchParser.LANGUAGE;
    }

    static {
        // Initialize the unmodifiable map of default options ...
        EnumMap<Option, String> defaults = new EnumMap<Option, String>(Option.class);
        defaults.put(Option.PROJECT_NODE_TYPES, DefaultOption.PROJECT_NODE_TYPES);
        defaults.put(Option.JAAS_LOGIN_CONFIG_NAME, DefaultOption.JAAS_LOGIN_CONFIG_NAME);
        defaults.put(Option.READ_DEPTH, DefaultOption.READ_DEPTH);
        defaults.put(Option.ANONYMOUS_USER_ROLES, DefaultOption.ANONYMOUS_USER_ROLES);
        defaults.put(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES,
                     DefaultOption.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES);
        defaults.put(Option.QUERY_EXECUTION_ENABLED, DefaultOption.QUERY_EXECUTION_ENABLED);
        defaults.put(Option.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY, DefaultOption.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY);
        defaults.put(Option.QUERY_INDEX_DIRECTORY, DefaultOption.QUERY_INDEX_DIRECTORY);
        defaults.put(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS, DefaultOption.PERFORM_REFERENTIAL_INTEGRITY_CHECKS);
        DEFAULT_OPTIONS = Collections.<Option, String>unmodifiableMap(defaults);
    }

    private final String sourceName;
    private final Map<String, Object> descriptors;
    private final ExecutionContext executionContext;
    private final RepositoryConnectionFactory connectionFactory;
    private final RepositoryNodeTypeManager repositoryTypeManager;
    @GuardedBy( "lockManagersLock" )
    private final ConcurrentMap<String, WorkspaceLockManager> lockManagers;
    private final Path locksPath;
    private final Map<Option, String> options;
    private final String systemSourceName;
    private final String systemWorkspaceName;
    private final Projection systemSourceProjection;
    private final FederatedRepositorySource federatedSource;
    private final NamespaceRegistry persistentRegistry;
    private final RepositoryObservationManager repositoryObservationManager;
    private final SecurityContext anonymousUserContext;
    private final QueryParsers queryParsers;
    // Until the federated connector supports queries, we have to use a search engine ...
    private final RepositoryQueryManager queryManager;

    // package-scoped to facilitate testing
    final WeakHashMap<JcrSession, Object> activeSessions = new WeakHashMap<JcrSession, Object>();

    /**
     * Creates a JCR repository that uses the supplied {@link RepositoryConnectionFactory repository connection factory} to
     * establish {@link Session sessions} to the underlying repository source upon {@link #login() login}.
     * 
     * @param executionContext the execution context in which this repository is to operate
     * @param connectionFactory the factory for repository connections
     * @param repositorySourceName the name of the repository source (in the connection factory) that should be used
     * @param repositoryObservable the repository library observable associated with this repository (never <code>null</code>)
     * @param repositorySourceCapabilities the capabilities of the repository source; may be null if the capabilities are not
     *        known
     * @param descriptors the {@link #getDescriptorKeys() descriptors} for this repository; may be <code>null</code>.
     * @param options the optional {@link Option settings} for this repository; may be null
     * @throws RepositoryException if there is a problem setting up this repository
     * @throws IllegalArgumentException If <code>executionContext</code>, <code>connectionFactory</code>,
     *         <code>repositorySourceName</code>, or <code>repositoryObservable</code> is <code>null</code>.
     */
    JcrRepository( ExecutionContext executionContext,
                   RepositoryConnectionFactory connectionFactory,
                   String repositorySourceName,
                   Observable repositoryObservable,
                   RepositorySourceCapabilities repositorySourceCapabilities,
                   Map<String, String> descriptors,
                   Map<Option, String> options ) throws RepositoryException {
        CheckArg.isNotNull(executionContext, "executionContext");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(repositorySourceName, "repositorySourceName");
        CheckArg.isNotNull(repositoryObservable, "repositoryObservable");

        // Initialize required JCR descriptors.
        this.descriptors = initializeDescriptors(executionContext.getValueFactories(), descriptors);

        // Set up the options ...
        if (options == null) {
            this.options = DEFAULT_OPTIONS;
        } else {
            // Initialize with defaults, then add supplied options ...
            EnumMap<Option, String> localOptions = new EnumMap<Option, String>(DEFAULT_OPTIONS);
            localOptions.putAll(options);
            this.options = Collections.unmodifiableMap(localOptions);
        }

        // Initialize the observer, which receives events from all repository sources
        this.repositoryObservationManager = new RepositoryObservationManager(repositoryObservable);

        // Set up the system source ...
        String systemSourceNameValue = this.options.get(Option.SYSTEM_SOURCE_NAME);
        String systemSourceName = null;
        String systemWorkspaceName = null;
        RepositoryConnectionFactory connectionFactoryWithSystem = connectionFactory;
        if (systemSourceNameValue != null) {
            // Find an existing source with the given name containing the named workspace ...
            try {
                SourceWorkspacePair pair = new SourceWorkspacePair(systemSourceNameValue);
                // Look for a source with the given name ...
                RepositoryConnection conn = connectionFactory.createConnection(pair.getSourceName());
                if (conn != null) {
                    // We found a source that we can use for the system ...
                    systemSourceName = pair.getSourceName();
                    if (pair.getWorkspaceName() != null) {
                        // There should be the named workspace ...
                        Graph temp = Graph.create(conn, executionContext);
                        temp.useWorkspace(pair.getWorkspaceName());
                        // found it ...
                        systemWorkspaceName = pair.getWorkspaceName();
                    }
                } else {
                    I18n msg = JcrI18n.systemSourceNameOptionValueDoesNotReferenceExistingSource;
                    LOGGER.warn(msg, systemSourceNameValue, systemSourceName);
                }
            } catch (InvalidWorkspaceException e) {
                // Bad workspace name ...
                systemSourceName = null;
                I18n msg = JcrI18n.systemSourceNameOptionValueDoesNotReferenceValidWorkspace;
                LOGGER.warn(msg, systemSourceNameValue, systemSourceName);
            } catch (IllegalArgumentException e) {
                // Invalid format ...
                systemSourceName = null;
                I18n msg = JcrI18n.systemSourceNameOptionValueIsNotFormattedCorrectly;
                LOGGER.warn(msg, systemSourceNameValue);
            }
        }
        if (systemSourceName == null) {
            // Create the in-memory repository source that we'll use for the "/jcr:system" branch in this repository.
            // All workspaces will be set up with a federation connector that projects this system repository into
            // "/jcr:system", and all other content is projected to the repositories actual source (and workspace).
            // (The federation connector refers to this configuration as an "offset mirror".)
            systemWorkspaceName = "jcr:system";
            systemSourceName = "jcr:system source";
            InMemoryRepositorySource transientSystemSource = new InMemoryRepositorySource();
            transientSystemSource.setName(systemSourceName);
            transientSystemSource.setDefaultWorkspaceName(systemWorkspaceName);
            connectionFactoryWithSystem = new DelegatingConnectionFactory(connectionFactory, transientSystemSource);
        }

        // Set up the query parsers, which we have to have even though queries might be disabled ...
        this.queryParsers = new QueryParsers(new JcrSql2QueryParser(), new XPathQueryParser(), new FullTextSearchParser(),
                                             new JcrSqlQueryParser());
        assert this.queryParsers.getParserFor(Query.XPATH) != null;

        this.systemWorkspaceName = systemWorkspaceName;
        this.systemSourceName = systemSourceName;
        this.connectionFactory = connectionFactoryWithSystem;
        assert this.systemSourceName != null;
        assert this.connectionFactory != null;
        this.sourceName = repositorySourceName;

        // Set up the "/jcr:system" branch ...
        Graph systemGraph = Graph.create(this.systemSourceName, this.connectionFactory, executionContext);
        systemGraph.useWorkspace(systemWorkspaceName);
        initializeSystemContent(systemGraph);

        // Create the namespace registry and corresponding execution context.
        // Note that this persistent registry has direct access to the system workspace.
        Name uriProperty = ModeShapeLexicon.NAMESPACE_URI;
        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        Path systemPath = pathFactory.create(JcrLexicon.SYSTEM);
        Path namespacesPath = pathFactory.create(systemPath, ModeShapeLexicon.NAMESPACES);
        PropertyFactory propertyFactory = executionContext.getPropertyFactory();
        Property namespaceType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.NAMESPACE);

        // Now create the registry implementation ...
        this.persistentRegistry = new GraphNamespaceRegistry(systemGraph, namespacesPath, uriProperty, namespaceType);
        this.executionContext = executionContext.with(persistentRegistry);

        // Add the built-ins, ensuring we overwrite any badly-initialized values ...
        for (Map.Entry<String, String> builtIn : JcrNamespaceRegistry.STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX.entrySet()) {
            this.persistentRegistry.register(builtIn.getKey(), builtIn.getValue());
        }

        // Set up the repository type manager ...
        try {
            boolean includeInheritedProperties = Boolean.valueOf(this.options.get(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES));
            // this.repositoryTypeManager = new RepositoryNodeTypeManager(this, includeInheritedProperties);
            this.repositoryTypeManager = new RepositoryNodeTypeManager(this, includeInheritedProperties);
            this.repositoryTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {
                "/org/modeshape/jcr/jsr_170_builtins.cnd", "/org/modeshape/jcr/dna_builtins.cnd"}));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }
        if (WORKSPACES_SHARE_SYSTEM_BRANCH) {
            if (Boolean.valueOf(this.options.get(Option.PROJECT_NODE_TYPES))) {
                // Note that the node types are written directly to the system workspace.
                Path parentOfTypeNodes = pathFactory.create(systemPath, JcrLexicon.NODE_TYPES);
                this.repositoryTypeManager.projectOnto(systemGraph, parentOfTypeNodes);
            }

            // Create the projection for the system repository ...
            ProjectionParser projectionParser = ProjectionParser.getInstance();
            String rule = "/jcr:system => /jcr:system";
            Projection.Rule[] systemProjectionRules = projectionParser.rulesFromString(this.executionContext, rule);
            this.systemSourceProjection = new Projection(systemSourceName, systemWorkspaceName, true, systemProjectionRules);

            // Define the federated repository source. Use the same name as the repository, since this federated source
            // will not be in the connection factory ...
            this.federatedSource = new FederatedRepositorySource();
            this.federatedSource.setName("JCR " + repositorySourceName);
            this.federatedSource.initialize(new FederatedRepositoryContext(this.connectionFactory));
        } else {
            this.federatedSource = null;
            this.systemSourceProjection = null;
        }

        this.lockManagers = new ConcurrentHashMap<String, WorkspaceLockManager>();
        this.locksPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);

        // If the repository is to support searching ...
        if (Boolean.valueOf(this.options.get(Option.QUERY_EXECUTION_ENABLED)) && WORKSPACES_SHARE_SYSTEM_BRANCH) {
            // Determine whether the federated source and original source support queries and searches ...
            RepositorySourceCapabilities fedCapabilities = federatedSource != null ? federatedSource.getCapabilities() : null;
            final boolean canQuerySource = repositorySourceCapabilities != null
                                           && repositorySourceCapabilities.supportsSearches()
                                           && repositorySourceCapabilities.supportsQueries();
            final boolean canQueryFederated = fedCapabilities != null && fedCapabilities.supportsSearches()
                                              && fedCapabilities.supportsQueries();

            // We can query the federated source if it supports queries and searches
            // AND the original source supports queries and searches ...
            if (canQuerySource && canQueryFederated) {
                this.queryManager = new PushDown(this.sourceName, this.executionContext, connectionFactory);
            } else {
                // Otherwise create a repository query manager that maintains its own search engine ...
                String indexDirectory = this.options.get(Option.QUERY_INDEX_DIRECTORY);
                boolean updateIndexesSynchronously = Boolean.valueOf(this.options.get(Option.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY));
                this.queryManager = new RepositoryQueryManager.SelfContained(this.executionContext, this.sourceName,
                                                                             connectionFactory, repositoryObservable,
                                                                             repositoryTypeManager, indexDirectory,
                                                                             updateIndexesSynchronously);
            }
        } else {
            this.queryManager = new RepositoryQueryManager.Disabled(this.sourceName);
        }

        /*
         * Set up the anonymous role, if appropriate
         */
        SecurityContext anonymousUserContext = null;
        String rawAnonRoles = this.options.get(Option.ANONYMOUS_USER_ROLES);
        if (rawAnonRoles != null) {
            final Set<String> roles = new HashSet<String>();
            for (String role : rawAnonRoles.split("\\s*,\\s*")) {
                roles.add(role);
            }
            if (roles.size() > 0) {
                anonymousUserContext = new SecurityContext() {

                    public String getUserName() {
                        return ANONYMOUS_USER_NAME;
                    }

                    public boolean hasRole( String roleName ) {
                        return roles.contains(roleName);
                    }

                    public void logout() {
                    }

                };
            }
        }

        this.anonymousUserContext = anonymousUserContext;
    }

    protected void initializeSystemContent( Graph systemGraph ) {
        // Make sure the "/jcr:system" node exists ...
        ExecutionContext context = systemGraph.getContext();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path systemPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM);
        Property systemPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.SYSTEM);
        systemGraph.create(systemPath, systemPrimaryType).ifAbsent().and();

        // Make sure the required jcr:versionStorage node exists...
        Path versionPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE);
        Property versionPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE,
                                                                          ModeShapeLexicon.VERSION_STORAGE);
        systemGraph.create(versionPath, versionPrimaryType).ifAbsent().and();

        // Right now, the other nodes will be created as needed
    }

    Graph createWorkspaceGraph( String workspaceName,
                                ExecutionContext workspaceContext ) {
        Graph graph = null;
        if (WORKSPACES_SHARE_SYSTEM_BRANCH) {
            // Connect via the federated source ...
            assert this.federatedSource != null;
            graph = Graph.create(this.federatedSource, workspaceContext);
        } else {
            // Otherwise, just create a graph directly to the connection factory ...
            graph = Graph.create(this.sourceName, this.connectionFactory, workspaceContext);
        }
        graph.useWorkspace(workspaceName);
        return graph;
    }

    Graph createSystemGraph( ExecutionContext sessionContext ) {
        assert this.systemSourceName != null;
        assert this.connectionFactory != null;
        assert sessionContext != null;
        // The default workspace should be the system workspace ...
        Graph result = Graph.create(this.systemSourceName, this.connectionFactory, sessionContext);
        if (this.systemWorkspaceName != null) {
            result.useWorkspace(systemWorkspaceName);
        }
        return result;
    }

    QueryParsers queryParsers() {
        return queryParsers;
    }

    /**
     * Get the query manager for this repository.
     * 
     * @return the query manager; never null
     */
    RepositoryQueryManager queryManager() {
        return queryManager;
    }

    /**
     * Returns the repository-level node type manager
     * 
     * @return the repository-level node type manager
     */
    RepositoryNodeTypeManager getRepositoryTypeManager() {
        return repositoryTypeManager;
    }

    /**
     * Get the options as configured for this repository.
     * 
     * @return the unmodifiable options; never null
     */
    public Map<Option, String> getOptions() {
        return options;
    }

    /**
     * Get the name of the repository source that this repository is using.
     * 
     * @return the name of the RepositorySource
     */
    String getRepositorySourceName() {
        return sourceName;
    }

    String getSystemSourceName() {
        return systemSourceName;
    }

    /**
     * Get the name of the source that we want to observe.
     * 
     * @return the name of the source that should be observed; never null
     */
    String getObservableSourceName() {
        return WORKSPACES_SHARE_SYSTEM_BRANCH ? federatedSource.getName() : sourceName;
    }

    /**
     * @return executionContext
     */
    ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @return persistentRegistry
     */
    NamespaceRegistry getPersistentRegistry() {
        return persistentRegistry;
    }

    /**
     * The observer to which all source events are sent.
     * 
     * @return the current observer, or null if there is no observer
     */
    Observer getObserver() {
        return this.repositoryObservationManager;
    }

    /**
     * The repository observable that listeners can be registered with.
     * 
     * @return the repository observable (never <code>null</code>)
     */
    Observable getRepositoryObservable() {
        return this.repositoryObservationManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>key</code> is <code>null</code>.
     * @see Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor( String key ) {
        if (!isSingleValueDescriptor(key)) return null;

        JcrValue value = (JcrValue)descriptors.get(key);
        try {
            return value.getString();
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if {@code key} is null or empty
     * @see Repository#getDescriptorValue(String)
     */
    public JcrValue getDescriptorValue( String key ) {
        if (!isSingleValueDescriptor(key)) return null;
        return (JcrValue)descriptors.get(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if {@code key} is null or empty
     * @see Repository#getDescriptorValues(String)
     */
    public JcrValue[] getDescriptorValues( String key ) {
        Object value = descriptors.get(key);
        if (value instanceof JcrValue[]) {
            return (JcrValue[])value;
        }
        if (value instanceof JcrValue) {
            return new JcrValue[] {(JcrValue)value};
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if {@code key} is null or empty
     * @see Repository#isSingleValueDescriptor(String)
     */
    public boolean isSingleValueDescriptor( String key ) {
        CheckArg.isNotEmpty(key, "key");
        return descriptors.get(key) instanceof JcrValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if {@code key} is null or empty
     * @see Repository#isStandardDescriptor(String)
     */
    public boolean isStandardDescriptor( String key ) {
        return STANDARD_DESCRIPTORS.contains(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return descriptors.keySet().toArray(new String[descriptors.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login()
     */
    public synchronized Session login() throws RepositoryException {
        return login(null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public synchronized Session login( Credentials credentials ) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public synchronized Session login( String workspaceName ) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>credentials</code> is not <code>null</code> but:
     *         <ul>
     *         <li>provides neither a <code>getLoginContext()</code> nor a <code>getAccessControlContext()</code> method and is
     *         not an instance of {@code SimpleCredentials}.</li>
     *         <li>provides a <code>getLoginContext()</code> method that doesn't return a {@link LoginContext}.
     *         <li>provides a <code>getLoginContext()</code> method that returns a <code>
     *         null</code> {@link LoginContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that doesn't return an {@link AccessControlContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that returns a <code>null</code> {@link AccessControlContext}.
     *         </ul>
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    public synchronized Session login( Credentials credentials,
                                       String workspaceName ) throws RepositoryException {
        // Ensure credentials are either null or provide a JAAS method
        Map<String, Object> sessionAttributes = new HashMap<String, Object>();
        ExecutionContext execContext = null;
        if (credentials == null) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject != null) {
                execContext = executionContext.with(new JaasSecurityContext(subject));
            }
            // Well. There's no JAAS subject. Try using an anonymous user (if that's enabled).
            else if (anonymousUserContext != null) {
                execContext = executionContext.with(this.anonymousUserContext);
            } else {
                throw new javax.jcr.LoginException(JcrI18n.mustBeInPrivilegedAction.text());
            }
        } else {
            try {
                if (credentials instanceof SimpleCredentials) {
                    SimpleCredentials simple = (SimpleCredentials)credentials;
                    execContext = executionContext.with(new JaasSecurityContext(options.get(Option.JAAS_LOGIN_CONFIG_NAME),
                                                                                simple.getUserID(), simple.getPassword()));
                    for (String attributeName : simple.getAttributeNames()) {
                        Object attributeValue = simple.getAttribute(attributeName);
                        sessionAttributes.put(attributeName, attributeValue);
                    }

                } else if (credentials instanceof SecurityContextCredentials) {
                    execContext = executionContext.with(((SecurityContextCredentials)credentials).getSecurityContext());
                } else {
                    // Check if credentials provide a login context
                    try {
                        Method method = credentials.getClass().getMethod("getLoginContext");
                        if (method.getReturnType() != LoginContext.class) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                        }
                        LoginContext loginContext = (LoginContext)method.invoke(credentials);
                        if (loginContext == null) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                        }
                        execContext = executionContext.with(new JaasSecurityContext(loginContext));
                    } catch (NoSuchMethodException error) {
                        throw new IllegalArgumentException(JcrI18n.credentialsMustProvideJaasMethod.text(credentials.getClass()),
                                                           error);
                    }
                }
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new javax.jcr.LoginException(error);
            }
        }
        return sessionForContext(execContext, workspaceName, sessionAttributes);
    }

    /**
     * Creates a new {@link JcrSession session} based on the given {@link ExecutionContext context} and its associated security
     * context.
     * 
     * @param execContext the execution context to use for the new session; may not be null and must have a non-null
     *        {@link ExecutionContext#getSecurityContext() security context}
     * @param workspaceName the name of the workspace to connect to; null indicates that the default workspace should be used
     * @param sessionAttributes the session attributes for this session; may not be null
     * @return a valid session for the user to access the repository
     * @throws RepositoryException if an error occurs creating the session
     */
    JcrSession sessionForContext( ExecutionContext execContext,
                                  String workspaceName,
                                  Map<String, Object> sessionAttributes ) throws RepositoryException {
        CheckArg.isNotNull(execContext.getSecurityContext(), "execContext.securityContext");
        // Ensure valid workspace name by talking directly to the source ...
        boolean isDefault = false;
        Graph graph = Graph.create(sourceName, connectionFactory, executionContext);
        if (workspaceName == null) {
            try {
                // Get the correct workspace name given the desired workspace name (which may be null) ...
                workspaceName = graph.getCurrentWorkspace().getName();
            } catch (RepositorySourceException e) {
                throw new RepositoryException(JcrI18n.errorObtainingDefaultWorkspaceName.text(sourceName, e.getMessage()), e);
            }
            isDefault = true;
        } else {
            // There is a non-null workspace name ...
            try {
                // Verify that the workspace exists (or can be created) ...
                Set<String> workspaces = graph.getWorkspaces();
                if (!workspaces.contains(workspaceName)) {
                    if (WORKSPACES_SHARE_SYSTEM_BRANCH) {
                        // Make sure there isn't a federated workspace ...
                        this.federatedSource.removeWorkspace(workspaceName);
                    }
                    // Per JCR 1.0 6.1.1, if the workspaceName is not recognized, a NoSuchWorkspaceException is thrown
                    throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName));
                }

                graph.useWorkspace(workspaceName);
            } catch (InvalidWorkspaceException e) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName), e);
            } catch (RepositorySourceException e) {
                String msg = JcrI18n.errorVerifyingWorkspaceName.text(sourceName, workspaceName, e.getMessage());
                throw new NoSuchWorkspaceException(msg, e);
            }
        }

        if (WORKSPACES_SHARE_SYSTEM_BRANCH) {
            assert this.federatedSource != null;
            assert this.systemSourceProjection != null;
            synchronized (this.federatedSource) {
                if (!this.federatedSource.hasWorkspace(workspaceName)) {
                    // Add the workspace to the federated source ...
                    ProjectionParser projectionParser = ProjectionParser.getInstance();
                    Projection.Rule[] mirrorRules = projectionParser.rulesFromString(this.executionContext, "/ => /");
                    List<Projection> projections = new ArrayList<Projection>(2);
                    projections.add(new Projection(sourceName, workspaceName, false, mirrorRules));
                    projections.add(this.systemSourceProjection);
                    this.federatedSource.addWorkspace(workspaceName, projections, isDefault);
                }
            }
        } else {
            // We're not sharing a '/jcr:system' branch, so we need to make sure there is one in the source.
            // Note that this doesn't always work with some connectors (e.g., the FileSystem or SVN connectors)
            // that don't allow arbitrary nodes.
            try {
                initializeSystemContent(graph);
            } catch (RepositorySourceException e) {
                Logger.getLogger(getClass())
                      .debug(e,
                             "Workspaces do not share a common /jcr:system branch, but the connector was unable to create one in this session. Errors may result.");
            }
        }

        // Create the workspace, which will create its own session ...
        sessionAttributes = Collections.unmodifiableMap(sessionAttributes);
        JcrWorkspace workspace = new JcrWorkspace(this, workspaceName, execContext, sessionAttributes);

        JcrSession session = (JcrSession)workspace.getSession();

        // Need to make sure that the user has access to this session
        try {
            session.checkPermission(workspaceName, null, ModeShapePermissions.READ);
        } catch (AccessControlException ace) {
            throw new LoginException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName), ace);
        }

        synchronized (this.activeSessions) {
            activeSessions.put(session, null);
        }

        return session;
    }

    void close() {
        if (this.federatedSource != null) {
            this.federatedSource.close();
        }

        this.repositoryObservationManager.shutdown();
    }

    /**
     * @return a list of all workspace names, without regard to the access permissions of any particular user
     */
    Set<String> workspaceNames() {
        return Graph.create(sourceName, connectionFactory, executionContext).getWorkspaces();
    }

    /**
     * Returns the lock manager for the named workspace (if one already exists) or creates a new lock manager and returns it. This
     * method is thread-safe.
     * 
     * @param workspaceName the name of the workspace for which the lock manager should be returned
     * @return the lock manager for the workspace; never null
     */
    WorkspaceLockManager getLockManager( String workspaceName ) {
        WorkspaceLockManager lockManager = lockManagers.get(workspaceName);
        if (lockManager != null) return lockManager;

        lockManager = new WorkspaceLockManager(executionContext, this, workspaceName, locksPath);
        WorkspaceLockManager newLockManager = lockManagers.putIfAbsent(workspaceName, lockManager);

        if (newLockManager != null) return newLockManager;
        return lockManager;
    }

    /**
     * Marks the given session as inactive (by removing it from the {@link #activeSessions active sessions map}.
     * 
     * @param session the session to be marked as inactive
     */
    void sessionLoggedOut( JcrSession session ) {
        synchronized (this.activeSessions) {
            this.activeSessions.remove(session);
        }
    }

    /**
     * Returns the set of active sessions in this repository
     * 
     * @return the set of active sessions in this repository
     */
    Set<JcrSession> activeSessions() {
        Set<JcrSession> activeSessions;

        synchronized (this.activeSessions) {
            activeSessions = new HashSet<JcrSession>(this.activeSessions.keySet());
        }
        // There can and will be elements in this set that are no longer live but haven't yet been gc'ed.
        // Filter those out
        for (Iterator<JcrSession> iter = activeSessions.iterator(); iter.hasNext();) {
            JcrSession session = iter.next();
            if (session != null && !session.isLive()) {
                iter.remove();
            }
        }

        return activeSessions;
    }

    /**
     * Iterates through the list of session-scoped locks in this repository, deleting any session-scoped locks that were created
     * by a session that is no longer active.
     */
    void cleanUpLocks() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(JcrI18n.cleaningUpLocks.text());
        }

        Set<JcrSession> activeSessions = activeSessions();
        Set<String> activeSessionIds = new HashSet<String>(activeSessions.size());

        for (JcrSession activeSession : activeSessions) {
            activeSessionIds.add(activeSession.sessionId());
        }

        Graph systemGraph = createSystemGraph(executionContext);
        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        ValueFactory<Boolean> booleanFactory = executionContext.getValueFactories().getBooleanFactory();
        ValueFactory<String> stringFactory = executionContext.getValueFactories().getStringFactory();

        DateTimeFactory dateFactory = executionContext.getValueFactories().getDateFactory();
        DateTime now = dateFactory.create();
        DateTime newExpirationDate = now.plusMillis(JcrEngine.LOCK_EXTENSION_INTERVAL_IN_MILLIS);

        Path locksPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);

        Subgraph locksGraph = null;
        try {
            locksGraph = systemGraph.getSubgraphOfDepth(2).at(locksPath);
        } catch (PathNotFoundException pnfe) {
            // It's possible for this to run before the dna:locks child node gets added to the /jcr:system node.
            return;
        }

        for (Location lockLocation : locksGraph.getRoot().getChildren()) {
            Node lockNode = locksGraph.getNode(lockLocation);

            Boolean isSessionScoped = booleanFactory.create(lockNode.getProperty(ModeShapeLexicon.IS_SESSION_SCOPED)
                                                                    .getFirstValue());

            if (!isSessionScoped) continue;
            String lockingSession = stringFactory.create(lockNode.getProperty(ModeShapeLexicon.LOCKING_SESSION).getFirstValue());

            // Extend locks held by active sessions
            if (activeSessionIds.contains(lockingSession)) {
                systemGraph.set(ModeShapeLexicon.EXPIRATION_DATE).on(lockLocation).to(newExpirationDate);
            } else {
                DateTime expirationDate = dateFactory.create(lockNode.getProperty(ModeShapeLexicon.EXPIRATION_DATE)
                                                                     .getFirstValue());
                // Destroy expired locks (if it was still held by an active session, it would have been extended by now)
                if (expirationDate.isBefore(now)) {
                    String workspaceName = stringFactory.create(lockNode.getProperty(ModeShapeLexicon.WORKSPACE).getFirstValue());
                    WorkspaceLockManager lockManager = lockManagers.get(workspaceName);
                    lockManager.unlock(executionContext, lockManager.createLock(lockNode));
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(JcrI18n.cleanedUpLocks.text());
        }
    }

    protected static Properties getBundleProperties() {
        if (bundleProperties == null) {
            // This is idempotent, so we don't need to lock ...
            InputStream stream = null;
            try {
                stream = JcrRepository.class.getClassLoader().getResourceAsStream("org/modeshape/jcr/repository.properties");
                assert stream != null;
                Properties props = new Properties();
                props.load(stream);
                bundleProperties = new UnmodifiableProperties(props);
            } catch (IOException e) {
                throw new IllegalStateException(JcrI18n.failedToReadPropertiesFromManifest.text(e.getLocalizedMessage()), e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    } finally {
                        stream = null;
                    }
                }
            }
        }
        return bundleProperties;
    }

    protected static String getBundleProperty( String propertyName,
                                               boolean required ) {
        String value = getBundleProperties().getProperty(propertyName);
        if (value == null && required) {
            throw new IllegalStateException(JcrI18n.failedToReadPropertyFromManifest.text(propertyName));
        }
        return value;
    }

    /**
     * Combines the given custom descriptors with the default repository descriptors.
     * 
     * @param factories the value factories to use to create the descriptor values
     * @param customDescriptors the custom descriptors; may be null
     * @return the custom descriptors (if any) combined with the default repository descriptors; never null or empty
     */
    private static Map<String, Object> initializeDescriptors( ValueFactories factories,
                                                              Map<String, String> customDescriptors ) {
        if (customDescriptors == null) customDescriptors = Collections.emptyMap();
        Map<String, Object> repoDescriptors = new HashMap<String, Object>(customDescriptors.size() + 60);

        for (Map.Entry<String, String> entry : customDescriptors.entrySet()) {
            repoDescriptors.put(entry.getKey(), valueFor(factories, entry.getValue()));
        }

        repoDescriptors.put(Repository.LEVEL_1_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.LEVEL_2_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_LOCKING_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.QUERY_XPATH_DOC_ORDER, valueFor(factories, false)); // see MODE-613
        repoDescriptors.put(Repository.QUERY_XPATH_POS_INDEX, valueFor(factories, true));

        repoDescriptors.put(Repository.WRITE_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.IDENTIFIER_STABILITY, valueFor(factories,
                                                                        Repository.IDENTIFIER_STABILITY_METHOD_DURATION));
        repoDescriptors.put(Repository.OPTION_XML_IMPORT_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_XML_EXPORT_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_UNFILED_CONTENT_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_ACTIVITIES_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_BASELINES_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_ACCESS_CONTROL_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_RETENTION_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_LIFECYCLE_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_SHAREABLE_NODES_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE,
                              valueFor(factories, Repository.NODE_TYPE_MANAGEMENT_INHERITANCE_MULTIPLE));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.QUERY_LANGUAGES, new JcrValue[] {valueFor(factories, Query.XPATH),
            valueFor(factories, JcrSql2QueryParser.LANGUAGE), valueFor(factories, Query.SQL)});
        repoDescriptors.put(Repository.QUERY_STORED_QUERIES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.QUERY_JOINS, valueFor(factories, Repository.QUERY_JOINS_INNER));
        repoDescriptors.put(Repository.SPEC_NAME_DESC, valueFor(factories, JcrI18n.SPEC_NAME_DESC.text()));
        repoDescriptors.put(Repository.SPEC_VERSION_DESC, valueFor(factories, "1.0"));

        if (!repoDescriptors.containsKey(Repository.REP_NAME_DESC)) {
            repoDescriptors.put(Repository.REP_NAME_DESC,
                                valueFor(factories,
                                                                 JcrRepository.getBundleProperty(Repository.REP_NAME_DESC, true)));
        }
        if (!repoDescriptors.containsKey(Repository.REP_VENDOR_DESC)) {
            repoDescriptors.put(Repository.REP_VENDOR_DESC, valueFor(factories,
                                                                   JcrRepository.getBundleProperty(Repository.REP_VENDOR_DESC,
                                                                                                   true)));
        }
        if (!repoDescriptors.containsKey(Repository.REP_VENDOR_URL_DESC)) {
            repoDescriptors.put(Repository.REP_VENDOR_URL_DESC,
                              valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_VENDOR_URL_DESC, true)));
        }
        if (!repoDescriptors.containsKey(Repository.REP_VERSION_DESC)) {
            repoDescriptors.put(Repository.REP_VERSION_DESC,
                                valueFor(factories,
                                                                    JcrRepository.getBundleProperty(Repository.REP_VERSION_DESC,
                                                                                                    true)));
        }

        return Collections.unmodifiableMap(repoDescriptors);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      int type,
                                      Object value ) {
        return new JcrValue(valueFactories, null, type, value);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      String value ) {
        return valueFor(valueFactories, PropertyType.STRING, value);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      boolean value ) {
        return valueFor(valueFactories, PropertyType.BOOLEAN, value);
    }

    protected class FederatedRepositoryContext implements RepositoryContext {
        private final RepositoryConnectionFactory connectionFactory;

        protected FederatedRepositoryContext( RepositoryConnectionFactory nonFederatingConnectionFactory ) {
            this.connectionFactory = nonFederatingConnectionFactory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
         */
        public Subgraph getConfiguration( int depth ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
         */
        public ExecutionContext getExecutionContext() {
            return JcrRepository.this.getExecutionContext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
         */
        public Observer getObserver() {
            return JcrRepository.this.getObserver();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
         */
        public RepositoryConnectionFactory getRepositoryConnectionFactory() {
            return connectionFactory;
        }
    }

    protected class DelegatingConnectionFactory implements RepositoryConnectionFactory {
        private final RepositoryConnectionFactory delegate;
        private final RepositorySource source;

        protected DelegatingConnectionFactory( RepositoryConnectionFactory delegate,
                                               RepositorySource source ) {
            assert delegate != null;
            this.delegate = delegate;
            this.source = source;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
         */
        public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
            if (this.source.getName().equals(sourceName)) {
                return this.source.getConnection();
            }
            return delegate.createConnection(sourceName);
        }
    }

    @Immutable
    protected static class SourceWorkspacePair {
        private final String sourceName;
        private final String workspaceName;

        protected SourceWorkspacePair( String sourceAndWorkspaceName ) {
            assert sourceAndWorkspaceName != null;
            sourceAndWorkspaceName = sourceAndWorkspaceName.trim();
            assert sourceAndWorkspaceName.length() != 0;
            sourceAndWorkspaceName = sourceAndWorkspaceName.trim();
            // Look for the first '@' not preceded by a '\' ...
            int maxIndex = sourceAndWorkspaceName.length() - 1;
            int index = sourceAndWorkspaceName.indexOf('@');
            while (index > 0 && index < maxIndex && sourceAndWorkspaceName.charAt(index - 1) == '\\') {
                index = sourceAndWorkspaceName.indexOf('@', index + 1);
            }
            if (index > 0) {
                // There is a workspace and source name ...
                workspaceName = sourceAndWorkspaceName.substring(0, index).trim().replaceAll("\\\\@", "@");
                if (index < maxIndex) sourceName = sourceAndWorkspaceName.substring(index + 1).trim().replaceAll("\\\\@", "@");
                else throw new IllegalArgumentException("The source name is invalid");
            } else if (index == 0) {
                // The '@' was used, but the workspace is empty
                if (sourceAndWorkspaceName.length() == 1) {
                    sourceName = "";
                } else {
                    sourceName = sourceAndWorkspaceName.substring(1).trim().replaceAll("\\\\@", "@");
                }
                workspaceName = "";
            } else {
                // There is just a source name...
                workspaceName = null;
                sourceName = sourceAndWorkspaceName.replaceAll("\\\\@", "@");
            }
            assert this.sourceName != null;
        }

        /**
         * @return sourceName
         */
        public String getSourceName() {
            return sourceName;
        }

        /**
         * @return workspaceName
         */
        public String getWorkspaceName() {
            return workspaceName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            if (sourceName == null) return "";
            if (workspaceName != null) {
                return workspaceName + '@' + sourceName;
            }
            return sourceName;
        }
    }

    protected class RepositoryObservationManager implements Observable, Observer {

        private final ExecutorService observerService = Executors.newSingleThreadExecutor();
        private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<Observer>();
        private final Observable repositoryObservable;

        /**
         * @param repositoryObservable the repository library observable this observer should register with
         */
        protected RepositoryObservationManager( Observable repositoryObservable ) {
            this.repositoryObservable = repositoryObservable;
            this.repositoryObservable.register(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
         */
        public void notify( final Changes changes ) {
            // We only care about events that come from the federated source ...
            if (!changes.getSourceName().equals(getObservableSourceName())) return;

            // We're still in the thread where the connector published its changes,
            // so we need to create a runnable that will send these changes to all
            // of the observers <i>at this moment</i>. Because 'observers' is
            // a CopyOnWriteArrayList, we can't old onto the list (because the list's content
            // might change). Instead, hold onto the Iterator over the listeners,
            // and that will be a snapshot of the listeners <i>at this moment</i>
            if (observers.isEmpty()) return;
            final Iterator<Observer> observerIterator = observers.iterator();

            Runnable sender = new Runnable() {
                public void run() {
                    while (observerIterator.hasNext()) {
                        Observer observer = observerIterator.next();
                        assert observer != null;
                        observer.notify(changes);
                    }
                }
            };

            // Now let the executor service run this in another thread ...
            this.observerService.execute(sender);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.Observable#register(org.modeshape.graph.observe.Observer)
         */
        public boolean register( Observer observer ) {
            if (observer == null) return false;
            return this.observers.addIfAbsent(observer);
        }

        /**
         * Must be called to shutdown the service that is used to notify the observers.
         */
        void shutdown() {
            synchronized (this) {
                this.repositoryObservable.unregister(this);
                this.observers.clear();
                this.observerService.shutdown();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.Observable#unregister(org.modeshape.graph.observe.Observer)
         */
        public boolean unregister( Observer observer ) {
            if (observer == null) return false;
            return this.observers.remove(observer);
        }
    }
}

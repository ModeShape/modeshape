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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
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
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.UnmodifiableProperties;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JaasSecurityContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.Graph.Batch;
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
import org.modeshape.graph.connector.xmlfile.XmlFileRepositorySource;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.NetChangeObserver;
import org.modeshape.graph.observe.Observable;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.GraphNamespaceRegistry;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryBuilder.ConstraintBuilder;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.parse.QueryParsers;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.jcr.RepositoryQueryManager.PushDown;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.SecurityContextCredentials;
import org.modeshape.jcr.query.JcrQomQueryParser;
import org.modeshape.jcr.query.JcrSql2QueryParser;
import org.modeshape.jcr.query.JcrSqlQueryParser;
import org.modeshape.jcr.xpath.XPathQueryParser;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

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
         * The depth of the subgraphs that should be loaded from the connectors during normal read operations. The default value
         * is 1.
         */
        READ_DEPTH,

        /**
         * The depth of the subgraphs that should be loaded from the connectors during indexing operations. The default value is
         * 4.
         */
        INDEX_READ_DEPTH,

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
         * applications will never perform searches or queries, then maintaining the query indexes is an unnecessary overhead, and
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
        PERFORM_REFERENTIAL_INTEGRITY_CHECKS,

        /**
         * A boolean flag that indicates whether a complete list of workspace names should be exposed in the custom repository
         * descriptor {@link org.modeshape.jcr.api.Repository#REPOSITORY_WORKSPACES}.
         * <p>
         * If this option is set to {@code true}, then any code that can access the repository can retrieve a complete list of
         * workspace names through the {@link javax.jcr.Repository#getDescriptor(String)} method without
         * {@link javax.jcr.Repository#login logging in}.
         * </p>
         * <p>
         * Since some ModeShape installations may consider the list of workspace names to be restricted information and limit the
         * ability of some or all users to see a complete list of workspace names, this option can be set to {@code false} to
         * disable this capability. If this option is set to {@code false}, the
         * {@link org.modeshape.jcr.api.Repository#REPOSITORY_WORKSPACES} descriptor will not be set. In other words, the
         * following code will print {@code false}.
         * </p>
         * 
         * <pre>
         * Repository repo = ...;
         * System.out.println(repo.getDescriptorKeys().contains(org.modeshape.jcr.api.Repository#REPOSITORY_WORKSPACES));
         * </pre>
         * <p>
         * The default value is 'true', meaning that the descriptor is populated.
         * </p>
         */
        EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR,

        /**
         * A String property that when specified tells the {@link JcrEngine} where to put the {@link Repository} in to JNDI.
         * Assumes that you have write access to the JNDI tree. If no value set, then the {@link Repository} will not be bound to
         * JNDI.
         */
        REPOSITORY_JNDI_LOCATION,

        /**
         * The structure of the version history. There are two values allowed:
         * <ul>
         * <li>"<strong>flat</strong>" will store all "<code>nt:versionHistory</code>" nodes with a name matching the UUID of the
         * versioned node and directly under the <code>/jcr:system/jcr:versionStorage</code> node. For example, given a "
         * <code>mix:versionable</code>" node with the UUID <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the corresponding "
         * <code>nt:versionHistory</code>" node will be at
         * <code>/jcr:system/jcr:versionStorage/fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>.</li>
         * <li>"<strong>hierarchical</strong>" will store all "<code>nt:versionHistory</code>" nodes under a hiearchical structure
         * created by the first 8 characters of the UUID string. For example, given a "<code>mix:versionable</code>" node with the
         * UUID <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the corresponding "<code>nt:versionHistory</code>" node will be
         * at <code>/jcr:system/jcr:versionStorage/fa/e2/b9/29/c5ef-4ce5-9fa1-514779ca0ae3</code>.</li>
         * </ul>
         * <p>
         * The "hierarchical" structure is used by default and in cases where the option's value does not case-independently match
         * the {@link VersionHistoryOption#FLAT} or {@link VersionHistoryOption#HIERARCHICAL} values.
         * </p>
         */
        VERSION_HISTORY_STRUCTURE,

        /**
         * A boolean option that dictates whether content derived from other content (e.g., by sequencers) should be automatically
         * removed when the content from which it was derived is removed from the repository.
         * <p>
         * For example, consider that a file is uploaded and sequenced, and that the content derived from the file is stored in
         * the repository. When that file is removed, this option dictates whether the derived content should also be removed
         * automatically.
         * </p>
         * <p>
         * A value of 'true' will ensure that all content derived from deleted content is also deleted. A value of 'false' will
         * leave the derived content. The default value is 'true'.
         * </p>
         */
        REMOVE_DERIVED_CONTENT_WITH_ORIGINAL, ;

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
     * The possible values for the {@link Option#VERSION_HISTORY_STRUCTURE} option.
     */
    public static class VersionHistoryOption {
        /**
         * The value that signals that all "<code>nt:versionHistory</code>" nodes with a name matching the UUID of the versioned
         * node are stored directly under the <code>/jcr:system/jcr:versionStorage</code> node. For example, given a "
         * <code>mix:versionable</code>" node with the UUID <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the corresponding "
         * <code>nt:versionHistory</code>" node will be at
         * <code>/jcr:system/jcr:versionStorage/fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>.
         */
        public static final String FLAT = "flat";

        /**
         * The value that signals that all "<code>nt:versionHistory</code>" nodes be stored under a 4-tier hiearchical structure
         * created by the first 8 characters of the UUID string broken into 2-character pairs. For example, given a "
         * <code>mix:versionable</code>" node with the UUID <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the corresponding "
         * <code>nt:versionHistory</code>" node will be at
         * <code>/jcr:system/jcr:versionStorage/fa/e2/b9/29/c5ef-4ce5-9fa1-514779ca0ae3</code>.
         */
        public static final String HIERARCHICAL = "hierarchical";
    }

    /**
     * The default values for each of the {@link Option}.
     */
    public static class DefaultOption {
        /**
         * The default value for the {@link Option#PROJECT_NODE_TYPES} option is {@value} .
         */
        public static final String PROJECT_NODE_TYPES = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#JAAS_LOGIN_CONFIG_NAME} option is {@value} .
         */
        public static final String JAAS_LOGIN_CONFIG_NAME = "modeshape-jcr";

        /**
         * The default value for the {@link Option#READ_DEPTH} option is {@value} .
         */
        public static final String READ_DEPTH = "1";

        /**
         * The default value for the {@link Option#INDEX_READ_DEPTH} option is {@value} .
         */
        public static final String INDEX_READ_DEPTH = "4";

        /**
         * The default value for the {@link Option#ANONYMOUS_USER_ROLES} option is {@value} .
         */
        public static final String ANONYMOUS_USER_ROLES = ModeShapeRoles.ADMIN;

        /**
         * The default value for the {@link Option#TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES} option is {@value} .
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
         * The default value for the {@link Option#PERFORM_REFERENTIAL_INTEGRITY_CHECKS} option is {@value} .
         */
        public static final String PERFORM_REFERENTIAL_INTEGRITY_CHECKS = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#REPOSITORY_JNDI_LOCATION} option is {@value}
         */
        public static final String REPOSITORY_JNDI_LOCATION = "";

        /**
         * The default value for the {@link Option#EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR} option is {@value} .
         */
        public static final String EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR = Boolean.TRUE.toString();

        /**
         * The default value for the {@link Option#VERSION_HISTORY_STRUCTURE} option is {@value} .
         */
        public static final String VERSION_HISTORY_STRUCTURE = VersionHistoryOption.HIERARCHICAL;

        /**
         * The default value for the {@link Option#REMOVE_DERIVED_CONTENT_WITH_ORIGINAL} option is {@value} .
         */
        public static final String REMOVE_DERIVED_CONTENT_WITH_ORIGINAL = Boolean.TRUE.toString();
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
        @SuppressWarnings( "deprecation" )
        public static final String XPATH = Query.XPATH;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL query language defined by the JCR 1.0.1
         * specification.
         */
        @SuppressWarnings( "deprecation" )
        public static final String JCR_SQL = Query.SQL;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL2 query language defined by the JCR 2.0
         * specification.
         */
        public static final String JCR_SQL2 = Query.JCR_SQL2;

        /**
         * The enhanced Query Object Model language defined by the JCR 2.0 specification.
         */
        public static final String JCR_JQOM = Query.JCR_JQOM;
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
        defaults.put(Option.INDEX_READ_DEPTH, DefaultOption.INDEX_READ_DEPTH);
        defaults.put(Option.ANONYMOUS_USER_ROLES, DefaultOption.ANONYMOUS_USER_ROLES);
        defaults.put(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES,
                     DefaultOption.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES);
        defaults.put(Option.QUERY_EXECUTION_ENABLED, DefaultOption.QUERY_EXECUTION_ENABLED);
        defaults.put(Option.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY, DefaultOption.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY);
        defaults.put(Option.QUERY_INDEX_DIRECTORY, DefaultOption.QUERY_INDEX_DIRECTORY);
        defaults.put(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS, DefaultOption.PERFORM_REFERENTIAL_INTEGRITY_CHECKS);
        defaults.put(Option.EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR, DefaultOption.EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR);
        defaults.put(Option.VERSION_HISTORY_STRUCTURE, DefaultOption.VERSION_HISTORY_STRUCTURE);
        defaults.put(Option.REPOSITORY_JNDI_LOCATION, DefaultOption.REPOSITORY_JNDI_LOCATION);
        defaults.put(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL, DefaultOption.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL);
        DEFAULT_OPTIONS = Collections.<Option, String>unmodifiableMap(defaults);
    }

    private final String sourceName;
    private final Map<String, Object> descriptors = new HashMap<String, Object>();
    private final ExecutionContext executionContext;
    private final RepositoryConnectionFactory connectionFactory;
    private final RepositoryNodeTypeManager repositoryTypeManager;
    private final RepositoryLockManager repositoryLockManager;
    private final Map<Option, String> options;
    private final String systemSourceName;
    private final String systemWorkspaceName;
    private final Projection systemSourceProjection;
    private final FederatedRepositorySource federatedSource;
    private final GraphNamespaceRegistry persistentRegistry;
    private final RepositoryObservationManager repositoryObservationManager;
    private final SecurityContext anonymousUserContext;
    private final QueryParsers queryParsers;
    private Set<String> cachedWorkspaceNames = new HashSet<String>();

    // Until the federated connector supports queries, we have to use a search engine ...
    private final RepositoryQueryManager queryManager;

    /* The location of the XML file containing the initial content for newly-created workspaces */
    private final String initialContentForNewWorkspaces;

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
     * @param initialContentForNewWorkspaces the URL, file system path, or classpath resource path to the XML file containing the
     *        initial content for newly-created workspaces; may be null
     * @throws RepositoryException if there is a problem setting up this repository
     * @throws IllegalArgumentException If <code>executionContext</code>, <code>connectionFactory</code>,
     *         <code>repositorySourceName</code>, or <code>repositoryObservable</code> is <code>null</code>.
     */
    @SuppressWarnings( "deprecation" )
    JcrRepository( ExecutionContext executionContext,
                   RepositoryConnectionFactory connectionFactory,
                   String repositorySourceName,
                   Observable repositoryObservable,
                   RepositorySourceCapabilities repositorySourceCapabilities,
                   Map<String, String> descriptors,
                   Map<Option, String> options,
                   String initialContentForNewWorkspaces ) throws RepositoryException {
        CheckArg.isNotNull(executionContext, "executionContext");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(repositorySourceName, "repositorySourceName");
        CheckArg.isNotNull(repositoryObservable, "repositoryObservable");

        // Set up the options ...
        if (options == null) {
            this.options = DEFAULT_OPTIONS;
        } else {
            // Initialize with defaults, then add supplied options ...
            EnumMap<Option, String> localOptions = new EnumMap<Option, String>(DEFAULT_OPTIONS);
            localOptions.putAll(options);
            this.options = Collections.unmodifiableMap(localOptions);
        }

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
        InMemoryRepositorySource transientSystemSource = null;
        if (systemSourceName == null) {
            // Create the in-memory repository source that we'll use for the "/jcr:system" branch in this repository.
            // All workspaces will be set up with a federation connector that projects this system repository into
            // "/jcr:system", and all other content is projected to the repositories actual source (and workspace).
            // (The federation connector refers to this configuration as an "offset mirror".)
            systemWorkspaceName = "jcr:system";
            systemSourceName = "jcr:system source";
            transientSystemSource = new InMemoryRepositorySource();
            transientSystemSource.setName(systemSourceName);
            transientSystemSource.setDefaultWorkspaceName(systemWorkspaceName);
            connectionFactoryWithSystem = new DelegatingConnectionFactory(connectionFactory, transientSystemSource);
        }

        // Set up the query parsers, which we have to have even though queries might be disabled ...
        this.queryParsers = new QueryParsers(new JcrSql2QueryParser(), new XPathQueryParser(), new FullTextSearchParser(),
                                             new JcrSqlQueryParser(), new JcrQomQueryParser());
        assert this.queryParsers.getParserFor(Query.XPATH) != null;
        assert this.queryParsers.getParserFor(Query.SQL) != null;
        assert this.queryParsers.getParserFor(Query.JCR_SQL2) != null;
        assert this.queryParsers.getParserFor(Query.JCR_JQOM) != null;
        assert this.queryParsers.getParserFor(QueryLanguage.SEARCH) != null;

        this.systemWorkspaceName = systemWorkspaceName;
        this.systemSourceName = systemSourceName;
        this.connectionFactory = connectionFactoryWithSystem;
        assert this.systemSourceName != null;
        assert this.connectionFactory != null;
        this.sourceName = repositorySourceName;
        this.initialContentForNewWorkspaces = initialContentForNewWorkspaces;

        // Set up the "/jcr:system" branch ...
        Graph systemGraph = Graph.create(this.systemSourceName, this.connectionFactory, executionContext);
        systemGraph.useWorkspace(systemWorkspaceName);
        initializeSystemContent(systemGraph);

        // Create the namespace registry and corresponding execution context.
        // Note that this persistent registry has direct access to the system workspace.
        Name uriProperty = ModeShapeLexicon.URI;
        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        Path systemPath = pathFactory.create(JcrLexicon.SYSTEM);
        final Path namespacesPath = pathFactory.create(systemPath, ModeShapeLexicon.NAMESPACES);
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
        Path parentOfTypeNodes = null;

        if (Boolean.valueOf(this.options.get(Option.PROJECT_NODE_TYPES))) {
            parentOfTypeNodes = pathFactory.create(systemPath, JcrLexicon.NODE_TYPES);
        }

        try {
            boolean includeInheritedProperties = Boolean.valueOf(this.options.get(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES));
            boolean includePseudoColumnInSelectStar = true;

            // this.repositoryTypeManager = new RepositoryNodeTypeManager(this, includeInheritedProperties);
            this.repositoryTypeManager = new RepositoryNodeTypeManager(this, parentOfTypeNodes, includeInheritedProperties,
                                                                       includePseudoColumnInSelectStar);
            CndNodeTypeReader nodeTypeReader = new CndNodeTypeReader(this.executionContext);
            nodeTypeReader.readBuiltInTypes();
            this.repositoryTypeManager.registerNodeTypes(nodeTypeReader);
        } catch (RepositoryException re) {
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }
        if (WORKSPACES_SHARE_SYSTEM_BRANCH) {

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

            // Set up the workspaces corresponding to all those available in the source (except the system)
            Graph graph = Graph.create(sourceName, connectionFactory, executionContext);
            String defaultWorkspaceName = graph.getCurrentWorkspaceName();
            for (String workspaceName : graph.getWorkspaces()) {
                boolean isDefault = workspaceName.equals(defaultWorkspaceName);
                addWorkspace(workspaceName, isDefault);
            }
        } else {
            this.federatedSource = null;
            this.systemSourceProjection = null;
        }

        if (descriptors == null) descriptors = new HashMap<String, String>();
        // Determine if it's possible to manage workspaces with the underlying source ...
        if (repositorySourceCapabilities != null && repositorySourceCapabilities.supportsCreatingWorkspaces()) {
            // Don't overwrite (so they workspace management can be disabled) ...
            if (!descriptors.containsKey(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED)) {
                descriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, Boolean.TRUE.toString());
            }
        } else {
            // Not possible, so overwrite any value that might have been added ...
            descriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, Boolean.FALSE.toString());
        }

        // Initialize required JCR descriptors.
        this.descriptors.putAll(initializeDescriptors(executionContext.getValueFactories(), descriptors));

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
                int maxDepthToRead = Integer.valueOf(this.options.get(Option.INDEX_READ_DEPTH));
                this.queryManager = new RepositoryQueryManager.SelfContained(this.executionContext, this.sourceName,
                                                                             connectionFactory, repositoryObservable,
                                                                             repositoryTypeManager, indexDirectory,
                                                                             updateIndexesSynchronously, maxDepthToRead);
            }
        } else {
            this.queryManager = new RepositoryQueryManager.Disabled(this.sourceName);
        }

        // Initialize the observer, which receives events from all repository sources
        this.repositoryObservationManager = new RepositoryObservationManager(repositoryObservable);
        if (transientSystemSource != null) {
            // The transient RepositorySource for the system content is not in the RepositoryLibrary, so we need to observe it ...
            final Observer observer = this.repositoryObservationManager;
            final ExecutionContext context = executionContext;
            transientSystemSource.initialize(new RepositoryContext() {
                @Override
                public Observer getObserver() {
                    return observer;
                }

                @Override
                public ExecutionContext getExecutionContext() {
                    return context;
                }

                @Override
                public Subgraph getConfiguration( int depth ) {
                    return null; // not needed for the in-memory transient repository
                }

                @Override
                public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                    return null; // not needed for the in-memory transient repository
                }
            });
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

        repositoryLockManager = new RepositoryLockManager(this);

        // Create a system observer to update the namespace registry cache ...
        final GraphNamespaceRegistry persistentRegistry = this.persistentRegistry;
        final JcrSystemObserver namespaceObserver = new JcrSystemObserver() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.jcr.JcrSystemObserver#getObservedPath()
             */
            public Path getObservedPath() {
                return namespacesPath;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
             */
            public void notify( Changes changes ) {
                // These changes apply to anything at or below the namespaces path ...
                persistentRegistry.refresh();
            }
        };

        // Define the set of "/jcr:system" observers ...
        // This observer picks up notification of changes to the system graph in a cluster. It's a NOP if there is no cluster.
        repositoryObservationManager.register(new SystemChangeObserver(Arrays.asList(new JcrSystemObserver[] {
            repositoryLockManager, namespaceObserver, repositoryTypeManager})));

        if (Boolean.valueOf(this.options.get(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL))) {
            // Add an observer that moves/removes derived content when the original is moved/removed ...
            repositoryObservationManager.register(new DerivedContentSynchronizer());
        }

        // If the JNDI Location is set and not trivial, attempt the bind.
        String jndiLocation = this.options.get(Option.REPOSITORY_JNDI_LOCATION);
        if (!jndiLocation.equals("")) {
            try {
                InitialContext ic = new InitialContext();
                ic.rebind(jndiLocation, this);
            } catch (NamingException e) {
                I18n msg = JcrI18n.unableToBindToJndi;
                LOGGER.error(msg, jndiLocation);
                throw new RepositoryException(msg.text(jndiLocation), e);
            }
        }

        // Make sure the workspace names are in the descriptor ...
        updateWorkspaceNames();
    }

    protected String repositoryName() {
        return getDescriptor(org.modeshape.jcr.api.Repository.REPOSITORY_NAME);
    }

    protected void updateWorkspaceNames() {
        if (!Boolean.valueOf(this.options.get(Option.EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR)).booleanValue()) return;

        ValueFactories factories = this.getExecutionContext().getValueFactories();
        List<JcrValue> values = new LinkedList<JcrValue>();
        this.cachedWorkspaceNames = readWorkspaceNamesFromSource();
        for (String name : this.cachedWorkspaceNames) {
            values.add(new JcrValue(factories, null, PropertyType.STRING, name));
        }
        descriptors.put(Repository.REPOSITORY_WORKSPACES, values.toArray(new JcrValue[values.size()]));
    }

    protected void addWorkspace( String workspaceName,
                                 boolean isDefault ) {
        synchronized (this.federatedSource) {
            if (this.federatedSource == null) return;
            assert this.systemSourceProjection != null;
            if (!this.federatedSource.hasWorkspace(workspaceName)) {
                if (workspaceName.equals(systemWorkspaceName)) return;
                // Add the workspace to the federated source ...
                ProjectionParser projectionParser = ProjectionParser.getInstance();
                Projection.Rule[] mirrorRules = projectionParser.rulesFromString(this.executionContext, "/ => /");
                List<Projection> projections = new ArrayList<Projection>(2);
                projections.add(new Projection(sourceName, workspaceName, false, mirrorRules));
                projections.add(this.systemSourceProjection);
                this.federatedSource.addWorkspace(workspaceName, projections, isDefault);
            }
        }
        updateWorkspaceNames();
    }

    /**
     * Create a new workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace to be destroyed; may not be null
     * @param clonedFromWorkspaceNamed the name of the workspace that is to be cloned, or null if the new workspace is to be empty
     * @throws InvalidWorkspaceException if the workspace cannot be created because one already exists
     * @throws UnsupportedRepositoryOperationException if this repository does not support workspace management
     */
    protected void createWorkspace( String workspaceName,
                                    String clonedFromWorkspaceNamed )
        throws InvalidWorkspaceException, UnsupportedRepositoryOperationException {
        assert workspaceName != null;
        if (!Boolean.parseBoolean(getDescriptor(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED))) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (workspaceName.equals(systemWorkspaceName)) {
            // Cannot create a workspace that has the same name as the system workspace ...
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(workspaceName, systemSourceName);
            throw new InvalidWorkspaceException(msg);
        }
        // Create a graph to the underlying source ...
        Graph graph = Graph.create(sourceName, connectionFactory, executionContext);
        // Create the workspace (which will fail if workspaces cannot be created) ...
        Workspace graphWorkspace = null;
        if (clonedFromWorkspaceNamed != null) {
            graphWorkspace = graph.createWorkspace().clonedFrom(clonedFromWorkspaceNamed).named(workspaceName);
        } else {
            graphWorkspace = graph.createWorkspace().named(workspaceName);
        }

        // Ensure the workspace contains the initial content (if there is any) ...
        if (initialContentForNewWorkspaces != null) {
            XmlFileRepositorySource initialContentSource = new XmlFileRepositorySource();
            initialContentSource.setName("Initial content for " + sourceName);
            initialContentSource.setContentLocation(initialContentForNewWorkspaces);
            Graph initialContentGraph = Graph.create(initialContentSource, executionContext);
            graph.merge(initialContentGraph); // uses its own batch
        }
        String actualName = graphWorkspace.getName();
        addWorkspace(actualName, false); // this updates the workspace names
    }

    /**
     * Destroy the workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace to be destroyed; may not be null
     * @param currentWorkspace the workspace performing the operation; may not be null
     * @throws InvalidWorkspaceException if the workspace cannot be destroyed
     * @throws UnsupportedRepositoryOperationException if this repository does not support workspace management
     * @throws NoSuchWorkspaceException if the workspace does not exist
     * @throws RepositorySourceException if there is an error destroying this workspace
     */
    protected void destroyWorkspace( String workspaceName,
                                     JcrWorkspace currentWorkspace )
        throws InvalidWorkspaceException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException,
        RepositorySourceException {
        assert workspaceName != null;
        assert currentWorkspace != null;
        if (!Boolean.parseBoolean(getDescriptor(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED))) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (workspaceName.equals(systemWorkspaceName)) {
            // Cannot create a workspace that has the same name as the system workspace ...
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(workspaceName, sourceName);
            throw new InvalidWorkspaceException(msg);
        }
        if (currentWorkspace.getName().equals(workspaceName)) {
            String msg = GraphI18n.currentWorkspaceCannotBeDeleted.text(workspaceName, sourceName);
            throw new InvalidWorkspaceException(msg);
        }
        // Make sure the workspace exists ...
        Graph graph = Graph.create(sourceName, connectionFactory, executionContext);
        graph.useWorkspace(currentWorkspace.getName());
        if (!graph.getWorkspaces().contains(workspaceName)) {
            // Cannot create a workspace that has the same name as the system workspace ...
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, sourceName);
            throw new NoSuchWorkspaceException(msg);
        }

        // Remove the federated workspace ...
        if (federatedSource != null) {
            synchronized (federatedSource) {
                federatedSource.removeWorkspace(workspaceName);
            }
        }

        // And now destroy the workspace ...
        graph.destroyWorkspace().named(workspaceName);
        updateWorkspaceNames();
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

    JcrGraph createWorkspaceGraph( String workspaceName,
                                   ExecutionContext workspaceContext ) {
        JcrGraph graph = null;
        if (WORKSPACES_SHARE_SYSTEM_BRANCH) {
            // Connect via the federated source ...
            assert this.federatedSource != null;
            graph = JcrGraph.create(this.federatedSource, workspaceContext);
        } else {
            // Otherwise, just create a graph directly to the connection factory ...
            graph = JcrGraph.create(this.sourceName, this.connectionFactory, workspaceContext);
        }
        graph.useWorkspace(workspaceName);
        return graph;
    }

    JcrGraph createSystemGraph( ExecutionContext sessionContext ) {
        assert this.systemSourceName != null;
        assert this.connectionFactory != null;
        assert sessionContext != null;
        // The default workspace should be the system workspace ...
        JcrGraph result = JcrGraph.create(this.systemSourceName, this.connectionFactory, sessionContext);
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
     * Returns the repository-level lock manager
     * 
     * @return the repository-level lock manager
     */
    RepositoryLockManager getRepositoryLockManager() {
        return repositoryLockManager;
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

    String getSystemWorkspaceName() {
        return systemWorkspaceName;
    }

    /**
     * Get the name of the source that we want to observe.
     * 
     * @return the name of the source that should be observed; never null
     */
    String getObservableSourceName() {
        if (!WORKSPACES_SHARE_SYSTEM_BRANCH) {
            return sourceName;
        }
        return federatedSource.getName();
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

    protected boolean isQueryExecutionEnabled() {
        return Boolean.valueOf(getOptions().get(Option.QUERY_EXECUTION_ENABLED));
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
                    String policyName = options.get(Option.JAAS_LOGIN_CONFIG_NAME);
                    try {
                        execContext = executionContext.with(new JaasSecurityContext(policyName, simple.getUserID(),
                                                                                    simple.getPassword()));
                    } catch (javax.security.auth.login.LoginException error) {
                        throw new javax.jcr.LoginException(JcrI18n.loginConfigNotFound.text(policyName,
                                                                                            Option.JAAS_LOGIN_CONFIG_NAME,
                                                                                            repositoryName()), error);
                    }
                    for (String attributeName : simple.getAttributeNames()) {
                        Object attributeValue = simple.getAttribute(attributeName);
                        sessionAttributes.put(attributeName, attributeValue);
                    }
                } else if (credentials instanceof SecurityContextCredentials) {
                    execContext = executionContext.with(contextFor((SecurityContextCredentials)credentials));
                } else if (credentials instanceof JcrSecurityContextCredentials) {
                    execContext = executionContext.with(((JcrSecurityContextCredentials)credentials).getSecurityContext());
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
                throw error; // pass along
            } catch (javax.jcr.LoginException error) {
                throw error; // pass along
            } catch (Exception error) {
                throw new javax.jcr.LoginException(error); // wrap
            }
        }
        return sessionForContext(execContext, workspaceName, sessionAttributes);
    }

    /**
     * Adapts the modeshape-jcr-api {@link org.modeshape.jcr.api.SecurityContext} to the modeshape-graph {@link SecurityContext}
     * needed for repository login.
     * 
     * @param credentials the credentials containing the modeshape-jcr-api {@code SecurityContext}
     * @return an equivalent modeshape-graph {@code SecurityContext}
     */
    private SecurityContext contextFor( SecurityContextCredentials credentials ) {
        assert credentials != null;

        final org.modeshape.jcr.api.SecurityContext jcrSecurityContext = credentials.getSecurityContext();
        assert jcrSecurityContext != null;

        return new SecurityContext() {
            @Override
            public String getUserName() {
                return jcrSecurityContext.getUserName();
            }

            @Override
            public boolean hasRole( String roleName ) {
                return jcrSecurityContext.hasRole(roleName);
            }

            @Override
            public void logout() {
                jcrSecurityContext.logout();
            }

        };
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
                    // JCR 2.0 does not explicitely state the behavior if the workspace name is not found, though the JavaDoc
                    // does.
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
            addWorkspace(workspaceName, isDefault);
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
        return cachedWorkspaceNames;
    }

    /**
     * @return a list of all workspace names, without regard to the access permissions of any particular user
     */
    private Set<String> readWorkspaceNamesFromSource() {
        return Graph.create(sourceName, connectionFactory, executionContext).getWorkspaces();
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
     * Get the component that exposes the metrics of a repository, which is for internal use only. This mechanism will almost
     * certainly change or be replaced in an upcoming release.
     * 
     * @return the metrics interface; never null
     */
    public Metrics getMetrics() {
        return new Metrics();
    }

    /**
     * Terminate all active sessions.
     */
    void terminateAllSessions() {
        synchronized (this.activeSessions) {
            for (JcrSession session : this.activeSessions.keySet()) {
                session.terminate(false); // don't remove from active sessions, as we're blocked and iterating on it ...
            }
            this.activeSessions.clear();
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
    @SuppressWarnings( "deprecation" )
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
        repoDescriptors.put(Repository.IDENTIFIER_STABILITY, valueFor(factories, Repository.IDENTIFIER_STABILITY_METHOD_DURATION));
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
        repoDescriptors.put(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED, valueFor(factories, false));
        repoDescriptors.put(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.OPTION_SHAREABLE_NODES_SUPPORTED, valueFor(factories, true));
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
        repoDescriptors.put(Repository.QUERY_LANGUAGES,
                            new JcrValue[] {valueFor(factories, Query.XPATH), valueFor(factories, JcrSql2QueryParser.LANGUAGE),
                                valueFor(factories, Query.SQL), valueFor(factories, Query.JCR_JQOM)});
        repoDescriptors.put(Repository.QUERY_STORED_QUERIES_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED, valueFor(factories, true));
        repoDescriptors.put(Repository.QUERY_JOINS, valueFor(factories, Repository.QUERY_JOINS_INNER_OUTER));
        repoDescriptors.put(Repository.SPEC_NAME_DESC, valueFor(factories, JcrI18n.SPEC_NAME_DESC.text()));
        repoDescriptors.put(Repository.SPEC_VERSION_DESC, valueFor(factories, "2.0"));

        if (!repoDescriptors.containsKey(Repository.REP_NAME_DESC)) {
            repoDescriptors.put(Repository.REP_NAME_DESC,
                                valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_NAME_DESC, true)));
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
                                valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_VERSION_DESC, true)));
        }

        if (!repoDescriptors.containsKey(Repository.REP_VERSION_DESC)) {
            repoDescriptors.put(Repository.REP_VERSION_DESC,
                                valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_VERSION_DESC, true)));
        }
        if (!repoDescriptors.containsKey(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED)) {
            repoDescriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, valueFor(factories, true));
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
        private final String sourceName;
        private final String systemSourceName;
        private final String repositorySourceName;
        private final String processId;

        /**
         * @param repositoryObservable the repository library observable this observer should register with
         */
        protected RepositoryObservationManager( Observable repositoryObservable ) {
            this.repositoryObservable = repositoryObservable;
            this.repositoryObservable.register(this);
            this.sourceName = getObservableSourceName();
            this.systemSourceName = getSystemSourceName();
            this.repositorySourceName = getRepositorySourceName();
            this.processId = getExecutionContext().getProcessId();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
         */
        public void notify( Changes changes ) {
            final Changes acceptableChanges = filter(changes);
            if (acceptableChanges != null) {

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
                            observer.notify(acceptableChanges);
                        }
                    }
                };

                // Now let the executor service run this in another thread ...
                this.observerService.execute(sender);
            }
        }

        private Changes filter( Changes changes ) {
            // We only care about events that come from the repository source, the system source,
            // or the repository source name (for remote events only) ...
            String changedSourceName = changes.getSourceName();
            if (sourceName.equals(changedSourceName)) {
                // These are changes made locally by this repository ...
                return changes;
            }
            if (repositorySourceName.equals(changedSourceName)) {
                // These may be events generated locally or from a remote engine in the cluster ...
                if (this.processId.equals(changes.getProcessId())) {
                    // These events were made locally and are being handled above, so we can ignore these ...
                    return null;
                }
                // Otherwise, the changes were received from another engine in the cluster and
                // we do want to respond to these changes. However, the source name of the changes
                // needs to be altered to match the 'sourceName' ...
                return new Changes(changes.getProcessId(), changes.getContextId(), changes.getUserName(), sourceName,
                                   changes.getTimestamp(), changes.getChangeRequests(), changes.getData());
            }
            assert !changedSourceName.equals(repositorySourceName);
            if (systemSourceName.equals(changedSourceName)) {
                // These are changes made locally by this repository ...
                return changes;
            }
            return null;
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

    /**
     * This component is used internally to expose the metrics of a repository, and should not be used outside of the ModeShape
     * codebase, as it will almost certainly change or be replaced in upcoming releases.
     */
    public class Metrics {
        /**
         * Get the number of currently-active sessions in this repository.
         * 
         * @return the number of current, active sessions; always non-negative.
         */
        public int getActiveSessionCount() {
            synchronized (activeSessions) {
                return activeSessions.size();
            }
        }
    }

    /**
     * Observer that forwards changes to the system workspace in the system repository source to registered
     * {@link JcrSystemObserver JcrSystemObservers}. This method is intended to support consistency of internal data structures in
     * a cluster and only forwards changes from <i>other</i> processes.
     */
    class SystemChangeObserver implements Observer {

        /**
         * Immutable collection of objects observing changes to the system graph
         */
        private final Collection<JcrSystemObserver> jcrSystemObservers;
        private final String processId;
        private final String systemSourceName;
        private final String systemWorkspaceName;

        SystemChangeObserver( Collection<JcrSystemObserver> jcrSystemObservers ) {
            this.jcrSystemObservers = Collections.unmodifiableCollection(jcrSystemObservers);
            processId = getExecutionContext().getProcessId();
            systemSourceName = getSystemSourceName();
            systemWorkspaceName = getSystemWorkspaceName();

            assert processId != null;
            assert systemSourceName != null;
            assert systemWorkspaceName != null;
        }

        @Override
        public void notify( Changes changes ) {
            // Don't process changes from outside the system graph
            if (!changes.getSourceName().equals(systemSourceName)) {
                /*
                 * It's permissable for the system source to be the same as the source for the workspaces.
                 * In that case, the RepositoryObservationManager would have already translated the system
                 * source name into the observeable source name (from getObservableSourceName()), obscuring
                 * the actual source.
                 * 
                 * So if the change source name doesn't equal the system source name BUT the system source name
                 * is the same as the repository (read: workspace) source name, don't give up yet.  The difference
                 * may be due to RepositoryObservationManager.  Rely on the systemWorkspaceName check below to
                 * be sure.
                 */
                if (!systemSourceName.equals(getRepositorySourceName())) return;
            }

            // Don't process changes from this repository
            if (changes.getProcessId().equals(processId)) return;

            Multimap<JcrSystemObserver, ChangeRequest> systemChanges = LinkedListMultimap.create();

            for (ChangeRequest change : changes.getChangeRequests()) {
                // Don't process changes from outside the system workspace
                if (!systemWorkspaceName.equals(change.changedWorkspace())) continue;

                Path changedPath = change.changedLocation().getPath();
                if (changedPath == null) continue;

                for (JcrSystemObserver jcrSystemObserver : jcrSystemObservers) {
                    if (changedPath.isAtOrBelow(jcrSystemObserver.getObservedPath())) {
                        systemChanges.put(jcrSystemObserver, change);
                    }
                }
            }

            // Parcel out the new change objects
            for (JcrSystemObserver jcrSystemObserver : systemChanges.keySet()) {
                List<ChangeRequest> changesForObserver = (List<ChangeRequest>)systemChanges.get(jcrSystemObserver);
                Changes filteredChanges = new Changes(changes.getProcessId(), changes.getContextId(), changes.getUserName(),
                                                      systemSourceName, changes.getTimestamp(), changesForObserver,
                                                      changes.getData());
                jcrSystemObserver.notify(filteredChanges);
            }
        }
    }

    class DerivedContentSynchronizer extends NetChangeObserver {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.NetChangeObserver#notify(org.modeshape.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( NetChanges netChanges ) {
            // Go through the changes and look for moves or removes, and accumulate the paths that we should search for ...
            Map<String, Map<Path, Path>> pathsByWorkspaceName = null;
            for (NetChange change : netChanges.getNetChanges()) {
                String workspaceName = change.getRepositoryWorkspaceName();

                // Don't watch the system workspace ...
                if (getSystemWorkspaceName().equals(workspaceName)) continue;

                // Go through each net change, and only process node/property adds and property changes ...
                if (change.includes(ChangeType.NODE_REMOVED, ChangeType.NODE_MOVED)) {
                    if (pathsByWorkspaceName == null) pathsByWorkspaceName = new HashMap<String, Map<Path, Path>>();
                    Map<Path, Path> paths = pathsByWorkspaceName.get(change.getRepositoryWorkspaceName());
                    if (paths == null) {
                        paths = new HashMap<Path, Path>();
                        pathsByWorkspaceName.put(change.getRepositoryWorkspaceName(), paths);
                    }
                    Path newPath = null;
                    if (change.includes(ChangeType.NODE_MOVED)) {
                        newPath = change.getLocation().getPath();
                    }
                    paths.put(change.getPath(), newPath);
                }
            }

            if (pathsByWorkspaceName == null) {
                // No removes or deletes ...
                return;
            }

            // We should have at least one query ...
            final ExecutionContext context = getExecutionContext();
            QueryBuilder builder = new QueryBuilder(context.getValueFactories().getTypeSystem());
            DateTime timestamp = netChanges.getTimestamp();
            Schemata schemata = getRepositoryTypeManager().getRepositorySchemata();
            Map<String, Object> variables = null;
            QueryCommand query = null;
            String workspaceName = null;
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();

            try {

                // Query for 'mode:derived' nodes that were derived from any content at/under these paths ...
                for (Map.Entry<String, Map<Path, Path>> entry : pathsByWorkspaceName.entrySet()) {
                    workspaceName = entry.getKey();
                    Map<Path, Path> newPathByOld = entry.getValue();
                    Set<Path> paths = newPathByOld.keySet();

                    // Build a query for each workspace ...
                    ConstraintBuilder constraint = builder.select("jcr:path", "mode:derivedFrom", "mode:derivedAt")
                                                          .from("mode:derived")
                                                          .where();
                    constraint = constraint.propertyValue("mode:derived", "mode:derivedAt")
                                           .isLessThanOrEqualTo()
                                           .literal(timestamp)
                                           .and()
                                           .openParen();
                    boolean first = true;
                    for (Path path : paths) {
                        if (first) first = false;
                        else constraint = constraint.or();
                        constraint = constraint.propertyValue("mode:derived", "mode:derivedFrom")
                                               .isEqualTo()
                                               .literal(strings.create(path))
                                               .or()
                                               .propertyValue("mode:derived", "mode:derivedFrom")
                                               .isLike(strings.create(path) + "/%");
                    }
                    constraint = constraint.closeParen();
                    query = constraint.end().query();

                    // Submit the query ...
                    PlanHints hints = new PlanHints();
                    QueryResults results = queryManager().query(workspaceName, query, schemata, hints, variables);
                    int locIndex = results.getColumns().getLocationIndex("mode:derived");
                    int fromIndex = results.getColumns().getColumnIndexForName("mode:derivedFrom");
                    Batch batch = createWorkspaceGraph(workspaceName, context).batch();
                    for (Object[] tuple : results.getTuples()) {
                        Location derivedLocation = (Location)tuple[locIndex];
                        Path derivedFrom = pathFactory.create(tuple[fromIndex]);
                        // Find out which of the changed paths this corresponds. Note that we have to walk the changed paths
                        // because the changed paths may be ancestors) ..
                        for (Path path : paths) {
                            if (derivedFrom.isAtOrBelow(path)) {
                                // The derived location should only be below one of the changed paths ...
                                Path changedToPath = newPathByOld.get(path);
                                if (changedToPath != null) {
                                    // This is a move, so figure out the new derivedFrom path ...
                                    Path relative = derivedFrom.relativeTo(path);
                                    Path newDerivedFrom = relative.resolveAgainst(changedToPath);
                                    batch.set(ModeShapeLexicon.DERIVED_FROM).on(derivedLocation).to(newDerivedFrom).and();
                                } else {
                                    // The changed node was deleted ...
                                    batch.delete(derivedLocation).and();
                                }
                                break;
                            }
                        }
                    }
                    builder.clear();

                    // Execute the batch ...
                    batch.execute();
                }
            } catch (RepositoryException e) {
                String queryStr = Visitors.readable(query);
                Logger.getLogger(JcrRepository.this.getClass()).error(e,
                                                                      JcrI18n.failedToQueryForDerivedContent,
                                                                      workspaceName,
                                                                      queryStr);
            }
        }
    }
}

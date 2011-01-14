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
package org.modeshape.connector.store.jpa;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.modeshape.common.annotation.AllowedValues;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.store.jpa.model.simple.SimpleModel;
import org.modeshape.connector.store.jpa.util.StoreOptionEntity;
import org.modeshape.connector.store.jpa.util.StoreOptions;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * The {@link RepositorySource} for the connector that stores content in a (custom) relational database. This connector uses Java
 * Persistence API as the interface to the database, with Hibernate as the JPA implementation. (Note that some Hibernate-specific
 * features are used.)
 */
@ThreadSafe
public class JpaSource implements RepositorySource, ObjectFactory {

    private final Logger LOGGER = Logger.getLogger(JpaSource.class);

    /**
     * This source is capable of using different database schemas
     * 
     * @author Randall Hauch
     */
    public static class Models {
        public static final Model SIMPLE = new SimpleModel();
        private static final Model[] ALL_ARRAY = new Model[] {SIMPLE};
        private static final List<Model> MODIFIABLE_MODELS = new ArrayList<Model>(Arrays.asList(ALL_ARRAY));
        public static final Collection<Model> ALL = Collections.unmodifiableCollection(MODIFIABLE_MODELS);
        public static final Model DEFAULT = SIMPLE;

        public static boolean addModel( Model model ) {
            CheckArg.isNotNull(model, "modelName");
            for (Model existing : MODIFIABLE_MODELS) {
                if (existing.getName().equals(model.getName())) return false;
            }
            return MODIFIABLE_MODELS.add(model);
        }

        public static Model getModel( String name ) {
            CheckArg.isNotEmpty(name, "name");
            name = name.trim();
            for (Model existing : ALL) {
                if (existing.getName().equals(name)) return existing;
            }
            return null;
        }
    }

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String DATA_SOURCE_JNDI_NAME = "dataSourceJndiName";
    protected static final String DIALECT = "dialect";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String URL = "url";
    protected static final String DRIVER_CLASS_NAME = "driverClassName";
    protected static final String DRIVER_CLASSLOADER_NAME = "driverClassloaderName";
    protected static final String ISOLATION_LEVEL = "isolationLevel";
    protected static final String MAXIMUM_CONNECTIONS_IN_POOL = "maximumConnectionsInPool";
    protected static final String MINIMUM_CONNECTIONS_IN_POOL = "minimumConnectionsInPool";
    protected static final String MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS = "maximumConnectionIdleTimeInSeconds";
    protected static final String MAXIMUM_SIZE_OF_STATEMENT_CACHE = "maximumSizeOfStatementCache";
    protected static final String NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED = "numberOfConnectionsToBeAcquiredAsNeeded";
    protected static final String IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS = "idleTimeInSecondsBeforeTestingConnections";
    protected static final String CACHE_TIME_TO_LIVE_IN_MILLISECONDS = "cacheTimeToLiveInMilliseconds";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String MODEL_NAME = "modelName";
    protected static final String LARGE_VALUE_SIZE_IN_BYTES = "largeValueSizeInBytes";
    protected static final String COMPRESS_DATA = "compressData";
    protected static final String ENFORCE_REFERENTIAL_INTEGRITY = "enforceReferentialIntegrity";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    protected static final String AUTO_GENERATE_SCHEMA = "autoGenerateSchema";

    /**
     * This source supports events.
     */
    protected static final boolean SUPPORTS_EVENTS = true;
    /**
     * This source does not support automatic garbage collection.
     */
    protected static final boolean SUPPORTS_AUTOMATIC_GARBAGE_COLLECTION = false;
    /**
     * This source supports same-name-siblings.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = true;
    /**
     * This source supports creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = true;
    /**
     * This source supports updates by default, but each instance may be configured to {@link #setAllowsUpdates(boolean) be
     * read-only or updateable}.
     */
    public static final boolean DEFAULT_ALLOWS_UPDATES = true;
    /**
     * This source does not output executed SQL by default, but this can be overridden by calling {@link #setShowSql(boolean)}.
     */
    public static final boolean DEFAULT_SHOW_SQL = false;
    /**
     * This source does support creating workspaces.
     */
    public static final boolean DEFAULT_SUPPORTS_CREATING_WORKSPACES = true;

    /**
     * The default UUID that is used for root nodes in a store.
     */
    public static final String DEFAULT_ROOT_NODE_UUID = "1497b6fe-8c7e-4bbb-aaa2-24f3d4942668";

    /**
     * The initial {@link #getDefaultWorkspaceName() name of the default workspace} is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_WORKSPACE = "default";

    /**
     * The default value for {@link #setIsolationLevel(Integer)} is 'null', meaning this source does not explicitly set the
     * isolation level, so the JDBC DataSource's own level will be used.
     */
    private static final Integer DEFAULT_ISOLATION_LEVEL = null;

    private static final int DEFAULT_RETRY_LIMIT = 0;
    private static final int DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS = 60 * 5; // 5 minutes
    private static final int DEFAULT_MAXIMUM_FETCH_DEPTH = 3;
    private static final int DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL = 5;
    private static final int DEFAULT_MINIMUM_CONNECTIONS_IN_POOL = 0;
    private static final int DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS = 60 * 10; // 10 minutes
    private static final int DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE = 100;
    private static final int DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED = 1;
    private static final int DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS = 60 * 3; // 3 minutes
    private static final int DEFAULT_LARGE_VALUE_SIZE_IN_BYTES = 1 << 10; // 1 kilobyte
    private static final boolean DEFAULT_COMPRESS_DATA = true;
    private static final boolean DEFAULT_ENFORCE_REFERENTIAL_INTEGRITY = true;

    /**
     * The initial {@link #getAutoGenerateSchema() automatic schema generation setting} is "{@value} ", unless otherwise
     * specified.
     */
    public static final String DEFAULT_AUTO_GENERATE_SCHEMA = "validate";

    /**
     * The first serialized version of this source.
     */
    private static final long serialVersionUID = 1L;

    @Description( i18n = JpaConnectorI18n.class, value = "namePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "namePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = JpaConnectorI18n.class, value = "dataSourceJndiNamePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "dataSourceJndiNamePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "dataSourceJndiNamePropertyCategory" )
    private volatile String dataSourceJndiName;

    @Description( i18n = JpaConnectorI18n.class, value = "dialectPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "dialectPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "dialectPropertyCategory" )
    private volatile String dialect;

    @Description( i18n = JpaConnectorI18n.class, value = "usernamePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "usernamePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "usernamePropertyCategory" )
    private volatile String username;

    @Description( i18n = JpaConnectorI18n.class, value = "passwordPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "passwordPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "passwordPropertyCategory" )
    private volatile String password;

    @Description( i18n = JpaConnectorI18n.class, value = "urlPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "urlPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "urlPropertyCategory" )
    private volatile String url;

    @Description( i18n = JpaConnectorI18n.class, value = "driverClassNamePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "driverClassNamePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "driverClassNamePropertyCategory" )
    private volatile String driverClassName;

    // @Description( i18n = JpaConnectorI18n.class, value = "driverClassloaderNamePropertyDescription" )
    // @Label( i18n = JpaConnectorI18n.class, value = "driverClassloaderNamePropertyLabel" )
    // @Category( i18n = JpaConnectorI18n.class, value = "driverClassloaderNamePropertyCategory" )
    private volatile String driverClassloaderName;

    @Description( i18n = JpaConnectorI18n.class, value = "rootNodeUuidPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "rootNodeUuidPropertyCategory" )
    private volatile String rootNodeUuid = DEFAULT_ROOT_NODE_UUID;

    @Description( i18n = JpaConnectorI18n.class, value = "maximumConnectionsInPoolPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "maximumConnectionsInPoolPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "maximumConnectionsInPoolPropertyCategory" )
    private volatile int maximumConnectionsInPool = DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL;

    @Description( i18n = JpaConnectorI18n.class, value = "minimumConnectionsInPoolPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "minimumConnectionsInPoolPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "minimumConnectionsInPoolPropertyCategory" )
    private volatile int minimumConnectionsInPool = DEFAULT_MINIMUM_CONNECTIONS_IN_POOL;

    @Description( i18n = JpaConnectorI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyCategory" )
    private volatile int maximumConnectionIdleTimeInSeconds = DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS;

    @Description( i18n = JpaConnectorI18n.class, value = "maximumSizeOfStatementCachePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "maximumSizeOfStatementCachePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "maximumSizeOfStatementCachePropertyCategory" )
    private volatile int maximumSizeOfStatementCache = DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE;

    @Description( i18n = JpaConnectorI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyCategory" )
    private volatile int numberOfConnectionsToAcquireAsNeeded = DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED;

    @Description( i18n = JpaConnectorI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyCategory" )
    private volatile int idleTimeInSecondsBeforeTestingConnections = DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS;

    @Description( i18n = JpaConnectorI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;

    @Description( i18n = JpaConnectorI18n.class, value = "cacheTimeToLiveInMillisecondsPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "cacheTimeToLiveInMillisecondsPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "cacheTimeToLiveInMillisecondsPropertyCategory" )
    private volatile int cacheTimeToLiveInMilliseconds = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS * 1000;

    @Description( i18n = JpaConnectorI18n.class, value = "largeValueSizeInBytesPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "largeValueSizeInBytesPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "largeValueSizeInBytesPropertyCategory" )
    private volatile long largeValueSizeInBytes = DEFAULT_LARGE_VALUE_SIZE_IN_BYTES;

    @Description( i18n = JpaConnectorI18n.class, value = "showSqlPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "showSqlPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "showSqlPropertyCategory" )
    private volatile boolean showSql = DEFAULT_SHOW_SQL;

    @Description( i18n = JpaConnectorI18n.class, value = "compressDataPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "compressDataPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "compressDataPropertyCategory" )
    private volatile boolean compressData = DEFAULT_COMPRESS_DATA;

    // @Description( i18n = JpaConnectorI18n.class, value = "referentialIntegrityEnforcedPropertyDescription" )
    // @Label( i18n = JpaConnectorI18n.class, value = "referentialIntegrityEnforcedPropertyLabel" )
    // @Category( i18n = JpaConnectorI18n.class, value = "referentialIntegrityEnforcedPropertyCategory" )
    private volatile boolean referentialIntegrityEnforced = DEFAULT_ENFORCE_REFERENTIAL_INTEGRITY;

    @Description( i18n = JpaConnectorI18n.class, value = "autoGenerateSchemaPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "autoGenerateSchemaPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "autoGenerateSchemaPropertyCategory" )
    @AllowedValues( {"create", "create-drop", "update", "validate"} )
    private volatile String autoGenerateSchema = DEFAULT_AUTO_GENERATE_SCHEMA;

    @Description( i18n = JpaConnectorI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace = DEFAULT_NAME_OF_DEFAULT_WORKSPACE;

    @Description( i18n = JpaConnectorI18n.class, value = "isolationLevelPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "isolationLevelPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "isolationLevelPropertyCategory" )
    private volatile Integer isolationLevel = DEFAULT_ISOLATION_LEVEL;

    @Description( i18n = JpaConnectorI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(
                                                                                                  SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  DEFAULT_ALLOWS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  DEFAULT_SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES).withAutomaticGarbageCollection(SUPPORTS_AUTOMATIC_GARBAGE_COLLECTION);

    @Description( i18n = JpaConnectorI18n.class, value = "modelNamePropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "modelNamePropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "modelNamePropertyCategory" )
    private volatile String modelName;
    private transient Model model;
    private transient DataSource dataSource;
    private transient EntityManagers entityManagers;
    private transient CachePolicy cachePolicy;
    private transient RepositoryContext repositoryContext;
    private transient UUID rootUuid = UUID.fromString(rootNodeUuid);

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Set the name for the source
     * 
     * @param name the new name for the source
     */
    public void setName( String name ) {
        if (name != null) {
            name = name.trim();
            if (name.length() == 0) name = null;
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Get whether this source allows updates.
     * 
     * @return true if this source allows updates, or false if this source only supports reading content.
     */
    @Description( i18n = JpaConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    public boolean areUpdatesAllowed() {
        return capabilities.supportsUpdates();
    }

    /**
     * Set whether this source allows updates.
     * 
     * @param allowsUpdates true if this source allows updating content, or false if this source only allows reading content.
     */
    public synchronized void setAllowsUpdates( boolean allowsUpdates ) {
        capabilities = new RepositorySourceCapabilities(capabilities.supportsSameNameSiblings(), allowsUpdates,
                                                        capabilities.supportsEvents(), capabilities.supportsCreatingWorkspaces(),
                                                        capabilities.supportsReferences());
    }

    /**
     * Get whether this source outputs the SQL that it executes
     * 
     * @return whether this source outputs the SQL that it executes
     */
    public boolean getShowSql() {
        return this.showSql;
    }

    /**
     * Sets whether this source should output the SQL that it executes
     * 
     * @param showSql true if this source should output the SQL that it executes, otherwise false
     */
    public synchronized void setShowSql( boolean showSql ) {
        this.showSql = showSql;
    }

    /**
     * Get the Hibernate setting dictating what it does with the database schema upon first connection. For more information, see
     * {@link #setAutoGenerateSchema(String)}.
     * 
     * @return the setting; never null
     */
    public String getAutoGenerateSchema() {
        return this.autoGenerateSchema;
    }

    /**
     * Sets the Hibernate setting dictating what it does with the database schema upon first connection. Valid values are as
     * follows (though the value is not checked):
     * <ul>
     * <li>"<code>create</code>" - Create the database schema objects when the {@link EntityManagerFactory} is created (actually
     * when Hibernate's {@link SessionFactory} is created by the entity manager factory). If a file named "import.sql" exists in
     * the root of the class path (e.g., '/import.sql') Hibernate will read and execute the SQL statements in this file after it
     * has created the database objects. Note that Hibernate first delete all tables, constraints, or any other database object
     * that is going to be created in the process of building the schema.</li>
     * <li>"<code>create-drop</code>" - Same as "<code>create</code>", except that the schema will be dropped after the
     * {@link EntityManagerFactory} is closed.</li>
     * <li>"<code>update</code>" - Attempt to update the database structure to the current mapping (but does not read and invoke
     * the SQL statements from "import.sql"). <i>Use with caution.</i></li>
     * <li>"<code>validate</code>" - Validates the existing schema with the current entities configuration, but does not make any
     * changes to the schema (and does not read and invoke the SQL statements from "import.sql"). This is often the proper setting
     * to use in production, and thus this is the default value.</li>
     * </ul>
     * 
     * @param autoGenerateSchema the setting for the auto-generation, or null if the default should be used
     */
    public synchronized void setAutoGenerateSchema( String autoGenerateSchema ) {
        this.autoGenerateSchema = autoGenerateSchema != null ? autoGenerateSchema.trim() : DEFAULT_AUTO_GENERATE_SCHEMA;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        if (limit < 0) limit = 0;
        this.retryLimit = limit;
    }

    /**
     * Get the time in milliseconds that content returned from this source may used while in the cache.
     * 
     * @return the time to live, in milliseconds, or 0 if the time to live is not specified by this source
     */
    public int getCacheTimeToLiveInMilliseconds() {
        return cacheTimeToLiveInMilliseconds;
    }

    /**
     * Set the time in milliseconds that content returned from this source may used while in the cache.
     * 
     * @param cacheTimeToLive the time to live, in milliseconds; 0 if the time to live is not specified by this source; or a
     *        negative number for the default value
     */
    public synchronized void setCacheTimeToLiveInMilliseconds( int cacheTimeToLive ) {
        if (cacheTimeToLive < 0) cacheTimeToLive = DEFAULT_CACHE_TIME_TO_LIVE_IN_SECONDS;
        this.cacheTimeToLiveInMilliseconds = cacheTimeToLive;
        this.cachePolicy = cacheTimeToLiveInMilliseconds > 0 ? new JpaCachePolicy(cacheTimeToLiveInMilliseconds) : null;
    }

    /**
     * Returns the current cache policy
     * 
     * @return the current cache policy
     */
    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    /**
     * Returns the current {@code EntityManagers} reference.
     * 
     * @return the current {@code EntityManagers} reference.
     */
    public EntityManagers getEntityManagers() {
        return entityManagers;
    }

    /**
     * @return rootNodeUuid
     */
    public String getRootNodeUuid() {
        return rootNodeUuid;
    }

    /**
     * @return rootUuid
     */
    public UUID getRootUuid() {
        return rootUuid;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     * @throws IllegalArgumentException if the string value cannot be converted to UUID
     */
    public void setRootNodeUuid( String rootNodeUuid ) {
        if (rootNodeUuid != null && rootNodeUuid.trim().length() == 0) rootNodeUuid = DEFAULT_ROOT_NODE_UUID;
        this.rootUuid = UUID.fromString(rootNodeUuid);
        this.rootNodeUuid = rootNodeUuid;
    }

    /**
     * Get the name of the default workspace.
     * 
     * @return the name of the workspace that should be used by default, or null if there is no default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspace;
    }

    /**
     * Set the name of the workspace that should be used when clients don't specify a workspace.
     * 
     * @param nameOfDefaultWorkspace the name of the workspace that should be used by default, or null if the
     *        {@link #DEFAULT_NAME_OF_DEFAULT_WORKSPACE default name} should be used
     */
    public synchronized void setDefaultWorkspaceName( String nameOfDefaultWorkspace ) {
        this.defaultWorkspace = nameOfDefaultWorkspace != null ? nameOfDefaultWorkspace : DEFAULT_NAME_OF_DEFAULT_WORKSPACE;
    }

    /**
     * Gets the names of the workspaces that are available when this source is created.
     * 
     * @return the names of the workspaces that this source starts with, or null if there are no such workspaces
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    public synchronized String[] getPredefinedWorkspaceNames() {
        String[] copy = new String[predefinedWorkspaces.length];
        System.arraycopy(predefinedWorkspaces, 0, copy, 0, predefinedWorkspaces.length);
        return copy;
    }

    /**
     * Sets the names of the workspaces that are available when this source is created.
     * 
     * @param predefinedWorkspaceNames the names of the workspaces that this source should start with, or null if there are no
     *        such workspaces
     * @see #setCreatingWorkspacesAllowed(boolean)
     * @see #getPredefinedWorkspaceNames()
     */
    public synchronized void setPredefinedWorkspaceNames( String[] predefinedWorkspaceNames ) {
        if (predefinedWorkspaceNames != null && predefinedWorkspaceNames.length == 1) {
            predefinedWorkspaceNames = predefinedWorkspaceNames[0].split("\\s*,\\s*");
        }
        this.predefinedWorkspaces = predefinedWorkspaceNames != null ? predefinedWorkspaceNames : new String[] {};
    }

    /**
     * Get whether this source allows workspaces to be created dynamically.
     * 
     * @return true if this source allows workspaces to be created by clients, or false if the
     *         {@link #getPredefinedWorkspaceNames() set of workspaces} is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    @Description( i18n = JpaConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyDescription" )
    @Label( i18n = JpaConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyLabel" )
    @Category( i18n = JpaConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyCategory" )
    public boolean isCreatingWorkspacesAllowed() {
        return capabilities.supportsCreatingWorkspaces();
    }

    /**
     * Set whether this source allows workspaces to be created dynamically.
     * 
     * @param allowWorkspaceCreation true if this source allows workspaces to be created by clients, or false if the
     *        {@link #getPredefinedWorkspaceNames() set of workspaces} is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #isCreatingWorkspacesAllowed()
     */
    public synchronized void setCreatingWorkspacesAllowed( boolean allowWorkspaceCreation ) {
        capabilities = new RepositorySourceCapabilities(capabilities.supportsSameNameSiblings(), capabilities.supportsUpdates(),
                                                        capabilities.supportsEvents(), allowWorkspaceCreation,
                                                        capabilities.supportsReferences());
    }

    /**
     * @return dialect
     */
    public String getDialect() {
        return dialect;
    }

    /**
     * @param dialect Sets dialect to the specified value.
     */
    public synchronized void setDialect( String dialect ) {
        if (dialect != null && dialect.trim().length() == 0) dialect = null;
        this.dialect = dialect;
    }

    /**
     * @return dataSourceJndiName
     */
    public String getDataSourceJndiName() {
        return dataSourceJndiName;
    }

    /**
     * @param dataSourceJndiName Sets dataSourceJndiName to the specified value.
     */
    public void setDataSourceJndiName( String dataSourceJndiName ) {
        if (dataSourceJndiName != null && dataSourceJndiName.trim().length() == 0) dataSourceJndiName = null;
        this.dataSourceJndiName = dataSourceJndiName;
    }

    /**
     * @return driverClassName
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * @param driverClassName Sets driverClassName to the specified value.
     */
    public synchronized void setDriverClassName( String driverClassName ) {
        if (driverClassName != null && driverClassName.trim().length() == 0) driverClassName = null;
        this.driverClassName = driverClassName;
    }

    /**
     * @return driverClassloaderName
     */
    public String getDriverClassloaderName() {
        return driverClassloaderName;
    }

    /**
     * @param driverClassloaderName Sets driverClassloaderName to the specified value.
     */
    public void setDriverClassloaderName( String driverClassloaderName ) {
        if (driverClassloaderName != null && driverClassloaderName.trim().length() == 0) driverClassloaderName = null;
        this.driverClassloaderName = driverClassloaderName;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username Sets username to the specified value.
     */
    public synchronized void setUsername( String username ) {
        this.username = username;
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password Sets password to the specified value.
     */
    public synchronized void setPassword( String password ) {
        this.password = password;
    }

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url Sets url to the specified value.
     */
    public synchronized void setUrl( String url ) {
        if (url != null && url.trim().length() == 0) url = null;
        this.url = url;
    }

    /**
     * @return maximumConnectionsInPool
     */
    public int getMaximumConnectionsInPool() {
        return maximumConnectionsInPool;
    }

    /**
     * @param maximumConnectionsInPool Sets maximumConnectionsInPool to the specified value.
     */
    public synchronized void setMaximumConnectionsInPool( int maximumConnectionsInPool ) {
        if (maximumConnectionsInPool < 0) maximumConnectionsInPool = DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL;
        this.maximumConnectionsInPool = maximumConnectionsInPool;
    }

    /**
     * @return minimumConnectionsInPool
     */
    public int getMinimumConnectionsInPool() {
        return minimumConnectionsInPool;
    }

    /**
     * @param minimumConnectionsInPool Sets minimumConnectionsInPool to the specified value.
     */
    public synchronized void setMinimumConnectionsInPool( int minimumConnectionsInPool ) {
        if (minimumConnectionsInPool < 0) minimumConnectionsInPool = DEFAULT_MINIMUM_CONNECTIONS_IN_POOL;
        this.minimumConnectionsInPool = minimumConnectionsInPool;
    }

    /**
     * @return maximumConnectionIdleTimeInSeconds
     */
    public int getMaximumConnectionIdleTimeInSeconds() {
        return maximumConnectionIdleTimeInSeconds;
    }

    /**
     * @param maximumConnectionIdleTimeInSeconds Sets maximumConnectionIdleTimeInSeconds to the specified value.
     */
    public synchronized void setMaximumConnectionIdleTimeInSeconds( int maximumConnectionIdleTimeInSeconds ) {
        if (maximumConnectionIdleTimeInSeconds < 0) maximumConnectionIdleTimeInSeconds = DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS;
        this.maximumConnectionIdleTimeInSeconds = maximumConnectionIdleTimeInSeconds;
    }

    /**
     * @return maximumSizeOfStatementCache
     */
    public int getMaximumSizeOfStatementCache() {
        return maximumSizeOfStatementCache;
    }

    /**
     * @param maximumSizeOfStatementCache Sets maximumSizeOfStatementCache to the specified value.
     */
    public synchronized void setMaximumSizeOfStatementCache( int maximumSizeOfStatementCache ) {
        if (maximumSizeOfStatementCache < 0) maximumSizeOfStatementCache = DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE;
        this.maximumSizeOfStatementCache = maximumSizeOfStatementCache;
    }

    /**
     * @return numberOfConnectionsToAcquireAsNeeded
     */
    public int getNumberOfConnectionsToAcquireAsNeeded() {
        return numberOfConnectionsToAcquireAsNeeded;
    }

    /**
     * @param numberOfConnectionsToAcquireAsNeeded Sets numberOfConnectionsToAcquireAsNeeded to the specified value.
     */
    public synchronized void setNumberOfConnectionsToAcquireAsNeeded( int numberOfConnectionsToAcquireAsNeeded ) {
        if (numberOfConnectionsToAcquireAsNeeded < 0) numberOfConnectionsToAcquireAsNeeded = DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED;
        this.numberOfConnectionsToAcquireAsNeeded = numberOfConnectionsToAcquireAsNeeded;
    }

    /**
     * @return idleTimeInSecondsBeforeTestingConnections
     */
    public int getIdleTimeInSecondsBeforeTestingConnections() {
        return idleTimeInSecondsBeforeTestingConnections;
    }

    /**
     * @param idleTimeInSecondsBeforeTestingConnections Sets idleTimeInSecondsBeforeTestingConnections to the specified value.
     */
    public synchronized void setIdleTimeInSecondsBeforeTestingConnections( int idleTimeInSecondsBeforeTestingConnections ) {
        if (idleTimeInSecondsBeforeTestingConnections < 0) idleTimeInSecondsBeforeTestingConnections = DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS;
        this.idleTimeInSecondsBeforeTestingConnections = idleTimeInSecondsBeforeTestingConnections;
    }

    /**
     * Get the {@link DataSource} object that this source is to use.
     * 
     * @return the data source; may be null if no data source has been set or found in JNDI
     * @see #setDataSource(DataSource)
     * @see #setDataSourceJndiName(String)
     */
    /*package*/DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the {@link DataSource} instance that this source should use.
     * 
     * @param dataSource the data source; may be null
     * @see #getDataSource()
     * @see #setDataSourceJndiName(String)
     */
    /*package*/synchronized void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    /**
     * Get the model that will be used. This may be null if not yet connected, but after connections will reflect the type of
     * model that is being used in the store.
     * 
     * @return the name of the model
     */
    public String getModel() {
        return modelName;
    }

    /**
     * Set the model that should be used for this store. If the store already has a model, specifying a different value has no
     * effect, since the store's model will not be changed. After connection, this value will reflect the actual store value.
     * 
     * @param modelName the name of the model that should be used for new stores, or null if the default should be used
     */
    public synchronized void setModel( String modelName ) {
        if (modelName != null) {
            modelName = modelName.trim();
            if (modelName.length() == 0) modelName = null;
        }
        if (modelName == null) {
            model = null;
            return;
        }
        Model model = Models.getModel(modelName);
        if (model == null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Model existing : Models.ALL) {
                if (!first) {
                    first = false;
                    sb.append(", ");
                }
                sb.append('"').append(existing.getName()).append('"');
            }
            String modelNames = sb.toString();
            throw new IllegalArgumentException(JpaConnectorI18n.unknownModelName.text(model, modelNames));
        }
        this.model = model;
        this.modelName = modelName;
    }

    /**
     * @return largeValueSizeInBytes
     */
    public long getLargeValueSizeInBytes() {
        return largeValueSizeInBytes;
    }

    /**
     * @param largeValueSizeInBytes Sets largeValueSizeInBytes to the specified value.
     */
    public void setLargeValueSizeInBytes( long largeValueSizeInBytes ) {
        if (largeValueSizeInBytes < 0) largeValueSizeInBytes = DEFAULT_LARGE_VALUE_SIZE_IN_BYTES;
        this.largeValueSizeInBytes = largeValueSizeInBytes;
    }

    /**
     * @return compressData
     */
    public boolean isCompressData() {
        return compressData;
    }

    /**
     * @param compressData Sets compressData to the specified value.
     */
    public void setCompressData( boolean compressData ) {
        this.compressData = compressData;
    }

    /**
     * @return referentialIntegrityEnforced
     */
    public boolean isReferentialIntegrityEnforced() {
        return referentialIntegrityEnforced;
    }

    /**
     * @param referentialIntegrityEnforced Sets referentialIntegrityEnforced to the specified value.
     */
    public void setReferentialIntegrityEnforced( boolean referentialIntegrityEnforced ) {
        this.referentialIntegrityEnforced = referentialIntegrityEnforced;
    }

    /**
     * Get the JDBC transaction isolation level that should be used. Note that if the isolation level is not set (the value is
     * null), then this source does not explicitly set the isolation level, so the data source's value will implicitly be used.
     * 
     * @return isolationLevel the value of the isolation level, or null if the isolation level is not set by this source (meaning
     *         the data source's current setting or its default will be used)
     */
    public Integer getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * Set the JDBC transaction isolation level that should be used. Note that if the isolation level is not set (the value is
     * null), then this source does not explicitly set the isolation level, so the data source's value will implicitly be used.
     * 
     * @param isolationLevel the value of the isolation level, or null if the isolation level is not set by this source (meaning
     *        the data source's current setting or its default will be used)
     */
    public synchronized void setIsolationLevel( Integer isolationLevel ) {
        if (isolationLevel == null) {
            isolationLevel = DEFAULT_ISOLATION_LEVEL;
        }

        if (isolationLevel != DEFAULT_ISOLATION_LEVEL && isolationLevel != Connection.TRANSACTION_NONE
            && isolationLevel != Connection.TRANSACTION_READ_COMMITTED
            && isolationLevel != Connection.TRANSACTION_READ_UNCOMMITTED
            && isolationLevel != Connection.TRANSACTION_REPEATABLE_READ && isolationLevel != Connection.TRANSACTION_SERIALIZABLE) {
            throw new RepositorySourceException(this.name, JpaConnectorI18n.invalidIsolationLevel.text(isolationLevel));
        }

        if (this.isolationLevel == isolationLevel) return;

        EntityManagers oldEntityManagers = this.entityManagers;
        this.entityManagers = null;
        oldEntityManagers.close();

        this.isolationLevel = isolationLevel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid()));
        ref.add(new StringRefAddr(DATA_SOURCE_JNDI_NAME, getDataSourceJndiName()));
        ref.add(new StringRefAddr(DIALECT, getDialect()));
        ref.add(new StringRefAddr(USERNAME, getUsername()));
        ref.add(new StringRefAddr(PASSWORD, getPassword()));
        ref.add(new StringRefAddr(URL, getUrl()));
        ref.add(new StringRefAddr(DRIVER_CLASS_NAME, getDriverClassName()));
        ref.add(new StringRefAddr(DRIVER_CLASSLOADER_NAME, getDriverClassloaderName()));
        ref.add(new StringRefAddr(ISOLATION_LEVEL, Integer.toString(getIsolationLevel())));
        ref.add(new StringRefAddr(MAXIMUM_CONNECTIONS_IN_POOL, Integer.toString(getMaximumConnectionsInPool())));
        ref.add(new StringRefAddr(MINIMUM_CONNECTIONS_IN_POOL, Integer.toString(getMinimumConnectionsInPool())));
        ref.add(new StringRefAddr(MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS,
                                  Integer.toString(getMaximumConnectionIdleTimeInSeconds())));
        ref.add(new StringRefAddr(MAXIMUM_SIZE_OF_STATEMENT_CACHE, Integer.toString(getMaximumSizeOfStatementCache())));
        ref.add(new StringRefAddr(NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED,
                                  Integer.toString(getNumberOfConnectionsToAcquireAsNeeded())));
        ref.add(new StringRefAddr(IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS,
                                  Integer.toString(getIdleTimeInSecondsBeforeTestingConnections())));
        ref.add(new StringRefAddr(CACHE_TIME_TO_LIVE_IN_MILLISECONDS, Integer.toString(getCacheTimeToLiveInMilliseconds())));
        ref.add(new StringRefAddr(LARGE_VALUE_SIZE_IN_BYTES, Long.toString(getLargeValueSizeInBytes())));
        ref.add(new StringRefAddr(COMPRESS_DATA, Boolean.toString(isCompressData())));
        ref.add(new StringRefAddr(ENFORCE_REFERENTIAL_INTEGRITY, Boolean.toString(isReferentialIntegrityEnforced())));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        ref.add(new StringRefAddr(AUTO_GENERATE_SCHEMA, getAutoGenerateSchema()));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
        }
        if (getModel() != null) {
            ref.add(new StringRefAddr(MODEL_NAME, getModel()));
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    /**
     * Returns the current repository context for the source, as set with a call to {@link #initialize(RepositoryContext)}.
     * 
     * @return the current repository context for the source
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectInstance( Object obj,
                                     javax.naming.Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) throws Exception {
        if (obj instanceof Reference) {
            Map<String, String> values = new HashMap<String, String>();
            Reference ref = (Reference)obj;
            Enumeration<?> en = ref.getAll();
            while (en.hasMoreElements()) {
                RefAddr subref = (RefAddr)en.nextElement();
                if (subref instanceof StringRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value != null) values.put(key, value.toString());
                }
            }
            String sourceName = values.get(SOURCE_NAME);
            String rootNodeUuid = values.get(ROOT_NODE_UUID);
            String dataSourceJndiName = values.get(DATA_SOURCE_JNDI_NAME);
            String dialect = values.get(DIALECT);
            String username = values.get(USERNAME);
            String password = values.get(PASSWORD);
            String url = values.get(URL);
            String driverClassName = values.get(DRIVER_CLASS_NAME);
            String driverClassloaderName = values.get(DRIVER_CLASSLOADER_NAME);
            String isolationLevel = values.get(ISOLATION_LEVEL);
            String maxConnectionsInPool = values.get(MAXIMUM_CONNECTIONS_IN_POOL);
            String minConnectionsInPool = values.get(MINIMUM_CONNECTIONS_IN_POOL);
            String maxConnectionIdleTimeInSec = values.get(MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS);
            String maxSizeOfStatementCache = values.get(MAXIMUM_SIZE_OF_STATEMENT_CACHE);
            String acquisitionIncrement = values.get(NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED);
            String idleTimeInSeconds = values.get(IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS);
            String cacheTtlInMillis = values.get(CACHE_TIME_TO_LIVE_IN_MILLISECONDS);
            String modelName = values.get(MODEL_NAME);
            String retryLimit = values.get(RETRY_LIMIT);
            String largeModelSize = values.get(LARGE_VALUE_SIZE_IN_BYTES);
            String compressData = values.get(COMPRESS_DATA);
            String refIntegrity = values.get(ENFORCE_REFERENTIAL_INTEGRITY);
            String defaultWorkspace = values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = values.get(ALLOW_CREATING_WORKSPACES);
            String autoGenerateSchema = values.get(AUTO_GENERATE_SCHEMA);

            String combinedWorkspaceNames = values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            JpaSource source = new JpaSource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuid != null) source.setRootNodeUuid(rootNodeUuid);
            if (dataSourceJndiName != null) source.setDataSourceJndiName(dataSourceJndiName);
            if (dialect != null) source.setDialect(dialect);
            if (username != null) source.setUsername(username);
            if (password != null) source.setPassword(password);
            if (url != null) source.setUrl(url);
            if (driverClassName != null) source.setDriverClassName(driverClassName);
            if (driverClassloaderName != null) source.setDriverClassloaderName(driverClassloaderName);
            if (isolationLevel != null) source.setIsolationLevel(Integer.parseInt(isolationLevel));
            if (maxConnectionsInPool != null) source.setMaximumConnectionsInPool(Integer.parseInt(maxConnectionsInPool));
            if (minConnectionsInPool != null) source.setMinimumConnectionsInPool(Integer.parseInt(minConnectionsInPool));
            if (maxConnectionIdleTimeInSec != null) source.setMaximumConnectionIdleTimeInSeconds(Integer.parseInt(maxConnectionIdleTimeInSec));
            if (maxSizeOfStatementCache != null) source.setMaximumSizeOfStatementCache(Integer.parseInt(maxSizeOfStatementCache));
            if (acquisitionIncrement != null) source.setNumberOfConnectionsToAcquireAsNeeded(Integer.parseInt(acquisitionIncrement));
            if (idleTimeInSeconds != null) source.setIdleTimeInSecondsBeforeTestingConnections(Integer.parseInt(idleTimeInSeconds));
            if (cacheTtlInMillis != null) source.setCacheTimeToLiveInMilliseconds(Integer.parseInt(cacheTtlInMillis));
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            if (modelName != null) source.setModel(modelName);
            if (largeModelSize != null) source.setLargeValueSizeInBytes(Long.parseLong(largeModelSize));
            if (compressData != null) source.setCompressData(Boolean.parseBoolean(compressData));
            if (refIntegrity != null) source.setReferentialIntegrityEnforced(Boolean.parseBoolean(refIntegrity));
            if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (autoGenerateSchema != null) source.setAutoGenerateSchema(autoGenerateSchema);
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (this.name == null || this.name.trim().length() == 0) {
            throw new RepositorySourceException(JpaConnectorI18n.repositorySourceMustHaveName.text());
        }
        assert rootNodeUuid != null;
        assert rootUuid != null;
        if (entityManagers == null) {
            // Create the JPA EntityManagerFactory by programmatically configuring Hibernate Entity Manager ...
            Ejb3Configuration configurator = new Ejb3Configuration();

            // Configure the entity classes ...
            configurator.addAnnotatedClass(StoreOptionEntity.class);

            // Set the Hibernate properties used in all situations ...
            if (this.dialect != null) {
                // The dialect may be auto-determined ...
                setProperty(configurator, "hibernate.dialect", this.dialect);
            }
            if (this.isolationLevel != null) {
                setProperty(configurator, "hibernate.connection.isolation", this.isolationLevel);
            }

            // Configure additional properties, which may be overridden by subclasses ...
            configure(configurator);

            // Now set the mandatory information, overwriting anything that the subclasses may have tried ...
            if (this.dataSource == null && this.dataSourceJndiName != null) {
                // Try to load the DataSource from JNDI ...
                try {
                    Context context = new InitialContext();
                    dataSource = (DataSource)context.lookup(this.dataSourceJndiName);
                } catch (Throwable t) {
                    Logger.getLogger(getClass())
                          .error(t, JpaConnectorI18n.errorFindingDataSourceInJndi, name, dataSourceJndiName);
                }
            }

            if (this.dataSource != null) {
                // Set the data source ...
                configurator.setDataSource(this.dataSource);
            } else {
                // Set the context class loader, so that the driver could be found ...
                if (this.repositoryContext != null && this.driverClassloaderName != null) {
                    try {
                        ExecutionContext context = this.repositoryContext.getExecutionContext();
                        ClassLoader loader = context.getClassLoader(this.driverClassloaderName);
                        if (loader != null) {
                            Thread.currentThread().setContextClassLoader(loader);
                        }
                    } catch (Throwable t) {
                        I18n msg = JpaConnectorI18n.errorSettingContextClassLoader;
                        Logger.getLogger(getClass()).error(t, msg, name, driverClassloaderName);
                    }
                }
                // Set the connection properties ...
                setProperty(configurator, "hibernate.connection.driver_class", this.driverClassName);
                setProperty(configurator, "hibernate.connection.username", this.username);
                setProperty(configurator, "hibernate.connection.password", this.password);
                setProperty(configurator, "hibernate.connection.url", this.url);
                setProperty(configurator, "hibernate.connection.max_fetch_depth", DEFAULT_MAXIMUM_FETCH_DEPTH);
                setProperty(configurator, "hibernate.connection.pool_size", 0); // don't use the built-in pool
                if (this.maximumConnectionsInPool > 0) {
                    // Set the connection pooling properties (to use C3P0) ...
                    setProperty(configurator,
                                "hibernate.connection.provider_class",
                                "org.hibernate.connection.C3P0ConnectionProvider");
                    setProperty(configurator, "hibernate.c3p0.max_size", this.maximumConnectionsInPool);
                    setProperty(configurator, "hibernate.c3p0.min_size", this.minimumConnectionsInPool);
                    setProperty(configurator, "hibernate.c3p0.timeout", this.maximumConnectionIdleTimeInSeconds);
                    setProperty(configurator, "hibernate.c3p0.max_statements", this.maximumSizeOfStatementCache);
                    setProperty(configurator, "hibernate.c3p0.idle_test_period", this.idleTimeInSecondsBeforeTestingConnections);
                    setProperty(configurator, "hibernate.c3p0.acquire_increment", this.numberOfConnectionsToAcquireAsNeeded);
                    setProperty(configurator, "hibernate.c3p0.validate", "false");
                }
            }

            Logger logger = getLogger();
            if (logger.isDebugEnabled()) {
                logger.debug("Properties for Hibernate configuration used for ModeShape JPA Source {0}:", getName());
                Properties props = configurator.getProperties();
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    String propName = entry.getKey().toString();
                    if (propName.startsWith("hibernate")) {
                        logger.debug("  {0} = {1}", propName, entry.getValue());
                    }
                }
            }

            EntityManagerFactory entityManagerFactory = configurator.buildEntityManagerFactory();
            try {
                // Establish a connection and obtain the store options...
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {

                    // Find and update/set the root node's UUID ...
                    StoreOptions options = new StoreOptions(entityManager);
                    UUID actualUuid = options.getRootNodeUuid();
                    if (actualUuid != null) {
                        this.setRootNodeUuid(actualUuid.toString());
                    } else {
                        options.setRootNodeUuid(this.rootUuid);
                    }

                    // Find or set the type of model that will be used.
                    String actualModelName = options.getModelName();
                    if (actualModelName == null) {
                        // This is a new store, so set to the specified model ...
                        if (model == null) setModel(Models.DEFAULT.getName());
                        assert model != null;
                        options.setModelName(model);
                    } else {
                        // Set the model to the what's listed in the database ...
                        try {
                            setModel(actualModelName);
                        } catch (Throwable e) {
                            // The actual model name doesn't match what's available in the software ...
                            String msg = JpaConnectorI18n.existingStoreSpecifiesUnknownModel.text(name, actualModelName);
                            throw new RepositorySourceException(msg);
                        }
                    }

                    // Determine the dialect, if it was to be automatically discovered ...
                    if (this.dialect == null || this.dialect.trim().length() == 0) {
                        this.dialect = determineDialect(entityManager);
                    }
                    if (this.dialect == null || this.dialect.trim().length() == 0) {
                        // The dialect could not be determined ...
                        String msg = JpaConnectorI18n.dialectCouldNotBeDeterminedAndMustBeSpecified.text(name);
                        throw new RepositorySourceException(msg);
                    }

                } finally {
                    entityManager.close();
                }
            } finally {
                entityManagerFactory.close();
            }

            // The model has not yet configured itself, so do that now ...
            model.configure(configurator);

            // Now, create another entity manager with the classes from the correct model and without changing the schema...
            entityManagers = new EntityManagers(configurator);
        }

        return model.createConnection(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        if (entityManagers != null) {
            try {
                // Close this object; existing connections will continue to work, and the last connection closed
                // will actually shut the lights off...
                entityManagers.close();
            } finally {
                entityManagers = null;
            }
        }
    }

    /**
     * Automatically determine the dialect.
     * 
     * @param entityManager the EntityManager instance; may not be null
     * @return the name of the dialect, or null if the dialect cannot be determined
     */
    protected String determineDialect( EntityManager entityManager ) {
        // We need the connection in order to determine the dialect ...
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor)entityManager.unwrap(Session.class)
                                                                                           .getSessionFactory();
        return sessionFactory.getDialect().toString();
    }

    /**
     * Set up the JPA configuration using Hibernate, except for the entity classes (which will already be configured when this
     * method is called) and the data source or connection information (which will be set after this method returns). Subclasses
     * may override this method to customize the configuration.
     * <p>
     * This method sets up the C3P0 connection pooling, the cache provider, and some DDL options.
     * </p>
     * 
     * @param configuration the Hibernate configuration; never null
     */
    protected void configure( Ejb3Configuration configuration ) {

        // Disable the second-level cache ...
        setProperty(configuration, "hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider");

        // Set up the schema and DDL options ...
        setProperty(configuration, "hibernate.show_sql", String.valueOf(this.showSql)); // writes all SQL statements to console
        setProperty(configuration, "hibernate.format_sql", "true");
        setProperty(configuration, "hibernate.use_sql_comments", "true");
        setProperty(configuration, "hibernate.hbm2ddl.auto", this.autoGenerateSchema);
    }

    protected void setProperty( Ejb3Configuration configurator,
                                String propertyName,
                                String propertyValue ) {
        assert configurator != null;
        assert propertyName != null;
        assert propertyName.trim().length() != 0;
        if (propertyValue != null) {
            configurator.setProperty(propertyName, propertyValue.trim());
        }
    }

    protected void setProperty( Ejb3Configuration configurator,
                                String propertyName,
                                int propertyValue ) {
        assert configurator != null;
        assert propertyName != null;
        assert propertyName.trim().length() != 0;
        configurator.setProperty(propertyName, Integer.toString(propertyValue));
    }

    @Immutable
    /*package*/class JpaCachePolicy implements CachePolicy {
        private static final long serialVersionUID = 1L;
        private final int ttl;

        /*package*/JpaCachePolicy( int ttl ) {
            this.ttl = ttl;
        }

        public long getTimeToLive() {
            return ttl;
        }

    }

}

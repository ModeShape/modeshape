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
package org.modeshape.connector.meta.jdbc;

import java.beans.PropertyVetoException;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.connector.meta.jdbc.JdbcMetadataRepository.JdbcMetadataTransaction;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.AbstractRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import com.mchange.v2.c3p0.ComboPooledDataSource;

@ThreadSafe
public class JdbcMetadataSource extends AbstractRepositorySource implements ObjectFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(JdbcMetadataSource.class);

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String DATA_SOURCE_JNDI_NAME = "dataSourceJndiName";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String URL = "url";
    protected static final String DRIVER_CLASS_NAME = "driverClassName";
    protected static final String DRIVER_CLASSLOADER_NAME = "driverClassloaderName";
    protected static final String MAXIMUM_CONNECTIONS_IN_POOL = "maximumConnectionsInPool";
    protected static final String MINIMUM_CONNECTIONS_IN_POOL = "minimumConnectionsInPool";
    protected static final String MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS = "maximumConnectionIdleTimeInSeconds";
    protected static final String MAXIMUM_SIZE_OF_STATEMENT_CACHE = "maximumSizeOfStatementCache";
    protected static final String NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED = "numberOfConnectionsToBeAcquiredAsNeeded";
    protected static final String IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS = "idleTimeInSecondsBeforeTestingConnections";
    protected static final String CACHE_TIME_TO_LIVE_IN_MILLISECONDS = "cacheTimeToLiveInMilliseconds";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String DEFAULT_CATALOG_NAME = "defaultCatalogName";
    protected static final String DEFAULT_SCHEMA_NAME = "defaultSchemaName";
    protected static final String METADATA_COLLECTOR_CLASS_NAME = "metadataCollectorClassName";

    /**
     * This source does not support events.
     */
    protected static final boolean SUPPORTS_EVENTS = false;
    /**
     * This source does support same-name-siblings for procedure nodes.
     */
    protected static final boolean SUPPORTS_SAME_NAME_SIBLINGS = true;
    /**
     * This source does not support creating references.
     */
    protected static final boolean SUPPORTS_REFERENCES = false;
    /**
     * This source does not support updates.
     */
    public static final boolean SUPPORTS_UPDATES = false;
    /**
     * This source does not support creating workspaces.
     */
    public static final boolean SUPPORTS_CREATING_WORKSPACES = false;

    /**
     * The initial {@link #getDefaultWorkspaceName() name of the default workspace} is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_WORKSPACE = "default";

    /**
     * The initial {@link #getDefaultCatalogName() catalog name for databases that do not support catalogs} is "{@value} ", unless
     * otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_CATALOG = "default";

    /**
     * The initial {@link #getDefaultSchemaName() schema name for databases that do not support schemas} is "{@value} ", unless
     * otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_SCHEMA = "default";

    private static final int DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL = 5;
    private static final int DEFAULT_MINIMUM_CONNECTIONS_IN_POOL = 0;
    private static final int DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS = 60 * 10; // 10 minutes
    private static final int DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE = 100;
    private static final int DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED = 1;
    private static final int DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS = 60 * 3; // 3 minutes
    private static final MetadataCollector DEFAULT_METADATA_COLLECTOR = new JdbcMetadataCollector();

    @Description( i18n = JdbcMetadataI18n.class, value = "dataSourceJndiNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "dataSourceJndiNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "dataSourceJndiNamePropertyCategory" )
    private volatile String dataSourceJndiName;

    @Description( i18n = JdbcMetadataI18n.class, value = "usernamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "usernamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "usernamePropertyCategory" )
    private volatile String username;

    @Description( i18n = JdbcMetadataI18n.class, value = "passwordPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "passwordPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "passwordPropertyCategory" )
    private volatile String password;

    @Description( i18n = JdbcMetadataI18n.class, value = "urlPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "urlPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "urlPropertyCategory" )
    private volatile String url;

    @Description( i18n = JdbcMetadataI18n.class, value = "driverClassNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "driverClassNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "driverClassNamePropertyCategory" )
    private volatile String driverClassName;

    // @Description( i18n = JdbcMetadataI18n.class, value = "driverClassloaderNamePropertyDescription" )
    // @Label( i18n = JdbcMetadataI18n.class, value = "driverClassloaderNamePropertyLabel" )
    // @Category( i18n = JdbcMetadataI18n.class, value = "driverClassloaderNamePropertyCategory" )
    private volatile String driverClassloaderName;

    @Description( i18n = JdbcMetadataI18n.class, value = "maximumConnectionsInPoolPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "maximumConnectionsInPoolPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "maximumConnectionsInPoolPropertyCategory" )
    private volatile int maximumConnectionsInPool = DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL;

    @Description( i18n = JdbcMetadataI18n.class, value = "minimumConnectionsInPoolPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "minimumConnectionsInPoolPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "minimumConnectionsInPoolPropertyCategory" )
    private volatile int minimumConnectionsInPool = DEFAULT_MINIMUM_CONNECTIONS_IN_POOL;

    @Description( i18n = JdbcMetadataI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "maximumConnectionIdleTimeInSecondsPropertyCategory" )
    private volatile int maximumConnectionIdleTimeInSeconds = DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS;

    @Description( i18n = JdbcMetadataI18n.class, value = "maximumSizeOfStatementCachePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "maximumSizeOfStatementCachePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "maximumSizeOfStatementCachePropertyCategory" )
    private volatile int maximumSizeOfStatementCache = DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE;

    @Description( i18n = JdbcMetadataI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "numberOfConnectionsToAcquireAsNeededPropertyCategory" )
    private volatile int numberOfConnectionsToAcquireAsNeeded = DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED;

    @Description( i18n = JdbcMetadataI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "idleTimeInSecondsBeforeTestingConnectionsPropertyCategory" )
    private volatile int idleTimeInSecondsBeforeTestingConnections = DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS;

    @Description( i18n = JdbcMetadataI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace = DEFAULT_NAME_OF_DEFAULT_WORKSPACE;

    @Description( i18n = JdbcMetadataI18n.class, value = "defaultCatalogNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "defaultCatalogNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "defaultCatalogNamePropertyCategory" )
    private volatile String defaultCatalogName = DEFAULT_NAME_OF_DEFAULT_CATALOG;

    @Description( i18n = JdbcMetadataI18n.class, value = "defaultSchemaNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "defaultSchemaNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "defaultSchemaNamePropertyCategory" )
    private volatile String defaultSchemaName = DEFAULT_NAME_OF_DEFAULT_SCHEMA;

    @Description( i18n = JdbcMetadataI18n.class, value = "metadataCollectorClassNamePropertyDescription" )
    @Label( i18n = JdbcMetadataI18n.class, value = "metadataCollectorClassNamePropertyLabel" )
    @Category( i18n = JdbcMetadataI18n.class, value = "metadataCollectorClassNamePropertyCategory" )
    private volatile String metadataCollectorClassName = DEFAULT_METADATA_COLLECTOR.getClass().getName();

    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(SUPPORTS_SAME_NAME_SIBLINGS,
                                                                                                  SUPPORTS_UPDATES,
                                                                                                  SUPPORTS_EVENTS,
                                                                                                  SUPPORTS_CREATING_WORKSPACES,
                                                                                                  SUPPORTS_REFERENCES);
    private transient DataSource dataSource;
    private transient JdbcMetadataRepository repository;
    private transient MetadataCollector metadataCollector = DEFAULT_METADATA_COLLECTOR;

    private ExecutionContext defaultContext = new ExecutionContext();

    final JdbcMetadataRepository repository() {
        return this.repository;
    }

    @Override
    public void close() {
        if (this.dataSource instanceof ComboPooledDataSource) {
            ((ComboPooledDataSource)this.dataSource).close();
        }
    }

    /**
     * @return the datasource that corresponds to the login information provided to this source
     * @see #setUsername(String)
     * @see #setPassword(String)
     * @see #setDriverClassName(String)
     * @see #setDriverClassloaderName(String)
     * @see #setUrl(String)
     * @see #setDataSourceJndiName(String)
     */
    DataSource getDataSource() {
        if (this.dataSource == null) {
            loadDataSource();
        }
        return this.dataSource;
    }

    public RepositorySourceCapabilities getCapabilities() {
        return this.capabilities;
    }

    /*
     * Synchronized to avoid race conditions with setters of datasource-related properties
     */
    private synchronized void loadDataSource() throws RepositorySourceException {
        // Now set the mandatory information, overwriting anything that the subclasses may have tried ...
        if (this.dataSource == null && this.dataSourceJndiName != null) {
            // Try to load the DataSource from JNDI ...
            try {
                Context context = new InitialContext();
                dataSource = (DataSource)context.lookup(this.dataSourceJndiName);
            } catch (Throwable t) {
                LOGGER.error(t, JdbcMetadataI18n.errorFindingDataSourceInJndi, getName(), dataSourceJndiName);
            }
        }

        if (this.dataSource == null) {
            // Set the context class loader, so that the driver could be found ...
            if (this.repositoryContext != null && this.driverClassloaderName != null) {
                try {
                    ExecutionContext context = this.repositoryContext.getExecutionContext();
                    ClassLoader loader = context.getClassLoader(this.driverClassloaderName);
                    if (loader != null) {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
                } catch (Throwable t) {
                    I18n msg = JdbcMetadataI18n.errorSettingContextClassLoader;
                    LOGGER.error(t, msg, getName(), driverClassloaderName);
                }
            }

            if (this.driverClassName == null || this.url == null) {
                throw new RepositorySourceException(JdbcMetadataI18n.driverClassNameAndUrlAreRequired.text(driverClassName, url));
            }

            ComboPooledDataSource cpds = new ComboPooledDataSource();

            try {
                cpds.setDriverClass(this.driverClassName);
                cpds.setJdbcUrl(this.url);
                cpds.setUser(this.username);
                cpds.setPassword(this.password);
                cpds.setMaxStatements(this.maximumSizeOfStatementCache);
                cpds.setAcquireRetryAttempts(retryLimit);
                cpds.setMaxIdleTime(this.maximumConnectionIdleTimeInSeconds);
                cpds.setMinPoolSize(this.minimumConnectionsInPool);
                cpds.setMaxPoolSize(this.maximumConnectionsInPool);
                cpds.setAcquireIncrement(this.numberOfConnectionsToAcquireAsNeeded);
                cpds.setIdleConnectionTestPeriod(this.idleTimeInSecondsBeforeTestingConnections);

            } catch (PropertyVetoException pve) {
                throw new IllegalStateException(JdbcMetadataI18n.couldNotSetDriverProperties.text(), pve);
            }

            this.dataSource = cpds;
        }

    }

    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (this.getName() == null || this.getName().trim().length() == 0) {
            throw new RepositorySourceException(JdbcMetadataI18n.repositorySourceMustHaveName.text());
        }

        if (repository == null) {
            repository = new JdbcMetadataRepository(this);

            ExecutionContext context = repositoryContext != null ? repositoryContext.getExecutionContext() : defaultContext;
            JdbcMetadataTransaction txn = repository.startTransaction(context, true);
            try {
                repository.createWorkspace(txn, getDefaultWorkspaceName(), CreateConflictBehavior.DO_NOT_CREATE, null);
            } finally {
                txn.commit();
            }

        }
        return new Connection<PathNode, JdbcMetadataWorkspace>(this, repository);
    }

    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuidObject().toString()));
        ref.add(new StringRefAddr(DATA_SOURCE_JNDI_NAME, getDataSourceJndiName()));
        ref.add(new StringRefAddr(USERNAME, getUsername()));
        ref.add(new StringRefAddr(PASSWORD, getPassword()));
        ref.add(new StringRefAddr(URL, getUrl()));
        ref.add(new StringRefAddr(DRIVER_CLASS_NAME, getDriverClassName()));
        ref.add(new StringRefAddr(DRIVER_CLASSLOADER_NAME, getDriverClassloaderName()));
        ref.add(new StringRefAddr(MAXIMUM_CONNECTIONS_IN_POOL, Integer.toString(getMaximumConnectionsInPool())));
        ref.add(new StringRefAddr(MINIMUM_CONNECTIONS_IN_POOL, Integer.toString(getMinimumConnectionsInPool())));
        ref.add(new StringRefAddr(MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS,
                                  Integer.toString(getMaximumConnectionIdleTimeInSeconds())));
        ref.add(new StringRefAddr(MAXIMUM_SIZE_OF_STATEMENT_CACHE, Integer.toString(getMaximumSizeOfStatementCache())));
        ref.add(new StringRefAddr(NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED,
                                  Integer.toString(getNumberOfConnectionsToAcquireAsNeeded())));
        ref.add(new StringRefAddr(IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS,
                                  Integer.toString(getIdleTimeInSecondsBeforeTestingConnections())));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));

        ref.add(new StringRefAddr(DEFAULT_CATALOG_NAME, getDefaultCatalogName()));
        ref.add(new StringRefAddr(DEFAULT_SCHEMA_NAME, getDefaultSchemaName()));
        ref.add(new StringRefAddr(METADATA_COLLECTOR_CLASS_NAME, getMetadataCollectorClassName()));

        return ref;
    }

    public Object getObjectInstance( Object obj,
                                     javax.naming.Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) throws Exception {
        if (!(obj instanceof Reference)) {
            return null;
        }

        Map<String, Object> values = valuesFrom((Reference)obj);

        String sourceName = (String)values.get(SOURCE_NAME);
        String rootNodeUuid = (String)values.get(ROOT_NODE_UUID);
        String dataSourceJndiName = (String)values.get(DATA_SOURCE_JNDI_NAME);
        String username = (String)values.get(USERNAME);
        String password = (String)values.get(PASSWORD);
        String url = (String)values.get(URL);
        String driverClassName = (String)values.get(DRIVER_CLASS_NAME);
        String driverClassloaderName = (String)values.get(DRIVER_CLASSLOADER_NAME);
        String maxConnectionsInPool = (String)values.get(MAXIMUM_CONNECTIONS_IN_POOL);
        String minConnectionsInPool = (String)values.get(MINIMUM_CONNECTIONS_IN_POOL);
        String maxConnectionIdleTimeInSec = (String)values.get(MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS);
        String maxSizeOfStatementCache = (String)values.get(MAXIMUM_SIZE_OF_STATEMENT_CACHE);
        String acquisitionIncrement = (String)values.get(NUMBER_OF_CONNECTIONS_TO_BE_ACQUIRED_AS_NEEDED);
        String idleTimeInSeconds = (String)values.get(IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS);
        String retryLimit = (String)values.get(RETRY_LIMIT);
        String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
        String defaultCatalogName = (String)values.get(DEFAULT_CATALOG_NAME);
        String defaultSchemaName = (String)values.get(DEFAULT_SCHEMA_NAME);
        String metadataCollectorClassName = (String)values.get(METADATA_COLLECTOR_CLASS_NAME);

        // Create the source instance ...
        JdbcMetadataSource source = new JdbcMetadataSource();
        if (sourceName != null) source.setName(sourceName);
        if (rootNodeUuid != null) source.setRootNodeUuidObject(rootNodeUuid);
        if (dataSourceJndiName != null) source.setDataSourceJndiName(dataSourceJndiName);
        if (username != null) source.setUsername(username);
        if (password != null) source.setPassword(password);
        if (url != null) source.setUrl(url);
        if (driverClassName != null) source.setDriverClassName(driverClassName);
        if (driverClassloaderName != null) source.setDriverClassloaderName(driverClassloaderName);
        if (maxConnectionsInPool != null) source.setMaximumConnectionsInPool(Integer.parseInt(maxConnectionsInPool));
        if (minConnectionsInPool != null) source.setMinimumConnectionsInPool(Integer.parseInt(minConnectionsInPool));
        if (maxConnectionIdleTimeInSec != null) source.setMaximumConnectionIdleTimeInSeconds(Integer.parseInt(maxConnectionIdleTimeInSec));
        if (maxSizeOfStatementCache != null) source.setMaximumSizeOfStatementCache(Integer.parseInt(maxSizeOfStatementCache));
        if (acquisitionIncrement != null) source.setNumberOfConnectionsToAcquireAsNeeded(Integer.parseInt(acquisitionIncrement));
        if (idleTimeInSeconds != null) source.setIdleTimeInSecondsBeforeTestingConnections(Integer.parseInt(idleTimeInSeconds));
        if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
        if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
        if (defaultCatalogName != null) source.setDefaultCatalogName(defaultCatalogName);
        if (defaultSchemaName != null) source.setDefaultCatalogName(defaultSchemaName);
        if (metadataCollectorClassName != null) source.setMetadataCollectorClassName(metadataCollectorClassName);

        return source;

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
    public synchronized void setDriverClassloaderName( String driverClassloaderName ) {
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
     * Get the name of the default catalog.
     * 
     * @return the name that should be used as the catalog name when the database does not support catalogs
     */
    public String getDefaultCatalogName() {
        return defaultCatalogName;
    }

    /**
     * Set the name of the catalog that should be used when the database does not support catalogs.
     * 
     * @param defaultCatalogName the name that should be used as the catalog name by default, or null if the
     *        {@link #DEFAULT_NAME_OF_DEFAULT_CATALOG default name} should be used
     */
    public void setDefaultCatalogName( String defaultCatalogName ) {
        this.defaultCatalogName = defaultCatalogName == null ? DEFAULT_NAME_OF_DEFAULT_CATALOG : defaultCatalogName;
    }

    /**
     * Get the name of the default schema.
     * 
     * @return the name that should be used as the schema name when the database does not support schemas
     */
    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    /**
     * Set the name of the schema that should be used when the database does not support schemas.
     * 
     * @param defaultSchemaName the name that should be used as the schema name by default, or null if the
     *        {@link #DEFAULT_NAME_OF_DEFAULT_SCHEMA default name} should be used
     */
    public void setDefaultSchemaName( String defaultSchemaName ) {
        this.defaultSchemaName = defaultSchemaName == null ? DEFAULT_NAME_OF_DEFAULT_SCHEMA : defaultSchemaName;
    }

    /**
     * Get the class name of the metadata collector.
     * 
     * @return the name the class name of the metadata collector
     */
    public String getMetadataCollectorClassName() {
        return metadataCollectorClassName;
    }

    /**
     * Set the class name of the metadata collector and instantiates a new metadata collector object for that class
     * 
     * @param metadataCollectorClassName the class name for the metadata collector, or null if the
     *        {@link #DEFAULT_METADATA_COLLECTOR default metadata collector} should be used
     * @throws ClassNotFoundException if the the named metadata collector class cannot be located
     * @throws IllegalAccessException if the metadata collector class or its nullary constructor is not accessible.
     * @throws InstantiationException if the metadata collector class represents an abstract class, an interface, an array class,
     *         a primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other
     *         reason.
     * @throws ClassCastException if the given class cannot be cast to {@link MetadataCollector}.
     * @see Class#forName(String)
     * @see Class#newInstance()
     */
    @SuppressWarnings( "unchecked" )
    public synchronized void setMetadataCollectorClassName( String metadataCollectorClassName )
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (metadataCollectorClassName == null) {
            this.metadataCollectorClassName = DEFAULT_METADATA_COLLECTOR.getClass().getName();
            this.metadataCollector = DEFAULT_METADATA_COLLECTOR;
        } else {
            Class newCollectorClass = Class.forName(metadataCollectorClassName);
            this.metadataCollector = (MetadataCollector)newCollectorClass.newInstance();
            this.metadataCollectorClassName = metadataCollectorClassName;
        }
    }

    /**
     * Returns the metadata collector instance
     * 
     * @return the metadata collector
     */
    public synchronized MetadataCollector getMetadataCollector() {
        return metadataCollector;
    }

    /**
     * In-memory connectors aren't shared and cannot be loaded from external sources if updates are not allowed. Therefore, in
     * order to avoid setting up an in-memory connector that is permanently empty (presumably, not a desired outcome), all
     * in-memory connectors must allow updates.
     * 
     * @param updatesAllowed must be true
     * @throws RepositorySourceException if {@code updatesAllowed != true}.
     */
    public void setUpdatesAllowed( boolean updatesAllowed ) {
        if (updatesAllowed == false) {
            throw new RepositorySourceException(JdbcMetadataI18n.sourceIsReadOnly.text(this.getName()));
        }

    }

}

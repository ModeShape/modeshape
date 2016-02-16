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
package org.modeshape.connector.meta.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.ReadOnlyConnector;
import org.modeshape.schematic.document.Document;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Readonly connector which exposes JDBC metadata.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JdbcMetadataConnector extends ReadOnlyConnector {

    protected static final String DELIMITER = "/";

    protected static final String DEFAULT_NAME_OF_DEFAULT_CATALOG = "default";
    protected static final String DEFAULT_NAME_OF_DEFAULT_SCHEMA = "default";

    private static final int DEFAULT_RETRY_LIMIT = 0;
    private static final int DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL = 5;
    private static final int DEFAULT_MINIMUM_CONNECTIONS_IN_POOL = 0;
    private static final int DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS = 60 * 10; // 10 minutes
    private static final int DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE = 100;
    private static final int DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED = 1;
    private static final int DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS = 60 * 3; // 3 minutes

    /**
     * The JNDI name of the JDBC DataSource instance that should be used. If not specified, the other driver properties must be set.
     */
    protected String dataSourceJndiName;

    /**
     * The username that should be used when creating JDBC connections using the JDBC driver class.
     * This is not required if the DataSource is found in JNDI.
     */
    protected String username;

    /**
     * The password that should be used when creating JDBC connections using the JDBC driver class.
     * This is not required if the DataSource is found in JNDI.
     */
    protected String password;

    /**
     * The URL that should be used when creating JDBC connections using the JDBC driver class.
     * This is not required if the DataSource is found in JNDI.
     */
    protected String url;

    /**
     * The name of the JDBC driver class. This is not required if the DataSource is found in JNDI, but is required otherwise.
     */
    protected String driverClassName;

    /**
     * The default number of times that a failed attempt to connect to a datasource should be retried
     */
    protected int retryLimit = DEFAULT_RETRY_LIMIT;

    /**
     * The maximum number of connections that may be in the connection pool. The default is "5".
     * This is not required if the DataSource is found in JNDI.
     */
    protected int maximumConnectionsInPool = DEFAULT_MAXIMUM_CONNECTIONS_IN_POOL;

    /**
     * The minimum number of connections that will be kept in the connection pool. The default is "0".
     */
    protected int minimumConnectionsInPool = DEFAULT_MINIMUM_CONNECTIONS_IN_POOL;

    /**
     * The maximum number of seconds that a connection can remain idle in the pool before it is closed.
     * The default is "600" seconds (or 10 minutes).
     */
    protected int maximumConnectionIdleTimeInSeconds = DEFAULT_MAXIMUM_CONNECTION_IDLE_TIME_IN_SECONDS;

    /**
     * The maximum number of statements that should be cached.
     * Statement caching can be disabled by setting to "0". The default is "100".
     */
    protected int maximumSizeOfStatementCache = DEFAULT_MAXIMUM_NUMBER_OF_STATEMENTS_TO_CACHE;

    /**
     * The number of connections that should be added to the pool when there are not enough to be used.
     * The default is "1".
     */
    protected int numberOfConnectionsToAcquireAsNeeded = DEFAULT_NUMBER_OF_CONNECTIONS_TO_ACQUIRE_AS_NEEDED;

    /**
     * The number of seconds that a connection can remain idle before the connection should be tested to ensure it is still valid.
     * The default is 180 seconds (or 3 minutes).
     */
    protected int idleTimeInSecondsBeforeTestingConnections = DEFAULT_IDLE_TIME_IN_SECONDS_BEFORE_TESTING_CONNECTIONS;

    /**
     * The name to use for the catalog name if the database does not support catalogs or the database has a catalog with the empty string as a name.
     * The default value is "default".
     */
    protected String defaultCatalogName = DEFAULT_NAME_OF_DEFAULT_CATALOG;

    /**
     * The name to use for the schema name if the database does not support schemas or the database has a schema with the empty string as a name.
     * The default value is "default".
     */
    protected String defaultSchemaName = DEFAULT_NAME_OF_DEFAULT_SCHEMA;

    /**
     * The name of a custom class to use for metadata collection. The class must implement the MetadataCollector interface.
     * If a null value is specified for this property, a default MetadataCollector implementation will be used that relies
     * on the DatabaseMetaData provided by the JDBC driver for the connection.
     * This property is provided as a means for connecting to databases with a JDBC driver that provides a non-standard DatabaseMetaData
     * implementation or no DatabaseMetaData implementation at all.
     */
    protected String metadataCollectorClassName = JdbcMetadataCollector.class.getName();

    /**
     * Whether to close the data source when the connector shuts down or not
     */
    protected boolean closeDataSourceOnShutdown = false;

    protected DataSource dataSource;
    protected MetadataCollector metadataCollector;
    protected List<? extends AbstractMetadataRetriever> metadataRetrievers;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        initMetadataCollector();
        initDataSource();
        initNodeTypes(nodeTypeManager);
        initRetrievers();
    }

    protected void initRetrievers() {
        metadataRetrievers = Arrays.asList(new DatabaseRetriever(this), new CatalogRetriever(this), new SchemaRetriever(this),
                                           new TableRetriever(this), new ColumnRetriever(this), new ProcedureRetriever(this),
                                           new ForeignKeyRetriever(this));
    }

    protected void initNodeTypes( NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        InputStream cndStream = getClass().getClassLoader().getResourceAsStream(
                "org/modeshape/connector/meta/jdbc/metajdbc.cnd");
        nodeTypeManager.registerNodeTypes(cndStream, true);
    }

    protected void initDataSource() {
        if (this.dataSource == null && this.dataSourceJndiName != null) {
            // Try to load the DataSource from JNDI ...
            try {
                Context context = new InitialContext();
                dataSource = (DataSource)context.lookup(this.dataSourceJndiName);
            } catch (Throwable t) {
                getLogger().error(t, JdbcMetadataI18n.errorFindingDataSourceInJndi, getSourceName(), dataSourceJndiName);
            }
        }

        if (this.dataSource == null) {
            if (StringUtil.isBlank(this.url) || StringUtil.isBlank(this.driverClassName)) {
                throw new JdbcMetadataException(JdbcMetadataI18n.driverClassNameAndUrlAreRequired, "driverClassName", "url");
            }

            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(this.driverClassName);
            dataSource.setJdbcUrl(this.url);
            dataSource.setUsername(this.username);
            dataSource.setPassword(this.password);
            dataSource.setMaximumPoolSize(this.maximumConnectionsInPool);
            dataSource.setIdleTimeout(this.maximumConnectionIdleTimeInSeconds * 1000);

            this.dataSource = dataSource;
        }
    }

    protected void initMetadataCollector() {
        try {
            Class<?> newCollectorClass = Class.forName(metadataCollectorClassName, true, getEnvironment().getClassLoader(this));
            this.metadataCollector = (MetadataCollector)newCollectorClass.newInstance();
        } catch (Exception e) {
            throw new JdbcMetadataException(e);
        }
    }

    @Override
    public Document getDocumentById( String id ) {
        DocumentWriter writer = newDocument(id);
        for (AbstractMetadataRetriever retriever : metadataRetrievers) {
            if (retriever.canHandle(id)) {
                Connection connection = null;
                try {
                    connection = dataSource.getConnection();
                    return retriever.getDocumentById(id, writer, connection);
                } catch (SQLException e) {
                    tryToClose(connection);
                    throw new JdbcMetadataException(JdbcMetadataI18n.errorObtainingConnection, e);
                } finally {
                    tryToClose(connection);
                }
            }
        }
        return null;
    }

    protected void tryToClose( Connection connection ) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                getLogger().debug("Cannot close JDBC connection", closeException);
            }
        }
    }

    @Override
    public String getDocumentId( String externalPath ) {
        externalPath = trimPath(externalPath);
        for (AbstractMetadataRetriever retriever : metadataRetrievers) {
            String documentId = retriever.idFrom(externalPath);
            if (documentId != null) {
                return documentId;
            }
        }
        return null;
    }

    @Override
    public Collection<String> getDocumentPathsById( String id ) {
        return Collections.singletonList(id);
    }

    protected String trimPath( String externalPath ) {
        if (externalPath.trim().equalsIgnoreCase(DELIMITER)) {
            return DELIMITER;
        } else if (externalPath.endsWith(DELIMITER)) {
            return externalPath.substring(0, externalPath.length() - 1);
        }
        return externalPath;
    }

    @Override
    public boolean hasDocument( String id ) {
        for (AbstractMetadataRetriever metadataRetriever : metadataRetrievers) {
            if (metadataRetriever.canHandle(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void shutdown() {
        if (closeDataSourceOnShutdown && this.dataSource instanceof HikariDataSource) {
            ((HikariDataSource)this.dataSource).close();
        }
    }

    protected MetadataCollector getMetadataCollector() {
        return metadataCollector;
    }

    protected String getDefaultCatalogName() {
        return defaultCatalogName;
    }

    protected String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    protected void setRetryLimit( Integer retryLimit ) {
        if (retryLimit != null) {
            this.retryLimit = retryLimit;
        }
    }

    protected void setMaximumConnectionsInPool( Integer maximumConnectionsInPool ) {
        if (maximumConnectionsInPool != null) {
            this.maximumConnectionsInPool = maximumConnectionsInPool;
        }
    }

    protected void setMinimumConnectionsInPool( Integer minimumConnectionsInPool ) {
        if (minimumConnectionsInPool != null) {
            this.minimumConnectionsInPool = minimumConnectionsInPool;
        }
    }

    protected void setMaximumConnectionIdleTimeInSeconds( Integer maximumConnectionIdleTimeInSeconds ) {
        if (maximumConnectionIdleTimeInSeconds != null) {
            this.maximumConnectionIdleTimeInSeconds = maximumConnectionIdleTimeInSeconds;
        }
    }

    protected void setMaximumSizeOfStatementCache( Integer maximumSizeOfStatementCache ) {
        if (maximumSizeOfStatementCache != null) {
            this.maximumSizeOfStatementCache = maximumSizeOfStatementCache;
        }
    }

    protected void setNumberOfConnectionsToAcquireAsNeeded( Integer numberOfConnectionsToAcquireAsNeeded ) {
        if (numberOfConnectionsToAcquireAsNeeded != null) {
            this.numberOfConnectionsToAcquireAsNeeded = numberOfConnectionsToAcquireAsNeeded;
        }
    }

    protected void setIdleTimeInSecondsBeforeTestingConnections( Integer idleTimeInSecondsBeforeTestingConnections ) {
        if (idleTimeInSecondsBeforeTestingConnections != null) {
            this.idleTimeInSecondsBeforeTestingConnections = idleTimeInSecondsBeforeTestingConnections;
        }
    }

    protected void setDefaultCatalogName( String defaultCatalogName ) {
        if (defaultCatalogName != null) {
            this.defaultCatalogName = defaultCatalogName;
        }
    }

    protected void setDefaultSchemaName( String defaultSchemaName ) {
        if (defaultSchemaName != null) {
            this.defaultSchemaName = defaultSchemaName;
        }
    }

    protected void setMetadataCollectorClassName( String metadataCollectorClassName ) {
        if (metadataCollectorClassName != null) {
            this.metadataCollectorClassName = metadataCollectorClassName;
        }
    }
}

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
package org.modeshape.persistence.relational;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.EnumSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.common.database.DatabaseUtil;
import org.modeshape.common.logging.Logger;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Class which handles the configuration and actual database discovery for a {@link RelationalDb}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class DataSourceManager {

    private static final Logger LOGGER = Logger.getLogger(DataSourceManager.class);

    private static final EnumSet<DatabaseType.Name> SUPPORTED_DBS = EnumSet.of(DatabaseType.Name.H2,
                                                                               DatabaseType.Name.MYSQL,
                                                                               DatabaseType.Name.POSTGRES,
                                                                               DatabaseType.Name.ORACLE,
                                                                               DatabaseType.Name.SQLSERVER);

    private final DataSource dataSource;
    private final DatabaseType dbType;

    protected DataSourceManager(RelationalDbConfig config) {
        String jndiName = config.datasourceJNDIName();
        if (jndiName != null) {
            dataSource = getFromJndi(jndiName);
        } else {
            dataSource = createManagedDS(config);
        }
        
        try (Connection connection = newConnection(false)) {
            DatabaseMetaData metaData = connection.getMetaData();
            dbType = DatabaseUtil.determineType(metaData);
            if (!SUPPORTED_DBS.contains(dbType.name())) {
                throw new RelationalProviderException(RelationalProviderI18n.unsupportedDBError, dbType);
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    private DataSource createManagedDS(RelationalDbConfig config) {
        DataSource dataSource = null;
        String connectionUrl = config.connectionUrl();
        String driver = config.driver();
        String userName = config.username();
        String password = config.password();
        LOGGER.debug("Attempting to connect to '{0}' with '{1}' for username '{2}' and password '{3}'", connectionUrl,
                     driver, userName, password);
        try {
            ComboPooledDataSource c3p0Ds = new ComboPooledDataSource();
            c3p0Ds.setJdbcUrl(connectionUrl);
            c3p0Ds.setDriverClass(driver);
            c3p0Ds.setUser(userName);
            c3p0Ds.setPassword(password);
            c3p0Ds.setMaxPoolSize(1000);
            c3p0Ds.setInitialPoolSize(10);
            dataSource = c3p0Ds;
        } catch (PropertyVetoException e) {
            throw new RelationalProviderException(e);
        }
        return dataSource;
    }

    private DataSource getFromJndi(String jndiName) {
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            Object result = initialContext.lookup(jndiName);
            if (!(result instanceof DataSource)) {
                throw new RelationalProviderException(RelationalProviderI18n.jndiError, jndiName, " incorrect type: " + result.getClass().getName());
            }
            return (DataSource) result;
        } catch (NamingException e) {
            throw new RelationalProviderException(e);
        } finally {
            if (initialContext != null) {
                try {
                    initialContext.close();
                } catch (NamingException e) {
                    LOGGER.debug(e, "Cannot close JNDI context");
                }
            }
        }
    }

    protected DatabaseType dbType() {
        return dbType;
    }
    
    protected Connection newConnection(boolean autocommit) {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(autocommit);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return connection;
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    @Override
    public String toString() {
        return "DataSourceManager[" + "dataSource=" + dataSource + ", dbType=" + dbType + ']';
    }
}

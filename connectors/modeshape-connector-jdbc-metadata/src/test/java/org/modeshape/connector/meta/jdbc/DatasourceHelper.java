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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.jcr.store.DataSourceConfig;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test helper which executes DDL statements from a file against a given datasource.
 */
public class DatasourceHelper {

    private static final DataSourceConfig DATA_SOURCE_CONFIG = new DataSourceConfig();
    private static ComboPooledDataSource dataSource;
    private static boolean print = false;

    /**
     * Sets a flag which allows debug-type information to be printed.
     *
     * @param print a boolean flag
     */
    public static void setPrint( boolean print ) {
        DatasourceHelper.print = print;
    }

    /**
     * Runs the ddl contained in the given filename, against the given data source.
     *
     * @param fileName a non-null string
     * @throws Exception if anything fails
     */
    public static void executeDdl( String fileName ) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        InputStream istream = null;
        BufferedReader reader = null;

        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();

            String filePath = "/" + DATA_SOURCE_CONFIG.getDatabase() + "/" + fileName;
            istream = DatasourceHelper.class.getResourceAsStream(filePath);
            assertThat(filePath + " cannot be found", istream, is(notNullValue()));
            reader = new BufferedReader(new InputStreamReader(istream));

            /*
             * We have to send the DDL line-at-a-time because the MySQL driver doesn't like getting multiple DDL statements at once
             */
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("--")) {
                    if (print) {
                        System.out.println("Executing: " + line);
                    }
                    stmt.execute(line);
                }
            }

        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception ignore) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignore) {
                }
            }
            if (istream != null) {
                try {
                    istream.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Creates a data source using C3P0.
     *
     * @return a {@link DataSource} instance, never {@code null}
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
            try {
                dataSource.setDriverClass(DATA_SOURCE_CONFIG.getDriverClassName());
                dataSource.setJdbcUrl(DATA_SOURCE_CONFIG.getUrl());
                dataSource.setUser(DATA_SOURCE_CONFIG.getUsername());
                dataSource.setPassword(DATA_SOURCE_CONFIG.getPassword());
                dataSource.setMaxStatements(DATA_SOURCE_CONFIG.getMaximumSizeOfStatementsCache());
                dataSource.setAcquireRetryAttempts(DATA_SOURCE_CONFIG.getRetryLimit());
                dataSource.setMaxIdleTime(DATA_SOURCE_CONFIG.getMaximumConnectionIdleTimeInSeconds());
                dataSource.setAcquireIncrement(DATA_SOURCE_CONFIG.getNumberOfConnectionsToAcquireAsNeeded());
                dataSource.setMinPoolSize(DATA_SOURCE_CONFIG.getMinimumConnectionsInPool());
                dataSource.setMaxPoolSize(DATA_SOURCE_CONFIG.getMaximumConnectionsInPool());

            } catch (PropertyVetoException pve) {
                throw new RuntimeException(pve);
            }
        }
        return dataSource;
    }

    public static void bindInJNDI( String name ) throws NamingException {
        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.commons.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        InitialContext ic = new InitialContext();

        ic.createSubcontext("java:");
        ic.bind("java:/" + name, dataSource);
    }

    public static void closeDataSource() {
        dataSource.close();
    }
}

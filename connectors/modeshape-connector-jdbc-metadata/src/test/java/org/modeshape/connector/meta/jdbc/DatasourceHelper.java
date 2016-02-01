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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
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
import com.zaxxer.hikari.HikariDataSource;

/**
 * Test helper which executes DDL statements from a file against a given datasource.
 */
public class DatasourceHelper {

    private static final DataSourceConfig DATA_SOURCE_CONFIG = new DataSourceConfig();
    private static HikariDataSource dataSource;
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
            dataSource = new HikariDataSource();
            dataSource.setDriverClassName(DATA_SOURCE_CONFIG.getDriverClassName());
            dataSource.setJdbcUrl(DATA_SOURCE_CONFIG.getUrl());
            dataSource.setUsername(DATA_SOURCE_CONFIG.getUsername());
            dataSource.setPassword(DATA_SOURCE_CONFIG.getPassword());
            dataSource.setIdleTimeout(DATA_SOURCE_CONFIG.getMaximumConnectionIdleTimeInSeconds() * 1000);
            dataSource.setMaximumPoolSize(DATA_SOURCE_CONFIG.getMaximumConnectionsInPool());
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

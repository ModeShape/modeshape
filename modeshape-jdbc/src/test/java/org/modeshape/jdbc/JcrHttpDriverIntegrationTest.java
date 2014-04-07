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
package org.modeshape.jdbc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * Integration test with the http protocol of the {@link JcrDriver}.
 *
 * Please note that this class only performs a series of "smoke tests" against a running remote repository and is nowhere near
 * complete from a point of view of testing a {@link java.sql.Driver} implementation.
 *
 * @author Horia Chiorean
 */
public class JcrHttpDriverIntegrationTest  {

    private Properties driverProperties = new Properties();
    private JcrDriver driver = new JcrDriver(null);

    @Before
    public void before() throws Exception {
        DriverManager.registerDriver(driver);

        driverProperties.setProperty(JcrDriver.USERNAME_PROPERTY_NAME, getUserName());
        driverProperties.setProperty(JcrDriver.PASSWORD_PROPERTY_NAME, getPassword());
    }

    @After
    public void after() throws Exception {
        DriverManager.deregisterDriver(driver);
    }

    @Test
    public void shouldCreateConnectionToRemoteServer() throws SQLException {
        Connection connection = connectToRemoteRepository();
        assertTrue(connection instanceof JcrConnection);
        assertTrue(connection.unwrap(JcrConnection.class) != null);
        assertFalse(connection.isClosed());
    }

    @Test(expected = SQLException.class)
    public void shouldNotConnectWithInvalidRepositoryName() throws Exception {
        DriverManager.getConnection(getContextPathUrl() + "/dummy", driverProperties);
    }

    @Test
    @FixFor( "MODE-2125" )
    public void shouldRetrieveMetaData() throws SQLException {
        Connection connection = connectToRemoteRepository();
        DatabaseMetaData metadata = connection.getMetaData();
        assertNotNull(metadata);

        //reads tables and columns
        readTables(metadata);
    }

    private void readTables( DatabaseMetaData metadata ) throws SQLException {
        ResultSet tables = metadata.getTables(null, null, null, null);
        try {
            while (tables.next()) {
                String tableCatalog = tables.getString(1);
                String tableSchema = tables.getString(2);
                String tableName = tables.getString(3);
                String tableType = tables.getString(4);
                String remarks = tables.getString(5);
                String typeCat = tables.getString(6);
                String typeSchema = tables.getString(7);
                String typeName = tables.getString(8);
                String selfRefColumnName = tables.getString(9);
                String refGeneration = tables.getString(10);

                readColumns(metadata, tableCatalog, tableSchema, tableName);
            }
        } finally {
            tables.close();
        }
    }

    private void readColumns( DatabaseMetaData metadata, String catalog, String schema, String table )
            throws SQLException {
        ResultSet columns = metadata.getColumns(catalog, schema, table, null);
        try {
            while (columns.next()) {
                String tableCatalog = columns.getString(1);
                String tableSchema = columns.getString(2);
                String tableName = columns.getString(3);
                String columnName = columns.getString(4);
                int type = columns.getInt(5);
                String typeName = columns.getString(6);
                int columnSize = columns.getInt(7);
                int bufferLength = columns.getInt(8);
                int decimalDigits = columns.getInt(9);
                int radix = columns.getInt(10);
                int nullable = columns.getInt(11);
                String remarks = columns.getString(12);
                String columnDef = columns.getString(13);
                int sqlDataType = columns.getInt(14);
                int sqlDataTimeSub = columns.getInt(15);
                int charOctetLength = columns.getInt(16);
                int ordinalPost = columns.getInt(17);
                String isNullable = columns.getString(18);
                String scopeCat = columns.getString(19);
                String scopeSchema = columns.getString(20);
                String scopeTable = columns.getString(21);
                short scopeDataType = columns.getShort(22);
//                String isAutoIncrement = columns.getString(23);
//                String isGenerated = columns.getString(24);
            }
        } finally {
            columns.close();
        }
    }

    @Test
    @FixFor("MODE-872")
    public void shouldReturnResultsFromSimpleQuery() throws Exception {
        Connection connection = connectToRemoteRepository();
        String query = "SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [nt:base] WHERE [jcr:path] LIKE '/%' AND [jcr:path] NOT LIKE '/%/%'ORDER BY [jcr:path]";
        String[] expectedResults = new String[] {
                "jcr:path[STRING]    jcr:name[STRING]    mode:depth[LONG]    mode:localName[STRING]    jcr:mixinTypes[STRING]    jcr:primaryType[STRING]",
                "/        0        null    mode:root",
                "/jcr:system    jcr:system    1    system    null    mode:system"
        };
        ConnectionResultsComparator.executeTest(connection, query, expectedResults, 3);
    }

    @Test
    @FixFor("MODE-872")
    public void shouldReturnEmptyResultSetWhenNoResultsFoundForQuery() throws Exception {
        Connection connection = connectToRemoteRepository();
        String query = "SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [mix:versionable] ORDER BY [jcr:path]";

        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();
            rs = statement.executeQuery(query);

            assertNotNull(rs);
            assertFalse(rs.next());
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    protected String getContextPathUrl() {
        //must match Cargo's configuration
        return "localhost:8090/modeshape/v1";
    }

    protected String getRepositoryName() {
        //must match the configuration from modeshape-web-jcr-rest-war
        return "repo";
    }

    protected String getWorkspaceName() {
        //must match the configuration from modeshape-web-jcr-rest-war
        return "default";
    }

    protected String getUserName() {
        //must match Cargo's configuration
        return "dnauser";
    }

    protected String getPassword() {
        //must match Cargo's configuration
        return "password";
    }

    private String getRepositoryUrl() {
        return JcrDriver.HTTP_URL_PREFIX + getContextPathUrl() + "/" + getRepositoryName() + "/" + getWorkspaceName();
    }

    private Connection connectToRemoteRepository() throws SQLException {
        Connection connection = DriverManager.getConnection(getRepositoryUrl(), driverProperties);
        assertNotNull(connection);
        return connection;
    }
}

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

import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Integration test with the http protocol of the {@link JcrDriver}.
 *
 * Please note that this class only performs a series of "smoke tests" against a running remote repository and is nowhere near
 * complete from a point of view of testing a {@link java.sql.Driver} implementation.
 *
 * @author Horia Chiorean
 */
public class JcrHttpDriverIntegrationTest  {

    /**
     * Constants which are needed to connect to the remote repository and therefore, must match Cargo's configuration parameters
     */
    private static final String SERVER_URL = JcrDriver.HTTP_URL_PREFIX + "localhost:8090/modeshape/test";
    private static final String REPOSITORY_NAME = "repo";
    private static final String REPOSITORY_URL = SERVER_URL + "/" + REPOSITORY_NAME + "/default";

    private Properties driverProperties = new Properties();
    private JcrDriver driver = new JcrDriver(null);

    @Before
    public void before() throws Exception {
        DriverManager.registerDriver(driver);

        driverProperties.setProperty(JcrDriver.USERNAME_PROPERTY_NAME, "dnauser");
        driverProperties.setProperty(JcrDriver.PASSWORD_PROPERTY_NAME, "password");
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
        DriverManager.getConnection(SERVER_URL + "/dummy", driverProperties);
    }

    @Test
    public void shouldRetrieveMetaData() throws SQLException {
        Connection connection = connectToRemoteRepository();
        DatabaseMetaData metaData = connection.getMetaData();
        assertNotNull(metaData);
    }

    @Test
    @FixFor("MODE-872")
    public void shouldReturnResultsFromSimpleQuery() throws Exception {
        Connection connection = connectToRemoteRepository();
        String query = "SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [nt:base] ORDER BY [jcr:path]";
        String[] expectedResults = new String[] {
                "jcr:path[STRING]    jcr:name[STRING]    mode:depth[LONG]    mode:localName[STRING]    jcr:mixinTypes[STRING]    jcr:primaryType[STRING]",
                "/        0        null    mode:root",
                "/jcr:system    jcr:system    1    system    null    mode:system",
        };
        ConnectionResultsComparator.executeTest(connection, query, expectedResults, 4);
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

    private Connection connectToRemoteRepository() throws SQLException {
        Connection connection = DriverManager.getConnection(REPOSITORY_URL, driverProperties);
        assertNotNull(connection);
        return connection;
    }
}

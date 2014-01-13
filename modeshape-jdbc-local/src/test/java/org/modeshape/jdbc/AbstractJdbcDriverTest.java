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
package org.modeshape.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the
 * {@link ModeShapeEngine}. Essentially this is an integration test, but it does test lower-level functionality of the
 * implementation of the JCR interfaces related to querying. (It is simply more difficult to unit test these implementations
 * because of the difficulty in mocking the many other components to replicate the same functionality.)
 * <p>
 * Also, because queries are read-only, the engine is set up once and used for the entire set of test methods.
 * </p>
 * <p>
 * The following are the SQL semantics that the tests will be covering:
 * <li>variations of simple SELECT * FROM</li>
 * <li>JOIN
 * </p>
 * <p>
 * To create the expected results to be used to run a test, use the test and print method: example:
 * DriverTestUtil.executeTestAndPrint(this.connection, "SELECT * FROM [nt:base]"); This will print the expected results like this:
 * String[] expected = { "jcr:primaryType[STRING]", "mode:root", "car:Car", "car:Car", "nt:unstructured" } Now copy the expected
 * results to the test method. Then change the test to run the executeTest method passing in the <code>expected</code> results:
 * example: DriverTestUtil.executeTest(this.connection, "SELECT * FROM [nt:base]", expected);
 * </p>
 */
public abstract class AbstractJdbcDriverTest extends MultiUseAbstractTest {

    protected Driver driver;
    protected Connection connection;
    protected DatabaseMetaData dbmd;
    protected ConnectionResultsComparator resultsComparator;

    @BeforeClass
    public static void beforeAll() throws Exception {
        RepositoryConfiguration config = new RepositoryConfiguration("cars");
        startRepository(config);

        registerNodeTypes("cars.cnd");
        importContent("/", "cars-system-view.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        // Use a session to load the contents ...
        Session session = repository.login();
        try {
            // Create a branch that contains some same-name-siblings ...
            Node other = session.getRootNode().addNode("Other", "nt:unstructured");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value3 quick brown fox");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value2 quick brown cat");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value1 quick black dog");
            session.getRootNode().addNode("NodeB", "nt:unstructured").setProperty("myUrl", "http://www.acme.com/foo/bar");
            session.save();

        } finally {
            session.logout();
        }
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        this.resultsComparator = new ConnectionResultsComparator();

        Properties properties = createConnectionProperties(repository);
        String url = createConnectionUrl(repository);
        driver = createDriver(repository);
        connect(url, properties);

        dbmd = this.connection.getMetaData();

        // only test were comparing metadata is not available at this time
        resultsComparator.compareColumns = true;
    }

    @Override
    @After
    public void afterEach() throws Exception {
        try {
            if (connection != null) {
                try {
                    connection.close();
                } finally {
                    connection = null;
                    dbmd = null;
                    driver = null;
                }
            }
            super.afterEach();
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    protected Properties createConnectionProperties( JcrRepository repository ) throws Exception {
        return new Properties();
    }

    /**
     * Subclasses should override this method and return a default URL for connecting the JDBC driver to the given repository.
     * 
     * @param repository the repository instance to which the driver should connect
     * @return the connection URL
     * @throws Exception if there is an exception
     */
    protected abstract String createConnectionUrl( JcrRepository repository ) throws Exception;

    /**
     * Subclasses should override this method and return a default URL for connecting the JDBC driver to the given repository.
     * 
     * @param repository the repository instance to which the driver should connect
     * @return the connection URL
     * @throws Exception if there is an exception
     */
    protected abstract Driver createDriver( JcrRepository repository ) throws Exception;

    /**
     * Establish a JDBC connection. This can be called by test methods, although it is called by default in the
     * {@link #beforeEach()} method.
     * 
     * @param url the connection URL; may not be null
     * @return the connection
     * @throws Exception if there is an exception
     */
    protected Connection connect( String url ) throws Exception {
        return connect(url, null);
    }

    /**
     * Establish a JDBC connection. This can be called by test methods, although it is called by default in the
     * {@link #beforeEach()} method.
     * 
     * @param url the connection URL; may not be null
     * @param properties the connection properties; may be null
     * @return the connection
     * @throws Exception if there is an exception
     */
    protected Connection connect( String url,
                                  Properties properties ) throws Exception {
        if (properties == null) properties = new Properties();
        Connection newConnection = driver.connect(url, properties);
        if (connection != null) {
            connection.close();
        }
        connection = newConnection;
        return connection;
    }

    @Test
    public void shouldStartUp() {
        assertThat(session, is(notNullValue()));
        assertThat(connection, is(notNullValue()));
    }

    public void executeQuery( String sql,
                              String[] expected,
                              int expectedRowCount ) throws SQLException {
        executeQuery(sql, expected, expectedRowCount, QueryLanguage.JCR_SQL2);
    }

    public void executeQuery( String sql,
                              String[] expected,
                              int expectedRowCount,
                              String language ) throws SQLException {
        ConnectionResultsComparator.executeTest(connection, sql, expected, expectedRowCount, language);
    }

    public void assertResultsSetEquals( final ResultSet resultSet,
                                        final String expected ) {
        resultsComparator.assertResultsSetEquals(resultSet, expected);
    }

    public void assertResultsSetEquals( ResultSet resultSet,
                                        String[] expected ) {
        resultsComparator.assertResultsSetEquals(resultSet, expected);
    }

    public void assertRowCount( int expected ) {
        resultsComparator.assertRowCount(expected);
    }
}

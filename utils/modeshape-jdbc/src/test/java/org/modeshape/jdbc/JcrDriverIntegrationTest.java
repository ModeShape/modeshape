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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.naming.Context;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.JcrRepository.QueryLanguage;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the JcrEngine.
 * Essentially this is an integration test, but it does test lower-level functionality of the implementation of the JCR interfaces
 * related to querying. (It is simply more difficult to unit test these implementations because of the difficulty in mocking the
 * many other components to replicate the same functionality.)
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
public class JcrDriverIntegrationTest {

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrDriverIntegrationTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrDriverIntegrationTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static String[] carColumnNames() {
        return new String[] {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine", "car:mpgHighway",
            "car:valueRating", "jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model", "car:msrp"};
    }

    @Mock
    private Context jndi;

    private JcrDriver driver;
    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static JcrRepository repository;
    private JcrConnection connection;

    private static String jndiNameForRepository = "java:MyRepository";
    private static String validUrl = "jdbc:jcr:jndi://" + jndiNameForRepository;
    private JcrDriver.JndiContextFactory contextFactory;

    @SuppressWarnings( "deprecation" )
    @BeforeClass
    public static void beforeAll() throws Exception {

        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     // Added ADMIN privilege to allow permanent namespace registration in one of the tests
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                ModeShapeRoles.READONLY + "," + ModeShapeRoles.READWRITE + "," + ModeShapeRoles.ADMIN)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        engine = configuration.build();
        engine.start();

        // Initialize the JAAS configuration to allow for an admin login later
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Start the repository ...
        repository = engine.getRepository("cars");

        // Use a session to load the contents ...
        Session session = repository.login();
        try {
            InputStream stream = resourceStream("cars-system-view.xml");
            assertNotNull(stream);

            try {
                session.getWorkspace().importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                stream.close();
            }

            // Create a branch that contains some same-name-siblings ...
            Node other = session.getRootNode().addNode("Other", "nt:unstructured");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value3 quick brown fox");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value2 quick brown cat");
            other.addNode("NodeA", "nt:unstructured").setProperty("something", "value1 quick black dog");
            session.getRootNode().addNode("NodeB", "nt:unstructured").setProperty("myUrl", "http://www.acme.com/foo/bar");
            session.save();

            // Prime creating a first XPath query and SQL query ...
            session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
            session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", JcrRepository.QueryLanguage.JCR_SQL2);
        } finally {
            session.logout();
        }

    }

    @AfterClass
    public static void afterAll() throws Exception {

        engine.shutdown();
        engine.awaitTermination(3, TimeUnit.SECONDS);
        engine = null;
        configuration = null;
    }

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(jndi.lookup(jndiNameForRepository)).thenReturn(repository);

        contextFactory = new JcrDriver.JndiContextFactory() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Context createContext( Properties properties ) {
                return jndi;
            }
        };

        driver = new JcrDriver();
        driver.setContextFactory(contextFactory);

        Properties validProperties = new Properties();
        // validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, "admin");
        // "<anonymous>");
        // "jsmith");
        // validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, "admin");

        connection = (JcrConnection)driver.connect(validUrl, validProperties);

    }

    @After
    public void afterEach() throws Exception {
        DriverManager.deregisterDriver(driver);

        if (connection != null) {
            try {
                connection.close();
            } finally {
                connection = null;
            }
        }
        driver = null;
        contextFactory = null;
    }

    @Test
    public void shouldStartUp() {
        assertThat(engine.getRepositoryService(), is(notNullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllNodes() throws SQLException {
        String[] expected = {"jcr:primaryType[STRING]", "mode:root", "car:Car", "car:Car", "nt:unstructured", "nt:unstructured",
            "car:Car", "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "nt:unstructured", "car:Car",
            "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured"};

        DriverTestUtil.executeTest(this.connection, "SELECT * FROM [nt:base]", expected, 23);

    }

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllCars() throws SQLException {

        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",};

        DriverTestUtil.executeTest(this.connection, "SELECT * FROM [car:Car]", expected, 12);
    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseUsingDefault() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car"};

        DriverTestUtil.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:maker]", expected, 12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseAsc() throws SQLException {
        String[] expected = {"car:model[STRING]", "Altima", "Continental", "DB9", "DTS", "F-150", "G37", "H3", "Highlander",
            "IS350", "LR2", "LR3", "Prius"};

        DriverTestUtil.executeTest(this.connection,
                                   "SELECT car.[car:model] FROM [car:Car] As car WHERE car.[car:model] IS NOT NULL ORDER BY car.[car:model] ASC",
                                   expected,
                                   12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderedByClauseDesc() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car"};
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        DriverTestUtil.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC", expected, 12);

    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws SQLException {

        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        DriverTestUtil.executeTest(this.connection,
                                   "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
                                   expected,
                                   3);

    }

    /**
     * JIRA: https://jira.jboss.org/browse/MODE-772
     * 
     * @throws SQLException
     */
    @Ignore
    @Test
    public void shouldBeAbleToExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws SQLException {
	String[] expected = {
		"jcr:path[String]    jcr:score[String]    jcr:primaryType[STRING]",
		"/Cars/Utility    1.0    nt:unstructured",
		"/Cars/Hybrid    1.0    nt:unstructured",
		"/Cars/Sports    1.0    nt:unstructured",
		"/Cars/Luxury    1.0    nt:unstructured"
		};
	
        DriverTestUtil.executeTest(this.connection,
                                           "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'", expected, 4);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryToFindAllUnstructuredNodes() throws SQLException {
        String[] expected = {"jcr:primaryType[STRING]", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car",
            "car:Car", "car:Car", "car:Car"};

        DriverTestUtil.executeTest(this.connection, "SELECT * FROM [nt:unstructured]", expected, 22);

    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws SQLException
     */
    @Test
    public void shouldBeAbleToExecuteSqlQueryWithChildAxisCriteria() throws SQLException {
        String[] expected = {"jcr:path[String]    jcr:score[String]    jcr:primaryType[STRING]",
            "/Cars/Utility    1.0    nt:unstructured", "/Cars/Hybrid    1.0    nt:unstructured",
            "/Cars/Sports    1.0    nt:unstructured", "/Cars/Luxury    1.0    nt:unstructured"};
        DriverTestUtil.executeTest(this.connection,
                                   "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%' ",
                                   expected,
                                   4,
                                   QueryLanguage.JCR_SQL);

    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws SQLException
     */
    @Test
    public void shouldBeAbleToExecuteSqlQueryWithContainsCriteria() throws SQLException {
        String[] expected = {"jcr:path[String]    jcr:score[String]    jcr:primaryType[STRING]",
            "/Cars/Utility    1.0    nt:unstructured", "/Cars/Hybrid    1.0    nt:unstructured",
            "/Cars/Sports    1.0    nt:unstructured", "/Cars/Luxury    1.0    nt:unstructured"};

        DriverTestUtil.executeTest(this.connection,
                                   "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                   expected,
                                   4,
                                   QueryLanguage.JCR_SQL);

    }

}

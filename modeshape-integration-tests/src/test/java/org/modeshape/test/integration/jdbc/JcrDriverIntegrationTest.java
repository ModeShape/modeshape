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
package org.modeshape.test.integration.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.modeshape.jdbc.ConnectionResultsComparator.executeTest;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.naming.Context;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jdbc.ConnectionResultsComparator;
import org.modeshape.jdbc.JcrConnection;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.util.ResultsComparator;
import org.modeshape.test.integration.AbstractMultiUseModeShapeTest;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the JcrEngine.
 * Essentially this is an integration test, but it does test lower-level functionality of the implementation of the JCR interfaces
 * related to querying.
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
public class JcrDriverIntegrationTest extends AbstractMultiUseModeShapeTest {

    private static String jndiNameForRepository = "jcr/local";
    private static String validUrl = JcrDriver.JNDI_URL_PREFIX + jndiNameForRepository + "?repositoryName=Repo";

    @Mock
    private Context jndi;
    private JcrDriver driver;
    private JcrConnection connection;
    private DatabaseMetaData dbmd;
    private ResultsComparator results;

    private JcrDriver.JcrContextFactory contextFactory;

    @BeforeClass
    public static void beforeAll() throws Exception {
        // // Initialize the JAAS configuration to allow for an admin login later
        // // Initialize IDTrust
        // String configFile = "security/jaas.conf.xml";
        // IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        //
        // try {
        // idtrustConfig.config(configFile);
        // } catch (Exception ex) {
        // throw new IllegalStateException(ex);
        // }
        startEngine(JcrDriverIntegrationTest.class, "config/configRepositoryForJdbc.xml", "Repo");
        importContent(JcrDriverIntegrationTest.class, "jdbc/cars-system-view-with-uuids.xml");

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

    @AfterClass
    public static void afterAll() throws Exception {
        stopEngine();
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Set up the mock JNDI context ...
        MockitoAnnotations.initMocks(this);
        when(jndi.lookup(jndiNameForRepository)).thenReturn(repository);
        contextFactory = new JcrDriver.JcrContextFactory() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Context createContext( Properties properties ) {
                return jndi;
            }
        };

        // Set up the driver and connection ...
        driver = new JcrDriver();
        driver.setContextFactory(contextFactory);
        connection = (JcrConnection)driver.connect(validUrl, new Properties());

        // And get database metadata ...
        dbmd = this.connection.getMetaData();
        results = new ResultsComparator();
    }

    @Override
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
        dbmd = null;
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
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "mode:root    /        1.0        0", "car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.0    Hummer H3    3",
            "car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.0    Infiniti G37    3",
            "nt:unstructured    /Cars/Utility    Utility    1.0    Utility    2",
            "nt:unstructured    /Cars    Cars    1.0    Cars    1",
            "car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.0    Cadillac DTS    3",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.0    Hybrid    2",
            "car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.0    Nissan Altima    3",
            "car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.0    Land Rover LR2    3",
            "car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.0    Toyota Prius    3",
            "car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.0    Ford F-150    3",
            "nt:unstructured    /Cars/Sports    Sports    1.0    Sports    2",
            "car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.0    Aston Martin DB9    3",
            "nt:unstructured    /Cars/Luxury    Luxury    1.0    Luxury    2",
            "car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.0    Bentley Continental    3",
            "car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.0    Land Rover LR3    3",
            "car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.0    Toyota Highlander    3",
            "car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.0    Lexus IS350    3",
            "nt:unstructured    /Other/NodeA[2]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA    NodeA    1.0    NodeA    2",
            "nt:unstructured    /NodeB    NodeB    1.0    NodeB    1",
            "nt:unstructured    /Other/NodeA[3]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other    Other    1.0    Other    1"};

        executeTest(this.connection, "SELECT * FROM [nt:base]", expected, 23);
    }

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllCars() throws SQLException {

        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.5705448389053345    Hummer H3    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.5705448389053345    Infiniti G37    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.5705448389053345    Cadillac DTS    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.5705448389053345    Nissan Altima    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.5705448389053345    Land Rover LR2    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.5705448389053345    Toyota Prius    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.5705448389053345    Ford F-150    3",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.5705448389053345    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.5705448389053345    Bentley Continental    3",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.5705448389053345    Land Rover LR3    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.5705448389053345    Toyota Highlander    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.5705448389053345    Lexus IS350    3"};
        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car]", expected, 12);
    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseUsingDefault() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.5705448389053345    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.5705448389053345    Bentley Continental    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.5705448389053345    Cadillac DTS    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.5705448389053345    Ford F-150    3",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.5705448389053345    Hummer H3    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.5705448389053345    Infiniti G37    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.5705448389053345    Land Rover LR2    3",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.5705448389053345    Land Rover LR3    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.5705448389053345    Lexus IS350    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.5705448389053345    Nissan Altima    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.5705448389053345    Toyota Prius    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.5705448389053345    Toyota Highlander    3"};

        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:maker]", expected, 12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseAsc() throws SQLException {
        String[] expected = {"car:model[STRING]", "Altima", "Continental", "DB9", "DTS", "F-150", "G37", "H3", "Highlander",
            "IS350", "LR2", "LR3", "Prius"};

        ConnectionResultsComparator.executeTest(this.connection,
                                                "SELECT car.[car:model] FROM [car:Car] As car WHERE car.[car:model] IS NOT NULL ORDER BY car.[car:model] ASC",
                                                expected,
                                                12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderedByClauseDesc() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.5705448389053345    Land Rover LR3    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.5705448389053345    Lexus IS350    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.5705448389053345    Infiniti G37    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.5705448389053345    Toyota Highlander    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.5705448389053345    Land Rover LR2    3",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.5705448389053345    Hummer H3    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.5705448389053345    Ford F-150    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.5705448389053345    Toyota Prius    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.5705448389053345    Nissan Altima    3",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.5705448389053345    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.5705448389053345    Bentley Continental    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.5705448389053345    Cadillac DTS    3"};
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC", expected, 12);

    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws SQLException {

        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        ConnectionResultsComparator.executeTest(this.connection,
                                                "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
                                                expected,
                                                3);

    }

    /*
     * FixFor( "MODE-722" )
     */
    @Test
    public void shouldBeAbleToExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws SQLException {
        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        ConnectionResultsComparator.executeTest(this.connection,
                                                "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'",
                                                expected,
                                                3);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryToFindAllUnstructuredNodes() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    0.003934855107218027    Nissan Altima    3",
            "car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    0.003934855107218027    Toyota Highlander    3",
            "car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    0.003934855107218027    Toyota Prius    3",
            "car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    0.003934855107218027    Bentley Continental    3",
            "car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    0.003934855107218027    Cadillac DTS    3",
            "car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    0.003934855107218027    Lexus IS350    3",
            "car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    0.003934855107218027    Aston Martin DB9    3",
            "car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    0.003934855107218027    Infiniti G37    3",
            "car:Car    /Cars/Utility/Ford F-150    Ford F-150    0.003934855107218027    Ford F-150    3",
            "car:Car    /Cars/Utility/Hummer H3    Hummer H3    0.003934855107218027    Hummer H3    3",
            "car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    0.003934855107218027    Land Rover LR2    3",
            "car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    0.003934855107218027    Land Rover LR3    3",
            "nt:unstructured    /Cars    Cars    0.004816451109945774    Cars    1",
            "nt:unstructured    /Cars/Hybrid    Hybrid    0.004816451109945774    Hybrid    2",
            "nt:unstructured    /Cars/Luxury    Luxury    0.004816451109945774    Luxury    2",
            "nt:unstructured    /Cars/Sports    Sports    0.004816451109945774    Sports    2",
            "nt:unstructured    /Cars/Utility    Utility    0.004816451109945774    Utility    2",
            "nt:unstructured    /NodeB    NodeB    0.004816451109945774    NodeB    1",
            "nt:unstructured    /Other    Other    0.004816451109945774    Other    1",
            "nt:unstructured    /Other/NodeA    NodeA    0.004816451109945774    NodeA    2",
            "nt:unstructured    /Other/NodeA[2]    NodeA    0.004816451109945774    NodeA    2",
            "nt:unstructured    /Other/NodeA[3]    NodeA    0.004816451109945774    NodeA    2",};
        ConnectionResultsComparator.executeTest(this.connection,
                                                "SELECT * FROM [nt:unstructured] ORDER BY [jcr:primaryType], [jcr:path]",
                                                expected,
                                                22);
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws SQLException
     */
    @Test
    public void shouldBeAbleToExecuteSqlQueryWithChildAxisCriteria() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "nt:unstructured    /Cars/Utility    Utility    1.4142135381698608    Utility    2",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.4142135381698608    Hybrid    2",
            "nt:unstructured    /Cars/Sports    Sports    1.4142135381698608    Sports    2",
            "nt:unstructured    /Cars/Luxury    Luxury    1.4142135381698608    Luxury    2"};
        ConnectionResultsComparator.executeTest(this.connection,
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
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "nt:unstructured    /Cars/Utility    Utility    1.4142135381698608    Utility    2",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.4142135381698608    Hybrid    2",
            "nt:unstructured    /Cars/Sports    Sports    1.4142135381698608    Sports    2",
            "nt:unstructured    /Cars/Luxury    Luxury    1.4142135381698608    Luxury    2"};

        ConnectionResultsComparator.executeTest(this.connection,
                                                "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                                expected,
                                                4,
                                                QueryLanguage.JCR_SQL);

    }

    @Test
    public void shouldGetCatalogs() throws SQLException {
        results.compareColumns = false;
        String[] expected = {"TABLE_CAT[String]", "Repo"};

        ResultSet rs = dbmd.getCatalogs();
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(1);
    }

    @Test
    public void shouldGetTableTypes() throws SQLException {
        results.compareColumns = false;
        String[] expected = {"TABLE_TYPE[String]", "VIEW"};

        ResultSet rs = dbmd.getTableTypes();
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(1);
    }

    @Test
    public void shouldGetAllTables() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "Repo    NULL    car:Car    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:addColumnDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:addTableConstraintDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:alterColumnDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:alterDomainStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:alterTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:alterable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:assertionOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:characterSetOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:collationOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:columnDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:columnOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:columnReference    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:constraintAttribute    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:creatable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createAssertionStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createCharacterSetStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createCollationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createDomainStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createSchemaStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createTranslationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:createViewStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:ddlProblem    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:domainOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropAssertionStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropCharacterSetStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropCollationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropColumnDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropDomainStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropSchemaStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropTableConstraintDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropTranslationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:dropViewStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:droppable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:fkColumnReference    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantOnCharacterSetStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantOnCollationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantOnDomainStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantOnTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantOnTranslationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantPrivilege    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:grantee    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:insertStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:insertable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:operand    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:operation    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:referenceOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:renamable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeOnCharacterSetStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeOnCollationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeOnDomainStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeOnTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeOnTranslationStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:revokeStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:schemaOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:setStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:settable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:simpleProperty    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:statement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:statementOption    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:tableConstraint    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:tableConstraintOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:tableOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:tableReference    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:translationOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    ddl:viewOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:columnDefinition    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createFunctionStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createIndexStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createProcedureStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createRoleStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createSynonymStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:createTriggerStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:declareGlobalTemporaryTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropFunctionStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropIndexStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropProcedureStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropRoleStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropSynonymStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:dropTriggerStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:functionOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:functionParameter    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:grantOnFunctionStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:grantOnProcedureStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:grantRolesStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:indexColumnReference    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:indexOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:lockTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:procedureOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:renameTableStatement    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:roleName    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:roleOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:synonymOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    derbyddl:triggerOperand    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    jdbcs:imported    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    jdbcs:source    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:created    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:etag    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:language    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:lastModified    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:lifecycle    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:lockable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:managedRetention    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:mimeType    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:referenceable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:shareable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:simpleVersionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:title    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mix:versionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mmcore:annotated    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mmcore:import    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mmcore:model    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mmcore:tags    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:defined    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:derived    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:hashed    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:lock    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:locks    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:namespace    VIEW    Is Mixin: false    NULL    NULL    NULL    mode:uri    DERIVED",
            "Repo    NULL    mode:namespaces    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:publishArea    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "Repo    NULL    mode:root    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:share    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:system    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:versionHistoryFolder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    mode:versionStorage    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "Repo    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "Repo    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "Repo    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:accessPattern    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:baseTable    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:catalog    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:column    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:columnSet    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:foreignKey    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:index    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:logicalRelationship    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:logicalRelationshipEnd    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:primaryKey    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:procedure    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:procedureParameter    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:procedureResult    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:relationalEntity    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:relationship    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:schema    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:table    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:uniqueConstraint    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:uniqueKey    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    relational:view    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    transform:transformed    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    transform:withSql    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    vdb:marker    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    vdb:markers    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    vdb:model    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    vdb:virtualDatabase    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    xmi:model    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    xmi:referenceable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(438);
    }

    @Test
    public void shouldGetAndQueryAllTables() throws SQLException {
        ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
        List<String> tableNames = new ArrayList<String>();
        while (rs.next()) {
            tableNames.add(rs.getString("TABLE_NAME"));
        }
        assertThat(tableNames.size(), is(181));
        List<String> tablesWithProblems = new ArrayList<String>();
        for (String table : tableNames) {
            try {
                connection.createStatement().execute("SELECT * FROM [" + table + "] LIMIT 2");
            } catch (SQLException e) {
                tablesWithProblems.add(table);
            }
        }
        if (!tablesWithProblems.isEmpty()) System.out.println(tablesWithProblems);
        assertThat(tablesWithProblems.isEmpty(), is(true));
    }

    @Test
    public void shouldGetNTPrefixedTables() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "Repo    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "Repo    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "Repo    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "Repo    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "nt:%", new String[] {});
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(20);
    }

    @Test
    public void shouldGetResourceSuffixedTables() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "Repo    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "Repo    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%:resource", new String[] {});
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(20);
    }

    @Test
    public void shouldGetTablesThatContainNodeTpe() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "Repo    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "Repo    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%nodeType%", new String[] {});
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(2);
    }

    @Test
    public void shouldGetAllColumnsFor1Table() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "Repo    NULL    car:Car    car:engine    12    String    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:lengthInInches    8    Double    20    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:maker    12    String    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:model    12    String    50    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:mpgCity    -5    Long    20    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:mpgHighway    -5    Long    20    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:userRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:valueRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:wheelbaseInInches    8    Double    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:year    12    String    50    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:name    12    String    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:path    12    String    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:primaryType    12    String    20    NULL    0    0    1        NULL    0    0    0    14    NO    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:score    8    Double    20    NULL    0    0    2        NULL    0    0    0    15    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    mode:depth    -5    Long    20    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    mode:localName    12    String    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "%");

        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(17);

    }

    @Test
    public void shouldGetAllColumnsForTableWithOnlyPseudoColumns() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "Repo    NULL    mmcore:tags    jcr:name    12    String    20    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
            "Repo    NULL    mmcore:tags    jcr:path    12    String    50    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
            "Repo    NULL    mmcore:tags    jcr:score    8    Double    20    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
            "Repo    NULL    mmcore:tags    mode:depth    -5    Long    20    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
            "Repo    NULL    mmcore:tags    mode:localName    12    String    50    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "mmcore:tags", "%");

        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(17);

    }

    @Test
    public void shouldGetOnlyColumnsForCarPrefixedTables() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "Repo    NULL    car:Car    car:engine    12    String    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:lengthInInches    8    Double    20    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:maker    12    String    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:model    12    String    50    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:mpgCity    -5    Long    20    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:mpgHighway    -5    Long    20    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:userRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:valueRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:wheelbaseInInches    8    Double    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    car:year    12    String    50    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:name    12    String    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:path    12    String    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:primaryType    12    String    20    NULL    0    0    1        NULL    0    0    0    14    NO    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    jcr:score    8    Double    20    NULL    0    0    2        NULL    0    0    0    15    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    mode:depth    -5    Long    20    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
            "Repo    NULL    car:Car    mode:localName    12    String    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car%", "%");

        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(11);

    }

    @Test
    public void shouldGetOnlyMSRPColumnForCarTable() throws SQLException {
        results.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "Repo    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "car:msrp");
        results.assertResultsSetEquals(rs, expected);
        results.assertRowCount(1);

    }

    @FixFor( "MODE-981" )
    /*
     * The issue was the first read was cached, and after a new file was uploaded, subsequent reads did not see the new rows.
     * This was due to a new  session wasn't being used.
     */
    @Test
    public void shouldSequence2XmlFiles() throws Exception {
        String[] expected1 = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "mode:root    /        1.0        0", "nt:unstructured    /Cars    Cars    1.0    Cars    1",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.0    Hybrid    2",
            "car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.0    Nissan Altima    3",
            "car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.0    Toyota Highlander    3",
            "car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.0    Toyota Prius    3",
            "nt:unstructured    /Cars/Luxury    Luxury    1.0    Luxury    2",
            "car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.0    Bentley Continental    3",
            "car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.0    Cadillac DTS    3",
            "car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.0    Lexus IS350    3",
            "nt:unstructured    /Cars/Sports    Sports    1.0    Sports    2",
            "car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.0    Aston Martin DB9    3",
            "car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.0    Infiniti G37    3",
            "nt:unstructured    /Cars/Utility    Utility    1.0    Utility    2",
            "car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.0    Ford F-150    3",
            "car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.0    Hummer H3    3",
            "car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.0    Land Rover LR2    3",
            "car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.0    Land Rover LR3    3",
            "nt:unstructured    /NodeB    NodeB    1.0    NodeB    1", "nt:unstructured    /Other    Other    1.0    Other    1",
            "nt:unstructured    /Other/NodeA    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[2]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[3]    NodeA    1.0    NodeA    2",
            "nt:folder    /files    files    1.0    files    1",
            "nt:file    /files/docWithComments.xml    docWithComments.xml    1.0    docWithComments.xml    2",
            "nt:resource    /files/docWithComments.xml/jcr:content    jcr:content    1.0    content    3"};

        String[] expected2 = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "mode:root    /        1.0        0", "nt:unstructured    /Cars    Cars    1.0    Cars    1",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.0    Hybrid    2",
            "car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.0    Nissan Altima    3",
            "car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.0    Toyota Highlander    3",
            "car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.0    Toyota Prius    3",
            "nt:unstructured    /Cars/Luxury    Luxury    1.0    Luxury    2",
            "car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.0    Bentley Continental    3",
            "car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.0    Cadillac DTS    3",
            "car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.0    Lexus IS350    3",
            "nt:unstructured    /Cars/Sports    Sports    1.0    Sports    2",
            "car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.0    Aston Martin DB9    3",
            "car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.0    Infiniti G37    3",
            "nt:unstructured    /Cars/Utility    Utility    1.0    Utility    2",
            "car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.0    Ford F-150    3",
            "car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.0    Hummer H3    3",
            "car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.0    Land Rover LR2    3",
            "car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.0    Land Rover LR3    3",
            "nt:unstructured    /NodeB    NodeB    1.0    NodeB    1", "nt:unstructured    /Other    Other    1.0    Other    1",
            "nt:unstructured    /Other/NodeA    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[2]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[3]    NodeA    1.0    NodeA    2",
            "nt:folder    /files    files    1.0    files    1",
            "nt:file    /files/docWithComments.xml    docWithComments.xml    1.0    docWithComments.xml    2",
            "nt:resource    /files/docWithComments.xml/jcr:content    jcr:content    1.0    content    3",
            "nt:file    /files/docWithComments2.xml    docWithComments2.xml    1.0    docWithComments2.xml    2",
            "nt:resource    /files/docWithComments2.xml/jcr:content    jcr:content    1.0    content    3"};

        Session session = repository.login();
        try {

            this.setSession(session);

            uploadFile("docWithComments.xml", "/files/");
            Thread.sleep(1000L);

            print = true;

            ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [nt:base] ORDER BY [jcr:path]", expected1, 26);

            uploadFile("docWithComments2.xml", "/files/");

            ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [nt:base] ORDER BY [jcr:path]", expected2, 28);

        } finally {
            session.logout();
        }

    }

    @FixFor( "MODE-909" )
    @Test
    public void shouldSequence2DdlFiles() throws Exception {
        String[] expected1 = {
            "jcr:primaryType[STRING]    jcr:path[PATH]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "car:Car    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.0    Nissan Altima    3",
            "car:Car    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.0    Toyota Highlander    3",
            "car:Car    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.0    Toyota Prius    3",
            "car:Car    /Cars/Luxury/Bentley Continental    Bentley Continental    1.0    Bentley Continental    3",
            "car:Car    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.0    Cadillac DTS    3",
            "car:Car    /Cars/Luxury/Lexus IS350    Lexus IS350    1.0    Lexus IS350    3",
            "car:Car    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.0    Aston Martin DB9    3",
            "car:Car    /Cars/Sports/Infiniti G37    Infiniti G37    1.0    Infiniti G37    3",
            "car:Car    /Cars/Utility/Ford F-150    Ford F-150    1.0    Ford F-150    3",
            "car:Car    /Cars/Utility/Hummer H3    Hummer H3    1.0    Hummer H3    3",
            "car:Car    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.0    Land Rover LR2    3",
            "car:Car    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.0    Land Rover LR3    3",
            "mode:root    /        1.0        0",
            "nt:file    /files/create_schema.ddl    create_schema.ddl    1.0    create_schema.ddl    2",
            "nt:file    /files/standard_test_statements.ddl    standard_test_statements.ddl    1.0    standard_test_statements.ddl    2",
            "nt:folder    /files    files    1.0    files    1",
            "nt:resource    /files/create_schema.ddl/jcr:content    jcr:content    1.0    content    3",
            "nt:resource    /files/standard_test_statements.ddl/jcr:content    jcr:content    1.0    content    3",
            "nt:unstructured    /Cars    Cars    1.0    Cars    1",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.0    Hybrid    2",
            "nt:unstructured    /Cars/Luxury    Luxury    1.0    Luxury    2",
            "nt:unstructured    /Cars/Sports    Sports    1.0    Sports    2",
            "nt:unstructured    /Cars/Utility    Utility    1.0    Utility    2",
            "nt:unstructured    /NodeB    NodeB    1.0    NodeB    1",
            "nt:unstructured    /Other    Other    1.0    Other    1",
            "nt:unstructured    /Other/NodeA    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[2]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /Other/NodeA[3]    NodeA    1.0    NodeA    2",
            "nt:unstructured    /sequenced    sequenced    1.0    sequenced    1",
            "nt:unstructured    /sequenced/ddl    ddl    1.0    ddl    2",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl    create_schema.ddl    1.0    create_schema.ddl    3",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements    ddl:statements    1.0    statements    4",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood    hollywood    1.0    hollywood    5",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood/films    films    1.0    films    6",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood/films/producerName    producerName    1.0    producerName    7",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood/films/release    release    1.0    release    7",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood/films/title    title    1.0    title    7",
            "nt:unstructured    /sequenced/ddl/create_schema.ddl/ddl:statements/hollywood/winners    winners    1.0    winners    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl    standard_test_statements.ddl    1.0    standard_test_statements.ddl    3",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements    ddl:statements    1.0    statements    4",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/ACTIVITIES    ACTIVITIES    1.0    ACTIVITIES    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/ACTIVITIES/ACTIVITY    ACTIVITY    1.0    ACTIVITY    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/ACTIVITIES/CITY_ID    CITY_ID    1.0    CITY_ID    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/ACTIVITIES/SEASON    SEASON    1.0    SEASON    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/Avgs    Avgs    1.0    Avgs    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT[2]    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT[3]    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT[4]    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT[5]    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/GRANT[6]    GRANT    1.0    GRANT    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY    HOTELAVAILABILITY    1.0    HOTELAVAILABILITY    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/BOOKING_DATE    BOOKING_DATE    1.0    BOOKING_DATE    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/HOTEL_ID    HOTEL_ID    1.0    HOTEL_ID    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/PK_1    PK_1    1.0    PK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/PK_1/BOOKING_DATE    BOOKING_DATE    1.0    BOOKING_DATE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/PK_1/HOTEL_ID    HOTEL_ID    1.0    HOTEL_ID    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/HOTELAVAILABILITY/ROOMS_TAKEN    ROOMS_TAKEN    1.0    ROOMS_TAKEN    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/MORE_ACTIVITIES    MORE_ACTIVITIES    1.0    MORE_ACTIVITIES    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/MORE_ACTIVITIES/ACTIVITY    ACTIVITY    1.0    ACTIVITY    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/MORE_ACTIVITIES/CITY_ID    CITY_ID    1.0    CITY_ID    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/MORE_ACTIVITIES/SEASON    SEASON    1.0    SEASON    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/PEOPLE    PEOPLE    1.0    PEOPLE    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/PEOPLE/PEOPLE_PK    PEOPLE_PK    1.0    PEOPLE_PK    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/PEOPLE/PEOPLE_PK/PERSON_ID    PERSON_ID    1.0    PERSON_ID    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/PEOPLE/PERSON    PERSON    1.0    PERSON    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/PEOPLE/PERSON_ID    PERSON_ID    1.0    PERSON_ID    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertIsZero    assertIsZero    1.0    assertIsZero    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertIsZero/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertNotNull    assertNotNull    1.0    assertNotNull    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertNotNull/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertconstr1    assertconstr1    1.0    assertconstr1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertconstr1/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertconstr1/CONSTRAINT_ATTRIBUTE[2]    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/assertconstr2    assertconstr2    1.0    assertconstr2    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/collation1    collation1    1.0    collation1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/collation2    collation2    1.0    collation2    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/collation3    collation3    1.0    collation3    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/cs1    cs1    1.0    cs1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/cs2    cs2    1.0    cs2    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/cs3    cs3    1.0    cs3    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/cs4    cs4    1.0    cs4    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/cs5    cs5    1.0    cs5    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee    employee    1.0    employee    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/deptno    deptno    1.0    deptno    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_fk1    emp_fk1    1.0    emp_fk1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_fk1/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_fk1/dept    dept    1.0    dept    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_fk1/deptno    deptno    1.0    deptno    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_fk1/deptno[2]    deptno    1.0    deptno    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_pk    emp_pk    1.0    emp_pk    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/emp_pk/empno    empno    1.0    empno    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/empname    empname    1.0    empname    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/empno    empno    1.0    empno    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/employee/job    job    1.0    job    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/full_domain    full_domain    1.0    full_domain    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/mydecimal    mydecimal    1.0    mydecimal    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/myinteger    myinteger    1.0    myinteger    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/mynchar    mynchar    1.0    mynchar    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe    oe    1.0    oe    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/GRANT    GRANT    1.0    GRANT    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product    new_product    1.0    new_product    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product/PK_1    PK_1    1.0    PK_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product/PK_1/color    color    1.0    color    8",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product/color    color    1.0    color    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product/quantity    quantity    1.0    quantity    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/oe/new_product_view    new_product_view    1.0    new_product_view    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/partial_domain    partial_domain    1.0    partial_domain    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1    schema_1    1.0    schema_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/table_1    table_1    1.0    table_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/table_1/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/table_1/col2    col2    1.0    col2    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/view_1    view_1    1.0    view_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/view_1/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_1/view_1/col2    col2    1.0    col2    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_2    schema_2    1.0    schema_2    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1    schema_name_1    1.0    schema_name_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/SAMP.V1    SAMP.V1    1.0    SAMP.V1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/SAMP.V1/COL_DIFF    COL_DIFF    1.0    COL_DIFF    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/SAMP.V1/COL_SUM    COL_SUM    1.0    COL_SUM    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name26    table_name26    1.0    table_name26    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name26/UC_1    UC_1    1.0    UC_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name26/UC_1/ref_column_name_1    ref_column_name_1    1.0    ref_column_name_1    8",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name26/column_name_1    column_name_1    1.0    column_name_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15    table_name_15    1.0    table_name_15    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15/FK_1    FK_1    1.0    FK_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15/FK_1/column_name_1    column_name_1    1.0    column_name_1    8",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15/FK_1/ref_column_name_1    ref_column_name_1    1.0    ref_column_name_1    8",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15/FK_1/ref_table_name    ref_table_name    1.0    ref_table_name    8",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/schema_name_1/table_name_15/column_name_1    column_name_1    1.0    column_name_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_3    table_3    1.0    table_3    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_3/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_3/option    option    1.0    option    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_4    table_4    1.0    table_4    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_4/PK_1    PK_1    1.0    PK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_4/PK_1/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_4/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_4/option    option    1.0    option    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5    table_5    1.0    table_5    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/CHECK_1    CHECK_1    1.0    CHECK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/CHECK_1[2]    CHECK_1    1.0    CHECK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1    FK_1    1.0    FK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1/col3    col3    1.0    col3    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1/table_3    table_3    1.0    table_3    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[2]    FK_1    1.0    FK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[2]/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[2]/col4    col4    1.0    col4    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[2]/table_4    table_4    1.0    table_4    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[3]    FK_1    1.0    FK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[3]/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[3]/col5    col5    1.0    col5    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[3]/table_1    table_1    1.0    table_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]    FK_1    1.0    FK_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/CONSTRAINT_ATTRIBUTE[2]    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/col2    col2    1.0    col2    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/col3    col3    1.0    col3    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/col4    col4    1.0    col4    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/col5    col5    1.0    col5    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/FK_1[4]/table_7    table_7    1.0    table_7    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1    UC_1    1.0    UC_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[2]    UC_1    1.0    UC_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[2]/col2    col2    1.0    col2    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[3]    UC_1    1.0    UC_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[3]/col6    col6    1.0    col6    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[4]    UC_1    1.0    UC_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[4]/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[4]/CONSTRAINT_ATTRIBUTE[2]    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[4]/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/UC_1[4]/col4    col4    1.0    col4    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/ck2    ck2    1.0    ck2    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col2    col2    1.0    col2    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col3    col3    1.0    col3    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col4    col4    1.0    col4    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col5    col5    1.0    col5    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/col6    col6    1.0    col6    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/pk5    pk5    1.0    pk5    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/pk5/CONSTRAINT_ATTRIBUTE    CONSTRAINT_ATTRIBUTE    1.0    CONSTRAINT_ATTRIBUTE    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_5/pk5/col1    col1    1.0    col1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29    table_name29    1.0    table_name29    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/column_name_1    column_name_1    1.0    column_name_1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/fk_name    fk_name    1.0    fk_name    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/fk_name/ref_column_name_1    ref_column_name_1    1.0    ref_column_name_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/fk_name/ref_column_name_1[2]    ref_column_name_1    1.0    ref_column_name_1    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/fk_name/ref_column_name_2    ref_column_name_2    1.0    ref_column_name_2    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/table_name29/fk_name/ref_table_name    ref_table_name    1.0    ref_table_name    7",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/tn1    tn1    1.0    tn1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/tn2    tn2    1.0    tn2    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/tn3    tn3    1.0    tn3    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1    view_1    1.0    view_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1/col2    col2    1.0    col2    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[2]    view_1    1.0    view_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[2]/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[2]/col2    col2    1.0    col2    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[3]    view_1    1.0    view_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[3]/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[3]/col2    col2    1.0    col2    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[4]    view_1    1.0    view_1    5",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[4]/col1    col1    1.0    col1    6",
            "nt:unstructured    /sequenced/ddl/standard_test_statements.ddl/ddl:statements/view_1[4]/col2    col2    1.0    col2    6",};

        String[] expected2 = {"jcr:primaryType[STRING]", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car",
            "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "mode:root", "nt:file", "nt:file", "nt:folder",
            "nt:resource", "nt:resource", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
            "nt:unstructured", "nt:unstructured",};

        Session session = repository.login();
        try {

            this.setSession(session);
            removeAllChildren("/files");

            uploadFile("org/modeshape/test/integration/sequencer/ddl/create_schema.ddl", "/files/");
            uploadFile("org/modeshape/test/integration/sequencer/ddl/standard_test_statements.ddl", "/files/");
            Thread.sleep(2000L);

            print = true;

            ConnectionResultsComparator.executeTest(this.connection,
                                                    "SELECT * FROM [nt:base] ORDER BY [jcr:primaryType], [jcr:path]",
                                                    expected1,
                                                    26);

            ConnectionResultsComparator.executeTest(this.connection,
                                                    "SELECT [jcr:primaryType] FROM [nt:base] ORDER BY [jcr:primaryType]",
                                                    expected2,
                                                    26);

        } finally {
            session.logout();
        }

    }

}

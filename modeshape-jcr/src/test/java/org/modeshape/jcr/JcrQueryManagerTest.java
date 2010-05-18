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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrQueryManager.JcrQueryResult;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jcr.nodetype.InvalidNodeTypeDefinitionException;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the JcrEngine.
 * Essentially this is an integration test, but it does test lower-level functionality of the implementation of the JCR interfaces
 * related to querying. (It is simply more difficult to unit test these implementations because of the difficulty in mocking the
 * many other components to replicate the same functionality.)
 * <p>
 * Also, because queries are read-only, the engine is set up once and used for the entire set of test methods.
 * </p>
 */
public class JcrQueryManagerTest {

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static String[] carColumnNames() {
        return new String[] {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine", "car:mpgHighway",
            "car:valueRating", "jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model", "car:msrp"};
    }

    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static JcrRepository repository;
    private Session session;
    private boolean print;

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
            InputStream stream = resourceStream("io/cars-system-view.xml");
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

        // Prime creating the schemata ...
        repository.getRepositoryTypeManager().getRepositorySchemata();
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
        print = false;
        // Obtain a session using the anonymous login capability, which we granted READ privilege
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
    }

    protected Name name( String name ) {
        return engine.getExecutionContext().getValueFactories().getNameFactory().create(name);
    }

    protected Segment segment( String segment ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().createSegment(segment);
    }

    protected List<Segment> segments( String... segments ) {
        List<Segment> result = new ArrayList<Segment>();
        for (String segment : segments) {
            result.add(segment(segment));
        }
        return result;
    }

    protected void assertResults( Query query,
                                  QueryResult result,
                                  long numberOfResults ) throws RepositoryException {
        assertThat(query, is(notNullValue()));
        assertThat(result, is(notNullValue()));
        if (print) {
            System.out.println();
            System.out.println(query);
            System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
            System.out.println(result);
        }
        assertThat(result.getNodes().getSize(), is(numberOfResults));
        assertThat(result.getRows().getSize(), is(numberOfResults));
    }

    protected void assertResultsHaveColumns( QueryResult result,
                                             String... columnNames ) throws RepositoryException {
        Set<String> expectedNames = new HashSet<String>();
        for (String name : columnNames) {
            expectedNames.add(name);
        }
        Set<String> actualNames = new HashSet<String>();
        for (String name : result.getColumnNames()) {
            actualNames.add(name);
        }
        assertThat(actualNames, is(expectedNames));
    }

    public class RowResult {
        private final Row row;

        public RowResult( Row row ) {
            this.row = row;
        }

        public RowResult has( String columnName,
                              String value ) throws RepositoryException {
            assertThat(row.getValue(columnName).getString(), is(value));
            return this;
        }

        public RowResult has( String columnName,
                              long value ) throws RepositoryException {
            assertThat(row.getValue(columnName).getLong(), is(value));
            return this;
        }

        public RowResult and( String columnName,
                              String value ) throws RepositoryException {
            return has(columnName, value);
        }

        public RowResult and( String columnName,
                              long value ) throws RepositoryException {
            return has(columnName, value);
        }
    }

    protected RowResult assertRow( QueryResult result,
                                   int rowNumber ) throws RepositoryException {
        RowIterator rowIter = result.getRows();
        Row row = null;
        for (int i = 0; i != rowNumber; ++i) {
            row = rowIter.nextRow();
        }
        assertThat(row, is(notNullValue()));
        return new RowResult(row);
    }

    @Test
    public void shouldStartUp() {
        assertThat(engine.getRepositoryService(), is(notNullValue()));
    }

    @Test
    public void shouldHaveLoadedContent() throws RepositoryException {
        Node node = session.getRootNode().getNode("Cars");
        assertThat(node, is(notNullValue()));
        assertThat(node.hasNode("Sports"), is(true));
        assertThat(node.hasNode("Utility"), is(true));
        assertThat(node.hasNode("Hybrid"), is(true));
        // System.out.println(node.getNode("Hybrid").getNodes().nextNode().getPath());
        assertThat(node.hasNode("Hybrid/Toyota Prius"), is(true));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:unstructured"));
    }

    @Test
    public void shouldReturnQueryManagerFromWorkspace() throws RepositoryException {
        assertThat(session.getWorkspace().getQueryManager(), is(notNullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 23);
        assertResultsHaveColumns(result, "jcr:primaryType");
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car]", QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarNodesOrderedByYear() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car] ORDER BY [car:year]",
                                                                           QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarNodesOrderedByMsrp() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC",
                                                                           QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, carColumnNames());
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        assertRow(result, 1).has("car:model", "LR3").and("car:msrp", "$48,525").and("car:mpgCity", 12);
        assertRow(result, 2).has("car:model", "IS350").and("car:msrp", "$36,305").and("car:mpgCity", 18);
        assertRow(result, 10).has("car:model", "DB9").and("car:msrp", "$171,600").and("car:mpgCity", 12);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
                                          QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        assertResultsHaveColumns(result, "car:maker", "car:model", "car:year", "car:msrp");
        assertRow(result, 1).has("car:model", "Altima").and("car:msrp", "$18,260").and("car:year", 2008);
        assertRow(result, 2).has("car:model", "Prius").and("car:msrp", "$21,500").and("car:year", 2008);
        assertRow(result, 3).has("car:model", "Highlander").and("car:msrp", "$34,200").and("car:year", 2008);
    }

    @Ignore
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'",
                                          QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:unstructured]",
                                                                           QueryLanguage.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 22);
        assertResultsHaveColumns(result, "jcr:primaryType");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY car:model ASC",
                                          QueryLanguage.JCR_SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithChildAxisCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          QueryLanguage.JCR_SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "jcr:primaryType");
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws RepositoryException
     */
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          QueryLanguage.JCR_SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "jcr:primaryType");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // XPath Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToCreateXPathQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 12);

        query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 22);
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 23);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 22);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByPropertyValue() throws RepositoryException {
        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery("//element(*,nt:unstructured) order by @jcr:primaryType", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 22);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = manager.createQuery("//element(*,car:Car) order by @car:year", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 12);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithPropertyOrderedByProperty() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(" /jcr:root/Cars/Hybrid/*[@car:year] order by @car:year ascending", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPath() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        print = true;
        assertResults(query, result, 16);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 12);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithPropertyOrderedByProperty() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(" /jcr:root/Cars//*[@car:year] order by @car:year ascending", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 12);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByScore() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured) order by jcr:score()",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 22);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindSameNameSiblingsByIndex() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 1);
        assertThat(result, is(notNullValue()));
        assertThat(result.getNodes().nextNode().getIndex(), is(1));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA[2]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 1);
        assertThat(result, is(notNullValue()));
        assertThat(result.getNodes().nextNode().getIndex(), is(2));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
                                 "jcr:path",
                                 "jcr:score",
                                 "car:mpgCity",
                                 "car:userRating",
                                 "car:mpgHighway",
                                 "car:engine",
                                 "car:model",
                                 "car:year",
                                 "car:maker",
                                 "car:lengthInInches",
                                 "car:valueRating",
                                 "car:wheelbaseInInches",
                                 "car:msrp");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNodeWithTypeCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars[@jcr:primaryType]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathAndAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars/Sports/Infiniti_x0020_G37[@car:year='2008']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//Infiniti_x0020_G37[@car:year='2008']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathUnderRootAndAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/NodeB[@myUrl='http://www.acme.com/foo/bar']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAnywhereNodeWithNameAndAttrbuteCriteriaMatchingUrl()
        throws RepositoryException {
        // See MODE-686
        Query query = session.getWorkspace().getQueryManager().createQuery("//NodeB[@myUrl='http://www.acme.com/foo/bar']",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithNameMatch() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//NodeB", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    /*
     * Adding this test case since its primarily a test of integration between the RNTM and the query engine
     */
    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringUsedPrimaryType() throws Exception {
        Session adminSession = null;

        try {
            adminSession = repository.login(new SimpleCredentials("superuser", "superuser".toCharArray()));
            adminSession.setNamespacePrefix("cars", "http://www.modeshape.org/examples/cars/1.0");

            JcrNodeTypeManager nodeTypeManager = (JcrNodeTypeManager)adminSession.getWorkspace().getNodeTypeManager();
            nodeTypeManager.unregisterNodeTypes(Collections.singletonList("cars:Car"));
        } finally {
            if (adminSession != null) adminSession.logout();
        }
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., 'liter')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"liter V 12\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForAllNodesBelowRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 23);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element(Cars)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForSingleNodeBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(Utility)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForMultipleNodesBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithRangeCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Other/*[@something <= 'value2' and @something > 'value1']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryWithNewlyRegisteredNamespace() throws RepositoryException {
        session.getWorkspace().getNamespaceRegistry().registerNamespace("newPrefix", "newUri");

        // We don't have any elements that use this yet, but let's at least verify that it can execute.
        Query query = session.getWorkspace().getQueryManager().createQuery("//*[@newPrefix:someColumn = 'someValue']",
                                                                           Query.XPATH);
        query.execute();

    }

    @Test
    public void shouldNotReturnNodesWithNoPropertyForPropertyCriterion() throws Exception {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars//*[@car:wheelbaseInInches]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:wheelbaseInInches"), is(true));
        }
    }

    @Test
    public void shouldNotReturnNodesWithNoPropertyForLikeCriterion() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars//*[jcr:like(@car:wheelbaseInInches, '%')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:wheelbaseInInches"), is(true));
        }
    }
}

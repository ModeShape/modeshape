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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
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
import org.modeshape.common.FixFor;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.nodetype.InvalidNodeTypeDefinitionException;
import org.modeshape.jcr.query.JcrQueryResult;

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
            "car:valueRating", "jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model", "car:msrp", "jcr:created",
            "jcr:createdBy", "jcr:name", "jcr:path", "jcr:score", "mode:depth", "mode:localName"};
    }

    protected static String[] minimumColumnNames() {
        return new String[] {"jcr:primaryType", "jcr:name", "jcr:path", "jcr:score", "mode:depth", "mode:localName"};
    }

    protected static String[] searchColumnNames() {
        return new String[] {};
    }

    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static JcrRepository repository;
    private Session session;
    private boolean print;

    @SuppressWarnings( "deprecation" )
    @BeforeClass
    public static void beforeAll() throws Exception {
        try {
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
            try {
                repository = engine.getRepository("cars");
            } catch (RuntimeException t) {
                t.printStackTrace();
                throw t;
            } catch (Exception t) {
                t.printStackTrace();
                throw t;
            }

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
                session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
            } finally {
                session.logout();
            }

            // Prime creating the schemata ...
            repository.getRepositoryTypeManager().getRepositorySchemata();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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
        if (print /*|| result.getNodes().getSize() != numberOfResults || result.getRows().getSize() != numberOfResults*/) {
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

    protected void assertResultsHaveRows( QueryResult result,
                                          String columnName,
                                          String... rowValuesAsStrings ) throws RepositoryException {
        RowIterator iter = result.getRows();
        int i = 0;
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row.getValue(columnName).getString(), is(rowValuesAsStrings[i++]));
        }
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
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base] WHERE [car:year] < 2009",
                                                                           Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldReturnNullValuesForNonExistantPropertiesInSelectClauseOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE [car:year] < 2009",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "bogus", "laughable", "car:year");
        RowIterator rows = result.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            assertThat(row.getValue("bogus"), is(nullValue()));
            assertThat(row.getValue("laughable"), is(nullValue()));
            assertThat(row.getValue("car:year"), is(not(nullValue())));
        }
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotMatchNodesWhenQueryUsesNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE argle < 2009", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0);
        assertResultsHaveColumns(result, "bogus", "laughable", "car:year");
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotOrderByNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] ORDER BY argle", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, "bogus", "laughable", "argle", "car:year");
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByYear() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car] ORDER BY [car:year]",
                                                                           Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByMsrp() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC",
                                                                           Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames());
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        assertRow(result, 1).has("car:model", "LR3").and("car:msrp", "$48,525").and("car:mpgCity", 12);
        assertRow(result, 2).has("car:model", "IS350").and("car:msrp", "$36,305").and("car:mpgCity", 18);
        assertRow(result, 10).has("car:model", "DB9").and("car:msrp", "$171,600").and("car:mpgCity", 12);
    }

    @FixFor( "MODE-1057" )
    @Test
    public void shouldAllowEqualityCriteriaOnPropertyDefinedWithNumericPropertyDefinition() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] AS car WHERE [car:userRating] = 4",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, "car:maker", "car:model", "car:year", "car:userRating");
    }

    @FixFor( "MODE-1057" )
    @Test
    public void shouldAllowLikeCriteriaOnPropertyDefinedWithNumericPropertyDefinition() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] AS car WHERE [car:userRating] LIKE 4",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, "car:maker", "car:model", "car:year", "car:userRating");
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
                                          Query.JCR_SQL2);
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
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryUsingJoinToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 12);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:unstructured]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 23);
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5L);
        String[] expectedColumnNames = {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine",
            "car:mpgHighway", "car:valueRating", "car.jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model",
            "car:msrp", "jcr:created", "jcr:createdBy", "category.jcr:primaryType", "jcr:name", "jcr:path", "jcr:score",
            "mode:depth", "mode:localName"};
        assertResultsHaveColumns(result, expectedColumnNames);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithoutCriteria() throws RepositoryException {
        // The 'all' selector will find all nodes, including '/Car' and '/Car/Sports'. Thus,
        // '/Car/Sports/Infiniti G37' will be a descendant of both '/Car' and '/Car/Sports', and thus will appear
        // once joined with '/Car' and once joined with '/Car/Sports'. These two tuples will be similar, but they are
        // actually not repeats (since the columns from 'all' will be different).
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as all ON ISDESCENDANTNODE(car,all)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 26L);
        String[] expectedColumnNames = {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine",
            "car:mpgHighway", "car:valueRating", "car.jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model",
            "car:msrp", "jcr:created", "jcr:createdBy", "all.jcr:primaryType", "car.jcr:name", "car.jcr:path", "car.jcr:score",
            "car.mode:depth", "car.mode:localName", "all.jcr:name", "all.jcr:path", "all.jcr:score", "all.mode:depth",
            "all.mode:localName"};
        assertResultsHaveColumns(result, expectedColumnNames);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithDepthCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE DEPTH(category) = 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumnNames = {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine",
            "car:mpgHighway", "car:valueRating", "car.jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model",
            "car:msrp", "jcr:created", "jcr:createdBy", "category.jcr:primaryType", "car.jcr:name", "car.jcr:path",
            "car.jcr:score", "car.mode:depth", "car.mode:localName", "category.jcr:name", "category.jcr:path",
            "category.jcr:score", "category.mode:depth", "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumnNames);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);
        assertResultsHaveColumns(result, carColumnNames());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5L);
        String[] expectedColumnNames = {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:engine",
            "car:mpgHighway", "car:valueRating", "car.jcr:primaryType", "car:wheelbaseInInches", "car:year", "car:model",
            "car:msrp", "jcr:created", "jcr:createdBy", "category.jcr:primaryType", "jcr:name", "jcr:path", "jcr:score",
            "mode:depth", "mode:localName"};
        assertResultsHaveColumns(result, expectedColumnNames);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBase() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:name", "cars.jcr:path",
            "cars.jcr:score", "cars.mode:depth", "cars.mode:localName", "category.jcr:name", "category.jcr:path",
            "category.jcr:score", "category.mode:depth", "category.mode:localName"};

        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBaseAndNameConstraint()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND NAME(cars) LIKE 'Toyota%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:name", "cars.jcr:path",
            "cars.jcr:score", "cars.mode:depth", "cars.mode:localName", "category.jcr:name", "category.jcr:path",
            "category.jcr:score", "category.mode:depth", "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] AS category JOIN [nt:unstructured] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0L); // no nodes have a 'name' property (strictly speaking)
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:name", "cars.jcr:path",
            "cars.jcr:score", "cars.mode:depth", "cars.mode:localName", "category.jcr:name", "category.jcr:path",
            "category.jcr:score", "category.mode:depth", "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldReturnNoResultsForJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithNoResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0); // no results, because it is a join on an invalid column
        assertResultsHaveColumns(result, new String[] {"cars.mode:depth", "cars.mode:localName", "category.jcr:primaryType",
            "category.jcr:score", "cars.jcr:score", "category.jcr:path", "cars.jcr:primaryType", "category.mode:localName",
            "cars.jcr:path", "category.mode:depth", "cars.jcr:name", "category.jcr:name"});
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE [car:year] >= 2008)",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13); // the 13 types of cars made by makers that made cars in 2008
        assertResultsHaveColumns(result, carColumnNames());
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria2() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE PATH() LIKE '%/Hybrid/%')",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars made by makers that make hybrids
        assertResultsHaveColumns(result, carColumnNames());
    }

    @FixFor( "MODE-909" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderBy() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [jcr:primaryType] from [nt:base] ORDER BY [jcr:primaryType]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType"});
        RowIterator iter = result.getRows();
        String primaryType = "";
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            String nextPrimaryType = row.getValues()[0].getString();
            assertThat(nextPrimaryType.compareTo(primaryType) >= 0, is(true));
            primaryType = nextPrimaryType;
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base]",
                                                                           Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:path] LIKE '/Cars/%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 17);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:name] LIKE '%3%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [mode:localName] LIKE '%3%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select base.[jcr:primaryType], base.[jcr:path], car.[car:year] "
                                          + "FROM [nt:base] AS base JOIN [car:Car] AS car ON car.[jcr:path] = base.[jcr:path]",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "car:year"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, minimumColumnNames());
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-1020" )
    @Test
    public void shouldFindAllPublishAreas() throws Exception {
        String sql = "SELECT [jcr:path], [jcr:title], [jcr:description] FROM [mode:publishArea]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0L); // currently no records
        assertResultsHaveColumns(result, new String[] {"jcr:path", "jcr:title", "jcr:description"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-1052" )
    @Test
    public void shouldProperlyUseNotWithPathConstraints() throws Exception {
        // Find all nodes that are children of '/Cars' ... there should be 4 ...
        String sql = "SELECT [jcr:path] FROM [nt:base] WHERE ISCHILDNODE([nt:base],'/Cars') ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result, "jcr:path", "/Cars/Hybrid", "/Cars/Luxury", "/Cars/Sports", "/Cars/Utility");

        // Find all nodes ... there should be 24 ...
        sql = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24L);

        // Find all nodes that are NOT children of '/Cars' ... there should be 4 ...
        sql = "SELECT [jcr:path] FROM [nt:base] WHERE NOT(ISCHILDNODE([nt:base],'/Cars')) ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 20L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result,
                              "jcr:path",
                              "/",
                              "/Cars",
                              "/Cars/Hybrid/Nissan Altima",
                              "/Cars/Hybrid/Toyota Highlander",
                              "/Cars/Hybrid/Toyota Prius",
                              "/Cars/Luxury/Bentley Continental",
                              "/Cars/Luxury/Cadillac DTS",
                              "/Cars/Luxury/Lexus IS350",
                              "/Cars/Sports/Aston Martin DB9",
                              "/Cars/Sports/Infiniti G37",
                              "/Cars/Utility/Ford F-150",
                              "/Cars/Utility/Hummer H3",
                              "/Cars/Utility/Land Rover LR2",
                              "/Cars/Utility/Land Rover LR3",
                              "/Cars/Utility/Toyota Land Cruiser",
                              "/NodeB",
                              "/Other",
                              "/Other/NodeA",
                              "/Other/NodeA[2]",
                              "/Other/NodeA[3]");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Full-text Search Queries
    // ----------------------------------------------------------------------------------------------------------------

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("land", JcrRepository.QueryLanguage.SEARCH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, searchColumnNames());
    }

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQueryWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("highlander", JcrRepository.QueryLanguage.SEARCH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, searchColumnNames());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY car:model ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByPathClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY PATH() ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithPathCriteriaAndOrderByClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE jcr:path LIKE '/Cars/%' ORDER BY car:model ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithChildAxisCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @Test
    @FixFor( "MODE-791" )
    @SuppressWarnings( "deprecation" )
    public void shouldReturnNodesWithPropertyConstrainedByTimestamp() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model, car:maker FROM car:Car " + "WHERE jcr:path LIKE '/Cars/%' "
                                          + "AND (car:msrp LIKE '$3%' OR car:msrp LIKE '$2') "
                                          + "AND (car:year LIKE '2008' OR car:year LIKE '2009') " + "AND car:valueRating > '1' "
                                          + "AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00' "
                                          + "AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:model"), is(true));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select jcr:primaryType, jcr:path FROM nt:base",
                                                                           Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:path LIKE '/Cars/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 17);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:name LIKE '%3%'", Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSqlQuery()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE mode:localName LIKE '%3%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select nt:base.jcr:primaryType, nt:base.jcr:path, car:Car.car:year "
                                          + "FROM nt:base, car:Car WHERE car:Car.jcr:path = nt:base.jcr:path",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "car:year", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select * FROM nt:base", Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, minimumColumnNames());
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // XPath Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateXPathQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 13);

        query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 23);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByPath() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//element(*,nt:base) order by @jcr:path", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByAttribute() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car) order by @car:maker",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "car:maker", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 23);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByPropertyValue() throws RepositoryException {
        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery("//element(*,nt:unstructured) order by @jcr:primaryType", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 23);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = manager.createQuery("//element(*,car:Car) order by @car:year", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPath() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 17);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesWithAllSnsIndexesUnderPath() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root//NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithPropertyOrderedByProperty() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(" /jcr:root/Cars//*[@car:year] order by @car:year ascending", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByScore() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured) order by jcr:score()",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 23);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, minimumColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindSameNameSiblingsByIndex() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        // assertThat(result.getNodes().nextNode().getIndex(), is(1));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA[2]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 1);
        assertThat(result, is(notNullValue()));
        assertThat(result.getNodes().nextNode().getIndex(), is(2));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
                                 "jcr:path",
                                 "jcr:score",
                                 "jcr:created",
                                 "jcr:createdBy",
                                 "jcr:name",
                                 "mode:localName",
                                 "mode:depth",
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNodeWithTypeCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars[@jcr:primaryType]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//Infiniti_x0020_G37[@car:year='2008']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., 'liter')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphen() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"5-speed\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNumberAndWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"5-s*\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndNoWildcard() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"heavy duty\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNoWildcard() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"heavy-duty\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndWildcard() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"heavy du*\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndWildcard() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"heavy-du*\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndLeadingWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., '\"*-speed\"')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @FixFor( "MODE-790" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithCompoundCriteria() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars//element(*,car:Car)[@car:year='2008' and jcr:contains(., '\"liter V 12\"')]",
                                          Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        assertResults(query, result, 1);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
                                 "jcr:path",
                                 "jcr:score",
                                 "jcr:created",
                                 "jcr:createdBy",
                                 "jcr:name",
                                 "mode:localName",
                                 "mode:depth",
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

        // Query again with a different criteria that should return no nodes ...
        query = session.getWorkspace()
                       .getQueryManager()
                       .createQuery("/jcr:root/Cars//element(*,car:Car)[@car:year='2007' and jcr:contains(., '\"liter V 12\"')]",
                                    Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForAllNodesBelowRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element(Cars)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForSingleNodeBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(Utility)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForMultipleNodesBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithNewlyRegisteredNamespace() throws RepositoryException {
        session.getWorkspace().getNamespaceRegistry().registerNamespace("newPrefix", "newUri");

        // We don't have any elements that use this yet, but let's at least verify that it can execute.
        Query query = session.getWorkspace().getQueryManager().createQuery("//*[@newPrefix:someColumn = 'someValue']",
                                                                           Query.XPATH);
        query.execute();

    }

    @SuppressWarnings( "deprecation" )
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

    @SuppressWarnings( "deprecation" )
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

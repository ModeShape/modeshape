/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.jcr.JcrQueryManager.JcrQueryResult;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.dna.jcr.JcrRepository.QueryLanguage;
import org.jboss.dna.jcr.nodetype.InvalidNodeTypeDefinitionException;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
                     .registerNamespace("car", "http://www.jboss.org/dna/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                JcrSession.DNA_READ_PERMISSION + "," + JcrSession.DNA_WRITE_PERMISSION)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "dna-jcr");
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
            other.addNode("NodeA", "nt:unstructured");
            other.addNode("NodeA", "nt:unstructured");
            other.addNode("NodeA", "nt:unstructured");
            session.save();

            // Prime creating a first XPath query and SQL query ...
            session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
            session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", JcrRepository.QueryLanguage.SQL);
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
    // JCR2-SQL Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", QueryLanguage.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 22);
        assertResultsHaveColumns(result, "jcr:primaryType");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // XPath Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToCreateXPathQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 21);
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 22);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 21);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByPropertyValue() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//element(*,nt:unstructured) order by @jcr:primaryType", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        print = true;
        assertResults(query, result, 21);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByScore() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured) order by jcr:score()",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 21);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindSameNameSiblingsByIndex() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        print = true;
        assertResults(query, result, 1);
        assertThat(result, is(notNullValue()));
        assertThat(result.getNodes().nextNode().getIndex(), is(1));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA[2]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        print = true;
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

    /*
     * Adding this test case since its primarily a test of integration between the RNTM and the query engine
     */
    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringUsedPrimaryType() throws Exception {
        Session adminSession = null;

        try {
            adminSession = repository.login(new SimpleCredentials("superuser", "superuser".toCharArray()));
            adminSession.setNamespacePrefix("cars", "http://www.jboss.org/dna/examples/cars/1.0");

            JcrNodeTypeManager nodeTypeManager = (JcrNodeTypeManager)adminSession.getWorkspace().getNodeTypeManager();
            nodeTypeManager.unregisterNodeType(Collections.singletonList("cars:Car"));
        } finally {
            if (adminSession != null) adminSession.logout();
        }
    }

}

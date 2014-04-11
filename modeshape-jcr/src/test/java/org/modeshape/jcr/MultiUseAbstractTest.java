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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.modeshape.jcr.api.query.QueryManager;

/**
 * A base class for tests that require a single shared JcrSession and JcrRepository for all test methods.
 */
public abstract class MultiUseAbstractTest extends AbstractJcrRepositoryTest {

    private static final String REPO_NAME = "testRepo";

    private static Environment environment;
    private static RepositoryConfiguration config;
    protected static JcrRepository repository;
    protected static JcrSession session;

    protected static void startRepository() throws Exception {
        startRepository(null);
    }

    protected static void startRepository( RepositoryConfiguration configuration ) throws Exception {
        environment = new TestingEnvironment();
        if (configuration != null) {
            config = new RepositoryConfiguration(configuration.getDocument(), configuration.getName(), environment);
        } else {
            config = new RepositoryConfiguration(REPO_NAME, environment);
        }
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    protected static void stopRepository() throws Exception {
        try {
            TestingUtil.killRepositories(repository);
        } finally {
            repository = null;
            config = null;
            environment.shutdown();
        }
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        startRepository();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        stopRepository();
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        // create a new session for each test ...
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        // log out of the session after each test ...
        try {
            session.logout();
        } finally {
            session = null;
        }
    }

    @Override
    public JcrRepository repository() {
        return repository;
    }

    @Override
    public JcrSession session() {
        return session;
    }

    public static org.modeshape.jcr.api.Session getSession() {
        return session;
    }

    protected static InputStream resourceStream( String name ) {
        return MultiUseAbstractTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static void registerNodeTypes( String resourceName ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Workspace workspace = session.getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        ntMgr.registerNodeTypes(stream, true);
    }

    protected static void registerNodeTypes( List<? extends NodeTypeDefinition> nodeTypes ) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        NodeTypeDefinition[] defns = nodeTypes.toArray(new NodeTypeDefinition[nodeTypes.size()]);
        ntMgr.registerNodeTypes(defns, true);
    }

    protected static void importContent( Node parent,
                                         String resourceName,
                                         int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        parent.getSession().getWorkspace().importXML(parent.getPath(), stream, uuidBehavior);
    }

    protected static void importContent( String parentPath,
                                         String resourceName,
                                         int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        session.getWorkspace().importXML(parentPath, stream, uuidBehavior);
    }

    protected void assertNodesAreFound( String queryString,
                                        String queryType,
                                        String... expectedNodesPaths ) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, queryType);
        QueryResult result = query.execute();

        List<String> actualNodePaths = new ArrayList<String>();
        for (NodeIterator nodeIterator = result.getNodes(); nodeIterator.hasNext();) {
            actualNodePaths.add(nodeIterator.nextNode().getPath().toLowerCase());
        }

        List<String> expectedNodePaths = Arrays.asList(expectedNodesPaths);

        assertEquals(expectedNodePaths.toString() + ":" + actualNodePaths.toString(), expectedNodePaths.size(),
                     actualNodePaths.size());
        for (String expectedPath : expectedNodePaths) {
            assertTrue(expectedPath + " not found", actualNodePaths.remove(expectedPath.toLowerCase()));
        }
    }

    protected void assertNodesNotFound( String queryString,
                                        String queryType ) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, queryType);
        QueryResult result = query.execute();

        List<String> actualNodePaths = new ArrayList<String>();
        for (NodeIterator nodeIterator = result.getNodes(); nodeIterator.hasNext();) {
            actualNodePaths.add(nodeIterator.nextNode().getPath().toLowerCase());
        }

        assertTrue("expected empty result, but: " + actualNodePaths.toString(), actualNodePaths.isEmpty());
    }
}

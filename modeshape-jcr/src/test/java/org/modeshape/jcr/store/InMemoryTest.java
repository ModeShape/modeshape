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
package org.modeshape.jcr.store;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.TransactionManager;
import org.infinispan.loaders.CacheLoaderConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.TestingEnvironment;
import org.modeshape.jcr.TestingUtil;

public class InMemoryTest {

    private static final String LARGE_STRING_VALUE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed fermentum iaculis placerat. Mauris condimentum dapibus pretium. Vestibulum gravida sodales tellus vitae porttitor. Nunc dictum, eros vel adipiscing pellentesque, sem mi iaculis dui, a aliquam neque magna non turpis. Maecenas imperdiet est eu lorem placerat mattis. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum scelerisque molestie tristique. Mauris nibh diam, vestibulum eu condimentum at, facilisis at nisi. Maecenas vehicula accumsan lacus in venenatis. Nulla nisi eros, fringilla at dapibus mollis, pharetra at urna. Praesent in risus magna, at iaculis sapien. Fusce id velit id dui tempor hendrerit semper a nunc. Nam eget mauris tellus.";
    private static final String SMALL_STRING_VALUE = "The quick brown fox jumped over the moon. What? ";

    private final String REPO_NAME = "testRepo";

    private static final Stopwatch STARTUP = new Stopwatch();
    private static final Stopwatch INFINISPAN_STARTUP = new Stopwatch();
    private static final Stopwatch MODESHAPE_STARTUP = new Stopwatch();

    private TestingEnvironment environment;
    private TransactionManager txnMgr;
    private RepositoryConfiguration config;
    protected ModeShapeEngine engine;
    protected Repository repository;
    protected Session session;

    @Before
    public void beforeEach() throws Exception {
        cleanUpFileSystem();

        STARTUP.start();
        INFINISPAN_STARTUP.start();
        environment = new TestingEnvironment(getCacheLoaderConfiguration());
        txnMgr = org.infinispan.test.TestingUtil.getTransactionManager(environment.getCacheContainer().getCache(REPO_NAME));
        INFINISPAN_STARTUP.stop();
        MODESHAPE_STARTUP.start();
        config = new RepositoryConfiguration(REPO_NAME, environment);
        engine = new ModeShapeEngine();
        engine.start();
        engine.deploy(config);
        repository = engine.startRepository(config.getName()).get();
        session = repository.login();
        MODESHAPE_STARTUP.stop();
        STARTUP.stop();
    }

    @After
    public void afterEach() throws Exception {
        try {
            TestingUtil.killEngine(engine);
        } finally {
            engine = null;
            repository = null;
            config = null;
            try {
                org.infinispan.test.TestingUtil.killTransaction(txnMgr);
            } finally {
                try {
                    environment.shutdown();
                } finally {
                    cleanUpFileSystem();
                }
            }
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        System.out.println("Infinispan (CacheManager) startup time: " + INFINISPAN_STARTUP.getSimpleStatistics());
        System.out.println("ModeShape startup time:                 " + MODESHAPE_STARTUP.getSimpleStatistics());
        System.out.println("Total startup time:                     " + STARTUP.getSimpleStatistics());
    }

    protected void cleanUpFileSystem() throws Exception {
        // do nothing by default
    }

    protected CacheLoaderConfig getCacheLoaderConfiguration() {
        return null;
    }

    @Test
    public void shouldHaveJcrSystemNodeUnderRoot() throws Exception {
        Node node = session.getRootNode();
        assertThat(node, is(notNullValue()));
        assertThat(node.getPath(), is("/"));
        Node system = node.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
        assertThat(system.getPath(), is("/jcr:system"));
    }

    @Test
    public void shouldAllowCreatingMultipleUnstructuredNodesWithSameNameSiblings() throws Exception {
        Stopwatch sw = new Stopwatch();
        System.out.print("Iterating ");
        for (int i = 0; i != 4; ++i) {
            System.out.print(".");
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            int count = 10;
            if (i > 2) sw.start();
            for (int j = 0; j != count; ++j) {
                node.addNode("childNode");
            }
            session.save();
            if (i > 2) sw.stop();

            // Now add another node ...
            node.addNode("oneMore");
            session.save();

            node.remove();
            session.save();
            assertThat(session.getRootNode().getNodes().getSize(), is(1L));
        }
        System.out.println();
        System.out.println(sw.getDetailedStatistics());
    }

    @Test
    public void shouldAllowCreatingNodeUnderUnsavedNode() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithNoSameNameSiblings() throws Exception {
        Stopwatch sw = new Stopwatch();
        System.out.print("Iterating ");
        for (int i = 0; i != 2; ++i) {
            System.out.print(".");
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            int count = 10;
            if (i > 1) sw.start();
            for (int j = 0; j != count; ++j) {
                node.addNode("childNode" + j);
            }
            session.save();
            if (i > 1) sw.stop();

            // Now add another node ...
            node.addNode("oneMore");
            session.save();

            session.getRootNode().getNode("testNode").remove();
            session.save();
            assertThat(session.getRootNode().getNodes().getSize(), is(1L));
        }
        System.out.println();
        System.out.println(sw.getDetailedStatistics());
    }

    @Test
    public void shouldAllowSmallerSubgraph() throws Exception {
        repeatedlyCreateSubgraph(5, 2, 3, 0, false, true);
    }

    protected void repeatedlyCreateSubgraph( int samples,
                                             int depth,
                                             int numberOfChildrenPerNode,
                                             int numberOfPropertiesPerNode,
                                             boolean useSns,
                                             boolean print ) throws Exception {
        Node node = session.getRootNode().addNode("testArea");
        session.save();

        Stopwatch sw = new Stopwatch();
        if (print) System.out.print("Iterating ");
        int numNodesEach = 0;
        for (int i = 0; i != samples; ++i) {
            System.out.print(".");
            sw.start();
            numNodesEach = createSubgraph(session, node, depth, numberOfChildrenPerNode, numberOfPropertiesPerNode, useSns, 1);
            sw.stop();
            session.save();
        }

        // session.getRootNode().getNode("testArea").remove();
        // session.save();
        // assertThat(session.getRootNode().getNodes().getSize(), is(1L)); // only '/jcr:system'
        if (print) {
            System.out.println();
            System.out.println("Create subgraphs with " + numNodesEach + " nodes each: " + sw.getSimpleStatistics());
        }

        // Now try getting a node at one level down ...
        String name = "childNode";
        int index = numberOfChildrenPerNode / 2;
        String path = useSns ? (name + "[" + index + "]") : (name + index);
        sw.reset();
        sw.start();
        Node randomNode = node.getNode(path);
        sw.stop();
        assertThat(randomNode, is(notNullValue()));
        if (print) {
            System.out.println("Find " + randomNode.getPath() + ": " + sw.getTotalDuration());
        }
    }

    /**
     * Create a structured subgraph by generating nodes with the supplied number of properties and children, to the supplied
     * maximum subgraph depth.
     * 
     * @param session the session that should be used; may not be null
     * @param parentNode the parent node under which the subgraph is to be created
     * @param depthRemaining the depth of the subgraph; must be a positive number
     * @param numberOfChildrenPerNode the number of child nodes to create under each node
     * @param numberOfPropertiesPerNode the number of properties to create on each node; must be 0 or more
     * @param useSns true if the child nodes under a parent should be same-name-siblings, or false if they should each have their
     *        own unique name
     * @param depthToSave
     * @return the number of nodes created in the subgraph
     * @throws RepositoryException if there is a problem
     */
    protected int createSubgraph( Session session,
                                  Node parentNode,
                                  int depthRemaining,
                                  int numberOfChildrenPerNode,
                                  int numberOfPropertiesPerNode,
                                  boolean useSns,
                                  int depthToSave ) throws RepositoryException {
        int numberCreated = 0;
        for (int i = 0; i != numberOfChildrenPerNode; ++i) {
            Node child = parentNode.addNode(useSns ? "childNode" : ("childNode" + i));
            for (int j = 0; j != numberOfPropertiesPerNode; ++j) {
                String value = (i % 10 == 0) ? LARGE_STRING_VALUE : SMALL_STRING_VALUE;
                child.setProperty("property" + j, value);
            }
            numberCreated += numberOfChildrenPerNode;
            if (depthRemaining > 1) {
                numberCreated += createSubgraph(session,
                                                child,
                                                depthRemaining - 1,
                                                numberOfChildrenPerNode,
                                                numberOfPropertiesPerNode,
                                                useSns,
                                                depthToSave);
            }
        }
        if (depthRemaining == depthToSave) {
            session.save();
        }
        return numberCreated;
    }

    protected int calculateTotalNumberOfNodesInTree( int numberOfChildrenPerNode,
                                                     int depth,
                                                     boolean countRoot ) {
        assert depth > 0;
        assert numberOfChildrenPerNode > 0;
        int totalNumber = 0;
        for (int i = 0; i <= depth; ++i) {
            totalNumber += (int)Math.pow(numberOfChildrenPerNode, i);
        }
        return countRoot ? totalNumber : totalNumber - 1;
    }

    protected String getTotalAndAverageDuration( Stopwatch stopwatch,
                                                 long numNodes ) {
        long totalDurationInMilliseconds = TimeUnit.NANOSECONDS.toMillis(stopwatch.getTotalDuration().longValue());
        long avgDuration = totalDurationInMilliseconds / numNodes;
        String units = " millisecond(s)";
        if (avgDuration < 1L) {
            long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotalDuration().longValue());
            avgDuration = totalDurationInMicroseconds / numNodes;
            units = " microsecond(s)";
        }
        return "total = " + stopwatch.getTotalDuration() + "; avg = " + avgDuration + units;
    }

}

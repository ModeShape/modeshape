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
package org.modeshape.test.performance;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Json;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.annotation.Performance;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

public class InMemoryPerformanceTest {

    private static final String LARGE_STRING_VALUE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed fermentum iaculis placerat. Mauris condimentum dapibus pretium. Vestibulum gravida sodales tellus vitae porttitor. Nunc dictum, eros vel adipiscing pellentesque, sem mi iaculis dui, a aliquam neque magna non turpis. Maecenas imperdiet est eu lorem placerat mattis. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum scelerisque molestie tristique. Mauris nibh diam, vestibulum eu condimentum at, facilisis at nisi. Maecenas vehicula accumsan lacus in venenatis. Nulla nisi eros, fringilla at dapibus mollis, pharetra at urna. Praesent in risus magna, at iaculis sapien. Fusce id velit id dui tempor hendrerit semper a nunc. Nam eget mauris tellus.";
    private static final String SMALL_STRING_VALUE = "The quick brown fox jumped over the moon. What? ";

    private static final Stopwatch STARTUP = new Stopwatch();
    private static final Stopwatch MODESHAPE_STARTUP = new Stopwatch();

    private static final int MANY_NODES_COUNT = 10000;

    private RepositoryConfiguration config;
    protected ModeShapeEngine engine;
    protected Repository repository;
    protected Session session;

    @Before
    public void beforeEach() throws Exception {
        cleanUpFileSystem();

        // Read the configuration file, which will be named the same as the class name ...
        String configFileName = getClass().getSimpleName() + ".json";
        String configFilePath = "config/" + configFileName;
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        assertThat("Unable to find configuration file '" + configFilePath, configStream, is(notNullValue()));

        Document configDoc = Json.read(configStream);

        STARTUP.start();
        config = new RepositoryConfiguration(configDoc, configFileName);

        MODESHAPE_STARTUP.start();
        engine = new ModeShapeEngine();
        engine.start();
        engine.deploy(config);
        repository = engine.startRepository(config.getName()).get();
        session = repository.login();
        registerNodeTypes("cnd/large-collections.cnd", session);
        MODESHAPE_STARTUP.stop();
        STARTUP.stop();
    }

    @After
    public void afterEach() throws Exception {
        try {
            org.modeshape.jcr.TestingUtil.killEngine(engine);
        } finally {
            engine = null;
            repository = null;
            config = null;
            cleanUpFileSystem();
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        System.out.println("ModeShape startup time:                 " + MODESHAPE_STARTUP.getSimpleStatistics());
        System.out.println("Total startup time:                     " + STARTUP.getSimpleStatistics());
    }

    protected void cleanUpFileSystem() throws Exception {
        // do nothing by default
    }

    @Test
    public void shouldHaveRootNode() throws Exception {
        Node node = session.getRootNode();
        assertThat(node, is(notNullValue()));
        assertThat(node.getPath(), is("/"));
    }

    @Test
    public void shouldHaveJcrSystemNodeUnderRoot() throws Exception {
        Node node = session.getRootNode();
        Node system = node.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
        assertThat(system.getPath(), is("/jcr:system"));
    }

    @Test
    public void shouldAllowCreatingManyUnstructuredNodesWithSameNameSiblings() throws Exception {
        Stopwatch sw = new Stopwatch();
        System.out.print("Iterating ");
        for (int i = 0; i != 15; ++i) {
            System.out.print(".");
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            if (i > 2) {
                sw.start();
            }
            for (int j = 0; j != MANY_NODES_COUNT; ++j) {
                node.addNode("childNode");
            }
            session.save();
            if (i > 2) {
                sw.stop();
            }

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
        for (int i = 0; i != 10; ++i) {
            System.out.print(".");
            // Each iteration adds another node under the root and creates the many nodes under that node ...
            Node node = session.getRootNode().addNode("testNode");
            session.save();

            int count = MANY_NODES_COUNT;
            if (i > 2) {
                sw.start();
            }
            for (int j = 0; j != count; ++j) {
                node.addNode("childNode" + j);
            }
            session.save();
            if (i > 2) {
                sw.stop();
            }

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
    public void shouldAllowSmallerSubgraph() throws Exception {
        repeatedlyCreateSubgraph(5, 2, 4, 0, false, true);
    }

    @Test
    public void shouldAllowSmallSubgraph() throws Exception {
        repeatedlyCreateSubgraph(5, 2, 10, 7, false, true);
    }

    @Performance
    @Test
    public void shouldAllowCreatingMillionNodeSubgraphUsingMultipleSaves() throws Exception {
        repeatedlyCreateSubgraph(1, 2, 100, 0, false, true);
    }

    @Performance
    @Test
    public void shouldAllowCreatingManyManyUnstructuredNodesWithNoSameNameSiblings() throws Exception {
        System.out.print("Iterating ");
        // Each iteration adds another node under the root and creates the many nodes under that node ...
        Node node = session.getRootNode().addNode("testNode");
        session.save();

        Stopwatch sw = new Stopwatch();
        Stopwatch total = new Stopwatch();
        try {
            total.start();
            for (int i = 0; i != 50; ++i) {
                System.out.print(".");
                int count = 100;
                sw.start();
                for (int j = 0; j != count; ++j) {
                    node.addNode("childNode" + j);
                }
                session.save();
                sw.stop();
            }
            total.stop();
        } finally {
            System.out.println();
            System.out.println(total.getDetailedStatistics());
            System.out.println(sw.getDetailedStatistics());
        }
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
        if (print) {
            System.out.print("Iterating ");
        }
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

    @Test
    @Performance
    public void shouldGetNodePathsInFlatLargeHierarchyWithSns() throws Exception {
        boolean print = true;

        // insert 100k nodes with 10 props each under the same parent in batches of 500
        int initialNodeCount = 100000;
        int insertBatchSize = 500;
        int insertBatches = initialNodeCount / insertBatchSize;
        int propertiesPerChild = 10;

        // create a parent with a number of nodes initially
        Node parent = session.getRootNode().addNode("testRoot");
        session.save();

        Stopwatch globalSw = new Stopwatch();
        globalSw.start();
        if (print) {
            System.out.println("Starting to insert batches...");
        }
        for (int i = 0; i < insertBatches; i++) {
            // reload the parent in the session after it was saved
            parent = session.getNode("/testRoot");
            createSubgraph(session, parent, 1, insertBatchSize, propertiesPerChild, true, 1);
        }
        globalSw.stop();
        if (print) {
            System.out.println("Inserted " + initialNodeCount + " nodes in: " + globalSw.getSimpleStatistics());
        }
        globalSw.reset();
        globalSw.start();
        Stopwatch readSW = new Stopwatch();
        // add additional batches of nodes while reading the paths after each batch of children was added
        int batchCount = 36;
        int batchSize = 1000;
        for (int i = 0; i < batchCount; i++) {
            // creates batchSize
            long childCountAtBatchStart = session.getNode("/testRoot").getNodes().getSize();
            int newChildrenCount = createSubgraph(session, parent, 1, batchSize, propertiesPerChild, true, 1);
            readSW.start();

            // load each of the newly added children into the session and get their paths
            final long newChildCount = childCountAtBatchStart + newChildrenCount;

            for (long j = childCountAtBatchStart; j < newChildCount; j++) {
                final String childAbsPath = "/testRoot/childNode[" + j + "]";
                final Node child = session.getNode(childAbsPath);
                child.getPath();
                child.getName();
            }
            readSW.lap();

            // change the parent & save so that it's flushed from the cache
            session.getNode("/testRoot").setProperty("test", "test");
            session.save();

            // now get the paths of each child via parent relative path navigation
            for (long j = childCountAtBatchStart; j <= newChildCount; j++) {
                final String childName = "childNode[" + j + "]";
                final Node child = session.getNode("/testRoot").getNode(childName);
                child.getPath();
                child.getName();
            }
            readSW.lap();

            // change the parent & save so that it's flushed from the cache
            session.getNode("/testRoot").setProperty("test", "test1");
            session.save();

            // iterate through all the children of the parent and read the path
            NodeIterator nodeIterator = session.getNode("/testRoot").getNodes();
            while (nodeIterator.hasNext()) {
                final Node child = nodeIterator.nextNode();
                child.getPath();
                child.getName();
            }
            readSW.stop();
            if (print) {
                System.out.println("Time to read batch " + i + " : " + readSW.getSimpleStatistics());
            }
            readSW.reset();

            // change the parent & save so that it's flushed from the cache
            session.getNode("/testRoot").setProperty("test", "test2");
            session.save();

            globalSw.lap();
        }
        if (print) {
            System.out.println("Overall time to read:" + globalSw.getSimpleStatistics());
        }
    }

    @Test
    @Performance
    @FixFor( "MODE-2266" )
    public void insertNodesInFlatHierarchyWithParentThatAllowsSNS() throws Exception {
        int totalNodeCount = 1000000;
        int childrenPerNode = 1000;
        int propertiesPerNode = 0;
        String nodeType = "nt:unstructured";

        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        int totalNumberOfNodes = createSubgraphBreadthFirst(1, nodeType, "/testRoot", totalNodeCount, childrenPerNode,
                                                            propertiesPerNode, true);
        sw.stop();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + childrenPerNode + ": "
                           + sw.getSimpleStatistics());
    }

    @Test
    @Performance
    @FixFor( "MODE-2266" )
    public void insertNodesInFlatHierarchyWithParentThatDisallowsSNS() throws Exception {
        int totalNodeCount = 1000000;
        int childrenPerNode = 1000;
        int propertiesPerNode = 0;
        String nodeType = "nt:folder";

        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        int totalNumberOfNodes = createSubgraphBreadthFirst(1, nodeType, "/testRoot", totalNodeCount, childrenPerNode,
                                                            propertiesPerNode, false);
        sw.stop();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + childrenPerNode + ": "
                           + sw.getSimpleStatistics());
    }
    
    @Test
    @Performance
    @FixFor( "MODE-2109" )
    public void insertNodesInFlatHierarchyWithinUnorderedCollection() throws Exception {
        int totalNodeCount = 1000000;
        int childrenPerNode = 1000;
        int propertiesPerNode = 0;
        String nodeType = "test:largeCollection";

        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        int totalNumberOfNodes = createSubgraphBreadthFirst(1, nodeType, "/testRoot", totalNodeCount, childrenPerNode,
                                                            propertiesPerNode, false);
        sw.stop();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + childrenPerNode + ": "
                           + sw.getSimpleStatistics());
    }

    @Test
    @Performance
    @FixFor( "MODE-2109" )
    public void insertNodesInLargeCollection() throws Exception {
        int totalNodeCount = 1000000;
        int batchSize = 1000;
        int propertiesPerNode = 1;
        String parentAbsPath = "/testRoot";
        String nodeType = "test:largeCollection";
        
        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        Session insertingSession = null;
        for (int i = 0; i < totalNodeCount; i++) {
            insertingSession = insertingSession == null ? repository.login() : insertingSession;
            Stopwatch sessionWatch = new Stopwatch();
            sessionWatch.start();
            Node parentNode = insertingSession.getNode(parentAbsPath);
            Node child = parentNode.addNode("childNode" + i);
            for (int j = 0; j != propertiesPerNode; ++j) {
                String value = (j % 5 == 0) ? LARGE_STRING_VALUE : SMALL_STRING_VALUE;
                child.setProperty("property" + j, value);
            }
            if (i > 0 && (i + 1)  % batchSize == 0) {
                insertingSession.save();
                sessionWatch.stop();
                long size = parentNode.getNodes().getSize();
                System.out.println("Time to insert " + batchSize + " nodes " + sessionWatch.getSimpleStatistics() + "; Total size:" + size);
                insertingSession.logout();
                insertingSession = null;
            }
        }
        sw.stop();
        long totalNumberOfNodes = session.getNode(parentAbsPath).getNodes().getSize();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + batchSize + ": "
                           + sw.getSimpleStatistics());
    }

    @Test
    @Performance
    @FixFor( "MODE-2266" )
    public void insertNodesInDeepHierarchyWithParentThatAllowsSNS() throws Exception {
        int totalNodeCount = 1000000;
        int childrenPerNode = 1000;
        int propertiesPerNode = 0;
        String nodeType = "nt:unstructured";

        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        int totalNumberOfNodes = createSubgraphDepthFirst(nodeType, "/testRoot", totalNodeCount, childrenPerNode,
                                                          propertiesPerNode, true);
        sw.stop();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + childrenPerNode + ": "
                           + sw.getSimpleStatistics());
    }

    @Test
    @Performance
    @FixFor( "MODE-2266" )
    public void insertNodesInDeepHierarchyWithParentThatDisallowsSNS() throws Exception {
        int totalNodeCount = 1000000;
        int childrenPerNode = 1000;
        int propertiesPerNode = 0;
        String nodeType = "nt:folder";

        session.getRootNode().addNode("testRoot", nodeType);
        session.save();
        Stopwatch sw = new Stopwatch();
        sw.start();
        int totalNumberOfNodes = createSubgraphDepthFirst(nodeType, "/testRoot", totalNodeCount, childrenPerNode,
                                                          propertiesPerNode, false);
        sw.stop();
        System.out.println("Total time to insert " + totalNumberOfNodes + " nodes in batches of " + childrenPerNode + ": "
                           + sw.getSimpleStatistics());
    }

    /**
     * Creates a balanced subgraph of {@code totalNumberOfNodes} nodes, where each parent will have as close as possible to
     * {@code numberOfChildrenPerNode} children. The code will save the session after each set of children has been inserted under
     * a parent.
     * 
     * @param level
     * @param nodeType
     * @param parentAbsPath
     * @param totalNumberOfNodes
     * @param numberOfChildrenPerNode
     * @param numberOfPropertiesPerNode
     * @param useSns
     * @return the total number of nodes created
     * @throws RepositoryException
     * @throws RepositoryException
     */
    protected int createSubgraphBreadthFirst( int level,
                                              String nodeType,
                                              String parentAbsPath,
                                              int totalNumberOfNodes,
                                              int numberOfChildrenPerNode,
                                              int numberOfPropertiesPerNode,
                                              boolean useSns ) throws RepositoryException {
        int numberCreated = 0;
        if (totalNumberOfNodes < numberOfChildrenPerNode) {
            numberOfChildrenPerNode = totalNumberOfNodes;
        }

        List<String> childPaths = new ArrayList<String>();
        Stopwatch sw = new Stopwatch();
        sw.start();

        Session session = repository.login();
        try {
            Node parentNode = session.getNode(parentAbsPath);
            for (int i = 0; i != numberOfChildrenPerNode; ++i) {
                Node child = parentNode.addNode(useSns ? "childNode" : ("childNode" + i), nodeType);
                for (int j = 0; j != numberOfPropertiesPerNode; ++j) {
                    String value = (i % 5 == 0) ? LARGE_STRING_VALUE : SMALL_STRING_VALUE;
                    child.setProperty("property" + j, value);
                }
                numberCreated++;
                childPaths.add(child.getPath());
            }
            session.save();
        } finally {
            session.logout();
        }
        sw.stop();
        System.out.println("Time to insert " + numberCreated + " nodes on level " + level + ": " + sw.getSimpleStatistics());

        if (numberCreated == totalNumberOfNodes) {
            return numberCreated;
        }
        int remainingNodes = totalNumberOfNodes - numberCreated;
        if (remainingNodes <= 0) {
            return numberCreated;
        }
        int totalNodesForChild = remainingNodes / numberOfChildrenPerNode;
        int overflow = remainingNodes % numberOfChildrenPerNode;

        if (totalNodesForChild > 0) {
            for (String childPath : childPaths) {
                numberCreated += createSubgraphBreadthFirst(level + 1, nodeType, childPath, totalNodesForChild,
                                                            numberOfChildrenPerNode, numberOfPropertiesPerNode, useSns);
            }
        }

        if (overflow > 0) {
            // add some extra children to the last child from the list
            numberCreated += createSubgraphBreadthFirst(level + 1, nodeType, childPaths.get(childPaths.size() - 1), overflow,
                                                        overflow, numberOfPropertiesPerNode, useSns);
        }
        return numberCreated;
    }

    /**
     * Creates an "extremely" left-unbalanced subgraph of {@code totalNumberOfNodes} nodes, where each level will have
     * {@code numberOfChildrenPerNode} nodes under the left-most node. The code will save the session after each set of children
     * has been inserted under a parent.
     *
     * @param nodeType
     * @param parentAbsPath
     * @param totalNumberOfNodes
     * @param numberOfChildrenPerNode
     * @param numberOfPropertiesPerNode
     * @param useSns
     * @return the total number of nodes created
     * @throws RepositoryException
     */
    protected int createSubgraphDepthFirst( String nodeType,
                                            String parentAbsPath,
                                            int totalNumberOfNodes,
                                            int numberOfChildrenPerNode,
                                            int numberOfPropertiesPerNode,
                                            boolean useSns ) throws RepositoryException {
        if (totalNumberOfNodes < numberOfChildrenPerNode) {
            numberOfChildrenPerNode = totalNumberOfNodes;
        }

        String firstChildPath;
        Stopwatch sw = new Stopwatch();
        sw.start();
        int level = 1;
        do {
            sw.reset();
            sw.start();
            firstChildPath = null;
            Session session = repository.login();
            try {
                Node parentNode = session.getNode(parentAbsPath);

                for (int i = 0; i != numberOfChildrenPerNode; ++i) {
                    Node child = parentNode.addNode(useSns ? "childNode" : ("childNode" + i), nodeType);
                    for (int j = 0; j != numberOfPropertiesPerNode; ++j) {
                        String value = (i % 5 == 0) ? LARGE_STRING_VALUE : SMALL_STRING_VALUE;
                        child.setProperty("property" + j, value);
                    }
                    if (firstChildPath == null) {
                        firstChildPath = child.getPath();
                    }
                }
                session.save();
            } finally {
                session.logout();
            }
            sw.stop();
            System.out.println("Time to insert " + numberOfChildrenPerNode + " nodes on level " + level++ + ": "
                               + sw.getSimpleStatistics());
            totalNumberOfNodes -= numberOfChildrenPerNode;
            parentAbsPath = firstChildPath;

        } while (totalNumberOfNodes > 0);
        return totalNumberOfNodes;
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
            numberCreated += 1;
            if (depthRemaining > 1) {
                numberCreated += createSubgraph(session, child, depthRemaining - 1, numberOfChildrenPerNode,
                                                numberOfPropertiesPerNode, useSns, depthToSave);
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

    protected static void registerNodeTypes( String resourceName, Session session ) throws RepositoryException, IOException {
        InputStream stream = InMemoryPerformanceTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Workspace workspace = session.getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        ntMgr.registerNodeTypes(stream, true);
    }
}

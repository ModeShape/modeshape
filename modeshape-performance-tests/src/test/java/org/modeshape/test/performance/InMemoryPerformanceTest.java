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
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.CustomLoaderTest;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.TestingEnvironment;

public class InMemoryPerformanceTest implements CustomLoaderTest {

    private static final String LARGE_STRING_VALUE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed fermentum iaculis placerat. Mauris condimentum dapibus pretium. Vestibulum gravida sodales tellus vitae porttitor. Nunc dictum, eros vel adipiscing pellentesque, sem mi iaculis dui, a aliquam neque magna non turpis. Maecenas imperdiet est eu lorem placerat mattis. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum scelerisque molestie tristique. Mauris nibh diam, vestibulum eu condimentum at, facilisis at nisi. Maecenas vehicula accumsan lacus in venenatis. Nulla nisi eros, fringilla at dapibus mollis, pharetra at urna. Praesent in risus magna, at iaculis sapien. Fusce id velit id dui tempor hendrerit semper a nunc. Nam eget mauris tellus.";
    private static final String SMALL_STRING_VALUE = "The quick brown fox jumped over the moon. What? ";

    private static final Stopwatch STARTUP = new Stopwatch();
    private static final Stopwatch INFINISPAN_STARTUP = new Stopwatch();
    private static final Stopwatch MODESHAPE_STARTUP = new Stopwatch();

    private static final int MANY_NODES_COUNT = 10000;

    private Environment environment;
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

        environment = new TestingEnvironment(this);

        STARTUP.start();
        INFINISPAN_STARTUP.start();
        config = new RepositoryConfiguration(configDoc, configFileName, environment);
        INFINISPAN_STARTUP.stop();

        MODESHAPE_STARTUP.start();
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
            org.modeshape.jcr.TestingUtil.killEngine(engine);
        } finally {
            engine = null;
            repository = null;
            config = null;
            try {
                environment.shutdown();
            } finally {
                cleanUpFileSystem();
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

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
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

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
package org.modeshape.test.integration.performance;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.repository.ModeShapeConfiguration;
import org.modeshape.repository.ModeShapeEngine;

/**
 * A class that provides standard benchmarks to help quantify performance characteristics across connectors. This is not intended
 * to be a functional test, but will require proper functioning of much of the connector in order to execute. </p>
 */
public class ConnectorBenchmarkTest {

    private static final boolean FORCE_RUN = true;

    private ModeShapeEngine engine;
    private Graph graph;
    private String currentSourceName;
    private String[] validLargeValues;
    private Properties benchmarkProps;
    private Map<String, String> results = new HashMap<String, String>();
    private boolean useLargeValues = true;
    private boolean useUniqueLargeValues = true;

    /**
     * Scenario definitions. Each subarray defines a single scenario to be run. Scenarios are defined as an integer triple
     * consisting of {<test depth>, <test breadth>, <number of props per node>}. So the triple {3, 10, 7} indicates that the test
     * scenario should build a graph 3 nodes deep, with 10 child nodes per node and 7 properties per node.
     */
    // TODO: Make this configurable by source
    private int[][] insertScenarios = new int[][] { {3, 10, 7}, {3, 10, 100}, {7, 3, 7}, {7, 3, 100},};

    private String getCurrentSourceName() {
        return currentSourceName;
    }

    @BeforeClass
    public static void beforeAny() throws Exception {
        File fileSourceRoot = new File("./target/fileRepositoryRoot");
        if (!fileSourceRoot.exists()) fileSourceRoot.mkdirs();
    }

    @Before
    public void beforeEach() throws Exception {
        String propsFileName = "src/test/resources/performance/benchmark.properties";
        benchmarkProps = new Properties();
        benchmarkProps.load(new FileReader(propsFileName));

        // Load in the large value ...
        validLargeValues = new String[] {IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum1.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum2.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum3.txt"))};
    }

    private boolean shouldRunBenchmark() {
        return System.getProperty("runBenchmark") != null || FORCE_RUN;
    }

    private void addResult( String testName,
                            Stopwatch sw ) {
        results.put(testName + " (" + getCurrentSourceName() + ")", String.valueOf(sw.getTotalDuration()));
    }

    private String[] getValidWorkspaceNames() {
        String rawWorkspaceNames = (String)benchmarkProps.get(getCurrentSourceName() + ".validWorkspaceNames");
        if (rawWorkspaceNames == null) return new String[] {"benchmark"};

        return rawWorkspaceNames.split(",");
    }

    private String getValidWorkspaceName() {
        boolean useRandomWorkspaceName = Boolean.valueOf(benchmarkProps.getProperty(getCurrentSourceName()
                                                                                    + ".useRandomWorkspaceName"));

        if (useRandomWorkspaceName) {
            return "workspace-" + UUID.randomUUID().toString();
        }

        String[] workspaceNames = getValidWorkspaceNames();
        assertThat(workspaceNames, is(notNullValue()));
        assertThat(workspaceNames.length, is(not(0)));

        return workspaceNames[0];
    }

    private void startEngine() throws Exception {
        ModeShapeConfiguration config = new ModeShapeConfiguration();
        config.loadFrom("src/test/resources/performance/benchmarkConfig.xml");

        engine = config.build();
        engine.start();

        for (Problem problem : engine.getProblems()) {
            System.err.println(problem.getMessageString());
        }

        if (engine.getProblems().hasProblems()) {
            throw new RuntimeException();
        }
    }

    private void printResults() throws Exception {
        List<String> testNames = new ArrayList<String>(results.keySet());
        Collections.sort(testNames);

        for (String testName : testNames) {
            System.out.println();
            System.out.println(testName);
            System.out.println(results.get(testName));
        }
        System.out.flush();

    }

    private void stopEngine() throws Exception {
        engine.shutdown();
        engine.awaitTermination(30, TimeUnit.SECONDS);

        engine = null;
    }

    private Path path( Path parentPath,
                       String leafSegment ) {
        PathFactory pathFactory = engine.getExecutionContext().getValueFactories().getPathFactory();
        return pathFactory.create(parentPath, leafSegment);
    }

    @Test
    public void runBenchmark() throws Exception {
        if (!shouldRunBenchmark()) return;

        startEngine();
        Collection<String> sourceNames = engine.getRepositoryService().getRepositoryLibrary().getSourceNames();

        String configSourceName = engine.getRepositoryService().getConfigurationSourceName();

        for (String sourceName : sourceNames) {
            if (configSourceName.equals(sourceName)) continue;

            this.currentSourceName = sourceName;
            System.out.println("Running test for source '" + sourceName + "'");

            if (engine == null) {
                startEngine();
            }
            this.graph = engine.getGraph(sourceName);

            // run benchmarks
            try {
                benchmarkCreatingEmptyWorkspace();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            try {
                benchmarkDestroyingEmptyWorkspace();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                benchmarkReadingAndWritingToGraph();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                benchmarkInsertingNodes();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                benchmarkReadingEntireGraphAsSubgraph();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            /*
             *     re-read the same node (or subgraph) multiple times
            random reads by path
            random reads by uuid

             */

            stopEngine();
        }

        printResults();
    }

    /**
     * Create a structured subgraph by generating nodes with the supplied number of properties and children, to the supplied
     * maximum subgraph depth.
     * 
     * @param graph the graph that should be used; may not be null
     * @param initialPath the path to the new subgraph
     * @param depth the depth of the subgraph; must be a positive number
     * @param numberOfChildrenPerNode the number of child nodes to create under each node
     * @param numberOfPropertiesPerNode the number of properties to create on each node; must be 0 or more
     * @param stopwatch the stopwatch that should be used to measure the timings
     * @return the number of nodes created in the subgraph
     */
    private int createSubgraph( Graph graph,
                                  String initialPath,
                                  int depth,
                                  int numberOfChildrenPerNode,
                                  int numberOfPropertiesPerNode,
                                  Stopwatch stopwatch ) {
        // Calculate the number of nodes that we'll created, but subtract 1 since it doesn't create the root
        if (initialPath == null) initialPath = "";

        Graph.Batch batch = graph.batch();
        long totalNumberCreated = createChildren(batch,
                                                 initialPath,
                                                 "node",
                                                 numberOfChildrenPerNode,
                                                 numberOfPropertiesPerNode,
                                                 depth,
                                                 null); // don't output anything
        if (stopwatch != null) stopwatch.start();
        batch.execute();
        if (stopwatch != null) stopwatch.stop();

        return (int)totalNumberCreated;
    }

    private int createChildren( Graph.Batch useBatch,
                                  String parentPath,
                                  String nodePrefix,
                                  int number,
                                  int numProps,
                                  int depthRemaining,
                                  PrintWriter output ) {
        int numberCreated = 0;
        Graph.Batch batch = useBatch;
        String originalValue = "The quick brown fox jumped over the moon. What? ";
        if (batch == null) batch = graph.batch();
        for (int i = 0; i != number; ++i) {
            String path = parentPath + "/" + nodePrefix + (i + 1);
            Graph.Create<Graph.Batch> create = batch.create(path);
            String value = originalValue;
            for (int j = 0; j != numProps; ++j) {
                 value = value + originalValue;
                 if ((useLargeValues) && i % 3 == 0) {
                    String largeValue = validLargeValues[(int)Math.random() * validLargeValues.length];
                    if (useUniqueLargeValues && i % 3 == 0) {
                        // Use a large value for some properties ...
                        largeValue = originalValue;
                        for (int k = 0; k != 100; ++k) {
                            largeValue = largeValue + "(" + k + ")";
                        }
                    }
                    create = create.with("property" + (j + 1), largeValue);
                 } else {
                create = create.with("property" + (j + 1), value);
                 }
            }
            create.and();
        }
        numberCreated += number;
        if (useBatch == null) {
            batch.execute();
        }
        if (depthRemaining > 1) {
            for (int i = 0; i != number; ++i) {
                String path = parentPath + "/" + nodePrefix + (i + 1);
                numberCreated += createChildren(useBatch, path, nodePrefix, number, numProps, depthRemaining - 1, null);
            }
        }
        return numberCreated;
    }

    /*
     * BENCHMARK IMPLEMENTATIONS GO BELOW THIS POINT
     */

    private void benchmarkCreatingEmptyWorkspace() throws Exception {
        if (!shouldRunBenchmark()) return;

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        Stopwatch sw = new Stopwatch();

        String[] validWorkspaceNames = this.getValidWorkspaceNames();

        for (int i = 0; i < validWorkspaceNames.length; i++) {
            sw.start();
            graph.createWorkspace().named(validWorkspaceNames[i]);
            sw.stop();
        }

        graph.useWorkspace(defaultWorkspaceName);
        for (int i = 0; i < validWorkspaceNames.length; i++) {
            graph.destroyWorkspace().named(validWorkspaceNames[i]);
        }

        addResult("Create Empty Workspace", sw);
    }

    private void benchmarkDestroyingEmptyWorkspace() throws Exception {
        if (!shouldRunBenchmark()) return;

        Stopwatch sw = new Stopwatch();

        String[] validWorkspaceNames = this.getValidWorkspaceNames();
        String defaultWorkspace = graph.getCurrentWorkspaceName();

        for (int i = 0; i < validWorkspaceNames.length; i++) {
            graph.createWorkspace().named(validWorkspaceNames[i]);
        }

        graph.useWorkspace(defaultWorkspace);

        for (int i = 0; i < validWorkspaceNames.length; i++) {
            sw.start();
            graph.destroyWorkspace().named(validWorkspaceNames[i]);
            sw.stop();
        }

        addResult("Destroy Empty Workspace", sw);
    }

    private void benchmarkInsertingNodes() throws Exception {
        if (!shouldRunBenchmark()) return;

        Stopwatch sw = new Stopwatch();

        String defaultWorkspace = graph.getCurrentWorkspaceName();

        int[] scenario;
        int nodeCount;
        for (int i = 0; i < insertScenarios.length; i++) {
            scenario = insertScenarios[i];
            int depth = scenario[0];
            int breadth = scenario[1];
            int numberOfProps = scenario[2];

            String workspaceName = getValidWorkspaceName();
            graph.createWorkspace().named(workspaceName);
            graph.useWorkspace(workspaceName);
            sw.reset();

            nodeCount = createSubgraph(graph, "/", depth, breadth, numberOfProps, sw);

            addResult("Inserted " + depth + "x" + breadth + " Tree w/ " + numberOfProps + " properties - " + nodeCount
                      + " - batch", sw);

            graph.useWorkspace(defaultWorkspace);
            graph.destroyWorkspace().named(workspaceName);
        }
    }

    private void benchmarkReadingEntireGraphAsSubgraph() throws Exception {
        Stopwatch sw = new Stopwatch();

        String workspaceName = getValidWorkspaceName();
        graph.createWorkspace().named(workspaceName);

        int depth = 3;
        int breadth = 10;
        int numberOfProps = 7;

        int nodeCount = createSubgraph(graph, "/", depth, breadth, numberOfProps, null);

        sw.start();
        graph.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
        sw.stop();

        addResult("Read " + depth + "x" + breadth + " Tree w/ " + numberOfProps + " properties - " + nodeCount + " - batch", sw);
    }

    private void benchmarkReadingAndWritingToGraph() throws Exception {
        List<Path> existingPaths = new ArrayList<Path>();
        final double WRITE_PCT = 10;
        final int INITIAL_DEPTH = 3;
        final int INITIAL_BREADTH = 5;
        final int NUM_PROPS = 7;
        final int ITERATIONS = 1000;

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = getValidWorkspaceName();

        graph.createWorkspace().named(workspaceName);
        graph.useWorkspace(workspaceName);

        createSubgraph(graph, "/", INITIAL_BREADTH, INITIAL_DEPTH, NUM_PROPS, null);

        Subgraph sg = graph.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
        for (SubgraphNode sgn : sg) {
            existingPaths.add(sgn.getLocation().getPath());
        }

        Path readPath;
        Stopwatch sw = new Stopwatch();
        sw.start();

        for (int i = 0; i < ITERATIONS; i++) {

            // Read two nodes
            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            graph.getNodeAt(readPath);

            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            graph.getNodeAt(readPath);

            if (Math.random() * 100 < WRITE_PCT) {
                // Write one node
                Path newNode = path(readPath, "n" + (System.currentTimeMillis() % 10000));

                graph.create(newNode).and();
                existingPaths.add(newNode);

            } else {
                // or read a third node
                readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
                graph.getNodeAt(readPath);

            }
        }

        sw.stop();

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

        addResult("Read/Write " + INITIAL_DEPTH + "x" + INITIAL_BREADTH + " Tree w/ " + NUM_PROPS + " properties - " + WRITE_PCT
                  + "% writes", sw);
    }
}

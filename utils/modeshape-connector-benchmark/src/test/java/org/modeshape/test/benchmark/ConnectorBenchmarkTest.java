package org.modeshape.test.benchmark;
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
import org.modeshape.common.annotation.Immutable;
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
    @SuppressWarnings( "unused" )
    // Unused if useLargeValues && useUniqueLargeValues == false
    private String[] validLargeValues;
    private Properties benchmarkProps;
    private Map<String, Map<String, TestResult>> results = new HashMap<String, Map<String, TestResult>>();
    private boolean useLargeValues = false;
    private boolean useUniqueLargeValues = false;

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
        Map<String, TestResult> testResult = results.get(testName);
        if (testResult == null) {
            testResult = new HashMap<String, TestResult>();
            results.put(testName, testResult);
        }

        testResult.put(getCurrentSourceName(), new TestResult(testName, getCurrentSourceName(),
                                   sw.getTotalDuration().getDurationInMilliseconds().longValue()));
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
        final int GRAPHS_PER_ROW = 3;

        List<String> testNames = new ArrayList<String>(results.keySet());
        Collections.sort(testNames);

        File templateFile = new File("./src/test/resources/performance/benchmark.html");
        String template = IoUtil.read(templateFile);

        StringBuilder dataTable = new StringBuilder();
        int testIndex = 0;
        for (String testName : testNames) {
            // var data = new google.visualization.DataTable();

            dataTable.append("var data" + testIndex + " = new google.visualization.DataTable();\n");

            // data.addColumn('number', 'In-Memory Store');
            // data.addColumn('number', 'File Store');
            // data.addRows(1);
            // data.setValue(0, 0, 1); // In-Memory Store Value
            // data.setValue(0, 1, 30); // File Store Value

            Map<String, TestResult> testResult = results.get(testName);
            List<String> connectorNames = new ArrayList<String>(testResult.keySet());
            Collections.sort(connectorNames);

            for (String connectorName : connectorNames) {
                dataTable.append("data" + testIndex + ".addColumn('number', '" + connectorName + "');\n");
            }

            dataTable.append("data" + testIndex + ".addRows(" + connectorNames.size() + ");\n");

            int index = 0;
            for (String connectorName : connectorNames) {
                TestResult connectorResult = testResult.get(connectorName);
                dataTable.append("data" + testIndex + ".setValue(0, " + index++ + ", "
                                 + connectorResult.getDurationInMilliseconds() + ");\n");
            }

            // var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
            // chart.draw(data, {width: 300, height: 240, hAxis: { textPosition: 'in'}, title: 'Red Repeated Path from 3x5 Tree w/
            // 7 properties (In-Memory Store)'});
            dataTable.append("var chart = new google.visualization.ColumnChart(document.getElementById('chart_div" + testIndex
                          + "'));\n");
            dataTable.append("chart.draw(data" + testIndex
                             + ", {width: 300, height: 240, hAxis: { textPosition: 'in'}, vAxis: {minValue: 0}, title: '"
                             + testName + "'});\n");
            testIndex++;
        }

        template = template.replaceFirst("\\$DATA\\$", dataTable.toString());

        StringBuilder divs = new StringBuilder();

        for (int i = 0; i < testIndex; i++) {
            divs.append("<td><div id=\"chart_div" + i + "\"></div></td>\n");
            if (i % GRAPHS_PER_ROW == GRAPHS_PER_ROW - 1 && i < testIndex - 1) {
                divs.append("</tr><tr>\n");
            }
        }

        template = template.replaceFirst("\\$DIVS\\$", divs.toString());

        File outputFile = new File("./target/benchmark.html");
        IoUtil.write(template, outputFile);

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

    @Immutable
    public class TestResult {
        private String testName;
        private String connectorName;
        private long durationInMilliseconds;

        public TestResult( String testName,
                           String connector,
                           long milliseconds ) {
            super();
            this.testName = testName;
            this.connectorName = connector;
            this.durationInMilliseconds = milliseconds;
        }

        public String getTestName() {
            return testName;
        }

        public String getConnectorName() {
            return connectorName;
        }

        public long getDurationInMilliseconds() {
            return durationInMilliseconds;
        }

    }

    private boolean skipTest(String testName) {
        return benchmarkProps.getProperty("skippedTests").indexOf(getCurrentSourceName() + "." + testName) != -1;
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
                if (!skipTest("benchmarkCreatingEmptyWorkspace")) {
                    benchmarkCreatingEmptyWorkspace();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkDestroyingEmptyWorkspace")) {
                    benchmarkDestroyingEmptyWorkspace();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkReadingAndWritingToGraph")) {
                    benchmarkReadingAndWritingToGraph();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkInsertingNodes")) {
                    benchmarkInsertingNodes();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkReadingEntireGraphAsSubgraph")) {
                    benchmarkReadingEntireGraphAsSubgraph();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkRandomReadsByPath")) {
                    benchmarkRandomReadsByPath();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkRandomReadsByUuid")) {
                    benchmarkRandomReadsByUuid();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                if (!skipTest("benchmarkRepeatedReadOfSameNode")) {
                    benchmarkRepeatedReadOfSameNode();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

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
                // value = value + originalValue;
                if ((useLargeValues || useUniqueLargeValues) && i % 3 == 0) {
                    // Use a large value for some properties ...
                    String largeValue = originalValue;
                    for (int k = 0; k != 100; ++k) {
                        largeValue = largeValue + "(" + k + ")";
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

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = getValidWorkspaceName();
        graph.createWorkspace().named(workspaceName);

        int depth = 3;
        int breadth = 10;
        int numberOfProps = 7;

        int nodeCount = createSubgraph(graph, "/", depth, breadth, numberOfProps, null);

        sw.start();
        graph.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
        sw.stop();

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

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

        for (int i = 0; i < ITERATIONS; i++) {

            // Read two nodes
            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            sw.start();
            graph.getNodeAt(readPath);
            sw.stop();

            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            sw.start();
            graph.getNodeAt(readPath);
            sw.stop();

            if (Math.random() * 100 < WRITE_PCT) {
                // Write one node
                Path newNode = path(readPath, "n" + (System.currentTimeMillis() % 10000));

                sw.start();
                graph.create(newNode).and();
                sw.stop();
                existingPaths.add(newNode);

            } else {
                // or read a third node
                readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
                sw.start();
                graph.getNodeAt(readPath);
                sw.stop();
            }
        }

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

        addResult("Read/Write " + INITIAL_DEPTH + "x" + INITIAL_BREADTH + " Tree w/ " + NUM_PROPS + " properties - " + WRITE_PCT
                  + "% writes", sw);
    }

    private void benchmarkRandomReadsByPath() throws Exception {
        List<Path> existingPaths = new ArrayList<Path>();
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

        for (int i = 0; i < ITERATIONS; i++) {

            // Read three nodes
            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            sw.start();
            graph.getNodeAt(readPath);
            sw.stop();

            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            sw.start();
            graph.getNodeAt(readPath);
            sw.stop();

            readPath = existingPaths.get((int)Math.floor(Math.random() * existingPaths.size()));
            sw.start();
            graph.getNodeAt(readPath);
            sw.stop();
        }

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

        addResult("Read Random Path from " + INITIAL_DEPTH + "x" + INITIAL_BREADTH + " Tree w/ " + NUM_PROPS + " properties", sw);
    }

    private void benchmarkRandomReadsByUuid() throws Exception {
        List<UUID> existingUuids = new ArrayList<UUID>();
        final int INITIAL_DEPTH = 3;
        final int INITIAL_BREADTH = 5;
        final int NUM_PROPS = 7;
        final int ITERATIONS = 1000;

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = getValidWorkspaceName();

        graph.createWorkspace().named(workspaceName);
        graph.useWorkspace(workspaceName);

        graph.create("/uuidTest").and();
        org.modeshape.graph.Node testNode = graph.getNodeAt("/uuidTest");
        if (testNode.getLocation().getUuid() == null) return;

        createSubgraph(graph, "/", INITIAL_BREADTH, INITIAL_DEPTH, NUM_PROPS, null);

        Subgraph sg = graph.getSubgraphOfDepth(Integer.MAX_VALUE).at("/");
        for (SubgraphNode sgn : sg) {
            existingUuids.add(sgn.getLocation().getUuid());
        }

        UUID readUuid;
        Stopwatch sw = new Stopwatch();

        for (int i = 0; i < ITERATIONS; i++) {
            // Read three nodes
            readUuid = existingUuids.get((int)Math.floor(Math.random() * existingUuids.size()));
            sw.start();
            graph.getNodeAt(readUuid);
            sw.stop();

            readUuid = existingUuids.get((int)Math.floor(Math.random() * existingUuids.size()));
            sw.start();
            graph.getNodeAt(readUuid);
            sw.stop();

            readUuid = existingUuids.get((int)Math.floor(Math.random() * existingUuids.size()));
            sw.start();
            graph.getNodeAt(readUuid);
            sw.stop();
        }

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

        addResult("Read Random UUID from " + INITIAL_DEPTH + "x" + INITIAL_BREADTH + " Tree w/ " + NUM_PROPS + " properties", sw);
    }

    private void benchmarkRepeatedReadOfSameNode() throws Exception {
        final int INITIAL_DEPTH = 3;
        final int INITIAL_BREADTH = 5;
        final int NUM_PROPS = 7;
        final int ITERATIONS = 10;

        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = getValidWorkspaceName();

        graph.createWorkspace().named(workspaceName);
        graph.useWorkspace(workspaceName);

        createSubgraph(graph, "/", INITIAL_BREADTH, INITIAL_DEPTH, NUM_PROPS, null);

        Stopwatch sw = new Stopwatch();

        for (int i = 0; i < ITERATIONS; i++) {
            sw.start();
            graph.getNodeAt("/node1/node2");
            sw.stop();
        }

        graph.useWorkspace(defaultWorkspaceName);
        graph.destroyWorkspace().named(workspaceName);

        addResult("Read Repeated Path from " + INITIAL_DEPTH + "x" + INITIAL_BREADTH + " Tree w/ " + NUM_PROPS + " properties",
                  sw);
    }
}

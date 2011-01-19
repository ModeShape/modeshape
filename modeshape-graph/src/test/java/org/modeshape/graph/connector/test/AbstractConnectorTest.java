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
package org.modeshape.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.CollectGarbageRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.Request;

/**
 * A class that provides standard reading verification tests for connectors. This class is designed to be extended for each
 * connector, and in each subclass the {@link #setUpSource()} method is defined to provide a valid {@link RepositorySource} for
 * the connector to be tested.
 * <p>
 * By default, this class sets up the repository before each test is run, ensuring that all tests have a pristine environment.
 * However, subclasses with tests that never modify the repository content may wish to set up the repository only once. Note: this
 * should be used with caution, but this can be achieved by overriding {@link #afterEach()} to do nothing. The repository source
 * will be set up during the first test, and will not be shut down until {@link #afterAll() all tests have been run}.
 * </p>
 */
public abstract class AbstractConnectorTest {

    protected ExecutionContext context;
    protected RepositorySource source;
    protected Graph graph;
    protected RepositorySource configSource;
    private RepositoryConnectionFactory connectionFactory;
    private List<RepositoryConnection> openConnections;
    private boolean running;
    private Location rootLocation;
    private UUID rootUuid;
    protected Observer observer;
    protected LinkedList<Changes> allChanges;
    protected boolean print = false;
    protected boolean useLargeValues = false;
    protected boolean useUniqueLargeValues = false;
    protected PrintStream output = null;
    protected long largeValueCounter = 0L;

    public void startRepository() throws Exception {
        if (!running) {
            // Set up the connection factory to other sources ...

            // Set up the execution context ...
            context = setUpExecutionContext(new ExecutionContext());

            // Set up the configuration source ...
            configSource = setUpConfigurationSource();

            // Set up the Observer and the list into which all Changes will be placed ...
            allChanges = new LinkedList<Changes>();
            observer = new Observer() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
                 */
                public void notify( Changes changes ) {
                    AbstractConnectorTest.this.allChanges.add(changes);
                }
            };

            // Set up the source ...
            source = setUpSource();

            // Now set up the connection factory ...
            openConnections = new ArrayList<RepositoryConnection>();
            connectionFactory = new RepositoryConnectionFactory() {
                @SuppressWarnings( "synthetic-access" )
                public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                    if (!source.getName().equals(sourceName)) return null;
                    RepositoryConnection connection = source.getConnection();
                    if (connection == null) {
                        throw new RepositorySourceException("Unable to create a repository connection to " + source.getName());
                    }
                    openConnections.add(connection);
                    return connection;
                }
            };

            // Initialize the source with the rest of the environment ...
            source.initialize(new RepositoryContext() {
                public ExecutionContext getExecutionContext() {
                    return context;
                }

                @SuppressWarnings( "synthetic-access" )
                public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                    return connectionFactory;
                }

                public Observer getObserver() {
                    return observer;
                }

                public Subgraph getConfiguration( int depth ) {
                    Subgraph result = null;
                    if (configSource != null) {
                        Graph config = Graph.create(configSource, getExecutionContext());
                        config.useWorkspace(null); // default workspace
                        result = config.getSubgraphOfDepth(depth).at(source.getName());
                    }
                    return result;
                }
            });

            // And set up the graph instance ...
            graph = Graph.create(source.getName(), connectionFactory, context);

            // Now have the source initialize the content (if any) ...
            initializeContent(graph);
            running = true;
        }
    }

    public void shutdownRepository() throws Exception {
        if (running) {
            try {
                // Shut down the connections to the source ...
                for (RepositoryConnection connection : openConnections) {
                    connection.close();
                }
            } finally {
                openConnections = null;
                running = false;
                rootLocation = null;
                rootUuid = null;
            }
        }
        if (source != null) {
            // Close the source, notifying it that it can reclaim any resources ...
            try {
                source.close();
            } finally {
                source = null;
            }
        }
        graph = null;
        context = null;
        configSource = null;
        connectionFactory = null;
    }

    /**
     * Method that is executed before each test. By default, this method {@link #startRepository() sets up the repository}.
     * 
     * @throws Exception
     */
    @Before
    public void beforeEach() throws Exception {
        print = false;
        startRepository();
    }

    /**
     * Method that is executed after each test. By default, this method {@link #shutdownRepository() shuts down the repository}.
     * 
     * @throws Exception
     */
    @After
    public void afterEach() throws Exception {
        shutdownRepository();
        cleanUpSourceResources();
    }

    /**
     * Method executed after all tests have completed. By default, this method ensures that the repository has been shut down (if
     * this was not done in {@link #afterEach()}).
     * 
     * @throws Exception
     */
    @AfterClass
    public static void afterAll() throws Exception {
        // shutdownRepository();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Methods used in the initialization and set up of test methods
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Set up the {@link ExecutionContext} for each unit test.
     * 
     * @param context the context that may be used directly, or used to create another context; never null
     * @return the execution context; may not be null
     */
    protected ExecutionContext setUpExecutionContext( ExecutionContext context ) {
        return context;
    }

    /**
     * Set up a {@link RepositorySource} that contains the configuration for the source being tested (and any other sources, if
     * needed). The source's default workspace will be used. This implementation returns null by default.
     * 
     * @return the configuration source, or null if no configuration is needed
     * @throws Exception if there is a problem setting up the source
     */
    protected RepositorySource setUpConfigurationSource() throws Exception {
        return null;
    }

    /**
     * Set up a {@link RepositorySource} that should be used for each of the unit tests.
     * 
     * @return the repository source
     * @throws Exception if there is a problem setting up the source
     */
    protected abstract RepositorySource setUpSource() throws Exception;

    /**
     * After the source has been closed, clean up any resources that may have been created by the source.
     * 
     * @throws Exception if there is a problem setting up the source
     */
    protected void cleanUpSourceResources() throws Exception {
    }

    /**
     * Initialize the content of the {@link RepositorySource} set up for each of the unit tests. This method is called shortly
     * after {@link #setUpSource()} is called and the returned RepositorySource is
     * {@link RepositorySource#initialize(RepositoryContext) initialized}.
     * 
     * @param graph the graph for the {@link RepositorySource} returned from {@link #setUpSource()}; never null
     * @throws Exception if there is a problem initializing the source
     */
    protected abstract void initializeContent( Graph graph ) throws Exception;

    // ----------------------------------------------------------------------------------------------------------------
    // Helper methods commonly needed in unit tests
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Utility to create a {@link Name} from a string.
     * 
     * @param name the string form of the name
     * @return the name object
     * @throws ValueFormatException if a name could not be created from the supplied string
     */
    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * Utility to create a {@link Path.Segment} from a string, where there will be no index
     * 
     * @param name the string form of the path segment, which may include a 1-based same-name-sibling index
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path.Segment segment( String name ) {
        return context.getValueFactories().getPathFactory().createSegment(name);
    }

    /**
     * Utility to create a {@link Path} from a string.
     * 
     * @param path the string form of the path
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    /**
     * Utility to create a {@link Path} from a parent string and a subpath string.
     * 
     * @param parentPath the string form of the parent path
     * @param subPath the string form of the subpath
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path path( String parentPath,
                         String subPath ) {
        return path(path(parentPath), subPath);
    }

    /**
     * Utility to create a {@link Path} from a parent string and a subpath string.
     * 
     * @param parentPath the string form of the parent path
     * @param subPath the string form of the subpath
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path path( Path parentPath,
                         String subPath ) {
        return context.getValueFactories().getPathFactory().create(parentPath, subPath);
    }

    /**
     * Utility to create a {@link Location} from a string path.
     * 
     * @param path the string form of the path
     * @return the location
     */
    protected Location location( String path ) {
        return Location.create(path(path));
    }

    /**
     * Utility to create a {@link Location} from a UUID.
     * 
     * @param uuid the UUID
     * @return the location
     */
    protected Location location( UUID uuid ) {
        return Location.create(uuid);
    }

    protected UUID getRootNodeUuid() {
        if (rootUuid == null) {
            Node root = graph.getNodeAt("/");
            rootLocation = root.getLocation();
            rootUuid = rootLocation.getUuid();
            if (rootUuid == null) {
                Property uuid = root.getProperty(ModeShapeLexicon.UUID);
                if (uuid != null) {
                    rootUuid = context.getValueFactories().getUuidFactory().create(uuid.getFirstValue());
                }
            }
            if (rootUuid == null) {
                Property uuid = root.getProperty(JcrLexicon.UUID);
                if (uuid != null) {
                    rootUuid = context.getValueFactories().getUuidFactory().create(uuid.getFirstValue());
                }
            }
        }
        return rootUuid;
    }

    protected String string( Object value ) {
        if (value instanceof Property) value = ((Property)value).getFirstValue();
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected DateTime date( Object value ) {
        if (value instanceof Property) value = ((Property)value).getFirstValue();
        return context.getValueFactories().getDateFactory().create(value);
    }

    protected long longValue( Object value ) {
        if (value instanceof Property) value = ((Property)value).getFirstValue();
        return context.getValueFactories().getLongFactory().create(value);
    }

    protected Name name( Object value ) {
        if (value instanceof Property) value = ((Property)value).getFirstValue();
        return context.getValueFactories().getNameFactory().create(value);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility method that may be used to execute requests against a repository ...
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Submit the supplied request to the {@link #source source} for processing, and then return the request.
     * 
     * @param request the request to be processed
     * @return the request after processing (for method chaining purposes)
     * @param <T> the type of request
     * @throws RuntimeException if the request generated a runtime error
     * @throws RepositorySourceException if the request generated an error that was not a {@link RuntimeException}
     */
    protected <T extends Request> T execute( T request ) {
        // Get a connection ...
        RepositoryConnection connection = connectionFactory.createConnection(source.getName());
        try {
            connection.execute(context, request);
        } finally {
            connection.close();
        }
        if (request.hasError()) {
            Throwable error = request.getError();
            if (error instanceof RuntimeException) throw (RuntimeException)error;
            throw new RepositorySourceException(source.getName(), error);
        }
        return request;
    }

    // ----------------------------------------------------------------------------------------------
    // Utility method for reading a node in all (or most) possible ways and comparing the results ...
    // ----------------------------------------------------------------------------------------------

    /**
     * Read the node at the supplied location, using a variety of techniques to read the node and compare that each technique
     * returned the same node. This method reads the entire node (via {@link Graph#getNodeAt(Location)}, which uses
     * {@link ReadNodeRequest}), reads all of the properties on the node (via {@link Graph#getProperties()}, which uses
     * {@link ReadAllPropertiesRequest}), and reads all of the children of the node (via {@link Graph#getChildren()}, which uses
     * {@link ReadAllChildrenRequest}).
     * 
     * @param location the location; may not be null
     * @return the node that was read
     */
    public Node readNodeThoroughly( Location location ) {
        assertThat(location, is(notNullValue()));
        Node result = null;
        if (location.hasPath() && location.hasIdProperties()) {
            // Read the node by the full location ...
            result = graph.getNodeAt(location);

            // Read the node by the path ...
            Node resultByPath = graph.getNodeAt(location.getPath());
            assertSameNode(resultByPath, result);

            // Read the node by identification properties ...
            if (location.hasIdProperties()) {
                Node resultByIdProps = graph.getNodeAt(location.getIdProperties());
                assertSameNode(resultByIdProps, result);
            }

            // Check the result has the correct location ...
            assertThat("The node that was read doesn't have the expected location", result.getLocation(), is(location));
        } else {
            // Read the node by using the location (as is)
            result = graph.getNodeAt(location);

            // Check the result has the correct location ...
            assertThat("The node that was read doesn't have the expected location",
                       result.getLocation().equals(location),
                       is(true));
        }

        // Read all the properties of the node ...
        assertSameProperties(result, graph.getProperties().on(location));

        // Read all the children of the node ...
        assertThat(graph.getChildren().of(location), is(result.getChildren()));

        return result;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility method that may be used to create content in a repository ...
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Create a structured subgraph by generating nodes with the supplied number of properties and children, to the supplied
     * maximum subgraph depth.
     * 
     * @param graph the graph that should be used; may not be null
     * @param initialPath the path to the new subgraph, or null if the root path should be used
     * @param depth the depth of the subgraph; must be a positive number
     * @param numberOfChildrenPerNode the number of child nodes to create under each node
     * @param numberOfPropertiesPerNode the number of properties to create on each node; must be 0 or more
     * @param oneBatch true if all of the nodes are to be created in one batch
     * @param stopwatch the stopwatch that should be used to measure the timings
     * @return the number of nodes created in the subgraph
     */
    protected int createSubgraph( Graph graph,
                                  String initialPath,
                                  int depth,
                                  int numberOfChildrenPerNode,
                                  int numberOfPropertiesPerNode,
                                  boolean oneBatch,
                                  Stopwatch stopwatch ) {
        return createSubgraph(graph,
                              initialPath,
                              depth,
                              numberOfChildrenPerNode,
                              numberOfPropertiesPerNode,
                              oneBatch,
                              stopwatch,
                              null,
                              null);
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
     * @param oneBatch true if all of the nodes are to be created in one batch
     * @param stopwatch the stopwatch that should be used to measure the timings
     * @param output the writer to which metrics and messages should be written, or null if no such information should be written
     * @param description the description of this subgraph (used for logging and printing), or null if the description should be
     *        generated automatically
     * @return the number of nodes created in the subgraph
     */
    protected int createSubgraph( Graph graph,
                                  String initialPath,
                                  int depth,
                                  int numberOfChildrenPerNode,
                                  int numberOfPropertiesPerNode,
                                  boolean oneBatch,
                                  Stopwatch stopwatch,
                                  PrintStream output,
                                  String description ) {
        // Calculate the number of nodes that we'll created, but subtrace 1 since it doesn't create the root
        long totalNumber = calculateTotalNumberOfNodesInTree(numberOfChildrenPerNode, depth, false);
        if (initialPath == null) initialPath = "";
        if (description == null) {
            description = "" + numberOfChildrenPerNode + "x" + depth + " tree with " + numberOfPropertiesPerNode
                          + " properties per node";
        }

        if (output != null) output.println(description + " (" + totalNumber + " nodes):");
        long totalNumberCreated = 0;
        Graph.Batch batch = oneBatch ? graph.batch() : null;
        if (batch != null) {
            totalNumberCreated += createChildren(batch,
                                                 initialPath,
                                                 "node",
                                                 numberOfChildrenPerNode,
                                                 numberOfPropertiesPerNode,
                                                 depth,
                                                 null); // don't output anything
            if (stopwatch != null) stopwatch.start();
            batch.execute();
        } else {
            if (stopwatch != null) stopwatch.start();
            totalNumberCreated += createChildren(null,
                                                 initialPath,
                                                 "node",
                                                 numberOfChildrenPerNode,
                                                 numberOfPropertiesPerNode,
                                                 depth,
                                                 null); // don't output anything
        }
        if (stopwatch != null) {
            stopwatch.stop();
            if (output != null) {
                output.println("    " + getTotalAndAverageDuration(stopwatch, totalNumberCreated));
            }

            // Perform second batch ...
            batch = graph.batch();
            totalNumberCreated += createChildren(batch, initialPath, "secondBranch", 2, numberOfPropertiesPerNode, 2, null);
            Stopwatch sw = new Stopwatch();
            sw.start();
            batch.execute();
            sw.stop();
            print("     final " + getTotalAndAverageDuration(sw, totalNumberCreated));
            assertThat(totalNumberCreated, is(totalNumber + calculateTotalNumberOfNodesInTree(2, 2, false)));
        }
        return (int)totalNumberCreated;

    }

    protected void print( String msg ) {
        if (print) {
            System.out.println(msg);
        }
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

    /**
     * Utility to create a number of children.
     * 
     * @param useBatch
     * @param parentPath
     * @param nodePrefix
     * @param number
     * @param numProps
     * @param depthRemaining
     * @param output
     * @return the number of children created
     */
    protected int createChildren( Graph.Batch useBatch,
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
                    if (useUniqueLargeValues) largeValue = "" + (++largeValueCounter) + largeValue;
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
            if (output != null) output.println("         total created ... " + numberCreated);
        }
        if (depthRemaining > 1) {
            for (int i = 0; i != number; ++i) {
                String path = parentPath + "/" + nodePrefix + (i + 1);
                numberCreated += createChildren(useBatch, path, nodePrefix, number, numProps, depthRemaining - 1, null);
                if (output != null) output.println("         total created ... " + numberCreated);
            }
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

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods to work with nodes
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Assert that the two supplied subgraphs have the same structure, and that corresponding nodes in the subgraphs have the same
     * properties and children.
     * 
     * @param subgraph1 the first subgraph; may not be null
     * @param subgraph2 the second subgraph; may not be null
     * @param idPropertiesShouldMatch true if the identification properties of each corresponding node should match, or false if
     *        the identification properties should be ignored
     * @param pathsShouldMatch true if the absolute paths of the subgraphs should match, or false if only the relative paths
     *        within the subgraphs should match
     */
    public static void assertEquivalentSubgraphs( Subgraph subgraph1,
                                                  Subgraph subgraph2,
                                                  boolean idPropertiesShouldMatch,
                                                  boolean pathsShouldMatch ) {
        assertThat(subgraph1, is(notNullValue()));
        assertThat(subgraph2, is(notNullValue()));

        // Shortcut ...
        if (subgraph1.getLocation().isSame(subgraph2.getLocation())) return;

        Path rootPath1 = subgraph1.getRoot().getLocation().getPath();
        Path rootPath2 = subgraph2.getRoot().getLocation().getPath();

        // Iterate over each subgraph. Note that because each location should have a path, the path can be used
        // to ensure the structure matches.
        Iterator<SubgraphNode> iter1 = subgraph1.iterator();
        Iterator<SubgraphNode> iter2 = subgraph2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            Node node1 = iter1.next();
            Node node2 = iter2.next();

            assertThat(node1, is(notNullValue()));
            assertThat(node2, is(notNullValue()));

            // Each node should have equivalent paths ..
            assertThat(node1.getLocation().hasPath(), is(true));
            assertThat(node2.getLocation().hasPath(), is(true));
            if (pathsShouldMatch) {
                assertThat(node1.getLocation().getPath(), is(node2.getLocation().getPath()));
            } else {
                Path relativeNode1 = node1.getLocation().getPath().relativeTo(rootPath1);
                Path relativeNode2 = node2.getLocation().getPath().relativeTo(rootPath2);
                assertThat(relativeNode1, is(relativeNode2));
            }

            // Each node should have the same Identification properties ...
            if (idPropertiesShouldMatch) {
                assertThat(node1.getLocation().getIdProperties(), is(node2.getLocation().getIdProperties()));
            }

            // Do not compare the workspace name.

            // Each node should have the same properties (excluding any identification properties) ...
            Map<Name, Property> properties1 = new HashMap<Name, Property>(node1.getPropertiesByName());
            Map<Name, Property> properties2 = new HashMap<Name, Property>(node2.getPropertiesByName());
            if (!idPropertiesShouldMatch) {
                for (Property idProperty : node1.getLocation().getIdProperties()) {
                    properties1.remove(idProperty.getName());
                }
                for (Property idProperty : node2.getLocation().getIdProperties()) {
                    properties2.remove(idProperty.getName());
                }
            }
            assertThat(properties1, is(properties2));

            // Each node should have the same children. We can check this, tho this will be enforced when comparing paths ...
            assertThat(node1.getChildrenSegments(), is(node2.getChildrenSegments()));
        }

        // There should be no more nodes in either iterator ...
        assertThat(iter1.hasNext(), is(false));
        assertThat(iter2.hasNext(), is(false));
    }

    /**
     * Assert that the two supplied nodes represent the exact same node with the same path, same ID properties, same properties,
     * and same children.
     * 
     * @param node1 the first node; may not be null
     * @param node2 the second node; may not be null
     */
    public static void assertSameNode( Node node1,
                                       Node node2 ) {
        assertThat(node1, is(notNullValue()));
        assertThat(node2, is(notNullValue()));

        // Check the locations ...
        Location location1 = node1.getLocation();
        Location location2 = node2.getLocation();
        assertThat(location1.isSame(location2), is(true));

        // Check the paths ...
        assertThat(location1.getPath(), is(location2.getPath()));

        // Check the ID properties ...
        assertThat(location1.getIdProperties(), is(location2.getIdProperties()));

        // Check the properties ...

        // Check the children ...
        assertThat(node1.getChildren(), is(node2.getChildren()));
        assertThat(node1.getChildrenSegments(), is(node2.getChildrenSegments()));
    }

    /**
     * Assert that the node has all of the supplied properties.
     * 
     * @param node the node; may not be null
     * @param properties the expected properties
     */
    public static void assertSameProperties( Node node,
                                             Map<Name, Property> properties ) {
        assertThat(node, is(notNullValue()));
        assertThat(properties, is(notNullValue()));
        Set<Name> names = new HashSet<Name>(properties.keySet());
        for (Property prop1 : node.getProperties()) {
            Name name = prop1.getName();
            assertThat(names.remove(name), is(true));
            assertThat(prop1, is(properties.get(name)));
        }
        assertThat(names.isEmpty(), is(true));
    }

    /**
     * Assert that the node has all of the supplied properties.
     * 
     * @param node the node; may not be null
     * @param properties the expected properties
     */
    public static void assertSameProperties( Node node,
                                             Iterable<Property> properties ) {
        assertThat(node, is(notNullValue()));
        assertThat(properties, is(notNullValue()));
        Set<Name> names = new HashSet<Name>(node.getPropertiesByName().keySet());
        for (Property prop1 : properties) {
            Name name = prop1.getName();
            assertThat(names.remove(name), is(true));
            assertThat(prop1, is(node.getProperty(name)));
        }
        assertThat(names.isEmpty(), is(true));
    }

    /**
     * Find the name for a node that does not exist under the supplied parent.
     * 
     * @param pathToExistingParent the parent to the node that must exist, under which the non-existent node is to exist
     * @return the name for a non-existent node; never null
     */
    public Path findPathToNonExistentNodeUnder( String pathToExistingParent ) {
        return findPathToNonExistentNodeUnder(path(pathToExistingParent));
    }

    /**
     * Find the name for a node that does not exist under the supplied parent.
     * 
     * @param pathToExistingParent the parent to the node that must exist, under which the non-existent node is to exist
     * @return the name for a non-existent node; never null
     */
    public Path findPathToNonExistentNodeUnder( Path pathToExistingParent ) {
        String nonExistentChildName = "ab39dbyfg739_adf7bg";
        boolean verifiedNoChildWithName = false;
        while (!verifiedNoChildWithName) {
            verifiedNoChildWithName = true;
            // Verify that no child of the root matches the name ...
            for (Location childLocation : graph.getChildren().of(pathToExistingParent)) {
                if (childLocation.getPath().getLastSegment().getName().getLocalName().equals(nonExistentChildName)) {
                    nonExistentChildName = nonExistentChildName + '2' + nonExistentChildName;
                    verifiedNoChildWithName = false;
                    break;
                }
            }
        }
        return path(pathToExistingParent, nonExistentChildName);
    }

    protected void collectGarbage( int maxNumberOfPasses ) {
        RepositoryConnection connection = connectionFactory.createConnection(source.getName());
        try {
            for (int i = 0; i != maxNumberOfPasses; ++i) {
                // And request garbage collection ...
                CollectGarbageRequest request = new CollectGarbageRequest();
                connection.execute(context, request);
                if (!request.isAdditionalPassRequired()) break;
            }
        } finally {
            // Always close this connection after each pass ...
            connection.close();
        }
    }
}

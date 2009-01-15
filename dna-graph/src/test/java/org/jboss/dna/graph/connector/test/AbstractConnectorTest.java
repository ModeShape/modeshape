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
package org.jboss.dna.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.ExecutionContextFactory;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.request.Request;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

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
 * 
 * @author Randall Hauch
 */
public abstract class AbstractConnectorTest {

    protected static ExecutionContextFactory contextFactory;
    protected static ExecutionContext context;
    protected static RepositorySource source;
    protected static Graph graph;
    private static RepositoryConnectionFactory connectionFactory;
    private static List<RepositoryConnection> openConnections;
    private static boolean running;
    private static Location rootLocation;

    public void startRepository() throws Exception {
        if (!running) {
            // Set up the connection factory to other sources ...

            // Set up the execution context ...
            contextFactory = new ExecutionContext();
            context = setUpExecutionContext(contextFactory);

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
                public ExecutionContextFactory getExecutionContextFactory() {
                    return contextFactory;
                }

                @SuppressWarnings( "synthetic-access" )
                public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                    return connectionFactory;
                }
            });

            // And set up the graph instance ...
            graph = Graph.create(source.getName(), connectionFactory, context);

            // Now have the source initialize the content (if any) ...
            initializeContent(graph);
            running = true;
        }
    }

    public static void shutdownRepository() {
        if (running) {
            try {
                // Shut down the connections to the source ...
                for (RepositoryConnection connection : openConnections) {
                    connection.close();
                }
            } finally {
                running = false;
                rootLocation = null;
            }
        }
    }

    /**
     * Method that is executed before each test. By default, this method {@link #startRepository() sets up the repository}.
     * 
     * @throws Exception
     */
    @Before
    public void beforeEach() throws Exception {
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
    }

    /**
     * Method executed after all tests have completed. By default, this method ensures that the repository has been shut down (if
     * this was not done in {@link #afterEach()}).
     * 
     * @throws Exception
     */
    @AfterClass
    public static void afterAll() throws Exception {
        shutdownRepository();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Methods used in the initialization and set up of test methods
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Set up the {@link ExecutionContext} for each unit test.
     * 
     * @param contextFactory the context factory that may be used; never null
     * @return the execution context; may not be null
     */
    protected ExecutionContext setUpExecutionContext( ExecutionContextFactory contextFactory ) {
        return contextFactory.create();
    }

    /**
     * Set up a {@link RepositorySource} that should be used for each of the unit tests.
     * 
     * @return the repository source
     * @throws Exception if there is a problem setting up the source
     */
    protected abstract RepositorySource setUpSource() throws Exception;

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
        return new Location(path(path));
    }

    /**
     * Utility to create a {@link Location} from a UUID.
     * 
     * @param uuid the UUID
     * @return the location
     */
    protected Location location( UUID uuid ) {
        return new Location(uuid);
    }

    protected UUID getRootNodeUuid() {
        if (rootLocation == null) {
            Node root = graph.getNodeAt("/");
            rootLocation = root.getLocation();
        }
        return rootLocation.getUuid();
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
     */
    protected <T extends Request> T execute( T request ) {
        // Get a connection ...
        RepositoryConnection connection = connectionFactory.createConnection(source.getName());
        try {
            connection.execute(context, request);
            return request;
        } finally {
            connection.close();
        }
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
                long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotalDuration().longValue());
                long avgDuration = totalDurationInMicroseconds / totalNumber / 1000L;
                String units = " millisecond(s)";
                if (avgDuration == 0L) {
                    avgDuration = totalDurationInMicroseconds / totalNumber;
                    units = " microsecond(s)";
                }
                output.println("     Total = " + stopwatch.getTotalDuration() + "; avg = " + avgDuration + units);
            }

            // Perform second batch ...
            batch = graph.batch();
            totalNumberCreated += createChildren(batch, initialPath, "secondBranch", 2, numberOfPropertiesPerNode, 2, null);
            Stopwatch sw = new Stopwatch();
            sw.start();
            batch.execute();
            sw.stop();
            long totalDurationInMicroseconds = TimeUnit.NANOSECONDS.toMicros(sw.getTotalDuration().longValue());
            long avgDuration = totalDurationInMicroseconds / totalNumber / 1000L;
            String units = " millisecond(s)";
            if (avgDuration == 0L) {
                avgDuration = totalDurationInMicroseconds / totalNumber;
                units = " microsecond(s)";
            }
            System.out.println("     Final total = " + sw.getTotalDuration() + "; avg = " + avgDuration + units);
            assertThat(totalNumberCreated, is(totalNumber + calculateTotalNumberOfNodesInTree(2, 2, false)));
        }
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
        if (batch == null) batch = graph.batch();
        for (int i = 0; i != number; ++i) {
            String path = parentPath + "/" + nodePrefix + (i + 1);
            Graph.Create<Graph.Batch> create = batch.create(path);
            String originalValue = "The quick brown fox jumped over the moon. What? ";
            String value = originalValue;
            for (int j = 0; j != numProps; ++j) {
                // value = value + originalValue;
                create = create.with("property" + (j + 1), value);
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
     * Assert that the two supplied nodes represent the exact same node with the same path, same ID properties, same properties,
     * and same children.
     * 
     * @param node1 the first node; may not be null
     * @param node2 the second node; may not be null
     */
    public void assertSameNode( Node node1,
                                Node node2 ) {
        assertThat(node1, is(notNullValue()));
        assertThat(node2, is(notNullValue()));

        // Check the locations ...
        Location location1 = node1.getLocation();
        Location location2 = node2.getLocation();
        assertThat(location1.isSame(location2, true), is(true));

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
    public void assertSameProperties( Node node,
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
    public void assertSameProperties( Node node,
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
}

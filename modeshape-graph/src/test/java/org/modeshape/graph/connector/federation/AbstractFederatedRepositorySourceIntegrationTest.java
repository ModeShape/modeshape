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
package org.modeshape.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Results;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.connector.test.AbstractConnectorTest;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;

/**
 * 
 */
public abstract class AbstractFederatedRepositorySourceIntegrationTest {

    private static final Stopwatch FEDERATED_TIMER = new Stopwatch();
    private static final Stopwatch SOURCE_TIMER = new Stopwatch();

    protected FederatedRepositorySource source;
    private String sourceName;
    private String repositoryName;
    private String configurationSourceName;
    private String configurationWorkspaceName;
    private InMemoryRepositorySource configRepositorySource;
    private RepositoryConnection configRepositoryConnection;
    protected ExecutionContext context;
    private Map<String, InMemoryRepositorySource> sources;
    protected Graph federated;
    private RepositoryContext repositoryContext;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        configurationSourceName = "configuration";
        configurationWorkspaceName = "configSpace";
        repositoryName = "Test Repository";

        // Set up the configuration repository ...
        configRepositorySource = new InMemoryRepositorySource();
        configRepositorySource.setName("Configuration Repository");
        configRepositorySource.setDefaultWorkspaceName(configurationWorkspaceName);

        // Set up the repository context ...
        repositoryContext = new RepositoryContext() {
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return connectionFactory;
            }

            @SuppressWarnings( "synthetic-access" )
            public Subgraph getConfiguration( int depth ) {
                Graph result = Graph.create(configRepositorySource, context);
                result.useWorkspace(configurationWorkspaceName);
                return result.getSubgraphOfDepth(depth).at("/a/b/Test Repository");
            }
        };
        configRepositorySource.initialize(repositoryContext);

        // Populate the configuration repository ...
        Graph config = Graph.create(configRepositorySource, context);
        config.create("/a").and();
        config.create("/a/b").and();
        config.create("/a/b/Test Repository").and();
        config.create("/a/b/Test Repository/mode:workspaces").and();

        // Set up the source ...
        source = new FederatedRepositorySource();
        source.setName(repositoryName);
        sourceName = "federated source";
        source.setName(sourceName);
        source.initialize(repositoryContext);

        // Set up the map of sources ...
        sources = new HashMap<String, InMemoryRepositorySource>();

        // Stub the RepositoryContext and RepositoryConnectionFactory instances ...
        configRepositoryConnection = configRepositorySource.getConnection();
        when(connectionFactory.createConnection(configurationSourceName)).thenReturn(configRepositoryConnection);
        when(connectionFactory.createConnection(sourceName)).thenAnswer(new Answer<RepositoryConnection>() {
            public RepositoryConnection answer( InvocationOnMock invocation ) throws Throwable {
                return source.getConnection();
            }
        });

        // Create the graph to the federated repository ...
        federated = Graph.create(sourceName, connectionFactory, context);
    }

    @AfterClass
    public static void afterAll() {
        // System.out.println("Results for federated reads:  " + FEDERATED_TIMER.getSimpleStatistics());
        // System.out.println("Results for source reads:     " + FEDERATED_TIMER.getSimpleStatistics());
    }

    /**
     * Add to the supplied workspace in the federated repository a projection from the workspace in the supplied source.
     * 
     * @param federatedWorkspace
     * @param projectionName
     * @param sourceName
     * @param workspaceName
     * @param projectionRules
     */
    protected void addProjection( String federatedWorkspace,
                                  String projectionName,
                                  String sourceName,
                                  String workspaceName,
                                  String... projectionRules ) {
        CheckArg.isNotNull(federatedWorkspace, "federatedWorkspace");
        CheckArg.isNotNull(projectionName, "projectionName");
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotEmpty(projectionRules, "projectionRules");
        String configPath = repositoryContext.getConfiguration(1)
                                             .getLocation()
                                             .getPath()
                                             .getString(context.getNamespaceRegistry());
        assertThat(configPath.endsWith("/"), is(false));
        String wsPath = configPath + "/mode:workspaces/" + federatedWorkspace;
        String projectionPath = wsPath + "/mode:projections/" + projectionName;
        Graph config = Graph.create(configRepositorySource, context);
        config.useWorkspace(configurationWorkspaceName);
        config.create(wsPath).ifAbsent().and();
        config.create(wsPath + "/mode:projections").ifAbsent().and();
        config.createAt(projectionPath)
              .with(ModeShapeLexicon.PROJECTION_RULES, (Object[])projectionRules)
              .with(ModeShapeLexicon.SOURCE_NAME, sourceName)
              .with(ModeShapeLexicon.WORKSPACE_NAME, workspaceName)
              .and();
        // Make sure the source and workspace exist ...
        graphFor(sourceName, workspaceName);
    }

    /**
     * Obtain a graph to the named source and workspace. If the source does not exist, it is created. Also, if the supplied
     * workspace does not exist, it is also created.
     * 
     * @param sourceName the name of the source; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @return the resulting graph; never null
     */
    protected Graph graphFor( String sourceName,
                              String workspaceName ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        InMemoryRepositorySource source = sources.get(sourceName);
        if (source == null) {
            // Add a new source with this name ...
            source = new InMemoryRepositorySource();
            source.setName(sourceName);
            sources.put(sourceName, source);
            final InMemoryRepositorySource newSource = source;
            // Stub the repository connection factory to return a new connection for this source ...
            when(connectionFactory.createConnection(sourceName)).thenAnswer(new Answer<RepositoryConnection>() {
                public RepositoryConnection answer( InvocationOnMock invocation ) throws Throwable {
                    return newSource.getConnection();
                }
            });
            source.initialize(repositoryContext);
        }
        // Make sure there's a workspace for it ...
        Graph sourceGraph = Graph.create(sourceName, connectionFactory, context);
        if (sourceGraph.getWorkspaces().contains(workspaceName)) {
            sourceGraph.useWorkspace(workspaceName);
        } else {
            sourceGraph.createWorkspace().named(workspaceName);
        }
        return sourceGraph;
    }

    /**
     * Assert that the node does not exist in the federated repository given by the supplied path nor in the underlying source
     * given by the path, source name, and workspace name.
     * 
     * @param pathInFederated
     * @param pathInSource
     * @param sourceName
     * @param workspaceName
     */
    protected void assertNoNode( String pathInFederated,
                                 String pathInSource,
                                 String sourceName,
                                 String workspaceName ) {
        try {
            FEDERATED_TIMER.start();
            federated.getNodeAt(pathInFederated);
            FEDERATED_TIMER.stop();
            fail("Did not expect to find federated node \"" + pathInFederated + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
        try {
            SOURCE_TIMER.start();
            graphFor(sourceName, workspaceName).getNodeAt(pathInSource);
            SOURCE_TIMER.stop();
            fail("Did not expect to find source node \"" + pathInSource + "\" in workspace \"" + workspaceName
                 + "\" of source \"" + sourceName + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    /**
     * Assert that the node does not exist in the federated repository given by the supplied path but does exist in the underlying
     * source given by the path, source name, and workspace name.
     * 
     * @param pathInFederated
     * @param pathInSource
     * @param sourceName
     * @param workspaceName
     */
    protected void assertNotFederated( String pathInFederated,
                                       String pathInSource,
                                       String sourceName,
                                       String workspaceName ) {
        try {
            FEDERATED_TIMER.start();
            federated.getNodeAt(pathInFederated);
            FEDERATED_TIMER.stop();
            fail("Did not expect to find federated node \"" + pathInFederated + "\"");
        } catch (PathNotFoundException e) {
            // expected
        }
        SOURCE_TIMER.start();
        graphFor(sourceName, workspaceName).getNodeAt(pathInSource);
        SOURCE_TIMER.stop();
    }

    /**
     * Assert that the node in the federated repository given by the supplied path represents the same node in the underlying
     * source given by the path, source name, and workspace name.
     * 
     * @param pathInFederated
     * @param pathInSource
     * @param sourceName
     * @param workspaceName
     * @param extraChildren
     */
    protected void assertSameNode( String pathInFederated,
                                   String pathInSource,
                                   String sourceName,
                                   String workspaceName,
                                   String... extraChildren ) {
        FEDERATED_TIMER.start();
        Node fedNode = federated.getNodeAt(pathInFederated);
        FEDERATED_TIMER.stop();
        SOURCE_TIMER.start();
        Node sourceNode = graphFor(sourceName, workspaceName).getNodeAt(pathInSource);
        SOURCE_TIMER.stop();

        Path fedPath = fedNode.getLocation().getPath();
        Path sourcePath = sourceNode.getLocation().getPath();
        if (fedPath.isRoot() || sourcePath.isRoot()) {
            UUID fedUuid = fedNode.getLocation().getUuid();
            UUID sourceUuid = sourceNode.getLocation().getUuid();
            assertThat(fedUuid, is(sourceUuid));
        } else {
            Name fedName = fedPath.getLastSegment().getName();
            Name sourceNodeName = sourcePath.getLastSegment().getName();

            // If the node names match, then the nodes should be projected and the UUIDs should match.
            // Otherwise, the federated node is likely a placeholder node, and thus will have a different UUID.
            if (fedName.equals(sourceNodeName)) {
                // The UUID should match ...
                UUID fedUuid = fedNode.getLocation().getUuid();
                UUID sourceUuid = sourceNode.getLocation().getUuid();
                assertThat(fedUuid, is(sourceUuid));
            }
        }

        // The children should match ...
        List<Path.Segment> fedChildren = new ArrayList<Path.Segment>();
        List<Path.Segment> sourceChildren = new ArrayList<Path.Segment>();
        for (Location child : fedNode.getChildren()) {
            fedChildren.add(child.getPath().getLastSegment());
        }
        for (Location child : sourceNode.getChildren()) {
            sourceChildren.add(child.getPath().getLastSegment());
        }
        // Add any extra children to the 'sourceChildren' ...
        for (String extraChild : extraChildren) {
            sourceChildren.add(context.getValueFactories().getPathFactory().createSegment(extraChild));
        }
        assertThat(fedChildren, is(sourceChildren));
        // The properties should match ...
        Map<Name, Property> fedProps = fedNode.getPropertiesByName();
        Map<Name, Property> sourceProps = sourceNode.getPropertiesByName();
        assertThat(fedProps, is(sourceProps));

        // Now, try to get the children only ...
        FEDERATED_TIMER.start();
        List<Location> children = federated.getChildren().of(pathInFederated);
        FEDERATED_TIMER.stop();
        fedChildren.clear();
        for (Location child : children) {
            fedChildren.add(child.getPath().getLastSegment());
        }
        assertThat(fedChildren, is(sourceChildren));

        // And try to get the properties only ...
        FEDERATED_TIMER.start();
        fedProps = federated.getPropertiesByName().on(pathInFederated);
        FEDERATED_TIMER.stop();
        assertThat(fedProps, is(sourceProps));

        // And try to get the properties one by one ...
        for (Property sourceProp : sourceProps.values()) {
            FEDERATED_TIMER.start();
            Property fedProp = federated.getProperty(sourceProp.getName()).on(pathInFederated);
            FEDERATED_TIMER.stop();
            assertThat(fedProp, is(sourceProp));
        }

        // Try reading a subgraph of depth 2 ...
        FEDERATED_TIMER.start();
        Subgraph fedSubgraph = federated.getSubgraphOfDepth(2).at(pathInFederated);
        FEDERATED_TIMER.stop();
        SOURCE_TIMER.start();
        Subgraph sourceSubgraph = graphFor(sourceName, workspaceName).getSubgraphOfDepth(2).at(pathInSource);
        SOURCE_TIMER.stop();
        if (extraChildren.length == 0) {
            // Can only compare the graphs when there are no extra children ...
            AbstractConnectorTest.assertEquivalentSubgraphs(fedSubgraph, sourceSubgraph, true, false);
        }
    }

    protected void assertReadUsingBatch( String... pathsInFederated ) {
        Graph.Batch batch = federated.batch();
        for (String pathInFederated : pathsInFederated) {
            batch.read(pathInFederated).and();
        }
        Results results = batch.execute();
        for (String pathInFederated : pathsInFederated) {
            assertThat(results.getNode(pathInFederated), is(notNullValue()));
        }
    }
}

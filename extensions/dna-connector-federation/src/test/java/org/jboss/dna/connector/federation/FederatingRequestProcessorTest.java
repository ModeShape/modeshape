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
package org.jboss.dna.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.jboss.dna.common.collection.IsIteratorContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.BasicCachePolicy;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.UnsupportedRequestException;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatingRequestProcessorTest {

    private FederatingRequestProcessor executor;
    private ExecutionContext context;
    private PathFactory pathFactory;
    private String sourceName;
    private Projection cacheProjection;
    private CachePolicy cachePolicy;
    private List<Projection> sourceProjections;
    private Projection.Rule[] cacheProjectionRules = new Projection.Rule[] {};
    private Map<String, FederatedWorkspace> workspaces;
    private FederatedWorkspace defaultWorkspace;
    private InMemoryRepositorySource cacheSource;
    private InMemoryRepositorySource source1;
    private InMemoryRepositorySource source2;
    private InMemoryRepositorySource source3;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        sourceName = "Federated Source";
        cachePolicy = new BasicCachePolicy(219L, TimeUnit.SECONDS);
        cacheSource = new InMemoryRepositorySource();
        cacheSource.setName("Cache");
        cacheSource.setDefaultWorkspaceName("cacheSpace");
        ProjectionParser ruleParser = ProjectionParser.getInstance();
        cacheProjectionRules = ruleParser.rulesFromStrings(context, "/ => /cache/repo/A");
        cacheProjection = new Projection(cacheSource.getName(), "cacheSpace", cacheProjectionRules);
        source1 = new InMemoryRepositorySource();
        source2 = new InMemoryRepositorySource();
        source3 = new InMemoryRepositorySource();
        source1.setName("Source 1");
        source2.setName("Source 2");
        source3.setName("Source 3");
        // Set up the cache policies ...
        source1.setDefaultCachePolicy(new BasicCachePolicy(100, TimeUnit.SECONDS));
        source2.setDefaultCachePolicy(new BasicCachePolicy(200, TimeUnit.SECONDS));
        source3.setDefaultCachePolicy(new BasicCachePolicy(300, TimeUnit.SECONDS));
        sourceProjections = new ArrayList<Projection>();
        // Source 1 projects from '/source/one/a' into repository '/a'
        // and from '/source/one/b' into repository '/b'
        sourceProjections.add(new Projection(source1.getName(), "workspace1", ruleParser.rulesFromStrings(context,
                                                                                                          "/a => /source/one/a",
                                                                                                          "/b => /source/one/b")));
        // Source 2 projects from '/source/two/a' into repository '/a'
        sourceProjections.add(new Projection(source2.getName(), "workspace2", ruleParser.rulesFromStrings(context,
                                                                                                          "/a => /source/two/a")));
        // Source 3 projects everything into repository at root
        sourceProjections.add(new Projection(source3.getName(), "workspace3", ruleParser.rulesFromStrings(context, "/ => /")));
        workspaces = new HashMap<String, FederatedWorkspace>();
        defaultWorkspace = new FederatedWorkspace("fedSpace", cacheProjection, sourceProjections, cachePolicy);
        workspaces.put(defaultWorkspace.getName(), defaultWorkspace);
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
        // stub(connectionFactory.createConnection(source1.getName())).toReturn(source1.getConnection());
        doReturn(source1.getConnection()).when(connectionFactory).createConnection(source1.getName());
        doReturn(source2.getConnection()).when(connectionFactory).createConnection(source2.getName());
        doReturn(source3.getConnection()).when(connectionFactory).createConnection(source3.getName());
        doReturn(cacheSource.getConnection()).when(connectionFactory).createConnection(cacheSource.getName());
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return pathFactory.create(path);
    }

    protected Path.Segment segment( String path ) {
        return pathFactory.createSegment(path);
    }

    protected Property property( String name,
                                 Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    protected Map<Name, Property> addProperty( Map<Name, Property> properties,
                                               String name,
                                               Object... values ) {
        Property property = property(name, values);
        properties.put(property.getName(), property);
        return properties;
    }

    protected void assertNodeHasChildren( String path,
                                          String... expectedChildSegments ) {
        Location parent = Location.create(path(path));
        ReadAllChildrenRequest request = new ReadAllChildrenRequest(parent, "fedSpace");
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path(path)));
        List<Location> actualChildLocations = request.getChildren();
        Path.Segment[] actualChildren = new Path.Segment[actualChildLocations.size()];
        for (int i = 0; i != actualChildren.length; ++i) {
            Path actualChildPath = actualChildLocations.get(i).getPath();
            actualChildren[i] = actualChildPath.getLastSegment();
            assertThat(actualChildPath.getParent(), is(path(path)));
        }
        Path.Segment[] expectedChildren = new Path.Segment[expectedChildSegments.length];
        for (int i = 0; i != expectedChildren.length; ++i) {
            expectedChildren[i] = segment(expectedChildSegments[i]);
        }
        assertThat(actualChildren, is(expectedChildren));
    }

    protected void assertNodeHasProperty( String path,
                                          String propertyName,
                                          Object... expectedValues ) {
        Location parent = Location.create(path(path));
        ReadPropertyRequest request = new ReadPropertyRequest(parent, "fedSpace", name(propertyName));
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path(path)));
        Object[] actualValues = request.getProperty().getValuesAsArray();
        String[] actualValuesAsStrings = new String[actualValues.length];
        for (int i = 0; i != actualValues.length; ++i) {
            actualValuesAsStrings[i] = context.getValueFactories().getStringFactory().create(actualValues[i]);
        }
        String[] expectedValuesAsStrings = new String[expectedValues.length];
        for (int i = 0; i != expectedValues.length; ++i) {
            expectedValuesAsStrings[i] = context.getValueFactories().getStringFactory().create(expectedValues[i]);
        }
        assertThat(actualValuesAsStrings, is(expectedValuesAsStrings));
    }

    protected void assertNodeHasNoProperty( String path,
                                            String propertyName ) {
        Location parent = Location.create(path(path));
        ReadAllPropertiesRequest request = new ReadAllPropertiesRequest(parent, "fedSpace");
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path(path)));
        assertThat(request.getPropertiesByName().containsKey(name(propertyName)), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenExecutionContextIsNull() {
        context = null;
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsNull() {
        sourceName = null;
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsEmpty() {
        sourceName = "";
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsBlank() {
        sourceName = "   ";
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenConnectionFactoryIsNull() {
        connectionFactory = null;
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenWorkspacesMapIsNull() {
        workspaces = null;
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenWorkspacesMapIsEmpty() {
        workspaces.clear();
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test
    public void shouldNotFailWhenDefaultWorkspaceIsNull() {
        defaultWorkspace = null;
        executor = new FederatingRequestProcessor(context, sourceName, workspaces, defaultWorkspace, connectionFactory);
    }

    @Test
    public void shouldHaveCurrentTimeInUtc() {
        DateTime currentTimeInUtc = executor.getCurrentTimeInUtc();
        assertThat(currentTimeInUtc, is(notNullValue()));
        assertThat(currentTimeInUtc.toUtcTimeZone(), is(currentTimeInUtc));
    }

    @Test
    public void shouldReturnSameExecutionContextSuppliedToConstructor() {
        assertThat(executor.getExecutionContext(), is(sameInstance(context)));
    }

    @Test
    public void shouldObtainCacheConnectionFromConnectionFactoryThenHoldOntoReference() throws Exception {
        // Stub the connection factory to return a connection instance that we specify ...
        RepositoryConnection connection = mock(RepositoryConnection.class);
        stub(connectionFactory.createConnection(cacheSource.getName())).toReturn(connection);
        // Obtain the connection to the cache ...
        assertThat(executor.getConnectionToCacheFor(defaultWorkspace), is(sameInstance(connection)));
        verify(connectionFactory, times(1)).createConnection(cacheSource.getName());
        // Call it again, and should not ask the factory ...
        assertThat(executor.getConnectionToCacheFor(defaultWorkspace), is(sameInstance(connection)));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldObtainRepositoryConnectionFromConnectionFactoryThenHoldOntoReference() throws Exception {
        String sourceName = "Some source";
        Projection projection = mock(Projection.class);
        stub(projection.getSourceName()).toReturn(sourceName);
        RepositoryConnection connection = mock(RepositoryConnection.class);
        stub(connectionFactory.createConnection(sourceName)).toReturn(connection);

        assertThat(executor.getConnection(projection), is(sameInstance(connection)));
        verify(connectionFactory, times(1)).createConnection(sourceName);
        // Call it again, and should not ask the factory ...
        assertThat(executor.getConnection(projection), is(sameInstance(connection)));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldCloseHavingNotOpenedConnections() throws Exception {
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        executor.close();
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldCloseAllOpenConnectionsWhenClosingExecutor() throws Exception {
        // Load the connections
        assertThat(executor.getConnectionToCacheFor(defaultWorkspace), is(notNullValue()));
        for (Projection projection : sourceProjections) {
            assertThat(executor.getConnection(projection), is(notNullValue()));
        }
        verify(connectionFactory).createConnection(cacheSource.getName());
        verify(connectionFactory).createConnection(source1.getName());
        verify(connectionFactory).createConnection(source2.getName());
        verify(connectionFactory).createConnection(source3.getName());
        assertThat(executor.getOpenConnections().isEmpty(), is(false));
        // Close the executor and verify all connections have been closed
        executor.close();
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldLoadContributionsForRootNodeFromSources() throws Exception {
        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        Graph.Batch batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.execute();
        Location nodeX = repository3.getNodeAt("/x").getLocation();
        Location nodeB = repository3.getNodeAt("/b").getLocation();

        Path path = pathFactory.createRootPath();
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        Location childA = Location.create(pathFactory.create(path, "a"));
        Location childB = Location.create(pathFactory.create(path, "b"));
        assertThat(contributions.get(0).getChildren(), hasItems(childA, childB));
        assertThat(contributions.get(1).getChildren(), hasItems(childA));
        assertThat(contributions.get(2).getChildren(), hasItems(nodeX, nodeB));
    }

    protected void hasChildren( Contribution contribution,
                                String... childNames ) {
        Location location = contribution.getLocationInSource();
        Iterator<Location> iter = contribution.getChildren();
        for (String childName : childNames) {
            Path expectedChildPath = context.getValueFactories().getPathFactory().create(location.getPath(), childName);
            Location expectedChild = Location.create(expectedChildPath);
            Location next = iter.next();
            if (!next.isSame(expectedChild)) {
                assertThat(next, is(expectedChild));
            }
        }
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldLoadContributionsForNonRootNodeWithOneContributionFromSources() throws Exception {
        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        Graph.Batch batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.execute();

        Path path = pathFactory.create("/x/y"); // from source 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "zA", "zB", "zC");

        path = pathFactory.create("/x"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "y");
    }

    @Test
    public void shouldLoadNonRootNodeWithTwoContributionFromSources() throws Exception {
        // Set up the content of source 1
        Graph repository1 = Graph.create(source1, context);
        repository1.createWorkspace().named("workspace1");
        Graph.Batch batch = repository1.batch();
        batch.create("/source").and();
        batch.create("/source/one").and();
        batch.create("/source/one/a").with("desc", "source 1 node a description").and();
        batch.create("/source/one/a/nA").with("desc", "source 1 node nA description").and();
        batch.create("/source/one/a/nB").with("desc", "source 1 node nB description").and();
        batch.create("/source/one/a/nC").with("desc", "source 1 node nC description").and();
        batch.create("/source/one/b").with("desc", "source 1 node b description").and();
        batch.create("/source/one/b/pA").with("desc", "source 1 node pA description").and();
        batch.create("/source/one/b/pB").with("desc", "source 1 node pB description").and();
        batch.create("/source/one/b/pC").with("desc", "source 1 node pC description").and();
        batch.execute();

        // Set up the content of source 2
        Graph repository2 = Graph.create(source2, context);
        repository2.createWorkspace().named("workspace2");
        batch = repository2.batch();
        batch.create("/source").and();
        batch.create("/source/two").and();
        batch.create("/source/two/a").with("desc", "source 2 node a description").and();
        batch.create("/source/two/a/qA").with("desc", "source 2 node qA description").and();
        batch.create("/source/two/a/qB").with("desc", "source 2 node qB description").and();
        batch.create("/source/two/a/qC").with("desc", "source 2 node qC description").and();
        batch.execute();

        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.execute();

        Path path = pathFactory.create("/b"); // from source 2 and source 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0), "pA", "pB", "pC");
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "by");

        path = pathFactory.create("/b/by"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(2)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1), "bzA", "bzB");
    }

    @Test
    public void shouldLoadNonRootNodeWithThreeContributionFromSources() throws Exception {
        // Set up the content of source 1
        Graph repository1 = Graph.create(source1, context);
        repository1.createWorkspace().named("workspace1");
        Graph.Batch batch = repository1.batch();
        batch.create("/source").and();
        batch.create("/source/one").and();
        batch.create("/source/one/a").with("desc", "source 1 node a description").and();
        batch.create("/source/one/a/nA").with("desc", "source 1 node nA description").and();
        batch.create("/source/one/a/nB").with("desc", "source 1 node nB description").and();
        batch.create("/source/one/a/nC").with("desc", "source 1 node nC description").and();
        batch.create("/source/one/b").with("desc", "source 1 node b description").and();
        batch.create("/source/one/b/pA").with("desc", "source 1 node pA description").and();
        batch.create("/source/one/b/pB").with("desc", "source 1 node pB description").and();
        batch.create("/source/one/b/pC").with("desc", "source 1 node pC description").and();
        batch.execute();

        // Set up the content of source 2
        Graph repository2 = Graph.create(source2, context);
        repository2.createWorkspace().named("workspace2");
        batch = repository2.batch();
        batch.create("/source").and();
        batch.create("/source/two").and();
        batch.create("/source/two/a").with("desc", "source 2 node a description").and();
        batch.create("/source/two/a/qA").with("desc", "source 2 node qA description").and();
        batch.create("/source/two/a/qB").with("desc", "source 2 node qB description").and();
        batch.create("/source/two/a/qC").with("desc", "source 2 node qC description").and();
        batch.execute();

        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.create("/a").and();
        batch.create("/a/ay").with("desc", "ay description").and();
        batch.create("/a/ay/azA").with("desc", "azA description").and();
        batch.create("/a/ay/azB").with("desc", "azB description").and();
        batch.execute();

        Path path = pathFactory.create("/a"); // from sources 1, 2 and 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0), "nA", "nB", "nC");
        hasChildren(contributions.get(1), "qA", "qB", "qC");
        hasChildren(contributions.get(2), "ay");

        path = pathFactory.create("/a/ay"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        assertThat(contributions.size(), is(1)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source3.getName()));
        hasChildren(contributions.get(0), "azA", "azB");
    }

    @Test
    public void shouldFailToLoadNodeFromSourcesWhenTheNodeDoesNotAppearInAnyOfTheSources() throws Exception {
        Path nonExistant = pathFactory.create("/nonExistant/Node/In/AnySource");
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(nonExistant), defaultWorkspace, null, contributions);
        // All of the contributions should be empty ...
        for (Contribution contribution : contributions) {
            assertThat(contribution.isEmpty(), is(true));
        }
    }

    @Test
    public void shouldComputeCachePolicyCorrectlyUsingCurrentTimeAndSourceDefaultCachePolicy() throws Exception {
        // Set up the content of source 1
        Graph repository1 = Graph.create(source1, context);
        repository1.createWorkspace().named("workspace1");
        Graph.Batch batch = repository1.batch();
        batch.create("/source").and();
        batch.create("/source/one").and();
        batch.create("/source/one/a").with("desc", "source 1 node a description").and();
        batch.create("/source/one/a/nA").with("desc", "source 1 node nA description").and();
        batch.create("/source/one/a/nB").with("desc", "source 1 node nB description").and();
        batch.create("/source/one/a/nC").with("desc", "source 1 node nC description").and();
        batch.create("/source/one/b").with("desc", "source 1 node b description").and();
        batch.create("/source/one/b/pA").with("desc", "source 1 node pA description").and();
        batch.create("/source/one/b/pB").with("desc", "source 1 node pB description").and();
        batch.create("/source/one/b/pC").with("desc", "source 1 node pC description").and();
        batch.execute();

        // Set up the content of source 2
        Graph repository2 = Graph.create(source2, context);
        repository2.createWorkspace().named("workspace2");
        batch = repository2.batch();
        batch.create("/source").and();
        batch.create("/source/two").and();
        batch.create("/source/two/a").with("desc", "source 2 node a description").and();
        batch.create("/source/two/a/qA").with("desc", "source 2 node qA description").and();
        batch.create("/source/two/a/qB").with("desc", "source 2 node qB description").and();
        batch.create("/source/two/a/qC").with("desc", "source 2 node qC description").and();
        batch.execute();

        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.create("/a").and();
        batch.create("/a/ay").with("desc", "ay description").and();
        batch.create("/a/ay/azA").with("desc", "azA description").and();
        batch.create("/a/ay/azB").with("desc", "azB description").and();
        batch.execute();

        Path path = pathFactory.create("/a"); // from sources 1, 2 and 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(Location.create(path), defaultWorkspace, null, contributions);

        // Check when the contributions expire ...
        DateTime nowInUtc = executor.getCurrentTimeInUtc();
        DateTime nowPlus10InUtc = nowInUtc.plusSeconds(10);
        DateTime nowPlus110InUtc = nowInUtc.plusSeconds(110);
        DateTime nowPlus210InUtc = nowInUtc.plusSeconds(210);
        DateTime nowPlus220InUtc = nowInUtc.plusSeconds(220);
        DateTime nowPlus310InUtc = nowInUtc.plusSeconds(310);
        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));
        // Nothing should be expired in 10 seconds ...
        assertThat(contributions.get(0).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(1).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(2).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(0).isExpired(nowPlus110InUtc), is(true)); // expired by source
        assertThat(contributions.get(1).isExpired(nowPlus110InUtc), is(false));
        assertThat(contributions.get(2).isExpired(nowPlus110InUtc), is(false));
        assertThat(contributions.get(0).isExpired(nowPlus210InUtc), is(true)); // expired by source @ 100
        assertThat(contributions.get(1).isExpired(nowPlus210InUtc), is(true)); // expired by source @ 200
        assertThat(contributions.get(2).isExpired(nowPlus210InUtc), is(false));
        assertThat(contributions.get(0).isExpired(nowPlus220InUtc), is(true)); // expired by source @ 100
        assertThat(contributions.get(1).isExpired(nowPlus220InUtc), is(true)); // expired by source @ 200
        assertThat(contributions.get(2).isExpired(nowPlus220InUtc), is(true)); // expired by cache @ 219
        assertThat(contributions.get(2).isExpired(nowPlus310InUtc), is(true)); // expired by cache @ 219
    }

    /**
     * Set up the sources such that:
     * <ul>
     * <li>Source 1: "{@code /a => /source/one/a}", "{@code /b => /source/one/b}"</li>
     * <li>Source 2: "{@code /a => /source/two/a}"</li>
     * <li>Source 1: "{@code / => /}"</li>
     * </ul>
     */
    protected void initializeSourcesWithContent() {
        // Set up the content of source 1
        Graph repository1 = Graph.create(source1, context);
        repository1.createWorkspace().named("workspace1");
        Graph.Batch batch = repository1.batch();
        batch.create("/source").and();
        batch.create("/source/one").and();
        batch.create("/source/one/a").with("desc", "source 1 node a description").and();
        batch.create("/source/one/a/nA").with("desc", "source 1 node nA description").and();
        batch.create("/source/one/a/nB").with("desc", "source 1 node nB description").and();
        batch.create("/source/one/a/nC").with("desc", "source 1 node nC description").and();
        batch.create("/source/one/b").with("desc", "source 1 node b description").and();
        batch.create("/source/one/b/pA").with("desc", "source 1 node pA description").and();
        batch.create("/source/one/b/pB").with("desc", "source 1 node pB description").and();
        batch.create("/source/one/b/pC").with("desc", "source 1 node pC description").and();
        batch.execute();

        // Set up the content of source 2
        Graph repository2 = Graph.create(source2, context);
        repository2.createWorkspace().named("workspace2");
        batch = repository2.batch();
        batch.create("/source").and();
        batch.create("/source/two").and();
        batch.create("/source/two/a").with("desc", "source 2 node a description").and();
        batch.create("/source/two/a/qA").with("desc", "source 2 node qA description").and();
        batch.create("/source/two/a/qB").with("desc", "source 2 node qB description").and();
        batch.create("/source/two/a/qC").with("desc", "source 2 node qC description").and();
        batch.execute();

        // Set up the content of source 3
        Graph repository3 = Graph.create(source3, context);
        repository3.createWorkspace().named("workspace3");
        batch = repository3.batch();
        batch.create("/x").and();
        batch.create("/x/y").with("desc", "y description").and();
        batch.create("/x/y/zA").with("desc", "zA description").and();
        batch.create("/x/y/zB").with("desc", "zB description").and();
        batch.create("/x/y/zC").with("desc", "zC description").and();
        batch.create("/b").and();
        batch.create("/b/by").with("desc", "by description").and();
        batch.create("/b/by/bzA").with("desc", "bzA description").and();
        batch.create("/b/by/bzB").with("desc", "bzB description").and();
        batch.create("/a").and();
        batch.create("/a/ay").with("desc", "ay description").and();
        batch.create("/a/ay/azA").with("desc", "azA description").and();
        batch.create("/a/ay/azB").with("desc", "azB description").and();
        batch.execute();

    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNotCreateNodeIfParentIsOutsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location parent = Location.create(path("/a")); // source 2 or 3?
        CreateNodeRequest request = new CreateNodeRequest(parent, "fedSpace", name("child"));
        executor.process(request);
        if (request.hasError()) throw request.getError();
    }

    @Test
    public void shouldCreateNodeOnlyIfParentIsInsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location parent = Location.create(path("/a/ay")); // source 3
        CreateNodeRequest request = new CreateNodeRequest(parent, "fedSpace", name("child"));
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path("/a/ay/child")));
        assertNodeHasChildren("/a/ay", "azA", "azB", "child");
    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNotDestroyNodeIfOutsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location location = Location.create(path("/a")); // source 2 or 3?
        DeleteBranchRequest request = new DeleteBranchRequest(location, "fedSpace");
        executor.process(request);
        if (request.hasError()) throw request.getError();
    }

    @Test
    public void shouldDestroyNodeOnlyIfInsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        Location location = Location.create(path("/a/ay")); // source 3
        DeleteBranchRequest request = new DeleteBranchRequest(location, "fedSpace");
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path("/a/ay")));
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC");
    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNotUpdateNodeIfOutsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location location = Location.create(path("/a")); // source 2 or 3?
        Map<Name, Property> properties = new HashMap<Name, Property>();
        addProperty(properties, "prop1", "value1");
        addProperty(properties, "prop2", "value2a", "value2b");
        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, "fedSpace", properties);
        executor.process(request);
        if (request.hasError()) throw request.getError();
    }

    @Test
    public void shouldUpdateNodeOnlyIfInsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        // assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasProperty("/a/ay", "desc", "ay description");
        assertNodeHasNoProperty("/a/ay", "prop1");
        assertNodeHasNoProperty("/a/ay", "prop2");
        Location location = Location.create(path("/a/ay")); // source 3
        Map<Name, Property> properties = new HashMap<Name, Property>();
        addProperty(properties, "desc", "ay description 2");
        addProperty(properties, "prop1", "value1");
        addProperty(properties, "prop2", "value2a", "value2b");
        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, "fedSpace", properties);
        executor.process(request);
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationOfNode().getPath(), is(path("/a/ay")));
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasProperty("/a/ay", "desc", "ay description 2");
        assertNodeHasProperty("/a/ay", "prop1", "value1");
        assertNodeHasProperty("/a/ay", "prop2", "value2a", "value2b");
    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNotMoveNodeIfParentIsOutsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location from = Location.create(path("/a/ay")); // source 3
        Location into = Location.create(path("/b")); // source 1 or source 3?
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasChildren("/b", "pA", "pB", "pC", "by");
        MoveBranchRequest request = new MoveBranchRequest(from, into, "fedSpace");
        executor.process(request);
        if (request.hasError()) throw request.getError();
    }

    @Test
    public void shouldMoveNodeOnlyIfParentIsInsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location from = Location.create(path("/a/ay")); // source 3
        Location into = Location.create(path("/b/by")); // source 3
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasChildren("/b", "pA", "pB", "pC", "by");
        assertNodeHasChildren("/b/by", "bzA", "bzB");
        MoveBranchRequest request = new MoveBranchRequest(from, into, "fedSpace");
        executor.process(request);
        if (request.hasError()) {
            System.out.println(request.getError().getMessage());
        }
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationBefore().getPath(), is(path("/a/ay")));
        assertThat(request.getActualLocationAfter().getPath(), is(path("/b/by/ay")));
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC");
        assertNodeHasChildren("/b/by", "bzA", "bzB", "ay");
    }

    @Test( expected = UnsupportedRequestException.class )
    public void shouldNotCopyNodeIfParentIsOutsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location from = Location.create(path("/a/ay")); // source 3
        Location into = Location.create(path("/b")); // source 1 or source 3?
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasChildren("/b", "pA", "pB", "pC", "by");
        CopyBranchRequest request = new CopyBranchRequest(from, "fedSpace", into, "fedSpace");
        executor.process(request);
        if (request.hasError()) throw request.getError();
    }

    @Test
    public void shouldCopyNodeOnlyIfParentIsInsideSingleProjection() throws Throwable {
        initializeSourcesWithContent();
        Location from = Location.create(path("/a/ay")); // source 3
        Location into = Location.create(path("/b/by")); // source 3
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasChildren("/a/ay", "azA", "azB");
        assertNodeHasChildren("/b", "pA", "pB", "pC", "by");
        assertNodeHasChildren("/b/by", "bzA", "bzB");
        CopyBranchRequest request = new CopyBranchRequest(from, "fedSpace", into, "fedSpace");
        executor.process(request);
        if (request.hasError()) {
            System.out.println(request.getError().getMessage());
        }
        assertThat(request.hasError(), is(false));
        assertThat(request.getActualLocationBefore().getPath(), is(path("/a/ay")));
        assertThat(request.getActualLocationAfter().getPath(), is(path("/b/by/ay")));
        assertNodeHasChildren("/a", "nA", "nB", "nC", "qA", "qB", "qC", "ay");
        assertNodeHasChildren("/b/by", "bzA", "bzB", "ay");
        assertNodeHasChildren("/b/by/ay", "azA", "azB");
    }

}

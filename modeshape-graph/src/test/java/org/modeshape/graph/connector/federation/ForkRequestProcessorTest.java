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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.MockRepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.ReadNodeRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * 
 */
public class ForkRequestProcessorTest {

    private ForkRequestProcessor processor;
    private ExecutionContext context;
    private DateTime now;
    private String sourceName;
    private String workspaceName;
    private String sourceNameA;
    private String sourceNameB;
    private String sourceNameC;
    private String workspaceNameA;
    private String workspaceNameB;
    private String workspaceNameC;
    private String nonExistantWorkspaceName;
    private LinkedList<FederatedRequest> federatedRequests;
    private ExecutorService executor;
    @Mock
    private FederatedWorkspace workspace;
    @Mock
    private FederatedRepository repository;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    private Projection projectionA;
    private Projection projectionB;
    // private Projection projectionC;
    private MockRepositoryConnection connectionForSourceA;
    private MockRepositoryConnection connectionForSourceB;
    private MockRepositoryConnection connectionForSourceC;
    private Map<Name, Property> properties;
    private List<ProjectedNode> children;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        executor = Executors.newSingleThreadExecutor();
        sourceName = "MySource";
        workspaceName = "MyWorkspace";
        sourceNameA = "SourceA";
        sourceNameB = "SourceB";
        sourceNameC = "SourceC";
        workspaceNameA = "WorkspaceA";
        workspaceNameB = "WorkspaceB";
        workspaceNameC = "WorkspaceC";
        nonExistantWorkspaceName = "Non-Existant Workspace";
        context = new ExecutionContext();
        now = context.getValueFactories().getDateFactory().create();
        federatedRequests = new LinkedList<FederatedRequest>();
        children = new ArrayList<ProjectedNode>();
        properties = new HashMap<Name, Property>();

        // Set up the connection factory and connection ...
        connectionForSourceA = new MockRepositoryConnection(sourceNameA);
        connectionForSourceB = new MockRepositoryConnection(sourceNameB);
        connectionForSourceC = new MockRepositoryConnection(sourceNameC);
        when(connectionFactory.createConnection(sourceNameA)).thenReturn(connectionForSourceA);
        when(connectionFactory.createConnection(sourceNameB)).thenReturn(connectionForSourceB);
        when(connectionFactory.createConnection(sourceNameC)).thenReturn(connectionForSourceC);

        // Stub the FederatedRepository ...
        when(repository.getSourceName()).thenReturn(sourceName);
        when(repository.getWorkspace(workspaceName)).thenReturn(workspace);
        when(repository.getExecutor()).thenReturn(executor);
        when(repository.getConnectionFactory()).thenReturn(connectionFactory);
        when(repository.getWorkspace(nonExistantWorkspaceName)).thenThrow(new InvalidWorkspaceException());

        // Stub the FederatedWorkspace ...
        when(workspace.getName()).thenReturn(workspaceName);
        // workspace.project(context,location) needs to be stubbed ...

        // Set up the projections ...
        projectionA = new Projection(sourceNameA, workspaceNameA, false, rules("/a => /"));
        projectionB = new Projection(sourceNameB, workspaceNameB, false, rules("/b => /"));
        // projectionC = new Projection(sourceNameC, workspaceNameC, rules("/c => /"));

        // Now set up the processor ...
        processor = new ForkRequestProcessor(repository, context, now, federatedRequests);
    }

    protected Rule[] rules( String... rule ) {
        Rule[] rules = new Rule[rule.length];
        for (int i = 0; i != rule.length; ++i) {
            rules[i] = Projection.fromString(rule[i], context);
        }
        return rules;
    }

    public Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    public Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    public Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    public Location location( String path ) {
        return Location.create(path(path));
    }

    public void addProperty( String name,
                             Object... values ) {
        Property property = context.getPropertyFactory().create(name(name), values);
        properties.put(property.getName(), property);
    }

    public void addChild( Location parent,
                          String childName ) {
        Path path = context.getValueFactories().getPathFactory().create(parent.getPath(), segment(childName));
        Map<Name, Property> properties = Collections.emptyMap();
        List<ProjectedNode> grandChildren = Collections.emptyList();
        PlaceholderNode child = new PlaceholderNode(Location.create(path), properties, grandChildren);
        this.children.add(child);
    }

    @Test
    public void shouldReturnImmediatelyFromAwaitIfNoChannelsAreMade() throws Exception {
        processor.await();
    }

    @Test
    public void shouldReturnFromAwaitAfterChannelsAreCompleted() throws Exception {
        ReadNodeRequest requestA = new ReadNodeRequest(location("/a/some"), workspaceNameA);
        ReadNodeRequest requestB = new ReadNodeRequest(location("/b/some"), workspaceNameB);
        ReadNodeRequest requestC = new ReadNodeRequest(location("/c/some"), workspaceNameC);
        processor.submit(requestA, sourceNameA);
        processor.submit(requestB, sourceNameB);
        processor.submit(requestC, sourceNameC);
        processor.close();
        processor.await();
    }

    @Test
    public void shouldReturnReadableLocation() {
        Location location = location("/mode:something/jcr:else");
        String result = processor.readable(location("/mode:something/jcr:else"));
        assertThat(result, is(location.getString(context.getNamespaceRegistry())));
    }

    @Test
    public void shouldFindFederatedWorkspaceByName() {
        ReadNodeRequest request = new ReadNodeRequest(location("/some"), this.workspace.getName());
        FederatedWorkspace workspace = processor.getWorkspace(request, request.inWorkspace());
        assertThat(workspace, is(sameInstance(this.workspace)));
    }

    @Test
    public void shouldRecordErrorOnRequestIfFederatedWorkspaceCouldNotBeFoundByName() {
        ReadNodeRequest request = new ReadNodeRequest(location("/some"), nonExistantWorkspaceName);
        FederatedWorkspace workspace = processor.getWorkspace(request, request.inWorkspace());
        assertThat(workspace, is(nullValue()));
        assertThat(request.hasError(), is(true));
        assertThat(request.getError(), is(instanceOf(InvalidWorkspaceException.class)));
    }

    @Test
    public void shouldSubmitFederatedRequestToQueueIfFederatedRequestHasNoIncompleteRequests() {
        FederatedRequest request = mock(FederatedRequest.class);
        when(request.hasIncompleteRequests()).thenReturn(false);
        processor.submit(request);
        assertThat(federatedRequests.size(), is(1));
        assertThat(federatedRequests.get(0), is(sameInstance(request)));
    }

    @Test
    public void shouldSubmitToSourcesTheSingleRequestInFederatedRequestAndAddFederatedRequestToQueue() throws Exception {
        ReadNodeRequest original = new ReadNodeRequest(location("/a/some"), this.workspace.getName());
        FederatedRequest request = new FederatedRequest(original);
        // Create the projection and the source request ...
        ReadNodeRequest sourceRequest = new ReadNodeRequest(location("/some"), workspaceNameA);
        request.add(sourceRequest, false, false, projectionA);
        assertThat(request.getFirstProjectedRequest().getProjection(), is(sameInstance(projectionA)));
        assertThat(request.getFirstProjectedRequest().hasNext(), is(false));

        // Submit the federated request ...
        processor.submit(request);
        assertThat(federatedRequests.size(), is(1));
        assertThat(federatedRequests.get(0), is(sameInstance(request)));
        // Wait for the processor to complete all source channels ...
        processor.close();
        processor.await();
        // The source should have received something like the original
        assertThat(connectionForSourceA.getProcessedRequests().contains(sourceRequest), is(true));
    }

    @Test
    public void shouldSubmitToSourcesTheMultipleRequestsInFederatedRequestAndAddFederatedRequestToQueue() throws Exception {
        ReadNodeRequest original = new ReadNodeRequest(location("/some"), this.workspace.getName());
        FederatedRequest request = new FederatedRequest(original);

        // Create the first projection and the source request ...
        ReadNodeRequest sourceRequestA = new ReadNodeRequest(location("/a/some/other"), workspaceNameA);
        request.add(sourceRequestA, false, false, projectionA);
        assertThat(request.getFirstProjectedRequest().getProjection(), is(sameInstance(projectionA)));

        // Create the second projection and the source request ...
        ReadNodeRequest sourceRequestB = new ReadNodeRequest(location("/b/some/other"), workspaceNameB);
        request.add(sourceRequestB, false, false, projectionB);
        assertThat(request.getFirstProjectedRequest().next().getProjection(), is(sameInstance(projectionB)));

        assertThat(request.getFirstProjectedRequest().next().hasNext(), is(false));

        // Submit the federated request ...
        processor.submit(request);
        assertThat(federatedRequests.size(), is(1));
        assertThat(federatedRequests.get(0), is(sameInstance(request)));
        // Wait for the processor to complete all source channels ...
        processor.close();
        processor.await();
        // The source should have received something like the original
        assertThat(connectionForSourceA.getProcessedRequests().contains(sourceRequestA), is(true));
        assertThat(connectionForSourceB.getProcessedRequests().contains(sourceRequestB), is(true));
        assertThat(connectionForSourceC.getProcessedRequests().isEmpty(), is(true));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // process(ReadNodeRequest)
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldNotForkReadNodeRequestIfWorkspaceNameIsInvalid() {
        // Stub the workspace to have no projection ...
        Location locationInFed = location("/a/b");
        when(workspace.project(context, locationInFed, false)).thenReturn(null);

        ReadNodeRequest request = new ReadNodeRequest(locationInFed, nonExistantWorkspaceName);
        processor.process(request);
        assertThat(request.hasError(), is(true));
        assertThat(request.getError(), is(instanceOf(InvalidWorkspaceException.class)));
    }

    @Test
    public void shouldNotForkReadNodeRequestIfThereIsNoProjection() {
        // Stub the workspace to have no projection ...
        Location locationInFed = location("/a/b");
        when(workspace.project(context, locationInFed, false)).thenReturn(null);

        ReadNodeRequest request = new ReadNodeRequest(locationInFed, workspaceName);
        processor.process(request);
        assertThat(request.hasError(), is(true));
        assertThat(request.getError(), is(instanceOf(PathNotFoundException.class)));
    }

    @Test
    public void shouldSubmitSingleSourceRequestWhenProcessingSingleReadNodeRequest() throws Exception {
        // Stub the workspace to have a projection ...
        Location locationInFed = location("/a/x/y");
        Location locationInSource = location("/x/y");
        ProjectedNode projectedNode = new ProxyNode(projectionA, locationInSource, locationInFed, false);
        when(workspace.project(context, locationInFed, false)).thenReturn(projectedNode);

        ReadNodeRequest request = new ReadNodeRequest(locationInFed, workspaceName);
        processor.process(request);
        assertThat(request.hasError(), is(false));

        // Check that the federated request has the right information ...
        FederatedRequest fedRequest = federatedRequests.poll();
        ReadNodeRequest projectedRequest = (ReadNodeRequest)fedRequest.getFirstProjectedRequest().getRequest();
        assertThat(projectedRequest.at(), is(locationInSource));
        assertThat(fedRequest.getFirstProjectedRequest().hasNext(), is(false));

        // Close the processor ...
        processor.close();
        processor.await();

        // Verify the source saw the expected read ...
        ReadNodeRequest sourceRequest = (ReadNodeRequest)connectionForSourceA.getProcessedRequests().poll();
        assertThat(sourceRequest.at().getPath(), is(locationInSource.getPath()));
        assertThat(connectionForSourceB.getProcessedRequests().isEmpty(), is(true));
        assertThat(connectionForSourceC.getProcessedRequests().isEmpty(), is(true));
    }

    @Test
    public void shouldNotSubmitPlaceholderNodesWhenProcessingReadNodeRequest() throws Exception {
        // Stub the workspace to have a projection ...
        Location locationInFed = location("/a/x/y");
        Location locationInSource = location("/a/x/y");
        addProperty("propA", "valueA");
        addProperty("propB", "valueB");
        addChild(locationInSource, "child1");
        addChild(locationInSource, "child2");
        PlaceholderNode projectedNode = new PlaceholderNode(locationInSource, properties, children);
        when(workspace.project(context, locationInFed, false)).thenReturn(projectedNode);

        ReadNodeRequest request = new ReadNodeRequest(locationInFed, workspaceName);
        processor.process(request);
        assertThat(request.hasError(), is(false));

        // Check that the federated request has the right information ...
        FederatedRequest fedRequest = federatedRequests.poll();
        ReadNodeRequest projectedRequest = (ReadNodeRequest)fedRequest.getFirstProjectedRequest().getRequest();
        assertThat(projectedRequest.at(), is(locationInFed));
        List<Location> expectedChildren = new ArrayList<Location>();
        for (ProjectedNode child : children) {
            expectedChildren.add(child.location());
        }
        assertThat(projectedRequest.getChildren(), is(expectedChildren));
        assertThat(projectedRequest.getPropertiesByName(), is(properties));
        assertThat(fedRequest.getFirstProjectedRequest().hasNext(), is(false));

        // Close the processor ...
        processor.close();
        processor.await();

        // Verify that no sources saw a request ...
        assertThat(connectionForSourceA.getProcessedRequests().isEmpty(), is(true));
        assertThat(connectionForSourceB.getProcessedRequests().isEmpty(), is(true));
        assertThat(connectionForSourceC.getProcessedRequests().isEmpty(), is(true));
    }
}

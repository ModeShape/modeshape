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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.RequestType;

/**
 * 
 */
public class JoinRequestProcessorTest {

    private JoinRequestProcessor joinProcessor;
    private ExecutionContext context;
    private DateTime now;
    private String sourceName;
    protected List<Request> unknownRequests;
    protected BlockingQueue<FederatedRequest> joinQueue;
    private Projection mirrorProjection;
    private Projection projectionA;
    @Mock
    private FederatedRepository repository;
    @Mock
    private CachePolicy cachePolicy;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        sourceName = "MySource";
        when(repository.getDefaultCachePolicy()).thenReturn(cachePolicy);
        when(repository.getSourceName()).thenReturn(sourceName);

        unknownRequests = new ArrayList<Request>();
        context = new ExecutionContext();
        now = context.getValueFactories().getDateFactory().create();
        joinProcessor = new JoinRequestProcessorWithUnknownHandler(repository, context, now);
        joinQueue = new LinkedBlockingQueue<FederatedRequest>();

        // Set up the projections ...
        mirrorProjection = new Projection("sourceA", "workspaceM", false, rules("/ => /"));
        projectionA = new Projection("sourceA", "workspaceA", false, rules("/a => /"));
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

    public Property property( String name,
                              Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    public Location child( Location parent,
                           String childName ) {
        Path path = context.getValueFactories().getPathFactory().create(parent.getPath(), segment(childName));
        return Location.create(path);
    }

    public static void pause( long millis ) {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            try {
                fail("Interrupted while sleeping");
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    public void shouldProcessFederatedRequestsUsingIteratable() {
        List<FederatedRequest> requests = new ArrayList<FederatedRequest>();
        Request original = mock(Request.class);
        when(original.getType()).thenReturn(RequestType.INVALID);
        FederatedRequest request = mock(FederatedRequest.class);
        when(request.original()).thenReturn(original);
        requests.add(request);
        joinProcessor.process(requests);
        assertThat(joinProcessor.federatedRequest, is(sameInstance(request)));
        assertThat(unknownRequests.size(), is(1));
        assertThat(unknownRequests.get(0), is(sameInstance(original)));
    }

    @Test
    public void shouldProcessFederatedRequestsUsingBlockingQueue() {
        Request original = mock(Request.class);
        when(original.getType()).thenReturn(RequestType.INVALID);
        // Create the original request, the projection, and the federated request ...
        FederatedRequest request = new FederatedRequest(original);
        request.add(original, false, false, projectionA);
        // Freeze the request ...
        request.freeze();
        // And mark it as done by decrementing the latch ...
        request.getLatch().countDown();
        assertThat(request.getLatch().getCount(), is(0L));
        // Create the queue and add the request ...
        BlockingQueue<FederatedRequest> queue = new LinkedBlockingQueue<FederatedRequest>();
        queue.add(request);
        // Add a terminating request ...
        queue.add(new NoMoreFederatedRequests());
        joinProcessor.process(queue);
        assertThat(joinProcessor.federatedRequest, is(sameInstance(request)));
        assertThat(unknownRequests.size(), is(1));
        assertThat(unknownRequests.get(0), is(sameInstance(original)));
    }

    @Test
    public void shouldProcessFederatedRequestsUsingBlockingQueueThatIsTerminatedAfterProcessingBegins() {
        final Request original = mock(Request.class);
        when(original.getType()).thenReturn(RequestType.INVALID);
        final FederatedRequest request = new FederatedRequest(original);
        Thread thread = new Thread() {
            @Override
            public void run() {
                // Create the original request, the projection, and the federated request ...
                Projection projection = mock(Projection.class);
                request.add(original, false, false, projection);
                // Freeze the request ...
                request.freeze();
                // Add the request ...
                joinQueue.add(request);
                // Pause ...
                pause(100L);
                // And mark it as done by decrementing the latch ...
                request.getLatch().countDown();
                assertThat(request.getLatch().getCount(), is(0L));
                // Pause ...
                pause(100L);
                // Add a terminating request ...
                joinQueue.add(new NoMoreFederatedRequests());
            }
        };
        thread.start();
        joinProcessor.process(joinQueue);
        assertThat(joinProcessor.federatedRequest, is(sameInstance(request)));
        assertThat(unknownRequests.size(), is(1));
        assertThat(unknownRequests.get(0), is(sameInstance(original)));
    }

    @Test
    public void shouldJoinSingleMirroredReadNodeRequest() {
        // Create the original read node request ...
        final ReadNodeRequest original = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        // Create a single federated request...
        final FederatedRequest request = new FederatedRequest(original);
        // And "fork" the original request ...
        final ReadNodeRequest projected = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        projected.setActualLocationOfNode(Location.create(projected.at().getPath(), UUID.randomUUID()));
        projected.addProperty(property("propA", "valueA"));
        projected.addProperty(property("propB", "valueB"));
        projected.addChild(child(projected.getActualLocationOfNode(), "child1"));
        projected.addChild(child(projected.getActualLocationOfNode(), "child2"));
        request.add(projected, true, false, mirrorProjection);
        request.freeze();
        request.getLatch().countDown();
        joinQueue.add(request);
        // Add a terminating request and join the request...
        joinQueue.add(new NoMoreFederatedRequests());
        joinProcessor.process(joinQueue);
        // Check the results of the original has the same results of the projected...
        assertThat(original.getChildren(), is(projected.getChildren()));
        assertThat(original.getPropertiesByName(), is(projected.getPropertiesByName()));
        assertThat(original.getActualLocationOfNode(), is(projected.getActualLocationOfNode()));
    }

    @Test
    public void shouldJoinSingleOffsetReadNodeRequest() {
        // Create the original read node request ...
        final ReadNodeRequest original = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        // Create a single federated request using the mirror projection...
        final FederatedRequest request = new FederatedRequest(original);
        // And "fork" the original request by creating a mirror
        final ReadNodeRequest projected = new ReadNodeRequest(location("/b/c"), "workspaceM");
        projected.setActualLocationOfNode(Location.create(projected.at().getPath(), UUID.randomUUID()));
        projected.addProperty(property("propA", "valueA"));
        projected.addProperty(property("propB", "valueB"));
        projected.addChild(child(projected.getActualLocationOfNode(), "child1"));
        projected.addChild(child(projected.getActualLocationOfNode(), "child2"));
        request.add(projected, false, false, projectionA);
        request.freeze();
        request.getLatch().countDown();
        joinQueue.add(request);
        // Add a terminating request and join the request...
        joinQueue.add(new NoMoreFederatedRequests());
        joinProcessor.process(joinQueue);
        // Check the results of the original has the same results of the projected...
        assertThat(original.getPropertiesByName(), is(projected.getPropertiesByName()));
        assertThat(original.getActualLocationOfNode().getPath(), is(path("/a/b/c")));
        assertThat(original.getChildren().get(0).getPath(), is(path("/a/b/c/child1")));
        assertThat(original.getChildren().get(1).getPath(), is(path("/a/b/c/child2")));
    }

    @Test
    public void shouldJoinMultipleReadNodeRequest() {
        // Create the original read node request ...
        final ReadNodeRequest original = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        // Create a single federated request using the mirror projection...
        final FederatedRequest request = new FederatedRequest(original);
        // And "fork" the original request into the first source request ...
        final ReadNodeRequest projected1 = new ReadNodeRequest(location("/b/c"), "workspaceM");
        projected1.setActualLocationOfNode(Location.create(projected1.at().getPath(), UUID.randomUUID()));
        projected1.addProperty(property("propA", "valueA"));
        projected1.addProperty(property("propB", "valueB"));
        projected1.addChild(child(projected1.getActualLocationOfNode(), "child1"));
        projected1.addChild(child(projected1.getActualLocationOfNode(), "child2"));
        request.add(projected1, false, false, projectionA);
        // And a second source request ...
        final ReadNodeRequest projected2 = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        projected2.setActualLocationOfNode(Location.create(projected2.at().getPath(), UUID.randomUUID()));
        projected2.addProperty(property("propC", "valueC"));
        projected2.addProperty(property("propD", "valueD"));
        projected2.addChild(child(projected2.getActualLocationOfNode(), "child2"));
        projected2.addChild(child(projected2.getActualLocationOfNode(), "child3"));
        request.add(projected2, true, false, mirrorProjection);
        request.freeze();
        request.getLatch().countDown();
        request.getLatch().countDown();
        joinQueue.add(request);
        // Add a terminating request and join the request...
        joinQueue.add(new NoMoreFederatedRequests());
        joinProcessor.process(joinQueue);
        // Check the results of the original has the same results of the projected...
        assertThat(original.getProperties().containsAll(projected1.getProperties()), is(true));
        assertThat(original.getProperties().containsAll(projected2.getProperties()), is(true));
        assertThat(original.getActualLocationOfNode().getPath(), is(path("/a/b/c")));
        assertThat(original.getActualLocationOfNode().getUuid(), is(projected1.getActualLocationOfNode().getUuid()));
        assertThat(original.getActualLocationOfNode().getIdProperty(ModeShapeLexicon.UUID).isMultiple(), is(true));
        assertThat(original.getActualLocationOfNode().getIdProperty(ModeShapeLexicon.UUID).getValuesAsArray()[0],
                   is((Object)projected1.getActualLocationOfNode().getUuid()));
        assertThat(original.getActualLocationOfNode().getIdProperty(ModeShapeLexicon.UUID).getValuesAsArray()[1],
                   is((Object)projected2.getActualLocationOfNode().getUuid()));
        assertThat(original.getChildren().get(0).getPath(), is(path("/a/b/c/child1")));
        assertThat(original.getChildren().get(1).getPath(), is(path("/a/b/c/child2")));
        assertThat(original.getChildren().get(2).getPath(), is(path("/a/b/c/child2[2]")));
        assertThat(original.getChildren().get(3).getPath(), is(path("/a/b/c/child3")));
    }

    @Test
    public void shouldCancelFederatedRequestIfOneOfSeveralMultipleReadNodeRequestIsCancelled() {
        // Create the original read node request ...
        final ReadNodeRequest original = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        // Create a single federated request using the mirror projection...
        final FederatedRequest request = new FederatedRequest(original);
        // And "fork" the original request into the first source request ...
        final ReadNodeRequest projected1 = new ReadNodeRequest(location("/b/c"), "workspaceM");
        projected1.setActualLocationOfNode(Location.create(projected1.at().getPath(), UUID.randomUUID()));
        projected1.addProperty(property("propA", "valueA"));
        projected1.addProperty(property("propB", "valueB"));
        projected1.addChild(child(projected1.getActualLocationOfNode(), "child1"));
        projected1.addChild(child(projected1.getActualLocationOfNode(), "child2"));
        request.add(projected1, false, false, projectionA);
        // And a second source request (that was cancelled)...
        final ReadNodeRequest projected2 = new ReadNodeRequest(location("/a/b/c"), "workspaceM");
        projected2.cancel();
        request.add(projected2, true, false, mirrorProjection);
        request.freeze();
        request.getLatch().countDown();
        request.getLatch().countDown();
        joinQueue.add(request);
        // Add a terminating request and join the request...
        joinQueue.add(new NoMoreFederatedRequests());
        joinProcessor.process(joinQueue);
        // Check the results of the original has the same results of the projected...
        assertThat(original.getProperties().isEmpty(), is(true));
        assertThat(original.getChildren().isEmpty(), is(true));
        assertThat(original.isCancelled(), is(true));
    }

    /**
     * A specialization of {@link JoinRequestProcessor} that simply records unknown request types into
     * {@link JoinRequestProcessorTest#unknownRequests}.
     */
    protected class JoinRequestProcessorWithUnknownHandler extends JoinRequestProcessor {
        protected JoinRequestProcessorWithUnknownHandler( FederatedRepository repository,
                                                          ExecutionContext context,
                                                          DateTime now ) {
            super(repository, context, null, now);
        }

        @Override
        protected void processUnknownRequest( Request request ) {
            unknownRequests.add(request);
        }
    }

}

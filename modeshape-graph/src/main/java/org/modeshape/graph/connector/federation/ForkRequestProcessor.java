/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.federation.FederatedRequest.ProjectedRequest;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.CompositeRequestChannel;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DeleteChildrenRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.ReadNextBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RenameNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UnsupportedRequestException;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * This is a {@link RequestProcessor} implementation that is responsible for forking each incoming request into the
 * source-specific requests. This processor uses an {@link ExecutorService} to begin processing the forked requests immediately.
 * As a result, while this processor processes the incoming requests on the federated content, the sources may already be
 * processing previously forked (and source-specific) requests. Thus, it's quite possible that the sources finish processing their
 * requests very shortly after this processor finishes its work.
 * <p>
 * This processor creates a separate channels for each source to which a request needs to be submitted. This channel submits all
 * requests to that source via a single {@link RepositoryConnection#execute(ExecutionContext, Request) execute} call, meaning that
 * all requests will be processed by the source within a single atomic operation. The channels also generally remain open until
 * this processor completes processing of all incoming requests (that is, until all requests have been forked), so that any
 * cancellation of this processor results in cancellation of each channel (and consequently the cancellation of the
 * {@link CompositeRequest} that the channel submitted to its source).
 * </p>
 * 
 * @see FederatedRepositoryConnection#execute(ExecutionContext, Request)
 * @see JoinRequestProcessor
 */
@NotThreadSafe
class ForkRequestProcessor extends RequestProcessor {
    private final FederatedRepository repository;
    private final ExecutorService executor;
    private final RepositoryConnectionFactory connectionFactory;
    private final Map<String, CompositeRequestChannel> channelBySourceName = new HashMap<String, CompositeRequestChannel>();
    private final Queue<FederatedRequest> federatedRequestQueue;

    /**
     * Create a new fork processor
     * 
     * @param repository the federated repository configuration; never null
     * @param context the execution context in which this processor is executing; may not be null
     * @param now the timestamp representing the current time in UTC; may not be null
     * @param federatedRequestQueue the queue into which should be placed the {@link FederatedRequest} objects created by this
     *        processor that still must be post-processed
     */
    public ForkRequestProcessor( FederatedRepository repository,
                                 ExecutionContext context,
                                 DateTime now,
                                 Queue<FederatedRequest> federatedRequestQueue ) {
        super(repository.getSourceName(), context, null, now);
        this.repository = repository;
        this.executor = this.repository.getExecutor();
        this.connectionFactory = this.repository.getConnectionFactory();
        this.federatedRequestQueue = federatedRequestQueue;
        assert this.executor != null;
        assert this.connectionFactory != null;
        assert this.federatedRequestQueue != null;
    }

    /**
     * Asynchronously submit a request to the supplied source. This is typically called when the forked requests are not needed
     * before continuing.
     * 
     * @param request the request to be submitted; may not be null
     * @param sourceName the name of the source; may not be null
     * @see #submitAndAwait(Request, String)
     * @see #submit(Request, String, CountDownLatch)
     */
    protected void submit( Request request,
                           String sourceName ) {
        assert request != null;
        CompositeRequestChannel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new CompositeRequestChannel(sourceName);
            channelBySourceName.put(sourceName, channel);
            channel.start(executor, getExecutionContext(), connectionFactory);
        }
        channel.add(request);
    }

    /**
     * Submit a request to the supplied source, and block until the request has been processed. This method is typically used when
     * a federated request is forked into multiple source-specific requests, but the output of a source-specific request is
     * required before forking other source-specific requests. This pattern is common in requests that update one source and any
     * information not stored by that source needs to be stored in another source.
     * 
     * @param request the request to be submitted; may not be null
     * @param sourceName the name of the source; may not be null
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #submit(Request, String)
     * @see #submit(Request, String, CountDownLatch)
     */
    protected void submitAndAwait( Request request,
                                   String sourceName ) throws InterruptedException {
        assert request != null;
        CompositeRequestChannel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new CompositeRequestChannel(sourceName);
            channelBySourceName.put(sourceName, channel);
            channel.start(executor, getExecutionContext(), connectionFactory);
        }
        channel.addAndAwait(request);
    }

    /**
     * Submit a request to the supplied source, and have the supplied {@link CountDownLatch latch} be
     * {@link CountDownLatch#countDown() decremented} when the request has been completed. Note that the same latch can be used
     * multiple times.
     * <p>
     * This method is typically used when a federated request is forked into multiple source-specific requests, but the output of
     * a source-specific request is required before forking other source-specific requests. This pattern is common in requests
     * that update one source and any information not stored by that source needs to be stored in another source.
     * </p>
     * 
     * @param request the request to be submitted; may not be null
     * @param sourceName the name of the source; may not be null
     * @param latch the count-down latch; may not be null
     * @see #submit(Request, String)
     * @see #submitAndAwait(Request, String)
     */
    protected void submit( Request request,
                           String sourceName,
                           CountDownLatch latch ) {
        assert request != null;
        CompositeRequestChannel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new CompositeRequestChannel(sourceName);
            channelBySourceName.put(sourceName, channel);
            channel.start(executor, getExecutionContext(), connectionFactory);
        }
        channel.add(request, latch);
    }

    /**
     * Await until all of this processor's channels have completed.
     * 
     * @throws CancellationException if the channel was cancelled
     * @throws ExecutionException if the channel execution threw an exception
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void await() throws ExecutionException, InterruptedException, CancellationException {
        for (CompositeRequestChannel channel : channelBySourceName.values()) {
            channel.await();
        }
    }

    /**
     * Utility to obtain the federated workspace referenced by the request. This method supports using the default workspace if
     * the workspace name is null. If no such workspace, the request is marked with an appropriate error.
     * 
     * @param request the request; may not be null
     * @param workspaceName the name of the workspace; may be null if the default workspace should be used
     * @return the federated workspace, or null if none was found
     */
    protected final FederatedWorkspace getWorkspace( Request request,
                                                     String workspaceName ) {
        try {
            return repository.getWorkspace(workspaceName);
        } catch (InvalidWorkspaceException e) {
            request.setError(e);
        } catch (Throwable e) {
            request.setError(e);
        }
        return null;
    }

    protected final String readable( Location location ) {
        return location.getString(getExecutionContext().getNamespaceRegistry());
    }

    protected final String readable( Name name ) {
        return name.getString(getExecutionContext().getNamespaceRegistry());
    }

    /**
     * Utility method to project the specified location into the federated repository's sources.
     * 
     * @param location the location that should be projected; may not be null
     * @param workspaceName the name of the workspace, or null if the default workspace should be used
     * @param request the request; may not be null
     * @param requiresUpdate true if the operation for which this projection is needed will update the content in some way, or
     *        false if read-only operations will be performed
     * @return the projected node, or null if there was no projected node (and an error was set on the request)
     */
    protected final ProjectedNode project( Location location,
                                           String workspaceName,
                                           Request request,
                                           boolean requiresUpdate ) {
        FederatedWorkspace workspace = getWorkspace(request, workspaceName);
        if (workspace == null) return null;

        ProjectedNode projectedNode = workspace.project(getExecutionContext(), location, requiresUpdate);
        if (projectedNode == null) {
            I18n msg = GraphI18n.locationCannotBeProjectedIntoWorkspaceAndSource;
            Path root = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
            request.setError(new PathNotFoundException(location, root, msg.text(readable(location),
                                                                                workspace.getName(),
                                                                                repository.getSourceName())));
        }
        return projectedNode;
    }

    protected void submit( FederatedRequest request ) {
        request.freeze();
        if (request.hasIncompleteRequests()) {
            // Submit the projected requests ...
            ProjectedRequest projected = request.getFirstProjectedRequest();
            while (projected != null) {
                if (!projected.isComplete()) {
                    // Submit to the appropriate source channel for execution ...
                    submit(projected.getRequest(), projected.getProjection().getSourceName(), request.getLatch());
                }
                projected = projected.next();
            }
        }
        // Record this federated request, ready for the join processor ...
        this.federatedRequestQueue.add(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#completeRequest(org.modeshape.graph.request.Request)
     */
    @Override
    protected void completeRequest( Request request ) {
        // Do nothing here, since this is the federated request which will be frozen in the join processor
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // Create a request and set the results ...
                VerifyNodeExistsRequest placeholderRequest = new VerifyNodeExistsRequest(request.at(), request.inWorkspace());
                placeholderRequest.setActualLocationOfNode(placeholder.location());
                federatedRequest.add(placeholderRequest, true, true, null);
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                VerifyNodeExistsRequest pushDownRequest = new VerifyNodeExistsRequest(proxy.location(), proxy.workspaceName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
        // Submit for processing ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // This placeholder may have proxy nodes as children, in which case we need to verify
                // their existance to get their actual locations.
                List<Location> children = new LinkedList<Location>();
                boolean firstRequest = true;
                for (ProjectedNode child : placeholder.children()) {
                    if (child.isPlaceholder()) {
                        children.add(child.location());
                        continue;
                    }
                    while (child != null && child.isProxy()) {
                        if (!children.isEmpty()) {
                            // Take any children so far and simply record a ReadNodeRequest with results ...
                            ReadNodeRequest placeholderRequest = new ReadNodeRequest(placeholder.location(),
                                                                                     request.inWorkspace());
                            placeholderRequest.addChildren(children);
                            if (firstRequest) {
                                firstRequest = false;
                                placeholderRequest.addProperties(placeholder.properties().values());
                            }
                            placeholderRequest.setActualLocationOfNode(placeholder.location());
                            federatedRequest.add(placeholderRequest, true, true, null);
                            children = new LinkedList<Location>();
                        }
                        // Now issue a VerifyNodeExistsRequest for the child.
                        // We'll mix these into the federated request along with the ReadNodeRequests ...
                        ProxyNode proxy = child.asProxy();
                        VerifyNodeExistsRequest verifyRequest = new VerifyNodeExistsRequest(proxy.location(),
                                                                                            proxy.workspaceName());
                        federatedRequest.add(verifyRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
                        child = child.next();
                    }
                }
                if (!children.isEmpty() || firstRequest) {
                    // Submit the children so far ...
                    ReadNodeRequest placeholderRequest = new ReadNodeRequest(placeholder.location(), request.inWorkspace());
                    placeholderRequest.addChildren(children);
                    if (firstRequest) {
                        firstRequest = false;
                        placeholderRequest.addProperties(placeholder.properties().values());
                    }
                    placeholderRequest.setActualLocationOfNode(placeholder.location());
                    federatedRequest.add(placeholderRequest, true, true, null);
                }
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                ReadNodeRequest pushDownRequest = new ReadNodeRequest(proxy.location(), proxy.workspaceName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
        // Submit for processing ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.of(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // This placeholder may have proxy nodes as children, in which case we need to verify
                // their existance to get their actual locations.
                List<Location> children = new LinkedList<Location>();
                boolean firstRequest = true;
                for (ProjectedNode child : placeholder.children()) {
                    if (child.isPlaceholder()) {
                        children.add(child.location());
                        continue;
                    }
                    while (child != null && child.isProxy()) {
                        if (!children.isEmpty()) {
                            // Take any children so far and simply record a ReadNodeRequest with results ...
                            ReadAllChildrenRequest placeholderRequest = new ReadAllChildrenRequest(placeholder.location(),
                                                                                                   request.inWorkspace());
                            placeholderRequest.addChildren(children);
                            if (firstRequest) {
                                firstRequest = false;
                            }
                            placeholderRequest.setActualLocationOfNode(placeholder.location());
                            federatedRequest.add(placeholderRequest, true, true, null);
                            children = new LinkedList<Location>();
                        }
                        // Now issue a VerifyNodeExistsRequest for the child.
                        // We'll mix these into the federated request along with the ReadNodeRequests ...
                        ProxyNode proxy = child.asProxy();
                        VerifyNodeExistsRequest verifyRequest = new VerifyNodeExistsRequest(proxy.location(),
                                                                                            proxy.workspaceName());
                        federatedRequest.add(verifyRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
                        child = child.next();
                    }
                }
                if (!children.isEmpty() || firstRequest) {
                    // Submit the children so far ...
                    ReadAllChildrenRequest placeholderRequest = new ReadAllChildrenRequest(placeholder.location(),
                                                                                           request.inWorkspace());
                    placeholderRequest.addChildren(children);
                    if (firstRequest) {
                        firstRequest = false;
                    }
                    placeholderRequest.setActualLocationOfNode(placeholder.location());
                    federatedRequest.add(placeholderRequest, true, true, null);
                }
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                ReadAllChildrenRequest pushDownRequest = new ReadAllChildrenRequest(proxy.location(), proxy.workspaceName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
        // Submit for processing ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // Create a request and set the results ...
                ReadAllPropertiesRequest placeholderRequest = new ReadAllPropertiesRequest(placeholder.location(),
                                                                                           request.inWorkspace());
                placeholderRequest.addProperties(placeholder.properties().values());
                placeholderRequest.setActualLocationOfNode(placeholder.location());
                federatedRequest.add(placeholderRequest, true, true, null);
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                ReadAllPropertiesRequest pushDownRequest = new ReadAllPropertiesRequest(proxy.location(), proxy.workspaceName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
        // Submit for processing ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // Create a request and set the results ...
                ReadPropertyRequest placeholderRequest = new ReadPropertyRequest(placeholder.location(), request.inWorkspace(),
                                                                                 request.named());
                Property property = placeholder.properties().get(request.named());
                placeholderRequest.setProperty(property);
                placeholderRequest.setActualLocationOfNode(placeholder.location());
                federatedRequest.add(placeholderRequest, true, true, null);
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                ReadPropertyRequest pushDownRequest = new ReadPropertyRequest(proxy.location(), proxy.workspaceName(),
                                                                              request.named());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
        // Submit for processing ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation creates a single {@link FederatedRequest} and a {@link ReadBranchRequest} for each {@link ProxyNode}
     * and one {@link ReadNodeRequest} for each {@link PlaceholderNode}.
     * </p>
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode != null) {
            FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());

            // And process the branch, creating ReadNodeRequests for each placeholder, and ReadBranchRequests for each proxy
            // node...
            if (request.maximumDepth() > 0) {
                processBranch(federatedRequest, projectedNode, workspace, request.maximumDepth());
            }
        }

        // Submit the requests for processing ...
        submit(federatedRequest);
    }

    /**
     * A method used recursively to add {@link ReadNodeRequest}s and {@link ReadBranchRequest}s for each
     * 
     * @param federatedRequest
     * @param projectedNode
     * @param workspace
     * @param maxDepth
     */
    protected void processBranch( FederatedRequest federatedRequest,
                                  ProjectedNode projectedNode,
                                  FederatedWorkspace workspace,
                                  int maxDepth ) {
        assert maxDepth > 0;
        while (projectedNode != null) {
            if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                // Create a request and set the results ...
                ReadNodeRequest placeholderRequest = new ReadNodeRequest(placeholder.location(), workspace.getName());
                List<Location> children = new ArrayList<Location>(placeholder.children().size());
                for (ProjectedNode child : placeholder.children()) {
                    if (child instanceof ProxyNode) {
                        ProxyNode proxy = (ProxyNode)child;
                        children.add(proxy.federatedLocation()); // the ProxyNodes will have only a path!
                        proxy.federatedLocation();
                    } else {
                        assert child instanceof PlaceholderNode;
                        children.add(child.location());
                    }
                }
                placeholderRequest.addChildren(children);
                placeholderRequest.addProperties(placeholder.properties().values());
                placeholderRequest.setActualLocationOfNode(placeholder.location());
                federatedRequest.add(placeholderRequest, true, true, null);
                if (maxDepth > 1) {
                    // For each child of the placeholder node ...
                    for (ProjectedNode child : placeholder.children()) {
                        // Call recursively, but reduce the max depth
                        processBranch(federatedRequest, child, workspace, maxDepth - 1);
                    }
                }
            } else if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                ReadBranchRequest pushDownRequest = new ReadBranchRequest(proxy.location(), proxy.workspaceName(), maxDepth);
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
            }
            projectedNode = projectedNode.next();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.under(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        PlaceholderNode placeholder = null;
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                CreateNodeRequest pushDownRequest = new CreateNodeRequest(proxy.location(), proxy.workspaceName(),
                                                                          request.named(), request.conflictBehavior(),
                                                                          request.properties());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            if (placeholder == null) placeholder = projectedNode.asPlaceholder();
            projectedNode = projectedNode.next();
        }

        // At this point, we know the parent node is a placeholder, so we cannot create children.
        // What we don't know, though, is whether there is an existing child at the desired location.
        if (placeholder != null) {
            switch (request.conflictBehavior()) {
                case UPDATE:
                case DO_NOT_REPLACE:
                    // See if there is an existing node at the desired location ...
                    Location parent = request.under();
                    if (parent.hasPath()) {
                        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
                        Path childPath = pathFactory.create(parent.getPath(), request.named());
                        Location childLocation = Location.create(childPath);
                        projectedNode = project(childLocation, request.inWorkspace(), request, true);
                        if (projectedNode != null) {
                            if (projectedNode.isProxy()) {
                                ProxyNode proxy = projectedNode.asProxy();

                                // We know that the parent is a placeholder, so this proxy node must exist, so do a read ...
                                ReadNodeRequest pushDownRequest = new ReadNodeRequest(proxy.location(), proxy.workspaceName());
                                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                                // Submit the requests for processing and then STOP ...
                                submit(federatedRequest);
                                return;
                            }
                            // The node does exist, so set the location ...
                            assert projectedNode.isPlaceholder();
                            request.setActualLocationOfNode(projectedNode.location());
                            return;
                        }
                    }
                    break;
                case APPEND:
                case REPLACE:
                    // Unable to perform this create ...
                    break;
            }
        }
        String msg = GraphI18n.unableToCreateNodeUnderPlaceholder.text(readable(request.named()),
                                                                       readable(request.under()),
                                                                       request.inWorkspace(),
                                                                       getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.from(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                RemovePropertyRequest pushDownRequest = new RemovePropertyRequest(proxy.location(), proxy.workspaceName(),
                                                                                  request.propertyName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.from()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                UpdatePropertiesRequest pushDownRequest = new UpdatePropertiesRequest(proxy.location(), proxy.workspaceName(),
                                                                                      request.properties());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.on()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdateValuesRequest)
     */
    @Override
    public void process( UpdateValuesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                UpdateValuesRequest pushDownRequest = new UpdateValuesRequest(proxy.workspaceName(), proxy.location(),
                                                                              request.property(), request.addedValues(),
                                                                              request.removedValues());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.on()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                SetPropertyRequest pushDownRequest = new SetPropertyRequest(proxy.location(), proxy.workspaceName(),
                                                                            request.property());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.on()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // A delete should be executed against any ProxyNode that applies ...
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());
        boolean submit = deleteBranch(federatedRequest, projectedNode, workspace, getExecutionContext(), false);
        if (submit) {
            // Submit the requests for processing and then STOP ...
            submit(federatedRequest);
        } else {
            // Unable to perform this delete ...
            String msg = GraphI18n.unableToDeletePlaceholder.text(readable(request.at()), request.inWorkspace(), getSourceName());
            request.setError(new UnsupportedRequestException(msg));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // A delete should be executed against any ProxyNode that applies ...
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());
        boolean submit = deleteBranch(federatedRequest, projectedNode, workspace, getExecutionContext(), true);
        if (submit) {
            // Submit the requests for processing and then STOP ...
            submit(federatedRequest);
        } else {
            // Unable to perform this delete ...
            String msg = GraphI18n.unableToDeletePlaceholder.text(readable(request.at()), request.inWorkspace(), getSourceName());
            request.setError(new UnsupportedRequestException(msg));
        }
    }

    protected boolean deleteBranch( FederatedRequest federatedRequest,
                                    ProjectedNode projectedNode,
                                    FederatedWorkspace workspace,
                                    ExecutionContext context,
                                    boolean includeParent ) {
        boolean submit = false;
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Is this proxy represent the top level node of the projection?
                if (proxy.isTopLevelNode() || !includeParent) {
                    // Then we want to delete everything *underneath* the node, but we don't want to delete
                    // the node itself since it is the node being projected and must exist in order for the
                    // projection to work.
                    DeleteChildrenRequest pushDownRequest = new DeleteChildrenRequest(proxy.location(), proxy.workspaceName());
                    federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
                } else {
                    // Create and submit a request for the projection ...
                    DeleteBranchRequest pushDownRequest = new DeleteBranchRequest(proxy.location(), proxy.workspaceName());
                    federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
                }
                submit = true;
            } else if (projectedNode.isPlaceholder()) {
                PlaceholderNode placeholder = projectedNode.asPlaceholder();
                if (includeParent) {
                    // Create a delete for this placeholder, but mark it completed. This is needed to know
                    // which placeholders were being deleted.
                    DeleteBranchRequest delete = new DeleteBranchRequest(placeholder.location(), workspace.getName());
                    delete.setActualLocationOfNode(placeholder.location());
                    federatedRequest.add(delete, true, true, null);
                }
                // Create and submit a request for each proxy below this placeholder ...
                // For each child of the placeholder node ...
                for (ProjectedNode child : placeholder.children()) {
                    while (child != null && child.isProxy()) {
                        // Call recursively ...
                        submit = deleteBranch(federatedRequest, child.asProxy(), workspace, context, true);
                        child = child.next();
                    }
                }
            }
            projectedNode = projectedNode.next();
        }
        return submit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.fromWorkspace(), request, false);
        if (projectedFromNode == null) {
            submit(new FederatedRequest(request));
            return;
        }
        ProjectedNode projectedIntoNode = project(request.into(), request.intoWorkspace(), request, true);
        if (projectedIntoNode == null) {
            submit(new FederatedRequest(request));
            return;
        }

        // Limitation: only able to project the copy if the 'from' and 'into' are in the same source & projection ...
        while (projectedFromNode != null) {
            if (projectedFromNode.isProxy()) {
                ProxyNode fromProxy = projectedFromNode.asProxy();
                // Look for a projectedIntoNode that has the same source/projection ...
                while (projectedIntoNode != null) {
                    if (projectedIntoNode.isProxy()) {
                        // Both are proxies, so compare the projection ...
                        ProxyNode intoProxy = projectedIntoNode.asProxy();
                        if (fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName())) break;
                    }
                    projectedIntoNode = projectedIntoNode.next();
                }
                if (projectedIntoNode != null) break;
            }
            projectedFromNode = projectedFromNode.next();
        }
        if (projectedFromNode == null || projectedIntoNode == null) {
            // The copy is not done within a single source ...
            String msg = GraphI18n.copyLimitedToBeWithinSingleSource.text(readable(request.from()),
                                                                          request.fromWorkspace(),
                                                                          readable(request.into()),
                                                                          request.intoWorkspace(),
                                                                          getSourceName());
            request.setError(new UnsupportedRequestException(msg));
            return;
        }

        ProxyNode fromProxy = projectedFromNode.asProxy();
        ProxyNode intoProxy = projectedIntoNode.asProxy();
        assert fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName());
        boolean sameLocation = fromProxy.isSameLocationAsOriginal() && intoProxy.isSameLocationAsOriginal();

        // Create the pushed-down request ...
        CopyBranchRequest pushDown = new CopyBranchRequest(fromProxy.location(), fromProxy.workspaceName(), intoProxy.location(),
                                                           intoProxy.workspaceName(), request.desiredName(),
                                                           request.nodeConflictBehavior());
        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        federatedRequest.add(pushDown, sameLocation, false, fromProxy.projection(), intoProxy.projection());

        // Submit the requests for processing and then STOP ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.fromWorkspace(), request, false);
        if (projectedFromNode == null) {
            submit(new FederatedRequest(request));
            return;
        }
        ProjectedNode projectedIntoNode = project(request.into(), request.intoWorkspace(), request, true);
        if (projectedIntoNode == null) {
            submit(new FederatedRequest(request));
            return;
        }

        // Limitation: only able to project the copy if the 'from' and 'into' are in the same source & projection ...
        while (projectedFromNode != null) {
            if (projectedFromNode.isProxy()) {
                ProxyNode fromProxy = projectedFromNode.asProxy();
                // Look for a projectedIntoNode that has the same source/projection ...
                while (projectedIntoNode != null) {
                    if (projectedIntoNode.isProxy()) {
                        // Both are proxies, so compare the projection ...
                        ProxyNode intoProxy = projectedIntoNode.asProxy();
                        if (fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName())) break;
                    }
                    projectedIntoNode = projectedIntoNode.next();
                }
                if (projectedIntoNode != null) break;
            }
            projectedFromNode = projectedFromNode.next();
        }
        if (projectedFromNode == null || projectedIntoNode == null) {
            // The copy is not done within a single source ...
            String msg = GraphI18n.cloneLimitedToBeWithinSingleSource.text(readable(request.from()),
                                                                           request.fromWorkspace(),
                                                                           readable(request.into()),
                                                                           request.intoWorkspace(),
                                                                           getSourceName());
            request.setError(new UnsupportedRequestException(msg));
            return;
        }

        ProxyNode fromProxy = projectedFromNode.asProxy();
        ProxyNode intoProxy = projectedIntoNode.asProxy();
        assert fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName());
        boolean sameLocation = fromProxy.isSameLocationAsOriginal() && intoProxy.isSameLocationAsOriginal();

        // Create the pushed-down request ...
        CloneBranchRequest pushDown = new CloneBranchRequest(fromProxy.location(), fromProxy.workspaceName(),
                                                             intoProxy.location(), intoProxy.workspaceName(),
                                                             request.desiredName(), request.desiredSegment(),
                                                             request.removeExisting());
        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        federatedRequest.add(pushDown, sameLocation, false, fromProxy.projection(), intoProxy.projection());

        // Submit the requests for processing and then STOP ...
        submit(federatedRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.inWorkspace(), request, true);
        if (projectedFromNode == null) {
            submit(new FederatedRequest(request));
            return;
        }

        ProjectedNode projectedBeforeNode = request.before() != null ? project(request.before(),
                                                                               request.inWorkspace(),
                                                                               request,
                                                                               true) : null;

        boolean sameLocation = true;
        if (request.into() != null) {
            // Look at where the node is being moved; it must be within the same source/projection ...
            ProjectedNode projectedIntoNode = project(request.into(), request.inWorkspace(), request, true);
            if (projectedIntoNode == null) return;
            // Limitation: only able to project the move if the 'from' and 'into' are in the same source & projection ...
            while (projectedFromNode != null) {
                if (projectedFromNode.isProxy()) {
                    ProxyNode fromProxy = projectedFromNode.asProxy();
                    // Look for a projectedIntoNode that has the same source/projection ...
                    while (projectedIntoNode != null) {
                        if (projectedIntoNode.isProxy()) {
                            // Both are proxies, so compare the projection ...
                            ProxyNode intoProxy = projectedIntoNode.asProxy();
                            if (fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName())) break;
                        }
                        projectedIntoNode = projectedIntoNode.next();
                    }
                    if (projectedIntoNode != null) break;
                }
                projectedFromNode = projectedFromNode.next();
            }
            if (projectedFromNode == null || projectedIntoNode == null) {
                // The move is not done within a single source ...
                String msg = GraphI18n.moveLimitedToBeWithinSingleSource.text(readable(request.from()),
                                                                              request.inWorkspace(),
                                                                              readable(request.into()),
                                                                              request.inWorkspace(),
                                                                              getSourceName());
                request.setError(new UnsupportedRequestException(msg));
                return;
            }

            ProxyNode fromProxy = projectedFromNode.asProxy();
            ProxyNode intoProxy = projectedIntoNode.asProxy();
            ProxyNode beforeProxy = request.before() != null ? projectedBeforeNode.asProxy() : null;
            assert fromProxy.projection().getSourceName().equals(intoProxy.projection().getSourceName());
            sameLocation = fromProxy.isSameLocationAsOriginal() && intoProxy.isSameLocationAsOriginal();

            // Create the pushed-down request ...
            Location beforeProxyLocation = beforeProxy != null ? beforeProxy.location() : null;
            MoveBranchRequest pushDown = new MoveBranchRequest(fromProxy.location(), intoProxy.location(), beforeProxyLocation,
                                                               intoProxy.workspaceName(), request.desiredName(),
                                                               request.conflictBehavior());
            // Create the federated request ...
            FederatedRequest federatedRequest = new FederatedRequest(request);
            federatedRequest.add(pushDown, sameLocation, false, fromProxy.projection(), intoProxy.projection());

            // Submit the requests for processing and then STOP ...
            submit(federatedRequest);
        } else {
            ProxyNode fromProxy = projectedFromNode.asProxy();
            ProxyNode beforeProxy = request.before() != null ? projectedBeforeNode.asProxy() : null;
            Location beforeProxyLocation = beforeProxy != null ? beforeProxy.location() : null;
            MoveBranchRequest pushDown = new MoveBranchRequest(fromProxy.location(), null, beforeProxyLocation,
                                                               fromProxy.workspaceName(), request.desiredName(),
                                                               request.conflictBehavior());
            // Create the federated request ...
            FederatedRequest federatedRequest = new FederatedRequest(request);
            federatedRequest.add(pushDown, sameLocation, false, fromProxy.projection());

            // Submit the requests for processing and then STOP ...
            submit(federatedRequest);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        // Figure out where the 'at' is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                RenameNodeRequest pushDownRequest = new RenameNodeRequest(proxy.location(), proxy.workspaceName(),
                                                                          request.toName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.at()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                LockBranchRequest pushDownRequest = new LockBranchRequest(proxy.location(), proxy.workspaceName(),
                                                                          request.lockScope(), request.lockTimeoutInMillis());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.at()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

        if (projectedNode == null) {
            submit(federatedRequest);
            return;
        }

        // Any non-read request should be submitted to the first ProxyNode ...
        while (projectedNode != null) {
            if (projectedNode.isProxy()) {
                ProxyNode proxy = projectedNode.asProxy();
                // Create and submit a request for the projection ...
                UnlockBranchRequest pushDownRequest = new UnlockBranchRequest(proxy.location(), proxy.workspaceName());
                federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());

                // Submit the requests for processing and then STOP ...
                submit(federatedRequest);
                return;
            }
            assert projectedNode.isPlaceholder();
            projectedNode = projectedNode.next();
        }
        // Unable to perform this update ...
        String msg = GraphI18n.unableToUpdatePlaceholder.text(readable(request.at()), request.inWorkspace(), getSourceName());
        request.setError(new UnsupportedRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.workspaceName());
        if (workspace != null) {
            request.setActualWorkspaceName(workspace.getName());

            // Get the root location ...
            Location root = Location.create(getExecutionContext().getValueFactories().getPathFactory().createRootPath());
            ProjectedNode projectedNode = project(root, workspace.getName(), request, false);

            // Create the federated request ...
            FederatedRequest federatedRequest = new FederatedRequest(request);
            while (projectedNode != null) {
                if (projectedNode.isPlaceholder()) {
                    PlaceholderNode placeholder = projectedNode.asPlaceholder();
                    // Create a request and set the results ...
                    VerifyNodeExistsRequest placeholderRequest = new VerifyNodeExistsRequest(root, workspace.getName());
                    placeholderRequest.setActualLocationOfNode(placeholder.location());
                    federatedRequest.add(placeholderRequest, true, true, null);
                } else if (projectedNode.isProxy()) {
                    ProxyNode proxy = projectedNode.asProxy();
                    // Create and submit a request for the projection ...
                    VerifyNodeExistsRequest pushDownRequest = new VerifyNodeExistsRequest(proxy.location(), proxy.workspaceName());
                    federatedRequest.add(pushDownRequest, proxy.isSameLocationAsOriginal(), false, proxy.projection());
                }
                projectedNode = projectedNode.next();
            }
            // Submit for processing ...
            submit(federatedRequest);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        request.setAvailableWorkspaceNames(repository.getWorkspaceNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportCreatingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportCloningWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportDestroyingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        super.close();
        for (CompositeRequestChannel channel : channelBySourceName.values()) {
            channel.close();
        }
    }

    protected void cancel( boolean mayInterruptIfRunning ) {
        for (CompositeRequestChannel channel : channelBySourceName.values()) {
            channel.cancel(mayInterruptIfRunning);
        }
    }
}

/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.graph.connector.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.federation.FederatedRequest.ProjectedRequest;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.FullTextSearchRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.LockBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.QueryRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.RemovePropertyRequest;
import org.jboss.dna.graph.request.RenameNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.SetPropertyRequest;
import org.jboss.dna.graph.request.UnlockBranchRequest;
import org.jboss.dna.graph.request.UnsupportedRequestException;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.UpdateValuesRequest;
import org.jboss.dna.graph.request.VerifyNodeExistsRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

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

    /**
     * A psuedo Request that is used by {@link Channel} to insert into a request queue so that the queue's iterator knows when
     * there are no more requests to process.
     */
    protected static class LastRequest extends Request {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isReadOnly() {
            return false;
        }
    }

    /**
     * Represents the channel for a specific source into which this processor submits the requests for that source. To use, create
     * a Channel, {@link Channel#start(ExecutorService, ExecutionContext, RepositoryConnectionFactory) start it}, and then
     * {@link Channel#add(Request) add} requests (optionally with a {@link Channel#add(Request, CountDownLatch) latch} or via a
     * {@link Channel#addAndAwait(Request) add and await}). Finally, call {@link Channel#done()} when there are no more requests.
     * <p>
     * When the channel is {@link Channel#start(ExecutorService, ExecutionContext, RepositoryConnectionFactory) started}, it
     * creates a {@link Callable} and submits it to the supplied {@link ExecutorService}. (The resulting {@link Future} is then
     * captured so that the channel can be {@link Channel#cancel(boolean) cancelled}.) The Callable obtains a
     * {@link RepositoryConnection connection} to the channel's source, and then has the connection process a single
     * {@link CompositeRequest} that fronts the queue of Request instances added to this channel. Because a blocking queue is
     * used, the CompositeRequest's {@link CompositeRequest#iterator() iterator} blocks (on {@link Iterator#hasNext()}) until the
     * next request is available. When {@link Channel#done()} is called, the iterator stops blocking and completes.
     * </p>
     */
    protected static class Channel {
        protected final String sourceName;
        /** The list of all requests that are or have been processed as part of this channel */
        protected final LinkedList<Request> allRequests = new LinkedList<Request>();
        /** The queue of requests that remain unprocessed */
        private final BlockingQueue<Request> queue = new LinkedBlockingQueue<Request>();
        /** The CompositeRequest that is submitted to the underlying processor */
        protected final CompositeRequest composite;
        /** The Future that is submitted to the ExecutorService to do the processing */
        protected Future<String> future;
        /** Flag that defines whether the channel has processed all requests */
        protected final AtomicBoolean done = new AtomicBoolean(false);
        protected Throwable compositeError = null;

        /**
         * Create a new channel that operates against the supplied source.
         * 
         * @param sourceName the name of the repository source used to execute this channel's {@link #allRequests() requests}; may
         *        not be null or empty
         */
        protected Channel( final String sourceName ) {
            assert sourceName != null;
            this.sourceName = sourceName;
            this.composite = new CompositeRequest(false) {
                private static final long serialVersionUID = 1L;
                private final LinkedList<Request> allRequests = Channel.this.allRequests;

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.CompositeRequest#iterator()
                 */
                @Override
                public Iterator<Request> iterator() {
                    return createIterator();
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.CompositeRequest#getRequests()
                 */
                @Override
                public List<Request> getRequests() {
                    return allRequests;
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.CompositeRequest#size()
                 */
                @Override
                public int size() {
                    return done.get() ? allRequests.size() : CompositeRequest.UNKNOWN_NUMBER_OF_REQUESTS;
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.Request#cancel()
                 */
                @Override
                public void cancel() {
                    done.set(true);
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.Request#setError(java.lang.Throwable)
                 */
                @Override
                public void setError( Throwable error ) {
                    compositeError = error;
                    super.setError(error);
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.request.Request#hasError()
                 */
                @Override
                public boolean hasError() {
                    return compositeError != null || super.hasError();
                }
            };
        }

        /**
         * Utility method to create an iterator over the requests in this channel. This really should be called once
         * 
         * @return the iterator over the channels
         */
        protected Iterator<Request> createIterator() {
            final BlockingQueue<Request> queue = this.queue;
            return new Iterator<Request>() {
                private Request next;

                public boolean hasNext() {
                    // If next still has a request, then 'hasNext()' has been called multiple times in a row
                    if (next != null) return true;

                    // Now, block for a next item (this blocks) ...
                    try {
                        next = queue.take();
                    } catch (InterruptedException e) {
                        // This happens when the federated connector has been told to shutdown now, and it shuts down
                        // its executor (the worker pool) immediately by interrupting each in-use thread.
                        // In this case, we should consider there to be more more requests ...
                        try {
                            return false;
                        } finally {
                            // reset the interrupted status ...
                            Thread.interrupted();
                        }
                    }
                    if (next instanceof LastRequest) {
                        return false;
                    }
                    return next != null;
                }

                public Request next() {
                    if (next == null) {
                        // Must have been called without first calling 'hasNext()' ...
                        try {
                            next = queue.take();
                        } catch (InterruptedException e) {
                            // This happens when the federated connector has been told to shutdown now, and it shuts down
                            // its executor (the worker pool) immediately by interrupting each in-use thread.
                            // In this case, we should consider there to be more more requests (again, this case
                            // is when 'next()' has been called without calling 'hasNext()') ...
                            try {
                                throw new NoSuchElementException();
                            } finally {
                                // reset the interrupted status ...
                                Thread.interrupted();
                            }
                        }
                    }
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    Request result = next;
                    next = null;
                    return result;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Begins processing any requests that have been {@link #add(Request) added} to this channel. Processing is done by
         * submitting the channel to the supplied executor.
         * 
         * @param executor the executor that is to do the work; may not be null
         * @param context the execution context in which the work is to be performed; may not be null
         * @param connectionFactory the connection factory that should be used to create connections; may not be null
         * @throws IllegalStateException if this channel has already been started
         */
        protected void start( final ExecutorService executor,
                              final ExecutionContext context,
                              final RepositoryConnectionFactory connectionFactory ) {
            assert executor != null;
            assert context != null;
            assert connectionFactory != null;
            if (this.future != null) {
                throw new IllegalStateException();
            }
            this.future = executor.submit(new Callable<String>() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see java.util.concurrent.Callable#call()
                 */
                public String call() throws Exception {
                    final RepositoryConnection connection = connectionFactory.createConnection(sourceName);
                    assert connection != null;
                    try {
                        connection.execute(context, composite);
                    } finally {
                        connection.close();
                    }
                    return sourceName;
                }
            });
        }

        /**
         * Add the request to this channel for asynchronous processing. This method is called by the
         * {@link ForkRequestProcessor#submit(Request, String)} method.
         * 
         * @param request the request to be submitted; may not be null
         * @throws IllegalStateException if this channel has already been marked as {@link #done()}
         */
        protected void add( Request request ) {
            if (done.get()) {
                throw new IllegalStateException(GraphI18n.unableToAddRequestToChannelThatIsDone.text(sourceName, request));
            }
            assert request != null;
            this.allRequests.add(request);
            this.queue.add(request);
        }

        /**
         * Add the request to this channel for asynchronous processing, and supply a {@link CountDownLatch count-down latch} that
         * should be {@link CountDownLatch#countDown() decremented} when this request is completed.
         * 
         * @param request the request to be submitted; may not be null
         * @param latch the count-down latch that should be decremented when <code>request</code> has been completed; may not be
         *        null
         * @return the same latch that was supplied, for method chaining purposes; never null
         * @throws IllegalStateException if this channel has already been marked as {@link #done()}
         */
        protected CountDownLatch add( Request request,
                                      CountDownLatch latch ) {
            if (done.get()) {
                throw new IllegalStateException(GraphI18n.unableToAddRequestToChannelThatIsDone.text(sourceName, request));
            }
            assert request != null;
            assert latch != null;
            // Submit the request for processing ...
            this.allRequests.add(request);
            request.setLatchForFreezing(latch);
            this.queue.add(request);
            return latch;
        }

        /**
         * Add the request to this channel for asynchronous processing, and supply a {@link CountDownLatch count-down latch} that
         * should be {@link CountDownLatch#countDown() decremented} when this request is completed. This method is called by the
         * {@link ForkRequestProcessor#submitAndAwait(Request, String)} method.
         * 
         * @param request the request to be submitted; may not be null
         * @throws InterruptedException if the current thread is interrupted while waiting
         */
        protected void addAndAwait( Request request ) throws InterruptedException {
            // Add the request with a latch, then block until the request has completed ...
            add(request, new CountDownLatch(1)).await();
        }

        /**
         * Mark this source as having no more requests to process.
         */
        protected void done() {
            this.done.set(true);
            this.queue.add(new LastRequest());
        }

        /**
         * Return whether this channel has been {@link #done() marked as done}.
         * 
         * @return true if the channel was marked as done, or false otherwise
         */
        protected boolean isDone() {
            return done.get();
        }

        /**
         * Cancel this forked channel, stopping work as soon as possible. If the channel has not yet been started, this method
         * 
         * @param mayInterruptIfRunning true if the channel is still being worked on, and the thread on which its being worked on
         *        may be interrupted, or false if the channel should be allowed to finish if it is already in work.
         */
        public void cancel( boolean mayInterruptIfRunning ) {
            if (this.future == null || this.future.isDone() || this.future.isCancelled()) return;

            // Mark the composite as cancelled first, so that the next composed request will be marked as
            // cancelled.
            this.composite.cancel();

            // Now mark the channel as being done ...
            done();

            // Now, mark the channel as being cancelled (do allow interrupting the worker thread) ...
            this.future.cancel(mayInterruptIfRunning);
        }

        /**
         * Return whether this channel has been {@link #start(ExecutorService, ExecutionContext, RepositoryConnectionFactory)
         * started}.
         * 
         * @return true if this channel was started, or false otherwise
         */
        public boolean isStarted() {
            return this.future != null;
        }

        /**
         * Return whether this channel has completed all of its work.
         * 
         * @return true if the channel was started and is complete, or false otherwise
         */
        public boolean isComplete() {
            return this.future != null && this.future.isDone();
        }

        /**
         * Await until this channel has completed.
         * 
         * @throws CancellationException if the channel was cancelled
         * @throws ExecutionException if the channel execution threw an exception
         * @throws InterruptedException if the current thread is interrupted while waiting
         */
        protected void await() throws ExecutionException, InterruptedException, CancellationException {
            this.future.get();
        }

        /**
         * Get all the requests that were submitted to this queue. The resulting list is the actual list that is appended when
         * requests are added, and may change until the channel is marked as {@link #done() done}.
         * 
         * @return all of the requests that were submitted to this channel; never null
         */
        protected List<Request> allRequests() {
            return allRequests;
        }

        /**
         * Get the name of the source that this channel uses.
         * 
         * @return the source name; never null
         */
        protected String sourceName() {
            return sourceName;
        }
    }

    private final FederatedRepository repository;
    private final ExecutorService executor;
    private final RepositoryConnectionFactory connectionFactory;
    private final Map<String, Channel> channelBySourceName = new HashMap<String, Channel>();
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
        Channel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new Channel(sourceName);
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
        Channel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new Channel(sourceName);
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
        Channel channel = channelBySourceName.get(sourceName);
        if (channel == null) {
            channel = new Channel(sourceName);
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
        for (Channel channel : channelBySourceName.values()) {
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#completeRequest(org.jboss.dna.graph.request.Request)
     */
    @Override
    protected void completeRequest( Request request ) {
        // Do nothing here, since this is the federated request which will be frozen in the join processor
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.of(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, false);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);
        FederatedWorkspace workspace = getWorkspace(request, request.inWorkspace());

        // And process the branch, creating ReadNodeRequests for each placeholder, and ReadBranchRequests for each proxy node...
        if (request.maximumDepth() > 0) {
            processBranch(federatedRequest, projectedNode, workspace, request.maximumDepth());
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
                    children.add(child.location()); // the ProxyNodes will have only a path!
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.under(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RemovePropertyRequest)
     */
    @Override
    public void process( RemovePropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.from(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdateValuesRequest)
     */
    @Override
    public void process( UpdateValuesRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.SetPropertyRequest)
     */
    @Override
    public void process( SetPropertyRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.on(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteChildrenRequest)
     */
    @Override
    public void process( DeleteChildrenRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.fromWorkspace(), request, false);
        if (projectedFromNode == null) return;
        ProjectedNode projectedIntoNode = project(request.into(), request.intoWorkspace(), request, true);
        if (projectedIntoNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.fromWorkspace(), request, false);
        if (projectedFromNode == null) return;
        ProjectedNode projectedIntoNode = project(request.into(), request.intoWorkspace(), request, true);
        if (projectedIntoNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        // Figure out where the 'from' is projected ...
        ProjectedNode projectedFromNode = project(request.from(), request.inWorkspace(), request, true);
        if (projectedFromNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        // Figure out where the 'at' is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        // Figure out where this request is projected ...
        ProjectedNode projectedNode = project(request.at(), request.inWorkspace(), request, true);
        if (projectedNode == null) return;

        // Create the federated request ...
        FederatedRequest federatedRequest = new FederatedRequest(request);

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        FederatedWorkspace workspace = getWorkspace(request, request.workspaceName());
        if (workspace != null) {
            request.setActualWorkspaceName(workspace.getName());

            // Get the root location ...
            Location root = Location.create(getExecutionContext().getValueFactories().getPathFactory().createRootPath());
            ProjectedNode projectedNode = project(root, workspace.getName(), request, false);
            if (projectedNode == null) return;

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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        request.setAvailableWorkspaceNames(repository.getWorkspaceNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportCreatingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportCloningWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        String msg = GraphI18n.federatedSourceDoesNotSupportDestroyingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.QueryRequest)
     */
    @Override
    public void process( QueryRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        super.close();
        for (Channel channel : channelBySourceName.values()) {
            channel.done();
        }
    }

    protected void cancel( boolean mayInterruptIfRunning ) {
        for (Channel channel : channelBySourceName.values()) {
            channel.cancel(mayInterruptIfRunning);
        }
    }

}

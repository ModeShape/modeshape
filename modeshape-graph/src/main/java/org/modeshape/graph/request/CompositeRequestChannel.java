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
package org.modeshape.graph.request;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A channel for Request objects that can be submitted to a consumer (typically a {@link RequestProcessor} or
 * {@link RepositoryConnection}) while allowing the channel owner to continue adding more Request objects into the channel.
 * <p>
 * The owner of this channel is responsible for starting the processing using one of the two <code>start(...)</code> methods,
 * adding {@link Request}s via <code>add(...)</code> methods, {@link #close() closing} the channel when there are no more requests
 * to be added, and finally {@link #await() awaiting} until all of the submitted requests have been processed. Note that the owner
 * can optionally pre-fill the channel with Request objects before calling <code>start(...)</code>.
 * </p>
 * <p>
 * The consumer will be handed a {@link CompositeRequest}, and should use the {@link CompositeRequest#iterator()} method to obtain
 * an Iterator&lt;Request>. The {@link Iterator#hasNext()} method will block until there is another Request available in the
 * channel, or until the channel is closed (at which point {@link Iterator#hasNext()} will return false. (Notice that the
 * {@link Iterator#next()} method will also block if it is not preceeded by a {@link Iterator#hasNext()}, but will throw a
 * {@link NoSuchElementException} when there are no more Request objects and the channel is closed.)
 * </p>
 * <p>
 * Because the CompositeRequest's iterator will block, the consumer will block while processing the request. Therefore, this
 * channel submits the CompositeRequest to the consumer asynchronously, via an {@link ExecutorService} supplied in one of the two
 * {@link #start(ExecutorService, ExecutionContext, RepositoryConnectionFactory) start}
 * {@link #start(ExecutorService, RequestProcessor, boolean) methods}.
 * </p>
 */
public class CompositeRequestChannel {

    protected final String sourceName;
    /** The list of all requests that are or have been processed as part of this channel */
    protected final LinkedList<Request> allRequests = new LinkedList<Request>();
    /** The queue of requests that remain unprocessed */
    private final BlockingQueue<Request> queue = new LinkedBlockingQueue<Request>();
    /** The CompositeRequest that is submitted to the underlying processor */
    protected final CompositeRequest composite;
    /**
     * The Future that is submitted to the ExecutorService to do the processing, which is used to {@link #await()} until the
     * processing is completed or {@link #cancel(boolean) cancel} the work
     */
    protected Future<String> future;
    /** Flag that defines whether the channel has processed all requests */
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected Throwable compositeError = null;

    /**
     * Create a new channel with the supplied channel name.
     * 
     * @param sourceName the name of the repository source used to execute this channel's {@link #allRequests() requests}; may not
     *        be null or empty
     */
    public CompositeRequestChannel( final String sourceName ) {
        assert sourceName != null;
        this.sourceName = sourceName;
        this.composite = new ChannelCompositeRequest();
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
                if (RequestType.LAST == next.getType()) {
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
     * Begins processing any requests that have been {@link #add(Request) added} to this channel. Processing is done by submitting
     * the channel to the supplied executor.
     * 
     * @param executor the executor that is to do the work; may not be null
     * @param context the execution context in which the work is to be performed; may not be null
     * @param connectionFactory the connection factory that should be used to create connections; may not be null
     * @throws IllegalStateException if this channel has already been started
     */
    public void start( final ExecutorService executor,
                       final ExecutionContext context,
                       final RepositoryConnectionFactory connectionFactory ) {
        assert executor != null;
        assert context != null;
        assert connectionFactory != null;
        assert sourceName != null;
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
     * Begins processing any requests that have been {@link #add(Request) added} to this channel. Processing is done by submitting
     * the channel to the supplied executor.
     * 
     * @param executor the executor that is to do the work; may not be null
     * @param processor the request processor that will be used to execute the requests; may not be null
     * @param closeProcessorWhenCompleted true if this method should call {@link RequestProcessor#close()} when the channel is
     *        completed, or false if the caller is responsible for doing this
     * @throws IllegalStateException if this channel has already been started
     */
    public void start( final ExecutorService executor,
                       final RequestProcessor processor,
                       final boolean closeProcessorWhenCompleted ) {
        assert executor != null;
        assert processor != null;
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
                try {
                    processor.process(composite);
                } finally {
                    if (closeProcessorWhenCompleted) processor.close();
                }
                return sourceName;
            }
        });
    }

    /**
     * Add the request to this channel for asynchronous processing.
     * 
     * @param request the request to be submitted; may not be null
     * @throws IllegalStateException if this channel has already been {@link #close() closed}
     */
    public void add( Request request ) {
        if (closed.get()) {
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
     * @param latch the count-down latch that should be decremented when <code>request</code> has been completed; may not be null
     * @return the same latch that was supplied, for method chaining purposes; never null
     * @throws IllegalStateException if this channel has already been {@link #close() closed}
     */
    public CountDownLatch add( Request request,
                               CountDownLatch latch ) {
        if (closed.get()) {
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
     * Add the request to this channel for processing, but wait to return until the request has been processed.
     * 
     * @param request the request to be submitted; may not be null
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void addAndAwait( Request request ) throws InterruptedException {
        // Add the request with a latch, then block until the request has completed ...
        add(request, new CountDownLatch(1)).await();
    }

    /**
     * Mark this source as having no more requests to process.
     */
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            this.queue.add(new LastRequest());
        }
    }

    /**
     * Return whether this channel has been {@link #close() closed}.
     * 
     * @return true if the channel was marked as done, or false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Cancel this forked channel, stopping work as soon as possible. If the channel has not yet been started, this method
     * 
     * @param mayInterruptIfRunning true if the channel is still being worked on, and the thread on which its being worked on may
     *        be interrupted, or false if the channel should be allowed to finish if it is already in work.
     */
    public void cancel( boolean mayInterruptIfRunning ) {
        if (this.future == null || this.future.isDone() || this.future.isCancelled()) return;

        // Mark the composite as cancelled first, so that the next composed request will be marked as
        // cancelled.
        this.composite.cancel();

        // Now mark the channel as being done ...
        close();

        // Now, mark the channel as being cancelled (do allow interrupting the worker thread) ...
        this.future.cancel(mayInterruptIfRunning);
    }

    /**
     * Return whether this channel has been {@link #start(ExecutorService, ExecutionContext, RepositoryConnectionFactory) started}
     * .
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
    public void await() throws ExecutionException, InterruptedException, CancellationException {
        this.future.get();
    }

    /**
     * Get all the requests that were submitted to this queue. The resulting list is the actual list that is appended when
     * requests are added, and may change until the channel is {@link #close() closed}.
     * 
     * @return all of the requests that were submitted to this channel; never null
     */
    public List<Request> allRequests() {
        return allRequests;
    }

    /**
     * Get the name of the source that this channel uses.
     * 
     * @return the source name; never null
     */
    public String sourceName() {
        return sourceName;
    }

    protected class ChannelCompositeRequest extends CompositeRequest {
        private static final long serialVersionUID = 1L;
        private final LinkedList<Request> allRequests = CompositeRequestChannel.this.allRequests;

        protected ChannelCompositeRequest() {
            super(false);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.CompositeRequest#iterator()
         */
        @Override
        public Iterator<Request> iterator() {
            return createIterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.CompositeRequest#getRequests()
         */
        @Override
        public List<Request> getRequests() {
            return allRequests;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.CompositeRequest#size()
         */
        @Override
        public int size() {
            return closed.get() ? allRequests.size() : CompositeRequest.UNKNOWN_NUMBER_OF_REQUESTS;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.Request#cancel()
         */
        @Override
        public void cancel() {
            closed.set(true);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.Request#setError(java.lang.Throwable)
         */
        @Override
        public void setError( Throwable error ) {
            compositeError = error;
            super.setError(error);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.Request#hasError()
         */
        @Override
        public boolean hasError() {
            return compositeError != null || super.hasError();
        }
    }

    /**
     * A psuedo Request that is used by the {@link CompositeRequestChannel} to insert into a request queue so that the queue's
     * iterator knows when there are no more requests to process.
     */
    protected static class LastRequest extends Request {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public RequestType getType() {
            return RequestType.LAST;
        }

    }
}

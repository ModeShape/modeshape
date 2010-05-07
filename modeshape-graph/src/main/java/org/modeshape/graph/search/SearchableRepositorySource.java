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
package org.modeshape.graph.search;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.CompositeRequestChannel;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A {@link RepositorySource} implementation that can be used as a wrapper around another
 * {@link RepositorySourceCapabilities#supportsSearches() non-searchable} or
 * {@link RepositorySourceCapabilities#supportsQueries() non-querable} RepositorySource instance to provide search and query
 * capability.
 */
@ThreadSafe
public class SearchableRepositorySource implements RepositorySource {

    private static final long serialVersionUID = 1L;

    private final RepositorySource delegate;
    private final boolean executeAsynchronously;
    private final boolean updateIndexesAsynchronously;
    private final transient ExecutorService executorService;
    private final transient SearchEngine searchEngine;

    /**
     * Create a new searchable and queryable RepositorySource around an instance that is neither.
     * 
     * @param wrapped the RepositorySource that is not searchable and queryable
     * @param searchEngine the search engine that is to be used
     * @param executorService the ExecutorService that should be used when submitting requests to the wrapped service; may be null
     *        if all operations should be performed in the calling thread
     * @param executeAsynchronously true if an {@link ExecutorService} is provided and the requests to the wrapped source are to
     *        be executed asynchronously
     * @param updateIndexesAsynchronously true if an {@link ExecutorService} is provided and the indexes are to be updated in a
     *        different thread than the thread executing the {@link RepositoryConnection#execute(ExecutionContext, Request)}
     *        calls.
     */
    public SearchableRepositorySource( RepositorySource wrapped,
                                       SearchEngine searchEngine,
                                       ExecutorService executorService,
                                       boolean executeAsynchronously,
                                       boolean updateIndexesAsynchronously ) {
        CheckArg.isNotNull(wrapped, "wrapped");
        CheckArg.isNotNull(searchEngine, "searchEngine");
        this.delegate = wrapped;
        this.executorService = executorService;
        this.searchEngine = searchEngine;
        this.updateIndexesAsynchronously = this.executorService != null && updateIndexesAsynchronously;
        this.executeAsynchronously = this.executorService != null && executeAsynchronously;
    }

    /**
     * Create a new searchable and queryable RepositorySource around an instance that is neither. All of the request processing
     * will be done in the calling thread, and updating the indexes will be done synchronously within the context of the
     * {@link RepositoryConnection#execute(ExecutionContext, Request)} method (and obviously on the same thread). This means that
     * the execution of the requests will not return until the indexes have been updated with any changes made by the requests.
     * <p>
     * This is equivalent to calling <code>new SearchableRepositorySource(wrapped,searchEngine,null,false)</code>
     * </p>
     * 
     * @param wrapped the RepositorySource that is not searchable and queryable
     * @param searchEngine the search engine that is to be used
     */
    public SearchableRepositorySource( RepositorySource wrapped,
                                       SearchEngine searchEngine ) {
        this(wrapped, searchEngine, null, false, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return delegate.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
        this.delegate.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        // Return the capabilities of the source, except with search and query suppport enabled ...
        return new RepositorySourceCapabilities(this.delegate.getCapabilities()) {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositorySourceCapabilities#supportsQueries()
             */
            @Override
            public boolean supportsQueries() {
                return true;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositorySourceCapabilities#supportsSearches()
             */
            @Override
            public boolean supportsSearches() {
                return true;
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (executeRequestsAsynchronously()) {
            // Use the executor service ...
            assert executorService != null;
            return new ParallelConnection(executorService);
        }
        // We need to do the processing in this thread ...
        return new SynchronousConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return delegate.getRetryLimit();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( final RepositoryContext context ) throws RepositorySourceException {
        final String delegateSourceName = delegate.getName();

        // The search engine will need a connection factory to the source, but the 'context' connection factory
        // will point back to this wrapper. So make one ...
        final RepositoryConnectionFactory originalConnectionFactory = context.getRepositoryConnectionFactory();
        final RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (delegateSourceName.equals(sourceName)) return delegate().getConnection();
                return originalConnectionFactory.createConnection(sourceName);
            }
        };

        // Create an observer so that we know what changes are being made in the delegate ...
        final Observer observer = new Observer() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
             */
            public void notify( final Changes changes ) {
                if (changes != null) {
                    if (updateIndexesAsynchronously()) {
                        // Enqueue the changes in the delegate content ...
                        executorService().submit(new Runnable() {
                            public void run() {
                                process(context.getExecutionContext(), changes);
                            }
                        });
                    } else {
                        process(context.getExecutionContext(), changes);
                    }
                }
            }
        };

        // Create a new RepositoryContext that uses our observer and connection factory ...
        final RepositoryContext newContext = new RepositoryContext() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
             */
            public Subgraph getConfiguration( int depth ) {
                return context.getConfiguration(depth);
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
             */
            public ExecutionContext getExecutionContext() {
                return context.getExecutionContext();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
             */
            public Observer getObserver() {
                return observer;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
             */
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return connectionFactory;
            }
        };

        // Now initialize the delegate with the delegate's context ...
        delegate.initialize(newContext);
    }

    protected final SearchEngine searchEngine() {
        assert searchEngine != null;
        return searchEngine;
    }

    protected final boolean updateIndexesAsynchronously() {
        return executorService != null && updateIndexesAsynchronously;
    }

    protected final boolean executeRequestsAsynchronously() {
        return executorService != null && executeAsynchronously;
    }

    protected final ExecutorService executorService() {
        assert executorService != null;
        return executorService;
    }

    protected final RepositorySource delegate() {
        return this.delegate;
    }

    /**
     * Do the work of processing the changes and updating the {@link #searchEngine}. This method may be called while on one of the
     * threads owned by the {@link #executorService executor service} (if {@link #updateIndexesAsynchronously()} returns true), or
     * from the thread {@link RepositoryConnection#execute(ExecutionContext, org.modeshape.graph.request.Request) executing} the
     * requests on the {@link #delegate} (if {@link #updateIndexesAsynchronously()} returns false).
     * 
     * @param context the execution context in which the indexes should be updated
     * @param changes the changes; never null
     */
    protected void process( ExecutionContext context,
                            Changes changes ) {
        assert context != null;
        assert changes != null;
        if (searchEngine == null) return; // null only after deserialization ...
        // Obtain a request processor
        searchEngine.index(context, changes.getChangeRequests());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        delegate.setRetryLimit(limit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() throws NamingException {
        return delegate.getReference();
    }

    @NotThreadSafe
    protected abstract class AbstractConnection implements RepositoryConnection {
        private RepositoryConnection delegateConnection;

        protected AbstractConnection() {
        }

        protected RepositoryConnection delegateConnection() {
            if (delegateConnection == null) {
                delegateConnection = delegate().getConnection();
            }
            return delegateConnection;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
         */
        public boolean ping( long time,
                             TimeUnit unit ) throws InterruptedException {
            return delegateConnection().ping(time, unit);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#getDefaultCachePolicy()
         */
        public CachePolicy getDefaultCachePolicy() {
            return delegateConnection().getDefaultCachePolicy();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#getSourceName()
         */
        public String getSourceName() {
            return delegate().getName();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#getXAResource()
         */
        public XAResource getXAResource() {
            return delegateConnection().getXAResource();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#close()
         */
        public void close() {
            if (delegateConnection != null) {
                try {
                    delegateConnection.close();
                } finally {
                    delegateConnection = null;
                }
            }
        }
    }

    /**
     * A {@link RepositoryConnection} implementation that calls the delegate processor in a background thread, allowing the
     * processing of the {@link FullTextSearchRequest} and {@link AccessQueryRequest} objects to be done in this thread and in
     * parallel with other requests.
     */
    @NotThreadSafe
    protected class ParallelConnection extends AbstractConnection {
        private final ExecutorService executorService;

        protected ParallelConnection( ExecutorService executorService ) {
            this.executorService = executorService;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
         *      org.modeshape.graph.request.Request)
         */
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            RequestProcessor searchProcessor = null;

            switch (request.getType()) {
                case ACCESS_QUERY:
                    AccessQueryRequest queryRequest = (AccessQueryRequest)request;
                    searchProcessor = searchEngine().createProcessor(context, null, true);
                    try {
                        searchProcessor.process(queryRequest);
                    } finally {
                        searchProcessor.close();
                    }
                    break;
                case FULL_TEXT_SEARCH:
                    FullTextSearchRequest searchRequest = (FullTextSearchRequest)request;
                    searchProcessor = searchEngine().createProcessor(context, null, true);
                    try {
                        searchProcessor.process(searchRequest);
                    } finally {
                        searchProcessor.close();
                    }
                    break;
                case COMPOSITE:
                    CompositeRequest composite = (CompositeRequest)request;
                    CompositeRequestChannel channel = null;
                    try {
                        for (Request nested : composite) {
                            switch (nested.getType()) {
                                case ACCESS_QUERY:
                                    queryRequest = (AccessQueryRequest)request;
                                    if (searchProcessor == null) {
                                        searchProcessor = searchEngine().createProcessor(context, null, true);
                                    }
                                    searchProcessor.process(queryRequest);
                                    break;
                                case FULL_TEXT_SEARCH:
                                    searchRequest = (FullTextSearchRequest)request;
                                    if (searchProcessor == null) {
                                        searchProcessor = searchEngine().createProcessor(context, null, true);
                                    }
                                    searchProcessor.process(searchRequest);
                                    break;
                                default:
                                    // Delegate to the channel ...
                                    if (channel == null) {
                                        // Create a connection factory that always returns the delegate connection ...
                                        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
                                            /**
                                             * {@inheritDoc}
                                             * 
                                             * @see org.modeshape.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
                                             */
                                            public RepositoryConnection createConnection( String sourceName )
                                                throws RepositorySourceException {
                                                assert delegate().getName().equals(sourceName);
                                                return delegateConnection();
                                            }
                                        };
                                        channel = new CompositeRequestChannel(delegate().getName());
                                        channel.start(executorService, context, connectionFactory);
                                    }
                                    channel.add(request);

                            }
                        }
                    } finally {
                        try {
                            if (searchProcessor != null) {
                                searchProcessor.close();
                            }
                        } finally {
                            if (channel != null) {
                                try {
                                    channel.close();
                                } finally {
                                    try {
                                        channel.await();
                                    } catch (CancellationException err) {
                                        composite.cancel();
                                    } catch (ExecutionException err) {
                                        composite.setError(err);
                                    } catch (InterruptedException err) {
                                        // Reset the thread ...
                                        Thread.interrupted();
                                        // Then log the message ...
                                        I18n msg = GraphI18n.interruptedWhileClosingChannel;
                                        Logger.getLogger(getClass()).warn(err, msg, delegate().getName());
                                        composite.setError(err);
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    delegateConnection().execute(context, request);

            }
        }
    }

    /**
     * A {@link RepositoryConnection} implementation that calls the delegate processor in the calling thread.
     */
    @NotThreadSafe
    protected class SynchronousConnection extends AbstractConnection {

        protected SynchronousConnection() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
         *      org.modeshape.graph.request.Request)
         */
        public void execute( final ExecutionContext context,
                             final Request request ) throws RepositorySourceException {
            RequestProcessor searchProcessor = null;

            switch (request.getType()) {
                case ACCESS_QUERY:
                    AccessQueryRequest queryRequest = (AccessQueryRequest)request;
                    searchProcessor = searchEngine().createProcessor(context, null, true);
                    try {
                        searchProcessor.process(queryRequest);
                    } finally {
                        searchProcessor.close();
                    }
                    break;
                case FULL_TEXT_SEARCH:
                    FullTextSearchRequest searchRequest = (FullTextSearchRequest)request;
                    searchProcessor = searchEngine().createProcessor(context, null, true);
                    try {
                        searchProcessor.process(searchRequest);
                    } finally {
                        searchProcessor.close();
                    }
                    break;
                case COMPOSITE:
                    CompositeRequest composite = (CompositeRequest)request;
                    List<Request> delegateRequests = null;
                    try {
                        Request delegateRequest = composite;
                        for (Request nested : composite) {
                            switch (nested.getType()) {
                                case ACCESS_QUERY:
                                    queryRequest = (AccessQueryRequest)request;
                                    if (searchProcessor == null) {
                                        searchProcessor = searchEngine().createProcessor(context, null, true);
                                    }
                                    searchProcessor.process(queryRequest);
                                    delegateRequest = null;
                                    break;
                                case FULL_TEXT_SEARCH:
                                    searchRequest = (FullTextSearchRequest)request;
                                    if (searchProcessor == null) {
                                        searchProcessor = searchEngine().createProcessor(context, null, true);
                                    }
                                    searchProcessor.process(searchRequest);
                                    delegateRequest = null;
                                    break;
                                default:
                                    // Delegate the request ...
                                    if (delegateRequests == null) {
                                        delegateRequests = new LinkedList<Request>();
                                    }
                                    delegateRequests.add(request);

                            }
                        }
                        if (delegateRequest == null) {
                            // Then there was at least one query or search request ...
                            if (delegateRequests != null) {
                                // There was other requests ...
                                assert !delegateRequests.isEmpty();
                                delegateRequest = CompositeRequest.with(delegateRequests);
                                delegateConnection().execute(context, delegateRequest);
                            } else {
                                // There were no other requests in the composite other than the search and/or query requests ...
                                // So nothing to do ...
                            }
                        } else {
                            // There were no search or query requests, so delegate the orginal composite request ...
                            delegateConnection().execute(context, request);
                        }
                    } finally {
                        if (searchProcessor != null) {
                            searchProcessor.close();
                        }
                    }
                    break;
                default:
                    // Just a single, non-query and non-search request ...
                    delegateConnection().execute(context, request);

            }
        }
    }
}

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
package org.modeshape.graph.connector;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A simple connection to a mock repository, where the connection accepts requests but does very little with them (other than
 * record that requests were processed in the Queue supplied in the constructor).
 */
public class MockRepositoryConnection implements RepositoryConnection {

    private final String sourceName;
    private final Queue<Request> processed;

    /**
     * Create a new connection
     * 
     * @param sourceName the name of the source that this connection represents; may not be null
     */
    public MockRepositoryConnection( String sourceName ) {
        this(sourceName, new LinkedList<Request>());
    }

    /**
     * Create a new connection
     * 
     * @param sourceName the name of the source that this connection represents; may not be null
     * @param processed a queue into which should be placed all requests that were {@link #execute(ExecutionContext, Request)
     *        executed}; may be null
     */
    public MockRepositoryConnection( String sourceName,
                                     Queue<Request> processed ) {
        assert sourceName != null;
        this.sourceName = sourceName;
        this.processed = processed != null ? processed : new LinkedList<Request>();
    }

    /**
     * Get the list of requests that have been processed.
     * 
     * @return the queue of processed requests; never null
     */
    public Queue<Request> getProcessedRequests() {
        return processed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#close()
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        RequestProcessor processor = new MockRepositoryRequestProcessor(sourceName, context, processed);
        try {
            processor.process(request);
        } finally {
            processor.close();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#getXAResource()
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

}

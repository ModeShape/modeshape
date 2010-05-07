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

import static org.mockito.Mockito.mock;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.request.Request;

/**
 * A simple {@link RepositorySource} that simulates an imaginary source with a built-in delay mechanism.
 */
@ThreadSafe
public class TimeDelayingRepositorySource implements RepositorySource {

    /**
     */
    private static final long serialVersionUID = -2756725117087437347L;
    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    protected static final RepositorySourceCapabilities CAPABILITIES = new RepositorySourceCapabilities(true, true);

    private String name;
    private final AtomicInteger connectionsOpenedCount = new AtomicInteger(0);
    private final AtomicInteger connectionsClosedCount = new AtomicInteger(0);
    private final Set<Connection> openConnections = new CopyOnWriteArraySet<Connection>();
    private final AtomicLong loadCount = new AtomicLong(0);
    private final AtomicLong loadDelay = new AtomicLong(0);
    private final AtomicLong pingCount = new AtomicLong(0);
    private final AtomicLong pingDelay = new AtomicLong(0);
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private CachePolicy defaultCachePolicy;
    private transient RepositoryContext repositoryContext;

    public TimeDelayingRepositorySource( String identifier ) {
        super();
        this.name = identifier;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the identifier
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    public void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * @return loadCount
     */
    public long getTotalExecuteCount() {
        return this.loadCount.get();
    }

    /**
     * @return pingCount
     */
    public long getTotalPingCount() {
        return this.pingCount.get();
    }

    /**
     * @return loadDelay
     */
    public long getConnectionExecuteDelayInMillis() {
        return this.loadDelay.get();
    }

    public void setConnectionExecuteDelay( long time,
                                           TimeUnit unit ) {
        this.loadDelay.set(unit.toMillis(time));
    }

    /**
     * @return pingDelay
     */
    public long getConnectionPingDelayInMillis() {
        return this.pingDelay.get();
    }

    public void setConnectionPingDelay( long time,
                                        TimeUnit unit ) {
        this.pingDelay.set(unit.toMillis(time));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        int connectionNumber = this.connectionsOpenedCount.incrementAndGet();
        String connectionName = "Connection " + connectionNumber;
        XAResource xaResource = newXaResource(connectionName);
        Connection c = newConnection(connectionName, xaResource);
        this.openConnections.add(c);
        return c;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
        // Do not do anything, since when the connections are closed they'll be removed from the 'openConnections' set
    }

    /**
     * Factory method that can be overridden by subclasses. This method implementation simply creates a mock {@link XAResource}.
     * 
     * @param connectionName the name of the connection
     * @return the XAResource, or null if this source does not support XA
     */
    protected XAResource newXaResource( String connectionName ) {
        return mock(XAResource.class);
    }

    /**
     * Factory method that can be overridden by subclasses. This method implementation creates a new {@link Connection} that uses
     * standard bean properties for the various delays and counts.
     * 
     * @param connectionName
     * @param xaResource
     * @return a new Connection
     * @throws RepositorySourceException
     */
    protected Connection newConnection( String connectionName,
                                        XAResource xaResource ) throws RepositorySourceException {
        Connection c = new Connection(connectionName, this.loadDelay.get(), this.pingDelay.get());
        c.setXaResource(xaResource);
        return c;
    }

    protected void close( Connection conn ) {
        if (conn != null && this.openConnections.remove(conn)) {
            this.connectionsClosedCount.incrementAndGet();
            this.loadCount.addAndGet(conn.getLoadCount());
            this.pingCount.addAndGet(conn.getPingCount());
        }
    }

    public int getOpenConnectionCount() {
        return this.openConnections.size();
    }

    public int getTotalConnectionsCreated() {
        return this.connectionsOpenedCount.get();
    }

    public int getTotalConnectionsClosed() {
        return this.connectionsClosedCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    public class Connection implements RepositoryConnection {

        private final String connectionName;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicBoolean loadResponse = new AtomicBoolean(true);
        private final AtomicBoolean pingResponse = new AtomicBoolean(true);
        private final AtomicLong closeCount = new AtomicLong(0);
        private final AtomicLong loadCount = new AtomicLong(0);
        private final AtomicLong loadDelay;
        private final AtomicLong pingCount = new AtomicLong(0);
        private final AtomicLong pingDelay;
        private final AtomicReference<XAResource> xaResource = new AtomicReference<XAResource>();

        protected Connection( String connectionName,
                              long loadDelay,
                              long pingDelay ) {
            assert connectionName != null && connectionName.trim().length() != 0;
            this.loadDelay = new AtomicLong(loadDelay);
            this.pingDelay = new AtomicLong(pingDelay);
            this.connectionName = connectionName;
        }

        public String getConnectionName() {
            return this.connectionName;
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            this.closeCount.incrementAndGet();
            this.closed.set(true);
            TimeDelayingRepositorySource.this.close(this);
        }

        /**
         * {@inheritDoc}
         */
        public String getSourceName() {
            return TimeDelayingRepositorySource.this.getName();
        }

        /**
         * {@inheritDoc}
         */
        public CachePolicy getDefaultCachePolicy() {
            return TimeDelayingRepositorySource.this.getDefaultCachePolicy();
        }

        /**
         * {@inheritDoc}
         */
        public XAResource getXAResource() {
            return this.xaResource.get();
        }

        public void setXaResource( XAResource xaResource ) {
            this.xaResource.set(xaResource);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
         *      org.modeshape.graph.request.Request)
         */
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            long delay = this.loadDelay.get();
            if (delay > 0l) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    throw new RepositorySourceException(this.getSourceName(), e);
                }
            }
            this.loadCount.incrementAndGet();
        }

        public void setLoadResponse( boolean response ) {
            this.loadResponse.set(response);
        }

        public void setLoadDelay( long time,
                                  TimeUnit unit ) {
            this.loadDelay.set(unit.toMillis(time));
        }

        /**
         * {@inheritDoc}
         */
        public boolean ping( long time,
                             TimeUnit unit ) {
            try {
                Thread.sleep(this.pingDelay.get());
            } catch (InterruptedException e) {
                Thread.interrupted();
                return false;
            }
            return this.pingResponse.get();
        }

        public void setPingResponse( boolean pingResponse ) {
            this.pingResponse.set(pingResponse);
        }

        public void setPingDelay( long time,
                                  TimeUnit unit ) {
            this.pingDelay.set(unit.toMillis(time));
        }

        public long getPingCount() {
            return this.pingCount.get();
        }

        public long getLoadCount() {
            return this.loadCount.get();
        }

        public long getCloseCount() {
            return this.closeCount.get();
        }

    }

}

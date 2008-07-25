/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.connection;

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
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.commands.GraphCommand;

/**
 * A simple {@link RepositorySource} that simulates an imaginary source with a built-in delay mechanism.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class TimeDelayingRepositorySource extends AbstractRepositorySource {

    /**
     */
    private static final long serialVersionUID = -2756725117087437347L;
    private String name;
    private final AtomicInteger connectionsOpenedCount = new AtomicInteger(0);
    private final AtomicInteger connectionsClosedCount = new AtomicInteger(0);
    private final Set<Connection> openConnections = new CopyOnWriteArraySet<Connection>();
    private final AtomicLong loadCount = new AtomicLong(0);
    private final AtomicLong loadDelay = new AtomicLong(0);
    private final AtomicLong pingCount = new AtomicLong(0);
    private final AtomicLong pingDelay = new AtomicLong(0);
    private CachePolicy defaultCachePolicy;

    public TimeDelayingRepositorySource( String identifier ) {
        super();
        this.name = identifier;
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
     * @see org.jboss.dna.spi.graph.connection.AbstractRepositorySource#createConnection()
     */
    @Override
    protected RepositoryConnection createConnection() throws RepositorySourceException {
        int connectionNumber = this.connectionsOpenedCount.incrementAndGet();
        String connectionName = "Connection " + connectionNumber;
        XAResource xaResource = newXaResource(connectionName);
        Connection c = newConnection(connectionName, xaResource);
        this.openConnections.add(c);
        return c;
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
         */
        public void execute( ExecutionEnvironment env,
                             GraphCommand... commands ) throws InterruptedException {
            long delay = this.loadDelay.get();
            if (delay > 0l) Thread.sleep(delay);
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
                             TimeUnit unit ) throws InterruptedException {
            Thread.sleep(this.pingDelay.get());
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

        /**
         * {@inheritDoc}
         */
        public void setListener( RepositorySourceListener listener ) {
        }

    }

}

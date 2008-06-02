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
import org.jmock.Mockery;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class MockRepositorySource implements RepositorySource {

    private final String identifier;
    private final AtomicInteger retryLimit = new AtomicInteger(0);
    private final Mockery context;
    private final AtomicInteger connectionsOpenedCount = new AtomicInteger(0);
    private final AtomicInteger connectionsClosedCount = new AtomicInteger(0);
    private final Set<Connection> openConnections = new CopyOnWriteArraySet<Connection>();
    private CachePolicy defaultCachePolicy;

    public MockRepositorySource( String identifier, Mockery context ) {
        this.identifier = identifier;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.identifier;
    }

    /**
     * {@inheritDoc}
     */
    public int getRetryLimit() {
        return this.retryLimit.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setRetryLimit( int limit ) {
        this.retryLimit.set(limit);
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    public void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * {@inheritDoc}
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        int connectionNumber = this.connectionsOpenedCount.incrementAndGet();
        String connectionName = "Connection " + connectionNumber;
        XAResource xaResource = context != null ? context.mock(XAResource.class, connectionName) : null;
        Connection c = newConnection(connectionName, xaResource);
        this.openConnections.add(c);
        return c;
    }

    protected Connection newConnection( String connectionName, XAResource xaResource ) throws RepositorySourceException {
        Connection c = new Connection(connectionName);
        c.setXaResource(xaResource);
        return c;
    }

    protected void close( Connection conn ) {
        if (conn != null && this.openConnections.remove(conn)) {
            this.connectionsClosedCount.incrementAndGet();
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
        private final AtomicLong loadDelay = new AtomicLong(0);
        private final AtomicLong pingCount = new AtomicLong(0);
        private final AtomicLong pingDelay = new AtomicLong(0);
        private final AtomicReference<XAResource> xaResource = new AtomicReference<XAResource>();

        protected Connection( String connectionName ) {
            assert connectionName != null && connectionName.trim().length() != 0;
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
            MockRepositorySource.this.close(this);
        }

        /**
         * {@inheritDoc}
         */
        public String getSourceName() {
            return MockRepositorySource.this.getName();
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
        public void execute( ExecutionEnvironment env, GraphCommand... commands ) throws RepositorySourceException, InterruptedException {
            long delay = this.loadDelay.get();
            if (delay > 0l) Thread.sleep(delay);
            this.loadCount.incrementAndGet();
        }

        public void setLoadResponse( boolean response ) {
            this.loadResponse.set(response);
        }

        public void setLoadDelay( long time, TimeUnit unit ) {
            this.loadDelay.set(unit.toMillis(time));
        }

        /**
         * {@inheritDoc}
         */
        public boolean ping( long time, TimeUnit unit ) throws InterruptedException {
            Thread.sleep(this.pingDelay.get());
            return this.pingResponse.get();
        }

        public void setPingResponse( boolean pingResponse ) {
            this.pingResponse.set(pingResponse);
        }

        public void setPingDelay( long time, TimeUnit unit ) {
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

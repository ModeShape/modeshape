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
package org.jboss.dna.spi.connector;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract implementation of {@link RepositorySource} that may serve as a foundation for most implementations, since it
 * automatically manages the {@link RepositoryConnection connections} using an internal {@link RepositoryConnectionPool pool}.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public abstract class AbstractRepositorySource implements RepositorySource {

    /**
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private RepositoryConnectionPool.ConnectionFactory connectionFactory;
    private transient RepositoryConnectionPool connections;

    /**
     * Create a new instance of the repository source. This constructor calls relying upon the {@link #createConnectionFactory()}
     * method, which creation of the {@link RepositoryConnection connections}.
     */
    protected AbstractRepositorySource() {
        this.connectionFactory = createConnectionFactory();
        assert this.connectionFactory != null;
        this.connections = new RepositoryConnectionPool(this.connectionFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#isRunning()
     */
    public boolean isRunning() {
        return this.connections.isRunning();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#isShutdown()
     */
    public boolean isShutdown() {
        return this.connections.isShutdown();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException, InterruptedException {
        return this.connections.getConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#shutdown()
     */
    public void shutdown() {
        this.connections.shutdown();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#shutdownNow()
     */
    public void shutdownNow() {
        this.connections.shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#isTerminated()
     */
    public boolean isTerminated() {
        return this.connections.isTerminated();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#isTerminating()
     */
    public boolean isTerminating() {
        return this.connections.isTerminating();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.connector.RepositorySource#awaitTermination(long, java.util.concurrent.TimeUnit)
     */
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        return this.connections.awaitTermination(timeout, unit);
    }

    private void writeObject( java.io.ObjectOutputStream out ) throws IOException {
        out.writeInt(this.getRetryLimit());
        // Write out the pool's state, since the pool is not serializable ...
        out.writeLong(connections.getKeepAliveTime(TimeUnit.NANOSECONDS));
        out.writeInt(connections.getCorePoolSize());
        out.writeInt(connections.getMaximumPoolSize());
        out.writeInt(connections.getMaxFailedAttemptsBeforeError());
        out.writeBoolean(connections.getValidateConnectionBeforeUse());
        out.writeLong(connections.getPingTimeoutInNanos());
    }

    private void readObject( java.io.ObjectInputStream in ) throws IOException {
        setRetryLimit(in.readInt());
        connectionFactory = createConnectionFactory();
        // Read the pool state parameters ...
        final long keepAliveTimeInNanos = in.readLong();
        final int corePoolSize = in.readInt();
        final int maxPoolSize = in.readInt();
        final int maxFailedAttemptsBeforeError = in.readInt();
        final boolean validateConnections = in.readBoolean();
        final long pingTimeoutInNanos = in.readLong();
        // Create a new pool and set it's parameters ...
        connections = new RepositoryConnectionPool(connectionFactory, corePoolSize, maxPoolSize, keepAliveTimeInNanos,
                                                   TimeUnit.NANOSECONDS);
        connections.setMaxFailedAttemptsBeforeError(maxFailedAttemptsBeforeError);
        connections.setValidateConnectionBeforeUse(validateConnections);
        connections.setPingTimeout(pingTimeoutInNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Create a connection factory that should be used. The default implementation returns a factory that delegates to the
     * {@link #createConnection()} method, which should be overridden.
     * 
     * @return the connection factory; never null
     */
    protected RepositoryConnectionPool.ConnectionFactory createConnectionFactory() {
        return new RepositoryConnectionPool.ConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.spi.connector.RepositoryConnectionPool.ConnectionFactory#createConnection()
             */
            public RepositoryConnection createConnection() throws RepositorySourceException, InterruptedException {
                return AbstractRepositorySource.this.createConnection();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.spi.connector.RepositoryConnectionPool#getSourceName()
             */
            public String getSourceName() {
                return AbstractRepositorySource.this.getName();
            }
        };
    }

    /**
     * Method to create a new {@link RepositoryConnection} instance. This method is called by the
     * {@link RepositoryConnectionPool.ConnectionFactory} returned by the default implementation of
     * {@link #createConnectionFactory()}. If the {@link #createConnectionFactory()} method is overridden, this method will not be
     * called.
     * 
     * @return the new connection
     * @throws RepositorySourceException
     * @throws InterruptedException
     */
    protected abstract RepositoryConnection createConnection() throws RepositorySourceException, InterruptedException;

}

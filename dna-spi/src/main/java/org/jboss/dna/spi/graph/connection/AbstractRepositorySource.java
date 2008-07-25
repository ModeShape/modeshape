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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract implementation of {@link RepositorySource} that may serve as a foundation for most implementations, since it
 * automatically manages the {@link RepositoryConnection connections} using an internal {@link ManagedRepositoryConnectionFactory pool}.
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
    private final RepositoryConnectionPool connections;
    private final RepositoryConnectionFactory connectionFactory;

    /**
     * Create a new instance of the repository source, relying upon the {@link #createConnection()} method to do the actual
     * creation of the {@link RepositoryConnection connections}.
     */
    protected AbstractRepositorySource() {
        this(null);
    }

    /**
     * Create a new instance of the repository source, relying upon the supplied factory to do the actual creation of the
     * {@link RepositoryConnection connections}. If the supplied factory is null, then this class will use the
     * {@link #createConnection()} method to do the actual creation of the {@link RepositoryConnection connections}.
     * 
     * @param factory the connection factory that creates the connections, or null if the {@link #createConnection()} method
     *        should be used to create connections
     */
    protected AbstractRepositorySource( RepositoryConnectionFactory factory ) {
        this.connectionFactory = factory != null ? factory : new ConnectionFactory();
        this.connections = new RepositoryConnectionPool(this.connectionFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#isRunning()
     */
    public boolean isRunning() {
        return this.connections.isRunning();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#isShutdown()
     */
    public boolean isShutdown() {
        return this.connections.isShutdown();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException, InterruptedException {
        return this.connections.getConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#shutdown()
     */
    public void shutdown() {
        this.connections.shutdown();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#shutdownNow()
     */
    public void shutdownNow() {
        this.connections.shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#isTerminated()
     */
    public boolean isTerminated() {
        return this.connections.isTerminated();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#isTerminating()
     */
    public boolean isTerminating() {
        return this.connections.isTerminating();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.ManagedRepositoryConnectionFactory#awaitTermination(long, java.util.concurrent.TimeUnit)
     */
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        return this.connections.awaitTermination(timeout, unit);
    }

    /**
     * Method to create a new {@link RepositoryConnection} instance. This method is called only when this instance was
     * {@link #AbstractRepositorySource(RepositoryConnectionFactory) constructed} with a null {@link RepositoryConnectionFactory}
     * reference. This makes it easy for subclasses to simply override this method can not be required to implement a separate
     * connection factory.
     * 
     * @return the new connection
     * @throws RepositorySourceException
     * @throws InterruptedException
     */
    protected RepositoryConnection createConnection() throws RepositorySourceException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Class that is used by the {@link AbstractRepositorySource} and it's pool to create connections as needed, by delegating to
     * the {@link AbstractRepositorySource}'s {@link AbstractRepositorySource#createConnection()} method.
     * 
     * @author Randall Hauch
     */
    protected class ConnectionFactory implements RepositoryConnectionFactory {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory#getConnection()
         */
        public RepositoryConnection getConnection() throws RepositorySourceException, InterruptedException {
            return AbstractRepositorySource.this.createConnection();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory#getName()
         */
        public String getName() {
            return AbstractRepositorySource.this.getName();
        }

    }
}

/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryConnectionPool;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.observe.ChangeObserver;
import org.jboss.dna.graph.observe.ChangeObservers;
import org.jboss.dna.graph.observe.Changes;
import org.jboss.dna.graph.observe.Observable;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.repository.mimetype.MimeTypeDetectors;
import org.jboss.dna.repository.service.AbstractServiceAdministrator;
import org.jboss.dna.repository.service.ServiceAdministrator;

/**
 * A library of {@link RepositorySource} instances and the {@link RepositoryConnectionPool} used to manage the connections for
 * each.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class RepositoryLibrary implements RepositoryConnectionFactory, Observable {

    /**
     * The administrative component for this service.
     * 
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.federationServiceName, State.STARTED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doStart( State fromState ) {
            super.doStart(fromState);
            RepositoryLibrary.this.start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            RepositoryLibrary.this.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return RepositoryLibrary.this.awaitTermination(timeout, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return RepositoryLibrary.this.isTerminated();
        }

    }

    private final MimeTypeDetectors mimeTypeDetectors = new MimeTypeDetectors();
    private final ServiceAdministrator administrator = new Administrator();
    private final ReadWriteLock sourcesLock = new ReentrantReadWriteLock();
    private final CopyOnWriteArrayList<RepositoryConnectionPool> pools = new CopyOnWriteArrayList<RepositoryConnectionPool>();
    private RepositoryConnectionFactory delegate;
    private final ExecutionContext executionContext;
    private final RepositoryContext repositoryContext;
    private final ObservationBus observationBus = new InMemoryObservationBus();

    /**
     * Create a new manager instance.
     */
    public RepositoryLibrary() {
        this(new ExecutionContext(), null);
    }

    /**
     * Create a new manager instance.
     * 
     * @param delegate the connection factory to which this instance should delegate in the event that a source is not found in
     *        this manager; may be null if there is no delegate
     */
    public RepositoryLibrary( RepositoryConnectionFactory delegate ) {
        this(new ExecutionContext(), delegate);
    }

    /**
     * Create a new manager instance.
     * 
     * @param executionContext the execution context, which can be used used by sources to create other {@link ExecutionContext}
     *        instances with different JAAS security contexts
     * @throws IllegalArgumentException if the <code>executionContextFactory</code> reference is null
     */
    public RepositoryLibrary( ExecutionContext executionContext ) {
        this(executionContext, null);
    }

    /**
     * Create a new manager instance.
     * 
     * @param executionContext the execution context, which can be used used by sources to create other {@link ExecutionContext}
     *        instances with different JAAS security contexts
     * @param delegate the connection factory to which this instance should delegate in the event that a source is not found in
     *        this manager; may be null if there is no delegate
     * @throws IllegalArgumentException if the <code>executionContextFactory</code> reference is null
     */
    public RepositoryLibrary( final ExecutionContext executionContext,
                              RepositoryConnectionFactory delegate ) {
        CheckArg.isNotNull(executionContext, "executionContext");
        this.delegate = delegate;
        this.executionContext = executionContext;
        final ObservationBus observationBus = this.observationBus;
        this.repositoryContext = new RepositoryContext() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryContext#getExecutionContext()
             */
            public ExecutionContext getExecutionContext() {
                return executionContext;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
             */
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return RepositoryLibrary.this;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryContext#getObserver()
             */
            public Observer getObserver() {
                return observationBus.hasObservers() ? observationBus : null;
            }

        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#register(org.jboss.dna.graph.observe.ChangeObserver)
     */
    public boolean register( ChangeObserver observer ) {
        return observationBus.register(observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#unregister(org.jboss.dna.graph.observe.ChangeObserver)
     */
    public boolean unregister( ChangeObserver observer ) {
        return observationBus.unregister(observer);
    }

    /**
     * @return executionContextFactory
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @return mimeTypeDetectors
     */
    public MimeTypeDetectors getMimeTypeDetectors() {
        return mimeTypeDetectors;
    }

    /**
     * Get the delegate connection factory.
     * 
     * @return the connection factory to which this instance should delegate in the event that a source is not found in this
     *         manager, or null if there is no delegate
     */
    public RepositoryConnectionFactory getDelegate() {
        return delegate;
    }

    /**
     * Set the delegate connection factory.
     * 
     * @param delegate the connection factory to which this instance should delegate in the event that a source is not found in
     *        this manager; may be null if there is no delegate
     */
    public void setDelegate( RepositoryConnectionFactory delegate ) {
        this.delegate = delegate;
    }

    /**
     * @return administrator
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Utility method called by the administrator.
     */
    protected void start() {
        // Do not establish connections to the pools; these will be established as needed

    }

    /**
     * Utility method called by the administrator.
     */
    protected void shutdown() {
        // Close all connections to the pools. This is done inside the pools write lock.
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools) {
                pool.shutdown();
            }
        } finally {
            this.sourcesLock.readLock().unlock();
        }
        // Remove all listeners ...
        this.observationBus.shutdown();
    }

    /**
     * Utility method called by the administrator.
     * 
     * @param timeout
     * @param unit
     * @return true if all pools were terminated in the supplied time (or were already terminated), or false if the timeout
     *         occurred before all the connections were closed
     * @throws InterruptedException
     */
    protected boolean awaitTermination( long timeout,
                                        TimeUnit unit ) throws InterruptedException {
        // Check whether all source pools are shut down. This is done inside the pools write lock.
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools) {
                if (!pool.awaitTermination(timeout, unit)) return false;
            }
            return true;
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Returns true if this federated repository is in the process of terminating after {@link ServiceAdministrator#shutdown()}
     * has been called on the {@link #getAdministrator() administrator}, but the federated repository has connections that have
     * not yet normally been {@link RepositoryConnection#close() closed}. This method may be useful for debugging. A return of
     * <tt>true</tt> reported a sufficient period after shutdown may indicate that connection users have ignored or suppressed
     * interruption, causing this repository not to properly terminate.
     * 
     * @return true if terminating but not yet terminated, or false otherwise
     * @see #isTerminated()
     */
    public boolean isTerminating() {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools) {
                if (pool.isTerminating()) return true;
            }
            return false;
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Return true if this federated repository has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     * @see #isTerminating()
     */
    public boolean isTerminated() {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools) {
                if (!pool.isTerminated()) return false;
            }
            return true;
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Get an unmodifiable collection of {@link RepositorySource} names.
     * 
     * @return the pools
     */
    public Collection<String> getSourceNames() {
        Set<String> sourceNames = new HashSet<String>();
        for (RepositoryConnectionPool pool : this.pools) {
            sourceNames.add(pool.getRepositorySource().getName());
        }
        return Collections.unmodifiableCollection(sourceNames);
    }

    /**
     * Get an unmodifiable collection of {@link RepositorySource} instances managed by this instance.
     * 
     * @return the pools
     */
    public Collection<RepositorySource> getSources() {
        List<RepositorySource> sources = new LinkedList<RepositorySource>();
        for (RepositoryConnectionPool pool : this.pools) {
            sources.add(pool.getRepositorySource());
        }
        return Collections.unmodifiableCollection(sources);
    }

    /**
     * Get the RepositorySource with the specified name managed by this instance.
     * 
     * @param sourceName the name of the source
     * @return the source, or null if no such source exists in this instance
     */
    public RepositorySource getSource( String sourceName ) {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool existingPool : this.pools) {
                RepositorySource source = existingPool.getRepositorySource();
                if (source.getName().equals(sourceName)) return source;
            }
        } finally {
            this.sourcesLock.readLock().unlock();
        }
        return null;
    }

    /**
     * Get the connection pool managing the {@link RepositorySource} with the specified name managed by this instance.
     * 
     * @param sourceName the name of the source
     * @return the pool, or null if no such pool exists in this instance
     */
    public RepositoryConnectionPool getConnectionPool( String sourceName ) {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool existingPool : this.pools) {
                RepositorySource source = existingPool.getRepositorySource();
                if (source.getName().equals(sourceName)) return existingPool;
            }
        } finally {
            this.sourcesLock.readLock().unlock();
        }
        return null;
    }

    /**
     * Add the supplied federated source. This method returns false if the source is null.
     * 
     * @param source the source to add
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     */
    public boolean addSource( RepositorySource source ) {
        if (source == null) return false;
        try {
            this.sourcesLock.writeLock().lock();
            for (RepositoryConnectionPool existingPool : this.pools) {
                if (existingPool.getRepositorySource().getName().equals(source.getName())) return false;
            }
            source.initialize(repositoryContext);
            RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
            this.pools.add(pool);
            return true;
        } finally {
            this.sourcesLock.writeLock().unlock();
        }
    }

    /**
     * Remove from this federated repository the supplied source (or a source with the same name as that supplied). This call
     * shuts down the connections in the source in an orderly fashion, allowing those connection currently in use to be used and
     * closed normally, but preventing further connections from being used.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param source the source to be removed
     * @param timeToAwait the amount of time to wait while all of the source's connections are closed, or non-positive if the call
     *        should not wait at all
     * @param unit the time unit to be used for <code>timeToAwait</code>
     * @return true if the source was removed, or false if the source was not a source for this repository.
     * @throws InterruptedException if the thread is interrupted while awaiting closing of the connections
     */
    public boolean removeSource( RepositorySource source,
                                 long timeToAwait,
                                 TimeUnit unit ) throws InterruptedException {
        // Use the name; don't use the object equality ...
        return removeSource(source.getName(), timeToAwait, unit) != null;
    }

    /**
     * Remove from this federated repository the source with the supplied name. This call shuts down the connections in the source
     * in an orderly fashion, allowing those connection currently in use to be used and closed normally, but preventing further
     * connections from being used.
     * 
     * @param name the name of the source to be removed
     * @param timeToAwait the amount of time to wait while all of the source's connections are closed, or non-positive if the call
     *        should not wait at all
     * @param unit the time unit to be used for <code>timeToAwait</code>
     * @return the source with the supplied name that was removed, or null if no existing source matching the supplied name could
     *         be found
     * @throws InterruptedException if the thread is interrupted while awaiting closing of the connections
     */
    public RepositorySource removeSource( String name,
                                          long timeToAwait,
                                          TimeUnit unit ) throws InterruptedException {
        try {
            this.sourcesLock.writeLock().lock();
            for (RepositoryConnectionPool existingPool : this.pools) {
                if (existingPool.getRepositorySource().getName().equals(name)) {
                    // Shut down the source ...
                    existingPool.shutdown();
                    if (timeToAwait > 0L) existingPool.awaitTermination(timeToAwait, unit);
                }
                return existingPool.getRepositorySource();
            }
        } finally {
            this.sourcesLock.writeLock().unlock();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
     */
    public RepositoryConnection createConnection( String sourceName ) {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool existingPool : this.pools) {
                RepositorySource source = existingPool.getRepositorySource();
                if (source.getName().equals(sourceName)) return existingPool.getConnection();
            }
            RepositoryConnectionFactory delegate = this.delegate;
            if (delegate != null) {
                return delegate.createConnection(sourceName);
            }
        } finally {
            this.sourcesLock.readLock().unlock();
        }
        return null;
    }

    protected interface ObservationBus extends Observable, Observer {
        boolean hasObservers();

        void shutdown();
    }

    protected class InMemoryObservationBus implements ObservationBus {
        private final ChangeObservers observers = new ChangeObservers();

        protected InMemoryObservationBus() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.observe.Observable#register(org.jboss.dna.graph.observe.ChangeObserver)
         */
        public boolean register( ChangeObserver observer ) {
            return observers.register(observer);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.observe.Observable#unregister(org.jboss.dna.graph.observe.ChangeObserver)
         */
        public boolean unregister( ChangeObserver observer ) {
            return observers.unregister(observer);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.observe.Observer#notify(org.jboss.dna.graph.observe.Changes)
         */
        public void notify( Changes changes ) {
            if (changes != null) {
                // Broadcast the changes to the registered observers ...
                observers.broadcast(changes);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.RepositoryLibrary.ObservationBus#hasObservers()
         */
        public boolean hasObservers() {
            return !observers.isEmpty();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.RepositoryLibrary.ObservationBus#shutdown()
         */
        public void shutdown() {
            observers.shutdown();
        }
    }
}

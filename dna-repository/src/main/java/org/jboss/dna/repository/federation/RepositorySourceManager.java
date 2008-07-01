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
package org.jboss.dna.repository.federation;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionPool;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * @author Randall Hauch
 */
public class RepositorySourceManager implements RepositoryConnectionFactories {

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
            RepositorySourceManager.this.start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            RepositorySourceManager.this.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return RepositorySourceManager.this.awaitTermination(timeout, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return RepositorySourceManager.this.isTerminated();
        }

    }

    private final ServiceAdministrator administrator = new Administrator();
    private final Lock sourcesWriteLock = new ReentrantLock();
    private final ConcurrentMap<String, RepositorySource> sources = new ConcurrentHashMap<String, RepositorySource>();
    private RepositoryConnectionFactories delegate;

    /**
     * Create a new manager instance.
     * 
     * @param delegate the factories object that this instance should delegate to in the event that a source is not found in this
     *        manager; may be null if there is no delegate
     */
    public RepositorySourceManager( RepositoryConnectionFactories delegate ) {
        this.delegate = delegate;
    }

    /**
     * @return delegate
     */
    public RepositoryConnectionFactories getDelegate() {
        return delegate;
    }

    /**
     * @param delegate Sets delegate to the specified value.
     */
    public void setDelegate( RepositoryConnectionFactories delegate ) {
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
        // Do not establish connections to the sources; these will be established as needed

    }

    /**
     * Utility method called by the administrator.
     */
    protected void shutdown() {
        // Close all connections to the sources. This is done inside the sources write lock.
        try {
            this.sourcesWriteLock.lock();
            for (RepositorySource source : this.sources.values()) {
                if (source instanceof RepositoryConnectionPool) {
                    RepositoryConnectionPool pool = (RepositoryConnectionPool)source;
                    pool.shutdown();
                }
            }
        } finally {
            this.sourcesWriteLock.unlock();
        }
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
        // Check whether all source pools are shut down. This is done inside the sources write lock.
        try {
            this.sourcesWriteLock.lock();
            for (RepositorySource source : this.sources.values()) {
                if (source instanceof RepositoryConnectionPool) {
                    RepositoryConnectionPool pool = (RepositoryConnectionPool)source;
                    if (!pool.awaitTermination(timeout, unit)) {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            this.sourcesWriteLock.unlock();
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
            this.sourcesWriteLock.lock();
            for (RepositorySource source : this.sources.values()) {
                if (source instanceof RepositoryConnectionPool) {
                    RepositoryConnectionPool pool = (RepositoryConnectionPool)source;
                    if (pool.isTerminating()) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            this.sourcesWriteLock.unlock();
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
            this.sourcesWriteLock.lock();
            for (RepositorySource source : this.sources.values()) {
                if (source instanceof RepositoryConnectionPool) {
                    RepositoryConnectionPool pool = (RepositoryConnectionPool)source;
                    if (!pool.isTerminated()) {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            this.sourcesWriteLock.unlock();
        }
    }

    /**
     * Get an unmodifiable collection of {@link RepositorySource federated sources}.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @return the sources
     */
    public Collection<RepositorySource> getSources() {
        return Collections.unmodifiableCollection(this.sources.values());
    }

    /**
     * Add the supplied federated source. This method returns false if the source is null.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param source the source to add
     * @param force true if the valid source should be added even if there is an existing source with the supplied name
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     */
    public boolean addSource( RepositorySource source,
                              boolean force ) {
        if (source == null) return false;
        try {
            this.sourcesWriteLock.lock();
            for (RepositorySource existingSource : this.sources.values()) {
                if (existingSource.getName().equals(source.getName())) return false;
            }
            if (force) {
                this.sources.put(source.getName(), source);
            } else {
                this.sources.putIfAbsent(source.getName(), source);
            }
        } finally {
            this.sourcesWriteLock.unlock();
        }
        return true;
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
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
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
            this.sourcesWriteLock.lock();
            RepositorySource existingSource = this.sources.remove(name);
            if (existingSource != null) {
                // Shut down the connection pool if it is one ...
                if (existingSource instanceof RepositoryConnectionPool) {
                    RepositoryConnectionPool pool = (RepositoryConnectionPool)existingSource;
                    pool.shutdown();
                    if (timeToAwait > 0l) pool.awaitTermination(timeToAwait, unit);
                }
            }
            return existingSource;
        } finally {
            this.sourcesWriteLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.federation.RepositoryConnectionFactories#getConnectionFactory(java.lang.String)
     */
    public RepositoryConnectionFactory getConnectionFactory( String sourceName ) {
        RepositoryConnectionFactory result = this.sources.get(sourceName);
        if (result == null) {
            RepositoryConnectionFactories delegate = this.delegate;
            if (delegate != null) {
                result = delegate.getConnectionFactory(sourceName);
            }
        }
        return result;
    }

}

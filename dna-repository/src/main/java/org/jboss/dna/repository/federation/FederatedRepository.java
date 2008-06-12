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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionPool;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * The component in the {@link FederationService} that represents a single federated repository. The federated repository manages
 * a set of {@link FederatedSource federated sources}, and provides the logic of interacting with those sources and presenting a
 * single unified graph.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepository {

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
            FederatedRepository.this.startRepository();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            FederatedRepository.this.shutdownRepository();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return FederatedRepository.this.awaitTermination(timeout, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return FederatedRepository.this.isTerminated();
        }

    }

    private final ServiceAdministrator administrator = new Administrator();
    private final String name;
    private final FederationService service;
    private final Lock sourcesWriteLock = new ReentrantLock();
    private final List<FederatedSource> sources = new CopyOnWriteArrayList<FederatedSource>();
    private final CopyOnWriteArrayList<RepositorySourceListener> listeners = new CopyOnWriteArrayList<RepositorySourceListener>();
    private CachePolicy defaultCachePolicy;

    /**
     * Create a federated repository instance, as managed by the supplied {@link FederationService}.
     * 
     * @param service the federation service that is managing this instance
     * @param name the name of the repository
     * @throws IllegalArgumentException if the service is null or the name is null or blank
     */
    public FederatedRepository( FederationService service,
                                String name ) {
        ArgCheck.isNotNull(service, "service");
        ArgCheck.isNotEmpty(name, "name");
        this.name = name;
        this.service = service;
    }

    /**
     * @return service
     */
    protected FederationService getService() {
        return this.service;
    }

    /**
     * Get the name of this repository
     * 
     * @return name
     */
    public String getName() {
        return this.name;
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
    protected void startRepository() {
        // Do not establish connections to the sources; these will be established as needed

    }

    /**
     * Utility method called by the administrator.
     */
    protected void shutdownRepository() {
        // Close all connections to the sources. This is done inside the sources write lock.
        try {
            this.sourcesWriteLock.lock();
            for (FederatedSource source : this.sources) {
                source.getConnectionPool().shutdown();
            }
        } finally {
            this.sourcesWriteLock.unlock();
        }
        // Connections to this repository check before doing anything with this, so just remove it from the service ...
        this.service.removeRepository(this);
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
            for (FederatedSource source : this.sources) {
                if (!source.getConnectionPool().awaitTermination(timeout, unit)) {
                    return false;
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
            for (FederatedSource source : this.sources) {
                if (source.getConnectionPool().isTerminating()) {
                    return true;
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
            for (FederatedSource source : this.sources) {
                if (!source.getConnectionPool().isTerminated()) {
                    return false;
                }
            }
            return true;
        } finally {
            this.sourcesWriteLock.unlock();
        }
    }

    /**
     * Get an unmodifiable collection of {@link FederatedSource federated sources}.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @return the sources
     */
    public List<FederatedSource> getSources() {
        return Collections.unmodifiableList(this.sources);
    }

    /**
     * Add the supplied federated source. This method returns false if the source is null.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param source the source to add
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     */
    public boolean addSource( FederatedSource source ) {
        if (source == null) return false;
        try {
            this.sourcesWriteLock.lock();
            for (FederatedSource existingSource : this.sources) {
                if (existingSource.getName().equals(source.getName())) return false;
            }
            this.sources.add(source);
        } finally {
            this.sourcesWriteLock.unlock();
        }
        return true;
    }

    /**
     * Add the supplied federated source. This method returns false if the source is null.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param source the source to add
     * @param index the index at which the source should be added
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public boolean addSource( FederatedSource source,
                              int index ) {
        if (source == null) return false;
        try {
            this.sourcesWriteLock.lock();
            for (FederatedSource existingSource : this.sources) {
                if (existingSource.getName().equals(source.getName())) return false;
            }
            this.sources.add(index, source);
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
    public boolean removeSource( FederatedSource source,
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
    public FederatedSource removeSource( String name,
                                         long timeToAwait,
                                         TimeUnit unit ) throws InterruptedException {
        try {
            this.sourcesWriteLock.lock();
            for (FederatedSource existingSource : this.sources) {
                if (existingSource.getName().equals(name)) {
                    boolean removed = this.sources.remove(existingSource);
                    assert removed;
                    // Shut down the connection pool for the source ...
                    RepositoryConnectionPool pool = existingSource.getConnectionPool();
                    pool.shutdown();
                    if (timeToAwait > 0l) pool.awaitTermination(timeToAwait, unit);
                    return existingSource;
                }
            }
        } finally {
            this.sourcesWriteLock.unlock();
        }
        return null;
    }

    /**
     * Add a listener that is to receive notifications to changes to content within this repository. This method does nothing if
     * the supplied listener is null.
     * 
     * @param listener the new listener
     * @return true if the listener was added, or false if the listener was not added (if reference is null, or if non-null
     *         listener is already an existing listener)
     */
    public boolean addListener( RepositorySourceListener listener ) {
        if (listener == null) return false;
        return this.listeners.addIfAbsent(listener);
    }

    /**
     * Remove the supplied listener. This method does nothing if the supplied listener is null.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, or false if the listener was not registered
     */
    public boolean removeListener( RepositorySourceListener listener ) {
        if (listener == null) return false;
        return this.listeners.remove(listener);
    }

    /**
     * Get the list of listeners, which is the actual list used by the repository.
     * 
     * @return the listeners
     */
    public List<RepositorySourceListener> getListeners() {
        return this.listeners;
    }

    /**
     * Authenticate the supplied username with the supplied credentials, and return whether authentication was successful.
     * 
     * @param username the username
     * @param credentials the credentials
     * @return true if authentication succeeded, or false otherwise
     */
    public boolean authenticate( String username,
                                 Object credentials ) {
        return true;
    }

    /**
     * Get the default cache policy for the repository with the supplied name
     * 
     * @return the default cache policy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * Set the default cache policy for the federated repository.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param defaultCachePolicy Sets defaultCachePolicy to the specified value.
     */
    public void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        ArgCheck.isNotNull(defaultCachePolicy, "defaultCachePolicy");
        this.defaultCachePolicy = defaultCachePolicy;
    }

}

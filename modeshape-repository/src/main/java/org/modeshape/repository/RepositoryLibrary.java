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
package org.modeshape.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryConnectionPool;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.observe.Observable;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Path;
import org.modeshape.repository.service.AbstractServiceAdministrator;
import org.modeshape.repository.service.ServiceAdministrator;

/**
 * A library of {@link RepositorySource} instances and the {@link RepositoryConnectionPool} used to manage the connections for
 * each.
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
            super(RepositoryI18n.repositoryServiceName, State.STARTED);
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

    private final ServiceAdministrator administrator = new Administrator();
    private final ReadWriteLock sourcesLock = new ReentrantReadWriteLock();
    private final Map<String, RepositoryConnectionPool> pools = new HashMap<String, RepositoryConnectionPool>();
    private RepositoryConnectionFactory delegate;
    private final ExecutionContext executionContext;
    private final ObservationBus observationBus;
    private final RepositorySource configurationSource;
    private final String configurationWorkspaceName;
    private final Path pathToConfigurationRoot;

    /**
     * Create a new manager instance.
     * 
     * @param configurationSource the {@link RepositorySource} that is the configuration repository
     * @param configurationWorkspaceName the name of the workspace in the {@link RepositorySource} that is the configuration
     *        repository, or null if the default workspace of the source should be used (if there is one)
     * @param pathToSourcesConfigurationRoot the path of the node in the configuration source repository that should be treated by
     *        this service as the root of the service's configuration
     * @param context the execution context in which this service should run
     * @param observationBus the {@link ObservationBus} instance that should be used for changes in the sources
     * @throws IllegalArgumentException if any of the <code>configurationSource</code>,
     *         <code>pathToSourcesConfigurationRoot</code>, <code>observationBus</code>, or <code>context</code> references are
     *         null
     */
    public RepositoryLibrary( RepositorySource configurationSource,
                              String configurationWorkspaceName,
                              Path pathToSourcesConfigurationRoot,
                              final ExecutionContext context,
                              ObservationBus observationBus ) {
        CheckArg.isNotNull(configurationSource, "configurationSource");
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(pathToSourcesConfigurationRoot, "pathToSourcesConfigurationRoot");
        CheckArg.isNotNull(observationBus, "observationBus");
        this.executionContext = context;
        this.configurationSource = configurationSource;
        this.configurationWorkspaceName = configurationWorkspaceName;
        this.pathToConfigurationRoot = pathToSourcesConfigurationRoot;
        this.observationBus = observationBus;
    }

    /**
     * Get the path to the top-level of the configuration root.
     * 
     * @return pathToConfigurationRoot
     */
    protected Path getPathToConfigurationRoot() {
        return pathToConfigurationRoot;
    }

    /**
     * @return configurationSource
     */
    protected RepositorySource getConfigurationSource() {
        return configurationSource;
    }

    /**
     * @return configurationWorkspaceName
     */
    protected String getConfigurationWorkspaceName() {
        return configurationWorkspaceName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This can be used to register observers for all of the repository sources managed by this library. The supplied observer
     * will receive all of the changes originating from these sources.
     * </p>
     * 
     * @see org.modeshape.graph.observe.Observable#register(org.modeshape.graph.observe.Observer)
     */
    public boolean register( Observer observer ) {
        if (observer == null) return false;
        return observationBus.register(observer);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This can be used to unregister observers for all of the repository sources managed by this library.
     * </p>
     * 
     * @see org.modeshape.graph.observe.Observable#unregister(org.modeshape.graph.observe.Observer)
     */
    public boolean unregister( Observer observer ) {
        return observationBus.unregister(observer);
    }

    /**
     * @return executionContextFactory
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
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
            for (RepositoryConnectionPool pool : this.pools.values()) {
                // Shutdown the pool of connections ...
                pool.shutdown();
                // Now close the source (still allows in-use connections to be used) ...
                pool.getRepositorySource().close();
            }
        } finally {
            this.sourcesLock.readLock().unlock();
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
        // Check whether all source pools are shut down. This is done inside the pools write lock.
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools.values()) {
                if (!pool.awaitTermination(timeout, unit)) return false;
            }
            return true;
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Returns true if this library is in the process of terminating after {@link ServiceAdministrator#shutdown()} has been called
     * on the {@link #getAdministrator() administrator}, but the library has connections that have not yet normally been
     * {@link RepositoryConnection#close() closed}. This method may be useful for debugging. A return of <tt>true</tt> reported a
     * sufficient period after shutdown may indicate that connection users have ignored or suppressed interruption, causing this
     * repository not to properly terminate.
     * 
     * @return true if terminating but not yet terminated, or false otherwise
     * @see #isTerminated()
     */
    public boolean isTerminating() {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools.values()) {
                if (pool.isTerminating()) return true;
            }
            return false;
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Return true if this library has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     * @see #isTerminating()
     */
    public boolean isTerminated() {
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools.values()) {
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
        try {
            this.sourcesLock.readLock().lock();
            return Collections.unmodifiableCollection(new HashSet<String>(this.pools.keySet()));
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Get an unmodifiable collection of {@link RepositorySource} instances managed by this instance.
     * 
     * @return the pools
     */
    public Collection<RepositorySource> getSources() {
        List<RepositorySource> sources = new LinkedList<RepositorySource>();
        try {
            this.sourcesLock.readLock().lock();
            for (RepositoryConnectionPool pool : this.pools.values()) {
                sources.add(pool.getRepositorySource());
            }
            return Collections.unmodifiableCollection(sources);
        } finally {
            this.sourcesLock.readLock().unlock();
        }
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
            RepositoryConnectionPool existingPool = this.pools.get(sourceName);
            return existingPool == null ? null : existingPool.getRepositorySource();
        } finally {
            this.sourcesLock.readLock().unlock();
        }
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
            return this.pools.get(sourceName);
        } finally {
            this.sourcesLock.readLock().unlock();
        }
    }

    /**
     * Add the supplied source. This method returns false if the source is null.
     * 
     * @param source the source to add
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     */
    public boolean addSource( RepositorySource source ) {
        return addSource(source, false);
    }

    /**
     * Add the supplied source. This method returns false if the source is null.
     * <p>
     * If a source with the same name already exists, it will be replaced only if <code>replaceIfExisting</code> is true. If this
     * is the case, then the existing source will be removed from the connection pool, and that pool will be
     * {@link RepositoryConnectionPool#shutdown() shutdown} (allowing any in-use connections to be used and finished normally).
     * </p>
     * 
     * @param source the source to add
     * @param replaceIfExisting true if an existing source should be replaced, or false if this method should return false if
     *        there is already an existing source with the supplied name.
     * @return true if the source is added, or false if the reference is null or if there is already an existing source with the
     *         supplied name.
     */
    public boolean addSource( RepositorySource source,
                              boolean replaceIfExisting ) {
        if (source == null) return false;
        final String sourceName = source.getName();
        if (!replaceIfExisting) {
            // Don't want to replace existing, so make sure there isn't one already ...
            try {
                this.sourcesLock.readLock().lock();
                if (this.pools.containsKey(sourceName)) return false;
            } finally {
                this.sourcesLock.readLock().unlock();
            }
        }
        // Create a repository context for this source ...
        final ObservationBus observationBus = this.observationBus;
        RepositoryContext repositoryContext = new RepositoryContext() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
             */
            public ExecutionContext getExecutionContext() {
                return RepositoryLibrary.this.getExecutionContext();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
             */
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return RepositoryLibrary.this;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
             */
            public Observer getObserver() {
                return observationBus.hasObservers() ? observationBus : null;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
             */
            public Subgraph getConfiguration( int depth ) {
                Subgraph result = null;
                RepositorySource configSource = getConfigurationSource();
                if (configSource != null) {
                    Graph config = Graph.create(configSource, getExecutionContext());
                    String workspaceName = getConfigurationWorkspaceName();
                    if (workspaceName != null) {
                        config.useWorkspace(workspaceName);
                    }
                    Path configPath = getPathToConfigurationRoot();
                    Path sourcePath = getExecutionContext().getValueFactories().getPathFactory().create(configPath, sourceName);
                    result = config.getSubgraphOfDepth(depth).at(sourcePath);
                }
                return result;
            }
        };
        // Do this before we remove the existing pool ...
        source.initialize(repositoryContext);
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        try {
            this.sourcesLock.writeLock().lock();
            // Need to first remove any existing one ...
            RepositoryConnectionPool existingPool = this.pools.remove(sourceName);
            if (existingPool != null) {
                // Then shut down the source gracefully (and don't wait) ...
                existingPool.shutdown();
            }
            this.pools.put(sourceName, pool);
            return true;
        } finally {
            this.sourcesLock.writeLock().unlock();
        }
    }

    /**
     * Remove from this library the supplied source (or a source with the same name as that supplied). This call shuts down the
     * connections in the source in an orderly fashion, allowing those connection currently in use to be used and closed normally,
     * but preventing further connections from being used.
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
     * Remove from this library the source with the supplied name. This call shuts down the connections in the source in an
     * orderly fashion, allowing those connection currently in use to be used and closed normally, but preventing further
     * connections from being used. However, this method never waits until the connections are all closed, and is equivalent to
     * calling <code>removeSource(name,0,TimeUnit.SECONDS)</code>.
     * 
     * @param name the name of the source to be removed
     * @return the source with the supplied name that was removed, or null if no existing source matching the supplied name could
     *         be found
     * @see #removeSource(String, long, TimeUnit)
     */
    public RepositorySource removeSource( String name ) {
        try {
            this.sourcesLock.writeLock().lock();
            RepositoryConnectionPool existingPool = this.pools.remove(name);
            if (existingPool != null) {
                // Then shut down the source gracefully (and don't wait) ...
                existingPool.shutdown();
                return existingPool.getRepositorySource();
            }
        } finally {
            this.sourcesLock.writeLock().unlock();
        }
        return null;
    }

    /**
     * Remove from this library the source with the supplied name. This call shuts down the connections in the source in an
     * orderly fashion, allowing those connection currently in use to be used and closed normally, but preventing further
     * connections from being used.
     * 
     * @param name the name of the source to be removed
     * @param timeToAwait the amount of time to wait while all of the source's connections are closed, or non-positive if the call
     *        should not wait at all
     * @param unit the time unit to be used for <code>timeToAwait</code>
     * @return the source with the supplied name that was removed, or null if no existing source matching the supplied name could
     *         be found
     * @throws InterruptedException if the thread is interrupted while awaiting closing of the connections
     * @see #removeSource(String)
     */
    public RepositorySource removeSource( String name,
                                          long timeToAwait,
                                          TimeUnit unit ) throws InterruptedException {
        try {
            this.sourcesLock.writeLock().lock();
            RepositoryConnectionPool existingPool = this.pools.remove(name);
            if (existingPool != null) {
                // Then shut down the source gracefully (and don't wait) ...
                existingPool.shutdown();
                if (timeToAwait > 0L) existingPool.awaitTermination(timeToAwait, unit);
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
     * @see org.modeshape.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
     */
    public RepositoryConnection createConnection( String sourceName ) {
        try {
            this.sourcesLock.readLock().lock();
            RepositoryConnectionPool existingPool = this.pools.get(sourceName);
            if (existingPool != null) return existingPool.getConnection();
            RepositoryConnectionFactory delegate = this.delegate;
            if (delegate != null) {
                return delegate.createConnection(sourceName);
            }
        } finally {
            this.sourcesLock.readLock().unlock();
        }
        return null;
    }
}

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
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * The component in the {@link FederationService} that represents a single federated repository. The federated repository manages
 * a set of {@link RepositorySource federated sources}, and provides the logic of interacting with those sources and presenting a
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
    private final ExecutionEnvironment env;
    private final RepositoryConnectionFactories connectionFactories;
    private final CopyOnWriteArrayList<FederatedRegion> regions = new CopyOnWriteArrayList<FederatedRegion>();
    private final CopyOnWriteArrayList<RepositorySourceListener> listeners = new CopyOnWriteArrayList<RepositorySourceListener>();
    private CachePolicy defaultCachePolicy;

    /**
     * Create a federated repository instance, as managed by the supplied {@link FederationService}.
     * 
     * @param repositoryName the name of the repository
     * @param env the execution environment
     * @param connectionFactories the set of connection factories that should be used
     * @throws IllegalArgumentException if any of the parameters are null, or if the name is blank
     */
    public FederatedRepository( String repositoryName,
                                ExecutionEnvironment env,
                                RepositoryConnectionFactories connectionFactories ) {
        ArgCheck.isNotNull(connectionFactories, "connectionFactories");
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotEmpty(repositoryName, "repositoryName");
        this.name = repositoryName;
        this.env = env;
        this.connectionFactories = connectionFactories;
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
     * @return the execution environment
     */
    public ExecutionEnvironment getExecutionEnvironment() {
        return env;
    }

    /**
     * @return connectionFactories
     */
    protected RepositoryConnectionFactories getConnectionFactories() {
        return connectionFactories;
    }

    /**
     * Utility method called by the administrator.
     */
    protected void startRepository() {
        // Look for the sources in the repository, creating any that are missing
        // Look for the
        // Do not establish connections to the sources; these will be established as needed

    }

    /**
     * Utility method called by the administrator.
     */
    protected void shutdownRepository() {
        // Connections to this repository check before doing anything with this, so no need to do anything to them ...
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
        return true;
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
        return false;
    }

    /**
     * Return true if this federated repository has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     * @see #isTerminating()
     */
    public boolean isTerminated() {
        return false;
    }

    /**
     * Return the unmodifiable list of bindings.
     * 
     * @return the bindings
     */
    public List<FederatedRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /**
     * Add the supplied federation region to this repository, if it is not already in the repository. . This method does not
     * attempt to check whether this region would result in a duplicate region.
     * 
     * @param region the region to be added
     * @return true if the region was added, or false if there was already a duplicate region
     * @throws IllegalArgumentException if the binding reference is null
     */
    protected boolean addRegionIfAbsent( FederatedRegion region ) {
        ArgCheck.isNotNull(region, "region");
        return this.regions.addIfAbsent(region);
    }

    protected boolean removeBinding( FederatedRegion region ) {
        return this.regions.remove(region);
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

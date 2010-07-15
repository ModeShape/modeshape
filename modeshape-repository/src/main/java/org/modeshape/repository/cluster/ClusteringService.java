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
package org.modeshape.repository.cluster;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.component.ComponentLibrary;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.LocalObservationBus;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.observe.Observer;
import org.modeshape.repository.RepositoryI18n;
import org.modeshape.repository.service.AbstractServiceAdministrator;
import org.modeshape.repository.service.AdministeredService;
import org.modeshape.repository.service.ServiceAdministrator;

/**
 * The service that provides the observation bus for a clustered (or unclustered) environment.
 */
public class ClusteringService implements AdministeredService, ObservationBus {

    /**
     * The administrative component for this service.
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.clusteringServiceName, State.PAUSED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doStart( State fromState ) {
            super.doStart(fromState);
            startService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            shutdownService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return isServiceTerminated();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) {
            return true; // nothing to wait for
        }

    }

    private ExecutionContext executionContext;
    private ObservationBus bus;
    private final ComponentLibrary<ObservationBus, ClusteringConfig> busLibrary = new ComponentLibrary<ObservationBus, ClusteringConfig>();

    /**
     * @return executionContext
     */
    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    /**
     * @param executionContext Sets executionContext to the specified value.
     */
    public void setExecutionContext( ExecutionContext executionContext ) {
        CheckArg.isNotNull(executionContext, "execution context");
        if (this.getAdministrator().isStarted()) {
            throw new IllegalStateException(RepositoryI18n.unableToChangeExecutionContextWhileRunning.text());
        }
        this.executionContext = executionContext;
        this.busLibrary.setClassLoaderFactory(executionContext);
    }

    /**
     * Set the configuration for the clustering. This method will replace any existing configuration.
     * 
     * @param config the new configuration, or null if the default configuration should be used
     * @return true if the configuration was set, or false otherwise
     */
    public boolean setClusteringConfig( ClusteringConfig config ) {
        if (config == null) config = createDefaultConfiguration();
        return this.busLibrary.removeAllAndAdd(config);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.ObservationBus#hasObservers()
     */
    @Override
    public boolean hasObservers() {
        return bus != null && bus.hasObservers();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.Observable#register(org.modeshape.graph.observe.Observer)
     */
    @Override
    public boolean register( Observer observer ) {
        if (bus == null) {
            throw new IllegalStateException(RepositoryI18n.unableToRegisterObserverOnUnstartedClusteringService.text());
        }
        return bus.register(observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.Observable#unregister(org.modeshape.graph.observe.Observer)
     */
    @Override
    public boolean unregister( Observer observer ) {
        if (bus == null) {
            throw new IllegalStateException(RepositoryI18n.unableToUnregisterObserverOnUnstartedClusteringService.text());
        }
        return bus.unregister(observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
     */
    @Override
    public void notify( Changes changes ) {
        if (bus == null) {
            throw new IllegalStateException(RepositoryI18n.unableToNotifyObserversOnUnstartedClusteringService.text());
        }
        bus.notify(changes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.service.AdministeredService#getAdministrator()
     */
    @Override
    public ServiceAdministrator getAdministrator() {
        return new Administrator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is equivalent to calling <code>getAdminstrator().start()</code> and can be called multiple times.
     * </p>
     * 
     * @see org.modeshape.graph.observe.ObservationBus#start()
     */
    @Override
    public void start() {
        getAdministrator().start();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is equivalent to calling <code>getAdminstrator().shutdown()</code>.
     * </p>
     * 
     * @see org.modeshape.graph.observe.ObservationBus#shutdown()
     */
    @Override
    public void shutdown() {
        getAdministrator().shutdown();
    }

    protected void startService() {
        List<ObservationBus> instances = busLibrary.getInstances();
        if (instances.isEmpty()) {
            setClusteringConfig(null);
            instances = busLibrary.getInstances();
            assert instances.size() > 0;
        }
        this.bus = instances.get(0);
        this.bus.start();
    }

    protected void shutdownService() {
        // Unregister our observer ...
        try {
            if (this.bus != null) {
                this.bus.shutdown();
            }
        } finally {
            this.bus = null;
        }
    }

    protected boolean isServiceTerminated() {
        return this.bus != null;
    }

    protected ClusteringConfig createDefaultConfiguration() {
        return new ClusteringConfig("bus", "Local observation bus", LocalObservationBus.class.getName(), null);
    }
}

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederationService implements AdministeredService {

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
            startService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return FederationService.this.isTerminated();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
            return FederationService.this.awaitTermination(timeout, unit);
        }

    }

    private final RepositorySource configurationSource;
    private final Administrator administrator = new Administrator();
    private final ConcurrentMap<String, FederatedRepository> repositories = new ConcurrentHashMap<String, FederatedRepository>();
    private RepositoryConnection configurationConnection;

    /**
     * Create a federation service instance
     * 
     * @param configurationSource the repository source that contains the configuration for this federation service (including the
     * respositories and the sources used by the federated repositories)
     * @throws IllegalArgumentException if the bootstrap source is null
     */
    public FederationService( RepositorySource configurationSource ) {
        ArgCheck.isNotNull(configurationSource, "configurationSource");
        this.configurationSource = configurationSource;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Get the source for the repository containing the configuration for this federation service.
     * 
     * @return the configuration repository source; never null
     */
    public RepositorySource getConfigurationSource() {
        return this.configurationSource;
    }

    public String getJndiName() {
        // TODO
        return null;
    }

    protected void startService() {
        if (this.configurationConnection == null) {
            try {
                this.configurationConnection = this.configurationSource.getConnection();
            } catch (InterruptedException err) {
                throw new FederationException(RepositoryI18n.interruptedWhileConnectingToFederationConfigurationRepository.text(this.configurationSource.getName()));
            }
        }
    }

    /**
     * Get the federated repository object with the given name. The resulting repository will be started and ready to use.
     * 
     * @param name the name of the repository
     * @return the repository instance
     */
    protected FederatedRepository getRepository( String name ) {
        // Look for an existing repository ...
        FederatedRepository repository = this.repositories.get(name);
        if (repository == null) {
            // Look up the node representing the repository in the configuration ...

            // New up a repository and configure it ...
            repository = new FederatedRepository(this, name);
            // Now register it, being careful to not overwrite any added since previous call ..
            FederatedRepository existingRepository = this.repositories.putIfAbsent(name, repository);
            if (existingRepository != null) repository = existingRepository;
        }
        // Make sure it's started. By doing this here, whoever finds it in the map will start it.
        repository.getAdministrator().start();
        return repository;
    }

    protected void shutdownService() {
        if (this.configurationConnection != null) {
            try {
                this.configurationConnection.close();
            } catch (InterruptedException err) {
                throw new FederationException(RepositoryI18n.interruptedWhileClosingConnectionToFederationConfigurationRepository.text(this.configurationSource.getName()));
            }
            // Now shut down all repositories ...
            for (String repositoryName : this.repositories.keySet()) {
                FederatedRepository repository = this.repositories.get(repositoryName);
                repository.getAdministrator().shutdown();
            }
        }
    }

    protected boolean isTerminated() {
        // Now shut down all repositories ...
        for (String repositoryName : this.repositories.keySet()) {
            FederatedRepository repository = this.repositories.get(repositoryName);
            if (!repository.getAdministrator().isTerminated()) return false;
        }
        return true;
    }

    protected boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
        // Now shut down all repositories ...
        for (String repositoryName : this.repositories.keySet()) {
            FederatedRepository repository = this.repositories.get(repositoryName);
            if (repository.getAdministrator().awaitTermination(timeout, unit)) return false;
        }
        return true;
    }

    /**
     * Create a {@link RepositorySource} that can be used to establish connections to the federated repository with the supplied
     * name.
     * 
     * @param repositoryName the name of the federated repository
     * @return the source that can be configured and used to establish connection to the repository
     */
    public RepositorySource createRepositorySource( String repositoryName ) {
        FederatedRepositorySource source = new FederatedRepositorySource(this, repositoryName);
        return source;
    }

    /**
     * Get the current set of repository names.
     * 
     * @return the unmodifiable names of the repository.
     */
    public Set<String> getRepositoryNames() {
        return Collections.unmodifiableSet(this.repositories.keySet());
    }

    protected void removeRepository( FederatedRepository repository ) {
        this.repositories.remove(repository.getName(), repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return false;
    }
}

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
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.repository.util.ExecutionContext;
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
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return FederationService.this.awaitTermination(timeout, unit);
        }

    }

    private final ClassLoaderFactory classLoaderFactory;
    private final ExecutionContext executionContext;
    private final RepositorySource configurationSource;
    private final Administrator administrator = new Administrator();
    private final ConcurrentMap<String, FederatedRepository> repositories = new ConcurrentHashMap<String, FederatedRepository>();
    private RepositoryConnection configurationConnection;

    /**
     * Create a federation service instance
     * 
     * @param configurationSource the repository source that contains the configuration for this federation service (including the
     *        respositories and the sources used by the federated repositories)
     * @param executionContext the context in which this service should run
     * @param classLoaderFactory the class loader factory used to instantiate {@link RepositorySource} instances; may be null if
     *        this instance should use a default factory that attempts to load classes first from the
     *        {@link Thread#getContextClassLoader() thread's current context class loader} and then from the class loader that
     *        loaded this class.
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public FederationService( RepositorySource configurationSource,
                              ExecutionContext executionContext,
                              ClassLoaderFactory classLoaderFactory ) {
        ArgCheck.isNotNull(configurationSource, "configurationSource");
        ArgCheck.isNotNull(executionContext, "executionContext");
        this.configurationSource = configurationSource;
        this.executionContext = executionContext;
        this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : new StandardClassLoaderFactory();
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

    /**
     * @return executionContext
     */
    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    /**
     * @return classLoaderFactory
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    public String getJndiName() {
        // TODO
        return null;
    }

    protected synchronized void startService() {
        if (this.configurationConnection == null) {
            try {
                this.configurationConnection = this.configurationSource.getConnection();
            } catch (InterruptedException err) {
                I18n msg = RepositoryI18n.interruptedWhileConnectingToFederationConfigurationRepository;
                throw new FederationException(msg.text(configurationSource.getName()));
            }
            // // Read the configuration and obtain the RepositorySource instances for each of the
            // // federated repositories. Each repository configuration is rooted at
            // // "/dna:repositories/[repositoryName]", and under this node at "dna:federation"
            // // is the RepositorySource for the integrated repository, and under the
            // // "dna:sources/[sourceName]" are the nodes representing the inital RepositorySource
            // // instances for each of the sources.
            // //
            // // The integrated repository for each federated repository contains the complete unified
            // // graph merged from all of the sources. It also contains the configuration for the federated
            // // repository at "/dna:system/dna:federation", including under "dna:sources" a node for each of the current
            // // sources (e.g., "/dna:system/dna:federation/dna:sources/[sourceName]"). If this area of the
            // // graph does not yet exist, the sources are copied from the
            // // "/dna:repositories/[repositoryName]/dna:sources" area of the service's configuration repository.
            // // However, after that, the federated repository manages its own sources.
            //
            // ValueFactories valueFactories = executionContext.getValueFactories();
            // PathFactory pathFactory = valueFactories.getPathFactory();
            //
            // // Get the list of repositories in the configuration, and create a FederatedRepository for each one ...
            // try {
            // Path repositoriesNode = pathFactory.create("dna:repositories");
            // BasicGetChildrenCommand getRepositories = new BasicGetChildrenCommand(repositoriesNode);
            // configurationConnection.execute(executionContext, getRepositories);
            //
            // // For each repository ...
            // for (Path.Segment child : getRepositories.getChildren()) {
            //
            // // Get the repository's name ...
            // final String repositoryName = child.getUnencodedString();
            // final Path pathToRepository = pathFactory.create(repositoriesNode, child);
            //
            // // Record the initial sources ...
            // final Path.Segment sourcesSegment = pathFactory.createSegment("dna:sources");
            // final Path pathToRepositorySourcesNode = pathFactory.create(pathToRepository, sourcesSegment);
            // BasicRecordBranchCommand getSources = new BasicRecordBranchCommand(pathToRepositorySourcesNode,
            // NodeConflictBehavior.DO_NOT_REPLACE);
            //
            // // Get the source of the integrated repository ...
            // final Path.Segment integratedRepositorySegment = pathFactory.createSegment("dna:federatedRepository");
            // final Path pathToIntegratedRepositoryNode = pathFactory.create(pathToRepository, integratedRepositorySegment);
            // BasicGetNodeCommand getSource = new BasicGetNodeCommand(pathToIntegratedRepositoryNode);
            // configurationConnection.execute(executionContext, getSource);
            // RepositorySource integratedRepositorySource = createRepositorySource(valueFactories,
            // getSource.getProperties());
            //
            // // Copy these to the federated repository ...
            // RepositoryConnection integratedConnection = integratedReposi
            // // Look for the
            // // Read the initial sources ...
            //
            // // Get the repository source information for the integrated repository ...
            //
            // // Look for existing sources and load them ...
            //
            // // Otherwise, read the intial sources from the
            //
            // }
            // } catch (InterruptedException err) {
            // I18n msg = RepositoryI18n.interruptedWhileUsingFederationConfigurationRepository;
            // throw new FederationException(msg.text(configurationSource.getName()));
            // }
            // // TODO
        }
    }

    // protected RepositorySource createRepositorySource( ValueFactories values,
    // Iterable<Property> properties ) {
    // // Put the properties in a map so we can find them by name ...
    // Map<Name, Property> byName = new HashMap<Name, Property>();
    // for (Property property : properties) {
    // byName.put(property.getName(), property);
    // }
    //
    // // Get the concrete class ...
    // Name classnameName = values.getNameFactory().create("dna:className");
    // Property classProperty = byName.get(classnameName);
    // if (classProperty == null) return null;
    // if (classProperty.isEmpty()) return null;
    // String className = values.getStringFactory().create(classProperty.getValues().next());
    // if (className == null) return null;
    //
    // Name classpathName = values.getNameFactory().create("dna:classpath");
    // Property classpathProperty = byName.get(classpathName);
    // String[] classpath = null;
    // if (classpathProperty != null) {
    // classpath = values.getStringFactory().create(classpathProperty.getValuesAsArray());
    // }
    //
    // // Load the class and look for the constructors ...
    // RepositorySource source = null;
    // try {
    // ClassLoader loader = getClassLoaderFactory().getClassLoader(classpath);
    // Class<?> sourceClass = loader.loadClass(className);
    // source = (RepositorySource)sourceClass.newInstance();
    // } catch (ClassNotFoundException err) {
    // return null;
    // } catch (InstantiationException err) {
    //
    // } catch (IllegalAccessException err) {
    // }
    // return null;
    // }

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

    protected synchronized void shutdownService() {
        if (this.configurationConnection != null) {
            try {
                this.configurationConnection.close();
            } catch (InterruptedException err) {
                throw new FederationException(
                                              RepositoryI18n.interruptedWhileClosingConnectionToFederationConfigurationRepository.text(this.configurationSource.getName()));
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

    protected boolean awaitTermination( long timeout,
                                        TimeUnit unit ) throws InterruptedException {
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

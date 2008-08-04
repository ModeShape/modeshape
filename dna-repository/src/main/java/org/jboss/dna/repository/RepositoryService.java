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
package org.jboss.dna.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.connector.federation.FederatedRepositorySource;
import org.jboss.dna.connector.federation.FederationException;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.connector.federation.executor.FederatingCommandExecutor;
import org.jboss.dna.connector.federation.executor.SingleProjectionCommandExecutor;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.executor.CommandExecutor;
import org.jboss.dna.spi.graph.commands.executor.NoOpCommandExecutor;
import org.jboss.dna.spi.graph.commands.impl.BasicCompositeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetChildrenCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class RepositoryService implements AdministeredService {

    protected static final String CLASSNAME_PROPERTY_NAME = "dna:classname";
    protected static final String CLASSPATH_PROPERTY_NAME = "dna:classpath";
    protected static final String PROJECTION_RULES_PROPERTY_NAME = "dna:projectionRules";
    protected static final String CACHE_POLICY_TIME_TO_EXPIRE = "dna:timeToExpire";
    protected static final String CACHE_POLICY_TIME_TO_CACHE = "dna:timeToCache";

    /**
     * The administrative component for this service.
     * 
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.federationServiceName, State.PAUSED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return true;
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
         * 
         * @see org.jboss.dna.repository.services.ServiceAdministrator#awaitTermination(long, java.util.concurrent.TimeUnit)
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) {
            return true;
        }
    }

    private final ClassLoaderFactory classLoaderFactory;
    private final ExecutionContext context;
    private final RepositorySourceManager sources;
    private final Projection configurationProjection;
    private final Administrator administrator = new Administrator();
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Create a federation service instance
     * 
     * @param sources the source manager
     * @param configurationProjection the projection defining where the service can find configuration information for the
     *        different repositories that it is to manage
     * @param context the execution context in which this service should run
     * @param classLoaderFactory the class loader factory used to instantiate {@link RepositorySource} instances; may be null if
     *        this instance should use a default factory that attempts to load classes first from the
     *        {@link Thread#getContextClassLoader() thread's current context class loader} and then from the class loader that
     *        loaded this class.
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public RepositoryService( RepositorySourceManager sources,
                              Projection configurationProjection,
                              ExecutionContext context,
                              ClassLoaderFactory classLoaderFactory ) {
        ArgCheck.isNotNull(configurationProjection, "configurationProjection");
        ArgCheck.isNotNull(sources, "sources");
        ArgCheck.isNotNull(context, "context");
        this.sources = sources;
        this.configurationProjection = configurationProjection;
        this.context = context;
        this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : new StandardClassLoaderFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return configurationProjection
     */
    public Projection getConfigurationProjection() {
        return configurationProjection;
    }

    /**
     * @return sources
     */
    public RepositorySourceManager getRepositorySourceManager() {
        return sources;
    }

    /**
     * @return env
     */
    public ExecutionContext getExecutionEnvironment() {
        return context;
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
        if (this.started.get() == false) {
            Problems problems = new SimpleProblems();

            // ------------------------------------------------------------------------------------
            // Read the configuration ...
            // ------------------------------------------------------------------------------------
            ValueFactories valueFactories = context.getValueFactories();
            PathFactory pathFactory = valueFactories.getPathFactory();
            NameFactory nameFactory = valueFactories.getNameFactory();

            final String configurationSourceName = configurationProjection.getSourceName();
            RepositoryConnectionFactory factory = sources.getConnectionFactory(configurationSourceName);
            if (factory == null) {
                throw new FederationException(RepositoryI18n.unableToFindRepositorySourceWithName.text(configurationSourceName));
            }

            // Create a federating command executor to execute the commands and merge the results into a single set of
            // commands.
            List<Projection> projections = Collections.singletonList(configurationProjection);
            CommandExecutor executor = null;
            if (configurationProjection.getRules().size() == 1) {
                // There is just a single projection for the configuration repository, so just use an executor that
                // translates the paths using the projection
                executor = new SingleProjectionCommandExecutor(context, configurationSourceName, configurationProjection, sources);
            } else if (configurationProjection.getRules().size() == 0) {
                // There is no projection for the configuration repository, so just use a no-op executor
                executor = new NoOpCommandExecutor(context, configurationSourceName);
            } else {
                // The configuration repository has more than one projection, so we need to merge the results
                executor = new FederatingCommandExecutor(context, configurationSourceName, projections, sources);
            }

            // Read the configuration and the repository sources, located as child nodes/branches under "/dna:sources",
            // and then instantiate and register each in the "sources" manager
            Path configurationRoot = pathFactory.create("/");
            try {
                Path sourcesNode = pathFactory.create(configurationRoot, nameFactory.create("dna:sources"));
                BasicGetChildrenCommand getSources = new BasicGetChildrenCommand(sourcesNode);
                executor.execute(getSources);
                if (getSources.hasNoError()) {

                    // Build the commands to get each of the children ...
                    List<Path.Segment> children = getSources.getChildren();
                    if (!children.isEmpty()) {
                        BasicCompositeCommand commands = new BasicCompositeCommand();
                        for (Path.Segment child : getSources.getChildren()) {
                            final Path pathToSource = pathFactory.create(sourcesNode, child);
                            commands.add(new BasicGetNodeCommand(pathToSource));
                        }
                        executor.execute(commands);

                        // Iterate over each source node obtained ...
                        for (GraphCommand command : commands) {
                            BasicGetNodeCommand getSourceCommand = (BasicGetNodeCommand)command;
                            if (getSourceCommand.hasNoError()) {
                                RepositorySource source = createRepositorySource(getSourceCommand.getPath(),
                                                                                 getSourceCommand.getProperties(),
                                                                                 problems);
                                if (source != null) sources.addSource(source, true);
                            }
                        }
                    }
                }
            } catch (Throwable err) {
                throw new FederationException(RepositoryI18n.errorStartingRepositoryService.text());
            }
            this.started.set(true);
        }
    }

    /**
     * Instantiate the {@link RepositorySource} described by the supplied properties.
     * 
     * @param path the path to the node where these properties were found; never null
     * @param properties the properties; never null
     * @param problems the problems container in which any problems should be reported; never null
     * @return the repository source instance, or null if it could not be created
     */
    @SuppressWarnings( "null" )
    protected RepositorySource createRepositorySource( Path path,
                                                       Map<Name, Property> properties,
                                                       Problems problems ) {
        ValueFactories valueFactories = context.getValueFactories();
        NameFactory nameFactory = valueFactories.getNameFactory();
        ValueFactory<String> stringFactory = valueFactories.getStringFactory();

        // Get the classname and classpath ...
        Property classnameProperty = properties.get(nameFactory.create(CLASSNAME_PROPERTY_NAME));
        Property classpathProperty = properties.get(nameFactory.create(CLASSPATH_PROPERTY_NAME));
        if (classnameProperty == null) {
            problems.addError(RepositoryI18n.requiredPropertyIsMissingFromNode, CLASSNAME_PROPERTY_NAME, path);
        }
        // If the classpath property is null or empty, the default classpath will be used
        if (problems.hasErrors()) return null;

        // Create the instance ...
        String classname = stringFactory.create(classnameProperty.getValues().next());
        String[] classpath = classpathProperty == null ? new String[] {} : stringFactory.create(classpathProperty.getValuesAsArray());
        ClassLoader classLoader = this.classLoaderFactory.getClassLoader(classpath);
        RepositorySource source = null;
        try {
            Class<?> sourceClass = classLoader.loadClass(classname);
            source = (RepositorySource)sourceClass.newInstance();
        } catch (ClassNotFoundException err) {
            problems.addError(err, RepositoryI18n.unableToLoadClassUsingClasspath, classname, classpath);
        } catch (IllegalAccessException err) {
            problems.addError(err, RepositoryI18n.unableToAccessClassUsingClasspath, classname, classpath);
        } catch (Throwable err) {
            problems.addError(err, RepositoryI18n.unableToInstantiateClassUsingClasspath, classname, classpath);
        }

        // Try to set the name property to the local name of the node...
        Reflection reflection = new Reflection(source.getClass());
        try {
            reflection.invokeSetterMethodOnTarget("name", source, path.getLastSegment().getName().getLocalName());
        } catch (SecurityException err) {
            // Do nothing ... assume not a JavaBean property
        } catch (NoSuchMethodException err) {
            // Do nothing ... assume not a JavaBean property
        } catch (IllegalArgumentException err) {
            // Do nothing ... assume not a JavaBean property
        } catch (IllegalAccessException err) {
            // Do nothing ... assume not a JavaBean property
        } catch (InvocationTargetException err) {
            // Do nothing ... assume not a JavaBean property
        }

        // Now set all the properties that we can, ignoring any property that doesn't fit pattern ...
        for (Map.Entry<Name, Property> entry : properties.entrySet()) {
            Name propertyName = entry.getKey();
            Property property = entry.getValue();
            String javaPropertyName = propertyName.getLocalName();
            if (property.isEmpty()) continue;
            Object value = null;
            if (property.isSingle()) {
                value = property.getValues().next();
            } else if (property.isMultiple()) {
                value = property.getValuesAsArray();
            }
            try {
                reflection.invokeSetterMethodOnTarget(javaPropertyName, source, value);
            } catch (SecurityException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (NoSuchMethodException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (IllegalArgumentException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (IllegalAccessException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (InvocationTargetException err) {
                // Do nothing ... assume not a JavaBean property
            }
        }
        return source;
    }

    /**
     * Get the current set of federated repository names.
     * 
     * @return the names of the repository, which is a mutable copy of the names that is not backed by the actual sources
     */
    public Set<String> getFederatedRepositoryNames() {
        Set<String> repositoryNames = new HashSet<String>();
        for (RepositorySource source : sources.getSources()) {
            if (source instanceof FederatedRepositorySource) {
                repositoryNames.add(source.getName());
            }
        }
        return repositoryNames;
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

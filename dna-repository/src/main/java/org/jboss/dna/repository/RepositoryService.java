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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.connector.federation.FederationException;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class RepositoryService implements AdministeredService {

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

    private final ExecutionContext context;
    private final RepositoryLibrary sources;
    private final String configurationSourceName;
    private final Path pathToConfigurationRoot;
    private final Administrator administrator = new Administrator();
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Create a service instance, reading the configuration describing new {@link RepositorySource} instances from the source with
     * the supplied name.
     * 
     * @param sources the source manager
     * @param configurationSourceName the name of the {@link RepositorySource} that is the configuration repository
     * @param context the execution context in which this service should run
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public RepositoryService( RepositoryLibrary sources,
                              String configurationSourceName,
                              ExecutionContext context ) {
        this(sources, configurationSourceName, null, context);
    }

    /**
     * Create a service instance, reading the configuration describing new {@link RepositorySource} instances from the source with
     * the supplied name and path within the repository.
     * 
     * @param sources the source manager
     * @param configurationSourceName the name of the {@link RepositorySource} that is the configuration repository
     * @param pathToConfigurationRoot the path of the node in the configuration source repository that should be treated by this
     *        service as the root of the service's configuration; if null, then "/dna:system" is used
     * @param context the execution context in which this service should run
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public RepositoryService( RepositoryLibrary sources,
                              String configurationSourceName,
                              Path pathToConfigurationRoot,
                              ExecutionContext context ) {
        CheckArg.isNotNull(configurationSourceName, "configurationSourceName");
        CheckArg.isNotNull(sources, "sources");
        CheckArg.isNotNull(context, "context");
        if (pathToConfigurationRoot == null) pathToConfigurationRoot = context.getValueFactories().getPathFactory().create("/dna:system");
        this.sources = sources;
        this.pathToConfigurationRoot = pathToConfigurationRoot;
        this.configurationSourceName = configurationSourceName;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return configurationSourceName
     */
    public String getConfigurationSourceName() {
        return configurationSourceName;
    }

    /**
     * @return sources
     */
    public RepositoryLibrary getRepositorySourceManager() {
        return sources;
    }

    /**
     * @return env
     */
    public ExecutionContext getExecutionEnvironment() {
        return context;
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

            // Read the configuration and repository source nodes (children under "/dna:sources") ...
            Graph graph = Graph.create(getConfigurationSourceName(), sources, context);
            Path pathToSourcesNode = context.getValueFactories().getPathFactory().create(pathToConfigurationRoot, "dna:sources");
            try {
                Subgraph sourcesGraph = graph.getSubgraphOfDepth(3).at(pathToSourcesNode);

                // Iterate over each of the children, and create the RepositorySource ...
                for (Location location : sourcesGraph.getRoot().getChildren()) {
                    Node sourceNode = sourcesGraph.getNode(location);
                    sources.addSource(createRepositorySource(location.getPath(), sourceNode.getPropertiesByName(), problems));
                }
            } catch (PathNotFoundException e) {
                // No sources were found, and this is okay!
            } catch (Throwable err) {
                throw new FederationException(RepositoryI18n.errorStartingRepositoryService.text(), err);
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
        ValueFactory<String> stringFactory = valueFactories.getStringFactory();

        // Get the classname and classpath ...
        Property classnameProperty = properties.get(DnaLexicon.CLASSNAME);
        Property classpathProperty = properties.get(DnaLexicon.CLASSPATH);
        if (classnameProperty == null) {
            problems.addError(RepositoryI18n.requiredPropertyIsMissingFromNode, DnaLexicon.CLASSNAME, path);
        }
        // If the classpath property is null or empty, the default classpath will be used
        if (problems.hasErrors()) return null;

        // Create the instance ...
        String classname = stringFactory.create(classnameProperty.getValues().next());
        String[] classpath = classpathProperty == null ? new String[] {} : stringFactory.create(classpathProperty.getValuesAsArray());
        ClassLoader classLoader = context.getClassLoader(classpath);
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return false;
    }
}

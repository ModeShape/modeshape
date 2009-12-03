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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.observe.Changes;
import org.jboss.dna.graph.observe.NetChangeObserver;
import org.jboss.dna.graph.observe.ObservedId;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.repository.service.AbstractServiceAdministrator;
import org.jboss.dna.repository.service.AdministeredService;
import org.jboss.dna.repository.service.ServiceAdministrator;

/**
 * A service that manages the {@link RepositorySource}es defined within a configuration repository.
 */
@ThreadSafe
public class RepositoryService implements AdministeredService, Observer {

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
         * @see org.jboss.dna.repository.service.AbstractServiceAdministrator#doShutdown(org.jboss.dna.repository.service.ServiceAdministrator.State)
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            shutdownService();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.service.ServiceAdministrator#awaitTermination(long, java.util.concurrent.TimeUnit)
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) {
            return true;
        }
    }

    private final ObservedId id;
    private final ExecutionContext context;
    private final RepositoryLibrary sources;
    private final String configurationSourceName;
    private final String configurationWorkspaceName;
    private final Path pathToConfigurationRoot;
    private final ConfigurationChangeObserver configurationChangeObserver;
    private final Administrator administrator = new Administrator();
    private final AtomicBoolean started = new AtomicBoolean(false);
    /** The problem sink used when encountering problems while starting repositories */
    private final Problems problems;

    /**
     * Create a service instance, reading the configuration describing new {@link RepositorySource} instances from the supplied
     * configuration repository.
     * 
     * @param configurationSource the {@link RepositorySource} that is the configuration repository
     * @param configurationWorkspaceName the name of the workspace in the {@link RepositorySource} that is the configuration
     *        repository, or null if the default workspace of the source should be used (if there is one)
     * @param pathToConfigurationRoot the path of the node in the configuration source repository that should be treated by this
     *        service as the root of the service's configuration; if null, then "/dna:system" is used
     * @param context the execution context in which this service should run
     * @param problems the {@link Problems} instance that this service should use to report problems starting repositories
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public RepositoryService( RepositorySource configurationSource,
                              String configurationWorkspaceName,
                              Path pathToConfigurationRoot,
                              ExecutionContext context,
                              Problems problems ) {
        CheckArg.isNotNull(configurationSource, "configurationSource");
        CheckArg.isNotNull(context, "context");
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        if (pathToConfigurationRoot == null) pathToConfigurationRoot = pathFactory.create("/dna:system");
        if (problems == null) problems = new SimpleProblems();
        Path sourcesPath = pathFactory.create(pathToConfigurationRoot, DnaLexicon.SOURCES);

        this.id = new ObservedId();
        this.sources = new RepositoryLibrary(configurationSource, configurationWorkspaceName, sourcesPath, context);
        this.sources.addSource(configurationSource);
        this.pathToConfigurationRoot = pathToConfigurationRoot;
        this.configurationSourceName = configurationSource.getName();
        this.configurationWorkspaceName = configurationWorkspaceName;
        this.context = context;
        this.problems = problems;
        this.configurationChangeObserver = new ConfigurationChangeObserver();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.observe.Observer#getId()
     */
    public ObservedId getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    public final ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return configurationSourceName
     */
    public final String getConfigurationSourceName() {
        return configurationSourceName;
    }

    /**
     * @return configurationWorkspaceName
     */
    public final String getConfigurationWorkspaceName() {
        return configurationWorkspaceName;
    }

    /**
     * Get the library of {@link RepositorySource} instances used by this service.
     * 
     * @return the RepositorySource library; never null
     */
    public final RepositoryLibrary getRepositoryLibrary() {
        return sources;
    }

    /**
     * @return pathToConfigurationRoot
     */
    protected final Path getPathToConfigurationRoot() {
        return pathToConfigurationRoot;
    }

    /**
     * @return env
     */
    public final ExecutionContext getExecutionEnvironment() {
        return context;
    }

    public String getJndiName() {
        // TODO
        return null;
    }

    protected synchronized void startService() {
        if (this.started.get() == false) {
            // ------------------------------------------------------------------------------------
            // Read the configuration ...
            // ------------------------------------------------------------------------------------

            // Read the configuration and repository source nodes (children under "/dna:sources") ...
            Graph graph = Graph.create(getConfigurationSourceName(), sources, context);
            Path pathToSourcesNode = context.getValueFactories().getPathFactory().create(pathToConfigurationRoot,
                                                                                         DnaLexicon.SOURCES);
            try {
                String workspaceName = getConfigurationWorkspaceName();
                if (workspaceName != null) graph.useWorkspace(workspaceName);

                Subgraph sourcesGraph = graph.getSubgraphOfDepth(3).at(pathToSourcesNode);

                // Iterate over each of the children, and create the RepositorySource ...
                for (Location location : sourcesGraph.getRoot().getChildren()) {
                    Node sourceNode = sourcesGraph.getNode(location);
                    sources.addSource(createRepositorySource(location.getPath(), sourceNode.getPropertiesByName(), problems));
                }
            } catch (PathNotFoundException e) {
                // No sources were found, and this is okay!
            } catch (Throwable err) {
                throw new DnaConfigurationException(RepositoryI18n.errorStartingRepositoryService.text(), err);
            }

            this.started.set(true);
        }
    }

    protected synchronized void shutdownService() {
        // Close the repository library ...
        this.sources.getAdministrator().shutdown();
    }

    /**
     * Instantiate the {@link RepositorySource} described by the supplied properties.
     * 
     * @param path the path to the node where these properties were found; never null
     * @param properties the properties; never null
     * @param problems the problems container in which any problems should be reported; never null
     * @return the repository source instance, or null if it could not be created
     */
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
        assert classnameProperty != null;
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
        if (source == null) return null;

        // We need to set the name using the local name of the node...
        Property nameProperty = context.getPropertyFactory().create(JcrLexicon.NAME,
                                                                    path.getLastSegment().getName().getLocalName());
        properties.put(JcrLexicon.NAME, nameProperty);

        // Attempt to set the configuration information as bean properties,
        // if they exist on the RepositorySource object and are not already set to some value ...
        setBeanPropertyIfExistsAndNotSet(source, "configurationSourceName", getConfigurationSourceName());
        setBeanPropertyIfExistsAndNotSet(source, "configurationWorkspaceName", getConfigurationWorkspaceName());
        setBeanPropertyIfExistsAndNotSet(source, "configurationPath", stringFactory.create(path));

        // Now set all the properties that we can, ignoring any property that doesn't fit the pattern ...
        Reflection reflection = new Reflection(source.getClass());
        for (Map.Entry<Name, Property> entry : properties.entrySet()) {
            Name propertyName = entry.getKey();
            Property property = entry.getValue();
            String javaPropertyName = propertyName.getLocalName();
            if (property.isEmpty()) continue;

            Object value = null;
            Method setter = null;
            try {
                setter = reflection.findFirstMethod("set" + javaPropertyName, false);
                if (setter == null) continue;
                // Determine the type of the one parameter ...
                Class<?>[] parameterTypes = setter.getParameterTypes();
                if (parameterTypes.length != 1) continue; // not a valid JavaBean property
                Class<?> paramType = parameterTypes[0];
                PropertyType allowedType = PropertyType.discoverType(paramType);
                if (allowedType == null) continue; // assume not a JavaBean property with usable type
                ValueFactory<?> factory = context.getValueFactories().getValueFactory(allowedType);
                if (paramType.isArray()) {
                    if (paramType.getComponentType().isArray()) continue; // array of array, which we don't do
                    Object[] values = factory.create(property.getValuesAsArray());
                    // Convert to an array of primitives if that's what the signature requires ...
                    Class<?> componentType = paramType.getComponentType();
                    if (Integer.TYPE.equals(componentType)) {
                        int[] primitiveValues = new int[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Long)values[i]).intValue();
                        }
                        value = primitiveValues;
                    } else if (Short.TYPE.equals(componentType)) {
                        short[] primitiveValues = new short[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Long)values[i]).shortValue();
                        }
                        value = primitiveValues;
                    } else if (Long.TYPE.equals(componentType)) {
                        long[] primitiveValues = new long[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Long)values[i]).longValue();
                        }
                        value = primitiveValues;
                    } else if (Double.TYPE.equals(componentType)) {
                        double[] primitiveValues = new double[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Double)values[i]).doubleValue();
                        }
                        value = primitiveValues;
                    } else if (Float.TYPE.equals(componentType)) {
                        float[] primitiveValues = new float[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Double)values[i]).floatValue();
                        }
                        value = primitiveValues;
                    } else if (Boolean.TYPE.equals(componentType)) {
                        boolean[] primitiveValues = new boolean[values.length];
                        for (int i = 0; i != values.length; ++i) {
                            primitiveValues[i] = ((Boolean)values[i]).booleanValue();
                        }
                        value = primitiveValues;
                    } else {
                        value = values;
                    }
                } else {
                    value = factory.create(property.getFirstValue());
                    // Convert to the correct primitive, if needed ...
                    if (Integer.TYPE.equals(paramType)) {
                        value = new Integer(((Long)value).intValue());
                    } else if (Short.TYPE.equals(paramType)) {
                        value = new Short(((Long)value).shortValue());
                    } else if (Float.TYPE.equals(paramType)) {
                        value = new Float(((Double)value).floatValue());
                    }
                }
                // Invoke the method ...
                String msg = "Setting property {0} to {1} on source at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).trace(msg,
                                                   javaPropertyName,
                                                   value,
                                                   path,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
                setter.invoke(source, value);
            } catch (SecurityException err) {
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", source.getClass(), setter);
            } catch (IllegalArgumentException err) {
                // Do nothing ... assume not a JavaBean property (but log)
                String msg = "Invalid argument invoking {0} with parameter {1} on source at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).debug(err,
                                                   msg,
                                                   setter,
                                                   value,
                                                   path,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
            } catch (IllegalAccessException err) {
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", source.getClass(), setter);
            } catch (InvocationTargetException err) {
                // Do nothing ... assume not a JavaBean property (but log)
                String msg = "Error invoking {0} with parameter {1} on source at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).debug(err.getTargetException(),
                                                   msg,
                                                   setter,
                                                   value,
                                                   path,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
            }
        }
        return source;
    }

    protected boolean setBeanPropertyIfExistsAndNotSet( Object target,
                                                        String propertyName,
                                                        Object value ) {
        Reflection reflection = new Reflection(target.getClass());
        try {
            if (reflection.invokeGetterMethodOnTarget(propertyName, target) == null) {
                reflection.invokeSetterMethodOnTarget(propertyName, target, value);
                return true;
            }
            return false;
        } catch (Exception e) {
            // Log that the property was not found ...
            Logger.getLogger(getClass())
                  .debug("Unknown property '{0}' on '{1}' class", propertyName, target.getClass().getName());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observer#notify(org.jboss.dna.graph.observe.Changes)
     */
    public void notify( Changes changes ) {
        // Forward the changes to the net change observer ...
        this.configurationChangeObserver.notify(changes);
    }

    protected class ConfigurationChangeObserver extends NetChangeObserver {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.observe.NetChangeObserver#notify(org.jboss.dna.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( NetChanges netChanges ) {
            if (!getConfigurationSourceName().equals(netChanges.getSourceName())) return;
            for (NetChange change : netChanges.getNetChanges()) {
                if (!getConfigurationWorkspaceName().equals(change.getRepositoryWorkspaceName())) return;
                Path changedPath = change.getPath();
                Path configPath = getPathToConfigurationRoot();
                if (!changedPath.isAtOrBelow(getPathToConfigurationRoot())) return;
                boolean changedNodeIsPotentiallySource = configPath.size() + 1 == changedPath.size();

                // At this point, we know that something inside the configuration changed, so figure out what happened ...
                if (changedNodeIsPotentiallySource && change.includes(ChangeType.NODE_REMOVED)) {
                    // Then potentially a source with the supplied name has been removed ...
                    String sourceName = changedPath.getLastSegment().getName().getLocalName();
                    getRepositoryLibrary().removeSource(sourceName);
                } else {
                    // The add/change/remove is either at or below a source, so try to create a new source for it ...
                    Path sourcePath = changedNodeIsPotentiallySource ? changedPath : changedPath.subpath(0, configPath.size() + 1);
                    Problems problems = new SimpleProblems();
                    // Now read the node and create the source ...
                    Graph graph = Graph.create(getConfigurationSourceName(), getRepositoryLibrary(), getExecutionEnvironment());
                    try {
                        String workspaceName = getConfigurationWorkspaceName();
                        if (workspaceName != null) graph.useWorkspace(workspaceName);
                        Map<Name, Property> properties = graph.getPropertiesByName().on(sourcePath);
                        RepositorySource source = createRepositorySource(sourcePath, properties, problems);
                        if (source != null) {
                            // It was the config for a source, so try to add or replace an existing source ...
                            getRepositoryLibrary().addSource(source, true);
                        }
                    } catch (PathNotFoundException e) {
                        // No source was found, and this is okay (since it may just been deleted)...
                        String sourceName = changedPath.getLastSegment().getName().getLocalName();
                        getRepositoryLibrary().removeSource(sourceName);
                    }
                }
            }
        }
    }
}

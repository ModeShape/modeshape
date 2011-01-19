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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.NetChangeObserver;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.request.CollectGarbageRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.repository.service.AbstractServiceAdministrator;
import org.modeshape.repository.service.AdministeredService;
import org.modeshape.repository.service.ServiceAdministrator;

/**
 * A service that manages the {@link RepositorySource}es defined within a configuration repository.
 */
@ThreadSafe
public class RepositoryService implements AdministeredService, Observer {

    public static final int MAXIMUM_NUMBER_OF_PASSES_PER_GC_RUN = 10;

    /**
     * The administrative component for this service.
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.repositoryServiceName, State.PAUSED);
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
         * @see org.modeshape.repository.service.AbstractServiceAdministrator#doShutdown(org.modeshape.repository.service.ServiceAdministrator.State)
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            shutdownService();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.repository.service.ServiceAdministrator#awaitTermination(long, java.util.concurrent.TimeUnit)
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) {
            return true;
        }
    }

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
     * @param observationBus the {@link ObservationBus} instance that should be used for changes in the sources
     * @param problems the {@link Problems} instance that this service should use to report problems starting repositories
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public RepositoryService( RepositorySource configurationSource,
                              String configurationWorkspaceName,
                              Path pathToConfigurationRoot,
                              ExecutionContext context,
                              ObservationBus observationBus,
                              Problems problems ) {
        CheckArg.isNotNull(configurationSource, "configurationSource");
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(observationBus, "observationBus");
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        if (pathToConfigurationRoot == null) pathToConfigurationRoot = pathFactory.create("/dna:system");
        if (problems == null) problems = new SimpleProblems();
        Path sourcesPath = pathFactory.create(pathToConfigurationRoot, ModeShapeLexicon.SOURCES);

        this.sources = new RepositoryLibrary(configurationSource, configurationWorkspaceName, sourcesPath, context,
                                             observationBus);
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
                                                                                         ModeShapeLexicon.SOURCES);
            try {
                String workspaceName = getConfigurationWorkspaceName();
                if (workspaceName != null) graph.useWorkspace(workspaceName);

                Subgraph sourcesGraph = graph.getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at(pathToSourcesNode);

                // Iterate over each of the children, and create the RepositorySource ...
                for (Location location : sourcesGraph.getRoot().getChildren()) {
                    sources.addSource(createRepositorySource(sourcesGraph, location, problems));
                }
            } catch (PathNotFoundException e) {
                // No sources were found, and this is okay!
            } catch (Throwable err) {
                throw new ModeShapeConfigurationException(RepositoryI18n.errorStartingRepositoryService.text(), err);
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
     * @param subgraph the subgraph containing the configuration information for this {@link RepositorySource}
     * @param location the location of the properties to apply to the new {@link RepositorySource}
     * @param problems the problems container in which any problems should be reported; never null
     * @return the repository source instance, or null if it could not be created
     */
    protected RepositorySource createRepositorySource( Subgraph subgraph,
                                                       Location location,
                                                       Problems problems ) {
        return (RepositorySource)createInstanceFromProperties(subgraph, location, problems, true);
    }

    /**
     * Instantiate the {@link Object} described by the supplied properties.
     * 
     * @param subgraph the subgraph containing the configuration information for this instance
     * @param location the location of the properties to apply to the new instance
     * @param problems the problems container in which any problems should be reported; never null
     * @param mustHaveClassName indicates that the properties must include a class name; if true a problem will be added for
     *        instances that do not have a class name specified
     * @return the instance, or null if it could not be created
     */
    protected Object createInstanceFromProperties( Subgraph subgraph,
                                                   Location location,
                                                   Problems problems,
                                                   boolean mustHaveClassName ) {
        ValueFactories valueFactories = context.getValueFactories();
        ValueFactory<String> stringFactory = valueFactories.getStringFactory();

        Node node = subgraph.getNode(location);
        assert location.hasPath();
        Path path = node.getLocation().getPath();
        Map<Name, Property> properties = node.getPropertiesByName();

        // Get the classname and classpath ...
        Property classnameProperty = properties.get(ModeShapeLexicon.CLASSNAME);
        Property classpathProperty = properties.get(ModeShapeLexicon.CLASSPATH);
        if (classnameProperty == null) {
            if (mustHaveClassName) {
                problems.addError(RepositoryI18n.requiredPropertyIsMissingFromNode, ModeShapeLexicon.CLASSNAME, path);
            }
            return null;
        }
        // If the classpath property is null or empty, the default classpath will be used
        if (problems.hasErrors()) return null;

        // Create the instance ...
        String classname = stringFactory.create(classnameProperty.getValues().next());
        String[] classpath = classpathProperty == null ? new String[] {} : stringFactory.create(classpathProperty.getValuesAsArray());
        ClassLoader classLoader = context.getClassLoader(classpath);
        Object instance = null;
        try {
            Class<?> sourceClass = classLoader.loadClass(classname);
            instance = sourceClass.newInstance();
        } catch (ClassNotFoundException err) {
            problems.addError(err, RepositoryI18n.unableToLoadClassUsingClasspath, classname, classpath);
        } catch (IllegalAccessException err) {
            problems.addError(err, RepositoryI18n.unableToAccessClassUsingClasspath, classname, classpath);
        } catch (Throwable err) {
            problems.addError(err, RepositoryI18n.unableToInstantiateClassUsingClasspath, classname, classpath);
        }
        if (instance == null) return null;

        // We need to set the name using the local name of the node...
        Property nameProperty = context.getPropertyFactory().create(JcrLexicon.NAME,
                                                                    path.getLastSegment().getName().getLocalName());
        properties.put(JcrLexicon.NAME, nameProperty);

        // Attempt to set the configuration information as bean properties,
        // if they exist on the object and are not already set to some value ...
        setBeanPropertyIfExistsAndNotSet(instance, "configurationSourceName", getConfigurationSourceName());
        setBeanPropertyIfExistsAndNotSet(instance, "configurationWorkspaceName", getConfigurationWorkspaceName());
        setBeanPropertyIfExistsAndNotSet(instance, "configurationPath", stringFactory.create(path));

        // Now set all the properties that we can, ignoring any property that doesn't fit the pattern ...
        Reflection reflection = new Reflection(instance.getClass());
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
                setter.invoke(instance, value);
            } catch (SecurityException err) {
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", instance.getClass(), setter);
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
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", instance.getClass(), setter);
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

        // Check for nested instances in the configuration
        for (Location childLocation : node.getChildren()) {
            assert childLocation.hasPath();
            Path childPath = childLocation.getPath();
            Name childName = childPath.getLastSegment().getName();

            Object value = createInstanceFromProperties(subgraph, childLocation, problems, false);
            if (problems.hasErrors()) {
                return null;
            }

            String javaPropertyName = childName.getLocalName();
            Method setter = reflection.findFirstMethod("set" + javaPropertyName, false);
            if (setter == null) continue;

            try {
                setter.invoke(instance, value);
                // Invoke the method ...
                String msg = "Setting property {0} to {1} on object at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).trace(msg,
                                                   javaPropertyName,
                                                   value,
                                                   childPath,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
                setter.invoke(instance, value);
            } catch (SecurityException err) {
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", instance.getClass(), setter);
            } catch (IllegalArgumentException err) {
                // Do nothing ... assume not a JavaBean property (but log)
                String msg = "Invalid argument invoking {0} with parameter {1} on object at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).debug(err,
                                                   msg,
                                                   setter,
                                                   value,
                                                   childPath,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
            } catch (IllegalAccessException err) {
                Logger.getLogger(getClass()).debug(err, "Error invoking {0}.{1}", instance.getClass(), setter);
            } catch (InvocationTargetException err) {
                // Do nothing ... assume not a JavaBean property (but log)
                String msg = "Error invoking {0} with parameter {1} on source at {2} in configuration repository {3} in workspace {4}";
                Logger.getLogger(getClass()).debug(err.getTargetException(),
                                                   msg,
                                                   setter,
                                                   value,
                                                   childPath,
                                                   configurationSourceName,
                                                   configurationWorkspaceName);
            }

        }

        return instance;

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
     * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
     */
    public void notify( Changes changes ) {
        // Forward the changes to the net change observer ...
        this.configurationChangeObserver.notify(changes);
    }

    /**
     * Determine if at least one source requires periodic garbage collection.
     * 
     * @return true if {@link #runGarbageCollection(Problems)} should be called periodically, or false otherwise
     */
    public boolean requiresGarbageCollection() {
        for (RepositorySource source : getRepositoryLibrary().getSources()) {
            if (!source.getCapabilities().supportsAutomaticGarbageCollection()) return true;
        }
        return false;
    }

    /**
     * This method goes through all {@link RepositorySource} instances in the {@link #getRepositoryLibrary() library}, and for
     * each of them that don't {@link RepositorySourceCapabilities#supportsAutomaticGarbageCollection() support automatic garbage
     * collection} will submit a {@link CollectGarbageRequest}.
     * <p>
     * This method does all this work in the calling thread, blocking until all such requests have been issued and completed. It
     * actually uses a queue, first enqueuing all RepositorySource instances that don't
     * {@link RepositorySourceCapabilities#supportsAutomaticGarbageCollection() support automatic garbage collection}. It then
     * pulls the first source from the queue, obtains a connection, submits a single {@link CollectGarbageRequest}, and
     * re-enqueues the source {@link CollectGarbageRequest#isAdditionalPassRequired() if required}. However, this method never
     * requests a source collect garbage more than {@link #MAXIMUM_NUMBER_OF_PASSES_PER_GC_RUN} times.
     * </p>
     * <p>
     * Thus a source can implement a garbage collection sweep in a manner that does not require excess amount of time so as to not
     * block other requests. After that pass is completed, the source can simply denote in the CollectGarbageRequest whether at
     * least one additional GC pass should be performed.
     * </p>
     * 
     * @param problems the problems container in which any errors should be reported; if null, then any problems will be logged
     */
    public void runGarbageCollection( Problems problems ) {
        final Logger logger = Logger.getLogger(getClass());
        Queue<GarbageCollectedSource> sourcesToGc = new LinkedList<GarbageCollectedSource>();
        for (RepositorySource source : getRepositoryLibrary().getSources()) {
            if (source.getCapabilities().supportsAutomaticGarbageCollection()) continue;
            sourcesToGc.add(new GarbageCollectedSource(source));
        }

        while (!sourcesToGc.isEmpty()) {
            GarbageCollectedSource gcSource = sourcesToGc.poll();
            RepositorySource source = gcSource.source;
            // Get a connection for this source ...
            RepositoryConnection connection = getRepositoryLibrary().createConnection(source.getName());
            try {
                // And request garbage collection ...
                logger.debug("Garbage collection requested for {0}", source.getName());
                CollectGarbageRequest request = new CollectGarbageRequest();
                connection.execute(context, request);
                gcSource.recordPass();
                if (request.isAdditionalPassRequired() && gcSource.hasPassesRemaining()) {
                    // This pass was not complete, so try to enqueue again ...
                    sourcesToGc.offer(gcSource);
                    logger.debug("Garbage collection partially completed for {0}; enqueuing again", source.getName());
                } else {
                    logger.debug("Garbage collection completed for {0}", source.getName());
                }
            } catch (Throwable t) {
                // Record this error and continue with the next source ...
                I18n msg = RepositoryI18n.errorCollectingGarbageInSource;
                if (problems != null) {
                    problems.addError(t, msg, source.getName(), t.getMessage());
                } else {
                    logger.error(msg, source.getName(), t.getMessage());
                }
            } finally {
                // Always close this connection after each pass ...
                connection.close();
            }
        }
    }

    protected static class GarbageCollectedSource {
        protected final RepositorySource source;
        private int passesRemaining = MAXIMUM_NUMBER_OF_PASSES_PER_GC_RUN;

        protected GarbageCollectedSource( RepositorySource source ) {
            this.source = source;
        }

        protected void recordPass() {
            --passesRemaining;
        }

        protected boolean hasPassesRemaining() {
            return passesRemaining > 0;
        }
    }

    protected class ConfigurationChangeObserver extends NetChangeObserver {

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.NetChangeObserver#notify(org.modeshape.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( NetChanges netChanges ) {
            if (getConfigurationWorkspaceName() == null) {
                // This was a transient configuration source, so it should never change ...
                return;
            }
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
                        Subgraph subgraph = graph.getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at(sourcePath);
                        RepositorySource source = createRepositorySource(subgraph, Location.create(sourcePath), problems);
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

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
package org.modeshape.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.common.util.Logger.Level;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrMixLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.mimetype.MimeTypeDetectorConfig;
import org.modeshape.graph.mimetype.MimeTypeDetectors;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathExpression;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.repository.cluster.ClusteringConfig;
import org.modeshape.repository.cluster.ClusteringService;
import org.modeshape.repository.sequencer.SequencerConfig;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * A single instance of the ModeShape services, which is obtained after setting up the {@link ModeShapeConfiguration#build()
 * configuration}.
 * 
 * @see ModeShapeConfiguration
 */
@Immutable
public class ModeShapeEngine {

    /**
     * The default interval (in seconds) for running the garbage collection sweeps, if there are sources require it. The default
     * value is 600 seconds, or 10 minutes.
     */
    public static final int DEFAULT_GARBAGE_COLLECTION_INTERVAL_IN_SECONDS = 60 * 10; // every ten minutes

    public static final String CONFIGURATION_REPOSITORY_NAME = "dna:configuration";

    protected final ModeShapeConfiguration.ConfigurationDefinition configuration;
    private final ConfigurationScanner scanner;
    private final Problems problems;
    protected final ExecutionContext context;

    private final RepositoryService repositoryService;
    private final SequencingService sequencingService;
    private final ExecutorService executorService;
    private final ClusteringService clusteringService;
    private final MimeTypeDetectors detectors;
    private final String engineId = UUID.randomUUID().toString();
    private final Logger logger;

    /**
     * Provides the ability to collect garbage periodically
     */
    private final ScheduledExecutorService gcService = new ScheduledThreadPoolExecutor(1);

    protected ModeShapeEngine( ExecutionContext context,
                               ModeShapeConfiguration.ConfigurationDefinition configuration ) {
        this.problems = new SimpleProblems();
        this.logger = Logger.getLogger(getClass());

        // Use the configuration's context ...
        this.detectors = new MimeTypeDetectors();
        this.context = context.with(detectors).with(engineId);

        // And set up the scanner ...
        this.configuration = configuration;
        this.scanner = new ConfigurationScanner(this.problems, this.context, this.configuration);

        // Add the mime type detectors in the configuration ...
        for (MimeTypeDetectorConfig config : scanner.getMimeTypeDetectors()) {
            detectors.addDetector(config);
        }
        // Add an extension-based detector by default ...
        detectors.addDetector(new MimeTypeDetectorConfig("ExtensionDetector", "Extension-based MIME type detector",
                                                         ExtensionBasedMimeTypeDetector.class));

        // Create the clustering service ...
        ClusteringConfig clusterConfig = scanner.getClusteringConfiguration();
        clusteringService = new ClusteringService();
        clusteringService.setExecutionContext(context);
        clusteringService.setClusteringConfig(clusterConfig);

        // Create the RepositoryService, pointing it to the configuration repository ...
        Path pathToConfigurationRoot = this.configuration.getPath();
        String configWorkspaceName = this.configuration.getWorkspace();
        RepositorySource configSource = this.configuration.getRepositorySource();
        repositoryService = new RepositoryService(configSource, configWorkspaceName, pathToConfigurationRoot, context,
                                                  clusteringService, problems);

        // Create the executor service (which starts out with 0 threads, so it's okay to do here) ...
        ThreadFactory threadPoolFactory = new NamedThreadFactory(configuration.getName());
        executorService = Executors.newCachedThreadPool(threadPoolFactory);

        // Create the sequencing service ...
        sequencingService = new SequencingService();
        sequencingService.setExecutionContext(context);
        sequencingService.setExecutorService(executorService);
        sequencingService.setRepositoryLibrary(repositoryService.getRepositoryLibrary());
        for (SequencerConfig sequencerConfig : scanner.getSequencingConfigurations()) {
            sequencingService.addSequencer(sequencerConfig);
        }

        // The rest of the instantiation/configuration will be done in start()
    }

    protected Logger logger() {
        return logger;
    }

    /**
     * Get the problems that were encountered when setting up this engine from the configuration.
     * 
     * @return the problems, which may be empty but will never be null
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * Get the context in which this engine is executing.
     * 
     * @return the execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * Get the {@link RepositorySource} instance used by this engine.
     * 
     * @param repositoryName the name of the repository source
     * @return the source, or null if no source with the given name exists
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final RepositorySource getRepositorySource( String repositoryName ) {
        checkRunning();
        return repositoryService.getRepositoryLibrary().getSource(repositoryName);
    }

    /**
     * Get a factory of connections, backed by the RepositorySor
     * 
     * @return the connection factory; never null
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final RepositoryConnectionFactory getRepositoryConnectionFactory() {
        checkRunning();
        return repositoryService.getRepositoryLibrary();
    }

    /**
     * Get the repository service.
     * 
     * @return the repository service owned by this engine; never null
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final RepositoryService getRepositoryService() {
        checkRunning();
        return repositoryService;
    }

    /**
     * Get a graph to the underlying source.
     * 
     * @param sourceName the name of the source
     * @return the graph
     * @throws IllegalArgumentException if the source name is null
     * @throws RepositorySourceException if a source with the supplied name does not exist
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final Graph getGraph( String sourceName ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        return getGraph(getExecutionContext(), sourceName);
    }

    /**
     * Get a graph to the underlying source, using the supplied context. Note that the supplied context should be a derivative of
     * the engine's {@link #getExecutionContext() context}.
     * 
     * @param context the context of execution for this graph; may not be null
     * @param sourceName the name of the source
     * @return the graph
     * @throws IllegalArgumentException if the context or source name are null
     * @throws RepositorySourceException if a source with the supplied name does not exist
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final Graph getGraph( ExecutionContext context,
                                 String sourceName ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(sourceName, "sourceName");
        checkRunning();
        Graph graph = Graph.create(sourceName, getRepositoryService().getRepositoryLibrary(), context);
        if (configuration.getRepositorySource().getName().equals(sourceName) && configuration.getWorkspace() != null) {
            // set the workspace ...
            graph.useWorkspace(configuration.getWorkspace());
        }
        return graph;
    }

    /**
     * Get the sequencing service.
     * 
     * @return the sequencing service owned by this engine; never null
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final SequencingService getSequencingService() {
        checkRunning();
        return sequencingService;
    }

    /**
     * Return the component that is able to detect MIME types given the name of a stream and a stream.
     * 
     * @return the MIME type detector used by this engine; never null
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    protected final MimeTypeDetector getMimeTypeDetector() {
        checkRunning();
        return detectors;
    }

    protected final boolean checkRunning() {
        if (repositoryService.getAdministrator().isStarted() && sequencingService.getAdministrator().isStarted()) {
            return true;
        }
        throw new IllegalStateException(RepositoryI18n.engineIsNotRunning.text());
    }

    /**
     * Check whether there are any problems that would prevent startup. Any warnings or errors will be logged, and this method
     * will throw a {@link ModeShapeConfigurationException} if there is at least one error.
     * 
     * @throws ModeShapeConfigurationException if there is at least one error
     */
    protected void checkProblemsOnStartup() throws ModeShapeConfigurationException {
        boolean errors = problems.hasErrors();
        if (errors || problems.hasWarnings()) {
            // First log the messages ...
            Level level = errors ? Level.ERROR : Level.WARNING;
            logger().log(level, RepositoryI18n.warningsWhileStarting, problems.size());
            for (Problem problem : getProblems()) {
                Throwable t = problem.getThrowable(); // may be null
                Level logLevel = problem.getStatus().getLogLevel();
                logger().log(logLevel, t, problem.getMessage(), problem.getParameters());
            }
        }
        if (errors) {
            // Then throw an exception ...
            throw newConfigurationException(RepositoryI18n.errorsPreventStarting.text(problems.size()));
        }
    }

    /**
     * Construct a new ModeShapeConfigurationException. This method can be overridden by subclasses when a subclass of
     * {@link ModeShapeConfigurationException} is to be thrown from {@link #start()}.
     * 
     * @param msg the message
     * @return the exception; may not be null
     */
    protected ModeShapeConfigurationException newConfigurationException( String msg ) {
        throw new ModeShapeConfigurationException(msg);
    }

    /**
     * Check the configuration given by the supplied graph.
     * 
     * @param configuration the configuration subgraph
     */
    protected void checkConfiguration( Subgraph configuration ) {
    }

    /**
     * Start this engine to make it available for use.
     * 
     * @throws IllegalStateException if this method is called when already shut down.
     * @throws ModeShapeConfigurationException if there is an error in the configuration or any of the services that prevents
     *         proper startup
     * @see #shutdown()
     */
    public void start() {
        // Check whether there are problems BEFORE startup ...
        checkProblemsOnStartup();

        // Create the RepositoryContext that the configuration repository source should use ...
        RepositoryContext configContext = new SimpleRepositoryContext(context, clusteringService, null);
        configuration.getRepositorySource().initialize(configContext);
        try {
            checkConfiguration(configuration.graph().getSubgraphOfDepth(10).at(configuration.getPath()));
        } catch (RuntimeException e) {
            problems.addError(e, RepositoryI18n.errorVerifyingConfiguration, e.getLocalizedMessage());
        }
        checkProblemsOnStartup();

        // Start the various services ...
        clusteringService.getAdministrator().start();
        repositoryService.getAdministrator().start();
        sequencingService.getAdministrator().start();

        // Now register the repository service to be notified of changes to the configuration ...
        clusteringService.register(repositoryService);

        // Start the periodic GC service
        startGcService();

        // Check whether there are problems AFTER startup ...
        checkProblemsOnStartup();
    }

    protected void startGcService() {
        if (!getRepositoryService().requiresGarbageCollection()) return;

        // Read the garbage collection interface from the configuration ...
        long intervalInSeconds = DEFAULT_GARBAGE_COLLECTION_INTERVAL_IN_SECONDS;
        Property intervalProp = configuration.getGlobalProperty(ModeShapeLexicon.GARBAGE_COLLECTION_INTERVAL);
        if (intervalProp != null && !intervalProp.isEmpty()) {
            try {
                // This may throw an exception if the value is not a valid long ...
                intervalInSeconds = context.getValueFactories().getLongFactory().create(intervalProp.getFirstValue());
            } catch (RuntimeException e) {
                String actualValue = context.getValueFactories().getStringFactory().create(intervalProp.getFirstValue());
                problems.addError(RepositoryI18n.unableToUseGarbageCollectionIntervalValue, actualValue);
            }
        }

        final Runnable gcTask = new Runnable() {
            public void run() {
                getRepositoryService().runGarbageCollection(null); // log problems as errors
            }
        };
        try {
            gcService.scheduleAtFixedRate(gcTask, intervalInSeconds, intervalInSeconds, TimeUnit.SECONDS);
            checkProblemsOnStartup();
        } catch (RuntimeException e) {
            try {
                shutdown();
            } catch (Throwable t) {
                // Don't care about these ...
            }
            throw e;
        }
    }

    /**
     * Shutdown this engine to close all connections, terminate any ongoing background operations (such as sequencing), and
     * reclaim any resources that were acquired by this engine. This method may be called multiple times, but only the first time
     * has an effect.
     * 
     * @see #start()
     */
    public void shutdown() {
        preShutdown();
        postShutdown();
    }

    protected void preShutdown() {
        // Terminate the garbage collection thread ...
        gcService.shutdown();

        // Terminate the executor service, which may be running background jobs that are not yet completed
        // and which will prevent new jobs being submitted (to the sequencing service) ...
        executorService.shutdown();

        // Next, shutdown the sequencing service, which will prevent any additional jobs from going through ...
        sequencingService.getAdministrator().shutdown();

        // Shut down the repository source, which closes all connections ...
        repositoryService.getAdministrator().shutdown();
    }

    protected void postShutdown() {
        // Finally shut down the clustering service ...
        clusteringService.shutdown();
    }

    /**
     * Blocks until the shutdown has completed, or the timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     * 
     * @param timeout the maximum time to wait for each component in this engine
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if this service complete shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        if (!gcService.awaitTermination(timeout, unit)) return false;
        if (!sequencingService.getAdministrator().awaitTermination(timeout, unit)) return false;
        if (!executorService.awaitTermination(timeout, unit)) return false;
        if (!repositoryService.getAdministrator().awaitTermination(timeout, unit)) return false;
        return true;
    }

    /**
     * Get a graph to the configuration content.
     * 
     * @return a graph to the configuration content
     */
    protected Graph getConfigurationGraph() {
        Graph result = Graph.create(configuration.getRepositorySource(), context);
        if (configuration.getWorkspace() != null) {
            result.useWorkspace(configuration.getWorkspace());
        }
        return result;
    }

    /**
     * The component responsible for reading the configuration repository and (eventually) for propagating changes in the
     * configuration repository into the services.
     */
    protected class ConfigurationScanner {
        /**
         * The name of the {@link ObservationBus} implementation class that will be used when clustering. This class will be
         * loaded reflectively (so that this library doesn't always require the clustering dependencies).
         */
        protected static final String CLUSTERED_OBSERVATION_BUS_CLASSNAME = "org.modeshape.clustering.ClusteredObservationBus";

        private final Problems problems;
        private final ExecutionContext context;
        private final ModeShapeConfiguration.ConfigurationDefinition configurationRepository;

        protected ConfigurationScanner( Problems problems,
                                        ExecutionContext context,
                                        ModeShapeConfiguration.ConfigurationDefinition configurationRepository ) {
            this.problems = problems;
            this.context = context;
            this.configurationRepository = configurationRepository;
        }

        public List<MimeTypeDetectorConfig> getMimeTypeDetectors() {
            List<MimeTypeDetectorConfig> detectors = new ArrayList<MimeTypeDetectorConfig>();
            Graph graph = Graph.create(configurationRepository.getRepositorySource(), context);
            Path pathToSequencersNode = context.getValueFactories().getPathFactory().create(configurationRepository.getPath(),
                                                                                            ModeShapeLexicon.MIME_TYPE_DETECTORS);
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(2).at(pathToSequencersNode);

                Set<Name> skipProperties = new HashSet<Name>();
                skipProperties.add(ModeShapeLexicon.READABLE_NAME);
                skipProperties.add(ModeShapeLexicon.DESCRIPTION);
                skipProperties.add(ModeShapeLexicon.CLASSNAME);
                skipProperties.add(ModeShapeLexicon.CLASSPATH);
                skipProperties.add(ModeShapeLexicon.PATH_EXPRESSION);
                Set<String> skipNamespaces = new HashSet<String>();
                skipNamespaces.add(JcrLexicon.Namespace.URI);
                skipNamespaces.add(JcrNtLexicon.Namespace.URI);
                skipNamespaces.add(JcrMixLexicon.Namespace.URI);

                for (Location detectorLocation : subgraph.getRoot().getChildren()) {
                    Node node = subgraph.getNode(detectorLocation);
                    String name = stringValueOf(node, ModeShapeLexicon.READABLE_NAME);
                    if (name == null) name = stringValueOf(node);
                    String desc = stringValueOf(node, ModeShapeLexicon.DESCRIPTION);
                    String classname = stringValueOf(node, ModeShapeLexicon.CLASSNAME);
                    String[] classpath = stringValuesOf(node, ModeShapeLexicon.CLASSPATH);
                    Map<String, Object> properties = new HashMap<String, Object>();
                    for (Property property : node.getProperties()) {
                        Name propertyName = property.getName();
                        if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                        if (skipProperties.contains(propertyName)) continue;
                        if (property.isSingle()) {
                            properties.put(propertyName.getLocalName(), property.getFirstValue());
                        } else {
                            properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                        }
                    }
                    MimeTypeDetectorConfig config = new MimeTypeDetectorConfig(name, desc, properties, classname, classpath);
                    detectors.add(config);
                }
            } catch (PathNotFoundException e) {
                // no detectors registered ...
            }
            return detectors;
        }

        public ClusteringConfig getClusteringConfiguration() {
            Graph graph = Graph.create(configurationRepository.getRepositorySource(), context);
            Path pathToClusteringNode = context.getValueFactories().getPathFactory().create(configurationRepository.getPath(),
                                                                                            ModeShapeLexicon.CLUSTERING);
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(2).at(pathToClusteringNode);

                Set<Name> skipProperties = new HashSet<Name>();
                skipProperties.add(ModeShapeLexicon.DESCRIPTION);
                skipProperties.add(ModeShapeLexicon.CLASSNAME);
                skipProperties.add(ModeShapeLexicon.CLASSPATH);
                Set<String> skipNamespaces = new HashSet<String>();
                skipNamespaces.add(JcrLexicon.Namespace.URI);
                skipNamespaces.add(JcrNtLexicon.Namespace.URI);
                skipNamespaces.add(JcrMixLexicon.Namespace.URI);

                Node clusterNode = subgraph.getRoot();
                // String name = stringValueOf(clusterNode);
                String clusterName = stringValueOf(clusterNode, ModeShapeLexicon.CLUSTER_NAME);
                String desc = stringValueOf(clusterNode, ModeShapeLexicon.DESCRIPTION);
                String classname = stringValueOf(clusterNode, ModeShapeLexicon.CLASSNAME);
                String[] classpath = stringValuesOf(clusterNode, ModeShapeLexicon.CLASSPATH);
                if (classname == null || classname.trim().length() == 0) {
                    classname = CLUSTERED_OBSERVATION_BUS_CLASSNAME;
                }
                if (clusterName == null || clusterName.trim().length() == 0) {
                    logger().warn(RepositoryI18n.clusteringConfigurationRequiresClusterName);
                    problems.addWarning(RepositoryI18n.clusteringConfigurationRequiresClusterName);
                    return null; // Signifies no clustering
                }

                Map<String, Object> properties = new HashMap<String, Object>();
                for (Property property : clusterNode.getProperties()) {
                    Name propertyName = property.getName();
                    if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                    if (skipProperties.contains(propertyName)) continue;
                    if (property.isSingle()) {
                        properties.put(propertyName.getLocalName(), property.getFirstValue());
                    } else {
                        properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                    }
                }
                return new ClusteringConfig(clusterName, desc, properties, classname, classpath);
            } catch (PathNotFoundException e) {
                // no detectors registered ...
            }
            return null;
        }

        public List<SequencerConfig> getSequencingConfigurations() {
            List<SequencerConfig> configs = new ArrayList<SequencerConfig>();
            Graph graph = Graph.create(configurationRepository.getRepositorySource(), context);
            Path pathToSequencersNode = context.getValueFactories().getPathFactory().create(configurationRepository.getPath(),
                                                                                            ModeShapeLexicon.SEQUENCERS);
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(2).at(pathToSequencersNode);

                Set<Name> skipProperties = new HashSet<Name>();
                skipProperties.add(ModeShapeLexicon.READABLE_NAME);
                skipProperties.add(ModeShapeLexicon.DESCRIPTION);
                skipProperties.add(ModeShapeLexicon.CLASSNAME);
                skipProperties.add(ModeShapeLexicon.CLASSPATH);
                skipProperties.add(ModeShapeLexicon.PATH_EXPRESSION);
                Set<String> skipNamespaces = new HashSet<String>();
                skipNamespaces.add(JcrLexicon.Namespace.URI);
                skipNamespaces.add(JcrNtLexicon.Namespace.URI);
                skipNamespaces.add(JcrMixLexicon.Namespace.URI);

                for (Location sequencerLocation : subgraph.getRoot().getChildren()) {
                    Node sequencerNode = subgraph.getNode(sequencerLocation);
                    String name = stringValueOf(sequencerNode, ModeShapeLexicon.READABLE_NAME);
                    if (name == null) name = stringValueOf(sequencerNode);
                    String desc = stringValueOf(sequencerNode, ModeShapeLexicon.DESCRIPTION);
                    String classname = stringValueOf(sequencerNode, ModeShapeLexicon.CLASSNAME);
                    String[] classpath = stringValuesOf(sequencerNode, ModeShapeLexicon.CLASSPATH);
                    String[] expressionStrings = stringValuesOf(sequencerNode, ModeShapeLexicon.PATH_EXPRESSION);
                    List<PathExpression> pathExpressions = new ArrayList<PathExpression>();
                    if (expressionStrings != null) {
                        for (String expressionString : expressionStrings) {
                            try {
                                pathExpressions.add(PathExpression.compile(expressionString));
                            } catch (Throwable t) {
                                problems.addError(t,
                                                  RepositoryI18n.pathExpressionIsInvalidOnSequencer,
                                                  expressionString,
                                                  name,
                                                  t.getLocalizedMessage());
                            }
                        }
                    }
                    String[] goodExpressionStrings = new String[pathExpressions.size()];
                    for (int i = 0; i != pathExpressions.size(); ++i) {
                        PathExpression expression = pathExpressions.get(i);
                        goodExpressionStrings[i] = expression.getExpression();
                    }
                    Map<String, Object> properties = new HashMap<String, Object>();
                    for (Property property : sequencerNode.getProperties()) {
                        Name propertyName = property.getName();
                        if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                        if (skipProperties.contains(propertyName)) continue;
                        if (property.isSingle()) {
                            properties.put(propertyName.getLocalName(), property.getFirstValue());
                        } else {
                            properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                        }
                    }
                    SequencerConfig config = new SequencerConfig(name, desc, properties, classname, classpath,
                                                                 goodExpressionStrings);
                    configs.add(config);
                }
            } catch (PathNotFoundException e) {
                // no detectors registered ...
            }
            return configs;
        }

        private String stringValueOf( Node node ) {
            return node.getLocation().getPath().getLastSegment().getString(context.getNamespaceRegistry());
        }

        private String stringValueOf( Node node,
                                      Name propertyName ) {
            Property property = node.getProperty(propertyName);
            if (property == null) {
                // Check whether the property exists with no namespace ...
                property = node.getProperty(context.getValueFactories().getNameFactory().create(propertyName.getLocalName()));
                if (property == null) return null;
            }
            if (property.isEmpty()) return null;
            return context.getValueFactories().getStringFactory().create(property.getFirstValue());
        }

        private String[] stringValuesOf( Node node,
                                         Name propertyName ) {
            Property property = node.getProperty(propertyName);
            if (property == null) {
                // Check whether the property exists with no namespace ...
                property = node.getProperty(context.getValueFactories().getNameFactory().create(propertyName.getLocalName()));
                if (property == null) return null;
            }
            return context.getValueFactories().getStringFactory().create(property.getValuesAsArray());
        }

    }
}

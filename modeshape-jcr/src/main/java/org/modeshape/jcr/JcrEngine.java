/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.modeshape.cnd.CndImporter;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ComponentConfig;
import org.modeshape.common.component.ComponentLibrary;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrMixLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.xmlfile.XmlFileRepositorySource;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.basic.GraphNamespaceRegistry;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.repository.ModeShapeConfiguration;
import org.modeshape.repository.ModeShapeConfigurationException;
import org.modeshape.repository.ModeShapeEngine;

/**
 * The basic component that encapsulates the ModeShape services, including the {@link Repository} instances.
 */
@ThreadSafe
public class JcrEngine extends ModeShapeEngine implements Repositories {

    final static int LOCK_SWEEP_INTERVAL_IN_MILLIS = 30000;
    final static int LOCK_EXTENSION_INTERVAL_IN_MILLIS = LOCK_SWEEP_INTERVAL_IN_MILLIS * 2;

    private static final Logger log = Logger.getLogger(ModeShapeEngine.class);

    private final Map<String, JcrRepositoryHolder> repositories;
    private final Lock repositoriesLock;
    private final Map<String, Object> descriptors = new HashMap<String, Object>();
    private final ExecutorService repositoryStarterService;

    /**
     * Provides the ability to schedule lock clean-up
     */
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);

    JcrEngine( ExecutionContext context,
               ModeShapeConfiguration.ConfigurationDefinition configuration ) {
        super(context, configuration);
        this.repositories = new HashMap<String, JcrRepositoryHolder>();
        this.repositoriesLock = new ReentrantLock();
        initDescriptors();

        // Create an executor service that we'll use to start the repositories ...
        ThreadFactory threadFactory = new NamedThreadFactory("modeshape-start-repo");
        this.repositoryStarterService = Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * Clean up session-scoped locks created by session that are no longer active by iterating over the {@link JcrRepository
     * repositories} and calling their {@link RepositoryLockManager#cleanUpLocks() clean-up method}.
     * <p>
     * It should not be possible for a session to be terminated without cleaning up its locks, but this method will help clean-up
     * dangling locks should a session terminate abnormally.
     * </p>
     */
    void cleanUpLocks() {
        Collection<JcrRepositoryHolder> repos = null;

        try {
            // Make a copy of the repositories to minimize the time that the lock needs to be held.
            repositoriesLock.lock();
            repos = new ArrayList<JcrRepositoryHolder>(repositories.values());
        } finally {
            repositoriesLock.unlock();
        }

        for (JcrRepositoryHolder repository : repos) {
            repository.cleanUpLocks();
        }
    }

    @Override
    protected void preShutdown() {
        repositoryStarterService.shutdown();
        scheduler.shutdown();
        super.preShutdown();

        try {
            this.repositoriesLock.lock();
            // Shut down all of the repositories ...
            for (JcrRepositoryHolder holder : repositories.values()) {
                holder.close();
            }
            this.repositories.clear();
        } finally {
            this.repositoriesLock.unlock();
        }
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
    @Override
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        if (!scheduler.awaitTermination(timeout, unit)) return false;

        return super.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeEngine#checkConfiguration(org.modeshape.graph.Subgraph)
     */
    @Override
    protected void checkConfiguration( Subgraph configuration ) {
        super.checkConfiguration(configuration);

        // Get the list of sources ...
        Set<String> sourceNames = new HashSet<String>();
        for (Location child : configuration.getNode(ModeShapeLexicon.SOURCES)) {
            String name = child.getPath().getLastSegment().getName().getLocalName();
            sourceNames.add(name);
        }
        // Verify all of the repositories reference valid sources ...
        for (Location child : configuration.getNode(ModeShapeLexicon.REPOSITORIES)) {
            String repositoryName = readable(child.getPath().getLastSegment().getName());
            Node repositoryNode = configuration.getNode(child);
            Property property = repositoryNode.getProperty(ModeShapeLexicon.SOURCE_NAME);
            if (property == null) {
                getProblems().addError(JcrI18n.repositoryReferencesNonExistantSource, repositoryName, "null");
            } else {
                String sourceName = string(property.getFirstValue());
                if (!sourceNames.contains(sourceName)) {
                    getProblems().addError(JcrI18n.repositoryReferencesNonExistantSource, repositoryName, sourceName);
                }
            }
        }
    }

    /**
     * Start this engine to make it available for use.
     * 
     * @throws IllegalStateException if this method is called when already shut down.
     * @throws JcrConfigurationException if there is an error in the configuration or any of the services that prevents proper
     *         startup
     * @see #start(boolean)
     * @see #shutdown()
     */
    @Override
    public void start() {
        super.start();

        final JcrEngine engine = this;
        Runnable cleanUpTask = new Runnable() {

            public void run() {
                engine.cleanUpLocks();
            }

        };
        try {
            scheduler.scheduleAtFixedRate(cleanUpTask,
                                          LOCK_SWEEP_INTERVAL_IN_MILLIS,
                                          LOCK_SWEEP_INTERVAL_IN_MILLIS,
                                          TimeUnit.MILLISECONDS);
            checkProblemsOnStartup();
        } catch (RuntimeException e) {
            try {
                super.shutdown();
            } catch (Throwable t) {
                // Don't care about these ...
            }
            throw e;
        }
    }

    /**
     * Start this engine to make it available for use, and optionally start each of the repositories in the configuration. Any
     * errors starting the repositories will be logged as problems.
     * <p>
     * This method starts each repository in parallel, and returns only after all repositories have been started (or failed
     * startup).
     * </p>
     * 
     * @param validateRepositoryConfigs true if the configurations of each repository should be validated and each repository
     *        started/initialized, or false otherwise
     * @throws IllegalStateException if this method is called when already shut down.
     * @throws JcrConfigurationException if there is an error in the configuration or any of the services that prevents proper
     *         startup
     * @see #start()
     * @see #shutdown()
     */
    public void start( boolean validateRepositoryConfigs ) {
        start(validateRepositoryConfigs, -1, TimeUnit.SECONDS);
    }

    /**
     * Start this engine to make it available for use, and optionally start each of the repositories in the configuration. Any
     * errors starting the repositories will be logged as problems.
     * <p>
     * This method starts each repository in parallel, and returns after the supplied timeout or after all repositories have been
     * started (or failed startup), whichever comes first.
     * </p>
     * 
     * @param validateRepositoryConfigs true if the configurations of each repository should be validated and each repository
     *        started/initialized, or false otherwise
     * @param timeout the maximum time to wait; can be 0 or a positive number, but use a negative number to wait indefinitely
     *        until all repositories are started (or failed)
     * @param timeoutUnit the time unit of the {@code timeout} argument; may not be null, but ignored if <code>timeout</code> is
     *        negative
     * @throws IllegalStateException if this method is called when already shut down.
     * @throws JcrConfigurationException if there is an error in the configuration or any of the services that prevents proper
     *         startup
     * @see #start()
     * @see #shutdown()
     */
    public void start( boolean validateRepositoryConfigs,
                       long timeout,
                       TimeUnit timeoutUnit ) {
        start();
        if (validateRepositoryConfigs) {
            Set<String> repositoryNames = getRepositoryNames();
            if (repositoryNames.isEmpty()) return;

            final CountDownLatch latch = new CountDownLatch(repositoryNames.size());

            try {
                repositoriesLock.lock();
                // Put in a holder with a future for each repository
                // (this should proceed quickly, as nothing waits for the initialization) ...
                for (final String repositoryName : repositoryNames) {
                    RepositoryInitializer initializer = new RepositoryInitializer(repositoryName, latch);
                    Future<JcrRepository> future = repositoryStarterService.submit(initializer);
                    JcrRepositoryHolder holder = new JcrRepositoryHolder(repositoryName, future);
                    this.repositories.put(repositoryName, holder);
                }
            } finally {
                repositoriesLock.unlock();
            }

            // Now wait for the all the startups to complete ...
            try {
                if (timeout < 0L) {
                    latch.await();
                } else {
                    latch.await(timeout, timeoutUnit);
                }
            } catch (InterruptedException e) {
                this.problems.addError(e, JcrI18n.startingAllRepositoriesWasInterrupted, e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeEngine#newConfigurationException(java.lang.String)
     */
    @Override
    protected ModeShapeConfigurationException newConfigurationException( String msg ) {
        return new JcrConfigurationException(msg);
    }

    /**
     * Get the version of this engine.
     * 
     * @return version
     */
    public String getEngineVersion() {
        return JcrRepository.getBundleProperty(Repository.REP_VERSION_DESC, true);
    }

    /**
     * Get the {@link Repository} implementation for the named repository.
     * 
     * @param repositoryName the name of the repository, which corresponds to the name of a configured {@link RepositorySource}
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final JcrRepository getRepository( String repositoryName ) throws RepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        checkRunning();

        JcrRepositoryHolder holder = null;
        try {
            repositoriesLock.lock();
            holder = repositories.get(repositoryName);
            if (holder != null) {
                // The repository was already placed in the map and thus initialization has been started
                // and may be finished. But this call will block until the repository has completed initialization...
                return holder.getRepository();
            }
            if (!getRepositoryNames().contains(repositoryName)) {
                // The repository name is not a valid repository ...
                String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                throw new RepositoryException(msg);
            }
            // Now create the initializer and holder ...
            RepositoryInitializer initializer = new RepositoryInitializer(repositoryName);
            Future<JcrRepository> future = repositoryStarterService.submit(initializer);
            holder = new JcrRepositoryHolder(repositoryName, future);
            repositories.put(repositoryName, holder);
        } finally {
            repositoriesLock.unlock();
        }
        JcrRepository repo = holder.getRepository();
        return repo;
    }

    /**
     * Get the names of each of the JCR repositories.
     * 
     * @return the immutable names of the repositories that exist at the time this method is called
     */
    public Set<String> getRepositoryNames() {
        checkRunning();
        Set<String> results = new HashSet<String>();
        // Read the names of the JCR repositories from the configuration (not from the Repository objects used so far) ...
        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Path repositoriesPath = pathFactory.create(configuration.getPath(), ModeShapeLexicon.REPOSITORIES);
        Graph configuration = getConfigurationGraph();
        for (Location child : configuration.getChildren().of(repositoriesPath)) {
            Name repositoryName = child.getPath().getLastSegment().getName();
            results.add(readable(repositoryName));
        }
        return Collections.unmodifiableSet(results);
    }

    protected JcrRepository doCreateJcrRepository( String repositoryName ) throws RepositoryException, PathNotFoundException {
        RepositoryConnectionFactory connectionFactory = getRepositoryConnectionFactory();
        Map<String, String> descriptors = new HashMap<String, String>();
        Map<Option, String> options = new HashMap<Option, String>();

        // Read the subgraph that represents the repository ...
        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Path repositoriesPath = pathFactory.create(configuration.getPath(), ModeShapeLexicon.REPOSITORIES);
        Path repositoryPath = pathFactory.create(repositoriesPath, repositoryName);
        Name repoName = getExecutionContext().getValueFactories().getNameFactory().create(repositoryName);
        Graph configuration = null;
        Subgraph subgraph = getConfigurationSubgraph(false);

        // Read the options ...
        Path optionsPath = pathFactory.createRelativePath(ModeShapeLexicon.REPOSITORIES, repoName, ModeShapeLexicon.OPTIONS);
        Node optionsNode = subgraph.getNode(optionsPath);
        if (optionsNode != null) {
            for (Location optionLocation : optionsNode.getChildren()) {
                Node optionNode = subgraph.getNode(optionLocation);
                Path.Segment segment = optionLocation.getPath().getLastSegment();
                Property valueProperty = optionNode.getProperty(ModeShapeLexicon.VALUE);
                if (valueProperty == null) {
                    log.warn(JcrI18n.noOptionValueProvided, segment.getName().getLocalName());
                    continue;
                }
                Option option = Option.findOption(segment.getName().getLocalName());
                if (option == null) {
                    log.warn(JcrI18n.invalidOptionProvided, segment.getName().getLocalName());
                    continue;
                }
                String value = valueProperty.getFirstValue() != null ? valueProperty.getFirstValue().toString() : "";
                options.put(option, value);
            }
        }

        // Disable the derived content removal option if not explicitly set and no sequencers ...
        if (!options.containsKey(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL) && getSequencingService().getSequencers().isEmpty()) {
            options.put(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL, Boolean.FALSE.toString());
        }

        // Read the descriptors ...
        Path descriptorsPath = pathFactory.createRelativePath(ModeShapeLexicon.REPOSITORIES,
                                                              repoName,
                                                              ModeShapeLexicon.DESCRIPTORS);
        Node descriptorsNode = subgraph.getNode(descriptorsPath);
        if (descriptorsNode != null) {
            for (Location descriptorLocation : descriptorsNode.getChildren()) {
                Node optionNode = subgraph.getNode(descriptorLocation);
                Path.Segment segment = descriptorLocation.getPath().getLastSegment();
                Property valueProperty = optionNode.getProperty(ModeShapeLexicon.VALUE);
                if (valueProperty == null) continue;
                descriptors.put(segment.getName().getLocalName(), valueProperty.getFirstValue().toString());
            }
        }

        // Read the namespaces ...
        ExecutionContext context = getExecutionContext();
        Path namespacesPath = pathFactory.createRelativePath(ModeShapeLexicon.REPOSITORIES, repoName, ModeShapeLexicon.NAMESPACES);
        Node namespacesNode = subgraph.getNode(namespacesPath);
        descriptors.put(org.modeshape.jcr.api.Repository.REPOSITORY_NAME, repositoryName);
        if (namespacesNode != null) {
            configuration = getConfigurationGraph();
            GraphNamespaceRegistry registry = new GraphNamespaceRegistry(configuration, namespacesNode.getLocation().getPath(),
                                                                         ModeShapeLexicon.URI, ModeShapeLexicon.GENERATED);
            context = context.with(registry);
        }

        // Get the name of the source ...
        Path repoPath = pathFactory.createRelativePath(ModeShapeLexicon.REPOSITORIES, repoName);
        Node repoNode = subgraph.getNode(repoPath);
        if (repoNode == null) {
            // There is no repository with the supplied name ...
            throw new PathNotFoundException(Location.create(repoPath), repositoriesPath,
                                            JcrI18n.repositoryDoesNotExist.text(readable(repoName)));
        }
        Property property = repoNode.getProperty(ModeShapeLexicon.SOURCE_NAME);
        if (property == null || property.isEmpty()) {
            if (configuration == null) configuration = getConfigurationGraph();
            String readableName = readable(ModeShapeLexicon.SOURCE_NAME);
            String readablePath = readable(subgraph.getLocation());
            String msg = JcrI18n.propertyNotFoundOnNode.text(readableName, readablePath, configuration.getCurrentWorkspaceName());
            throw new RepositoryException(msg);
        }
        String sourceName = context.getValueFactories().getStringFactory().create(property.getFirstValue());

        // Verify the source exists ...
        RepositorySource source = getRepositorySource(sourceName);
        if (source == null) {
            throw new RepositoryException(JcrI18n.repositoryReferencesNonExistantSource.text(repositoryName, sourceName));
        }

        // Read the initial content ...
        String initialContentForNewWorkspaces = null;
        for (Location initialContentLocation : repoNode.getChildren(ModeShapeLexicon.INITIAL_CONTENT)) {
            Node initialContent = subgraph.getNode(initialContentLocation);
            if (initialContent == null) continue;

            // Determine where to load the initial content from ...
            Property contentReference = initialContent.getProperty(ModeShapeLexicon.CONTENT);
            if (contentReference == null || contentReference.isEmpty()) {
                if (configuration == null) configuration = getConfigurationGraph();
                String readableName = readable(ModeShapeLexicon.CONTENT);
                String readablePath = readable(initialContentLocation);
                String msg = JcrI18n.propertyNotFoundOnNode.text(readableName,
                                                                 readablePath,
                                                                 configuration.getCurrentWorkspaceName());
                throw new RepositoryException(msg);
            }
            String contentRef = string(contentReference.getFirstValue());

            // Determine which workspaces this should apply to ...
            Property workspaces = initialContent.getProperty(ModeShapeLexicon.WORKSPACES);
            if (workspaces == null || workspaces.isEmpty()) {
                if (configuration == null) configuration = getConfigurationGraph();
                String readableName = readable(ModeShapeLexicon.WORKSPACES);
                String readablePath = readable(initialContentLocation);
                String msg = JcrI18n.propertyNotFoundOnNode.text(readableName,
                                                                 readablePath,
                                                                 configuration.getCurrentWorkspaceName());
                throw new RepositoryException(msg);
            }

            // Load the initial content into a transient source ...
            XmlFileRepositorySource initialContentSource = new XmlFileRepositorySource();
            initialContentSource.setName("Initial content for " + repositoryName);
            initialContentSource.setContentLocation(contentRef);
            Graph initialContentGraph = Graph.create(initialContentSource, context);
            Graph sourceGraph = Graph.create(sourceName, connectionFactory, context);

            // And initialize the source with the content (if not already there) ...
            for (Object value : workspaces) {
                String workspaceName = string(value);
                if (workspaceName != null && workspaceName.trim().length() != 0) {
                    // Load the content into the workspace with this name ...
                    sourceGraph.useWorkspace(workspaceName);
                    try {
                        sourceGraph.merge(initialContentGraph);
                    } catch (RuntimeException e) {
                        throw new RepositoryException(JcrI18n.unableToImportInitialContent.text(readable(repoName), contentRef),
                                                      e);
                    }
                }
            }

            // Determine if this initial content should apply to new workspaces ...
            Property applyToNewWorkspaces = initialContent.getProperty(ModeShapeLexicon.APPLY_TO_NEW_WORKSPACES);
            if (applyToNewWorkspaces != null && !applyToNewWorkspaces.isEmpty() && isTrue(applyToNewWorkspaces.getFirstValue())) {
                initialContentForNewWorkspaces = contentRef; // may overwrite the value if seen more than once!
            }
        }

        // Set up the authenticators ...
        ComponentLibrary<AuthenticationProvider, ComponentConfig> authenticators = new ComponentLibrary<AuthenticationProvider, ComponentConfig>();
        for (Location authProvidersLocation : repoNode.getChildren(ModeShapeLexicon.AUTHENTICATION_PROVIDERS)) {
            Node authProviders = subgraph.getNode(authProvidersLocation);
            if (authProviders == null) continue;

            for (Location authProviderLocation : authProviders.getChildren()) {
                Node authProvider = subgraph.getNode(authProviderLocation);
                if (authProvider == null) continue;

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

                String name = stringValueOf(authProvider, ModeShapeLexicon.READABLE_NAME);
                if (name == null) name = stringValueOf(authProvider);
                String desc = stringValueOf(authProvider, ModeShapeLexicon.DESCRIPTION);
                String classname = stringValueOf(authProvider, ModeShapeLexicon.CLASSNAME);
                String[] classpath = stringValuesOf(authProvider, ModeShapeLexicon.CLASSPATH);
                Map<String, Object> properties = new HashMap<String, Object>();
                for (Property authProp : authProvider.getProperties()) {
                    Name propertyName = authProp.getName();
                    if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                    if (skipProperties.contains(propertyName)) continue;
                    if (property.isSingle()) {
                        properties.put(propertyName.getLocalName(), property.getFirstValue());
                    } else {
                        properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                    }
                }
                try {
                    ComponentConfig config = new ComponentConfig(name, desc, properties, classname, classpath);
                    authenticators.add(config);
                } catch (Throwable t) {
                    this.problems.addError(t,
                                           JcrI18n.unableToInitializeAuthenticationProvider,
                                           name,
                                           repositoryName,
                                           t.getMessage());
                }
            }
        }

        // Find the capabilities ...
        RepositorySourceCapabilities capabilities = source.getCapabilities();
        // Create the repository ...
        JcrRepository repository = new JcrRepository(context, connectionFactory, sourceName,
                                                     getRepositoryService().getRepositoryLibrary(), capabilities, descriptors,
                                                     options, initialContentForNewWorkspaces, authenticators);

        // Register all the the node types ...
        Path nodeTypesPath = pathFactory.createRelativePath(ModeShapeLexicon.REPOSITORIES, repoName, JcrLexicon.NODE_TYPES);
        Node nodeTypesNode = subgraph.getNode(nodeTypesPath);
        if (nodeTypesNode != null) {
            boolean needToRefreshSubgraph = false;
            if (configuration == null) configuration = getConfigurationGraph();

            // Expand any references to a CND file
            Property resourceProperty = nodeTypesNode.getProperty(ModeShapeLexicon.RESOURCE);
            if (resourceProperty != null) {
                ClassLoader classLoader = this.context.getClassLoader();
                for (Object resourceValue : resourceProperty) {
                    String resources = this.context.getValueFactories().getStringFactory().create(resourceValue);

                    for (String resource : resources.split("\\s*,\\s*")) {
                        Graph.Batch batch = configuration.batch();
                        GraphBatchDestination destination = new GraphBatchDestination(batch);

                        Path nodeTypesAbsPath = pathFactory.create(repositoryPath, JcrLexicon.NODE_TYPES);
                        CndImporter importer = new CndImporter(destination, nodeTypesAbsPath, true, false);
                        InputStream is = IoUtil.getResourceAsStream(resource, classLoader, getClass());
                        Problems cndProblems = new SimpleProblems();
                        if (is == null) {
                            String msg = JcrI18n.unableToFindNodeTypeDefinitionsOnClasspathOrFileOrUrl.text(resource);
                            throw new RepositoryException(msg);
                        }
                        try {
                            importer.importFrom(is, cndProblems, resource);
                            batch.execute();
                            needToRefreshSubgraph = true;
                        } catch (IOException ioe) {
                            String msg = JcrI18n.errorLoadingNodeTypeDefintions.text(resource, ioe.getMessage());
                            throw new RepositoryException(msg, ioe);
                        }
                        if (!cndProblems.isEmpty()) {
                            // Add any warnings or information to this engine's list ...
                            getProblems().addAll(cndProblems);
                            if (cndProblems.hasErrors()) {
                                String msg = null;
                                Throwable cause = null;
                                for (Problem problem : cndProblems) {
                                    if (problem.getStatus() == Status.ERROR) {
                                        msg = problem.getMessageString();
                                        cause = problem.getThrowable();
                                        break;
                                    }
                                }
                                throw new RepositoryException(JcrI18n.errorLoadingNodeTypeDefintions.text(resource, msg), cause);
                            }
                        }
                    }
                }
            }

            // Load any namespaces from the configuration into the repository's context ...
            NamespaceRegistry repoRegistry = repository.getExecutionContext().getNamespaceRegistry();
            repoRegistry.register(configuration.getContext().getNamespaceRegistry().getNamespaces());

            // Re-read the subgraph, in case any new nodes were added
            Subgraph nodeTypesSubgraph = subgraph;
            if (needToRefreshSubgraph) {
                nodeTypesSubgraph = configuration.getSubgraphOfDepth(4).at(nodeTypesNode.getLocation().getPath());
            }

            repository.getRepositoryTypeManager().registerNodeTypes(nodeTypesSubgraph, nodeTypesNode.getLocation(), false);
        }

        return repository;
    }

    protected final String readable( Name name ) {
        return name.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Path path ) {
        return path.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Location location ) {
        return location.getString(context.getNamespaceRegistry());
    }

    protected final String string( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected final boolean isTrue( Object value ) {
        return context.getValueFactories().getBooleanFactory().create(value);
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

    /**
     * @return descriptors
     */
    public Map<String, Object> initDescriptors() {
        ValueFactories factories = this.getExecutionContext().getValueFactories();
        descriptors.put(Repository.SPEC_NAME_DESC, valueFor(factories, JcrI18n.SPEC_NAME_DESC.text()));
        descriptors.put(Repository.SPEC_VERSION_DESC, valueFor(factories, "2.0"));

        if (!descriptors.containsKey(Repository.REP_NAME_DESC)) {
            descriptors.put(Repository.REP_NAME_DESC,
                            valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_NAME_DESC, true)));
        }
        if (!descriptors.containsKey(Repository.REP_VENDOR_DESC)) {
            descriptors.put(Repository.REP_VENDOR_DESC,
                            valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_VENDOR_DESC, true)));
        }
        if (!descriptors.containsKey(Repository.REP_VENDOR_URL_DESC)) {
            descriptors.put(Repository.REP_VENDOR_URL_DESC,
                            valueFor(factories, JcrRepository.getBundleProperty(Repository.REP_VENDOR_URL_DESC, true)));
        }
        if (!descriptors.containsKey(Repository.REP_VERSION_DESC)) {
            descriptors.put(Repository.REP_VERSION_DESC, valueFor(factories, getEngineVersion()));
        }
        return descriptors;
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      int type,
                                      Object value ) {
        return new JcrValue(valueFactories, null, type, value);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      String value ) {
        return valueFor(valueFactories, PropertyType.STRING, value);
    }

    /**
     * This method is equivalent to calling {@link #shutdown()} followed by {@link #awaitTermination(long, TimeUnit)}, except that
     * after those methods are called any remaining JCR sessions are terminated automatically. This is useful when shutting down
     * while there are long-running JCR sessions (such as for event listeners).
     * 
     * @param timeout the maximum time to wait for each component in this engine
     * @param unit the time unit of the timeout argument
     * @throws InterruptedException if interrupted while waiting
     */
    public void shutdownAndAwaitTermination( long timeout,
                                             TimeUnit unit ) throws InterruptedException {
        shutdown();
        awaitTermination(timeout, unit);
        for (JcrRepositoryHolder repository : repositories.values()) {
            if (repository != null) repository.terminateAllSessions();
        }
    }

    protected Logger getLogger() {
        return log;
    }

    protected Problems problems() {
        return problems;
    }

    protected class JcrRepositoryHolder {
        private final String repositoryName;
        private JcrRepository repository;
        private Future<JcrRepository> future;
        private Throwable error;

        protected JcrRepositoryHolder( String repositoryName,
                                       Future<JcrRepository> future ) {
            this.repositoryName = repositoryName;
            this.future = future;
            assert this.future != null;
        }

        public String getName() {
            return repositoryName;
        }

        public synchronized JcrRepository getRepository() throws RepositoryException {
            if (repository == null) {
                if (future != null) {
                    try {
                        // Otherwise it is still initializing, so wait for it ...
                        this.repository = future.get();
                    } catch (Throwable e) {
                        error = e.getCause();
                        String msg = JcrI18n.errorStartingRepositoryCheckConfiguration.text(repositoryName, error.getMessage());
                        throw new RepositoryException(msg, error);
                    } finally {
                        this.future = null;
                    }
                }
                if (repository == null) {
                    // There is no future, but the repository could not be initialized correctly ...
                    String msg = JcrI18n.errorStartingRepositoryCheckConfiguration.text(repositoryName, error.getMessage());
                    throw new RepositoryException(msg, error);
                }
            }
            return this.repository;
        }

        public synchronized void close() {
            if (future != null) {
                try {
                    future.cancel(false);
                } finally {
                    future = null;
                }
            }
            if (repository != null) {
                try {
                    repository.close();
                } finally {
                    repository = null;
                }
            }
        }

        public synchronized void terminateAllSessions() {
            // only need to do this on repositories that have been used; i.e., not including just-initialized repositories
            if (repository != null) repository.terminateAllSessions();
        }

        public synchronized void cleanUpLocks() {
            // only need to do this on repositories that have been used; i.e., not including just-initialized repositories
            if (repository != null) {
                try {
                    repository.getRepositoryLockManager().cleanUpLocks();
                } catch (Throwable t) {
                    getLogger().error(t, JcrI18n.errorCleaningUpLocks, repository.getRepositorySourceName());
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return repositoryName;
        }
    }

    protected class RepositoryInitializer implements Callable<JcrRepository> {
        private final String repositoryName;
        private final CountDownLatch latch;

        protected RepositoryInitializer( String repositoryName ) {
            this(repositoryName, null);
        }

        protected RepositoryInitializer( String repositoryName,
                                         CountDownLatch latch ) {
            this.repositoryName = repositoryName;
            this.latch = latch;
        }

        public JcrRepository call() throws Exception {
            JcrRepository repository = null;
            try {
                repository = doCreateJcrRepository(repositoryName);
                getLogger().info(JcrI18n.completedStartingRepository, repositoryName);
                return repository;
            } catch (RepositoryException t) {
                // Record this in the problems ...
                problems().addError(t, JcrI18n.errorStartingRepositoryCheckConfiguration, repositoryName, t.getMessage());
                throw t;
            } catch (Throwable t) {
                // Record this in the problems ...
                problems().addError(t, JcrI18n.errorStartingRepositoryCheckConfiguration, repositoryName, t.getMessage());
                String msg = JcrI18n.errorStartingRepositoryCheckConfiguration.text(repositoryName, t.getMessage());
                throw new RepositoryException(msg, t);
            } finally {
                if (latch != null) latch.countDown();
            }
        }
    }

}

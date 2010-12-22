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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.cnd.CndImporter;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
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

    private final Map<String, JcrRepository> repositories;
    private final Lock repositoriesLock;
    private final Map<String, Object> descriptors = new HashMap<String, Object>();

    /**
     * Provides the ability to schedule lock clean-up
     */
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);

    JcrEngine( ExecutionContext context,
               ModeShapeConfiguration.ConfigurationDefinition configuration ) {
        super(context, configuration);
        this.repositories = new HashMap<String, JcrRepository>();
        this.repositoriesLock = new ReentrantLock();
        initDescriptors();
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
        Collection<JcrRepository> repos;

        try {
            // Make a copy of the repositories to minimize the time that the lock needs to be held
            repositoriesLock.lock();
            repos = new ArrayList<JcrRepository>(repositories.values());
        } finally {
            repositoriesLock.unlock();
        }

        for (JcrRepository repository : repos) {
            try {
                repository.getRepositoryLockManager().cleanUpLocks();
            } catch (Throwable t) {
                log.error(t, JcrI18n.errorCleaningUpLocks, repository.getRepositorySourceName());
            }
        }
    }

    @Override
    protected void preShutdown() {
        scheduler.shutdown();
        super.preShutdown();

        try {
            this.repositoriesLock.lock();
            // Shut down all of the repositories ...
            for (JcrRepository repository : repositories.values()) {
                repository.close();
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

        try {
            repositoriesLock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                try {
                    repository = doCreateJcrRepository(repositoryName);
                } catch (PathNotFoundException e) {
                    // The repository name is not a valid repository ...
                    String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                    throw new RepositoryException(msg);
                }
                repositories.put(repositoryName, repository);
            }
            return repository;
        } finally {
            repositoriesLock.unlock();
        }
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
        Graph configuration = getConfigurationGraph();
        Subgraph subgraph = configuration.getSubgraphOfDepth(6).at(repositoryPath);

        // Read the options ...
        Node optionsNode = subgraph.getNode(ModeShapeLexicon.OPTIONS);
        if (optionsNode != null) {
            for (Location optionLocation : optionsNode.getChildren()) {
                Node optionNode = configuration.getNodeAt(optionLocation);
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
                options.put(option, valueProperty.getFirstValue().toString());
            }
        }

        // Disable the derived content removal option if not explicitly set and no sequencers ...
        if (!options.containsKey(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL) && getSequencingService().getSequencers().isEmpty()) {
            options.put(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL, Boolean.FALSE.toString());
        }

        // Read the descriptors ...
        Node descriptorsNode = subgraph.getNode(ModeShapeLexicon.DESCRIPTORS);
        if (descriptorsNode != null) {
            for (Location descriptorLocation : descriptorsNode.getChildren()) {
                Node optionNode = configuration.getNodeAt(descriptorLocation);
                Path.Segment segment = descriptorLocation.getPath().getLastSegment();
                Property valueProperty = optionNode.getProperty(ModeShapeLexicon.VALUE);
                if (valueProperty == null) continue;
                descriptors.put(segment.getName().getLocalName(), valueProperty.getFirstValue().toString());
            }
        }

        // Read the namespaces ...
        ExecutionContext context = getExecutionContext();
        Node namespacesNode = subgraph.getNode(ModeShapeLexicon.NAMESPACES);
        descriptors.put(org.modeshape.jcr.api.Repository.REPOSITORY_NAME, repositoryName);
        if (namespacesNode != null) {
            GraphNamespaceRegistry registry = new GraphNamespaceRegistry(configuration, namespacesNode.getLocation().getPath(),
                                                                         ModeShapeLexicon.URI);
            context = context.with(registry);
        }

        // Get the name of the source ...
        Property property = subgraph.getRoot().getProperty(ModeShapeLexicon.SOURCE_NAME);
        if (property == null || property.isEmpty()) {
            String readableName = readable(ModeShapeLexicon.SOURCE_NAME);
            String readablePath = readable(subgraph.getLocation());
            String msg = JcrI18n.propertyNotFoundOnNode.text(readableName, readablePath, configuration.getCurrentWorkspaceName());
            throw new RepositoryException(msg);
        }
        String sourceName = context.getValueFactories().getStringFactory().create(property.getFirstValue());

        // Verify the sourc exists ...
        RepositorySource source = getRepositorySource(sourceName);
        if (source == null) {
            throw new RepositoryException(JcrI18n.repositoryReferencesNonExistantSource.text(repositoryName, sourceName));
        }

        // Read the initial content ...
        String initialContentForNewWorkspaces = null;
        for (Location initialContentLocation : subgraph.getRoot().getChildren(ModeShapeLexicon.INITIAL_CONTENT)) {
            Node initialContent = subgraph.getNode(initialContentLocation);
            if (initialContent == null) continue;

            // Determine where to load the initial content from ...
            Property contentReference = initialContent.getProperty(ModeShapeLexicon.CONTENT);
            if (contentReference == null || contentReference.isEmpty()) {
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
                    sourceGraph.merge(initialContentGraph);
                }
            }

            // Determine if this initial content should apply to new workspaces ...
            Property applyToNewWorkspaces = initialContent.getProperty(ModeShapeLexicon.APPLY_TO_NEW_WORKSPACES);
            if (applyToNewWorkspaces != null && !applyToNewWorkspaces.isEmpty() && isTrue(applyToNewWorkspaces.getFirstValue())) {
                initialContentForNewWorkspaces = contentRef; // may overwrite the value if seen more than once!
            }
        }

        // Find the capabilities ...
        RepositorySourceCapabilities capabilities = source.getCapabilities();
        // Create the repository ...
        JcrRepository repository = new JcrRepository(context, connectionFactory, sourceName,
                                                     getRepositoryService().getRepositoryLibrary(), capabilities, descriptors,
                                                     options, initialContentForNewWorkspaces);

        // Register all the the node types ...
        Node nodeTypesNode = subgraph.getNode(JcrLexicon.NODE_TYPES);
        if (nodeTypesNode != null) {
            boolean needToRefreshSubgraph = false;

            // Expand any references to a CND file
            Property resourceProperty = nodeTypesNode.getProperty(ModeShapeLexicon.RESOURCE);
            if (resourceProperty != null) {
                ClassLoader classLoader = this.context.getClassLoader();
                for (Object resourceValue : resourceProperty) {
                    String resources = this.context.getValueFactories().getStringFactory().create(resourceValue);

                    for (String resource : resources.split("\\s*,\\s*")) {
                        Graph.Batch batch = configuration.batch();
                        GraphBatchDestination destination = new GraphBatchDestination(batch);

                        Path nodeTypesPath = pathFactory.create(repositoryPath, JcrLexicon.NODE_TYPES);
                        CndImporter importer = new CndImporter(destination, nodeTypesPath, true);
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
            for (NamespaceRegistry.Namespace namespace : configuration.getContext().getNamespaceRegistry().getNamespaces()) {
                if (!repoRegistry.isRegisteredNamespaceUri(namespace.getNamespaceUri())) {
                    repoRegistry.register(namespace.getPrefix(), namespace.getNamespaceUri());
                }
            }

            // Re-read the subgraph, in case any new nodes were added
            Subgraph nodeTypesSubgraph = subgraph;
            if (needToRefreshSubgraph) {
                nodeTypesSubgraph = configuration.getSubgraphOfDepth(4).at(nodeTypesNode.getLocation().getPath());
            }

            repository.getRepositoryTypeManager().registerNodeTypes(nodeTypesSubgraph, nodeTypesNode.getLocation());// throws
            // exception
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

    /**
     * @return descriptors
     */
    public Map<String, Object> initDescriptors() {
        ValueFactories factories = this.getExecutionContext().getValueFactories();
        descriptors.put(Repository.SPEC_NAME_DESC, valueFor(factories, JcrI18n.SPEC_NAME_DESC.text()));
        descriptors.put(Repository.SPEC_VERSION_DESC, valueFor(factories, "2.0"));

        if (!descriptors.containsKey(Repository.REP_NAME_DESC)) {
            descriptors.put(Repository.REP_NAME_DESC, valueFor(factories,
                                                               JcrRepository.getBundleProperty(Repository.REP_NAME_DESC, true)));
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
        for (JcrRepository repository : repositories.values()) {
            if (repository != null) repository.terminateAllSessions();
        }
    }
}

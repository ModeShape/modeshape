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

package org.modeshape.jcr;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.util.ReflectionUtil;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.RepositoryConfiguration.ProjectionConfiguration;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.AllPathsCache;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.federation.ConnectorChangeSetFactory;
import org.modeshape.jcr.federation.ConnectorChangeSetImpl;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.ConnectorChangeSet;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.WorkspaceAndPath;

/**
 * Class which maintains (based on the configuration) the list of available connectors for a repository.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @author Randall Hauch (rhauch@redhat.com)
 */
@ThreadSafe
public final class Connectors {

    private static final Logger LOGGER = Logger.getLogger(Connectors.class);

    private final JcrRepository.RunningState repository;
    private final Logger logger;

    private boolean initialized = false;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<Snapshot>();
    private volatile DocumentTranslator translator;

    protected Connectors( JcrRepository.RunningState repository,
                          Collection<Component> components,
                          Map<String, List<RepositoryConfiguration.ProjectionConfiguration>> preconfiguredProjections ) {
        this.repository = repository;
        this.logger = Logger.getLogger(getClass());
        this.snapshot.set(new Snapshot(components, preconfiguredProjections));
    }

    @GuardedBy( "this" )
    protected synchronized void initialize() throws RepositoryException {
        if (initialized || !hasConnectors()) {
            // nothing to do ...
            return;
        }

        // initialize the configured connectors
        initializeConnectors();
        // load the projection -> node mappings from the system area
        loadStoredProjections();
        // creates any preconfigured projections
        createPreconfiguredProjections();
        // load the projections, but with all pre-configured projections
        loadStoredProjections();

        initialized = true;
    }

    private void createPreconfiguredProjections() throws RepositoryException {
        assert !initialized;
        Snapshot current = this.snapshot.get();
        for (String workspaceName : current.getWorkspacesWithProjections()) {
            JcrSession session = repository.loginInternalSession(workspaceName);
            try {
                FederationManager federationManager = session.getWorkspace().getFederationManager();
                List<RepositoryConfiguration.ProjectionConfiguration> projections = current.getProjectionConfigurationsForWorkspace(workspaceName);
                for (RepositoryConfiguration.ProjectionConfiguration projectionCfg : projections) {
                    String repositoryPath = projectionCfg.getRepositoryPath();
                    String alias = projectionCfg.getAlias();

                    AbstractJcrNode node = session.getNode(repositoryPath);
                    // only create the projectionCfg if one doesn't exist with the same alias
                    if (!current.hasInternalProjection(alias, node.key().toString()) && !projectedPathExists(session, projectionCfg)) {
                        federationManager.createProjection(repositoryPath,
                                                           projectionCfg.getSourceName(),
                                                           projectionCfg.getExternalPath(),
                                                           alias);
                    }
                }
            } finally {
                session.logout();
            }
        }
    }

    private boolean projectedPathExists( JcrSession session,
                                         RepositoryConfiguration.ProjectionConfiguration projectionCfg )
        throws RepositoryException {
        try {
            session.getNode(projectionCfg.getProjectedPath());
            repository.warn(JcrI18n.projectedPathPointsTowardsInternalNode,
                            projectionCfg,
                            projectionCfg.getSourceName(),
                            projectionCfg.getProjectedPath());
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    private void loadStoredProjections() {
        assert !initialized;
        SessionCache systemSession = repository.createSystemSession(repository.context(), true);

        CachedNode systemNode = getSystemNode(systemSession);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);
        if (federationNodeRef != null) {
            Collection<Projection> newProjections = loadStoredProjections(systemSession, federationNodeRef);
            Snapshot current = this.snapshot.get();
            Snapshot updated = current.withProjections(newProjections);
            this.snapshot.compareAndSet(current, updated);
        }
    }

    private Collection<Projection> loadStoredProjections( SessionCache systemSession,
                                                          ChildReference federationNodeRef ) {
        CachedNode federationNode = systemSession.getNode(federationNodeRef.getKey());
        ChildReferences federationChildRefs = federationNode.getChildReferences(systemSession);
        // the stored projection mappings use SNS
        int projectionsCount = federationChildRefs.getChildCount(ModeShapeLexicon.PROJECTION);
        Collection<Projection> projections = new ArrayList<Projection>(projectionsCount);

        Iterator<ChildReference> iter = federationChildRefs.iterator(ModeShapeLexicon.PROJECTION);
        while (iter.hasNext()) {
            ChildReference projectionRef = iter.next();
            NodeKey projectionRefKey = projectionRef.getKey();
            CachedNode projection = systemSession.getNode(projectionRefKey);
            String externalNodeKey = projection.getProperty(ModeShapeLexicon.EXTERNAL_NODE_KEY, systemSession)
                                               .getFirstValue()
                                               .toString();
            assert externalNodeKey != null;

            String projectedNodeKey = projection.getProperty(ModeShapeLexicon.PROJECTED_NODE_KEY, systemSession)
                                                .getFirstValue()
                                                .toString();
            assert projectedNodeKey != null;

            String alias = projection.getProperty(ModeShapeLexicon.PROJECTION_ALIAS, systemSession).getFirstValue().toString();
            assert alias != null;

            projections.add(new Projection(externalNodeKey, projectedNodeKey, alias));
        }
        return projections;
    }

    private CachedNode getSystemNode( SessionCache systemSession ) {
        CachedNode systemRoot = systemSession.getNode(systemSession.getRootKey());
        ChildReference systemNodeRef = systemRoot.getChildReferences(systemSession).getChild(JcrLexicon.SYSTEM);
        assert systemNodeRef != null;
        return systemSession.getNode(systemNodeRef.getKey());
    }

    private void initializeConnectors() {
        assert !initialized;
        // Get a session that we'll pass to the connectors to use for registering namespaces and node types
        Session session = null;
        try {
            // Get a session that we'll pass to the connectors to use for registering namespaces and node types
            session = repository.loginInternalSession();
            NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

            if (!(nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
                throw new IllegalStateException("Invalid node type manager (expected modeshape NodeTypeManager): "
                                                + nodeTypeManager.getClass().getName());
            }

            // Initialize each connector using the supplied session ...
            Snapshot current = this.snapshot.get();
            Collection<Connector> connectorsWithErrors = new ArrayList<Connector>();
            for (Connector connector : current.getConnectors()) {
                try {
                    initializeConnector(connector, registry, (org.modeshape.jcr.api.nodetype.NodeTypeManager)nodeTypeManager);
                } catch (Throwable t) {
                    repository.error(t, JcrI18n.unableToInitializeConnector, connector, repository.name(), t.getMessage());
                    connectorsWithErrors.add(connector);
                }
            }
            if (!connectorsWithErrors.isEmpty()) {
                Snapshot updated = current.withoutConnectors(connectorsWithErrors);
                this.snapshot.compareAndSet(current, updated);
                // None of the removed connectors were running, so there's no need to remote unused ones from 'updated'
            }
        } catch (RepositoryException e) {
            throw new SystemFailureException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private void storeProjection( Projection projection ) {
        PropertyFactory propertyFactory = repository.context().getPropertyFactory();

        // we need to store the projection mappings so that we don't loose that information
        SessionCache systemSession = repository.createSystemSession(repository.context(), false);
        NodeKey systemNodeKey = getSystemNode(systemSession).getKey();
        MutableCachedNode systemNode = systemSession.mutable(systemNodeKey);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);

        if (federationNodeRef == null) {
            // there isn't a federation node present, so we need to add it
            try {
                Property primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.FEDERATION);
                systemNode.createChild(systemSession,
                                       systemNodeKey.withId("mode:federation"),
                                       ModeShapeLexicon.FEDERATION,
                                       primaryType);
                systemSession.save();
                federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        NodeKey federationNodeKey = federationNodeRef.getKey();
        MutableCachedNode federationNode = systemSession.mutable(federationNodeKey);

        Property primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.PROJECTION);
        Property externalNodeKeyProp = propertyFactory.create(ModeShapeLexicon.EXTERNAL_NODE_KEY, projection.getExternalNodeKey());

        Property projectedNodeKeyProp = propertyFactory.create(ModeShapeLexicon.PROJECTED_NODE_KEY,
                                                               projection.getProjectedNodeKey());
        Property alias = propertyFactory.create(ModeShapeLexicon.PROJECTION_ALIAS, projection.getAlias());
        federationNode.createChild(systemSession,
                                   federationNodeKey.withRandomId(),
                                   ModeShapeLexicon.PROJECTION,
                                   primaryType,
                                   externalNodeKeyProp,
                                   projectedNodeKeyProp,
                                   alias);

        systemSession.save();
    }

    /**
     * Returns the key of the internal (federated) node which has been projected on the external node with the given key.
     * 
     * @param externalNodeKey a {@code non-null} String representing the {@link NodeKey} format an external node
     * @return either a {@code non-null} String representing the node key of the projected node, or {@code null} if there is no
     *         projection.
     */
    public String getProjectedNodeKey( String externalNodeKey ) {
        Projection projection = snapshot.get().getProjectionForExternalNode(externalNodeKey);
        return projection != null ? projection.getProjectedNodeKey() : null;
    }

    /**
     * Stores a mapping from an external node towards an existing, internal node which will become a federated node. These
     * projections are created via
     * {@link org.modeshape.jcr.api.federation.FederationManager#createProjection(String, String, String, String)} and need to be
     * stored so that parent back references (from the projection to the external node) are correctly handled.
     * 
     * @param externalNodeKey a {@code non-null} String representing the {@link NodeKey} format of the projection's id.
     * @param projectedNodeKey a {@code non-null} String, representing the value of the external node's key
     * @param alias a {@code non-null} String, representing the alias of the projection.
     */
    public synchronized void addProjection( String externalNodeKey,
                                            String projectedNodeKey,
                                            String alias ) {
        Projection projection = new Projection(externalNodeKey, projectedNodeKey, alias);
        storeProjection(projection);
        Snapshot current = this.snapshot.get();
        Snapshot updated = current.withProjection(projection);
        this.snapshot.compareAndSet(current, updated);
    }

    /**
     * Signals that an external node with the given key has been removed.
     * 
     * @param externalNodeKey a {@code non-null} String
     */
    public void externalNodeRemoved( String externalNodeKey ) {
        if (this.snapshot.get().containsProjectionForExternalNode(externalNodeKey)) {
            // the external node was the root of a projection, so we need to remove that projection
            synchronized (this) {
                Snapshot current = this.snapshot.get();
                Snapshot updated = current.withoutProjection(externalNodeKey);
                if (current != updated) {
                    this.snapshot.compareAndSet(current, updated);
                }
            }
        }
    }

    /**
     * Signals that an internal node with the given key has been removed.
     * 
     * @param internalNodeKey a {@code non-null} String
     */
    public void internalNodeRemoved( String internalNodeKey ) {
        if (this.snapshot.get().containsProjectionForInternalNode(internalNodeKey)) {
            // identify all the projections which from this internal (aka. federated node) and remove them
            synchronized (this) {
                Snapshot current = this.snapshot.get();
                Snapshot updated = current;
                for (Projection projection : current.getProjections()) {
                    if (internalNodeKey.equalsIgnoreCase(projection.getProjectedNodeKey())) {
                        String externalNodeKey = projection.getExternalNodeKey();
                        removeStoredProjection(externalNodeKey);
                        updated = updated.withoutProjection(externalNodeKey);
                    }
                }
                if (current != updated) {
                    this.snapshot.compareAndSet(current, updated);
                }
            }
        }
    }

    /**
     * This method removes a persisted projection definition, but does not update the {@link #snapshot} instance.
     * 
     * @param externalNodeKey the external key for the projection
     */
    private void removeStoredProjection( String externalNodeKey ) {
        SessionCache systemSession = repository.createSystemSession(repository.context(), false);
        NodeKey systemNodeKey = getSystemNode(systemSession).getKey();
        MutableCachedNode systemNode = systemSession.mutable(systemNodeKey);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);

        // if we're removing a projection, one had to be stored previously, so there should be a federation node present
        assert federationNodeRef != null;

        NodeKey federationNodeKey = federationNodeRef.getKey();
        MutableCachedNode federationNode = systemSession.mutable(federationNodeKey);
        ChildReferences federationChildRefs = federationNode.getChildReferences(systemSession);

        int projectionsCount = federationChildRefs.getChildCount(ModeShapeLexicon.PROJECTION);

        for (int i = 1; i <= projectionsCount; i++) {
            ChildReference projectionRef = federationChildRefs.getChild(ModeShapeLexicon.PROJECTION, i);
            NodeKey projectionRefKey = projectionRef.getKey();
            CachedNode storedProjection = systemSession.getNode(projectionRefKey);
            String storedProjectionExternalNodeKey = storedProjection.getProperty(ModeShapeLexicon.EXTERNAL_NODE_KEY,
                                                                                  systemSession)
                                                                     .getFirstValue()
                                                                     .toString();
            assert storedProjectionExternalNodeKey != null;
            if (storedProjectionExternalNodeKey.equals(externalNodeKey)) {
                federationNode.removeChild(systemSession, projectionRefKey);
                systemSession.destroy(projectionRefKey);
                systemSession.save();
                break;
            }
        }
    }

    protected Connector instantiateConnector( Component component ) {
        try {
            // Instantiate the connector and set the 'name' field ...
            Connector connector = component.createInstance(getClass().getClassLoader());

            // Set the repository name field ...
            ReflectionUtil.setValue(connector, "repositoryName", repository.name());

            // Set the logger instance
            ReflectionUtil.setValue(connector, "logger", Logger.getLogger(connector.getClass()));

            // Set the logger instance
            ReflectionUtil.setValue(connector, "simpleLogger", ExtensionLogger.getLogger(connector.getClass()));

            // We'll initialize it later in #intialize() ...
            return connector;
        } catch (Throwable t) {
            if (t.getCause() != null) {
                t = t.getCause();
            }
            logger.error(t, JcrI18n.unableToInitializeConnector, component, repository.name(), t.getMessage());
            return null;
        }
    }

    protected void initializeConnector( Connector connector,
                                        NamespaceRegistry registry,
                                        org.modeshape.jcr.api.nodetype.NodeTypeManager nodeTypeManager )
        throws IOException, RepositoryException {

        // Set the execution context instance ...
        ReflectionUtil.setValue(connector, "context", repository.context());

        // Set the execution context instance ...
        ReflectionUtil.setValue(connector, "translator", getDocumentTranslator());

        // Set the MIME type detector ...
        ReflectionUtil.setValue(connector, "mimeTypeDetector", repository.mimeTypeDetector());

        // Set the transaction manager
        ReflectionUtil.setValue(connector, "transactionManager", repository.txnManager());

        // Set the ConnectorChangedSet factory
        ReflectionUtil.setValue(connector, "connectorChangedSetFactory", createConnectorChangedSetFactory(connector));

        // Set the Environment
        ReflectionUtil.setValue(connector, "environment", repository.environment());

        // Set the ExtraPropertiesStore instance, which is unique to this connector ...
        LocalDocumentStore store = repository.documentStore().localStore();
        String name = connector.getSourceName();
        String sourceKey = NodeKey.keyForSourceName(name);
        DocumentTranslator translator = getDocumentTranslator();
        ExtraPropertiesStore defaultExtraPropertiesStore = new LocalDocumentStoreExtraProperties(store, sourceKey, translator);
        ReflectionUtil.setValue(connector, "extraPropertiesStore", defaultExtraPropertiesStore);

        connector.initialize(registry, nodeTypeManager);

        // If successful, call the 'postInitialize' method reflectively (due to inability to call directly) ...
        Method postInitialize = ReflectionUtil.findMethod(Connector.class, "postInitialize");
        ReflectionUtil.invokeAccessibly(connector, postInitialize, new Object[] {});
    }

    protected RunningState repository() {
        return repository;
    }

    private ConnectorChangeSetFactory createConnectorChangedSetFactory( final Connector c ) {
        return new ConnectorChangeSetFactory() {
            @Override
            public ConnectorChangeSet newChangeSet() {
                PathMappings mappings = getPathMappings(c);
                RunningState repository = repository();
                final ExecutionContext context = repository.context();
                return new ConnectorChangeSetImpl(Connectors.this, mappings,
                                                  context.getId(), context.getProcessId(),
                                                  repository.repositoryKey(), repository.changeBus(),
                                                  repository.context().getValueFactories().getDateFactory());
            }
        };
    }

    protected final Set<String> getWorkspacesWithProjectionsFor( Connector connector ) {
        return this.snapshot.get().getWorkspacesWithProjectionsFor(connector);
    }

    protected synchronized void shutdown() {
        if (!initialized || !hasConnectors()) {
            return;
        }
        Snapshot current = this.snapshot.get();
        for (Connector connector : current.getConnectors()) {
            try {
                connector.shutdown();
            } catch (Exception e) {
                LOGGER.debug(e, "Error while stopping connector for {0}", connector.getSourceName());
            }
        }
        this.snapshot.set(current.withOnlyProjectionConfigurations());
    }

    protected boolean hasReadonlyConnectors() {
        return snapshot.get().hasReadonlyConnectors();
    }

    /**
     * Returns the connector which is mapped to the given source key.
     * 
     * @param sourceKey a {@code non-null} {@link String}
     * @return either a {@link Connector} instance of {@code null}
     */
    public Connector getConnectorForSourceKey( String sourceKey ) {
        return this.snapshot.get().getConnectorWithSourceKey(sourceKey);
    }

    /**
     * Returns the name of the external source mapped at the given key.
     *
     * @param sourceKey the key of the source; may not be null
     * @return the name of the external source to which the key is mapped; may be null
     */
    public String getSourceNameAtKey( String sourceKey ) {
        return this.snapshot.get().getSourceNameAtKey(sourceKey);
    }

    /**
     * Determine there is a projection with the given alias and projected (internal) node key
     *
     * @param alias the alias
     * @param externalNodeKey the node key of the projected (internal) node
     * @return true if there is such a projection, or false otherwise
     */
    public boolean hasExternalProjection( String alias,
                                          String externalNodeKey ) {
        return this.snapshot.get().hasExternalProjection(alias, externalNodeKey);
    }

    /**
     * Returns a connector which was registered for the given source name.
     * 
     * @param sourceName a {@code non-null} String; the name of a source
     * @return either a {@link Connector} instance or {@code null}
     */
    public Connector getConnectorForSourceName( String sourceName ) {
        assert sourceName != null;
        return this.snapshot.get().getConnectorWithSourceKey(NodeKey.keyForSourceName(sourceName));
    }

    /**
     * Checks if there are any registered connectors.
     * 
     * @return {@code true} if any connectors are registered, {@code false} otherwise.
     */
    public boolean hasConnectors() {
        return this.snapshot.get().hasConnectors();
    }

    /**
     * Returns the repository's document translator.
     * 
     * @return a {@link DocumentTranslator} instance.
     */
    public DocumentTranslator getDocumentTranslator() {
        if (translator == null) {
            // We don't want the connectors to use a translator that converts large strings to binary values that are
            // managed within ModeShape's binary store. Instead, all of the connector-created string property values
            // should be kept as strings ...
            translator = repository.repositoryCache().getDocumentTranslator().withLargeStringSize(Long.MAX_VALUE);
        }
        return translator;
    }

    /**
     * Get the immutable mappings from connector-specific external paths to projected, repository paths. The supplied object is
     * intended to be used for a specific activity (where a consistent set of mappings is expected), discarded, and then
     * reacquired the next time mappings are needed.
     * 
     * @param connector the connector for which the path mappings are requested; may not be null
     * @return the path mappings; never null
     */
    public PathMappings getPathMappings( Connector connector ) {
        return this.snapshot.get().getPathMappings(connector);
    }

    protected boolean isReadonlyPath( Path path,
                                      JcrSession session ) throws RepositoryException {
        AbstractJcrNode node = session.node(path);
        Connector connector = getConnectorForSourceKey(node.key().getSourceKey());
        return connector != null && connector.isReadonly();
    }

    /**
     * An immutable class used internally to provide a consistent (immutable) view of the {@link Connector} instances, along with
     * various cached data to make it easy to find a {@link Connector} instance by projected or external source keys, etc.
     * <p>
     * Instances are publicly immutable.
     */
    @Immutable
    protected class Snapshot {

        /**
         * A map of [sourceName, connector] instances.
         */
        private final Map<String, Connector> sourceKeyToConnectorMap;

        /**
         * A map of [workspaceName, projection] instances which holds the preconfigured projections for each workspace
         */
        private final Map<String, List<RepositoryConfiguration.ProjectionConfiguration>> preconfiguredProjections;

        /**
         * A map of (externalNodeKey, Projection) instances holds the existing projections in-memory
         */
        private final Map<String, Projection> projections;

        /**
         * A set of internal node keys that are used in the projections.
         */
        private final Set<String> projectedInternalNodeKeys;

        /**
         * A list connectors that have been replaced and are not used anymore
         */
        private final List<Connector> unusedConnectors = new LinkedList<Connector>();

        /**
         * The set of path mappings for a given connector. Because the connector instance might change, we key these by the
         * connector {@link Connector#getSourceName() source name}.
         */
        private volatile Map<String, BasicPathMappings> mappingsByConnectorSourceName;

        /**
         * A flag which is used to track the presence of any readonly connectors
         */
        private boolean hasReadonlyConnectors;

        protected Snapshot( Collection<Component> components,
                            Map<String, List<RepositoryConfiguration.ProjectionConfiguration>> preconfiguredProjections ) {
            this.preconfiguredProjections = preconfiguredProjections;
            this.projections = new HashMap<String, Connectors.Projection>();
            this.sourceKeyToConnectorMap = new HashMap<String, Connector>();
            this.projectedInternalNodeKeys = new HashSet<String>();
            registerConnectors(components);
        }

        protected Snapshot( Snapshot original ) {
            this.projections = new HashMap<String, Connectors.Projection>(original.projections);
            this.sourceKeyToConnectorMap = new HashMap<String, Connector>(original.sourceKeyToConnectorMap);
            this.preconfiguredProjections = new HashMap<String, List<ProjectionConfiguration>>(original.preconfiguredProjections);
            this.projectedInternalNodeKeys = new HashSet<String>(original.projectedInternalNodeKeys);
            this.hasReadonlyConnectors = original.hasReadonlyConnectors;
        }

        protected synchronized void shutdownUnusedConnectors() {
            for (Connector connector : unusedConnectors) {
                connector.shutdown();
            }
            unusedConnectors.clear();
        }

        private void registerConnectors( Collection<Component> components ) {
            for (Component component : components) {
                Connector connector = instantiateConnector(component);
                if (connector != null) {
                    registerConnector(connector);
                }
            }
            checkForReadonlyConnectors();
        }

        private String keyFor( Connector connector ) {
            return NodeKey.keyForSourceName(connector.getSourceName());
        }

        /**
         * Determine if this snapshot contains any {@link Connector} instances.
         * 
         * @return true if there is at least one Connector instance, or false otherwise
         */
        public boolean hasConnectors() {
            return !sourceKeyToConnectorMap.isEmpty();
        }

        /**
         * Get the {@link Connector} instance that has the same source key (generated from the connector's
         * {@link Connector#getSourceName() source name}).
         * 
         * @param sourceKey the source key
         * @return the Connector, or null if no such connector exists
         */
        public Connector getConnectorWithSourceKey( String sourceKey ) {
            return sourceKeyToConnectorMap.get(sourceKey);
        }

        /**
         * Returns the name of the external source mapped at the given key.
         *
         * @param sourceKey the key of the source; may not be null
         * @return the name of the external source to which the key is mapped; may be null
         */
        public String getSourceNameAtKey( String sourceKey ) {
            Connector connector = sourceKeyToConnectorMap.get(sourceKey);
            return connector != null ? connector.getSourceName() : null;
        }

        /**
         * Get the {@link Connector} instances.
         * 
         * @return the (immutable) collection of Connector instances
         */
        public Collection<Connector> getConnectors() {
            return Collections.unmodifiableCollection(sourceKeyToConnectorMap.values());
        }

        /**
         * Get the names of the workspaces that contain at least one projection.
         * 
         * @return the (immutable) collection of workspace names
         * @see #getProjectionConfigurationsForWorkspace(String)
         */
        public Collection<String> getWorkspacesWithProjections() {
            return Collections.unmodifiableCollection(this.preconfiguredProjections.keySet());
        }

        /**
         * Get all of the {@link ProjectionConfiguration}s that apply to the workspace with the given name.
         * 
         * @param workspaceName the name of the workspace
         * @return the (immutable) list of projection configurations
         * @see #getWorkspacesWithProjections()
         */
        public List<RepositoryConfiguration.ProjectionConfiguration> getProjectionConfigurationsForWorkspace( String workspaceName ) {
            return Collections.unmodifiableList(this.preconfiguredProjections.get(workspaceName));
        }

        /**
         * Get the set of workspace names that contain projections of the supplied connector.
         * 
         * @param connector the connector
         * @return the set of workspace names
         */
        public Set<String> getWorkspacesWithProjectionsFor( Connector connector ) {
            String connectorSrcName = connector.getSourceName();
            Set<String> workspaceNames = new HashSet<String>();
            for (Map.Entry<String, List<RepositoryConfiguration.ProjectionConfiguration>> entry : preconfiguredProjections.entrySet()) {
                for (ProjectionConfiguration config : entry.getValue()) {
                    if (config.getSourceName().equals(connectorSrcName)) {
                        workspaceNames.add(entry.getKey());
                    }
                }
            }
            return workspaceNames;
        }

        /**
         * Determine if this snapshot contains a projection with the given alias and projected (internal) node key
         * 
         * @param alias the alias
         * @param projectedNodeKey the node key of the projected (internal) node
         * @return true if there is such a projection, or false otherwise
         */
        public boolean hasInternalProjection( String alias,
                                              String projectedNodeKey ) {
            for (Projection projection : projections.values()) {
                if (projection.hasAlias(alias) && projection.hasProjectedNodeKey(projectedNodeKey)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Determine if this snapshot contains a projection with the given alias and external node key
         *
         * @param alias the alias
         * @param externalNodeKey the node key of the external node
         * @return true if there is such a projection, or false otherwise
         */
        public boolean hasExternalProjection( String alias,
                                              String externalNodeKey ) {
            for (Projection projection : projections.values()) {
                if (projection.hasAlias(alias) && projection.hasExternalNodeKey(externalNodeKey)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the {@link Projection} instances in this snapshot.
         * 
         * @return the (immutable) collection of (immutable) Projection instances
         */
        public Collection<Projection> getProjections() {
            return Collections.unmodifiableCollection(projections.values());
        }

        private void registerConnector( Connector connector ) {
            String key = keyFor(connector);
            Connector existing = sourceKeyToConnectorMap.put(key, connector);
            if (existing != null) {
                unusedConnectors.add(existing);
            }
        }

        private boolean unregisterConnector( Connector connector ) {
            String key = keyFor(connector);
            Connector existingConnector = sourceKeyToConnectorMap.get(key);
            if (existingConnector == connector) {
                sourceKeyToConnectorMap.remove(key);
                unusedConnectors.add(connector);
                return true;
            }
            return false;
        }

        /**
         * Get the projection that uses the supplied external node key.
         * 
         * @param externalNodeKey the node key for the external node
         * @return the projection, or null if there is no such projection
         */
        public Projection getProjectionForExternalNode( String externalNodeKey ) {
            return this.projections.get(externalNodeKey);
        }

        /**
         * Determine if this snapshot contains a projection that uses the supplied external node key.
         * 
         * @param externalNodeKey the node key for the external node
         * @return true if there is an existing projection, or false otherwise
         */
        public boolean containsProjectionForExternalNode( String externalNodeKey ) {
            return this.projections.containsKey(externalNodeKey);
        }

        /**
         * Determine if this snapshot contains a projection that uses the supplied internal node key.
         * 
         * @param internalNodeKey the node key for the internal node
         * @return true if there is an existing projection, or false otherwise
         */
        public boolean containsProjectionForInternalNode( String internalNodeKey ) {
            return this.projectedInternalNodeKeys.contains(internalNodeKey);
        }

        /**
         * Create a new snapshot that excludes any existing projection that uses the supplied external node key.
         * 
         * @param externalNodeKey the node key for the external node
         * @return the new snapshot, or this instance if there is no such projection
         */
        protected Snapshot withoutProjection( String externalNodeKey ) {
            if (this.projections.containsKey(externalNodeKey)) {
                Snapshot clone = new Snapshot(this);
                clone.projections.remove(externalNodeKey);
                return clone;
            }
            return this;
        }

        /**
         * Create a new snapshot that is a copy of this snapshot but which excludes any of the supplied connector instances.
         * 
         * @param connectors the connectors
         * @return the new snapshot, or this instance if it contains none of the supplied connectors
         * @see #shutdownUnusedConnectors() should be called after this Snapshot is no longer accessible, so that any previous
         *      Connector instance is {@link Connector#shutdown()}
         */
        protected Snapshot withoutConnectors( Iterable<Connector> connectors ) {
            Snapshot clone = new Snapshot(this);
            boolean modified = false;
            for (Connector connector : connectors) {
                if (clone.unregisterConnector(connector)) modified = true;
            }
            if (modified) {
                checkForReadonlyConnectors();
            }
            return modified ? clone : this;
        }

        /**
         * Create a new snapshot that is a copy of this snapshot but which includes the supplied projection.
         * 
         * @param projection the projection
         * @return the new snapshot
         */
        protected Snapshot withProjection( Projection projection ) {
            Snapshot clone = new Snapshot(this);
            clone.projections.put(projection.getExternalNodeKey(), projection);
            clone.projectedInternalNodeKeys.add(projection.getProjectedNodeKey());
            return clone;
        }

        /**
         * Create a new snapshot that is a copy of this snapshot but which includes the supplied projections.
         * 
         * @param projections the projections
         * @return the new snapshot
         */
        protected Snapshot withProjections( Iterable<Projection> projections ) {
            Snapshot clone = new Snapshot(this);
            for (Projection projection : projections) {
                clone.projections.put(projection.getExternalNodeKey(), projection);
                clone.projectedInternalNodeKeys.add(projection.getProjectedNodeKey());
            }
            return clone;
        }

        /**
         * Create a new snapshot that contains only the same {@link ProjectionConfiguration projection configurations} that this
         * snapshot contains.
         * 
         * @return the new snapshot
         */
        protected Snapshot withOnlyProjectionConfigurations() {
            return new Snapshot(Collections.<Component>emptyList(), this.preconfiguredProjections);
        }

        /**
         * Get the immutable mappings from connector-specific external paths to projected, repository paths. The supplied object
         * is intended to be used for a specific activity (where a consistent set of mappings is expected), discarded, and then
         * reacquired the next time mappings are needed.
         * 
         * @param connector the connector for which the path mappings are requested; may not be null
         * @return the path mappings; never null
         */
        public PathMappings getPathMappings( Connector connector ) {
            String connectorSourceName = connector.getSourceName();
            if (mappingsByConnectorSourceName == null) {
                // We construct these immutable and idempotent mappings (for all connectors) lazily,
                // but we still need to synchronize upon the creation of them ...
                synchronized (this) {
                    if (mappingsByConnectorSourceName == null) {
                        final RunningState repository = repository();
                        final PathFactory pathFactory = repository().context().getValueFactories().getPathFactory();
                        // Get the map of workspace names by their key (since projections do not contain the workspace names) ...
                        final Map<String, String> workspaceNamesByKey = new HashMap<String, String>();
                        final RepositoryCache repositoryCache = repository.repositoryCache();
                        for (String workspaceName : repository.repositoryCache().getWorkspaceNames()) {
                            workspaceNamesByKey.put(NodeKey.keyForWorkspaceName(workspaceName), workspaceName);
                        }

                        Map<String, BasicPathMappings> mappingsByConnectorSourceName = new HashMap<String, BasicPathMappings>();
                        // Iterate through the projections ...
                        for (Projection projection : this.projections.values()) {
                            final String alias = projection.getAlias();

                            String externalKeyStr = projection.getExternalNodeKey(); // contains the source & workspace keys ...
                            final NodeKey externalKey = new NodeKey(externalKeyStr);
                            final String externalDocId = externalKey.getIdentifier();

                            // Find the connector that serves up this external key ...
                            Connector conn = getConnectorForSourceKey(externalKey.getSourceKey());
                            if (conn == null) continue; // really?
                            // Find the path mappings ...
                            BasicPathMappings mappings = mappingsByConnectorSourceName.get(connectorSourceName);
                            if (mappings == null) {
                                mappings = new BasicPathMappings(connectorSourceName, pathFactory);
                                mappingsByConnectorSourceName.put(connectorSourceName, mappings);
                            }
                            // Now add the path mapping for this projection. First, find the path of the one projected node ...
                            String projectedKeyStr = projection.getProjectedNodeKey();
                            NodeKey projectedKey = new NodeKey(projectedKeyStr);
                            String workspaceName = workspaceNamesByKey.get(projectedKey.getWorkspaceKey());
                            if (workspaceName == null) continue;
                            try {
                                WorkspaceCache cache = repositoryCache.getWorkspaceCache(workspaceName);
                                AllPathsCache allPathsCache = new AllPathsCache(cache, null, pathFactory);
                                CachedNode node = cache.getNode(projectedKey);
                                for (Path nodePath : allPathsCache.getPaths(node)) {
                                    Path internalPath = pathFactory.create(nodePath, alias);
                                    // Then find the path(s) for the external node with the aforementioned key ...
                                    for (String externalPathStr : conn.getDocumentPathsById(externalDocId)) {
                                        Path externalPath = pathFactory.create(externalPathStr);
                                        mappings.add(externalPath, internalPath, workspaceName);
                                    }
                                }
                            } catch (WorkspaceNotFoundException e) {
                                // ignore and continue
                            }
                        }
                        for (BasicPathMappings mappings : mappingsByConnectorSourceName.values()) {
                            mappings.freeze();
                        }
                        // After we're done initialize the map for *all* connectors, assign it ...
                        this.mappingsByConnectorSourceName = mappingsByConnectorSourceName;
                    }
                }
            }
            // We know we have the mappings, so simply return them ...
            PathMappings mappings = mappingsByConnectorSourceName.get(connectorSourceName);
            return mappings != null ? mappings : new EmptyPathMappings(connectorSourceName, repository().context()
                                                                                                        .getValueFactories()
                                                                                                        .getPathFactory());
        }

        private void checkForReadonlyConnectors() {
            for (Connector connector : sourceKeyToConnectorMap.values()) {
                if (connector.isReadonly()) {
                    hasReadonlyConnectors = true;
                    return;
                }
            }
            hasReadonlyConnectors = false;
        }

        protected boolean hasReadonlyConnectors() {
            return hasReadonlyConnectors;
        }
    }

    /**
     * The immutable mappings between the (federated) repository nodes and the external nodes exposed by a connector that they
     * project. This view of mappings will remain consistent, but may become out of date.
     * 
     * @see #getPathMappings(Connector)
     */
    @Immutable
    public static interface PathMappings {
        /**
         * Attempt to resolve the supplied external path (from the point of view of a connector) to the internal repository
         * path(s) using the connector's projections at the time this object {@link Connectors#getPathMappings(Connector) was
         * obtained}. This method returns an empty collection if the external node at the given path is not projected into the
         * repository.
         * 
         * @param externalPath the external path of a node in the tree of content exposed by the connector; this path is from the
         *        point of view of the connector.
         * @return the resolved repository paths, each in the associated named workspaces, or an empty collection if this mapping
         *         projected the supplied external path
         */
        Collection<WorkspaceAndPath> resolveExternalPathToInternal( Path externalPath );

        /**
         * Get a path factory that can be used to create new paths.
         * 
         * @return the path factory; never null
         */
        PathFactory getPathFactory();

        /**
         * Get the source name of the connector for which this mapping is defined.
         * 
         * @return the connector source name; never null
         */
        String getConnectorSourceName();
    }

    @Immutable
    protected static abstract class AbstractPathMappings implements PathMappings {
        protected static final Collection<WorkspaceAndPath> EMPTY = Collections.emptyList();
        protected final String connectorSourceName;
        protected final PathFactory pathFactory;

        protected AbstractPathMappings( String connectorSourceName,
                                        PathFactory pathFactory ) {
            this.connectorSourceName = connectorSourceName;
            this.pathFactory = pathFactory;
            assert this.connectorSourceName != null;
            assert this.pathFactory != null;
        }

        @Override
        public PathFactory getPathFactory() {
            return pathFactory;
        }

        @Override
        public String getConnectorSourceName() {
            return connectorSourceName;
        }
    }

    @Immutable
    protected static final class EmptyPathMappings extends AbstractPathMappings {

        protected EmptyPathMappings( String connectorSourceName,
                                     PathFactory pathFactory ) {
            super(connectorSourceName, pathFactory);
        }

        @Override
        public Collection<WorkspaceAndPath> resolveExternalPathToInternal( Path externalPath ) {
            return EMPTY;
        }

        @Override
        public String toString() {
            return "No mappings";
        }
    }

    protected static final class BasicPathMappings extends AbstractPathMappings {
        private Set<PathMapping> mappings;
        private volatile boolean frozen;

        protected BasicPathMappings( String connectorSourceName,
                                     PathFactory pathFactory ) {
            super(connectorSourceName, pathFactory);
            this.mappings = new HashSet<PathMapping>();
        }

        @Override
        public Collection<WorkspaceAndPath> resolveExternalPathToInternal( Path externalPath ) {
            assert this.frozen;
            // Most repository configurations will project a single external node to a single path, so this
            // method is optimized for that case. We'll keep track of the first WorkspaceAndPath instance and
            // only if at least one more is created will this method instantiate a List instance ...
            WorkspaceAndPath first = null;
            Collection<WorkspaceAndPath> results = null;
            for (PathMapping mapping : mappings) {
                WorkspaceAndPath resolved = mapping.resolveExternalPathToInternal(externalPath, pathFactory);
                if (resolved != null) {
                    if (first == null) {
                        first = resolved;
                    } else {
                        if (results == null) {
                            results = new LinkedList<WorkspaceAndPath>();
                            results.add(first);
                        }
                        results.add(resolved);
                    }
                }
            }
            return results != null ? results : (first != null ? Collections.singletonList(first) : EMPTY);
        }

        protected void add( Path externalPath,
                            Path internalPath,
                            String workspaceName ) {
            this.mappings.add(new PathMapping(externalPath, internalPath, workspaceName));
        }

        protected void freeze() {
            // Slight optimizations for common cases ...
            if (this.mappings.size() == 0) this.mappings = Collections.emptySet();
            if (this.mappings.size() == 1) this.mappings = Collections.singleton(mappings.iterator().next());
            this.frozen = true;
        }

        @Override
        public String toString() {
            return connectorSourceName + " mappings: " + mappings;
        }
    }

    protected static final class PathMapping {
        private final Path externalPath;
        private final WorkspaceAndPath internalPath;
        private final int hc;

        protected PathMapping( Path externalPath,
                               Path internalPath,
                               String workspaceName ) {
            this.externalPath = externalPath;
            this.internalPath = new WorkspaceAndPath(workspaceName, internalPath);
            this.hc = HashCode.compute(this.externalPath, this.internalPath);
            assert this.externalPath != null;
        }

        /**
         * Attempt to resolve the supplied external path to an internal path. This method returns null if this mapping is not
         * applicable for the given external path.
         * 
         * @param externalPath the external path of a node in the tree of content exposed by the connector; this path is from the
         *        point of view of the connector.
         * @param pathFactory the path factory; may not be null
         * @return the resolved repository path in a given workspace, or null if this mapping did not apply to the supplied
         *         external path
         */
        public WorkspaceAndPath resolveExternalPathToInternal( Path externalPath,
                                                               PathFactory pathFactory ) {
            if (this.externalPath.isRoot()) {
                // Simply prepend the supplied path to the internal path ...
                return internalPath.withPath(pathFactory.create(internalPath.getPath(), externalPath));
            }
            if (this.externalPath.isAtOrAbove(externalPath)) {
                if (this.externalPath.size() == externalPath.size()) {
                    // The externals are exactly the same, so simply return the internal path ...
                    return internalPath;
                }
                // Simply prepend the external subpath to the internal path ...
                Path subpath = externalPath.subpath(this.externalPath.size());
                return internalPath.withPath(pathFactory.create(internalPath.getPath(), subpath));
            }
            return null;
        }

        @Override
        public int hashCode() {
            return hc;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof PathMapping) {
                PathMapping that = (PathMapping)obj;
                if (this.hc != that.hc) return false; // can't be the same
                return this.externalPath.equals(that.externalPath) && this.internalPath.equals(that.internalPath);
            }
            return false;
        }

        @Override
        public String toString() {
            return internalPath.toString() + " => " + externalPath.toString();
        }
    }

    @Immutable
    protected class Projection {
        private final String externalNodeKey;
        private final String projectedNodeKey;
        private final String alias;

        protected Projection( String externalNodeKey,
                              String projectedNodeKey,
                              String alias ) {
            this.externalNodeKey = externalNodeKey;
            this.alias = alias;
            this.projectedNodeKey = projectedNodeKey;
        }

        protected boolean hasAlias( String alias ) {
            return this.alias.equalsIgnoreCase(alias);
        }

        protected boolean hasProjectedNodeKey( String projectedNodeKey ) {
            return this.projectedNodeKey.equals(projectedNodeKey);
        }

        protected boolean hasExternalNodeKey( String externalNodeKey ) {
            return this.externalNodeKey.equals(externalNodeKey);
        }

        protected String getProjectedNodeKey() {
            return projectedNodeKey;
        }

        protected String getAlias() {
            return alias;
        }

        protected String getExternalNodeKey() {
            return externalNodeKey;
        }
    }

    protected static class LocalDocumentStoreExtraProperties implements ExtraPropertiesStore {
        private final LocalDocumentStore localStore;
        private final String sourceKey;
        private final DocumentTranslator translator;

        protected LocalDocumentStoreExtraProperties( LocalDocumentStore localStore,
                                                     String sourceKey,
                                                     DocumentTranslator translator ) {
            this.localStore = localStore;
            this.sourceKey = sourceKey;
            this.translator = translator;
            assert this.localStore != null;
            assert this.sourceKey != null;
            assert this.translator != null;
        }

        protected String keyFor( String id ) {
            return sourceKey + ":" + id;
        }

        @Override
        public Map<Name, Property> getProperties( String id ) {
            String key = keyFor(id);
            SchematicEntry entry = localStore.get(key);
            if (entry == null) {
                return NO_PROPERTIES;
            }
            Document doc = entry.getContentAsDocument();
            Map<Name, Property> props = new HashMap<Name, Property>();
            translator.getProperties(doc, props);
            return props;
        }

        @Override
        public boolean removeProperties( String id ) {
            String key = keyFor(id);
            return localStore.remove(key);
        }

        @Override
        public void storeProperties( String id,
                                     Map<Name, Property> properties ) {
            String key = keyFor(id);
            EditableDocument doc = Schematic.newDocument();
            for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                Property property = entry.getValue();
                if (property != null) {
                    translator.setProperty(doc, property, null);
                }
            }
            localStore.storeDocument(key, doc);
        }

        @Override
        public void updateProperties( String id,
                                      Map<Name, Property> properties ) {
            String key = keyFor(id);
            SchematicEntry entry = localStore.get(key);
            EditableDocument doc = null;
            if (entry != null) {
                doc = entry.editDocumentContent();
            } else {
                doc = Schematic.newDocument();
            }
            for (Map.Entry<Name, Property> propertyEntry : properties.entrySet()) {
                Property property = propertyEntry.getValue();
                if (property != null) {
                    translator.setProperty(doc, property, null);
                } else {
                    translator.removeProperty(doc, propertyEntry.getKey(), null);
                }
            }
            localStore.storeDocument(key, doc);
        }
    }
}

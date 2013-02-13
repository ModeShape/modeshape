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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;

/**
 * Class which maintains (based on the configuration) the list of available connectors for a repository.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class Connectors {

    private static final Logger LOGGER = Logger.getLogger(Connectors.class);

    private final JcrRepository.RunningState repository;
    private final Logger logger;

    private boolean initialized = false;

    /**
     * A map of [sourceName, connector] instances.
     */
    private Map<String, Connector> sourceKeyToConnectorMap = new HashMap<String, Connector>();

    /**
     * A map of [workspaceName, projection] instances which holds the preconfigured projections for each workspace
     */
    private Map<String, List<RepositoryConfiguration.ProjectionConfiguration>> preconfiguredProjections = new HashMap<String, List<RepositoryConfiguration.ProjectionConfiguration>>();

    /**
     * A map of (externalNodeKey, Projection) instances holds the existing projections in-memory
     */
    private Map<String, Projection> projections;

    protected Connectors( JcrRepository.RunningState repository,
                       Collection<Component> components,
                       Map<String, List<RepositoryConfiguration.ProjectionConfiguration>> preconfiguredProjections) {
        this.repository = repository;
        this.logger = Logger.getLogger(getClass());
        this.preconfiguredProjections = preconfiguredProjections;

        registerConnectors(components);
    }


    private void registerConnectors( Collection<Component> components ) {
        for (Component component : components) {
            Connector connector = instantiateConnector(component);
            if (connector != null) {
                registerConnector(connector);
            }
        }
    }

    protected void initialize() throws RepositoryException {
        if (initialized || !hasConnectors()) {
            // nothing to do ...
            return;
        }

        //initialize the configured connectors
        initializeConnectors();
        //load the projection -> node mappings from the system area
        loadStoredProjections();
        //creates any preconfigured projections
        createPreconfiguredProjections();

        initialized = true;
    }

    private void createPreconfiguredProjections() throws RepositoryException {
        for (String workspaceName : preconfiguredProjections.keySet()) {
            JcrSession session = repository.loginInternalSession(workspaceName);
            try {
                FederationManager federationManager = session.getWorkspace().getFederationManager();
                List<RepositoryConfiguration.ProjectionConfiguration> projections = preconfiguredProjections.get(workspaceName);
                for (RepositoryConfiguration.ProjectionConfiguration projectionCfg : projections) {
                    String repositoryPath = projectionCfg.getRepositoryPath();
                    String alias = projectionCfg.getAlias();

                    AbstractJcrNode node = session.getNode(repositoryPath);
                    //only create the projectionCfg if one doesn't exist with the same alias
                    if (!projectionExists(alias, node.key().toString()) && !projectedPathExists(session, projectionCfg)) {
                        federationManager.createProjection(repositoryPath, projectionCfg.getSourceName(),
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
                                         RepositoryConfiguration.ProjectionConfiguration projectionCfg ) throws RepositoryException {
        try {
            session.getNode(projectionCfg.getProjectedPath());
            LOGGER.warn(JcrI18n.projectedPathPointsTowardsInternalNode, projectionCfg, projectionCfg.getSourceName(), projectionCfg.getProjectedPath());
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    private boolean projectionExists( String alias,
                                      String projectedNodeKey ) {
        for (Projection projection : projections.values()) {
            if (projection.hasAlias(alias) && projection.hasProjectedNodeKey(projectedNodeKey)) {
                return true;
            }
        }
        return false;
    }

    private void loadStoredProjections() {
        assert !initialized;
        SessionCache systemSession = repository.createSystemSession(repository.context(), false);

        CachedNode systemNode = getSystemNode(systemSession);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);
        this.projections = federationNodeRef != null ? loadStoredProjections(systemSession,
                                                                             federationNodeRef)
                                                     : new HashMap<String, Projection>();
    }

    private Map<String, Projection> loadStoredProjections( SessionCache systemSession,
                                                           ChildReference federationNodeRef ) {
        CachedNode federationNode = systemSession.getNode(federationNodeRef.getKey());
        ChildReferences federationChildRefs = federationNode.getChildReferences(systemSession);
        //the stored projection mappings use SNS
        int projectionsCount = federationChildRefs.getChildCount(ModeShapeLexicon.PROJECTION);
        Map<String, Projection> projections = new HashMap<String, Projection>(projectionsCount);

        for (int i = 1; i <= projectionsCount; i++) {
            ChildReference projectionRef = federationChildRefs.getChild(ModeShapeLexicon.PROJECTION, i);
            NodeKey projectionRefKey = projectionRef.getKey();
            CachedNode projection = systemSession.getNode(projectionRefKey);
            String externalNodeKey = projection.getProperty(ModeShapeLexicon.EXTERNAL_NODE_KEY, systemSession).getFirstValue()
                                               .toString();
            assert externalNodeKey != null;

            String projectedNodeKey = projection.getProperty(ModeShapeLexicon.PROJECTED_NODE_KEY, systemSession).getFirstValue()
                                                .toString();
            assert projectedNodeKey != null;

            String alias = projection.getProperty(ModeShapeLexicon.PROJECTION_ALIAS, systemSession).getFirstValue().toString();
            assert alias != null;

            projections.put(externalNodeKey, new Projection(externalNodeKey, projectedNodeKey, alias));
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
        // Get a session that we'll pass to the connectors to use for registering namespaces and node types
        Session session = null;
        try {
            // Get a session that we'll pass to the sequencers to use for registering namespaces and node types
            session = repository.loginInternalSession();
            NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

            if (!(nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
                throw new IllegalStateException("Invalid node type manager (expected modeshape NodeTypeManager): "
                                                        + nodeTypeManager.getClass().getName());
            }

            // Initialize each connector using the supplied session ...
            for (Iterator<Map.Entry<String, Connector>> connectorsIterator = sourceKeyToConnectorMap.entrySet()
                                                                                                    .iterator(); connectorsIterator
                    .hasNext(); ) {
                Connector connector = connectorsIterator.next().getValue();
                try {
                    initializeConnector(connector, registry, (org.modeshape.jcr.api.nodetype.NodeTypeManager)nodeTypeManager);
                } catch (Throwable t) {
                    logger.error(t, JcrI18n.unableToInitializeConnector, connector, repository.name(), t.getMessage());
                    connectorsIterator.remove(); // removes from the map
                }
            }
        } catch (RepositoryException e) {
            throw new SystemFailureException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Stores a mapping from an external node towards an existing, internal node which will become a federated node.
     * These projections are created via {@link org.modeshape.jcr.api.federation.FederationManager#createProjection(String, String, String, String)}
     * and need to be stored so that parent back references (from the projection to the external node) are correctly handled.
     *
     * @param externalNodeKey a {@code non-null} String representing the {@link NodeKey} format of the projection's id.
     * @param projectedNodeKey a {@code non-null} String, representing the value of the external node's key
     * @param alias a {@code non-null} String, representing the alias of the projection.
     */
    public void addProjection( String externalNodeKey,
                               String projectedNodeKey,
                               String alias ) {
        Projection projection = new Projection(externalNodeKey, projectedNodeKey, alias);
        projections.put(externalNodeKey, projection);
        storeProjection(projection);
    }

    private void storeProjection( Projection projection ) {
        PropertyFactory propertyFactory = repository.context().getPropertyFactory();

        //we need to store the projection mappings so that we don't loose that information
        SessionCache systemSession = repository.createSystemSession(repository.context(), false);
        NodeKey systemNodeKey = getSystemNode(systemSession).getKey();
        MutableCachedNode systemNode = systemSession.mutable(systemNodeKey);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);

        if (federationNodeRef == null) {
            //there isn't a federation node present, so we need to add it
            try {
                Property primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.FEDERATION);
                systemNode.createChild(systemSession, systemNodeKey.withId("mode:federation"),
                                       ModeShapeLexicon.FEDERATION, primaryType);
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
        federationNode.createChild(systemSession, federationNodeKey.withRandomId(), ModeShapeLexicon.PROJECTION,
                                   primaryType, externalNodeKeyProp, projectedNodeKeyProp, alias);

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
        Projection projection = projections.get(externalNodeKey);
        return projection != null ? projection.getProjectedNodeKey() : null;
    }

    /**
     * Signals that an external node with the given key has been removed.
     *
     * @param externalNodeKey a {@code non-null} String
     */
    public void externalNodeRemoved( String externalNodeKey ) {
        Projection removedProjection = projections.remove(externalNodeKey);
        if (removedProjection != null) {
            //the external node was the root of a projection, so we need to remove that projection
            removeStoredProjection(externalNodeKey);
        }
    }

    /**
     * Signals that an internal node with the given key has been removed.
     *
     * @param internalNodeKey a {@code non-null} String
     */
    public void internalNodeRemoved( String internalNodeKey ) {
        //identify all the projections which from this internal (aka. federated node) and remove them
        for (Iterator<Map.Entry<String, Projection>> projectionsIt = projections.entrySet().iterator(); projectionsIt.hasNext();) {
            Projection projection = projectionsIt.next().getValue();
            if (internalNodeKey.equalsIgnoreCase(projection.getProjectedNodeKey())) {
                String externalNodeKey = projection.getExternalNodeKey();
                removeStoredProjection(externalNodeKey);
                projectionsIt.remove();
            }
        }
    }

    private void removeStoredProjection(String externalNodeKey) {
        SessionCache systemSession = repository.createSystemSession(repository.context(), false);
        NodeKey systemNodeKey = getSystemNode(systemSession).getKey();
        MutableCachedNode systemNode = systemSession.mutable(systemNodeKey);
        ChildReference federationNodeRef = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.FEDERATION);

        //if we're removing a projection, one had to be stored previously, so there should be a federation node present
        assert federationNodeRef != null;

        NodeKey federationNodeKey = federationNodeRef.getKey();
        MutableCachedNode federationNode = systemSession.mutable(federationNodeKey);
        ChildReferences federationChildRefs = federationNode.getChildReferences(systemSession);

        int projectionsCount = federationChildRefs.getChildCount(ModeShapeLexicon.PROJECTION);

        for (int i = 1; i <= projectionsCount; i++) {
            ChildReference projectionRef = federationChildRefs.getChild(ModeShapeLexicon.PROJECTION, i);
            NodeKey projectionRefKey = projectionRef.getKey();
            CachedNode storedProjection = systemSession.getNode(projectionRefKey);
            String storedProjectionExternalNodeKey = storedProjection.getProperty(ModeShapeLexicon.EXTERNAL_NODE_KEY, systemSession).getFirstValue()
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

    /**
     * Add a new connector by supplying the component definition. This method instantiates, initializes, and then registers the
     * connector into this manager. If registration is successful, this method will replace any running connector already
     * registered with the same name.
     *
     * @param component the component describing the connector; may not be null
     * @throws RepositoryException if there is a problem initializing the connector
     */
    public void addConnector( Component component ) throws RepositoryException {
        Connector connector = instantiateConnector(component);
        if (initialized) {
            // We need to initialize the connector right away ...
            // Get a session that we'll pass to the sequencers to use for registering namespaces and node types
            Session session = null;
            try {
                // Get a session that we'll pass to the sequencers to use for registering namespaces and node types
                session = repository.loginInternalSession();
                NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
                NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

                if (!(nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
                    throw new IllegalStateException("Invalid node type manager (expected modeshape NodeTypeManager): "
                                                            + nodeTypeManager.getClass().getName());
                }
                initializeConnector(connector, registry, (org.modeshape.jcr.api.nodetype.NodeTypeManager)nodeTypeManager);
            } catch (IOException e) {
                String msg = JcrI18n.unableToInitializeConnector.text(component, repository.name(), e.getMessage());
                throw new RepositoryException(msg, e);
            } catch (RuntimeException e) {
                String msg = JcrI18n.unableToInitializeConnector.text(component, repository.name(), e.getMessage());
                throw new RepositoryException(msg, e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
        registerConnector(connector);
    }

    /**
     * Remove an existing connector registered with the supplied name.
     *
     * @param connectorName the name of the connector that should be removed; may not be null
     * @return true if the existing connector was found and removed, or false if there was no connector with the given name
     */
    public boolean removeConnector( String connectorName ) {
        String key = NodeKey.keyForSourceName(connectorName);
        Connector existing = sourceKeyToConnectorMap.remove(key);
        if (existing == null) {
            return false;
        }
        existing.shutdown();
        return true;
    }

    protected Connector instantiateConnector( Component component ) {
        try {
            // Instantiate the connector and set the 'name' field ...
            Connector connector = component.createInstance(getClass().getClassLoader());

            // Set the repository name field ...
            ReflectionUtil.setValue(connector, "repositoryName", repository.name());

            // Set the logger instance
            ReflectionUtil.setValue(connector, "logger", Logger.getLogger(connector.getClass()));

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
        ReflectionUtil.invokeAccessibly(connector, postInitialize, new Object[] { });
    }

    protected void registerConnector( Connector connector ) {
        String key = NodeKey.keyForSourceName(connector.getSourceName());
        Connector existing = sourceKeyToConnectorMap.put(key, connector);
        if (existing != null) {
            existing.shutdown();
        }
    }

    protected void shutdown() {
        if (!initialized || !hasConnectors()) {
            return;
        }
        shutdownConnectors();
        projections.clear();
    }

    private void shutdownConnectors() {
        for (String sourceName : sourceKeyToConnectorMap.keySet()) {
            sourceKeyToConnectorMap.get(sourceName).shutdown();
        }
        sourceKeyToConnectorMap.clear();
        sourceKeyToConnectorMap = null;
    }

    /**
     * Returns the connector which is mapped to the given source key.
     *
     * @param sourceKey a {@code non-null} {@link String}
     * @return either a {@link Connector} instance of {@code null}
     */
    public Connector getConnectorForSourceKey( String sourceKey ) {
        return sourceKeyToConnectorMap.get(sourceKey);
    }

    /**
     * Returns a connector which was registered for the given source name.
     *
     * @param sourceName a {@code non-null} String; the name of a source
     * @return either a {@link Connector} instance or {@code null}
     */
    public Connector getConnectorForSourceName( String sourceName ) {
        assert sourceName != null;
        return sourceKeyToConnectorMap.get(NodeKey.keyForSourceName(sourceName));
    }

    /**
     * Returns the repository's document translator.
     *
     * @return a {@link DocumentTranslator} instance.
     */
    public DocumentTranslator getDocumentTranslator() {
        return repository.repositoryCache().getDocumentTranslator();
    }

    /**
     * Checks if there are any registered connectors.
     *
     * @return {@code true} if any connectors are registered, {@code false} otherwise.
     */
    public boolean hasConnectors() {
        return !sourceKeyToConnectorMap.isEmpty();
    }

    protected class Projection {
        private final String externalNodeKey;
        private final String projectedNodeKey;
        private final String alias;

        protected Projection( String externalNodeKey,
                              String projectedNodeKey,
                              String alias) {
            this.externalNodeKey = externalNodeKey;
            this.alias = alias;
            this.projectedNodeKey = projectedNodeKey;
        }

        protected boolean hasAlias(String alias) {
            return this.alias.equalsIgnoreCase(alias);
        }

        protected boolean hasProjectedNodeKey(String projectedNodeKey) {
            return this.projectedNodeKey.equals(projectedNodeKey);
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

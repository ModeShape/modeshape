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
import java.util.Map;
import javax.jcr.NamespaceRegistry;
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
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.federation.Connector;
import org.modeshape.jcr.federation.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Class which maintains (based on the configuration) the list of available connectors for a repository.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com) //TODO author=Horia Chiorean date=11/1/12 description=This should get the
 *         configuration from the running state and initialize the connectors
 */
public class Connectors {

    private final Map<String, Connector> sourceKeyToConnectorMap = new HashMap<String, Connector>();
    private final JcrRepository.RunningState repository;
    private final Logger logger;
    private boolean initialized = false;

    public Connectors( JcrRepository.RunningState repository,
                       Collection<Component> components ) {
        this.repository = repository;
        this.logger = Logger.getLogger(getClass());
        for (Component component : components) {
            Connector connector = instantiateConnector(component);
            registerConnector(connector);
        }
    }

    protected void initialize() {
        if (initialized) {
            // nothing to do ...
            return;
        }

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

            // Initialize each connector using the supplied session ...
            for (Iterator<Map.Entry<String, Connector>> connectorsIterator = sourceKeyToConnectorMap.entrySet().iterator(); connectorsIterator.hasNext();) {
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
        if (existing == null) return false;
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

    protected void registerConnector( Connector connector ) {
        String key = NodeKey.keyForSourceName(connector.getSourceName());
        Connector existing = sourceKeyToConnectorMap.put(key, connector);
        if (existing != null) {
            existing.shutdown();
        }
    }

    public Connector getConnectorForSourceName( String sourceName ) {
        assert sourceName != null;
        return sourceKeyToConnectorMap.get(NodeKey.keyForSourceName(sourceName));
    }

    public Connector getConnectorForSourceKey( String sourceKey ) {
        return sourceKeyToConnectorMap.get(sourceKey);
    }

    public DocumentTranslator getDocumentTranslator() {
        return repository.repositoryCache().getDocumentTranslator();
    }

    public boolean hasConnectors() {
        return !sourceKeyToConnectorMap.isEmpty();
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
            if (entry == null) return NO_PROPERTIES;
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

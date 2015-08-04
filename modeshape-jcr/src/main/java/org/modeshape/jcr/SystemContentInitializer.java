/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache.ContentInitializer;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;

/**
 * The {@link ContentInitializer} implementation that populates the "/jcr:system" content for a new repository.
 */
class SystemContentInitializer implements ContentInitializer {

    protected static final String INDEXES_NODE_ID = "mode:indexes";
    protected static final String SYSTEM_NODE_ID = "jcr:system";

    private PropertyFactory propFactory;

    public SystemContentInitializer() {
    }

    @Override
    public void initialize( SessionCache session,
                            MutableCachedNode parent ) {
        this.propFactory = session.getContext().getPropertyFactory();
        MutableCachedNode system = null;
        MutableCachedNode namespaces = null;

        // Create the "/jcr:system" node ...
        system = createNode(session, parent, SYSTEM_NODE_ID, JcrLexicon.SYSTEM, ModeShapeLexicon.SYSTEM);

        // Create the "/jcr:system/jcr:nodeTypes" node ...
        createNode(session, system, "jcr:nodeTypes", JcrLexicon.NODE_TYPES, ModeShapeLexicon.NODE_TYPES);

        // Create the "/jcr:system/jcr:versionStorage" node which we don't want to index
        MutableCachedNode versionStorage = createNode(session, system, "jcr:versionStorage", JcrLexicon.VERSION_STORAGE, ModeShapeLexicon.VERSION_STORAGE);
        versionStorage.excludeFromSearch();

        // Create the "/jcr:system/mode:namespaces" node ...
        namespaces = createNode(session, system, "mode:namespaces", ModeShapeLexicon.NAMESPACES, ModeShapeLexicon.NAMESPACES);

        // Create the standard namespaces ...
        // createNamespace(session, namespaces, "", ""); // Don't initialize the "" namespaces
        createNamespace(session, namespaces, JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        createNamespace(session, namespaces, JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
        createNamespace(session, namespaces, JcrMixLexicon.Namespace.PREFIX, JcrMixLexicon.Namespace.URI);
        createNamespace(session, namespaces, JcrSvLexicon.Namespace.PREFIX, JcrSvLexicon.Namespace.URI);
        createNamespace(session, namespaces, ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        createNamespace(session, namespaces, JcrNamespaceRegistry.XML_NAMESPACE_PREFIX, JcrNamespaceRegistry.XML_NAMESPACE_URI);
        createNamespace(session,
                        namespaces,
                        JcrNamespaceRegistry.XMLNS_NAMESPACE_PREFIX,
                        JcrNamespaceRegistry.XMLNS_NAMESPACE_URI);
        createNamespace(session,
                        namespaces,
                        JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_PREFIX,
                        JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_URI);
        createNamespace(session,
                        namespaces,
                        JcrNamespaceRegistry.XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX,
                        JcrNamespaceRegistry.XML_SCHEMA_INSTANCE_NAMESPACE_URI);

        // Create the "/jcr:system/mode:locks" node which we don't want to index
        MutableCachedNode locks = createNode(session, system, "mode:locks", ModeShapeLexicon.LOCKS, ModeShapeLexicon.LOCKS);
        locks.excludeFromSearch();

        // Create the "/jcr:system/mode:indexes" node which we don't want to index
        MutableCachedNode indexes = createNode(session, system, INDEXES_NODE_ID, ModeShapeLexicon.INDEXES, ModeShapeLexicon.INDEXES);
        indexes.excludeFromSearch();
    }

    protected MutableCachedNode createNode( SessionCache session,
                                            MutableCachedNode parent,
                                            String id,
                                            Name name,
                                            Name primaryType,
                                            Property... properties ) {
        NodeKey key = session.getRootKey().withId(id);
        return parent.createChild(session, key, name, propFactory.create(JcrLexicon.PRIMARY_TYPE, primaryType), properties);
    }

    protected MutableCachedNode createNamespace( SessionCache session,
                                                 MutableCachedNode parent,
                                                 String prefix,
                                                 String uri ) {
        Name nodeName = prefix.length() != 0 ? session.getContext().getValueFactories().getNameFactory().create(prefix) : ModeShapeLexicon.NAMESPACE;
        return createNode(session,
                          parent,
                          "mode:namespaces-" + uri,
                          nodeName,
                          ModeShapeLexicon.NAMESPACE,
                          propFactory.create(ModeShapeLexicon.URI, uri),
                          propFactory.create(ModeShapeLexicon.GENERATED, Boolean.FALSE));
    }
}

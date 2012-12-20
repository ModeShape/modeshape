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
        system = createNode(session, parent, "jcr:system", JcrLexicon.SYSTEM, ModeShapeLexicon.SYSTEM);

        // Create the "/jcr:system/jcr:nodeTypes" node ...
        createNode(session, system, "jcr:nodeTypes", JcrLexicon.NODE_TYPES, ModeShapeLexicon.NODE_TYPES);

        // Create the "/jcr:system/jcr:versionStorage" node which we don't want to index
        MutableCachedNode versionStorage = createNode(session, system, "jcr:versionStorage", JcrLexicon.VERSION_STORAGE, ModeShapeLexicon.VERSION_STORAGE);
        versionStorage.setQueryable(false);

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
        locks.setQueryable(false);
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

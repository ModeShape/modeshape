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
package org.modeshape.jcr.cache.document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * 
 */
public abstract class AbstractSessionCache implements SessionCache, DocumentCache {

    @Immutable
    protected static final class BasicSaveContext implements SaveContext {
        private final DateTime now;
        private final String userId;

        protected BasicSaveContext( ExecutionContext context ) {
            this.now = context.getValueFactories().getDateFactory().create();
            this.userId = context.getSecurityContext().getUserName();
        }

        @Override
        public DateTime getTime() {
            return now;
        }

        @Override
        public String getUserId() {
            return userId;
        }
    }

    protected final WorkspaceCache workspaceCache;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final Path rootPath;
    private final SessionEnvironment sessionContext;

    private ExecutionContext context;

    protected AbstractSessionCache( ExecutionContext context,
                                    WorkspaceCache workspaceCache,
                                    SessionEnvironment sessionContext ) {
        this.context = context;
        this.workspaceCache = workspaceCache;
        ValueFactories factories = this.context.getValueFactories();
        this.nameFactory = factories.getNameFactory();
        this.pathFactory = factories.getPathFactory();
        this.rootPath = this.pathFactory.createRootPath();
        this.sessionContext = sessionContext;
    }

    @Override
    public final ExecutionContext getContext() {
        return context;
    }

    @Override
    public final WorkspaceCache workspaceCache() {
        return workspaceCache;
    }

    final DocumentTranslator translator() {
        return workspaceCache.translator();
    }

    final ExecutionContext context() {
        return context;
    }

    final NameFactory nameFactory() {
        return nameFactory;
    }

    final PathFactory pathFactory() {
        return pathFactory;
    }

    final Path rootPath() {
        return rootPath;
    }

    final SessionEnvironment sessionContext() {
        return sessionContext;
    }

    @Override
    public final void addContextData( String key,
                                      String value ) {
        this.context = context.with(key, value);
    }

    @Override
    public NodeKey createNodeKey() {
        return getRootKey().withId(generateIdentifier());
    }

    @Override
    public NodeKey createNodeKeyWithIdentifier( String identifier ) {
        return getRootKey().withId(identifier);
    }

    @Override
    public NodeKey createNodeKeyWithSource( String sourceName ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        return getRootKey().withSourceKeyAndId(sourceKey, generateIdentifier());
    }

    @Override
    public NodeKey createNodeKey( String sourceName,
                                  String identifier ) {
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        if (identifier == null) identifier = generateIdentifier();
        return getRootKey().withSourceKeyAndId(sourceKey, identifier);
    }

    protected String generateIdentifier() {
        return UUID.randomUUID().toString();
    }

    @Override
    public NodeKey getRootKey() {
        return workspaceCache.getRootKey();
    }

    @Override
    public NodeCache getWorkspace() {
        return workspaceCache;
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        return workspaceCache.getNode(key);
    }

    @Override
    public CachedNode getNode( ChildReference reference ) {
        return getNode(reference.getKey());
    }

    @Override
    public Set<NodeKey> getNodeKeysAtAndBelow( NodeKey nodeKey ) {
        CachedNode node = this.getNode(nodeKey);
        if (node == null) {
            return Collections.emptySet();
        }
        Set<NodeKey> result = new HashSet<NodeKey>();
        result.add(nodeKey);

        for (ChildReference reference : node.getChildReferences(this)) {
            NodeKey childKey = reference.getKey();
            result.addAll(getNodeKeysAtAndBelow(childKey));
        }
        return result;
    }

    @Override
    public abstract SessionNode mutable( NodeKey key );

}

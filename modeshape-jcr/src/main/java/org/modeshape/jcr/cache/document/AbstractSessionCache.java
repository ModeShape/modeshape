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
package org.modeshape.jcr.cache.document;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
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
    
    protected final Logger logger;
    
    private final WorkspaceCache sharedWorkspaceCache;
    private final AtomicReference<WorkspaceCache> workspaceCache = new AtomicReference<>();
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final Path rootPath;
    
    private ExecutionContext context;

    protected AbstractSessionCache(ExecutionContext context,
                                   WorkspaceCache sharedWorkspaceCache) {
        this.logger = Logger.getLogger(getClass());
        this.context = context;
        this.sharedWorkspaceCache = sharedWorkspaceCache;
        this.workspaceCache.set(sharedWorkspaceCache);
        ValueFactories factories = this.context.getValueFactories();
        this.nameFactory = factories.getNameFactory();
        this.pathFactory = factories.getPathFactory();
        this.rootPath = this.pathFactory.createRootPath();
    }
    
    @Override
    public final SessionCache unwrap() {
        return this;
    }

    protected final String workspaceName() {
        return workspaceCache().getWorkspaceName();
    }

    @Override
    public final ExecutionContext getContext() {
        return context;
    }

    @Override
    public final WorkspaceCache workspaceCache() {
        return workspaceCache.get();
    }

    final DocumentTranslator translator() {
        return workspaceCache().translator();
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
    
    final AbstractSessionCache setWorkspaceCache(WorkspaceCache cache) {
        this.workspaceCache.set(cache);
        return this;
    }
    
    final WorkspaceCache sharedWorkspaceCache() {
        return sharedWorkspaceCache;
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
        return workspaceCache().getRootKey();
    }

    @Override
    public WorkspaceCache getWorkspace() {
        return workspaceCache();
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        return workspaceCache().getNode(key);
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

    @Override
    public Iterator<NodeKey> getAllNodeKeys() {
        return getAllNodeKeysAtAndBelow(getRootKey());
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeysAtAndBelow( NodeKey startingKey ) {
        return new NodeCacheIterator(this, startingKey);
    }

    @Override
    public final void clear( CachedNode node ) {
        doClear(node);
        WorkspaceCache wscache = workspaceCache.get();
        if (wscache != sharedWorkspaceCache) {
            assert wscache instanceof TransactionalWorkspaceCache;
            wscache.clear();
        }
    }

    @Override
    public final void clear() {
        doClear();
        WorkspaceCache wscache = workspaceCache.get();
        if (wscache != sharedWorkspaceCache) {
            assert wscache instanceof TransactionalWorkspaceCache;
            wscache.clear();
        }
    }

    protected abstract void doClear( CachedNode node );

    protected abstract void doClear();
}

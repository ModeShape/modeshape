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

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.infinispan.api.BasicCache;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.util.concurrent.TimeoutException;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;

/**
 * A {@link NodeCache} implementation that uses Infinispan's {@link SchematicDb} for storage, with each node represented as a
 * single {@link Document}. The nodes in this cache represent the actual, unmodified values.
 */
public class WorkspaceCache implements DocumentCache, ChangeSetListener {

    protected static final Logger LOGGER = Logger.getLogger(WorkspaceCache.class);

    private final DocumentTranslator translator;
    private final ExecutionContext context;
    private final DocumentStore documentStore;
    private final ConcurrentMap<NodeKey, CachedNode> nodesByKey;
    private final NodeKey rootKey;
    private final ChildReference childReferenceForRoot;
    private final String repositoryKey;
    private final String workspaceName;
    private final String workspaceKey;
    private final String sourceKey;
    private final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final ChangeSetListener changeSetListener;
    private volatile boolean closed = false;

    public WorkspaceCache( ExecutionContext context,
                           String repositoryKey,
                           String workspaceName,
                           DocumentStore documentStore,
                           DocumentTranslator translator,
                           NodeKey rootKey,
                           ConcurrentMap<NodeKey, CachedNode> cache,
                           ChangeSetListener changeSetListener ) {
        this.context = context;
        this.documentStore = documentStore;
        this.changeSetListener = changeSetListener;
        this.translator = translator;
        this.rootKey = rootKey;
        this.childReferenceForRoot = new ChildReference(rootKey, Path.ROOT_NAME, 1);
        this.repositoryKey = repositoryKey;
        this.workspaceName = workspaceName;
        this.workspaceKey = rootKey.getWorkspaceKey();
        this.sourceKey = rootKey.getSourceKey();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.nodesByKey = cache;
    }

    protected WorkspaceCache( WorkspaceCache original,
                              ConcurrentMap<NodeKey, CachedNode> cache) {
        this.context = original.context;
        this.documentStore = original.documentStore;
        this.translator = original.translator;
        this.rootKey = original.rootKey;
        this.childReferenceForRoot = original.childReferenceForRoot;
        this.repositoryKey = original.repositoryKey;
        this.workspaceName = original.workspaceName;
        this.workspaceKey = original.workspaceKey;
        this.sourceKey = original.sourceKey;
        this.pathFactory = original.pathFactory;
        this.nameFactory = original.nameFactory;
        this.nodesByKey = cache;
        //this is not copied on purpose, because this ctr should only be used to create "transient" caches
        this.changeSetListener = null;
    }

    public void setMinimumStringLengthForBinaryStorage( long largeValueSize ) {
        assert largeValueSize > -1;
        this.translator.setMinimumStringLengthForBinaryStorage(largeValueSize);
    }

    @Override
    public final WorkspaceCache workspaceCache() {
        return this;
    }

    public final String getRepositoryKey() {
        return repositoryKey;
    }

    public final String getWorkspaceKey() {
        return workspaceKey;
    }

    public final String getWorkspaceName() {
        return workspaceName;
    }

    final DocumentTranslator translator() {
        return translator;
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
        return pathFactory().createRootPath();
    }

    final DocumentStore documentStore() {
        return documentStore;
    }

    final Document documentFor( String key ) {
        // Look up the information in the database ...
        SchematicEntry entry = documentStore.get(key);
        if (entry == null) {
            // There is no such node ...
            return null;
        }
        try {
            return entry.getContentAsDocument();
        } catch (IllegalStateException e) {
            LOGGER.debug("The document '{0}' was concurrently removed; returning null.", key);
            // The document was already removed
            return null;
        }
    }

    final Document blockFor( String key ) {
        return documentStore.getChildrenBlock(key);
    }

    final Document documentFor( NodeKey key ) {
        return documentFor(key.toString());
    }

    final ChildReference childReferenceForRoot() {
        return this.childReferenceForRoot;
    }

    final String sourceKey() {
        return sourceKey;
    }

    final void purge( Iterable<NodeKey> nodeKeys ) {
        for (NodeKey nodeKey : nodeKeys) {
            this.nodesByKey.remove(nodeKey);
        }
    }

    @Override
    public NodeKey getRootKey() {
        checkNotClosed();
        return rootKey;
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        checkNotClosed();
        CachedNode node = nodesByKey.get(key);
        if (node == null) {
            // Load the node from the database ...
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Node '{0}' is not found in the '{1}' workspace cache; looking in store", key, workspaceName);
            }
            Document doc = documentFor(key);
            if (doc != null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Materialized document '{0}' in '{1}' workspace from store: {2}", key, workspaceName, doc);
                }
                // Create a new node and put into this cache ...
                CachedNode newNode = new LazyCachedNode(key, doc);
                try {
                    Integer cacheTtlSeconds = translator().getCacheTtlSeconds(doc);
                    if (cacheTtlSeconds != null && nodesByKey instanceof BasicCache) {
                        node = ((BasicCache<NodeKey, CachedNode>)nodesByKey).putIfAbsent(key,
                                                                                         newNode,
                                                                                         cacheTtlSeconds.longValue(),
                                                                                         TimeUnit.SECONDS);
                    } else {
                        node = nodesByKey.putIfAbsent(key, newNode);
                    }
                } catch (TimeoutException e) {
                    node = null;
                }
                if (node == null) {
                    // Either the put timed out or there was no previous entry, so just use our new CachedNode ...
                    node = newNode;
                }
            }
        }
        return node;
    }

    @Override
    public CachedNode getNode( ChildReference reference ) {
        checkNotClosed();
        return getNode(reference.getKey());
    }

    public ChildReference getChildReference( NodeKey parentKey,
                                             NodeKey childKey ) {
        // Look up the information in the document store ...
        Document doc = documentStore.getChildReference(parentKey.toString(), childKey.toString());
        if (doc == null) return null;
        return translator.childReferenceFrom(doc);
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeys() {
        return getAllNodeKeysAtAndBelow(getRootKey());
    }

    @Override
    public Iterator<NodeKey> getAllNodeKeysAtAndBelow( NodeKey startingKey ) {
        return new NodeCacheIterator(this, startingKey);
    }

    @Override
    public void clear() {
        nodesByKey.clear();
    }

    @Override
    public void notify( ChangeSet changes ) {
        if (!closed) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Cache for workspace '{0}' received {1} changes from remote sessions: {2}",
                             workspaceName,
                             changes.size(),
                             changes);
            }
            // Clear this workspace's cached nodes (iteratively is okay since it's a ConcurrentMap) ...
            for (NodeKey key : changes.changedNodes()) {
                if (closed) break;
                nodesByKey.remove(key);
            }
        }
    }

    /**
     * Signal that changes have been made to the persisted data. Related information in the cache is cleared, and this workspace's
     * listener is notified of the changes.
     * 
     * @param changes the changes to be made; may not be null
     */
    public void changed( ChangeSet changes ) {
        checkNotClosed();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Cache for workspace '{0}' received {1} changes from local sessions: {2}",
                         workspaceName,
                         changes.size(),
                         changes);
        }
        // Clear this workspace's cached nodes (iteratively is okay since it's a ConcurrentMap) ...
        for (NodeKey key : changes.changedNodes()) {
            if (closed) break;
            nodesByKey.remove(key);
        }

        // Notify the listener ...
        if (changeSetListener != null) changeSetListener.notify(changes);
    }

    @Override
    public NodeCache unwrap() {
        return this;
    }

    protected final void checkNotClosed() {
        if (closed) {
            throw new WorkspaceNotFoundException(JcrI18n.workspaceHasBeenDeleted.text(getWorkspaceName()));
        }
    }

    public void signalDeleted() {
        this.closed = true;
        clear();
    }

    public void signalClosing() {
        this.closed = true;
    }

    public void signalClosed() {
        this.closed = true;
        clear();
    }

    /**
     * Checks if this ws cache is empty. An empty cache is considered when the only node key under root is the system key.
     * 
     * @return {@code true} if the system key is the only key under root, {@code false} other wise.
     */
    public boolean isEmpty() {
        CachedNode root = getNode(getRootKey());
        // expect there to be 1 child under root - the system key
        return root.getChildReferences(this).size() == 1;
    }

    @Override
    public String toString() {
        return workspaceName;
    }

    /**
     * Returns a workspace cache which has the latest persisted information read from Infinispan for the given nodes.
     * After reading each node from Infinispan, that node will also be updated/inserted into *this* workspace cache.
     *
     * @param nodeKeys an {@code Iterable} of {@code NodeKey}; may not be null
     * @return a workspace cache instance which only contains the latest persisted information for the requested nodes.
     */
    protected WorkspaceCache persistedCache(Iterable<NodeKey> nodeKeys) {
        ConcurrentHashMap<NodeKey, CachedNode> nodes = new ConcurrentHashMap<NodeKey, CachedNode>();
        for (NodeKey nodeKey : nodeKeys) {
            Document nodeData = documentFor(nodeKey);
            if (nodeData != null) {
                CachedNode persistedNode = new LazyCachedNode(nodeKey, nodeData);
                nodes.put(nodeKey, persistedNode);
                this.nodesByKey.put(nodeKey, persistedNode);
            }
        }
        return new WorkspaceCache(this, nodes);
    }
}

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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.infinispan.api.BasicCache;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
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

    public void setMinimumBinarySizeInBytes( long largeValueSize ) {
        assert largeValueSize > -1;
        this.translator.setLargeValueSize(largeValueSize);
    }

    @Override
    public final WorkspaceCache workspaceCache() {
        return this;
    }

    public final String getProcessKey() {
        return context.getProcessId();
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
        return entry.getContentAsDocument();
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
            Document doc = documentFor(key);
            if (doc != null) {
                // Create a new node and put into this cache ...
                CachedNode newNode = new LazyCachedNode(key, doc);
                Integer cacheTtlSeconds = translator().getCacheTtlSeconds(doc);
                if (nodesByKey instanceof BasicCache && cacheTtlSeconds != null) {
                    node = ((BasicCache<NodeKey, CachedNode>)nodesByKey).putIfAbsent(key,
                                                                                     newNode,
                                                                                     cacheTtlSeconds.longValue(),
                                                                                     TimeUnit.SECONDS);
                } else {
                    node = nodesByKey.putIfAbsent(key, newNode);
                }
                if (node == null) node = newNode;
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
    public void notify( ChangeSet changeSet ) {
        if (!closed) {
            // Clear this workspace's cached nodes (iteratively is okay since it's a ConcurrentMap) ...
            for (NodeKey key : changeSet.changedNodes()) {
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
        // Clear this workspace's cached nodes (iteratively is okay since it's a ConcurrentMap) ...
        for (NodeKey key : changes.changedNodes()) {
            if (closed) break;
            nodesByKey.remove(key);
        }

        // Notify the listener ...
        if (changeSetListener != null) changeSetListener.notify(changes);
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
}

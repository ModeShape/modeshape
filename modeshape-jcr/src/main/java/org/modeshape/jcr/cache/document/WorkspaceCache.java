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

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.bus.ChangeBus;
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
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;

/**
 * A {@link NodeCache} implementation that uses a concurrent LRU map to store nodes. The nodes in this cache represent the actual, 
 * unmodified values.
 */
public class WorkspaceCache implements DocumentCache {

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
    private final ChangeBus changeBus;
    private final ChangeSetListener systemChangeNotifier;
    private final ChangeSetListener nonSystemChangeNotifier;
    private final RepositoryEnvironment repositoryEnvironment;
    private volatile boolean closed = false;

    public WorkspaceCache( ExecutionContext context,
                           String repositoryKey,
                           String workspaceName,
                           WorkspaceCache systemWorkspace,
                           DocumentStore documentStore,
                           DocumentTranslator translator,
                           NodeKey rootKey,
                           ConcurrentMap<NodeKey, CachedNode> cache,
                           ChangeBus changeBus,
                           RepositoryEnvironment repositoryEnvironment) {
        assert context != null;
        assert repositoryKey != null;
        assert workspaceName != null;
        assert documentStore != null;
        assert translator != null;
        assert rootKey != null;
        assert cache != null;
        assert changeBus != null;
        this.context = context;
        this.documentStore = documentStore;
        this.changeBus = changeBus;
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
        this.repositoryEnvironment = repositoryEnvironment;
        if (systemWorkspace != null) {
            // This is not the system workspace, so we have to listen both asynchronously and synchronously ...
            this.systemChangeNotifier = new SystemChangeNotifier(systemWorkspace.getWorkspaceName());
            this.nonSystemChangeNotifier = new NonSystemChangeNotifier(systemWorkspace.getWorkspaceName());
            this.changeBus.registerInThread(this.systemChangeNotifier);
            this.changeBus.register(this.nonSystemChangeNotifier);
        } else {
            // This IS the system workspace, so we have to listen synchronously for changes ...
            this.nonSystemChangeNotifier = null;
            this.systemChangeNotifier = new SystemChangeNotifier(this.workspaceName);
            this.changeBus.registerInThread(this.systemChangeNotifier);
        }
    }

    protected WorkspaceCache( WorkspaceCache original,
                              ConcurrentMap<NodeKey, CachedNode> cache ) {
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
        this.repositoryEnvironment = original.repositoryEnvironment;
        this.nodesByKey = cache;
        this.systemChangeNotifier = null;
        this.nonSystemChangeNotifier = null;
        //the change bus is not copied on purpose because this ctr should only be used for creating lightweight, "transient" instances
        this.changeBus = null;
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
    
    final RepositoryEnvironment repositoryEnvironment() {
        return repositoryEnvironment;
    }

    final Document documentFor( String key ) {
        // Look up the information in the database ...
        SchematicEntry entry = documentStore.get(key);
        if (entry == null) {
            // There is no such node ...
            return null;
        }
        try {
            return entry.content();
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
    
    final void purge(NodeKey key) {
        this.nodesByKey.remove(key);
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
                if (translator.isCacheable(doc)) {
                    node = nodesByKey.putIfAbsent(key, newNode);
                }
                if (node == null) {
                    // there was no previous entry, so just use our new CachedNode ...
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

    protected void evictChangedNodes( ChangeSet changes ) {
        if (!closed) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Cache for workspace '{0}' received {1} changes from remote sessions: {2}", workspaceName,
                             changes.size(), changes);
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
            LOGGER.trace("Cache for workspace '{0}' received {1} changes from local sessions: {2}", workspaceName,
                         changes.size(), changes);
        }
        // Clear this workspace's cached nodes (iteratively is okay since it's a ConcurrentMap) ...
        for (NodeKey key : changes.changedNodes()) {
            if (closed) break;
            nodesByKey.remove(key);
        }

        // Send the changes to the change bus so that others can see them ...
        if (changeBus != null) changeBus.notify(changes);
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

    protected void loadFromDocumentStore(Set<String> keys) {
        this.documentStore.load(keys).forEach(entry -> {
            String key = entry.id();
            Document document = entry.content();
            NodeKey nodeKey = new NodeKey(key);
            // in some cases (user transactions) we may be replacing a node, but it's important to do so
            this.nodesByKey.put(nodeKey, new LazyCachedNode(nodeKey, document));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Read a fresh copy from the document store for '{0}' and stored it in the tx ws cache as '{1}'",
                             key, document);
            }
        });
    }

    /**
     * Signal that the workspace for this workspace cache has been deleted/destroyed, so this cache is not needed anymore.
     */
    public void signalDeleted() {
        this.closed = true;
        removeListeners();
        clear();
    }

    /**
     * Signal that this workspace cache is to be closed, and it can start reclaiming resources.
     */
    public void signalClosing() {
        this.closed = true;
        // We are closing (and the repository is shutting down), and while our listeners will eventually be removed,
        // we know we can remove them immediately and not process any events any further ...
        removeListeners();
    }

    /**
     * Signal that this workspace cache has been fully closed.
     */
    public void signalClosed() {
        this.closed = true;
        clear();
    }

    private void removeListeners() {
        if (this.changeBus != null) {
            // Unregister our listeners ...
            if (this.systemChangeNotifier != null) this.changeBus.unregister(systemChangeNotifier);
            if (this.nonSystemChangeNotifier != null) this.changeBus.unregister(nonSystemChangeNotifier);
        }
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

    protected final class SystemChangeNotifier implements ChangeSetListener {
        private final String systemWorkspaceName;

        protected SystemChangeNotifier( String systemWorkspaceName ) {
            this.systemWorkspaceName = systemWorkspaceName;
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
                // The change affects the 'system' workspace, and we likely have some system nodes cached. So we have
                // to clear out from our cache any changed system nodes
                evictChangedNodes(changeSet);
            }
        }
    }

    protected final class NonSystemChangeNotifier implements ChangeSetListener {
        private final String systemWorkspaceName;

        protected NonSystemChangeNotifier( String systemWorkspaceName ) {
            this.systemWorkspaceName = systemWorkspaceName;
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
                // we already processed it via SystemChangeNotifier ...
                return;
            }
            // A workspace cache might contain a cached node that is federated, shared, or a system node.
            // Because of this, those nodes might be changed in other workspaces and we might still have a cached
            // representation. Therefore, we need to expell all nodes that have changed, even if they are changed
            // in other workspaces ...
            evictChangedNodes(changeSet);
        }
    }
}

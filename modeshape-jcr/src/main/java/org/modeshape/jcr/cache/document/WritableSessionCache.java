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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.TimeoutException;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.AllPathsCache;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.DocumentAlreadyExistsException;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.cache.LockFailureException;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.cache.ReferentialIntegrityException;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.WrappedException;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.cache.document.SessionNode.ChangedAdditionalParents;
import org.modeshape.jcr.cache.document.SessionNode.ChangedChildren;
import org.modeshape.jcr.cache.document.SessionNode.LockChange;
import org.modeshape.jcr.cache.document.SessionNode.MixinChanges;
import org.modeshape.jcr.cache.document.SessionNode.ReferrerChanges;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.txn.Transactions.Transaction;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.binary.AbstractBinary;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * A writable {@link SessionCache} implementation capable of making transient changes and saving them.
 */
@ThreadSafe
public class WritableSessionCache extends AbstractSessionCache {

    /** An atomic counter used in each thread when issuing TRACE log messages to the #SAVE_LOGGER */
    private static final AtomicInteger SAVE_NUMBER = new AtomicInteger(1);
    /**
     * The (approximate) largest save number used. This needs to be large enough for concurrent writes, but since this is only
     * used in TRACE messages (not used in production), it is doubtful that it needs to be very large.
     */
    private static final int MAX_SAVE_NUMBER = 100;
    /**
     * The TRACE-level logger used to record the changes that are saved. Note that this log context is the same as the
     * transaction-related classes, so that simply enabling this log context will provide very useful TRACE logging.
     */
    private static final Logger SAVE_LOGGER = Logger.getLogger("org.modeshape.jcr.txn");

    protected static final Logger LOGGER = Logger.getLogger(WritableSessionCache.class);
    private static final NodeKey REMOVED_KEY = new NodeKey("REMOVED_NODE_SHOULD_NEVER_BE_PERSISTED");
    private static final SessionNode REMOVED = new SessionNode(REMOVED_KEY, false);
    private static final int MAX_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT = 4;
    private static final long PAUSE_TIME_BEFORE_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT = 50L;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<NodeKey, SessionNode> changedNodes;
    private Set<NodeKey> replacedNodes;
    private LinkedHashSet<NodeKey> changedNodesInOrder;
    private Map<NodeKey, ReferrerChanges> referrerChangesForRemovedNodes;
    private final Transactions txns;
    
    /**
     * Track the binary keys which are being referenced/unreferenced by nodes so they can be locked in ISPN
     */                        
    private final ConcurrentHashMap<NodeKey, Set<BinaryKey>> binaryReferencesByNodeKey; 

    /**
     * Create a new SessionCache that can be used for making changes to the workspace.
     *
     * @param context the execution context; may not be null
     * @param workspaceCache the (shared) workspace cache; may not be null
     * @param repositoryEnvironment the context for the session; may not be null
     */
    public WritableSessionCache( ExecutionContext context,
                                 WorkspaceCache workspaceCache,
                                 RepositoryEnvironment repositoryEnvironment ) {
        super(context, workspaceCache, repositoryEnvironment);
        this.changedNodes = new HashMap<NodeKey, SessionNode>();
        this.changedNodesInOrder = new LinkedHashSet<NodeKey>();
        this.referrerChangesForRemovedNodes = new HashMap<NodeKey, ReferrerChanges>();
        this.binaryReferencesByNodeKey = new ConcurrentHashMap<NodeKey, Set<BinaryKey>>();
        this.txns = repositoryEnvironment.getTransactions();
    }

    protected final void assertInSession( SessionNode node ) {
        assert this.changedNodes.get(node.getKey()) == node : "Node " + node.getKey() + " is not in this session";
    }
    
    protected NodeTypes nodeTypes() {
        RepositoryEnvironment repositoryEnvironment = workspaceCache().repositoryEnvironment();
        if (repositoryEnvironment != null) {
            return repositoryEnvironment.nodeTypes();
        }
        return null;
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    public CachedNode getNode( NodeKey key ) {
        CachedNode sessionNode = null;
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            sessionNode = changedNodes.get(key);
        } finally {
            lock.unlock();
        }
        if (sessionNode == REMOVED) {
            // This node's been removed ...
            return null;
        }
        return sessionNode != null ? sessionNode : super.getNode(key);
    }

    @Override
    public SessionNode mutable( NodeKey key ) {
        SessionNode sessionNode = null;
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            sessionNode = changedNodes.get(key);
        } finally {
            lock.unlock();
        }
        if (sessionNode == null || sessionNode == REMOVED) {
            sessionNode = new SessionNode(key, false);
            lock = this.lock.writeLock();
            try {
                lock.lock();
                sessionNode = changedNodes.get(key);
                if (sessionNode == null) {
                    sessionNode = new SessionNode(key, false);
                    changedNodes.put(key, sessionNode);
                    changedNodesInOrder.add(key);
                }
            } finally {
                lock.unlock();
            }
        } else {
            // The node was found in the 'changedNodes', but it may not be in 'changedNodesInOrder'
            // (if the JCR client is using transactions and there were multiple saves), so make sure it's there ...
            if (!changedNodesInOrder.contains(key)) {
                changedNodesInOrder.add(key);
            }
        }
        return sessionNode;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    protected void doClear() {
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            referrerChangesForRemovedNodes.clear();
            changedNodes.clear();
            changedNodesInOrder.clear();
            referrerChangesForRemovedNodes.clear();
            binaryReferencesByNodeKey.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doClear( CachedNode node ) {
        final Path nodePath = node.getPath(this);
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            // we must first remove the children and only then the parents, otherwise child paths won't be found
            List<SessionNode> nodesToRemoveInOrder = getChangedNodesAtOrBelowChildrenFirst(nodePath);
            for (SessionNode nodeToRemove : nodesToRemoveInOrder) {
                NodeKey key = nodeToRemove.getKey();
                changedNodes.remove(key);
                changedNodesInOrder.remove(key);
            }
            NodeKey key = node.getKey();
            referrerChangesForRemovedNodes.remove(key);
            binaryReferencesByNodeKey.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the list of changed nodes at or below the given path, starting with the children.
     *
     * @param nodePath the path of the parent node
     * @return the list of changed nodes
     */
    private List<SessionNode> getChangedNodesAtOrBelowChildrenFirst( Path nodePath ) {
        List<SessionNode> changedNodesChildrenFirst = new ArrayList<SessionNode>();
        for (NodeKey key : changedNodes.keySet()) {
            SessionNode changedNode = changedNodes.get(key);
            boolean isAtOrBelow = false;
            try {
                isAtOrBelow = changedNode.isAtOrBelow(this, nodePath);
            } catch (NodeNotFoundException e) {
                isAtOrBelow = false;
            }

            if (!isAtOrBelow) {
                continue;
            }

            int insertIndex = changedNodesChildrenFirst.size();
            Path changedNodePath = changedNode.getPath(this);
            for (int i = 0; i < changedNodesChildrenFirst.size(); i++) {
                if (changedNodesChildrenFirst.get(i).getPath(this).isAncestorOf(changedNodePath)) {
                    insertIndex = i;
                    break;
                }
            }
            changedNodesChildrenFirst.add(insertIndex, changedNode);
        }
        return changedNodesChildrenFirst;
    }

    @Override
    public Set<NodeKey> getChangedNodeKeys() {
        Lock readLock = this.lock.readLock();
        try {
            readLock.lock();
            return new HashSet<NodeKey>(changedNodes.keySet());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<NodeKey> getChangedNodeKeysAtOrBelow( CachedNode srcNode ) {
        CheckArg.isNotNull(srcNode, "srcNode");
        final Path sourcePath = srcNode.getPath(this);
        WorkspaceCache workspaceCache = workspaceCache();

        // Create a path cache so that we don't recompute the path for the same node more than once ...
        AllPathsCache allPathsCache = new AllPathsCache(this, workspaceCache, context()) {
            @Override
            protected Set<NodeKey> getAdditionalParentKeys( CachedNode node,
                                                            NodeCache cache ) {
                Set<NodeKey> keys = super.getAdditionalParentKeys(node, cache);
                if (node instanceof SessionNode) {
                    SessionNode sessionNode = (SessionNode)node;
                    // Per the JCR TCK, we have to consider the nodes that *used to be* shared nodes before this
                    // session removed them, so we need to include the keys of the additional parents that were removed ...
                    ChangedAdditionalParents changed = sessionNode.additionalParents();
                    if (changed != null) {
                        keys = new HashSet<NodeKey>(keys);
                        keys.addAll(sessionNode.additionalParents().getRemovals());
                    }
                }
                return keys;
            }
        };

        Lock readLock = this.lock.readLock();
        Set<NodeKey> result = new HashSet<NodeKey>();
        try {
            readLock.lock();
            for (Map.Entry<NodeKey, SessionNode> entry : changedNodes.entrySet()) {
                SessionNode changedNodeThisSession = entry.getValue();
                NodeKey changedNodeKey = entry.getKey();
                CachedNode changedNode = null;

                if (changedNodeThisSession == REMOVED) {
                    CachedNode persistentRemovedNode = workspaceCache.getNode(changedNodeKey);
                    if (persistentRemovedNode == null) {
                        // the node has been removed without having been persisted previously, so we'll take it into account
                        result.add(changedNodeKey);
                        continue;
                    }
                    changedNode = persistentRemovedNode;
                } else {
                    changedNode = changedNodeThisSession;
                }

                // Compute all of the valid paths by which this node can be accessed. If *any* of these paths
                // are below the source path, then the node should be included in the result ...
                for (Path validPath : allPathsCache.getPaths(changedNode)) {
                    if (validPath.isAtOrBelow(sourcePath)) {
                        // The changed node is directly below the source node ...
                        result.add(changedNodeKey);
                        break;
                    }
                }
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean hasChanges() {
        Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return !changedNodesInOrder.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    protected final void logChangesBeingSaved( Iterable<NodeKey> firstNodesInOrder,
                                               Map<NodeKey, SessionNode> firstNodes,
                                               Iterable<NodeKey> secondNodesInOrder,
                                               Map<NodeKey, SessionNode> secondNodes ) {
        if (SAVE_LOGGER.isTraceEnabled()) {
            String txn = txns.currentTransactionId();

            // // Determine if there are any changes to be made. Note that this number is generally between 1 and 100,
            // // though for high concurrency some numbers may go above 100. However, the 100th save will always reset
            // // the counter back down to 1. (Any thread that got a save number above 100 will simply use it.)
            final int s = SAVE_NUMBER.getAndIncrement();
            if (s == MAX_SAVE_NUMBER) SAVE_NUMBER.set(1); // only the 100th
            int changes = 0;

            // There are at least some changes ...
            ExecutionContext context = getContext();
            String id = context.getId();
            String username = context.getSecurityContext().getUserName();
            NamespaceRegistry registry = context.getNamespaceRegistry();
            if (username == null) username = "<anonymous>";
            SAVE_LOGGER.trace("Save #{0} (part of transaction '{1}') by session {2}({3}) is persisting the following changes:",
                              s, txn, username, id);
            for (NodeKey key : firstNodesInOrder) {
                SessionNode node = changedNodes.get(key);
                if (node != null && node.hasChanges()) {
                    SAVE_LOGGER.trace(" #{0} {1}", s, node.getString(registry));
                    ++changes;
                }
            }
            if (secondNodesInOrder != null) {
                for (NodeKey key : secondNodesInOrder) {
                    SessionNode node = changedNodes.get(key);
                    if (node != null && node.hasChanges()) {
                        SAVE_LOGGER.trace(" #{0} {1}", s, node.getString(registry));
                        ++changes;
                    }
                }
            }
            SAVE_LOGGER.trace("Save #{0} (part of transaction '{1}') by session {2}({3}) completed persisting changes to {4} nodes",
                              s, txn, username, id, changes);
        }
    }

    /**
     * Persist the changes within a transaction.
     *
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     */
    @Override
    public void save() {
        save((PreSave)null);
    }

    protected void save( PreSave preSaveOperation ) {
        if (!this.hasChanges()) {
            return;
        }

        ChangeSet events = null;
        Lock lock = this.lock.writeLock();
        Transaction txn = null;
        try {
            lock.lock();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
            runPreSaveBeforeTransaction(preSaveOperation);

            final int numNodes = this.changedNodes.size();

            int repeat = txns.isCurrentlyInTransaction() ? 1 : MAX_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT;
            while (--repeat >= 0) {
                try {
                    // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                    txn = txns.begin();
                    assert txn != null;

                    // Lock the nodes in Infinispan and bring the latest version of these nodes in the shared workspace cache
                    WorkspaceCache persistedCache = lockNodes(changedNodesInOrder);

                    // We've either started our own transaction or one was already started. In either case *we must* make 
                    // sure we're not using the shared workspace cache as the active workspace cache. This is because the
                    // the shared workspace cache is "shared" with all others (incl readers) and we might end up placing entries in the 
                    // shared workspace cache which are being edited by us & this can happen after we've placed in the ISPN
                    // cache the clone for each document we're editing. Remember that for each active transaction ISPN will
                    // see the "most recent" object reference which was placed in the cache. See SchematicEntryLiteral#edit
                    // The shared workspace cache can become corrupted whenever methods like `node.getPrimaryType(this)` are
                    // called from the #persistChanges method

                    checkForTransaction();
                    
                    // process after locking
                    runPreSaveAfterLocking(preSaveOperation, persistedCache);

                    // Now persist the changes ...
                    logChangesBeingSaved(this.changedNodesInOrder, this.changedNodes, null, null);
                    events = persistChanges(this.changedNodesInOrder, persistedCache);

                    // If there are any binary changes, add a function which will update the binary store
                    if (events.hasBinaryChanges()) {
                        txn.uponCommit(binaryUsageUpdateFunction(events.usedBinaries(), events.unusedBinaries()));
                    }

                    LOGGER.debug("Altered {0} node(s)", numNodes);

                    // Commit the transaction ...
                    txn.commit();

                    clearState();

                } catch (org.infinispan.util.concurrent.TimeoutException e) {
                    txn.rollback();
                    if (repeat <= 0) {
                        throw new TimeoutException(e.getMessage(), e);
                    }
                    Thread.sleep(PAUSE_TIME_BEFORE_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT);
                    continue;
                } catch (NotSupportedException err) {
                    // No nested transactions are supported ...
                    throw new SystemFailureException(err);
                } catch (SecurityException err) {
                    // No privilege to commit ...
                    throw new SystemFailureException(err);
                } catch (IllegalStateException err) {
                    // Not associated with a txn??
                    throw new SystemFailureException(err);
                } catch (RollbackException err) {
                    // Couldn't be committed, but the txn is already rolled back ...
                    return;
                } catch (HeuristicMixedException err) {
                    // Rollback has occurred ...
                    return;
                } catch (HeuristicRollbackException err) {
                    // Rollback has occurred ...
                    return;
                } catch (SystemException err) {
                    // System failed unexpectedly ...
                    throw new SystemFailureException(err);
                } catch (Throwable t) {
                    // any other exception/error we should rollback
                    if (txn != null) {
                        txn.rollback();
                    }
                    // let the exception bubble up
                    throw t;
                }

                // If we've made it this far, we should never repeat ...
                break;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new WrappedException(t);
        } finally {
            lock.unlock();
        }

        txns.updateCache(workspaceCache(), events, txn);
    }

    private void runPreSaveBeforeTransaction( PreSave preSaveOperation ) throws Exception {
        if (preSaveOperation != null) {
            SaveContext saveContext = new BasicSaveContext(context());
            for (MutableCachedNode node : this.changedNodes.values()) {
                if (node == REMOVED) {
                    continue;
                }
                checkNodeNotRemovedByAnotherTransaction(node);
                preSaveOperation.process(node, saveContext);

            }
        }
    }

    private void runPreSaveAfterLocking( PreSave preSaveOperation,
                                         NodeCache persistedCache ) throws Exception {
        if (preSaveOperation != null) {
            SaveContext saveContext = new BasicSaveContext(context());
            for (MutableCachedNode node : this.changedNodes.values()) {
                // only process existing nodes that have not been removed
                if (node == REMOVED || node.isNew()) {
                    continue;
                }
                preSaveOperation.processAfterLocking(node, saveContext, persistedCache);
            }
        }
    }

    protected void clearState() {
        // The changes have been made, so create a new map (we're using the keys from the current map) ...
        this.changedNodes = new HashMap<NodeKey, SessionNode>();
        this.referrerChangesForRemovedNodes.clear();
        this.changedNodesInOrder.clear();
        this.binaryReferencesByNodeKey.clear();
        this.replacedNodes = null;
        this.checkForTransaction();
    }

    protected void clearState( Iterable<NodeKey> savedNodesInOrder ) {
        // The changes have been made, so remove the changes from this session's map ...
        for (NodeKey savedNode : savedNodesInOrder) {
            this.changedNodes.remove(savedNode);
            this.changedNodesInOrder.remove(savedNode);
            if (this.replacedNodes != null) {
                this.replacedNodes.remove(savedNode);
            }
            this.referrerChangesForRemovedNodes.remove(savedNode);
            this.binaryReferencesByNodeKey.remove(savedNode);
        }
        this.checkForTransaction();
    }

    @Override
    public void save( SessionCache other,
                      PreSave preSaveOperation ) {

        // Try getting locks on both sessions ...
        final WritableSessionCache that = (WritableSessionCache)other.unwrap();
        Lock thisLock = this.lock.writeLock();
        Lock thatLock = that.lock.writeLock();

        ChangeSet events1 = null;
        ChangeSet events2 = null;
        Transaction txn = null;
        try {
            thisLock.lock();
            thatLock.lock();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
            runPreSaveBeforeTransaction(preSaveOperation);

            final int numNodes = this.changedNodes.size() + that.changedNodes.size();

            int repeat = txns.isCurrentlyInTransaction() ? 1 : MAX_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT;
            while (--repeat >= 0) {
                try {
                    // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                    txn = txns.begin();
                    assert txn != null;

                    // Get a monitor via the transaction ...
                    try {
                        // Lock the nodes in Infinispan  and bring the latest version of these nodes in the shared workspace cache
                        WorkspaceCache thisPersistedCache = lockNodes(this.changedNodesInOrder);
                        WorkspaceCache thatPersistedCache = that.lockNodes(that.changedNodesInOrder);
                       
                        // We've either started our own transaction or one was already started. In either case *we must* make 
                        // sure we're not using the shared workspace cache as the active workspace cache. This is because the
                        // the shared workspace cache is "shared" with all others (incl readers) and we might end up placing entries in the 
                        // shared workspace cache which are being edited by us - this can happen after we've placed in the ISPN
                        // cache the clone for each document we're editing. Remember that for each active transaction ISPN will
                        // see the "most recent" object reference which was placed in the cache. See SchematicEntryLiteral#edit
                        // The shared workspace cache can become corrupted whenever methods like `node.getPrimaryType(this)` are
                        // called from the #persistChanges method

                        this.checkForTransaction();
                        that.checkForTransaction();
                        
                        // process after locking
                        runPreSaveAfterLocking(preSaveOperation, thisPersistedCache);

                        // Now persist the changes ...
                        logChangesBeingSaved(this.changedNodesInOrder, this.changedNodes, that.changedNodesInOrder,
                                             that.changedNodes);
                        events1 = persistChanges(this.changedNodesInOrder, thisPersistedCache);
                        // If there are any binary changes, add a function which will update the binary store
                        if (events1.hasBinaryChanges()) {
                            txn.uponCommit(binaryUsageUpdateFunction(events1.usedBinaries(), events1.unusedBinaries()));
                        }
                        events2 = that.persistChanges(that.changedNodesInOrder, thatPersistedCache);
                        if (events2.hasBinaryChanges()) {
                            txn.uponCommit(binaryUsageUpdateFunction(events2.usedBinaries(), events2.unusedBinaries()));
                        }
                    } catch (org.infinispan.util.concurrent.TimeoutException e) {
                        txn.rollback();
                        if (repeat <= 0) throw new TimeoutException(e.getMessage(), e);
                        --repeat;
                        Thread.sleep(PAUSE_TIME_BEFORE_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT);
                        continue;
                    } catch (IllegalStateException err) {
                        // Not associated with a txn??
                        throw new SystemFailureException(err);
                    } catch (IllegalArgumentException err) {
                        // Not associated with a txn??
                        throw new SystemFailureException(err);
                    } catch (Exception e) {
                        LOGGER.debug(e, "Error while attempting to save");
                        // Some error occurred (likely within our code) ...
                        txn.rollback();
                        throw e;
                    }

                    LOGGER.debug("Altered {0} node(s)", numNodes);

                    // Commit the transaction ...
                    txn.commit();

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Altered {0} keys: {1}", numNodes, this.changedNodes.keySet());
                    }

                    this.clearState();
                    that.clearState();

                } catch (NotSupportedException err) {
                    // No nested transactions are supported ...
                    return;
                } catch (SecurityException err) {
                    // No privilege to commit ...
                    throw new SystemFailureException(err);
                } catch (IllegalStateException err) {
                    // Not associated with a txn??
                    throw new SystemFailureException(err);
                } catch (RollbackException err) {
                    // Couldn't be committed, but the txn is already rolled back ...
                    return;
                } catch (HeuristicMixedException err) {
                } catch (HeuristicRollbackException err) {
                    // Rollback has occurred ...
                    return;
                } catch (SystemException err) {
                    // System failed unexpectedly ...
                    throw new SystemFailureException(err);
                }

                // If we've made it this far, we should never repeat ...
                break;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WrappedException(e);
        } finally {
            try {
                thatLock.unlock();
            } finally {
                thisLock.unlock();
            }
        }

        // TODO: Events ... these events should be combined, but cannot each ChangeSet only has a single workspace
        // Notify the workspaces of the changes made. This is done outside of our lock but still before the save returns ...
        txns.updateCache(this.workspaceCache(), events1, txn);
        txns.updateCache(that.workspaceCache(), events2, txn);
    }

    private void checkNodeNotRemovedByAnotherTransaction( MutableCachedNode node ) {
        String keyString = node.getKey().toString();
        // if the node is not new and also missing from the document, another transaction has deleted it
        if (!node.isNew() && !workspaceCache().documentStore().containsKey(keyString)) {
            throw new DocumentNotFoundException(keyString);
        }
    }

    /**
     * This method saves the changes made by both sessions within a single transaction. <b>Note that this must be used with
     * caution, as this method attempts to get write locks on both sessions, meaning they <i>cannot<i> be concurrently used
     * elsewhere (otherwise deadlocks might occur).</b>
     *
     * @param toBeSaved the set of keys identifying the nodes whose changes should be saved; may not be null
     * @param other the other session
     * @param preSaveOperation the pre-save operation
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     * @throws DocumentStoreException if there is a problem storing or retrieving a document
     */
    @Override
    public void save( Set<NodeKey> toBeSaved,
                      SessionCache other,
                      PreSave preSaveOperation ) {

        // Try getting locks on both sessions ...
        final WritableSessionCache that = (WritableSessionCache)other.unwrap();
        Lock thisLock = this.lock.writeLock();
        Lock thatLock = that.lock.writeLock();

        ChangeSet events1 = null;
        ChangeSet events2 = null;
        Transaction txn = null;
        try {
            thisLock.lock();
            thatLock.lock();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes below the path ...
            final List<NodeKey> savedNodesInOrder = new LinkedList<NodeKey>();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
            if (preSaveOperation != null) {
                SaveContext saveContext = new BasicSaveContext(context());
                for (MutableCachedNode node : this.changedNodes.values()) {
                    if (node == REMOVED || !toBeSaved.contains(node.getKey())) {
                        continue;
                    }
                    checkNodeNotRemovedByAnotherTransaction(node);
                    preSaveOperation.process(node, saveContext);
                    savedNodesInOrder.add(node.getKey());
                }
            }

            final int numNodes = savedNodesInOrder.size() + that.changedNodesInOrder.size();

            int repeat = txns.isCurrentlyInTransaction() ? 1 : MAX_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT;
            while (--repeat >= 0) {
                try {
                    // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                    txn = txns.begin();
                    assert txn != null;

                    try {
                        // Lock the nodes in Infinispan and bring the latest version of these nodes in the shared workspace cache
                        WorkspaceCache thisPersistedCache = lockNodes(savedNodesInOrder);
                        WorkspaceCache thatPersistedCache = that.lockNodes(that.changedNodesInOrder);

                        // We've either started our own transaction or one was already started. In either case *we must* make 
                        // sure we're not using the shared workspace cache as the active workspace cache. This is because the
                        // the shared workspace cache is "shared" with all others (incl readers) and we might end up placing entries in the 
                        // shared workspace cache which are being edited by us - this can happen after we've placed in the ISPN
                        // cache the clone for each document we're editing. Remember that for each active transaction ISPN will
                        // see the "most recent" object reference which was placed in the cache. See SchematicEntryLiteral#edit
                        // The shared workspace cache can become corrupted whenever methods like `node.getPrimaryType(this)` are
                        // called from the #persistChanges method
                        this.checkForTransaction();
                        that.checkForTransaction();
                        
                        // process after locking
                        // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
                        if (preSaveOperation != null) {
                            SaveContext saveContext = new BasicSaveContext(context());
                            for (MutableCachedNode node : this.changedNodes.values()) {
                                if (node == REMOVED || !toBeSaved.contains(node.getKey())) {
                                    continue;
                                }
                                preSaveOperation.processAfterLocking(node, saveContext, thisPersistedCache);
                            }
                        }

                        // Now persist the changes ...
                        logChangesBeingSaved(savedNodesInOrder, this.changedNodes, that.changedNodesInOrder, that.changedNodes);
                        events1 = persistChanges(savedNodesInOrder, thisPersistedCache);
                        // If there are any binary changes, add a function which will update the binary store
                        if (events1.hasBinaryChanges()) {
                            txn.uponCommit(binaryUsageUpdateFunction(events1.usedBinaries(), events1.unusedBinaries()));
                        }
                        events2 = that.persistChanges(that.changedNodesInOrder, thatPersistedCache);
                        if (events2.hasBinaryChanges()) {
                            txn.uponCommit(binaryUsageUpdateFunction(events2.usedBinaries(), events2.unusedBinaries()));
                        }
                    } catch (org.infinispan.util.concurrent.TimeoutException e) {
                        txn.rollback();
                        if (repeat <= 0) throw new TimeoutException(e.getMessage(), e);
                        --repeat;
                        Thread.sleep(PAUSE_TIME_BEFORE_REPEAT_FOR_LOCK_ACQUISITION_TIMEOUT);
                        continue;
                    } catch (IllegalStateException err) {
                        // Not associated with a txn??
                        throw new SystemFailureException(err);
                    } catch (IllegalArgumentException err) {
                        // Not associated with a txn??
                        throw new SystemFailureException(err);
                    } catch (Exception e) {
                        // Some error occurred (likely within our code) ...
                        txn.rollback();
                        throw e;
                    }

                    LOGGER.debug("Altered {0} node(s)", numNodes);

                    // Commit the transaction ...
                    txn.commit();

                    clearState(savedNodesInOrder);
                    that.clearState();

                } catch (NotSupportedException err) {
                    // No nested transactions are supported ...
                    return;
                } catch (SecurityException err) {
                    // No privilege to commit ...
                    throw new SystemFailureException(err);
                } catch (IllegalStateException err) {
                    // Not associated with a txn??
                    throw new SystemFailureException(err);
                } catch (RollbackException err) {
                    // Couldn't be committed, but the txn is already rolled back ...
                    return;
                } catch (HeuristicMixedException err) {
                } catch (HeuristicRollbackException err) {
                    // Rollback has occurred ...
                    return;
                } catch (SystemException err) {
                    // System failed unexpectedly ...
                    throw new SystemFailureException(err);
                }

                // If we've made it this far, we should never repeat ...
                break;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WrappedException(e);
        } finally {
            try {
                thatLock.unlock();
            } finally {
                thisLock.unlock();
            }
        }

        // TODO: Events ... these events should be combined, but cannot each ChangeSet only has a single workspace
        txns.updateCache(this.workspaceCache(), events1, txn);
        txns.updateCache(that.workspaceCache(), events2, txn);
    }

    /**
     * Persist the changes within an already-established transaction.
     *
     * @param changedNodesInOrder the nodes that are to be persisted; may not be null
     * @param persistedCache a view of the existing (persisted) nodes which are going to be modified.
     * @return the ChangeSet encapsulating the changes that were made
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     */
    @GuardedBy( "lock" )
    protected ChangeSet persistChanges( Iterable<NodeKey> changedNodesInOrder,
                                        WorkspaceCache persistedCache ) {
        // Compute the save meta-info ...
        ExecutionContext context = context();
        String userId = context.getSecurityContext().getUserName();
        Map<String, String> userData = context.getData();
        final boolean acquireLock = false; // we already pre-locked all of the existing documents that we'll edit ...
        DateTime timestamp = context.getValueFactories().getDateFactory().create();
        String workspaceName = persistedCache.getWorkspaceName();
        String repositoryKey = persistedCache.getRepositoryKey();
        RecordingChanges changes = new RecordingChanges(context.getId(), context.getProcessId(), repositoryKey, workspaceName,
                                                        sessionContext().journalId());

        // Get the documentStore ...
        DocumentStore documentStore = persistedCache.documentStore();
        DocumentTranslator translator = persistedCache.translator();

        PathCache sessionPaths = new PathCache(this);
        PathCache workspacePaths = new PathCache(persistedCache);

        Set<NodeKey> removedNodes = null;
        Set<NodeKey> removedUnorderedCollections = null;
        Set<BinaryKey> unusedBinaryKeys = new HashSet<>();
        Set<BinaryKey> usedBinaryKeys = new HashSet<>();
        Set<NodeKey> renamedExternalNodes = new HashSet<>();
        Map<NodeKey, Map<BucketId, Set<NodeKey>>> unorderedCollectionBucketRemovals = null;
        
        NodeTypes nodeTypes = nodeTypes();
        for (NodeKey key : changedNodesInOrder) {
            SessionNode node = changedNodes.get(key);
            String keyStr = key.toString();
            boolean isExternal = !node.getKey().getSourceKey().equalsIgnoreCase(workspaceCache().getRootKey().getSourceKey());
            if (node == REMOVED) {
                // We need to read some information from the node before we remove it ...
                CachedNode persisted = persistedCache.getNode(key);
                if (persisted != null) {
                    // This was a persistent node, so we have to generate an event and deal with the remove ...
                    if (removedNodes == null) {
                        removedNodes = new HashSet<>();
                    }
                    Name primaryType = persisted.getPrimaryType(this);
                    Set<Name> mixinTypes = persisted.getMixinTypes(this);
                    boolean isUnorderedCollection = nodeTypes != null && nodeTypes.isUnorderedCollection(primaryType, mixinTypes);
                    if (isUnorderedCollection && removedUnorderedCollections == null) {
                        removedUnorderedCollections = new HashSet<>();
                    }
                   
                    Path path = workspacePaths.getPath(persisted);
                    NodeKey parentKey = persisted.getParentKey(persistedCache);
                    CachedNode parent = getNode(parentKey);
                    Name parentPrimaryType;
                    Set<Name> parentMixinTypes;
                    if (parent != null) {
                        // the parent is loaded in this session
                        parentPrimaryType = parent.getPrimaryType(this);
                        parentMixinTypes = parent.getMixinTypes(this);
                    } else {
                        // the parent has been removed
                        parent = persistedCache.getNode(parentKey);
                        parentPrimaryType = parent.getPrimaryType(persistedCache);
                        parentMixinTypes = parent.getMixinTypes(persistedCache);
                    }
                    changes.nodeRemoved(key, parentKey, path, primaryType, mixinTypes, parentPrimaryType, parentMixinTypes);
                    removedNodes.add(key);
                    if (isUnorderedCollection) {
                        removedUnorderedCollections.add(key);
                    }

                    // if there were any referrer changes for the removed nodes, we need to process them
                    ReferrerChanges referrerChanges = referrerChangesForRemovedNodes.get(key);
                    if (referrerChanges != null) {
                        EditableDocument doc = documentStore.edit(keyStr, false, acquireLock);
                        if (doc != null) translator.changeReferrers(doc, referrerChanges);
                    }

                    // if the node had any binary properties, make sure we decrement the ref count of each
                    for (Iterator<Property> propertyIterator = persisted.getProperties(persistedCache); propertyIterator.hasNext();) {
                        Property property = propertyIterator.next();
                        if (property.isBinary()) {
                            Object value = property.isMultiple() ? Arrays.asList(property.getValuesAsArray()) : property.getFirstValue();
                            translator.decrementBinaryReferenceCount(value, unusedBinaryKeys, null);
                        }
                    }

                    // Note 1: Do not actually remove the document from the documentStore yet; see below (note 2)
                }
                // Otherwise, the removed node was created in the session (but not ever persisted),
                // so we don't have to do anything ...
            } else {
                // Get the primary and mixin type names; even though we're passing in the session, the two properties
                // should be there and shouldn't require a looking in the cache...
                Name primaryType = node.getPrimaryType(this);
                Set<Name> mixinTypes = node.getMixinTypes(this);
                boolean isUnorderedCollection = nodeTypes != null && nodeTypes.isUnorderedCollection(primaryType, mixinTypes);
               
                CachedNode persisted = null;
                Path newPath = null;
                NodeKey newParent = node.newParent();
                EditableDocument doc = null;
                ChangedAdditionalParents additionalParents = node.additionalParents();

                if (node.isNew()) {
                    doc = Schematic.newDocument();
                    translator.setKey(doc, key);
                    translator.setParents(doc, newParent, null, additionalParents);
                    translator.addInternalProperties(doc, node.getAddedInternalProperties());
                    
                    SessionNode mutableParent = mutable(newParent);
                    Set<Name> parentMixinTypes = mutableParent.getMixinTypes(this);
                    boolean parentAllowsSNS = nodeTypes != null && nodeTypes.allowsNameSiblings(primaryType, parentMixinTypes);
                    if (!parentAllowsSNS) {
                        // if the parent of the *new* node doesn't allow SNS, we can optimize the path lookup by
                        // looking directly at the parent's appended nodes because:
                        // a) "this" is a new node
                        // b) the parent must be already in this cache 
                        // c) no SNS are allowed meaning that we don't care about the already existent child references  
                        // and determining the path in a regular fashion *can* very expensive because it requires loading all of the parent's child references
                        MutableChildReferences appended = mutableParent.appended(false);
                        assert appended != null;
                        Path parentPath = sessionPaths.getPath(mutableParent);
                        newPath = pathFactory().create(parentPath, appended.getChild(key).getName());
                        sessionPaths.put(key, newPath);
                    } else {
                        // do a regular lookup of the path, which *will load* the parent's child references
                        newPath = sessionPaths.getPath(node);
                    }                    
                    // Create an event ...
                    changes.nodeCreated(key, newParent, newPath, primaryType, mixinTypes, node.changedProperties());
                } else {
                    doc = documentStore.edit(keyStr, true, acquireLock);
                    if (doc == null) {
                        if (isExternal && renamedExternalNodes.contains(key)) {
                            // this is a renamed external node which has been processed in the parent, so we can skip it
                            continue;
                        }
                        // Could not find the entry in the documentStore, which means it was deleted by someone else
                        // just moments before we got our transaction to save ...
                        throw new DocumentNotFoundException(keyStr);
                    }
                    // process any internal properties first
                    translator.addInternalProperties(doc, node.getAddedInternalProperties());
                    translator.removeInteralProperties(doc, node.getRemovedInternalProperties());
                    
                    // only after we're certain the document exists in the store can we safely compute this path
                    newPath = sessionPaths.getPath(node);
                    if (newParent != null) {
                        persisted = persistedCache.getNode(key);
                        // The node has moved (either within the same parent or to another parent) ...
                        Path oldPath = workspacePaths.getPath(persisted);
                        NodeKey oldParentKey = persisted.getParentKey(persistedCache);
                        if (!oldParentKey.equals(newParent) || (additionalParents != null && !additionalParents.isEmpty())) {
                            translator.setParents(doc, node.newParent(), oldParentKey, additionalParents);
                        }
                        // We only want to fire the event if the node we're working with is in the same workspace as the current
                        // workspace. The node will be in a different workspace when it is linked or un-linked
                        // (e.g. shareable node or jcr:system).
                        String workspaceKey = node.getKey().getWorkspaceKey();
                        boolean isSameWorkspace = persistedCache.getWorkspaceKey().equalsIgnoreCase(workspaceKey);
                        if (isSameWorkspace) {
                            changes.nodeMoved(key, primaryType, mixinTypes, newParent, oldParentKey, newPath, oldPath);
                        }
                    } else if (additionalParents != null) {
                        // The node in another workspace has been linked to this workspace ...
                        translator.setParents(doc, null, null, additionalParents);
                    }

                    // Deal with mixin changes here (since for new nodes they're put into the properties) ...
                    MixinChanges mixinChanges = node.mixinChanges(false);
                    if (mixinChanges != null && !mixinChanges.isEmpty()) {
                        Property oldProperty = translator.getProperty(doc, JcrLexicon.MIXIN_TYPES);
                        translator.addPropertyValues(doc, JcrLexicon.MIXIN_TYPES, true, mixinChanges.getAdded(),
                                                     unusedBinaryKeys, usedBinaryKeys);
                        translator.removePropertyValues(doc, JcrLexicon.MIXIN_TYPES, mixinChanges.getRemoved(), unusedBinaryKeys,
                                                        usedBinaryKeys);
                        // the property was changed ...
                        Property newProperty = translator.getProperty(doc, JcrLexicon.MIXIN_TYPES);
                        if (oldProperty == null) {
                            changes.propertyAdded(key, primaryType, mixinTypes, newPath, newProperty);
                        } else if (newProperty == null) {
                            changes.propertyRemoved(key, primaryType, mixinTypes, newPath, oldProperty);
                        } else {
                            changes.propertyChanged(key, primaryType, mixinTypes, newPath, newProperty, oldProperty);
                        }
                    }
                }

                LockChange lockChange = node.getLockChange();
                if (lockChange != null) {
                    switch (lockChange) {
                        case LOCK_FOR_SESSION:
                        case LOCK_FOR_NON_SESSION:
                            // check is another session has already locked the document
                            if (translator.isLocked(doc)) {
                                throw new LockFailureException(key);
                            }
                            break;
                        case UNLOCK:
                            break;
                    }
                }

                // As we go through the removed and changed properties, we want to keep track of whether there are any
                // effective modifications to the persisted properties.
                boolean hasPropertyChanges = false;

                // Save the removed properties ...
                Set<Name> removedProperties = node.removedProperties();
                if (!removedProperties.isEmpty()) {
                    assert !node.isNew();
                    if (persisted == null) {
                        persisted = persistedCache.getNode(key);
                    }
                    for (Name name : removedProperties) {
                        Property oldProperty = translator.removeProperty(doc, name, unusedBinaryKeys, usedBinaryKeys);
                        if (oldProperty != null) {
                            // the property was removed ...
                            changes.propertyRemoved(key, primaryType, mixinTypes, newPath, oldProperty);
                            // and we know that there are modifications to the properties ...
                            hasPropertyChanges = true;
                        }
                    }
                }

                // Save the changes to the properties
                if (!node.changedProperties().isEmpty()) {
                    if (!node.isNew() && persisted == null) {
                        persisted = persistedCache.getNode(key);
                    }
                    for (Map.Entry<Name, Property> propEntry : node.changedProperties().entrySet()) {
                        Name name = propEntry.getKey();
                        Property prop = propEntry.getValue();
                        // Get the old property ...
                        Property oldProperty = persisted != null ? persisted.getProperty(name, persistedCache) : null;
                        translator.setProperty(doc, prop, unusedBinaryKeys, usedBinaryKeys);
                        if (oldProperty == null) {
                            // the property was created ...
                            changes.propertyAdded(key, primaryType, mixinTypes, newPath, prop);
                            // and we know that there are modifications to the properties ...
                            hasPropertyChanges = true;
                        } else if (hasPropertyChanges || !oldProperty.equals(prop)) {
                            // The 'hasPropertyChanges ||' in the above condition is what gives us the "slight optimization"
                            // mentioned in the longer comment above. This is noticeably more efficient (since the
                            // '!oldProperty.equals(prop)' has to be called for only some of the changes) and does result
                            // in correct indexing behavior, but the compromise is that some no-op property changes will
                            // result in a PROPERTY_CHANGE event. To remove all potential no-op PROPERTY CHANGE events,
                            // simply remove the 'hasPropertyChanges||' in the above condition.
                            // See MODE-1856 for details.

                            // the property was changed and is actually different than the persisted property ...
                            changes.propertyChanged(key, primaryType, mixinTypes, newPath, prop, oldProperty);
                            hasPropertyChanges = true;
                        }
                    }
                }

                // Save the change to the child references. Note that we only need to generate events for renames;
                // moves (to the same or another parent), removes, and inserts are all recorded as changes in the
                // child node, and events are generated handled when we process
                // the child node.
                ChangedChildren changedChildren = node.changedChildren();
                MutableChildReferences appended = node.appended(false);
                if ((changedChildren == null || changedChildren.isEmpty()) && (appended != null && !appended.isEmpty())) {
                    // Just appended children ...  
                    if (!isUnorderedCollection) {
                        translator.changeChildren(doc, changedChildren, appended);
                    } else {
                        translator.addChildrenToBuckets(doc, appended);
                    }
                } else if (changedChildren != null && !changedChildren.isEmpty()) {
                    if (!changedChildren.getRemovals().isEmpty() && !isUnorderedCollection) {
                        // This node is not being removed (or added), but it has removals, and we have to calculate the paths
                        // of the removed nodes before we actually change the child references of this node.
                        for (NodeKey removed : changedChildren.getRemovals()) {
                            CachedNode persistent = persistedCache.getNode(removed);
                            if (persistent != null) {
                                if (appended != null && appended.hasChild(persistent.getKey())) {
                                    // the same node has been both removed and appended => reordered at the end
                                    ChildReference appendedChildRef = node.getChildReferences(this).getChild(persistent.getKey());
                                    Path newNodePath = pathFactory().create(newPath, appendedChildRef.getSegment());
                                    Path oldNodePath = workspacePaths.getPath(persistent);
                                    changes.nodeReordered(persistent.getKey(), primaryType, mixinTypes, node.getKey(), newNodePath,
                                                          oldNodePath, null);
                                }
                            }

                        }
                    }

                    // Now change the children ...
                    if (!isUnorderedCollection) {
                        // this is a regular node
                        translator.changeChildren(doc, changedChildren, appended);
                    } else {
                        // there are both added & removed children for this collection
                        if (appended != null && !appended.isEmpty()) {
                            // process additions first
                            translator.addChildrenToBuckets(doc, appended);
                        }
                        if (changedChildren.removalCount() > 0){
                            // when removing nodes from unordered collections, a 2 step process is required
                            // 1. collect all the nodes which will be removed from each bucket, without persisting the actual bucket
                            // changes in ISPN. This is because if bucket changes are persisted here, we may have to process a deleted
                            // child later on while not having any more information
                            // 2. after all the changed nodes have been processed, make the actual changes to the bucket documents
                            // see below
                            Map<BucketId, Set<NodeKey>> removalsPerBucket = translator.preRemoveChildrenFromBuckets(workspaceCache(),
                                                                                                                    doc,
                                                                                                                    changedChildren.getRemovals());
                            if (!removalsPerBucket.isEmpty()) {
                                if (unorderedCollectionBucketRemovals == null) {
                                    unorderedCollectionBucketRemovals = new LinkedHashMap<>();
                                }
                                unorderedCollectionBucketRemovals.put(key, removalsPerBucket);
                            }
                        }
                    }

                    // Generate events for renames, as this is only captured in the parent node ...
                    Map<NodeKey, Name> newNames = changedChildren.getNewNames();
                    if (!newNames.isEmpty()) {
                        for (Map.Entry<NodeKey, Name> renameEntry : newNames.entrySet()) {
                            NodeKey renamedKey = renameEntry.getKey();
                            CachedNode oldRenamedNode = persistedCache.getNode(renamedKey);
                            if (oldRenamedNode == null) {
                                // The node was created in this session, so we can ignore this ...
                                continue;
                            }
                            Path renamedFromPath = workspacePaths.getPath(oldRenamedNode);
                            Path renamedToPath = pathFactory().create(renamedFromPath.getParent(), renameEntry.getValue());
                            changes.nodeRenamed(renamedKey, renamedToPath, renamedFromPath.getLastSegment(), primaryType,
                                                mixinTypes);
                            if (isExternal) {
                                renamedExternalNodes.add(renamedKey);
                            }
                        }
                    }

                    // generate reordering events for nodes which have not been reordered to the end
                    Map<NodeKey, SessionNode.Insertions> insertionsByBeforeKey = changedChildren.getInsertionsByBeforeKey();
                    for (SessionNode.Insertions insertion : insertionsByBeforeKey.values()) {
                        for (ChildReference insertedRef : insertion.inserted()) {
                            CachedNode insertedNodePersistent = persistedCache.getNode(insertedRef);
                            CachedNode insertedNode = getNode(insertedRef.getKey());
                            Path nodeNewPath = sessionPaths.getPath(insertedNode);
                            if (insertedNodePersistent != null) {
                                Path nodeOldPath = workspacePaths.getPath(insertedNodePersistent);
                                Path insertedBeforePath = null;
                                CachedNode insertedBeforeNode = persistedCache.getNode(insertion.insertedBefore());
                                if (insertedBeforeNode != null) {
                                    insertedBeforePath = workspacePaths.getPath(insertedBeforeNode);
                                    boolean isSnsReordering = nodeOldPath.getLastSegment().getName()
                                                                         .equals(insertedBeforePath.getLastSegment().getName());
                                    if (isSnsReordering) {
                                        nodeNewPath = insertedBeforePath;
                                    }
                                }
                                changes.nodeReordered(insertedRef.getKey(), insertedNode.getPrimaryType(this),
                                                      insertedNode.getMixinTypes(this), node.getKey(), nodeNewPath, nodeOldPath,
                                                      insertedBeforePath);

                            } else {
                                // if the node is new and reordered at the same time (most likely due to either a version restore
                                // or explicit reordering of transient nodes) there is no "old path"
                                CachedNode insertedBeforeNode = getNode(insertion.insertedBefore().getKey());
                                Path insertedBeforePath = sessionPaths.getPath(insertedBeforeNode);
                                changes.nodeReordered(insertedRef.getKey(), insertedNode.getPrimaryType(this),
                                                      insertedNode.getMixinTypes(this), node.getKey(), nodeNewPath, null,
                                                      insertedBeforePath);
                            }
                        }
                    }
                }

                ReferrerChanges referrerChanges = node.getReferrerChanges();
                boolean nodeChanged = false;
                if (referrerChanges != null && !referrerChanges.isEmpty()) {
                    translator.changeReferrers(doc, referrerChanges);
                    changes.nodeChanged(key, newPath, primaryType, mixinTypes);
                    nodeChanged = true;
                }

                // write the federated segments
                for (Map.Entry<String, String> federatedSegment : node.getAddedFederatedSegments().entrySet()) {
                    String externalNodeKey = federatedSegment.getKey();
                    String childName = federatedSegment.getValue();
                    translator.addFederatedSegment(doc, externalNodeKey, childName);
                    if (!nodeChanged) {
                        changes.nodeChanged(key, newPath, primaryType, mixinTypes);
                        nodeChanged = true;
                    }
                }
                Set<String> removedFederatedSegments = node.getRemovedFederatedSegments();
                if (!removedFederatedSegments.isEmpty()) {
                    translator.removeFederatedSegments(doc, node.getRemovedFederatedSegments());
                    if (!nodeChanged) {
                        changes.nodeChanged(key, newPath, primaryType, mixinTypes);
                        nodeChanged = true;
                    }
                }

                // write additional node "metadata", meaning various flags which have internal meaning
                boolean excludedFromSearch = node.isExcludedFromSearch(this);
                if (excludedFromSearch && translator.isQueryable(doc)) {
                    translator.setQueryable(doc, false);
                }
                if (!excludedFromSearch && !translator.isQueryable(doc)) {
                    translator.setQueryable(doc, true);
                }

                if (node.isNew()) {
                    // We need to create the schematic entry for the new node ...
                    if (documentStore.storeDocument(keyStr, doc) != null) {
                        if (replacedNodes != null && replacedNodes.contains(key)) {
                            // Then a node is being removed and recreated with the same key ...
                            documentStore.localStore().put(keyStr, doc);
                        } else if (removedNodes != null && removedNodes.contains(key)) {
                            // Then a node is being removed and recreated with the same key ...
                            documentStore.localStore().put(keyStr, doc);
                            removedNodes.remove(key);
                        } else {
                            // We couldn't create the entry because one already existed ...
                            throw new DocumentAlreadyExistsException(keyStr);
                        }
                    }
                } else {
                    boolean externalNodeChanged = isExternal
                                                  && (hasPropertyChanges || node.hasNonPropertyChanges() || node.changedChildren()
                                                                                                                .renameCount() > 0);

                    // writable connectors *may* change their data in-place, so the update operation needs to be called only
                    // after the index changes have finished.
                    if (externalNodeChanged) {
                        // in the case of external nodes, only if there are changes should the update be called
                        documentStore.updateDocument(keyStr, doc, node);
                    }
                }

                // The above code doesn't properly generate events for newly linked or unlinked nodes (e.g., shareable nodes
                // in JCR), because NODE_ADDED or NODE_REMOVED events are generated based upon the creation or removal of the
                // child nodes, whereas linking and unlinking nodes don't result in creation/removal of nodes. Instead,
                // the linked/unlinked node is modified with the addition/removal of additional parents.
                //
                // NOTE that this happens somewhat rarely (as linked/shared nodes are used far less frequently) ...
                //
                if (additionalParents != null) {
                    // Generate NODE_ADDED events for each of the newly-added parents ...
                    for (NodeKey parentKey : additionalParents.getAdditions()) {
                        // Find the mutable parent node (if it exists) ...
                        SessionNode parent = this.changedNodes.get(parentKey);
                        if (parent != null) {
                            // Then the parent was changed in this session, so find the one-and-only child reference ...
                            ChildReference ref = parent.getChildReferences(this).getChild(key);
                            Path parentPath = sessionPaths.getPath(parent);
                            Path childPath = pathFactory().create(parentPath, ref.getSegment());
                            changes.nodeCreated(key, parentKey, childPath, primaryType, mixinTypes, null);
                        }
                    }
                    // Generate NODE_REMOVED events for each of the newly-removed parents ...
                    for (NodeKey parentKey : additionalParents.getRemovals()) {
                        // We need to read some information from the parent node before it was changed ...
                        CachedNode persistedParent = persistedCache.getNode(parentKey);
                        if (persistedParent != null) {
                            // Find the path to the removed child ...
                            ChildReference ref = persistedParent.getChildReferences(this).getChild(key);
                            if (ref != null) {
                                Path parentPath = workspacePaths.getPath(persistedParent);
                                Path childPath = pathFactory().create(parentPath, ref.getSegment());
                                Name parentPrimaryType = persistedParent.getPrimaryType(persistedCache);
                                Set<Name> parentMixinTypes = persistedParent.getMixinTypes(persistedCache);
                                changes.nodeRemoved(key, parentKey, childPath, primaryType, mixinTypes,
                                                    parentPrimaryType, parentMixinTypes);
                            }
                        }
                    }
                }
            }
        }
        
        // persist any bucket document changes resulted from removing children from unordered collections
        if (unorderedCollectionBucketRemovals != null) {
            for (Map.Entry<NodeKey, Map<BucketId, Set<NodeKey>>> entry : unorderedCollectionBucketRemovals.entrySet()) {
                translator.persistBucketRemovalChanges(entry.getKey(), entry.getValue());                
            }
        }

        // Remove all the buckets from all the unordered collections which have been removed...
        // This will only remove the buckets, not the original nodes which are processed below
        if (removedUnorderedCollections != null) {
            for (NodeKey removedUnorderedCollectionKey : removedUnorderedCollections) {
                translator.removeAllBucketsFromUnorderedCollection(removedUnorderedCollectionKey);
            }
        }

        if (removedNodes != null) {
            assert !removedNodes.isEmpty();
            // we need to collect the referrers at the end only, so that other potential changes in references have been computed
            Set<NodeKey> referrers = new HashSet<NodeKey>();
            for (NodeKey removedKey : removedNodes) {
                // we need the current document from the documentStore, because this differs from what's persisted
                SchematicEntry entry = documentStore.get(removedKey.toString());
                if (entry != null) {
                    // The entry hasn't yet been removed by another (concurrent) session ...
                    Document doc = documentStore.get(removedKey.toString()).getContent();
                    referrers.addAll(translator.getReferrers(doc, ReferenceType.STRONG));
                }
            }
            // check referential integrity ...
            referrers.removeAll(removedNodes);

            if (!referrers.isEmpty()) {
                throw new ReferentialIntegrityException(removedNodes, referrers);
            }

            // Now remove all of the nodes from the documentStore.
            // Note 2: we do this last because the children are removed from their parent before the removal is handled above
            // (see Node 1), meaning getting the path and other information for removed nodes never would work properly.
            for (NodeKey removedKey : removedNodes) {
                documentStore.remove(removedKey.toString());
            }
        }

        if (!unusedBinaryKeys.isEmpty()) {
            // There are some binary values that are no longer referenced ...
            for (BinaryKey key : unusedBinaryKeys) {
                changes.binaryValueNoLongerUsed(key);
            }
        }

        if (!usedBinaryKeys.isEmpty()) {
            // There are some binary values which need to be marked as used ...
            for (BinaryKey key : usedBinaryKeys) {
                changes.binaryValueUsed(key);
            }
        }

        changes.setChangedNodes(changedNodes.keySet()); // don't need to make a copy
        changes.freeze(userId, userData, timestamp);
        return changes;
    }

    private WorkspaceCache lockNodes( Collection<NodeKey> changedNodesInOrder ) {
        if (changedNodesInOrder.isEmpty()) {
            return workspaceCache();
        }
        DocumentStore documentStore = workspaceCache().documentStore();

        if (LOGGER.isDebugEnabled()) {
            if (!this.changedNodes.isEmpty()) {
                LOGGER.debug("Attempting to lock nodes in Infinispan: {0}", changedNodes.keySet());
            }
        }
        // Try to acquire from the DocumentStore locks for all the nodes that we're going to change ...
        Set<String> keysToLock = new TreeSet<String>();

        for (NodeKey key : changedNodesInOrder) {
            SessionNode node = changedNodes.get(key);
            if (!node.isNew()) {
                String keyStr = key.toString();
                keysToLock.add(keyStr);
            }
            // collect all binary reference document keys since they have to be locked as well
            Set<BinaryKey> binaryReferencesForNode = binaryReferencesByNodeKey.get(key);
            if (binaryReferencesForNode != null) {
                for (BinaryKey binaryKey : binaryReferencesForNode) {
                    keysToLock.add(translator().keyForBinaryReferenceDocument(binaryKey.toString()));
                }
            }
        }
        
        if (!keysToLock.isEmpty()) {
            int retryCountOnLockTimeout = 2;
            boolean locksAquired = false;
            while (!locksAquired && retryCountOnLockTimeout > 0) {
                locksAquired = documentStore.lockDocuments(keysToLock);
                if (locksAquired) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Locked the nodes: {0}", keysToLock);
                    }
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Timeout while attempting to lock keys {0} in Infinispan. Retrying....", keysToLock);
                    }
                    --retryCountOnLockTimeout;
                }
            }
            if (!locksAquired) {
                throw new org.infinispan.util.concurrent.TimeoutException("Timeout while attempting to lock the keys " + keysToLock + " after " + retryCountOnLockTimeout + " retry attempts.");
            }
        }
        //return a transient workspace cache, which contains the latest view of the nodes which will be changed
        return workspaceCache().persistedCache(changedNodesInOrder);
    }
   
    private Transactions.TransactionFunction binaryUsageUpdateFunction( final Set<BinaryKey> usedBinaries,
                                                                        final Set<BinaryKey> unusedBinaries ) {
        final BinaryStore binaryStore = getContext().getBinaryStore();
        return new Transactions.TransactionFunction() {
            @Override
            public void execute() {
                if (!usedBinaries.isEmpty()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Marking binary values as used: {0}", usedBinaries);
                    }
                    try {
                        binaryStore.markAsUsed(usedBinaries);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Finished marking binary values as used: {0}", usedBinaries);
                        }
                    } catch (BinaryStoreException e) {
                        LOGGER.error(e, JcrI18n.errorMarkingBinaryValuesUsed, e.getMessage());
                    }
                }

                if (!unusedBinaries.isEmpty()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Marking binary values as unused: {0}", unusedBinaries);
                    }
                    try {
                        binaryStore.markAsUnused(unusedBinaries);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Finished marking binary values as unused: {0}", unusedBinaries);
                        }
                    } catch (BinaryStoreException e) {
                        LOGGER.error(e, JcrI18n.errorMarkingBinaryValuesUnused, e.getMessage());
                    }
                }
            }
        };
    }

    protected SessionNode add( SessionNode newNode ) {
        assert newNode != REMOVED;
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            NodeKey key = newNode.getKey();
            SessionNode node = changedNodes.put(key, newNode);
            if (node != null) {
                if (node != REMOVED) {
                    // Put the original node back ...
                    changedNodes.put(key, node);
                    return node;
                }
                // Otherwise, a node with the same key was removed by this session before creating a new
                // node with the same ID ...
                if (replacedNodes == null) {
                    replacedNodes = new HashSet<NodeKey>();
                }
                replacedNodes.add(key);
            }
            changedNodesInOrder.add(key);
            return newNode;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings( "finally" )
    @Override
    public void destroy( NodeKey key ) {
        assert key != null;
        final WorkspaceCache workspace = workspaceCache();
        CachedNode topNode = getNode(key);
        if (topNode == null) {
            throw new NodeNotFoundException(key);
        }

        Map<NodeKey, SessionNode> removed = new HashMap<NodeKey, SessionNode>();
        LinkedHashSet<NodeKey> addToChangedNodes = new LinkedHashSet<NodeKey>();

        // Now destroy this node and all descendants ...
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            Queue<NodeKey> keys = new LinkedList<NodeKey>();
            keys.add(key);
            while (!keys.isEmpty()) {
                NodeKey nodeKey = keys.remove();

                // Find the node in the session and/or workspace ...
                SessionNode node = this.changedNodes.put(nodeKey, REMOVED);
                boolean cleanupReferences = false;
                ChildReferences children = null;
                if (node != null) {
                    if (node == REMOVED) {
                        continue;
                    }
                    // There was a node within this cache ...
                    children = node.getChildReferences(this);
                    removed.put(nodeKey, node);
                    // we need to preserve any existing transient referrer changes for the node which we're removing, as they can
                    // influence ref integrity
                    referrerChangesForRemovedNodes.put(nodeKey, node.getReferrerChanges());
                    cleanupReferences = true;
                    
                    // we need to go through all the node's binary properties and mark them as unused by this node
                    for (Iterator<Property> it = node.getProperties(this); it.hasNext();) {
                        Property property = it.next();
                        if (property != null) {
                            if (property.isBinary() && !node.isPropertyNew(this, property.getName())) {
                                // We need to register the binary value as not being in use anymore
                                for (Object binaryObject : property.getValuesAsArray()) {
                                    assert binaryObject instanceof Binary;
                                    if (binaryObject instanceof AbstractBinary) {
                                        BinaryKey binaryKey = ((AbstractBinary)binaryObject).getKey();
                                        addBinaryReference(nodeKey, binaryKey);
                                    }
                                }                                                                       
                            }
                        }
                    }
                } else {
                    // The node did not exist in the session, so get it from the workspace ...
                    addToChangedNodes.add(nodeKey);
                    CachedNode persisted = workspace.getNode(nodeKey);
                    if (persisted == null) {
                        continue;
                    }
                    children = persisted.getChildReferences(workspace);
                    // Look for outgoing references that need to be cleaned up ...
                    for (Iterator<Property> it = persisted.getProperties(workspace); it.hasNext();) {
                        Property property = it.next();
                        if (property != null) {
                            if (property.isReference()) {
                                // We need to get the node in the session's cache ...
                                this.changedNodes.remove(nodeKey); // we put REMOVED a dozen lines up ...
                                node = this.mutable(nodeKey);
                                if (node != null) {
                                    cleanupReferences = true;
                                }
                                this.changedNodes.put(nodeKey, REMOVED);
                            }

                            if (property.isBinary()) {
                                // We need to register the binary value as not being in use anymore
                                for (Object binaryObject : property.getValuesAsArray()) {
                                    assert binaryObject instanceof Binary;
                                    if (binaryObject instanceof AbstractBinary) {
                                        BinaryKey binaryKey = ((AbstractBinary)binaryObject).getKey();
                                        addBinaryReference(nodeKey, binaryKey);
                                    }
                                }
                            }
                        }
                    }
                }
                if (cleanupReferences) {
                    assert node != null;
                    // cleanup (remove) all outgoing references from this node to other nodes
                    node.removeAllReferences(this);
                }

                // Now find all of the children ...
                assert children != null;
                for (ChildReference child : children) {
                    NodeKey childKey = child.getKey();
                    // only recursively delete children from the same source (prevents deletion of external nodes in case of
                    // federation)
                    if (childKey.getSourceKey().equalsIgnoreCase(key.getSourceKey())) {
                        keys.add(childKey);
                    }
                }
            }

            // Now update the 'changedNodesInOrder' set ...
            this.changedNodesInOrder.addAll(addToChangedNodes);
        } catch (RuntimeException e) {
            // Need to roll back the changes we've made ...
            try {
                // Put the changed nodes back into the map ...
                this.changedNodes.putAll(removed);
            } catch (RuntimeException e2) {
                I18n msg = JcrI18n.failedWhileRollingBackDestroyToRuntimeError;
                LOGGER.error(e2, msg, e2.getMessage(), e.getMessage());
            } finally {
                // Re-throw original exception ...
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isDestroyed( NodeKey key ) {
        return changedNodes.get(key) == REMOVED;
    }

    @Override
    protected void addBinaryReference( NodeKey nodeKey, BinaryKey... binaryKeys ) {
        if (binaryKeys.length == 0) {
            return;
        }
        Set<BinaryKey> binaryReferencesForNode = this.binaryReferencesByNodeKey.get(nodeKey);
        if (binaryReferencesForNode == null) {
            Set<BinaryKey> emptySet = Collections.newSetFromMap(new ConcurrentHashMap<BinaryKey, Boolean>());
            binaryReferencesForNode = this.binaryReferencesByNodeKey.putIfAbsent(nodeKey, emptySet);
            if (binaryReferencesForNode == null) {
                binaryReferencesForNode = emptySet;
            }
        }
        binaryReferencesForNode.addAll(Arrays.asList(binaryKeys));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        NamespaceRegistry reg = context().getNamespaceRegistry();
        sb.append("Session ").append(context().getId()).append(" to workspace '").append(workspaceName());
        for (NodeKey key : changedNodesInOrder) {
            SessionNode changes = changedNodes.get(key);
            if (changes == null) {
                continue;
            }
            sb.append("\n ");
            sb.append(changes.getString(reg));
        }
        return sb.toString();
    }
}

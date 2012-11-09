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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment;
import org.modeshape.jcr.cache.SessionEnvironment.Monitor;
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
import org.modeshape.jcr.txn.Transactions.TransactionFunction;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * A writable {@link SessionCache} implementation capable of making transient changes and saving them.
 */
@ThreadSafe
public class WritableSessionCache extends AbstractSessionCache {

    private static final NodeKey REMOVED_KEY = new NodeKey("REMOVED_NODE_SHOULD_NEVER_BE_PERSISTED");
    private static final SessionNode REMOVED = new SessionNode(REMOVED_KEY, false);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<NodeKey, SessionNode> changedNodes;
    private Set<NodeKey> replacedNodes;
    private LinkedHashSet<NodeKey> changedNodesInOrder;
    private Map<NodeKey, ReferrerChanges> referrerChangesForRemovedNodes;
    private final Transactions txns;

    /**
     * Create a new SessionCache that can be used for making changes to the workspace.
     * 
     * @param context the execution context; may not be null
     * @param workspaceCache the (shared) workspace cache; may not be null
     * @param sessionContext the context for the session; may not be null
     */
    public WritableSessionCache( ExecutionContext context,
                                 WorkspaceCache workspaceCache,
                                 SessionEnvironment sessionContext ) {
        super(context, workspaceCache, sessionContext);
        this.changedNodes = new HashMap<NodeKey, SessionNode>();
        this.changedNodesInOrder = new LinkedHashSet<NodeKey>();
        this.referrerChangesForRemovedNodes = new HashMap<NodeKey, ReferrerChanges>();
        this.txns = sessionContext.getTransactions();
    }

    protected final void assertInSession( SessionNode node ) {
        assert this.changedNodes.get(node.getKey()) == node : "Node " + node.getKey() + " is not in this session";
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
    public void clear() {
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            changedNodes.clear();
            changedNodesInOrder.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear( CachedNode node ) {
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
            if (!changedNode.isAtOrBelow(this, nodePath)) {
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

            try {
                // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                txn = txns.begin();

                // Get a monitor via the transaction ...
                final Monitor monitor = txn.createMonitor();
                try {
                    // Now persist the changes ...
                    events = persistChanges(this.changedNodesInOrder, monitor);
                    clearModifiedState();
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    txn.rollback();
                    throw e;
                }

                // Register a handler that will execute upon successful commit of the transaction (whever that happens) ...
                final ChangeSet changes = events;
                txn.uponCompletion(new TransactionFunction() {
                    @Override
                    public void transactionComplete() {
                        if (changes != null && monitor != null) {
                            monitor.recordChanged(changes.changedNodes().size());
                        }
                        clearState();
                    }
                });

                // Commit the transaction ...
                txn.commit();

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

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WrappedException(e);
        } finally {
            lock.unlock();
        }

        txns.updateCache(workspaceCache, events, txn);
    }

    protected void clearState() {
        // The changes have been made, so create a new map (we're using the keys from the current map) ...
        this.changedNodes = new HashMap<NodeKey, SessionNode>();
        this.referrerChangesForRemovedNodes.clear();
        this.changedNodesInOrder.clear();
        this.replacedNodes = null;
    }

    protected void clearState( Iterable<NodeKey> savedNodesInOrder ) {
        // The changes have been made, so remove the changes from this session's map ...
        for (NodeKey savedNode : savedNodesInOrder) {
            this.changedNodes.remove(savedNode);
            this.changedNodesInOrder.remove(savedNode);
            if (this.replacedNodes != null) {
                this.replacedNodes.remove(savedNode);
            }
        }
    }

    protected void clearModifiedState() {
        this.changedNodesInOrder.clear();
    }

    protected void clearModifiedState( Iterable<NodeKey> savedNodesInOrder ) {
        // The changes have been made, so remove the changes from this session's map ..
        for (NodeKey savedNode : savedNodesInOrder) {
            this.changedNodesInOrder.remove(savedNode);
        }
    }

    @Override
    public void save( SessionCache other,
                      PreSave preSaveOperation ) {

        // Try getting locks on both sessions ...
        final WritableSessionCache that = (WritableSessionCache)other;
        Lock thisLock = this.lock.writeLock();
        Lock thatLock = that.lock.writeLock();

        ChangeSet events1 = null;
        ChangeSet events2 = null;
        Transaction txn = null;
        try {
            thisLock.lock();
            thatLock.lock();

            try {
                // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
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

                // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                txn = txns.begin();

                // Get a monitor via the transaction ...
                final Monitor monitor = txn.createMonitor();
                try {
                    // Now persist the changes ...
                    events1 = persistChanges(this.changedNodesInOrder, monitor);
                    events2 = that.persistChanges(that.changedNodesInOrder, monitor);
                    this.clearModifiedState();
                    that.clearModifiedState();
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    txn.rollback();
                    throw e;
                }

                // Register a handler that will execute upon successful commit of the transaction (whever that happens) ...
                final ChangeSet changes1 = events1;
                final ChangeSet changes2 = events2;
                txn.uponCompletion(new TransactionFunction() {
                    @Override
                    public void transactionComplete() {
                        if (monitor != null) {
                            if (changes1 != null) {
                                monitor.recordChanged(changes1.changedNodes().size());
                            }
                            if (changes2 != null) {
                                monitor.recordChanged(changes2.changedNodes().size());
                            }
                        }
                        clearState();
                        that.clearState();
                    }
                });

                // Commit the transaction ...
                txn.commit();

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
        txns.updateCache(this.workspaceCache, events1, txn);
        txns.updateCache(that.workspaceCache, events2, txn);
    }

    private void checkNodeNotRemovedByAnotherTransaction( MutableCachedNode node ) {
        String keyString = node.getKey().toString();
        // if the node is not new and also missing from the document, another transaction has deleted it
        if (!node.isNew() && !workspaceCache.documentStore().containsKey(keyString)) {
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
        final WritableSessionCache that = (WritableSessionCache)other;
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

            try {
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

                // Start a ModeShape transaction (which may be a part of a larger JTA transaction) ...
                txn = txns.begin();

                // Get a monitor via the transaction ...
                final Monitor monitor = txn.createMonitor();

                try {
                    // Now persist the changes ...
                    events1 = persistChanges(savedNodesInOrder, monitor);
                    events2 = that.persistChanges(that.changedNodesInOrder, monitor);
                    clearModifiedState(savedNodesInOrder);
                    clearModifiedState();
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    txn.rollback();
                    throw e;
                }

                // Register a handler that will execute upon successful commit of the transaction (whever that happens) ...
                final ChangeSet changes1 = events1;
                final ChangeSet changes2 = events2;
                txn.uponCompletion(new TransactionFunction() {
                    @Override
                    public void transactionComplete() {
                        if (monitor != null) {
                            if (changes1 != null) {
                                monitor.recordChanged(changes1.changedNodes().size());
                            }
                            if (changes2 != null) {
                                monitor.recordChanged(changes2.changedNodes().size());
                            }
                        }
                        clearState(savedNodesInOrder);
                        that.clearState();
                    }
                });

                // Commit the transaction ...
                txn.commit();

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
        txns.updateCache(this.workspaceCache, events1, txn);
        txns.updateCache(that.workspaceCache, events2, txn);
    }

    /**
     * Persist the changes within an already-established transaction.
     * 
     * @param changedNodesInOrder the nodes that are to be persisted; may not be null
     * @param monitor the monitor for these changes; may be null if not needed
     * @return the ChangeSet encapsulating the changes that were made
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     */
    @GuardedBy( "lock" )
    protected ChangeSet persistChanges( Iterable<NodeKey> changedNodesInOrder,
                                        Monitor monitor ) {
        // Compute the save meta-info ...
        ExecutionContext context = context();
        String userId = context.getSecurityContext().getUserName();
        Map<String, String> userData = context.getData();
        DateTime timestamp = context.getValueFactories().getDateFactory().create();
        String workspaceName = workspaceCache().getWorkspaceName();
        String repositoryKey = workspaceCache().getRepositoryKey();
        String processKey = workspaceCache().getProcessKey();
        RecordingChanges changes = new RecordingChanges(processKey, repositoryKey, workspaceName);

        // Get the documentStore ...
        DocumentStore documentStore = workspaceCache.documentStore();
        DocumentTranslator translator = workspaceCache.translator();

        PathCache sessionPaths = new PathCache(this);
        PathCache workspacePaths = new PathCache(workspaceCache);

        Set<NodeKey> removedNodes = null;
        Set<BinaryKey> unusedBinaryKeys = new HashSet<BinaryKey>();
        for (NodeKey key : changedNodesInOrder) {
            SessionNode node = changedNodes.get(key);
            String keyStr = key.toString();
            if (node == REMOVED) {
                // We need to read some information from the node before we remove it ...
                CachedNode persisted = workspaceCache.getNode(key);
                if (persisted != null) {
                    // This was a persistent node, so we have to generate an event and deal with the remove ...
                    if (removedNodes == null) {
                        removedNodes = new HashSet<NodeKey>();
                    }
                    Path path = workspacePaths.getPath(persisted);
                    changes.nodeRemoved(key, persisted.getParentKey(workspaceCache), path);
                    removedNodes.add(key);

                    // if there were any referrer changes for the removed nodes, we need to process them
                    ReferrerChanges referrerChanges = referrerChangesForRemovedNodes.get(key);
                    if (referrerChanges != null) {
                        EditableDocument doc = documentStore.get(keyStr).editDocumentContent();
                        translator.changeReferrers(doc, referrerChanges);
                    }
                    // Note 1: Do not actually remove the document from the documentStore yet; see below (note 2)
                }
                // Otherwise, the removed node was created in the session (but not ever persisted),
                // so we don't have to do anything ...
            } else {
                CachedNode persisted = null;
                Path newPath = sessionPaths.getPath(node);
                NodeKey newParent = node.newParent();
                EditableDocument doc = null;
                ChangedAdditionalParents additionalParents = node.additionalParents();

                if (node.isNew()) {
                    doc = Schematic.newDocument();
                    translator.setKey(doc, key);
                    translator.setParents(doc, newParent, null, additionalParents);
                    // Create an event ...
                    changes.nodeCreated(key, newParent, newPath, node.changedProperties());

                    // And record the new node via the monitor ...
                    if (monitor != null) {
                        // Get the primary and mixin type names; even though we're passing in the session, the two properties
                        // should be there and shouldn't require a looking in the cache...
                        Name primaryType = node.getPrimaryType(this);
                        Set<Name> mixinTypes = node.getMixinTypes(this);
                        monitor.recordAdd(workspaceName, key, newPath, primaryType, mixinTypes, node.changedProperties().values());
                    }

                } else {
                    SchematicEntry nodeEntry = documentStore.get(keyStr);
                    if (nodeEntry == null) {
                        // Could not find the entry in the documentStore, which means it was deleted by someone else
                        // just moments before we got our transaction to save ...
                        throw new DocumentNotFoundException(keyStr);
                    }
                    doc = nodeEntry.editDocumentContent();
                    if (newParent != null) {
                        persisted = workspaceCache.getNode(key);
                        // The node has moved (either within the same parent or to another parent) ...
                        Path oldPath = workspacePaths.getPath(persisted);
                        NodeKey oldParentKey = persisted.getParentKey(workspaceCache);
                        if (!oldParentKey.equals(newParent) || (additionalParents != null && !additionalParents.isEmpty())) {
                            translator.setParents(doc, node.newParent(), oldParentKey, additionalParents);
                        }
                        // Generate a move even either way ...
                        changes.nodeMoved(key, newParent, oldParentKey, newPath, oldPath);
                    } else if (additionalParents != null) {
                        // The node in another workspace has been linked to this workspace ...
                        translator.setParents(doc, null, null, additionalParents);
                    }

                    // Deal with mixin changes here (since for new nodes they're put into the properties) ...
                    MixinChanges mixinChanges = node.mixinChanges(false);
                    if (mixinChanges != null && !mixinChanges.isEmpty()) {
                        Property oldProperty = translator.getProperty(doc, JcrLexicon.MIXIN_TYPES);
                        translator.addPropertyValues(doc, JcrLexicon.MIXIN_TYPES, true, mixinChanges.getAdded(), unusedBinaryKeys);
                        translator.removePropertyValues(doc, JcrLexicon.MIXIN_TYPES, mixinChanges.getRemoved(), unusedBinaryKeys);
                        // the property was changed ...
                        Property newProperty = translator.getProperty(doc, JcrLexicon.MIXIN_TYPES);
                        if (oldProperty == null) {
                            changes.propertyAdded(key, newPath, newProperty);
                        } else if (newProperty == null) {
                            changes.propertyRemoved(key, newPath, oldProperty);
                        } else {
                            changes.propertyChanged(key, newPath, newProperty, oldProperty);
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

                // Save the removed properties ...
                Set<Name> removedProperties = node.removedProperties();
                if (!removedProperties.isEmpty()) {
                    assert !node.isNew();
                    if (persisted == null) {
                        persisted = workspaceCache.getNode(key);
                    }
                    for (Name name : removedProperties) {
                        Property oldProperty = translator.removeProperty(doc, name, unusedBinaryKeys);
                        if (oldProperty != null) {
                            // the property was removed ...
                            changes.propertyRemoved(key, newPath, oldProperty);
                        }
                    }
                }

                // Save the changes to the properties
                if (!node.changedProperties().isEmpty()) {
                    if (!node.isNew() && persisted == null) {
                        persisted = workspaceCache.getNode(key);
                    }
                    for (Map.Entry<Name, Property> propEntry : node.changedProperties().entrySet()) {
                        Name name = propEntry.getKey();
                        Property prop = propEntry.getValue();
                        // Get the old property ...
                        Property oldProperty = persisted != null ? persisted.getProperty(name, workspaceCache) : null;
                        translator.setProperty(doc, prop, unusedBinaryKeys);
                        if (oldProperty == null) {
                            // the property was created ...
                            changes.propertyAdded(key, newPath, prop);
                        } else {
                            // the property was changed ...
                            changes.propertyChanged(key, newPath, prop, oldProperty);
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
                    translator.changeChildren(doc, changedChildren, appended);
                } else if (changedChildren != null && !changedChildren.isEmpty()) {
                    if (!changedChildren.getRemovals().isEmpty()) {
                        // This node is not being removed (or added), but it has removals, and we have to calculate the paths
                        // of the removed nodes before we actually change the child references of this node.
                        for (NodeKey removed : changedChildren.getRemovals()) {
                            CachedNode persistent = workspaceCache.getNode(removed);
                            if (persistent != null) {
                                Path oldPath = workspacePaths.getPath(persistent);
                                if (appended != null && appended.hasChild(persistent.getKey())) {
                                    // the same node has been both removed and appended => reordered at the end
                                    ChildReference appendedChildRef = node.getChildReferences(this).getChild(persistent.getKey());
                                    newPath = pathFactory().create(sessionPaths.getPath(node), appendedChildRef.getSegment());
                                    changes.nodeReordered(persistent.getKey(), node.getKey(), newPath, oldPath, null);
                                }
                            }

                        }
                    }

                    // Now change the children ...
                    translator.changeChildren(doc, changedChildren, appended);

                    // Generate events for renames, as this is only captured in the parent node ...
                    Map<NodeKey, Name> newNames = changedChildren.getNewNames();
                    if (!newNames.isEmpty()) {
                        for (Map.Entry<NodeKey, Name> renameEntry : newNames.entrySet()) {
                            NodeKey renamedKey = renameEntry.getKey();
                            CachedNode oldRenamedNode = workspaceCache.getNode(renamedKey);
                            if (oldRenamedNode == null) {
                                // The node was created in this session, so we can ignore this ...
                                continue;
                            }
                            CachedNode renamedNode = getNode(renamedKey);
                            Path renamedFromPath = workspacePaths.getPath(oldRenamedNode);
                            Path renamedToPath = sessionPaths.getPath(renamedNode);
                            changes.nodeRenamed(renamedKey, renamedToPath, renamedFromPath.getLastSegment());
                        }
                    }

                    // generate reordering events for nodes which have not been reordered to the end
                    Map<NodeKey, SessionNode.Insertions> insertionsByBeforeKey = changedChildren.getInsertionsByBeforeKey();
                    for (SessionNode.Insertions insertion : insertionsByBeforeKey.values()) {
                        for (ChildReference insertedRef : insertion.inserted()) {
                            CachedNode insertedNodePersistent = workspaceCache.getNode(insertedRef);
                            Path nodeOldPath = insertedNodePersistent != null ? workspacePaths.getPath(insertedNodePersistent) : null;

                            CachedNode insertedBeforeNode = workspaceCache.getNode(insertion.insertedBefore());
                            Path insertedBeforePath = workspacePaths.getPath(insertedBeforeNode);

                            Path nodeNewPath = null;
                            if (nodeOldPath != null) {
                                boolean isSnsReordering = nodeOldPath.getLastSegment()
                                                                     .getName()
                                                                     .equals(insertedBeforePath.getLastSegment().getName());
                                nodeNewPath = isSnsReordering ? insertedBeforePath : nodeOldPath;
                            } else {
                                // there is no old path, which means the node is new and reordered at the same time (most likely
                                // due to a version restore)
                                nodeNewPath = sessionPaths.getPath(changedNodes.get(insertedRef.getKey()));
                            }

                            changes.nodeReordered(insertedRef.getKey(),
                                                  node.getKey(),
                                                  nodeNewPath,
                                                  nodeOldPath,
                                                  insertedBeforePath);
                        }
                    }
                }

                ReferrerChanges referrerChanges = node.getReferrerChanges();
                if (referrerChanges != null && !referrerChanges.isEmpty()) {
                    translator.changeReferrers(doc, referrerChanges);
                    changes.nodeChanged(key, newPath);
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
                    documentStore.updateDocument(keyStr, doc);
                    boolean isSameWorkspace = workspaceCache().getWorkspaceKey()
                                                              .equalsIgnoreCase(node.getKey().getWorkspaceKey());
                    // only update the indexes if the node we're working with is in the same workspace as the current workspace.
                    // when linking/un-linking nodes (e.g. shareable node or jcr:system) this condition will be false.
                    // the downside of this is that there may be cases (e.g. back references when working with versions) in which
                    // we might loose information from the indexes
                    if (monitor != null && isSameWorkspace) {
                        // Get the primary and mixin type names; even though we're passing in the session, the two properties
                        // should be there and shouldn't require a looking in the cache...
                        Name primaryType = node.getPrimaryType(this);
                        Set<Name> mixinTypes = node.getMixinTypes(this);
                        monitor.recordUpdate(workspaceName, key, newPath, primaryType, mixinTypes, node.getProperties(this));
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
                            changes.nodeCreated(key, parentKey, childPath, null);
                        }
                    }
                    // Generate NODE_REMOVED events for each of the newly-removed parents ...
                    for (NodeKey parentKey : additionalParents.getRemovals()) {
                        // We need to read some information from the parent node before it was changed ...
                        CachedNode persistedParent = workspaceCache.getNode(parentKey);
                        if (persistedParent != null) {
                            // Find the path to the removed child ...
                            ChildReference ref = persistedParent.getChildReferences(this).getChild(key);
                            if (ref != null) {
                                Path parentPath = workspacePaths.getPath(persistedParent);
                                Path childPath = pathFactory().create(parentPath, ref.getSegment());
                                changes.nodeRemoved(key, parentKey, childPath);
                            }
                        }
                    }
                }
            }
        }

        if (removedNodes != null) {
            assert !removedNodes.isEmpty();
            // we need to collect the referrers at the end only, so that other potential changes in references have been computed
            Set<NodeKey> referrers = new HashSet<NodeKey>();
            for (NodeKey removedKey : removedNodes) {
                // we need the current document from the documentStore, because this differs from what's persisted
                Document doc = documentStore.get(removedKey.toString()).getContentAsDocument();
                referrers.addAll(translator.getReferrers(doc, ReferenceType.STRONG));
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

            // And record the removals via the monitor ...
            if (monitor != null) {
                monitor.recordRemove(workspaceName, removedNodes);
            }
        }

        if (!unusedBinaryKeys.isEmpty()) {
            // There are some binary values that are no longer referenced ...
            for (BinaryKey key : unusedBinaryKeys) {
                changes.binaryValueNoLongerUsed(key);
            }
        }

        changes.setChangedNodes(changedNodes.keySet()); // don't need to make a copy
        changes.freeze(userId, userData, timestamp);
        return changes;
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
                        if (property != null && property.isReference()) {
                            // We need to get the node in the session's cache ...
                            this.changedNodes.remove(nodeKey); // we put REMOVED a dozen lines up ...
                            node = this.mutable(nodeKey);
                            if (node != null) {
                                cleanupReferences = true;
                            }
                            this.changedNodes.put(nodeKey, REMOVED);
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
                    keys.add(childKey);
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
                Logger.getLogger(getClass()).error(e2, msg, e2.getMessage(), e.getMessage());
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        NamespaceRegistry reg = context().getNamespaceRegistry();
        sb.append("Session ").append(context().getId()).append(" to workspace '").append(workspaceCache.getWorkspaceName());
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

    public void createExternalProjection( NodeKey key,
                                          String sourceName,
                                          String externalPath,
                                          String alias ) {
        // register the node in the changes, so it can be saved later
        mutable(key);
        DocumentStore documentStore = workspaceCache().documentStore();
        EditableDocument document = documentStore.get(key.toString()).editDocumentContent();
        DocumentTranslator translator = workspaceCache().translator();
        translator.addFederatedSegment(document, sourceName, externalPath, alias);
    }
}

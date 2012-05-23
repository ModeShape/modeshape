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
import javax.transaction.TransactionManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.DocumentAlreadyExistsException;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.LockFailureException;
import org.modeshape.jcr.cache.MutableCachedNode;
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
    private final TransactionManager tm;

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
        this.tm = sessionContext.getTransactionManager();
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
                changedNodes.remove(nodeToRemove.getKey());
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
            Path changedNodePath = changedNode.getPath(this);
            if (!changedNodePath.isAtOrBelow(nodePath)) {
                continue;
            }

            int insertIndex = changedNodesChildrenFirst.size();
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
        Path sourcePath = srcNode.getPath(this);

        Lock readLock = this.lock.readLock();
        Set<NodeKey> result = new HashSet<NodeKey>();
        try {
            readLock.lock();
            for (Map.Entry<NodeKey, SessionNode> entry : changedNodes.entrySet()) {
                SessionNode changedNodeThisSession = entry.getValue();
                NodeKey changedNodeKey = entry.getKey();
                Path changedNodePath = null;

                if (changedNodeThisSession == REMOVED) {
                    CachedNode persistentRemovedNode = workspaceCache.getNode(changedNodeKey);
                    if (persistentRemovedNode == null) {
                        // the node has been removed without having been persisted previously, so we'll take it into account
                        result.add(changedNodeKey);
                        continue;
                    }
                    changedNodePath = persistentRemovedNode.getPath(this);
                } else {
                    changedNodePath = changedNodeThisSession.getPath(this);
                }

                if (changedNodePath != null && changedNodePath.isAtOrBelow(sourcePath)) {
                    result.add(changedNodeKey);
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
            return !changedNodes.isEmpty();
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
        if (!this.hasChanges()) return;

        Logger logger = Logger.getLogger(getClass());
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning SessionCache.save() with these changes: \n{0}", this);
        }

        ChangeSet events = null;
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes ...
            if (preSaveOperation != null) {
                SaveContext saveContext = new BasicSaveContext(context());
                for (MutableCachedNode node : this.changedNodes.values()) {
                    if (node == REMOVED) continue;
                    checkNodeNotRemovedByAnotherTransaction(node);
                    preSaveOperation.process(node, saveContext);
                }
            }

            try {
                // Try to begin a transaction, if there is a transaction manager ...
                boolean closeTxn = false;
                if (tm != null) {
                    // Start a transaction ...
                    tm.begin();
                    closeTxn = true;
                }

                // Get a monitor ...
                Monitor monitor = sessionContext().createMonitor();

                try {
                    // Now persist the changes ...
                    events = persistChanges(this.changedNodesInOrder, monitor);
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    if (tm != null) tm.rollback();
                    throw e;
                }

                if (closeTxn) {
                    assert tm != null;
                    // Commit the transaction ...
                    tm.commit();
                }

                if (events != null && monitor != null) monitor.recordChanged(events.changedNodes().size());

            } catch (NotSupportedException err) {
                // No nested transactions are supported ...
            } catch (SecurityException err) {
                // No privilege to commit ...
                throw new SystemFailureException(err);
            } catch (IllegalStateException err) {
                // Not associated with a txn??
                throw new SystemFailureException(err);
            } catch (RollbackException err) {
                // Couldn't be committed, but the txn is already rolled back ...
            } catch (HeuristicMixedException err) {
            } catch (HeuristicRollbackException err) {
                // Rollback has occurred ...
                return;
            } catch (SystemException err) {
                // System failed unexpectedly ...
                throw new SystemFailureException(err);
            }

            // The changes have been made, so create a new map (we're using the keys from the current map) ...
            this.changedNodes = new HashMap<NodeKey, SessionNode>();
            this.referrerChangesForRemovedNodes.clear();
            this.changedNodesInOrder.clear();
            this.replacedNodes = null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WrappedException(e);
        } finally {
            lock.unlock();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Completing SessionCache.save()");
        }

        fireChanges(events);
    }

    @Override
    public void save( SessionCache other,
                      PreSave preSaveOperation ) {
        // Try getting locks on both sessions ...
        WritableSessionCache that = (WritableSessionCache)other;
        Lock thisLock = this.lock.writeLock();
        Lock thatLock = that.lock.writeLock();

        ChangeSet events1 = null;
        ChangeSet events2 = null;
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

                // Try to begin a transaction, if there is a transaction manager ...
                boolean closeTxn = false;
                if (tm != null) {
                    // Start a transaction ...
                    tm.begin();
                    closeTxn = true;
                }

                // Get a monitor ...
                Monitor monitor = sessionContext().createMonitor();

                try {
                    // Now persist the changes ...
                    events1 = persistChanges(this.changedNodesInOrder, monitor);
                    events2 = that.persistChanges(that.changedNodesInOrder, monitor);
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    if (tm != null) tm.rollback();
                    throw e;
                }

                if (closeTxn) {
                    assert tm != null;
                    // Commit the transaction ...
                    tm.commit();
                }
                if (monitor != null) {
                    if (events1 != null) monitor.recordChanged(events1.changedNodes().size());
                    if (events2 != null) monitor.recordChanged(events2.changedNodes().size());
                }

            } catch (NotSupportedException err) {
                // No nested transactions are supported ...
            } catch (SecurityException err) {
                // No privilege to commit ...
                throw new SystemFailureException(err);
            } catch (IllegalStateException err) {
                // Not associated with a txn??
                throw new SystemFailureException(err);
            } catch (RollbackException err) {
                // Couldn't be committed, but the txn is already rolled back ...
            } catch (HeuristicMixedException err) {
            } catch (HeuristicRollbackException err) {
                // Rollback has occurred ...
                return;
            } catch (SystemException err) {
                // System failed unexpectedly ...
                throw new SystemFailureException(err);
            }

            // The changes have been made, so create a new map (we're using the keys from the current map) ...
            this.changedNodes = new HashMap<NodeKey, SessionNode>();
            this.referrerChangesForRemovedNodes.clear();
            this.changedNodesInOrder.clear();
            this.replacedNodes = null;

            // And in the referenced session ...
            that.changedNodes = new HashMap<NodeKey, SessionNode>();
            that.changedNodesInOrder.clear();
            that.replacedNodes = null;

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
        fireChanges(events1);
        fireChanges(events2);
    }

    private void checkNodeNotRemovedByAnotherTransaction( MutableCachedNode node ) {
        String keyString = node.getKey().toString();
        // if the node is not new and also missing from the document, another transaction has deleted it
        if (!node.isNew() && !workspaceCache.database().containsKey(keyString)) {
            throw new DocumentNotFoundException(keyString);
        }
    }

    /**
     * This method saves the changes made by both sessions within a single transaction. <b>Note that this must be used with
     * caution, as this method attempts to get write locks on both sessions, meaning they <i>cannot<i> be concurrently used
     * elsewhere (otherwise deadlocks might occur).</b>
     * 
     * @param other the other session
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     */
    @Override
    public void save( CachedNode node,
                      SessionCache other,
                      PreSave preSaveOperation ) {
        Path topPath = node.getPath(this);

        Logger logger = Logger.getLogger(getClass());
        if (logger.isDebugEnabled()) {
            String pathStr = topPath.getString(context().getNamespaceRegistry());
            logger.debug("Beginning SessionCache.save(Path) with subset of changes below '{0}': \n{1}", pathStr, this);
        }

        // Try getting locks on both sessions ...
        WritableSessionCache that = (WritableSessionCache)other;
        Lock thisLock = this.lock.writeLock();
        Lock thatLock = that.lock.writeLock();

        ChangeSet events1 = null;
        ChangeSet events2 = null;
        try {
            thisLock.lock();
            thatLock.lock();

            // Before we start the transaction, apply the pre-save operations to the new and changed nodes below the path ...
            List<NodeKey> savedNodesInOrder = filterChangesAtOrBelowPath(topPath, preSaveOperation);

            try {
                // Try to begin a transaction, if there is a transaction manager ...
                boolean closeTxn = false;
                if (tm != null) {
                    // Start a transaction ...
                    tm.begin();
                    closeTxn = true;
                }

                // Get a monitor ...
                Monitor monitor = sessionContext().createMonitor();

                try {
                    // Now persist the changes ...
                    events1 = persistChanges(savedNodesInOrder, monitor);
                    events2 = that.persistChanges(that.changedNodesInOrder, monitor);
                } catch (RuntimeException e) {
                    // Some error occurred (likely within our code) ...
                    if (tm != null) tm.rollback();
                    throw e;
                }

                if (closeTxn) {
                    assert tm != null;
                    // Commit the transaction ...
                    tm.commit();
                }

                if (monitor != null) {
                    if (events1 != null) monitor.recordChanged(events1.changedNodes().size());
                    if (events2 != null) monitor.recordChanged(events2.changedNodes().size());
                }

            } catch (NotSupportedException err) {
                // No nested transactions are supported ...
            } catch (SecurityException err) {
                // No privilege to commit ...
                throw new SystemFailureException(err);
            } catch (IllegalStateException err) {
                // Not associated with a txn??
                throw new SystemFailureException(err);
            } catch (RollbackException err) {
                // Couldn't be committed, but the txn is already rolled back ...
            } catch (HeuristicMixedException err) {
            } catch (HeuristicRollbackException err) {
                // Rollback has occurred ...
                return;
            } catch (SystemException err) {
                // System failed unexpectedly ...
                throw new SystemFailureException(err);
            }

            // The changes have been made, so remove the changes from this session's map ...
            for (NodeKey savedNode : savedNodesInOrder) {
                this.changedNodes.remove(savedNode);
                this.changedNodesInOrder.remove(savedNode);
                if (this.replacedNodes != null) {
                    this.replacedNodes.remove(savedNode);
                }
            }

            // And in the referenced session ...
            that.changedNodes = new HashMap<NodeKey, SessionNode>();
            that.changedNodesInOrder.clear();
            that.replacedNodes = null;

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
        fireChanges(events1);
        fireChanges(events2);
    }

    private List<NodeKey> filterChangesAtOrBelowPath( Path topPath,
                                                      PreSave preSaveOperation ) throws Exception {
        List<NodeKey> savedNodesInOrder = new LinkedList<NodeKey>();
        SaveContext saveContext = new BasicSaveContext(context());

        for (NodeKey key : this.changedNodesInOrder) {
            MutableCachedNode changedNode = this.changedNodes.get(key);
            if (changedNode != REMOVED) {
                Path path = changedNode.getPath(this);
                if (topPath.isAtOrAbove(path)) {
                    if (preSaveOperation != null) {
                        checkNodeNotRemovedByAnotherTransaction(changedNode);
                        preSaveOperation.process(changedNode, saveContext);
                    }
                    savedNodesInOrder.add(key);
                } else if (!changedNode.getChangedReferrerNodes().isEmpty()) {
                    // we need to include any referrer changes
                    savedNodesInOrder.add(key);
                }
            } else {
                // we can't ignore removed nodes below the top path
                CachedNode removedNode = workspaceCache().getNode(key);
                if (removedNode == null) {
                    // probably removed by someone else
                    continue;
                }
                if (topPath.isAtOrAbove(removedNode.getPath(this))) {
                    savedNodesInOrder.add(key);
                }
            }
        }
        return savedNodesInOrder;
    }

    private void fireChanges( ChangeSet changeSet ) {
        // Notify the workspaces of the changes made. This is done outside of our lock but still before the save returns ...
        if (changeSet != null && changeSet.size() != 0) {
            // Notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(changeSet);
        }
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

        // Get the database ...
        SchematicDb database = workspaceCache.database();
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
                        EditableDocument doc = database.get(keyStr).editDocumentContent();
                        translator.changeReferrers(doc, referrerChanges);
                    }
                    // Note 1: Do not actually remove the document from the database yet; see below (note 2)
                }
                // Otherwise, the removed node was created in the session (but not ever persisted),
                // so we don't have to do anything ...
            } else {
                CachedNode persisted = null;
                Path newPath = sessionPaths.getPath(node);
                NodeKey newParent = node.newParent();
                EditableDocument doc = null;
                EditableDocument metadata = null;
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
                    SchematicEntry nodeEntry = database.get(keyStr);
                    if (nodeEntry == null) {
                        // Could not find the entry in the database, which means it was deleted by someone else
                        // just moments before we got our transaction to save ...
                        throw new DocumentNotFoundException(keyStr);
                    }
                    doc = nodeEntry.editDocumentContent();
                    if (newParent != null) {
                        persisted = workspaceCache.getNode(key);
                        // The node has moved (either within the same parent or to another parent) ...
                        Path oldPath = workspacePaths.getPath(persisted);
                        NodeKey oldParentKey = persisted.getParentKey(workspaceCache);
                        if (!oldParentKey.equals(newParent) || !additionalParents.isEmpty()) {
                            // Don't need to change the doc, since we've moved within the same parent ...
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
                    translator.changeChildren(key, doc, changedChildren, appended);
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
                    translator.changeChildren(key, doc, changedChildren, appended);

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
                    if (database.putIfAbsent(keyStr, doc, metadata) != null) {
                        if (replacedNodes != null && replacedNodes.contains(key)) {
                            // Then a node is being removed and recreated with the same key ...
                            database.put(keyStr, doc, metadata);
                        } else if (removedNodes != null && removedNodes.contains(key)) {
                            // Then a node is being removed and recreated with the same key ...
                            database.put(keyStr, doc, metadata);
                            removedNodes.remove(key);
                        } else {
                            // We couldn't create the entry because one already existed ...
                            throw new DocumentAlreadyExistsException(keyStr);
                        }
                    }
                } else if (monitor != null) {
                    // Get the primary and mixin type names; even though we're passing in the session, the two properties
                    // should be there and shouldn't require a looking in the cache...
                    Name primaryType = node.getPrimaryType(this);
                    Set<Name> mixinTypes = node.getMixinTypes(this);
                    monitor.recordUpdate(workspaceName, key, newPath, primaryType, mixinTypes, node.getProperties(this));
                }
            }
        }

        if (removedNodes != null) {
            assert !removedNodes.isEmpty();
            // we need to collect the referrers at the end only, so that other potential changes in references have been computed
            Set<NodeKey> referrers = new HashSet<NodeKey>();
            for (NodeKey removedKey : removedNodes) {
                referrers.addAll(workspaceCache.getNode(removedKey).getReferrers(workspaceCache, ReferenceType.STRONG));
            }
            // check referential integrity ...
            referrers.removeAll(removedNodes);

            if (!referrers.isEmpty()) {
                throw new ReferentialIntegrityException(removedNodes, referrers);
            }

            // Now remove all of the nodes from the database.
            // Note 2: we do this last because the children are removed from their parent before the removal is handled above
            // (see Node 1), meaning getting the path and other information for removed nodes never would work properly.
            for (NodeKey removedKey : removedNodes) {
                database.remove(removedKey.toString());
            }

            // And record the removals via the monitor ...
            if (monitor != null) monitor.recordRemove(workspaceName, removedNodes);
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
                if (replacedNodes == null) replacedNodes = new HashSet<NodeKey>();
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
                ChildReferences children = null;
                if (node != null) {
                    if (node == REMOVED) continue;
                    // There was a node within this cache ...
                    children = node.getChildReferences(this);
                    removed.put(nodeKey, node);
                    // we need to preserve any existing transient referrer changes for the node which we're removing, as they can
                    // influence ref integrity
                    referrerChangesForRemovedNodes.put(nodeKey, node.getReferrerChanges());
                    // cleanup (remove) all outgoing references from this node to other nodes
                    node.removeAllReferences(this);
                } else {
                    // The node did not exist in the session, so get it from the workspace ...
                    addToChangedNodes.add(nodeKey);
                    CachedNode persisted = workspace.getNode(nodeKey);
                    if (persisted == null) continue;
                    children = persisted.getChildReferences(workspace);
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
            if (changes == null) continue;
            sb.append("\n ");
            sb.append(changes.getString(reg));
        }
        return sb.toString();
    }

}

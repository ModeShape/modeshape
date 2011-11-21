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
import javax.transaction.TransactionManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.LockFailureException;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.ReferentialIntegrityException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionCacheMonitor;
import org.modeshape.jcr.cache.WrappedException;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.cache.document.SessionNode.ChangedAdditionalParents;
import org.modeshape.jcr.cache.document.SessionNode.ChangedChildren;
import org.modeshape.jcr.cache.document.SessionNode.LockChange;
import org.modeshape.jcr.cache.document.SessionNode.MixinChanges;
import org.modeshape.jcr.cache.document.SessionNode.ReferrerChanges;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.DateTime;
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
    private LinkedHashSet<NodeKey> changedNodesInOrder;
    private final TransactionManager tm;

    /**
     * Create a new SessionCache that can be used for making changes to the workspace.
     * 
     * @param context the execution context; may not be null
     * @param workspaceCache the (shared) workspace cache; may not be null
     * @param txnMgr the transaction manager that should be used; may be null if this session should not explicitly begin and
     *        commit transactions
     * @param monitor the cache monitor; may be null
     */
    public WritableSessionCache( ExecutionContext context,
                                 WorkspaceCache workspaceCache,
                                 TransactionManager txnMgr,
                                 SessionCacheMonitor monitor ) {
        super(context, workspaceCache, monitor);
        this.changedNodes = new HashMap<NodeKey, SessionNode>();
        this.changedNodesInOrder = new LinkedHashSet<NodeKey>();
        this.tm = txnMgr;
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
            Iterator<Map.Entry<NodeKey, SessionNode>> iter = changedNodes.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<NodeKey, SessionNode> entry = iter.next();
                SessionNode changedNode = entry.getValue();
                if (changedNode == node) iter.remove();
                Path changedPath = changedNode.getPath(this);
                if (changedPath.isAtOrBelow(nodePath)) iter.remove();
            }
        } finally {
            lock.unlock();
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

                try {
                    // Now persist the changes ...
                    events = persistChanges(this.changedNodesInOrder);
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
            this.changedNodesInOrder.clear();
        } catch (Exception e) {
            throw new WrappedException(e);
        } finally {
            lock.unlock();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Completing SessionCache.save()");
        }

        if (events != null && events.size() != 0) {
            // Then there were changes made, so first record metrics for the changes ...
            recordMetrics(events);

            // and now notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(events);
        }
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
                        if (node == REMOVED) continue;
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

                try {
                    // Now persist the changes ...
                    events1 = persistChanges(this.changedNodesInOrder);
                    events2 = that.persistChanges(that.changedNodesInOrder);
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
            this.changedNodesInOrder.clear();

            // And in the referenced session ...
            that.changedNodes = new HashMap<NodeKey, SessionNode>();
            that.changedNodesInOrder.clear();

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
        if (events1 != null && events1.size() != 0) {
            // Then there were changes made, so first record metrics for the changes ...
            recordMetrics(events1);
            // and now notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(events1);
        }
        if (events2 != null) {
            // Then there were changes made, so first record metrics for the changes ...
            recordMetrics(events2);
            // and now notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(events2);
        }
    }

    /**
     * This method saves the changes made by both sessions within a single transaction. <b>Note that this must be used with
     * caution, as this method attempts to get write locks on both sessions, meaning they <i>cannot<i> be concurrently used
     * elsewhere (otherwise deadlocks might occur).</b>
     * 
     * @param other the other session
     * @throws LockFailureException if a requested lock could not be made
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
            List<NodeKey> savedNodesInOrder = new LinkedList<NodeKey>();
            if (preSaveOperation != null) {
                SaveContext saveContext = new BasicSaveContext(context());
                for (NodeKey key : this.changedNodesInOrder) {
                    MutableCachedNode changedNode = this.changedNodes.get(key);
                    if (changedNode == REMOVED) continue;
                    Path path = changedNode.getPath(this);
                    if (topPath.isAtOrAbove(path)) {
                        preSaveOperation.process(changedNode, saveContext);
                        savedNodesInOrder.add(key);
                    }
                }
            } else {
                for (NodeKey key : this.changedNodesInOrder) {
                    MutableCachedNode changedNode = this.changedNodes.get(key);
                    if (changedNode == REMOVED) continue;
                    Path path = changedNode.getPath(this);
                    if (topPath.isAtOrAbove(path)) savedNodesInOrder.add(key);
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

                try {
                    // Now persist the changes ...
                    events1 = persistChanges(savedNodesInOrder);
                    events2 = that.persistChanges(that.changedNodesInOrder);
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
            for (NodeKey savedNode : savedNodesInOrder) {
                this.changedNodes.remove(savedNode);
                this.changedNodesInOrder.remove(savedNode);
            }

            // And in the referenced session ...
            that.changedNodes = new HashMap<NodeKey, SessionNode>();
            that.changedNodesInOrder.clear();

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
        if (events1 != null && events1.size() != 0) {
            // Then there were changes made, so first record metrics for the changes ...
            recordMetrics(events1);
            // and now notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(events1);
        }
        if (events2 != null) {
            // Then there were changes made, so first record metrics for the changes ...
            recordMetrics(events2);
            // and now notify the workspace (outside of the lock, but still before the save returns) ...
            workspaceCache.changed(events2);
        }
    }

    /**
     * Track any metrics given the supplied changes that have already been committed.
     * 
     * @param changeSet
     */
    protected void recordMetrics( ChangeSet changeSet ) {
        SessionCacheMonitor monitor = monitor();
        if (monitor != null) {
            int count = changeSet.changedNodes().size();
            monitor.performChange(count);
        }

    }

    /**
     * Persist the changes within an already-established transaction.
     * 
     * @param changedNodesInOrder the nodes that are to be persisted; may not be null
     * @return the ChangeSet encapsulating the changes that were made
     * @throws LockFailureException if a requested lock could not be made
     */
    @GuardedBy( "lock" )
    protected ChangeSet persistChanges( Iterable<NodeKey> changedNodesInOrder ) {
        // Compute the save meta-info ...
        ExecutionContext context = context();
        String userId = context.getSecurityContext().getUserName();
        Map<String, String> userData = context.getData();
        DateTime timestamp = context.getValueFactories().getDateFactory().create();
        String workspaceKey = workspaceCache().getWorkspaceKey();
        String repositoryKey = workspaceCache().getRepositoryKey();
        String processKey = workspaceCache().getProcessKey();
        RecordingChanges changes = new RecordingChanges(processKey, repositoryKey, workspaceKey);

        // Get the database ...
        SchematicDb database = workspaceCache.database();
        DocumentTranslator translator = workspaceCache.translator();

        PathHelper sessionPaths = new PathHelper(this);
        PathHelper workspacePaths = new PathHelper(workspaceCache);

        // Make the changes (in order the nodes were added to the session) ...
        Set<NodeKey> referrers = null;
        Set<NodeKey> removedNodes = null;
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
                        referrers = new HashSet<NodeKey>();
                    }
                    assert referrers != null;
                    referrers.addAll(persisted.getReferrers(workspaceCache, ReferenceType.STRONG));
                    Path path = workspacePaths.getPath(persisted);
                    changes.nodeRemoved(key, persisted.getParentKey(workspaceCache), path);
                    removedNodes.add(key);
                    // Note 1: Do not actually remove the document from the database yet; see below (note 2)
                }
                // Otherwise, it's a new node created in the session, so we don't have to do anything ...
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
                } else {
                    SchematicEntry nodeEntry = database.get(keyStr);
                    doc = nodeEntry.editDocumentContent();
                    if (newParent != null) {
                        persisted = workspaceCache.getNode(key);
                        // The node has moved (either within the same parent or to another parent) ...
                        Path oldPath = workspacePaths.getPath(persisted);
                        NodeKey oldParent = persisted.getParentKey(workspaceCache);
                        if (!oldParent.equals(newParent) || !additionalParents.isEmpty()) {
                            // Don't need to change the doc, since we've moved within the same parent ...
                            translator.setParents(doc, node.newParent(), oldParent, additionalParents);
                        }
                        // Generate a move even either way ...
                        changes.nodeMoved(key, newParent, oldParent, newPath, oldPath);
                    } else if (additionalParents != null) {
                        // The node in another workspace has been linked to this workspace ...
                        translator.setParents(doc, null, null, additionalParents);
                    }

                    // Deal with mixin changes here (since for new nodes they're put into the properties) ...
                    MixinChanges mixinChanges = node.mixinChanges(false);
                    if (mixinChanges != null && !mixinChanges.isEmpty()) {
                        Property oldProperty = translator.getProperty(doc, JcrLexicon.MIXIN_TYPES);
                        translator.addPropertyValues(doc, JcrLexicon.MIXIN_TYPES, true, mixinChanges.getAdded());
                        translator.removePropertyValues(doc, JcrLexicon.MIXIN_TYPES, mixinChanges.getRemoved());
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
                    boolean success = true;
                    switch (lockChange) {
                        case LOCK_FOR_SESSION:
                            success = translator.lock(doc, userId, true);
                            break;
                        case LOCK_FOR_NON_SESSION:
                            success = translator.lock(doc, userId, false);
                            break;
                        case UNLOCK:
                            translator.unlock(doc);
                            break;
                    }
                    if (!success) {
                        throw new LockFailureException(key);
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
                        Property oldProperty = translator.removeProperty(doc, name);
                        if (oldProperty != null) {
                            // the property was removed ...
                            changes.propertyRemoved(key, newPath, oldProperty);
                        }
                    }
                }

                // Save the changes to the properties ...
                if (!node.changedProperties().isEmpty()) {
                    if (!node.isNew() && persisted == null) {
                        persisted = workspaceCache.getNode(key);
                    }
                    for (Map.Entry<Name, Property> propEntry : node.changedProperties().entrySet()) {
                        Name name = propEntry.getKey();
                        Property prop = propEntry.getValue();
                        // Get the old property ...
                        Property oldProperty = persisted != null ? persisted.getProperty(name, workspaceCache) : null;
                        translator.setProperty(doc, prop);
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
                            if (persistent != null) workspacePaths.getPath(persistent);
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
                }

                ReferrerChanges referrerChanges = node.getReferrerChanges();
                if (referrerChanges != null && !referrerChanges.isEmpty()) {
                    translator.changeReferrers(doc, referrerChanges);
                    changes.nodeChanged(key, newPath);
                }

                if (node.isNew()) {
                    // We need to create the schematic entry for the new node ...
                    database.put(keyStr, doc, metadata);
                }
            }
        }

        if (removedNodes != null) {
            assert !removedNodes.isEmpty();
            assert referrers != null;
            // At least one node was deleted, so check referential integrity ...
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
        }

        changes.setChangedNodes(changedNodes.keySet()); // don't need to make a copy
        changes.freeze(userId, userData, timestamp);
        return changes;
    }

    protected static class PathHelper {
        private final NodeCache cache;
        private final Map<NodeKey, Path> paths = new HashMap<NodeKey, Path>();

        public PathHelper( NodeCache cache ) {
            this.cache = cache;
        }

        public Path getPath( CachedNode node ) {
            NodeKey key = node.getKey();
            Path path = paths.get(key);
            if (path == null) {
                path = node.getPath(cache);
                paths.put(key, path); // even if null
            }
            return path;
        }

        public boolean removePath( NodeKey key ) {
            return paths.remove(key) != null;
        }
    }

    protected SessionNode add( SessionNode newNode ) {
        assert newNode != REMOVED;
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            NodeKey key = newNode.getKey();
            SessionNode node = changedNodes.put(key, newNode);
            if (node != null && node != REMOVED) {
                // Put the original node back ...
                changedNodes.put(key, node);
                return node;
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

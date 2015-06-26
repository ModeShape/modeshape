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
package org.modeshape.jcr.cache;

import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * 
 */
public interface SessionCache extends NodeCache {

    /**
     * The context of a save operation, created during each call to {@link #save} and passed to the
     * {@link PreSave#process(MutableCachedNode, SaveContext)} invocations.
     */
    public static interface SaveContext {
        /**
         * Get the instance in time that the save is taking place.
         * 
         * @return the save timestamp; never null
         */
        DateTime getTime();

        /**
         * Get the identifier for the user that is making this save.
         * 
         * @return the user identifier; never null
         */
        String getUserId();
    }

    /**
     * The definition of a callback that can be implemented and passed to {@link SessionCache#save(SessionCache, PreSave)} and
     * {@link SessionCache#save(Set, SessionCache, PreSave)}, allowing the caller to receive a hook where they can interrogate
     * each of the changed nodes and perform additional logic prior to the actual persisting of the changes. Note that
     * implementations are free to make additional modifications to the supplied nodes, and even create additional nodes or change
     * persistent but unchanged nodes, as long as these operations are done within the same calling thread.
     */
    public static interface PreSave {
        /**
         * Process the supplied node prior to saving the changes. This allows implementations to use the changes to automatically
         * adjust this node or other content.
         * 
         * @param modifiedOrNewNode the mutable node that was changed in this session; never null
         * @param context the context of the save operation; never null
         * @throws Exception if there is a problem during the processing
         */
        void process( MutableCachedNode modifiedOrNewNode,
                      SaveContext context ) throws Exception;

        /**
         * Process the supplied existing node prior to saving the changes but only after the entry corresponding to the key of the
         * node has been locked in Infinispan. Note that locking in Infinispan does not occur always, but only if the
         * {@link org.infinispan.transaction.LockingMode#PESSIMISTIC} flag is enabled. This method should be implemented as
         * optimal as possible and should only be needed in multi-threaded scenarios where concurrent modifications may break
         * consistency.
         * 
         * @param modifiedNode the mutable node that was changed in this session; never null
         * @param context the context of the save operation; never null
         * @param persistentNodeCache the node cache from which the persistent representation of the nodes can be obtained; never
         *        null
         * @throws Exception if there is a problem during the processing
         */
        void processAfterLocking( MutableCachedNode modifiedNode,
                                  SaveContext context,
                                  NodeCache persistentNodeCache ) throws Exception;
    }

    /**
     * Get the context for this session.
     * 
     * @return the session's context; never null
     */
    public ExecutionContext getContext();

    /**
     * Adds a [key,value] data pair for this cache's context
     * 
     * @param key the key for the context data
     * @param value the value for the context data
     */
    public void addContextData( String key,
                                String value );

    /**
     * Saves all changes made within this session.
     * 
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     * @throws DocumentStoreException if there is a problem storing or retrieving a document
     */
    public void save();

    /**
     * Saves all of this session's changes that were made at or below the specified path. Note that this is not terribly
     * efficient, but is done to implement the deprecated {@link javax.jcr.Item#save()}.
     * 
     * @param toBeSaved the set of keys identifying the nodes whose changes should be saved; may not be null
     * @param otherSession another session whose changes should be saved with this session's changes; may not be null
     * @param preSaveOperation the set of operations to run against the new and changed nodes prior to saving; may be null
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     * @throws DocumentStoreException if there is a problem storing or retrieving a document
     */
    public void save( Set<NodeKey> toBeSaved,
                      SessionCache otherSession,
                      PreSave preSaveOperation );

    /**
     * Saves all changes made within this session and the supplied session, using a single transaction for both.
     * 
     * @param otherSession another session whose changes should be saved with this session's changes; may not be null
     * @param preSaveOperation the set of operations to run against the new and changed nodes prior to saving; may be null
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     * @throws DocumentStoreException if there is a problem storing or retrieving a document
     */
    public void save( SessionCache otherSession,
                      PreSave preSaveOperation );

    /**
     * Determine whether this session has any transient, unsaved changes.
     * 
     * @return true if there are unsaved changes, or false otherwise
     */
    public boolean hasChanges();

    /**
     * Returns a set with the {@link NodeKey}s of the transient nodes from this cache. Please note that there may be nodes which
     * have been removed by another session.
     * 
     * @return a <code>Set</code> with the changed keys, or an empty set if
     *         {@link org.modeshape.jcr.cache.SessionCache#hasChanges()} is false. The returned set is a mutable copy of the
     *         underlying set.
     */
    public Set<NodeKey> getChangedNodeKeys();

    /**
     * Returns a set with the {@link NodeKey}s of the transient nodes from this cache which are at or below the path of the given
     * node
     * 
     * @param node a non-null {@link CachedNode} instance
     * @return a <set>Set</set> of nodekeys, or an empty set if no nodes are found
     * @throws NodeNotFoundException if any of changes registered in this cache refer to nodes that have been removed in the
     *         meantime.
     */
    public Set<NodeKey> getChangedNodeKeysAtOrBelow( CachedNode node ) throws NodeNotFoundException;

    /**
     * Returns a set with the {@link NodeKey}s of the existing nodes (persistent not transient & new) which are at and below the
     * path of the node with the given key. Note that this method will attempt to load each node
     * 
     * @param nodeKey the key of node which will be considered the root node
     * @return a <set>Set</set> of nodekeys or an empty set
     */
    public Set<NodeKey> getNodeKeysAtAndBelow( NodeKey nodeKey );

    /**
     * Clears all changes in the cache that are at or below the supplied node.
     * 
     * @param node the node at or below which all changes should be cleared; may not be null
     */
    public void clear( CachedNode node );

    /**
     * Get the cache the reflects the workspace content, without any of the transient, unsaved changes of this session.
     * 
     * @return the workspace cache; never null
     */
    public WorkspaceCache getWorkspace();

    /**
     * Get a mutable form of the node with the supplied key. If this session already has a mutable node in its cache, that
     * existing mutable node is returned; otherwise, a new mutable node is created and added to the session's cache.
     * 
     * @param key the key for the node; may not be null
     * @return the mutable child node
     * @throws NodeNotFoundException if there is no existing node in the session cache or workspace cache
     * @throws UnsupportedOperationException if this session is marked for read-only operations
     */
    public MutableCachedNode mutable( NodeKey key ) throws NodeNotFoundException, UnsupportedOperationException;

    /**
     * Destroy the subgraph with the supplied node as the top node in the subgraph. This method should be called after the node is
     * already {@link MutableCachedNode#removeChild(SessionCache, NodeKey) removed from its parent node}.
     * 
     * @param key the key for the top node in the subgraph; may not be null
     * @throws NodeNotFoundException if there is no existing node in the session cache or workspace cache
     * @throws UnsupportedOperationException if this session is marked for read-only operations
     */
    public void destroy( NodeKey key ) throws NodeNotFoundException, UnsupportedOperationException;

    /**
     * Return whether the node with the supplied key has been removed using this session but not yet persisted.
     * 
     * @param key the for the node; may not be null
     * @return true if the node was removed in this session, or false otherwise
     */
    public boolean isDestroyed( NodeKey key );

    /**
     * Create a new node key for the current source and workspace.
     * 
     * @return a new node key; never null
     */
    public NodeKey createNodeKey();

    /**
     * Create a new node key for the current source and workspace.
     * 
     * @param identifier the unique identifier for the key; if null, a generated identifier will be used
     * @return a new node key; never null
     */
    public NodeKey createNodeKeyWithIdentifier( String identifier );

    /**
     * Returns whether this cache is readonly.
     * 
     * @return true if this cache is readonly, or false otherwise
     */
    public boolean isReadOnly();

    /**
     * Create a new node key for the current source and workspace.
     * 
     * @param sourceName the name (not key) for the source; if null, the key for the current source is used
     * @return a new node key; never null
     */
    public NodeKey createNodeKeyWithSource( String sourceName );

    /**
     * Create a new node key for the current source and workspace.
     * 
     * @param sourceName the name (not key) for the source; if null, the key for the current source is used
     * @param identifier the unique identifier for the key; if null, a generated identifier will be used
     * @return a new node key; never null
     */
    public NodeKey createNodeKey( String sourceName,
                                  String identifier );

    /**
     * Check whether this session is running within a transaction. This is commonly called by components that change persistent
     * state. Such persistent state might not be noticed by this session cache.
     */
    public void checkForTransaction();

    @Override
    public SessionCache unwrap();
}

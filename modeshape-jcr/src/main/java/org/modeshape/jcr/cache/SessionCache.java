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
package org.modeshape.jcr.cache;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import java.util.Set;

/**
 * 
 */
public interface SessionCache extends NodeCache {

    /**
     * The context of a save operation, created during each call to {@link #save} and passed to the
     * {@link PreSave#process(MutableCachedNode, SaveContext)} invocations.
     */
    static interface SaveContext {
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
     * {@link SessionCache#save(CachedNode, SessionCache, PreSave)}, allowing the caller to recieve a hook where they can
     * interrogate each of the changed nodes and perform additional logic prior to the actual persisting of the changes. Note that
     * implementations are free to make additional modifications to the supplied nodes, and even create additional nodes or change
     * persistent but unchanged nodes, as long as these operations are done within the same calling thread.
     */
    static interface PreSave {
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
     */
    public void save();

    /**
     * Saves all of this session's changes that were made at or below the specified path. Note that this is not terribly
     * efficient, but is done to implement the deprecated {@link javax.jcr.Item#save()}.
     * 
     * @param node the node at or below which all changes should be saved; may not be null
     * @param otherSession another session whose changes should be saved with this session's changes; may not be null
     * @param preSaveOperation the set of operations to run against the new and changed nodes prior to saving; may be null
     * @throws LockFailureException if a requested lock could not be made
     * @throws DocumentAlreadyExistsException if this session attempts to create a document that has the same key as an existing
     *         document
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
     */
    public void save( CachedNode node,
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
     * Returns a set with the {@link NodeKey}s of the transient nodes from this cache.
     *
     * @return a <code>Set</code> with the changed keys, or an empty set if {@link org.modeshape.jcr.cache.SessionCache#hasChanges()} is false.
     * The returned set is a mutable copy of the underlying set.
     */
    public Set<NodeKey> getChangedNodeKeys();

    /**
     * Returns a set with the {@link NodeKey}s of the transient nodes from this cache which are at or below the path of the given
     * node
     *
     * @param node a non-null {@link CachedNode} instance
     * @return a <set>Set</set> of nodekeys, or an empty set if no nodes are found
     */
    public Set<NodeKey> getChangedNodeKeysAtOrBelow(CachedNode node);

    /**
     * Clears all changes in the cache that are at or below the supplied node.
     * 
     * @param node the node at or below which all changes should be cleared; may not be null
     */
    void clear( CachedNode node );

    /**
     * Get the cache the reflects the workspace content, without any of the transient, unsaved changes of this session.
     * 
     * @return the workspace cache; never null
     */
    public NodeCache getWorkspace();

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
}

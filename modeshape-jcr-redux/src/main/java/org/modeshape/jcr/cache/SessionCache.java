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

import org.modeshape.jcr.core.ExecutionContext;

/**
 * 
 */
public interface SessionCache extends NodeCache {

    /**
     * Get the context for this session.
     * 
     * @return the session's context; never null
     */
    public ExecutionContext getContext();

    /**
     * Saves all changes made within this session.
     */
    public void save();

    /**
     * Saves all of this session's changes that were made at or below the specified path. Note that this is not terribly
     * efficient, but is done to implement the deprecated {@link javax.jcr.Item#save()}.
     * 
     * @param node the node at or below which all changes should be saved; may not be null
     */
    public void save( CachedNode node );

    /**
     * Saves all changes made within this session and the supplied session, using a single transaction for both.
     * 
     * @param otherSession another session whose changes should be saved with this session's changes; may not be null
     */
    public void save( SessionCache otherSession );

    /**
     * Determine whether this session has any transient, unsaved changes.
     * 
     * @return true if there are unsaved changes, or false otherwise
     */
    public boolean hasChanges();

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

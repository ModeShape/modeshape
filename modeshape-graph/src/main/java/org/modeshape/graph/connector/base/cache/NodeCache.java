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
package org.modeshape.graph.connector.base.cache;

import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.graph.connector.base.Node;

/**
 * The basic contract for a workspace-level cache of paths to the nodes stored at that path.
 * <p>
 * Implementations must provide a no-argument constructor in order to be instantiated by {@link NodeCachePolicy}. After
 * instantiation, the {@link #assignPolicy(NodeCachePolicy)} method will be called to inject the cache policy into the
 * implementation.
 * </p>
 * <p>
 * Implementations must be thread-safe.
 * </p>
 * 
 * @param <KeyType> the key for the cache entries, normally the natural unique identifier for the node
 * @param <NodeType> the node type that is being cached
 */
@ThreadSafe
public interface NodeCache<KeyType, NodeType extends Node> {

    /**
     * Injects the cache policy into the cache
     * 
     * @param policy the active cache policy for the repository source with which this cache is associated
     * @throws IllegalStateException if this method is called on a cache that has already been initialized.
     */
    public void assignPolicy( NodeCachePolicy<KeyType, NodeType> policy );

    /**
     * Clears all statistics for this cache
     */
    public void clearStatistics();

    /**
     * @return the statistics since the most recent of the cache initialization or the last call to {@link #clearStatistics()};
     *         never null
     */
    public CacheStatistics getStatistics();

    /**
     * Retrieves the cached node with the given path, it it exists and is valid
     * 
     * @param path the path for the node to be retrieved
     * @return the cached node with the given path; may be null if no node with that path is cached or a node with that path is
     *         cached but is deemed invalid for implementation-specific reasons
     */
    public NodeType get( KeyType path );

    /**
     * Attempts to cache the given node. Implementations must call {@link NodeCachePolicy#shouldCache(Node)} on the the
     * {@link #assignPolicy(NodeCachePolicy) assigned policy} to determine if the node should be cached.
     * 
     * @param key the key for the node that is to be cached; may not be null
     * @param node the node that is to be cached; may not be null
     */
    public void put( KeyType key,
                     NodeType node );

    /**
     * Removes the node with the given key from the cache, if it is in currently in the cache. If no node with the given key is
     * currently in the cache, this method returns silently.
     * 
     * @param key the key for the node that is to be removed; may not be null
     */
    public void remove( KeyType key );

    /**
     * Removes all nodes from the cache.
     */
    public void removeAll();

    /**
     * Indicates that the cache is no longer in use and should relinquish any resources.
     */
    public void close();
}

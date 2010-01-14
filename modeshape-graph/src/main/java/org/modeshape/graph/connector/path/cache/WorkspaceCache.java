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
package org.modeshape.graph.connector.path.cache;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.connector.path.PathNode;
import org.modeshape.graph.property.Path;

/**
 * The basic contract for a workspace-level cache of paths to the nodes stored at that path.
 * <p>
 * Implementations must provide a no-argument constructor in order to be instantiated by {@link PathRepositoryCache}. After
 * instantiation, the {@link #initialize(PathCachePolicy, String)} method will be called to inject the cache policy into the
 * implementation.
 * </p>
 * <p>
 * Implementations must be thread-safe.
 * </p>
 */
@ThreadSafe
public interface WorkspaceCache {

    /**
     * Injects the cache policy into the cache
     * 
     * @param policy the active cache policy for the repository source with which this cache is associated
     * @param workspaceName the name of the workspace that this cache is managing
     * @throws IllegalStateException if this method is called on a cache that has already been initialized.
     */
    public void initialize( PathCachePolicy policy,
                            String workspaceName );

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
    public PathNode get( Path path );

    /**
     * Attempts to cache the given node. Implementations must call {@link PathCachePolicy#shouldCache(PathNode)} on the policy
     * from the {@link #initialize(PathCachePolicy, String)} method to determine if the node should be cached.
     * 
     * @param node the node that is to be cached; may not be null
     */
    public void set( PathNode node );

    /**
     * Invalidates all nodes in the cache that have the given path or have a path that is an ancestor of the given path
     * 
     * @param path the root of the branch of nodes that are to be invalidated; may not be null
     */
    public void invalidate( Path path );

    /**
     * Indicates that the cache is no longer in use and should relinquish any resources.
     */
    public void close();
}

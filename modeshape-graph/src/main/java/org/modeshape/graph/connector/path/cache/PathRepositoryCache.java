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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;

/**
 * The repository-source level cache of workspace names to {@link WorkspaceCache workspace caches}.
 * <p>
 * This object gets created for each repository whenever the {@link PathCachePolicy cache policy} is set. When the cache policy is
 * modified, the old {@code PathRepositoryCache} is {@link #close() closed} after the new path repository cache is created.
 * </p>
 */
@ThreadSafe
public class PathRepositoryCache {

    private final PathCachePolicy policy;
    private final ConcurrentMap<String, WorkspaceCache> cachesByName = new ConcurrentHashMap<String, WorkspaceCache>();

    public PathRepositoryCache( PathCachePolicy policy ) {
        this.policy = policy;
    }

    public void close() {
        for (WorkspaceCache cache : cachesByName.values()) {
            cache.close();
        }
    }

    /**
     * Gets the cache for the named workspace, creating a cache if necessary. Subsequent calls to this method with the same
     * workspace name must return the exact same cache instance.
     * 
     * @param workspaceName the name of the workspace for which the cache should be returned.
     * @return the cache instance associated with the workspace; never null
     * @throws IllegalStateException if no cache exists for this workspace and an instance of the
     *         {@link PathCachePolicy#getCacheClass() cache class from the policy} cannot be created.
     */
    public WorkspaceCache getCache( String workspaceName ) {
        WorkspaceCache cache = cachesByName.get(workspaceName);
        if (cache != null) return cache;

        cache = newCache(workspaceName);
        cachesByName.putIfAbsent(workspaceName, cache);

        return cachesByName.get(workspaceName);
    }

    private final WorkspaceCache newCache( String workspaceName ) {
        WorkspaceCache cache = null;

        try {
            cache = policy.getCacheClass().newInstance();
            cache.initialize(policy, workspaceName);
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException(iae);
        } catch (InstantiationException ie) {
            throw new IllegalStateException(ie);
        }
        return cache;
    }

}

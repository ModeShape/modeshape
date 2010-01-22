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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.connector.path.PathNode;
import org.modeshape.graph.property.Path;

/**
 * Implementation of {@link WorkspaceCache} that stores all nodes in-memory.
 */
@ThreadSafe
public class InMemoryWorkspaceCache implements WorkspaceCache {
    private final ConcurrentMap<Path, CacheEntry> nodesByPath = new ConcurrentHashMap<Path, CacheEntry>();
    private PathCachePolicy policy = null;
    private DefaultCacheStatistics statistics = new DefaultCacheStatistics();

    public void initialize( PathCachePolicy policy,
                            String workspaceName ) {
        if (this.policy != null) {
            throw new IllegalStateException();
        }

        this.policy = policy;
    }

    protected PathCachePolicy policy() {
        return policy;
    }

    public void clearStatistics() {
        statistics = new DefaultCacheStatistics();
    }

    public CacheStatistics getStatistics() {
        return this.statistics;
    }

    public PathNode get( Path path ) {
        assert path != null;

        CacheEntry entry = nodesByPath.get(path);
        if (entry == null) {
            statistics.incrementMisses();
            return null;
        }

        PathNode node = entry.getNode();
        if (node != null) {
            statistics.incrementHits();
            return node;
        }

        nodesByPath.remove(path, entry);
        statistics.incrementExpirations();
        return null;
    }

    public void set( PathNode node ) {
        assert node != null;

        if (!policy.shouldCache(node)) return;
        
        statistics.incrementWrites();
        nodesByPath.put(node.getPath(), new CacheEntry(node));
    }

    public void invalidate( Path path ) {
        assert path != null;

        for (Iterator<Path> iter = nodesByPath.keySet().iterator(); iter.hasNext();) {
            Path key = iter.next();
            if (key.isAtOrBelow(path)) {
                iter.remove();
            }
        }
    }

    public void close() {
        assert this.statistics != null : "Attempt to close an already-closed cache";

        this.nodesByPath.clear();
        this.statistics = null;
    }

    class CacheEntry {
        private final SoftReference<PathNode> ref;
        private final long expiryTime;

        CacheEntry( PathNode node ) {
            ref = new SoftReference<PathNode>(node);
            expiryTime = System.currentTimeMillis() + (policy().getTimeToLive() * 1000);
        }

        PathNode getNode() {
            if (System.currentTimeMillis() > expiryTime) {
                return null;
            }

            return ref.get();
        }
    }

    /**
     * Trivial path cache policy implementation that caches all nodes in an in-memory cache.
     * <p>
     * As a result, this cache policy may not be safe for use with some large repositories as it does not attempt to limit cache
     * attempts based on the size of the node or the current size of the cache.
     * </p>
     */
    public static class InMemoryCachePolicy implements PathCachePolicy {

        private static final long serialVersionUID = 1L;

        private long cacheTimeToLiveInSeconds;

        /**
         * @return true for all nodes
         * @see PathCachePolicy#shouldCache(PathNode)
         */
        public boolean shouldCache( PathNode node ) {
            return true;
        }

        public long getTimeToLive() {
            return this.cacheTimeToLiveInSeconds;
        }

        public void setTimeToLive( long timeToLiveInSeconds ) {
            this.cacheTimeToLiveInSeconds = timeToLiveInSeconds;
        }

        public Class<? extends WorkspaceCache> getCacheClass() {
            return InMemoryWorkspaceCache.class;
        }
    }
}

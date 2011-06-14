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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.graph.connector.base.MapNode;
import org.modeshape.graph.connector.base.Node;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.property.Path;

/**
 * Implementation of {@link WorkspaceCache} that stores all nodes in-memory.
 * 
 * @param <KeyType> the key for the cache entries, normally the natural unique identifier for the node
 * @param <NodeType> the node type that is being cached
 */
@ThreadSafe
public abstract class InMemoryWorkspaceCache<KeyType, NodeType extends Node> implements WorkspaceCache<KeyType, NodeType> {
    protected final ConcurrentMap<KeyType, CacheEntry> entriesByKey = new ConcurrentHashMap<KeyType, CacheEntry>();
    protected BaseCachePolicy<KeyType, NodeType> policy = null;
    private DefaultCacheStatistics statistics = new DefaultCacheStatistics();

    public InMemoryWorkspaceCache( BaseCachePolicy<KeyType, NodeType> policy ) {
        assignPolicy(policy);
    }

    public void assignPolicy( BaseCachePolicy<KeyType, NodeType> policy) {
        if (this.policy != null) {
            throw new IllegalStateException();
        }

        this.policy = policy;
    }
    
    public void clearStatistics() {
        statistics = new DefaultCacheStatistics();
    }

    public CacheStatistics getStatistics() {
        return this.statistics;
    }

    public NodeType get( KeyType path ) {
        assert path != null;

        CacheEntry entry = entriesByKey.get(path);
        if (entry == null) {
            statistics.incrementMisses();
            return null;
        }

        NodeType node = entry.getNode();
        if (node != null) {
            statistics.incrementHits();
            return node;
        }

        entriesByKey.remove(path, entry);
        statistics.incrementExpirations();
        return null;
    }

    public void put( KeyType key,
                     NodeType node ) {

        assert node != null;

        if (!policy.shouldCache(node)) return;

        statistics.incrementWrites();
        entriesByKey.put(key, new CacheEntry(node));
    }

    public void remove( KeyType key ) {
        entriesByKey.remove(key);
    }

    public void close() {
        assert this.statistics != null : "Attempt to close an already-closed cache";

        this.entriesByKey.clear();
        this.statistics = null;
    }

    /**
     * Internal class to associate cache entries with a expiration time and wrap the reference.
     */
    private class CacheEntry {
        private final SoftReference<NodeType> ref;
        private final long expiryTime;

        CacheEntry( NodeType node ) {
            ref = new SoftReference<NodeType>(node);
            expiryTime = System.currentTimeMillis() + (policy.getTimeToLive() * 1000);
        }

        NodeType getNode() {
            if (System.currentTimeMillis() > expiryTime) {
                return null;
            }

            return ref.get();
        }
    }

    /**
     * Path cache policy implementation that caches all nodes in an in-memory cache.
     * <p>
     * As a result, this cache policy may not be safe for use with some large repositories as it does not attempt to limit cache
     * attempts based on the size of the node or the current size of the cache.
     * </p>
     * 
     * @param <NodeType> the node type that is being cached
     */
    public static class MapCachePolicy<NodeType extends MapNode> implements BaseCachePolicy<UUID, NodeType> {

        private static final long serialVersionUID = 1L;

        private long cacheTimeToLiveInSeconds;

        public MapCachePolicy( long timeToLiveInSeconds ) {
            super();
            this.setTimeToLive(timeToLiveInSeconds);
        }

        /**
         * @param node
         * @return true for all nodes
         * @see BaseCachePolicy#shouldCache(Node)
         */
        public boolean shouldCache( NodeType node ) {
            return true;
        }

        public long getTimeToLive() {
            return this.cacheTimeToLiveInSeconds;
        }

        public void setTimeToLive( long timeToLiveInSeconds ) {
            this.cacheTimeToLiveInSeconds = timeToLiveInSeconds;
        }

        @SuppressWarnings( "unchecked" )
        public MapCache<NodeType> newCache() {
            return new MapCache<NodeType>(this);
        }
    }

    public static class MapCache<N extends MapNode> extends InMemoryWorkspaceCache<UUID, N> {

        public MapCache( BaseCachePolicy<UUID, N> policy ) {
            super(policy);
        }
    }

    public static class PathCachePolicy implements BaseCachePolicy<Path, PathNode> {

        private static final long serialVersionUID = 1L;

        private long cacheTimeToLiveInSeconds;

        public PathCachePolicy( long timeToLiveInSeconds ) {
            super();
            this.setTimeToLive(timeToLiveInSeconds);
        }

        /**
         * @return true for all nodes
         * @see BaseCachePolicy#shouldCache(Node)
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

        @SuppressWarnings( "unchecked" )
        public PathCache newCache() {
            return new PathCache(this);
        }
    }

    public static class PathCache extends InMemoryWorkspaceCache<Path, PathNode> implements PathWorkspaceCache<Path, PathNode> {

        public PathCache( BaseCachePolicy<Path, PathNode> policy ) {
            super(policy);
        }

        public void invalidate( Path path ) {
            assert path != null;

            for (Iterator<Path> iter = entriesByKey.keySet().iterator(); iter.hasNext();) {
                Path key = iter.next();
                if (key.isAtOrBelow(path)) {
                    iter.remove();
                }
            }
        }

    }
}

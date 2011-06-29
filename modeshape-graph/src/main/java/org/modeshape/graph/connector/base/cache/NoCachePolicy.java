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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.connector.base.Node;
import org.modeshape.graph.connector.path.PathNode;
import org.modeshape.graph.connector.path.cache.PathCachePolicy;
import org.modeshape.graph.property.Path;

/**
 * Trivial path cache policy implementation that performs no caching at all
 * 
 * @param <KeyType>
 * @param <NodeType>
 */
@Immutable
public class NoCachePolicy<KeyType, NodeType extends Node> implements NodeCachePolicy<KeyType, NodeType> {

    private static final long serialVersionUID = 1L;

    /**
     * @return false for all nodes
     * @see PathCachePolicy#shouldCache(PathNode)
     */
    public boolean shouldCache( NodeType node ) {
        return false;
    }

    public long getTimeToLive() {
        return 0;
    }

    @SuppressWarnings( "unchecked" )
    public NullCache newCache() {
        return new NullCache();
    }

    class NullCache implements NodeCache<KeyType, NodeType> {

        private DefaultCacheStatistics statistics = new DefaultCacheStatistics();

        public void assignPolicy( NodeCachePolicy<KeyType, NodeType> policy ) {
            // Do nothing
        }

        public void close() {
            // Do nothing
        }

        public NodeType get( KeyType path ) {
            statistics.incrementMisses();
            return null;
        }

        public void put( KeyType key,
                         NodeType node ) {
            // Do nothing
        }

        public void remove( KeyType key ) {
            // Do nothing
        }

        public void removeAll() {
            // Do nothing
        }

        public void invalidate( Path path ) {
            // Do nothing
        }

        public void clearStatistics() {
            statistics = new DefaultCacheStatistics();
        }

        public CacheStatistics getStatistics() {
            return this.statistics;
        }
    }
}

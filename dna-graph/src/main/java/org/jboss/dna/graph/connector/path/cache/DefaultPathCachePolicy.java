/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.path.cache;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.connector.path.PathNode;

/**
 * Trivial path cache policy implementation that caches all nodes in an in-memory cache.
 * <p>
 * As a result, this cache policy may not be safe for use with some large repositories as it does not attempt to limit cache
 * attempts based on the size of the node or the current size of the cache.
 * </p>
 */
@Immutable
public class DefaultPathCachePolicy implements PathCachePolicy {

    private static final long serialVersionUID = 1L;

    private final long cacheTimeToLiveInSeconds;

    public DefaultPathCachePolicy( long cacheTimeToLiveInSeconds ) {
        super();
        this.cacheTimeToLiveInSeconds = cacheTimeToLiveInSeconds;
    }

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

    public Class<? extends WorkspaceCache> getCacheClass() {
        return InMemoryWorkspaceCache.class;
    }
}

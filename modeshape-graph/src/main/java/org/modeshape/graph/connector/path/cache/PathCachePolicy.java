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

import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.path.PathNode;

/**
 * The {@PathCachePolicy} provides a method for selectively allowing or disallowing nodes to be cached based on
 * their size, volatility, or any other factor that can be determined for the node.
 */
public interface PathCachePolicy extends CachePolicy {

    /**
     * Indicates whether the node should be cached .
     * 
     * @param node the node that may or may not be cached; may not be null
     * @return true if the node should be cached and false if it should not be cached
     */
    boolean shouldCache( PathNode node );

    /**
     * Return the class to be used as the cache implementation
     * 
     * @return the cache class
     */
    Class<? extends WorkspaceCache> getCacheClass();
}

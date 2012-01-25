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

/**
 * 
 */
public interface NodeCache {

    /**
     * Clears all changes in the cache.
     */
    void clear();

    /**
     * Get the node key for the root node.
     * 
     * @return the root node's key; never null
     */
    NodeKey getRootKey();

    /**
     * Get the cached representation of the node with the supplied node key.
     * 
     * @param key the node key; may not be null
     * @return the cached node, or null if there is no such node
     */
    CachedNode getNode( NodeKey key );

    /**
     * Get the cached representation of the node as represented by the supplied child reference. This is a convenience method that
     * is equivalent to calling:
     * 
     * <pre>
     * getNode(reference.getKey());
     * </pre>
     * 
     * @param reference the child node reference; may not be null
     * @return the cached node to which the reference points, or null if the child reference no longer points to a valid node
     */
    CachedNode getNode( ChildReference reference );
}

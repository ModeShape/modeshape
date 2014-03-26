/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.cache;

import java.util.Iterator;

/**
 * 
 */
public interface NodeCache extends CachedNodeSupplier {

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

    /**
     * Get an iterator over all node keys within this cache. The order of the keys is not defined.
     * 
     * @return an iterator over all the cache's node keys; never null
     */
    Iterator<NodeKey> getAllNodeKeys();

    /**
     * Get an iterator over all keys for the supplied node and all its descendants. The order of the keys is not defined.
     * 
     * @param startingKey the key for the node to start; may not be null
     * @return an iterator over all the cache's node keys; never null
     */
    Iterator<NodeKey> getAllNodeKeysAtAndBelow( NodeKey startingKey );

    /**
     * Unwrap this instance.
     * 
     * @return the unwrapped SessionCache instance, or this object if there is no wrapper.
     */
    NodeCache unwrap();
}

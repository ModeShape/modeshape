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
package org.modeshape.jcr.cache.document;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;

/**
 * An iterator that returns all of the keys for the nodes in the cache that are below the specified starting node.
 */
public class NodeCacheIterator implements Iterator<NodeKey> {

    private final Queue<NodeKey> keys = new LinkedList<NodeKey>();
    private final NodeCache cache;
    private final NodeFilter filter;
    private final NodeKey startingNode;
    private NodeKey nextNode;

    /**
     * Create a new iterator over the nodes in the supplied node cache that are at or below the supplied starting node.
     * 
     * @param cache the node cache; may not be null
     * @param startingNode the starting node and the root of the subgraph; may not be null
     */
    public NodeCacheIterator( NodeCache cache,
                              NodeKey startingNode ) {
        this(cache, startingNode, null);
    }

    /**
     * Create a new iterator over the nodes in the supplied node cache that are at or below the supplied starting node.
     * 
     * @param cache the node cache; may not be null
     * @param startingNode the starting node and the root of the subgraph; may not be null
     * @param filter the filter that should be used to determine which nodes are exposed by this iterator; may be null if the
     *        iterator should not filter
     */
    public NodeCacheIterator( NodeCache cache,
                              NodeKey startingNode,
                              NodeFilter filter ) {
        CheckArg.isNotNull(cache, "cache");
        CheckArg.isNotNull(startingNode, "startingNode");
        this.cache = cache;
        this.startingNode = startingNode;
        this.keys.add(startingNode);
        this.filter = filter;
    }

    @Override
    public final boolean hasNext() {
        nextNode();
        return nextNode != null;
    }

    @Override
    public final NodeKey next() {
        if (nextNode == null) {
            // May be successive calls to 'next()' ...
            nextNode();
            if (nextNode == null) {
                // Still didn't find one, so we're at the end ...
                throw new NoSuchElementException();
            }
        }
        try {
            return nextNode;
        } finally {
            nextNode = null;
        }
    }

    protected final void nextNode() {
        if (this.nextNode != null) return;
        while (true) {
            // Pop the next key off the queue ...
            NodeKey nextKey = keys.poll();
            if (nextKey == null) {
                // We're finished ...
                this.nextNode = null;
                return;
            }

            // Find the next node ...
            CachedNode node = cache.getNode(nextKey);
            if (node == null) {
                // skip this node ...
                continue;
            }
            if (filter != null && !filter.includeNode(node, cache)) {
                // the node should not be included by the filter but check if any of its children should be
                if (filter.continueProcessingChildren(node, cache)) {
                    nextKey = null;
                } else {
                    // this node is excluded by the filter, so skip it ...
                    continue;
                }
            }
            // Add all of the children onto the queue ...
            Iterator<NodeKey> iter = node.getChildReferences(cache).getAllKeys();
            while (iter.hasNext()) {
                keys.add(iter.next());
            }
            // Set the nextNode ref and return if we have a valid node...
            this.nextNode = nextKey;
            if (nextKey != null) {
                return;
            }
        }
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(nodes from ").append(cache);
        // Compute the path ...
        sb.append(" under ").append(cache.getNode(startingNode).getPath(cache));
        if (filter != null) sb.append(" satisfying ").append(filter);
        sb.append(")");
        return sb.toString();
    }

    public static interface NodeFilter {
        /**
         * Determine if the supplied node is to be included in the iterator. If this method returns false, the code should
         * use the {@link org.modeshape.jcr.cache.document.NodeCacheIterator.NodeFilter#continueProcessingChildren(CachedNode, NodeCache)}
         * method to determine if any of the children of the node should be processed.
         * By default, this method always returns true.
         * 
         * @param node the node; never null
         * @param cache the node cache; never null
         * @return true if the node is to be included; or false otherwise
         */
        public boolean includeNode( CachedNode node,
                                    NodeCache cache );
        
        /**
         * Determine if for a given node that isn't included by 
         * {@link org.modeshape.jcr.cache.document.NodeCacheIterator.NodeFilter#includeNode(CachedNode, NodeCache)}, its children 
         * should be processed or not.
         *                                                                
         * @return {@code true} if any children of an excluded node should be taken into account, {@code false} otherwise.
         */
        public boolean continueProcessingChildren(CachedNode node,
                                                  NodeCache cache);
    }
}

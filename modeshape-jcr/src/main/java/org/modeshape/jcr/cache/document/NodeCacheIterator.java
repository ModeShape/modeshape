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
public final class NodeCacheIterator implements Iterator<NodeKey> {

    private final Queue<NodeKey> keys = new LinkedList<NodeKey>();
    private final NodeCache cache;
    private NodeKey nextNode;

    public NodeCacheIterator( NodeCache cache,
                              NodeKey startingNode ) {
        CheckArg.isNotNull(cache, "cache");
        CheckArg.isNotNull(startingNode, "startingNode");
        this.cache = cache;
        this.keys.add(startingNode);
    }

    @Override
    public boolean hasNext() {
        nextNode();
        return nextNode != null;
    }

    @Override
    public NodeKey next() {
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

    protected void nextNode() {
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
            // Add all of the children onto the queue ...
            Iterator<NodeKey> iter = node.getChildReferences(cache).getAllKeys();
            while (iter.hasNext()) {
                keys.add(iter.next());
            }
            // Set the nextNode ref and return ...
            this.nextNode = nextKey;
            return;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

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

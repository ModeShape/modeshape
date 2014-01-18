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
package org.modeshape.jcr;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.util.CheckArg;

/**
 * A concrete {@link NodeIterator} that returns the supplied nodes.
 */
class JcrNodeListIterator implements NodeIterator {

    private final long size;
    private final Iterator<? extends Node> nodes;
    private Node nextNode;
    private long position = 0L;

    protected JcrNodeListIterator( Iterator<? extends Node> nodeIter,
                                   long size ) {
        this.nodes = nodeIter;
        this.size = size;
    }

    @Override
    public Node nextNode() {
        if (hasNext()) {
            assert nextNode != null;
            try {
                ++position;
                return nextNode;
            } finally {
                nextNode = null;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void skip( long skipNum ) {
        CheckArg.isNonNegative(skipNum, "skipNum");
        if (skipNum == 0L) return;
        for (long i = 0; i != skipNum; i++) {
            if (!hasNext()) return;
            next();
        }
    }

    @Override
    public boolean hasNext() {
        while (nextNode == null && nodes.hasNext()) {
            nextNode = nodes.next();
        }
        return nextNode != null;
    }

    @Override
    public Object next() {
        return nextNode();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

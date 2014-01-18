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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.cache.NodeKey;

/**
 * A concrete {@link NodeIterator} that returns the nodes in the session given the supplied keys and optional expected node type.
 */
class JcrNodeIterator implements NodeIterator {

    private final long size;
    private final Iterator<NodeKey> keyIter;
    private final JcrSession session;
    private final Type expectedType;
    private Node nextNode;
    private long position = 0L;

    protected JcrNodeIterator( JcrSession session,
                               Iterator<NodeKey> iter,
                               long size,
                               Type expectedType ) {
        this.session = session;
        this.keyIter = iter;
        this.size = size;
        this.expectedType = expectedType; // may be null
        assert session != null;
        assert session != null;
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
        while (nextNode == null && keyIter.hasNext()) {
            NodeKey key = keyIter.next();
            try {
                nextNode = session.node(key, expectedType);
            } catch (ItemNotFoundException e) {
                // Might have been removed from the session, so just skip this ...
            }
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

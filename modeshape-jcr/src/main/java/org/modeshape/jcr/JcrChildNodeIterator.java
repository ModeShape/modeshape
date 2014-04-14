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
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;

/**
 * A concrete {@link NodeIterator} implementation for children. Where possible, the creator should pass in the size. However, if
 * it is not known, the size is computed by this iterator only when needed.
 */
@NotThreadSafe
final class JcrChildNodeIterator implements NodeIterator {

    protected static interface NodeResolver {
        public Node nodeFrom( ChildReference ref );
    }

    private final NodeResolver resolver;
    private final Iterator<ChildReference> iterator;
    private Node resolvedNode;
    private Iterator<Node> nodeIterator;
    private int ndx;
    private long size;

    JcrChildNodeIterator( NodeResolver resolver,
                          Iterator<ChildReference> iterator ) {
        this.resolver = resolver;
        this.iterator = iterator;
        this.size = -1L; // we'll calculate if needed
    }

    JcrChildNodeIterator( NodeResolver resolver,
                          ChildReferences childReferences ) {
        assert size >= 0L;
        this.resolver = resolver;
        this.iterator = childReferences.iterator();
        this.size = childReferences.size();
    }

    @Override
    public long getPosition() {
        return ndx;
    }

    @Override
    public long getSize() {
        if (size > -1L) return size;
        if (!hasNext()) {
            // There are no more, so return the number of nodes we've already seen ...
            size = resolvedNode == null ? ndx : ndx + 1;
            return size;
        }
        // Otherwise, we have to iterate through the remaining iterator and keep the results ...
        List<Node> remainingNodes = new LinkedList<>();
        if (resolvedNode != null) {
            //we've already looked ahead once, so take that into account
            size = ndx + 1;
            remainingNodes.add(resolvedNode);
            resolvedNode = null;
        } else {
            size = ndx;
        }

        while (iterator.hasNext()) {
            Node node = resolver.nodeFrom(iterator.next());
            if (node != null) {
                remainingNodes.add(node);
                ++size;
            }
        }
        nodeIterator = remainingNodes.iterator();
        return size;
    }

    @Override
    public boolean hasNext() {
        if (nodeIterator != null) {
            return nodeIterator.hasNext();
        }
        //we need to look ahead in the child reference iterator, because the resolver might not return a node
        while (iterator.hasNext() && resolvedNode == null) {
            ChildReference ref = iterator.next();
            resolvedNode = resolver.nodeFrom(ref);
        }
        return resolvedNode != null;
    }

    @Override
    public Object next() {
        return nextNode();
    }

    @Override
    public Node nextNode() {
        if (nodeIterator != null) {
            return nodeIterator.next();
        }
        Node child = null;
        if (resolvedNode == null) {
            do {
                ChildReference childRef = iterator.next();
                child = resolver.nodeFrom(childRef);
            } while (child == null);
        } else {
            child = resolvedNode;
            resolvedNode = null;
        }
        ndx++;
        return child;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void skip( long count ) {
        CheckArg.isNonNegative(count, "count");
        while (--count >= 0) {
            nextNode();
        }
    }
}

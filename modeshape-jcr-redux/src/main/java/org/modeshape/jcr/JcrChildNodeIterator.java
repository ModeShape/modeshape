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
package org.modeshape.jcr;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
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

    private final JcrSession session;
    private final Iterator<ChildReference> iterator;
    private Iterator<Node> nodeIterator;
    private int ndx;
    private long size;

    JcrChildNodeIterator( JcrSession session,
                          Iterator<ChildReference> iterator ) {
        this.session = session;
        this.iterator = iterator;
        this.size = -1L; // we'll calculate if needed
    }

    JcrChildNodeIterator( JcrSession session,
                          ChildReferences childReferences ) {
        assert size >= 0L;
        this.session = session;
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
            size = ndx;
            return size;
        }
        // Otherwise, we have to iterate through the remaining iterator and keep the results ...
        List<Node> remainingNodes = new LinkedList<Node>();
        size = ndx;
        while (iterator.hasNext()) {
            Node node = nodeFrom(iterator.next());
            if (node != null) {
                remainingNodes.add(node);
                ++size;
            }
        }
        nodeIterator = remainingNodes.iterator();
        return size;
    }

    protected final Node nodeFrom( ChildReference ref ) {
        try {
            return session.node(ref.getKey(), null);
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        return nodeIterator != null ? nodeIterator.hasNext() : iterator.hasNext();
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
        do {
            child = nodeFrom(iterator.next());
        } while (child == null);
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

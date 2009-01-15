/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;

/**
 * @author jverhaeg
 */
final class JcrNodeIterator implements NodeIterator {

    private final Node parent;
    private final Iterator<Name> childIterator;
    private final Iterator<Integer> childNameCountIterator;
    private transient Name child;
    private transient int childNameCount;
    private transient int childNdx = 1;
    private transient int ndx;
    private transient Node node;

    JcrNodeIterator( Node parent,
                     List<Name> children,
                     List<Integer> childNameCounts ) {
        assert parent != null;
        this.parent = parent;
        childIterator = (children == null ? null : children.iterator());
        childNameCountIterator = (childNameCounts == null ? null : childNameCounts.iterator());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.RangeIterator#getPosition()
     */
    public long getPosition() {
        return ndx;
    }

    /**
     * {@inheritDoc}
     * 
     * @return -1L
     * @see javax.jcr.RangeIterator#getSize()
     */
    public long getSize() {
        return -1L;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return ((childIterator != null && childIterator.hasNext()) || (child != null && childNdx <= childNameCount));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Iterator#next()
     */
    public Object next() {
        return nextNode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NodeIterator#nextNode()
     */
    public Node nextNode() {
        if (childIterator == null) {
            throw new NoSuchElementException();
        }
        if (child == null || childNdx > childNameCount) {
            child = childIterator.next();
            childNameCount = childNameCountIterator.next();
            childNdx = 1;
        }
        try {
            node = parent.getNode(child.getString(((JcrSession)parent.getSession()).getExecutionContext().getNamespaceRegistry())
                                  + '[' + childNdx + ']');
            childNdx++;
            ndx++;
            return node;
        } catch (RepositoryException error) {
            // TODO: Change to DnaException once DNA-180 is addressed
            throw new RuntimeException(error);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>count</code> is negative.
     * @see javax.jcr.RangeIterator#skip(long)
     */
    public void skip( long count ) {
        CheckArg.isNonNegative(count, "count");
        while (--count >= 0) {
            nextNode();
        }
    }
}

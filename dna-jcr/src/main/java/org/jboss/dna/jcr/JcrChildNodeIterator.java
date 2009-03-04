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
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;

/**
 * @author jverhaeg
 */
@Immutable
final class JcrChildNodeIterator implements NodeIterator {

    private final NamespaceRegistry registry;
    private final Node parent;
    private final Iterator<Path.Segment> iterator;
    private int ndx;
    private int size;

    JcrChildNodeIterator( Node parent,
                          NamespaceRegistry registry,
                          List<Path.Segment> children ) {
        assert parent != null;
        assert registry != null;
        assert children != null;
        this.registry = registry;
        this.parent = parent;
        iterator = children.iterator();
        size = children.size();
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
     * @see javax.jcr.RangeIterator#getSize()
     */
    public long getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return iterator.hasNext();
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
        Path.Segment childSegment = iterator.next();
        ndx++;
        String childName = childSegment.getString(registry);
        try {
            return parent.getNode(childName);
        } catch (RepositoryException error) {
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

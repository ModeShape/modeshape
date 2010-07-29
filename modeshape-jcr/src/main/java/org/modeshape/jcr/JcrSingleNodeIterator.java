/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.modeshape.common.util.CheckArg;

/**
 * A concrete {@link NodeIterator} that delegates to the supplied iterator.
 */
class JcrSingleNodeIterator implements NodeIterator {

    private final long size;
    private final Iterator<AbstractJcrNode> iterator;

    protected JcrSingleNodeIterator( AbstractJcrNode singleNode ) {
        assert singleNode != null;
        this.iterator = Collections.singleton(singleNode).iterator();
        this.size = 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NodeIterator#nextNode()
     */
    public Node nextNode() {
        return this.iterator.next();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.RangeIterator#getPosition()
     */
    public long getPosition() {
        return 0;
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
     * @see javax.jcr.RangeIterator#skip(long)
     */
    public void skip( long skipNum ) {
        CheckArg.isNonNegative(skipNum, "skipNum");
        if (skipNum == 0L) return;
        throw new NoSuchElementException();
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
        return iterator.next();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

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
import java.util.Set;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.jboss.dna.common.util.CheckArg;

/**
 * @author jverhaeg
 */
final class JcrPropertyIterator implements PropertyIterator {

    private final Iterator<Property> iterator;
    private int ndx;

    JcrPropertyIterator( Set<Property> properties ) {
        assert properties != null;
        iterator = properties.iterator();
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
        return iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.Iterator#next()
     */
    public Object next() {
        return nextProperty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.PropertyIterator#nextProperty()
     */
    public Property nextProperty() {
        Property property = iterator.next();
        ndx++;
        return property;
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
            nextProperty();
        }
    }
}

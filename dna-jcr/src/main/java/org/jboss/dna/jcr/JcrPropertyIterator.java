/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import org.jboss.dna.common.util.ArgCheck;

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
        ArgCheck.isNonNegative(count, "count");
        while (--count >= 0) {
            nextProperty();
        }
    }
}

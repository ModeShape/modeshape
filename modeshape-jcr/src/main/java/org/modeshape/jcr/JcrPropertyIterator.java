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

import java.util.Collection;
import java.util.Iterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;

/**
 * ModeShape implementation of a {@link PropertyIterator}.
 */
@NotThreadSafe
final class JcrPropertyIterator implements PropertyIterator {

    private final Iterator<? extends Property> iterator;
    private int ndx;
    private int size;

    JcrPropertyIterator( Collection<? extends Property> properties ) {
        assert properties != null;
        iterator = properties.iterator();
        size = properties.size();
    }

    @Override
    public long getPosition() {
        return ndx;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object next() {
        return nextProperty();
    }

    @Override
    public Property nextProperty() {
        Property next = iterator.next();
        ndx++;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void skip( long count ) {
        CheckArg.isNonNegative(count, "count");
        while (--count >= 0) {
            nextProperty();
        }
    }
}

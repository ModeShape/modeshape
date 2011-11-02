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
package org.modeshape.jcr.cache.document;

import java.util.Iterator;

/**
 * An iterator that presents the union of two iterators. The second iterator is retrieved only when needed.
 * 
 * @param <Type> the value type of the iterator
 */
public class UnionIterator<Type> implements Iterator<Type> {

    private final Iterable<Type> next;
    private Iterator<Type> current;
    private boolean useNext;

    /**
     * Create a union iterator
     * 
     * @param firstIterator the first iterator; may not be null
     * @param next the Iterable from which the second iterator is to be obtained; may be null
     */
    public UnionIterator( Iterator<Type> firstIterator,
                          Iterable<Type> next ) {
        this.next = next;
        this.current = firstIterator;
        this.useNext = this.next != null;
    }

    @Override
    public boolean hasNext() {
        boolean nextValue = current.hasNext();
        if (!nextValue && useNext) {
            current = next.iterator();
            useNext = false;
            nextValue = current.hasNext();
        }
        return nextValue;
    }

    @Override
    public Type next() {
        return current.next();
    }

    @Override
    public void remove() {
        current.remove();
    }
}

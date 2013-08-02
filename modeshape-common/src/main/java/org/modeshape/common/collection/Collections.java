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
package org.modeshape.common.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A set of utilities for more easily creating various kinds of collections.
 */
public class Collections {

    public static <T> Set<T> unmodifiableSet( T... values ) {
        return unmodifiableSet(Arrays.asList(values));
    }

    public static <T> Set<T> unmodifiableSet( Collection<T> values ) {
        return java.util.Collections.unmodifiableSet(new HashSet<T>(values));
    }

    public static <T> Set<T> unmodifiableSet( Set<T> values ) {
        return java.util.Collections.unmodifiableSet(values);
    }

    /**
     * Concatenate two Iterable sources
     * 
     * @param a a non-null Iterable value
     * @param b a non-null Iterable value
     * @return an Iterable that will iterate through all the values from 'a' and then all the values from 'b'
     * @param <T> the value type
     */
    public static <T> Iterable<T> concat( final Iterable<T> a,
                                          final Iterable<T> b ) {
        assert (a != null);
        assert (b != null);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Collections.concat(a.iterator(), b.iterator());
            }
        };

    }

    /**
     * Concatenate two Iterators
     * 
     * @param a a non-null Iterator
     * @param b a non-null Iterator
     * @return an Iterator that will iterate through all the values of 'a', and then all the values of 'b'
     * @param <T> the value type
     */
    public static <T> Iterator<T> concat( final Iterator<T> a,
                                          final Iterator<T> b ) {
        assert (a != null);
        assert (b != null);

        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public T next() {
                if (a.hasNext()) {
                    return a.next();
                }

                return b.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

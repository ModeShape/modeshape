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

    @SafeVarargs
    public static <T> Set<T> unmodifiableSet(T... values ) {
        return unmodifiableSet(Arrays.asList(values));
    }

    public static <T> Set<T> unmodifiableSet( Collection<T> values ) {
        return java.util.Collections.unmodifiableSet(new HashSet<>(values));
    }

    public static <T> Set<T> unmodifiableSet( Set<T> values ) {
        return java.util.Collections.unmodifiableSet(values);
    }

    private Collections() {
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
        return () -> Collections.concat(a.iterator(), b.iterator());

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

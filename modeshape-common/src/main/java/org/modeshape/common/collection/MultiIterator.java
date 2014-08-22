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

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.NotThreadSafe;

/**
 * An iterator implementation that wraps multiple other iterators.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <E> the value type
 */
@NotThreadSafe
public final class MultiIterator<E> implements Iterator<E> {

    public static <E> MultiIterator<E> fromIterators( Iterable<Iterator<E>> iterators ) {
        return new MultiIterator<>(iterators);
    }

    public static <E> MultiIterator<E> fromIterables( final Iterable<? extends Iterable<E>> iterables ) {
        final Iterator<? extends Iterable<E>> iterator = iterables.iterator();
        Iterable<Iterator<E>> iterators = new Iterable<Iterator<E>>() {
            @Override
            public Iterator<Iterator<E>> iterator() {
                return new Iterator<Iterator<E>>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Iterator<E> next() {
                        return iterator.next().iterator();
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
        return new MultiIterator<>(iterators);
    }

    private final Iterator<Iterator<E>> iterators;
    private Iterator<E> current;

    protected MultiIterator( Iterable<Iterator<E>> iterators ) {
        this.iterators = iterators.iterator();
    }

    @Override
    public boolean hasNext() {
        if (nextIterator()) return current.hasNext();
        return false;
    }

    @Override
    public E next() {
        if (nextIterator()) return current.next();
        throw new NoSuchElementException();
    }

    protected boolean nextIterator() {
        while (current == null || !current.hasNext()) {
            // Find the next iterator ...
            if (!iterators.hasNext()) return false;
            current = iterators.next();
        }
        return true;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

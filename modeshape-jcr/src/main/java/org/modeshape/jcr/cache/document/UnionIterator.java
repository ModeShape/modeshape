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
package org.modeshape.jcr.cache.document;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
        if (current.hasNext()) {
            // At least one more on the current iterator ...
            return current.next();
        }
        // The current iterator has no more ...
        if (useNext) {
            // We have another iterator ...
            current = next.iterator();
            useNext = false;
            return current.next();
        }
        // No more iterators ...
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        current.remove();
    }
}

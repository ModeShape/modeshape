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

/**
 * An iterator that abstracts iterating over two other iterators.
 * 
 * @param <T> the type
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class SequentialIterator<T> implements Iterator<T> {

    public static <T> SequentialIterator<T> create( Iterator<T> first,
                                                    Iterator<T> second ) {
        return new SequentialIterator<T>(first, second);
    }

    private final Iterator<T> first;
    private final Iterator<T> second;
    private boolean completedFirst = false;

    public SequentialIterator( Iterator<T> first,
                               Iterator<T> second ) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean hasNext() {
        if (!completedFirst) {
            if (first.hasNext()) return true;
            completedFirst = true;
        }
        return second.hasNext();
    }

    @Override
    public T next() {
        if (!completedFirst) {
            if (first.hasNext()) return first.next();
            completedFirst = true;
        }
        return second.next();
    }

    @Override
    public void remove() {
        if (!completedFirst) {
            first.remove();
        }
        second.remove();
    }

}

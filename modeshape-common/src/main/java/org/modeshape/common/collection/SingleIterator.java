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
 * An {@link Iterator} that is used to iterate over a single, fixed value.
 * 
 * @param <T> the value type
 */
@NotThreadSafe
public class SingleIterator<T> implements Iterator<T> {
    private T value;

    public SingleIterator( T value ) {
        this.value = value;
    }

    @Override
    public boolean hasNext() {
        return value != null;
    }

    @Override
    public T next() {
        if (value == null) throw new NoSuchElementException();
        T next = value;
        value = null;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

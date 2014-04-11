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
 * An {@link Iterator} implementation that only allows reading elements, used as a wrapper around another iterator to make the
 * contents immutable to the user of this iterator.
 * 
 * @param <T> the type of the elements over which the iteration is being performed
 */
public final class ReadOnlyIterator<T> implements Iterator<T> {

    public static <T> ReadOnlyIterator<T> around( Iterator<T> delegate ) {
        return new ReadOnlyIterator<T>(delegate);
    }

    private final Iterator<T> delegate;

    public ReadOnlyIterator( Iterator<T> delegate ) {
        this.delegate = delegate;
        assert this.delegate != null;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        return delegate.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

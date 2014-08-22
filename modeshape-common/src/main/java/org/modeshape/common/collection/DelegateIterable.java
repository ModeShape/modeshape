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
import org.modeshape.common.function.Function;

/**
 * An iterable that adapts the values returned by a delegate iterable.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the iterator's type
 * @param <V> the type of the delegate iterator
 */
public class DelegateIterable<T, V> implements Iterable<T> {

    public static <T, V> Iterable<T> around( Iterable<V> delegate,
                                             Function<V, T> converter ) {
        return new DelegateIterable<T, V>(delegate, converter);
    }

    private final Function<V, T> converter;
    private final Iterable<V> delegate;

    protected DelegateIterable( Iterable<V> delegate,
                                Function<V, T> converter ) {
        this.converter = converter;
        this.delegate = delegate;
    }

    @Override
    public Iterator<T> iterator() {
        return DelegateIterator.around(delegate.iterator(), converter);
    }
}

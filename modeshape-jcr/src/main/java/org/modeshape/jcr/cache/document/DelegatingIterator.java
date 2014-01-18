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

/**
 * @param <Type> the value type of the iterator
 */
class DelegatingIterator<Type> implements Iterator<Type> {

    private final Iterator<Type> delegate;

    protected DelegatingIterator( Iterator<Type> delegate ) {
        this.delegate = delegate;
        assert this.delegate != null;
    }

    @Override
    public boolean hasNext() {
        return this.delegate.hasNext();
    }

    @Override
    public Type next() {
        return this.delegate.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

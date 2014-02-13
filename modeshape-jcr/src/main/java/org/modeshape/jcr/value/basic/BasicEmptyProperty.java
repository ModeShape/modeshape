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
package org.modeshape.jcr.value.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;

/**
 * An immutable version of a property that has no values. This is done for efficiency of the in-memory representation, since many
 * properties will have just a single value, while others will have multiple values.
 */
@Immutable
public class BasicEmptyProperty extends BasicProperty {
    private static final long serialVersionUID = 1L;
    private static final Iterator<Object> SHARED_ITERATOR = new EmptyIterator<Object>();

    /**
     * Create a property with no values.
     * 
     * @param name the property name
     */
    public BasicEmptyProperty( Name name ) {
        super(name);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isMultiple() {
        return false;
    }

    @Override
    public boolean isSingle() {
        return false;
    }
    
    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isSimpleReference() {
        return false;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object getFirstValue() {
        return null;
    }

    @Override
    public Iterator<Object> iterator() {
        return SHARED_ITERATOR;
    }

    @Override
    public Object getValue( int index ) throws IndexOutOfBoundsException {
        return null;
    }

    protected static class EmptyIterator<T> implements Iterator<T> {

        protected EmptyIterator() {
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}

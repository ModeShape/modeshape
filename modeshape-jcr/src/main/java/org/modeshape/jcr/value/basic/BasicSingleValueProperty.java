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
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Reference;

/**
 * An immutable version of a property that has exactly 1 value. This is done for efficiency of the in-memory representation, since
 * many properties will have just a single value, while others will have multiple values.
 */
@Immutable
public class BasicSingleValueProperty extends BasicProperty {
    private static final long serialVersionUID = 1L;

    protected final Object value;

    /**
     * Create a property with a single value
     * 
     * @param name the property name
     * @param value the property value (which may be null)
     */
    public BasicSingleValueProperty( Name name,
                                     Object value ) {
        super(name);
        this.value = value;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isMultiple() {
        return false;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean isReference() {
        Object firstValue = getFirstValue();
        if (firstValue instanceof NodeKeyReference) {
            // simple references are not recognized as legitimate references
            return !((NodeKeyReference)firstValue).isSimple();
        }
        return firstValue instanceof Reference;
    }

    @Override
    public boolean isSimpleReference() {
        Object firstValue = getFirstValue();
        return firstValue instanceof Reference && ((Reference)firstValue).isSimple();
    }

    @Override
    public boolean isBinary() {
        return getFirstValue() instanceof Binary;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Object getFirstValue() {
        return value;
    }

    @Override
    public Iterator<Object> iterator() {
        return new ValueIterator();
    }

    @Override
    public Object getValue( int index ) throws IndexOutOfBoundsException {
        if (index == 0) {
            return getFirstValue();
        }
        throw new IndexOutOfBoundsException(JcrI18n.indexOutsidePropertyValuesBoundaries.text(index, size()));
    }

    protected class ValueIterator implements Iterator<Object> {

        private boolean done = false;

        protected ValueIterator() {
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public Object next() {
            if (!done) {
                done = true;
                return BasicSingleValueProperty.this.value;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}

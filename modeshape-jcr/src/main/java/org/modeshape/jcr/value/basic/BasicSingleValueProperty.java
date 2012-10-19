/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;

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
        return getFirstValue() instanceof NodeKeyReference;
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

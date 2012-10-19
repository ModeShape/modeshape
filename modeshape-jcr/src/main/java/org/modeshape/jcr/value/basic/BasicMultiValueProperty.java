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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.Name;

/**
 * An immutable version of a property that has 2 or more values. This is done for efficiency of the in-memory representation,
 * since many properties will have just a single value, while others will have multiple values.
 */
@Immutable
public class BasicMultiValueProperty extends BasicProperty {
    private static final long serialVersionUID = 1L;

    private final List<Object> values;

    /**
     * Create a property with 2 or more values. Note that the supplied list may be modifiable, as this object does not expose any
     * means for modifying the contents.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    List<Object> values ) {
        super(name);
        CheckArg.isNotNull(values, "values");
        this.values = values;
    }

    /**
     * Create a property with 2 or more values.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    Object... values ) {
        super(name);
        CheckArg.isNotNull(values, "values");
        CheckArg.hasSizeOfAtLeast(values, 2, "values");
        this.values = Arrays.asList(values);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean isMultiple() {
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isReference() {
        return getFirstValue() instanceof NodeKeyReference;
    }

    @Override
    public int size() {
        return values != null ? values.size() : 0;
    }

    @Override
    public Object getFirstValue() {
        return size() == 0 ? null : values.get(0);
    }

    @Override
    public Iterator<Object> iterator() {
        return new ReadOnlyIterator(values.iterator());
    }

    protected class ReadOnlyIterator implements Iterator<Object> {

        private final Iterator<Object> values;

        protected ReadOnlyIterator( Iterator<Object> values ) {
            assert values != null;
            this.values = values;
        }

        @Override
        public boolean hasNext() {
            return values.hasNext();
        }

        @Override
        public Object next() {
            return values.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

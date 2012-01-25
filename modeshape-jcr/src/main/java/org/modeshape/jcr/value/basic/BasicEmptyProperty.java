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

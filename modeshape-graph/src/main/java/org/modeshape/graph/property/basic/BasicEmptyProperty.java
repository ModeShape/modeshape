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
package org.modeshape.graph.property.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;

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

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingle() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Property#getFirstValue()
     */
    public Object getFirstValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Object> iterator() {
        return SHARED_ITERATOR;
    }

    protected static class EmptyIterator<T> implements Iterator<T> {

        protected EmptyIterator() {
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public T next() {
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}

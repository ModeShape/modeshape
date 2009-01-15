/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.property.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;

/**
 * An immutable version of a property that has exactly 1 value. This is done for efficiency of the in-memory representation, since
 * many properties will have just a single value, while others will have multiple values.
 * 
 * @author Randall Hauch
 */
@Immutable
public class BasicSingleValueProperty extends BasicProperty {

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

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return false;
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
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Property#getFirstValue()
     */
    public Object getFirstValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Object> iterator() {
        return new ValueIterator();
    }

    protected class ValueIterator implements Iterator<Object> {

        private boolean done = false;

        protected ValueIterator() {
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return !done;
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            if (!done) {
                done = true;
                return BasicSingleValueProperty.this.value;
            }
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

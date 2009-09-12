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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;

/**
 * An immutable version of a property that has 2 or more values. This is done for efficiency of the in-memory representation,
 * since many properties will have just a single value, while others will have multiple values.
 */
@Immutable
public class BasicMultiValueProperty extends BasicProperty {

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
        CheckArg.hasSizeOfAtLeast(values, 2, "values");
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
        return true;
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
        return values.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Property#getFirstValue()
     */
    public Object getFirstValue() {
        return values.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Object> iterator() {
        return new ReadOnlyIterator(values.iterator());
    }

    protected class ReadOnlyIterator implements Iterator<Object> {

        private final Iterator<Object> values;

        protected ReadOnlyIterator( Iterator<Object> values ) {
            assert values != null;
            this.values = values;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return values.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return values.next();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}

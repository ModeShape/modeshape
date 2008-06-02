/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Iterator;
import net.jcip.annotations.Immutable;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * @author Randall Hauch
 */
@Immutable
public abstract class BasicProperty implements Property {

    private final Name name;
    private final Name definitionName;
    private final PropertyType type;
    private final ValueFactories factories;

    /**
     * @param name
     * @param type
     * @param definitionName
     * @param valueFactories
     */
    public BasicProperty( Name name, PropertyType type, Name definitionName, ValueFactories valueFactories ) {
        this.name = name;
        this.type = type;
        this.definitionName = definitionName;
        this.factories = valueFactories;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Name getDefinitionName() {
        return definitionName;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyType getPropertyType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Binary> getBinaryValues() {
        return new ValueIterator<Binary>(factories.getBinaryFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Boolean> getBooleanValues() {
        return new ValueIterator<Boolean>(factories.getBooleanFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DateTime> getDateValues() {
        return new ValueIterator<DateTime>(factories.getDateFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<BigDecimal> getDecimalValues() {
        return new ValueIterator<BigDecimal>(factories.getDecimalFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Double> getDoubleValues() {
        return new ValueIterator<Double>(factories.getDoubleFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Long> getLongValues() {
        return new ValueIterator<Long>(factories.getLongFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Name> getNameValues() {
        return new ValueIterator<Name>(factories.getNameFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Path> getPathValues() {
        return new ValueIterator<Path>(factories.getPathFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Reference> getReferenceValues() {
        return new ValueIterator<Reference>(factories.getReferenceFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> getStringValues() {
        return new ValueIterator<String>(factories.getStringFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<URI> getUriValues() {
        return new ValueIterator<URI>(factories.getUriFactory(), iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<?> getValues() {
        return iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<?> getValues( PropertyType type ) {
        switch (type) {
            case BINARY:
                return getBinaryValues();
            case BOOLEAN:
                return getBooleanValues();
            case DATE:
                return getDateValues();
            case DECIMAL:
                return getDecimalValues();
            case DOUBLE:
                return getDoubleValues();
            case LONG:
                return getLongValues();
            case NAME:
                return getNameValues();
            case OBJECT:
                return getValues();
            case PATH:
                return getPathValues();
            case REFERENCE:
                return getReferenceValues();
            case STRING:
                return getStringValues();
            case URI:
                return getUriValues();
        }
        return getValues(); // should never get here ...
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getValuesAsArray() {
        if (size() == 0) return null;
        Object[] results = new Object[size()];
        Iterator<?> iter = iterator();
        int index = 0;
        while (iter.hasNext()) {
            Object value = iter.next();
            results[index++] = value;
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Property o ) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof Property) {
            Property that = (Property)obj;
            if (!this.getName().equals(that.getName())) return false;
            if (this.getPropertyType() != that.getPropertyType()) return false;
            if (this.size() != that.size()) return false;
            Iterator<?> thisIter = iterator();
            Iterator<?> thatIter = that.iterator();
            while (thisIter.hasNext()) { // && thatIter.hasNext()
                Object thisValue = thisIter.next();
                Object thatValue = thatIter.next();
                if (thisValue == null) {
                    if (thatValue != null) return false;
                    // else both are null
                } else {
                    if (!thisValue.equals(thatValue)) return false;
                }
            }
            return true;
        }
        return false;
    }

    protected class ValueIterator<T> implements Iterator<T> {

        private final ValueFactory<T> factory;
        private final Iterator<Object> values;

        protected ValueIterator( ValueFactory<T> factory, Iterator<Object> values ) {
            assert factory != null;
            assert values != null;
            this.factory = factory;
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
        public T next() {
            return factory.create(values.next());
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}

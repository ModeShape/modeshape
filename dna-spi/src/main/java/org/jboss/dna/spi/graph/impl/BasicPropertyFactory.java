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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * @author Randall Hauch
 */
public class BasicPropertyFactory implements PropertyFactory {

    private final ValueFactories factories;

    /**
     * @param valueFactories the value factories
     * @throws IllegalArgumentException if the reference to the value factories is null
     */
    public BasicPropertyFactory( ValueFactories valueFactories ) {
        ArgCheck.isNotNull(valueFactories, "value factories");
        this.factories = valueFactories;
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name, PropertyType type, Name definitionName, Object... values ) {
        ArgCheck.isNotNull(name, "name");
        ArgCheck.isNotNull(type, "type");
        if (values == null || values.length == 0) {
            return new BasicEmptyProperty(name, type, definitionName, this.factories);
        }
        final int len = values.length;
        final ValueFactory<?> factory = factories.getValueFactory(type);
        if (values.length == 1) {
            Object value = values[0];
            // Check whether the sole value was a collection ...
            if (value instanceof Collection) {
                // The single value is a collection, so create property with the collection's contents ...
                return create(name, type, definitionName, (Collection<?>)value);
            }
            value = factory.create(values[0]);
            return new BasicSingleValueProperty(name, type, definitionName, this.factories, value);
        }
        List<Object> valueList = new ArrayList<Object>(len);
        for (int i = 0; i != len; ++i) {
            Object value = factory.create(values[i]);
            valueList.add(value);
        }
        return new BasicMultiValueProperty(name, type, definitionName, this.factories, valueList);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    public Property create( Name name, PropertyType type, Name definitionName, Iterable<?> values ) {
        ArgCheck.isNotNull(name, "name");
        ArgCheck.isNotNull(type, "type");
        List<Object> valueList = null;
        if (values instanceof Collection) {
            Collection<Object> originalValues = (Collection<Object>)values;
            if (originalValues.isEmpty()) {
                return new BasicEmptyProperty(name, type, definitionName, this.factories);
            }
            valueList = new ArrayList<Object>(originalValues.size());
        } else {
            // We don't know the size
            valueList = new ArrayList<Object>();
        }
        // Copy the values, ensuring that the values are the correct type ...
        final ValueFactory<?> factory = factories.getValueFactory(type);
        for (Object value : values) {
            valueList.add(factory.create(value));
        }
        if (valueList.isEmpty()) { // may not have been a collection earlier
            return new BasicEmptyProperty(name, type, definitionName, this.factories);
        }
        if (valueList.size() == 1) {
            return new BasicSingleValueProperty(name, type, definitionName, this.factories, valueList.get(0));
        }
        return new BasicMultiValueProperty(name, type, definitionName, this.factories, valueList);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name, PropertyType type, Name definitionName, Iterator<?> values ) {
        ArgCheck.isNotNull(name, "name");
        ArgCheck.isNotNull(type, "type");
        final List<Object> valueList = new ArrayList<Object>();
        final ValueFactory<?> factory = factories.getValueFactory(type);
        while (values.hasNext()) {
            Object value = values.next();
            value = factory.create(value);
            valueList.add(value);
        }
        if (valueList.isEmpty()) {
            return new BasicEmptyProperty(name, type, definitionName, this.factories);
        }
        if (valueList.size() == 1) {
            return new BasicSingleValueProperty(name, type, definitionName, this.factories, valueList.get(0));
        }
        return new BasicMultiValueProperty(name, type, definitionName, this.factories, valueList);
    }

}

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
package org.jboss.dna.graph.properties.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.PropertyType;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.ValueFactory;

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
        CheckArg.isNotNull(valueFactories, "value factories");
        this.factories = valueFactories;
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            Iterable<?> values ) {
        return create(name, PropertyType.OBJECT, values);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            Iterator<?> values ) {
        return create(name, PropertyType.OBJECT, values);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            Object... values ) {
        return create(name, PropertyType.OBJECT, values);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            PropertyType desiredType,
                            Object... values ) {
        CheckArg.isNotNull(name, "name");
        if (values == null || values.length == 0) {
            return new BasicEmptyProperty(name);
        }
        final int len = values.length;
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        if (values.length == 1) {
            Object value = values[0];
            // Check whether the sole value was a collection ...
            if (value instanceof Collection) {
                // The single value is a collection, so create property with the collection's contents ...
                return create(name, (Collection<?>)value);
            }
            value = factory.create(values[0]);
            return new BasicSingleValueProperty(name, value);
        }
        List<Object> valueList = new ArrayList<Object>(len);
        for (int i = 0; i != len; ++i) {
            Object value = factory.create(values[i]);
            valueList.add(value);
        }
        return new BasicMultiValueProperty(name, valueList);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    public Property create( Name name,
                            PropertyType desiredType,
                            Iterable<?> values ) {
        CheckArg.isNotNull(name, "name");
        List<Object> valueList = null;
        if (values instanceof Collection) {
            Collection<Object> originalValues = (Collection<Object>)values;
            if (originalValues.isEmpty()) {
                return new BasicEmptyProperty(name);
            }
            valueList = new ArrayList<Object>(originalValues.size());
        } else {
            // We don't know the size
            valueList = new ArrayList<Object>();
        }
        // Copy the values, ensuring that the values are the correct type ...
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        for (Object value : values) {
            valueList.add(factory.create(value));
        }
        if (valueList.isEmpty()) { // may not have been a collection earlier
            return new BasicEmptyProperty(name);
        }
        if (valueList.size() == 1) {
            return new BasicSingleValueProperty(name, valueList.get(0));
        }
        return new BasicMultiValueProperty(name, valueList);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            PropertyType desiredType,
                            Iterator<?> values ) {
        CheckArg.isNotNull(name, "name");
        final List<Object> valueList = new ArrayList<Object>();
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        while (values.hasNext()) {
            Object value = values.next();
            value = factory.create(value);
            valueList.add(value);
        }
        if (valueList.isEmpty()) {
            return new BasicEmptyProperty(name);
        }
        if (valueList.size() == 1) {
            return new BasicSingleValueProperty(name, valueList.get(0));
        }
        return new BasicMultiValueProperty(name, valueList);
    }

}

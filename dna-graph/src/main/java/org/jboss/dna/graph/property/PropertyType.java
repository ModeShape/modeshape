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
package org.jboss.dna.graph.property;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public enum PropertyType {

    STRING("String", ValueComparators.STRING_COMPARATOR, String.class),
    BINARY("Binary", ValueComparators.BINARY_COMPARATOR, Binary.class),
    LONG("Long", ValueComparators.LONG_COMPARATOR, Long.class),
    DOUBLE("Double", ValueComparators.DOUBLE_COMPARATOR, Double.class),
    DECIMAL("Decimal", ValueComparators.DECIMAL_COMPARATOR, BigDecimal.class),
    DATE("Date", ValueComparators.DATE_TIME_COMPARATOR, DateTime.class),
    BOOLEAN("Boolean", ValueComparators.BOOLEAN_COMPARATOR, Boolean.class),
    NAME("Name", ValueComparators.NAME_COMPARATOR, Name.class),
    PATH("Path", ValueComparators.PATH_COMPARATOR, Path.class),
    UUID("UUID", ValueComparators.UUID_COMPARATOR, UUID.class),
    REFERENCE("Reference", ValueComparators.REFERENCE_COMPARATOR, Reference.class),
    URI("URI", ValueComparators.URI_COMPARATOR, URI.class),
    OBJECT("Object", ValueComparators.OBJECT_COMPARATOR, Object.class);

    private static final List<PropertyType> ALL_PROPERTY_TYPES;
    static {
        List<PropertyType> types = new ArrayList<PropertyType>();
        for (PropertyType type : PropertyType.values()) {
            types.add(type);
        }
        ALL_PROPERTY_TYPES = Collections.unmodifiableList(types);
    }

    private final String name;
    private final Comparator<?> comparator;
    private final Class<?> valueClass;

    private PropertyType( String name,
                          Comparator<?> comparator,
                          Class<?> valueClass ) {
        this.name = name;
        this.comparator = comparator;
        this.valueClass = valueClass;
    }

    public Class<?> getValueClass() {
        return this.valueClass;
    }

    public String getName() {
        return this.name;
    }

    public Comparator<?> getComparator() {
        return this.comparator;
    }

    public boolean isTypeFor( Object value ) {
        return this.valueClass.isInstance(value);
    }

    public boolean isTypeForEach( Iterable<?> values ) {
        for (Object value : values) {
            if (!this.valueClass.isInstance(value)) return false;
        }
        return true;
    }

    public boolean isTypeForEach( Iterator<?> values ) {
        while (values.hasNext()) {
            Object value = values.next();
            if (!this.valueClass.isInstance(value)) return false;
        }
        return true;
    }

    public static PropertyType discoverType( Object value ) {
        if (value == null) {
            throw new IllegalArgumentException(GraphI18n.unableToDiscoverPropertyTypeForNullValue.text());
        }
        for (PropertyType type : PropertyType.values()) {
            if (type == OBJECT) continue;
            if (type.isTypeFor(value)) return type;
        }
        return OBJECT;
    }

    /**
     * Discover the most appropriate {@link PropertyType} whose values can be assigned to variables or parameters of the supplied
     * type. This method does check whether the supplied {@link Class} is an array, in which case it just evalutes the
     * {@link Class#getComponentType() component type} of the array.
     * 
     * @param clazz the class representing the type of a value or parameter; may not be null
     * @return the PropertyType that best represents the type whose values can be used as a value in the supplied class, or null
     *         if no matching PropertyType could be found
     */
    public static PropertyType discoverType( Class<?> clazz ) {
        CheckArg.isNotNull(clazz, "clazz");
        // Is the supplied class an array (or an array of arrays)?
        while (clazz.isArray()) {
            // Then just call extract the component type that of which we have an array ...
            clazz = clazz.getComponentType();
        }
        // Try each property type, and see if its value type is an exact match ...
        for (PropertyType type : PropertyType.values()) {
            if (type.valueClass.equals(clazz)) return type;
            // If the property type is capable of handling a primitive ...
            switch (type) {
                case LONG:
                    if (Long.TYPE.equals(clazz) || Integer.TYPE.equals(clazz) || Short.TYPE.equals(clazz)) return type;
                    if (Integer.class.equals(clazz) || Short.class.equals(clazz)) return type;
                    break;
                case DOUBLE:
                    if (Double.TYPE.equals(clazz) || Float.TYPE.equals(clazz)) return type;
                    if (Float.class.equals(clazz)) return type;
                    break;
                case BOOLEAN:
                    if (Boolean.TYPE.equals(clazz)) return type;
                    break;
                default:
                    break;
            }
        }
        // No value class of the property type matched exactly, so now see if any property type is assignable to 'clazz' ...
        for (PropertyType type : PropertyType.values()) {
            if (clazz.isAssignableFrom(type.valueClass)) return type;
        }
        // Nothing works, ...
        return null;
    }

    /**
     * Return an iterator over all the property type enumeration literals.
     * 
     * @return an immutable iterator
     */
    public static Iterator<PropertyType> iterator() {
        return ALL_PROPERTY_TYPES.iterator();
    }
}

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
package org.modeshape.graph.property;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;

/**
 * The data types for property values.
 */
@Immutable
public enum PropertyType {

    STRING("String", ValueComparators.STRING_COMPARATOR, new ObjectCanonicalizer(), String.class),
    BINARY("Binary", ValueComparators.BINARY_COMPARATOR, new ObjectCanonicalizer(), Binary.class),
    LONG("Long", ValueComparators.LONG_COMPARATOR, new LongCanonicalizer(), Long.class, Integer.class, Short.class),
    DOUBLE("Double", ValueComparators.DOUBLE_COMPARATOR, new DoubleCanonicalizer(), Double.class, Float.class),
    DECIMAL("Decimal", ValueComparators.DECIMAL_COMPARATOR, new ObjectCanonicalizer(), BigDecimal.class),
    DATE("Date", ValueComparators.DATE_TIME_COMPARATOR, new ObjectCanonicalizer(), DateTime.class),
    BOOLEAN("Boolean", ValueComparators.BOOLEAN_COMPARATOR, new ObjectCanonicalizer(), Boolean.class),
    NAME("Name", ValueComparators.NAME_COMPARATOR, new ObjectCanonicalizer(), Name.class),
    PATH("Path", ValueComparators.PATH_COMPARATOR, new ObjectCanonicalizer(), Path.class),
    UUID("UUID", ValueComparators.UUID_COMPARATOR, new ObjectCanonicalizer(), UUID.class),
    REFERENCE("Reference", ValueComparators.REFERENCE_COMPARATOR, new ObjectCanonicalizer(), Reference.class),
    WEAKREFERENCE("WeakReference", ValueComparators.REFERENCE_COMPARATOR, new ObjectCanonicalizer(), Reference.class),
    URI("URI", ValueComparators.URI_COMPARATOR, new ObjectCanonicalizer(), URI.class),
    OBJECT("Object", ValueComparators.OBJECT_COMPARATOR, new ObjectCanonicalizer(), Object.class);

    private static interface Canonicalizer {
        Object canonicalizeValue( Object value );
    }

    protected final static class ObjectCanonicalizer implements Canonicalizer {
        public Object canonicalizeValue( Object value ) {
            return value;
        }
    }

    protected final static class LongCanonicalizer implements Canonicalizer {
        public Object canonicalizeValue( Object value ) {
            if (value instanceof Integer) return new Long((Integer)value);
            if (value instanceof Short) return new Long((Short)value);
            return value;
        }
    }

    protected final static class DoubleCanonicalizer implements Canonicalizer {
        public Object canonicalizeValue( Object value ) {
            if (value instanceof Float) return new Double((Float)value);
            return value;
        }
    }

    private static final List<PropertyType> ALL_PROPERTY_TYPES;
    private static final Map<String, PropertyType> PROPERTY_TYPE_BY_LOWERCASE_NAME;
    static {
        List<PropertyType> types = new ArrayList<PropertyType>();
        Map<String, PropertyType> byLowerCaseName = new HashMap<String, PropertyType>();
        for (PropertyType type : PropertyType.values()) {
            types.add(type);
            byLowerCaseName.put(type.getName().toLowerCase(), type);
        }
        ALL_PROPERTY_TYPES = Collections.unmodifiableList(types);
        PROPERTY_TYPE_BY_LOWERCASE_NAME = Collections.unmodifiableMap(byLowerCaseName);
    }

    private final String name;
    private final Comparator<?> comparator;
    private final Canonicalizer canonicalizer;
    private final Class<?> valueClass;
    private final Set<Class<?>> castableValueClasses;

    private PropertyType( String name,
                          Comparator<?> comparator,
                          Canonicalizer canonicalizer,
                          Class<?> valueClass,
                          Class<?>... castableClasses ) {
        this.name = name;
        this.comparator = comparator;
        this.canonicalizer = canonicalizer;
        this.valueClass = valueClass;
        if (castableClasses != null && castableClasses.length != 0) {
            castableValueClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(castableClasses)));
        } else {
            castableValueClasses = Collections.emptySet();
        }
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

    /**
     * Obtain a value of this type in its canonical form. Some property types allow values to be instances of the canonical class
     * or an alternative class. This method ensures that the value is always an instance of the canonical class.
     * <p>
     * Note that this method does <i>not</i> cast from one property type to another.
     * </p>
     * 
     * @param value the property value
     * @return the value in canonical form
     */
    public Object getCanonicalValue( Object value ) {
        return this.canonicalizer.canonicalizeValue(value);
    }

    public final boolean isTypeFor( Object value ) {
        if (this.valueClass.isInstance(value)) return true;
        for (Class<?> valueClass : castableValueClasses) {
            if (valueClass.isInstance(value)) return true;
        }
        return false;
    }

    public final boolean isTypeForEach( Iterable<?> values ) {
        for (Object value : values) {
            if (!isTypeFor(value)) return false;
        }
        return true;
    }

    public final boolean isTypeForEach( Iterator<?> values ) {
        while (values.hasNext()) {
            Object value = values.next();
            if (!isTypeFor(value)) return false;
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

    public static PropertyType valueFor( String typeNameInAnyCase ) {
        PropertyType type = PROPERTY_TYPE_BY_LOWERCASE_NAME.get(typeNameInAnyCase);
        return type != null ? type : PropertyType.STRING;
    }
}

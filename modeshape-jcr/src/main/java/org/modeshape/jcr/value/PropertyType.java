/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.LinkedListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicPath;
import org.modeshape.jcr.value.basic.ChildPath;
import org.modeshape.jcr.value.basic.IdentifierPath;
import org.modeshape.jcr.value.basic.JodaDateTime;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.RootPath;
import org.modeshape.jcr.value.basic.StringReference;

/**
 * The data types for property values.
 */
@Immutable
public enum PropertyType {

    STRING("String", ValueComparators.STRING_COMPARATOR, new ObjectCanonicalizer(), String.class, javax.jcr.PropertyType.STRING),
    BINARY("Binary", ValueComparators.BINARY_COMPARATOR, new ObjectCanonicalizer(), BinaryValue.class,
           javax.jcr.PropertyType.BINARY),
    LONG("Long", ValueComparators.LONG_COMPARATOR, new LongCanonicalizer(), Long.class, javax.jcr.PropertyType.LONG,
         Integer.class, Short.class),
    DOUBLE("Double", ValueComparators.DOUBLE_COMPARATOR, new DoubleCanonicalizer(), Double.class, javax.jcr.PropertyType.DOUBLE,
           Float.class),
    DECIMAL("Decimal", ValueComparators.DECIMAL_COMPARATOR, new ObjectCanonicalizer(), BigDecimal.class,
            javax.jcr.PropertyType.DECIMAL),
    DATE("Date", ValueComparators.DATE_TIME_COMPARATOR, new DateCanonicalizer(), DateTime.class, javax.jcr.PropertyType.DATE,
         Calendar.class, Date.class),
    BOOLEAN("Boolean", ValueComparators.BOOLEAN_COMPARATOR, new ObjectCanonicalizer(), Boolean.class,
            javax.jcr.PropertyType.BOOLEAN),
    NAME("Name", ValueComparators.NAME_COMPARATOR, new ObjectCanonicalizer(), Name.class, javax.jcr.PropertyType.NAME,
         BasicName.class),
    PATH("Path", ValueComparators.PATH_COMPARATOR, new ObjectCanonicalizer(), Path.class, javax.jcr.PropertyType.PATH,
         BasicPath.class, ChildPath.class, IdentifierPath.class, RootPath.class),
    REFERENCE("Reference", ValueComparators.REFERENCE_COMPARATOR, new ObjectCanonicalizer(), Reference.class,
              javax.jcr.PropertyType.REFERENCE, NodeKeyReference.class, StringReference.class),
    WEAKREFERENCE("WeakReference", ValueComparators.REFERENCE_COMPARATOR, new ObjectCanonicalizer(), Reference.class,
                  javax.jcr.PropertyType.WEAKREFERENCE, NodeKeyReference.class, StringReference.class),
    SIMPLEREFERENCE(org.modeshape.jcr.api.PropertyType.TYPENAME_SIMPLE_REFERENCE, ValueComparators.REFERENCE_COMPARATOR,
                    new ObjectCanonicalizer(), Reference.class, org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE,
                    NodeKeyReference.class),
    URI("URI", ValueComparators.URI_COMPARATOR, new ObjectCanonicalizer(), URI.class, javax.jcr.PropertyType.URI),
    OBJECT("Object", ValueComparators.OBJECT_COMPARATOR, new ObjectCanonicalizer(), Object.class,
           javax.jcr.PropertyType.UNDEFINED);

    private static interface Canonicalizer {
        Object canonicalizeValue( Object value );
    }

    protected final static class ObjectCanonicalizer implements Canonicalizer {
        @Override
        public Object canonicalizeValue( Object value ) {
            return value;
        }
    }

    protected final static class LongCanonicalizer implements Canonicalizer {
        @Override
        public Object canonicalizeValue( Object value ) {
            if (value instanceof Integer) return new Long((Integer)value);
            if (value instanceof Short) return new Long((Short)value);
            return value;
        }
    }

    protected final static class DoubleCanonicalizer implements Canonicalizer {
        @Override
        public Object canonicalizeValue( Object value ) {
            if (value instanceof Float) return new Double((Float)value);
            return value;
        }
    }

    protected final static class DateCanonicalizer implements Canonicalizer {
        @Override
        public Object canonicalizeValue( Object value ) {
            if (value instanceof DateTime) return value;
            if (value instanceof Calendar) {
                return new JodaDateTime((Calendar)value);
            }
            if (value instanceof Date) {
                return new JodaDateTime((Date)value);
            }
            return value;
        }
    }

    private static interface TypeChecker {
        boolean isTypeFor( Object value );
    }

    protected final static class ClassBasedTypeChecker implements TypeChecker {
        private final Class<?> valueClass;

        protected ClassBasedTypeChecker( Class<?> valueClass ) {
            this.valueClass = valueClass;
            assert this.valueClass != null;
        }

        @Override
        public boolean isTypeFor( Object value ) {
            return this.valueClass.isInstance(value);
        }
    }

    private static final List<PropertyType> ALL_PROPERTY_TYPES;
    private static final Map<String, PropertyType> PROPERTY_TYPE_BY_LOWERCASE_NAME;
    private static final Multimap<Class<?>, PropertyType> PROPERTY_TYPES_BY_INSTANCE_CLASS;
    static {
        List<PropertyType> types = new ArrayList<PropertyType>();
        Map<String, PropertyType> byLowerCaseName = new HashMap<String, PropertyType>();
        Multimap<Class<?>, PropertyType> propTypesByClass = LinkedListMultimap.create();
        for (PropertyType type : PropertyType.values()) {
            types.add(type);
            byLowerCaseName.put(type.getName().toLowerCase(), type);
            for (Class<?> clazz : type.castableValueClasses) {
                propTypesByClass.put(clazz, type);
            }
            propTypesByClass.put(type.valueClass, type);
        }
        // Add the primitive classes ...
        propTypesByClass.put(Long.TYPE, PropertyType.LONG);
        propTypesByClass.put(Short.TYPE, PropertyType.LONG);
        propTypesByClass.put(Integer.TYPE, PropertyType.LONG);
        propTypesByClass.put(Double.TYPE, PropertyType.DOUBLE);
        propTypesByClass.put(Float.TYPE, PropertyType.DOUBLE);
        propTypesByClass.put(Boolean.TYPE, PropertyType.BOOLEAN);
        byLowerCaseName.put("undefined", PropertyType.OBJECT);
        ALL_PROPERTY_TYPES = Collections.unmodifiableList(types);
        PROPERTY_TYPE_BY_LOWERCASE_NAME = Collections.unmodifiableMap(byLowerCaseName);
        PROPERTY_TYPES_BY_INSTANCE_CLASS = propTypesByClass;
    }

    private final String name;
    private final Comparator<?> comparator;
    private final Canonicalizer canonicalizer;
    private final Class<?> valueClass;
    private final Set<Class<?>> castableValueClasses;
    private final TypeChecker typeChecker;
    private final int jcrType;

    private PropertyType( String name,
                          Comparator<?> comparator,
                          Canonicalizer canonicalizer,
                          Class<?> valueClass,
                          int jcrType,
                          Class<?>... castableClasses ) {
        this(name, comparator, canonicalizer, valueClass, null, jcrType, castableClasses);
    }

    private PropertyType( String name,
                          Comparator<?> comparator,
                          Canonicalizer canonicalizer,
                          Class<?> valueClass,
                          TypeChecker typeChecker,
                          int jcrType,
                          Class<?>... castableClasses ) {
        this.name = name;
        this.comparator = comparator;
        this.canonicalizer = canonicalizer;
        this.valueClass = valueClass;
        this.jcrType = jcrType;
        if (castableClasses != null && castableClasses.length != 0) {
            castableValueClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(castableClasses)));
        } else {
            castableValueClasses = Collections.emptySet();
        }
        this.typeChecker = typeChecker != null ? typeChecker : new ClassBasedTypeChecker(this.valueClass);
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
        // Does the value exactly comply with this type ...
        if (this.typeChecker.isTypeFor(value)) return true;
        // Is the value castable to this type ...
        for (Class<?> valueClass : castableValueClasses) {
            if (valueClass.isInstance(value)) return true;
        }
        return false;
    }

    public final boolean isTypeFor( Class<?> clazz ) {
        // Is the value castable to this type ...
        for (Class<?> valueClass : castableValueClasses) {
            if (valueClass.isAssignableFrom(clazz)) return true;
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

    /**
     * Returns the JCR numeric constant which represents the current type.
     *
     * @return an {@code int} value
     * @see javax.jcr.PropertyType
     */
    public final int jcrType() {
        return this.jcrType;
    }

    public static PropertyType discoverType( Object value ) {
        if (value == null) {
            throw new IllegalArgumentException(GraphI18n.unableToDiscoverPropertyTypeForNullValue.text());
        }
        PropertyType classBasedType = discoverType(value.getClass());
        if (classBasedType != null) {
            if (classBasedType == PropertyType.REFERENCE && value instanceof Reference) {
                Reference ref = (Reference)value;
                if (ref.isSimple()) {
                    return PropertyType.SIMPLEREFERENCE;
                }
                return ref.isWeak() ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
            }
            return classBasedType;
        }
        for (PropertyType type : PropertyType.values()) {
            if (type == OBJECT) continue;
            if (type.isTypeFor(value)) {
                return type;
            }
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

        // Check non-primitives ...
        Collection<PropertyType> types = PROPERTY_TYPES_BY_INSTANCE_CLASS.get(clazz);
        int num = types.size();
        if (num == 1) {
            return types.iterator().next();
        } else if (num > 1) {
            // Look at all of the types ...
            for (PropertyType aType : types) {
                if (aType.isTypeFor(clazz)) return aType;
            }
        }

        // No value class of the property type matched exactly, so now see if any property type is assignable to 'clazz' ...
        for (PropertyType aType : PropertyType.values()) {
            if (aType.isTypeFor(clazz)) return aType;
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
        PropertyType type = PROPERTY_TYPE_BY_LOWERCASE_NAME.get(typeNameInAnyCase.toLowerCase());
        return type != null ? type : PropertyType.STRING;
    }

    public static PropertyType valueFor( int jcrPropertyType ) {
        switch (jcrPropertyType) {
            case javax.jcr.PropertyType.BINARY:
                return PropertyType.BINARY;
            case javax.jcr.PropertyType.BOOLEAN:
                return PropertyType.BOOLEAN;
            case javax.jcr.PropertyType.DATE:
                return PropertyType.DATE;
            case javax.jcr.PropertyType.DECIMAL:
                return PropertyType.DECIMAL;
            case javax.jcr.PropertyType.DOUBLE:
                return PropertyType.DOUBLE;
            case javax.jcr.PropertyType.LONG:
                return PropertyType.LONG;
            case javax.jcr.PropertyType.NAME:
                return PropertyType.NAME;
            case javax.jcr.PropertyType.PATH:
                return PropertyType.PATH;
            case javax.jcr.PropertyType.REFERENCE:
                return PropertyType.REFERENCE;
            case javax.jcr.PropertyType.STRING:
                return PropertyType.STRING;
            case javax.jcr.PropertyType.URI:
                return PropertyType.URI;
            case javax.jcr.PropertyType.WEAKREFERENCE:
                return PropertyType.WEAKREFERENCE;
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                return PropertyType.SIMPLEREFERENCE;
        }
        return PropertyType.OBJECT;
    }
}

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
package org.modeshape.jcr.query.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An interface that defines the value types used in tuples.
 */
@Immutable
public abstract class TypeSystem {

    /**
     * Get the type factory given the name of the type.
     * 
     * @param typeName the name of the type
     * @return the type factory, or null if there is no such type in this system
     */
    public abstract TypeFactory<?> getTypeFactory( String typeName );

    /**
     * Get the type factory for the type denoted by the supplied prototype value.
     * 
     * @param prototype the value whose type is to be identified
     * @return the type factory, or null if there is no such type in this system
     */
    public abstract TypeFactory<?> getTypeFactory( Object prototype );

    /**
     * Get the type factory for boolean types.
     * 
     * @return the boolean factory; never null
     */
    public abstract TypeFactory<Boolean> getBooleanFactory();

    /**
     * Get the type factory for long types.
     * 
     * @return the long factory; never null
     */
    public abstract TypeFactory<Long> getLongFactory();

    /**
     * Get the type factory for string types.
     * 
     * @return the string factory; never null
     */
    public abstract TypeFactory<String> getStringFactory();

    /**
     * Get the type factory for double types.
     * 
     * @return the double factory; never null
     */
    public abstract TypeFactory<Double> getDoubleFactory();

    /**
     * Get the type factory for decimal types.
     * 
     * @return the decimal factory; never null
     */
    public abstract TypeFactory<BigDecimal> getDecimalFactory();

    /**
     * Get the type factory for date-time objects.
     * 
     * @return the date-time factory, or null if this type system doesn't support date-time objects
     */
    public abstract TypeFactory<?> getDateTimeFactory();

    /**
     * Get the type factory for path objects.
     * 
     * @return the path factory, or null if this type system doesn't support path objects
     */
    public abstract TypeFactory<Path> getPathFactory();

    /**
     * Get the type factory for name objects.
     * 
     * @return the name factory, or null if this type system doesn't support path objects
     */
    public abstract TypeFactory<Name> getNameFactory();

    /**
     * Get the type factory for references objects.
     * 
     * @return the reference factory, or null if this type system doesn't support reference objects
     */
    public abstract TypeFactory<Reference> getReferenceFactory();

    /**
     * Get the type factory for node key objects.
     * 
     * @return the node key factory, or null if this type system doesn't support node key objects
     */
    public abstract TypeFactory<NodeKey> getNodeKeyFactory();

    /**
     * Get the type factory for binary objects.
     * 
     * @return the binary factory, or null if this type system doesn't support binary objects
     */
    public abstract TypeFactory<BinaryValue> getBinaryFactory();

    /**
     * Get the name of the type that is used by default.
     * 
     * @return the default type name; never null
     */
    public abstract String getDefaultType();

    /**
     * Get the comparator that should be used by default.
     * 
     * @return the default comparator; never null
     */
    public abstract Comparator<Object> getDefaultComparator();

    /**
     * Get the names of the supported types.
     * 
     * @return the immutable set of the uppercase names of the types; never null
     */
    public abstract Set<String> getTypeNames();

    /**
     * Get the string representation of the supplied value, using the most appropriate factory given the value.
     * 
     * @param value the value
     * @return the string representation; never null
     */
    public abstract String asString( Object value );

    /**
     * Get the type that is compatible with both of the supplied types.
     * 
     * @param type1 the first type; may be null
     * @param type2 the second type; may be null
     * @return the compatible type; never null
     */
    public abstract String getCompatibleType( String type1,
                                              String type2 );

    /**
     * Get the type that is compatible with both of the supplied types.
     * 
     * @param type1 the first type; may be null
     * @param type2 the second type; may be null
     * @return the compatible type; never null
     */
    public abstract TypeFactory<?> getCompatibleType( TypeFactory<?> type1,
                                                      TypeFactory<?> type2 );

    /**
     * Factory interface for creating values from strings.
     * 
     * @param <T> the type of value object
     */
    public static interface TypeFactory<T> {

        /**
         * Get the class representing the value type.
         * 
         * @return the value type; never null
         */
        Class<T> getType();

        /**
         * Get a comparator that can be used to store the values of this type.
         * 
         * @return the comparator; never null
         */
        Comparator<T> getComparator();

        /**
         * Get the name of the type created by this factory.
         * 
         * @return the type name; never null and never empty or blank
         */
        String getTypeName();

        /**
         * Create the typed representation of the value given the supplied string representation.
         * 
         * @param value the string representation of the value
         * @return the typed representation, which will be an instance of the {@link #getType() type}
         * @throws ValueFormatException if the string cannot be converted to a typed value
         */
        T create( String value ) throws ValueFormatException;

        /**
         * Create the typed representation of the value given the supplied object representation.
         * 
         * @param value the object representation of the value
         * @return the typed representation, which will be an instance of the {@link #getType() type}
         * @throws ValueFormatException if the object cannot be converted to a typed value
         */
        T create( Object value ) throws ValueFormatException;

        /**
         * Get the string representation of the supplied value.
         * 
         * @param value the value
         * @return the string representation; never null
         */
        String asString( Object value );

        /**
         * Get the length of the supplied value.
         * 
         * @param value the value
         * @return the length; never negative
         */
        long length( Object value );

        /**
         * Get a readable and potentially shorter string representation of the supplied value.
         * 
         * @param value the value
         * @return the readable string representation; never null
         */
        String asReadableString( Object value );
    }

    public static interface SerializableComparator<T> extends Comparator<T>, Serializable {

    }

    /**
     * Return a new type factory that has a comparator that inverts the normal comparison.
     * 
     * @param original the original type factory; may not be null
     * @param comparator the new comparator to use; may be null
     * @return the new type factory; never null, but {@code original} if {@code comparator} is null
     */
    public static <T> TypeFactory<T> withComparator( final TypeFactory<T> original,
                                                     final Comparator<T> comparator ) {
        if (comparator == null) return original;
        return new TypeFactory<T>() {

            @Override
            public Class<T> getType() {
                return original.getType();
            }

            @Override
            public Comparator<T> getComparator() {
                return comparator;
            }

            @Override
            public String getTypeName() {
                return original.getTypeName();
            }

            @Override
            public T create( String value ) throws ValueFormatException {
                return original.create(value);
            }

            @Override
            public T create( Object value ) throws ValueFormatException {
                return original.create(value);
            }

            @Override
            public String asString( Object value ) {
                return original.asString(value);
            }

            @Override
            public long length( Object value ) {
                return original.length(value);
            }

            @Override
            public String asReadableString( Object value ) {
                return original.asReadableString(value);
            }

            @Override
            public String toString() {
                return original.toString() + " with comparator " + comparator;
            }

        };
    }

    /**
     * Return a new type factory that has a comparator that inverts the normal comparison.
     * 
     * @param original the original type factory; may not be null
     * @return the new type factory; never null
     */
    public static <T> TypeFactory<T> withOppositeComparator( final TypeFactory<T> original ) {
        final Comparator<T> comparator = original.getComparator();
        final Comparator<T> inverted = new SerializableComparator<T>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int compare( T arg0,
                                T arg1 ) {
                return comparator.compare(arg1, arg0);
            }
        };
        return withComparator(original, inverted);
    }

    /**
     * Return a new type factory that has a comparator that uses the supplied {@link Order} and {@link NullOrder} behavior.
     * 
     * @param original the original type factory; may not be null
     * @param order the specification of whether the comparator should order ascending or descending; may not be null
     * @param nullOrder the specification of whether null values should appear first or last; may not be null
     * @return the new type factory; never null, but possibly {@code original} if it has the desired behavior
     */
    public static <T> TypeFactory<T> with( final TypeFactory<T> original,
                                           final Order order,
                                           final NullOrder nullOrder ) {
        final Comparator<T> comparator = original.getComparator();
        final String toString = "Comparator<" + original.getTypeName() + ">(" + order + " " + nullOrder + ")";
        switch (order) {
            case ASCENDING:
                switch (nullOrder) {
                    case NULLS_FIRST:
                        // Nulls must be first, so we can just check that before we delegate to the original comparator ...
                        Comparator<T> nullFirst = new SerializableComparator<T>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public int compare( T o1,
                                                T o2 ) {
                                if (o1 == null) return -1;
                                if (o2 == null) return 1;
                                assert o1 != null;
                                assert o2 != null;
                                return comparator.compare(o1, o2); // order is same!
                            }

                            @Override
                            public String toString() {
                                return toString;
                            }
                        };
                        return withComparator(original, nullFirst);
                    case NULLS_LAST:
                        // Otherwise, nulls must be last, so we can just check that before we delegate to the original comparator
                        // ...
                        Comparator<T> nullLast = new SerializableComparator<T>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public int compare( T o1,
                                                T o2 ) {
                                if (o1 == null) return 1;
                                if (o2 == null) return -1;
                                assert o1 != null;
                                assert o2 != null;
                                return comparator.compare(o1, o2); // order is same!
                            }

                            @Override
                            public String toString() {
                                return toString;
                            }
                        };
                        return withComparator(original, nullLast);
                }
                assert false;
                break;
            case DESCENDING:
                switch (nullOrder) {
                    case NULLS_FIRST:
                        // Otherwise, nulls must be last, so we can just check that before we delegate to the original comparator
                        // ...
                        Comparator<T> nullFirst = new SerializableComparator<T>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public int compare( T o1,
                                                T o2 ) {
                                if (o1 == null) return -1;
                                if (o2 == null) return 1;
                                assert o1 != null;
                                assert o2 != null;
                                return comparator.compare(o2, o1); // order is different!
                            }

                            @Override
                            public String toString() {
                                return toString;
                            }
                        };
                        return withComparator(original, nullFirst);
                    case NULLS_LAST:
                        // Otherwise, nulls must be last, so we can just check that before we delegate to the original comparator
                        // ...
                        Comparator<T> nullLast = new SerializableComparator<T>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public int compare( T o1,
                                                T o2 ) {
                                if (o1 == null) return 1;
                                if (o2 == null) return -1;
                                assert o1 != null;
                                assert o2 != null;
                                return comparator.compare(o2, o1); // order is different!
                            }

                            @Override
                            public String toString() {
                                return toString;
                            }
                        };
                        return withComparator(original, nullLast);
                }
        }
        assert false;
        return null;
    }

}

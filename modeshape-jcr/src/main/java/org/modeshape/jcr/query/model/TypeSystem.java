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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An interface that defines the value types used in tuples.
 */
@Immutable
public interface TypeSystem {

    /**
     * Get the type factory given the name of the type.
     * 
     * @param typeName the name of the type
     * @return the type factory, or null if there is no such type in this system
     */
    TypeFactory<?> getTypeFactory( String typeName );

    /**
     * Get the type factory for the type denoted by the supplied prototype value.
     * 
     * @param prototype the value whose type is to be identified
     * @return the type factory, or null if there is no such type in this system
     */
    TypeFactory<?> getTypeFactory( Object prototype );

    /**
     * Get the type factory for boolean types.
     * 
     * @return the boolean factory; never null
     */
    TypeFactory<Boolean> getBooleanFactory();

    /**
     * Get the type factory for long types.
     * 
     * @return the long factory; never null
     */
    TypeFactory<Long> getLongFactory();

    /**
     * Get the type factory for string types.
     * 
     * @return the string factory; never null
     */
    TypeFactory<String> getStringFactory();

    /**
     * Get the type factory for double types.
     * 
     * @return the double factory; never null
     */
    TypeFactory<Double> getDoubleFactory();

    /**
     * Get the type factory for decimal types.
     * 
     * @return the decimal factory; never null
     */
    TypeFactory<BigDecimal> getDecimalFactory();

    /**
     * Get the type factory for date-time objects.
     * 
     * @return the date-time factory, or null if this type system doesn't support date-time objects
     */
    TypeFactory<?> getDateTimeFactory();

    /**
     * Get the type factory for path objects.
     * 
     * @return the path factory, or null if this type system doesn't support path objects
     */
    TypeFactory<?> getPathFactory();

    /**
     * Get the type factory for references objects.
     * 
     * @return the reference factory, or null if this type system doesn't support reference objects
     */
    TypeFactory<?> getReferenceFactory();

    /**
     * Get the type factory for binary objects.
     * 
     * @return the binary factory, or null if this type system doesn't support binary objects
     */
    TypeFactory<?> getBinaryFactory();

    /**
     * Get the name of the type that is used by default.
     * 
     * @return the default type name; never null
     */
    String getDefaultType();

    /**
     * Get the comparator that should be used by default.
     * 
     * @return the default comparator; never null
     */
    Comparator<Object> getDefaultComparator();

    /**
     * Get the names of the supported types.
     * 
     * @return the immutable set of the uppercase names of the types; never null
     */
    Set<String> getTypeNames();

    /**
     * Get the string representation of the supplied value, using the most appropriate factory given the value.
     * 
     * @param value the value
     * @return the string representation; never null
     */
    String asString( Object value );

    /**
     * Get the type that is compatible with both of the supplied types.
     * 
     * @param type1 the first type; may be null
     * @param type2 the second type; may be null
     * @return the compatible type; never null
     */
    String getCompatibleType( String type1,
                              String type2 );

    /**
     * Factory interface for creating values from strings.
     * 
     * @param <T> the type of value object
     */
    public interface TypeFactory<T> {

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

}

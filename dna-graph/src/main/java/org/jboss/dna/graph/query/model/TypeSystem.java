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
package org.jboss.dna.graph.query.model;

import java.util.Comparator;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.ValueFormatException;

/**
 * 
 */
@Immutable
public interface TypeSystem {

    TypeFactory<?> getTypeFactory( String typeName );

    TypeFactory<?> getTypeFactory( Object prototype );

    TypeFactory<Boolean> getBooleanFactory();

    TypeFactory<Long> getLongFactory();

    TypeFactory<String> getStringFactory();

    TypeFactory<Double> getDoubleFactory();

    TypeFactory<?> getDateTimeFactory();

    TypeFactory<?> getPathFactory();

    String getDefaultType();

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

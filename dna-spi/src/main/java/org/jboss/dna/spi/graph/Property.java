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
package org.jboss.dna.spi.graph;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

/**
 * Representation of a property consisting of a name and value(s). Note that this property is immutable, meaning that the property
 * values may not be changed through this interface.
 * 
 * @author Randall Hauch
 */
@Immutable
public interface Property extends Iterable<Object>, Comparable<Property> {

    /**
     * Get the name of the property.
     * 
     * @return the property name; never null
     */
    Name getName();

    /**
     * Get the name of the property's definition.
     * 
     * @return the property definition's name; never null
     */
    Name getDefinitionName();

    /**
     * Get the number of actual values in this property. If the property allows {@link #isMultiple() multiple values}, then this
     * method may return a value greater than 1. If the property only allows a {@link #isSingle() single value}, then this method
     * will return either 0 or 1. This method may return 0 regardless of whether the property allows a
     * {@link #isSingle() single value}, or {@link #isMultiple() multiple values}.
     * 
     * @return the number of actual values in this property; always non-negative
     */
    int size();

    /**
     * Determine whether the property currently has multiple values.
     * 
     * @return true if the property has multiple values, or false otherwise.
     * @see #isSingle()
     * @see #isEmpty()
     */
    boolean isMultiple();

    /**
     * Determine whether the property currently has a single value.
     * 
     * @return true if the property has a single value, or false otherwise.
     * @see #isMultiple()
     * @see #isEmpty()
     */
    boolean isSingle();

    /**
     * Determine whether this property has no actual values. This method may return <code>true</code> regardless of whether the
     * property allows a {@link #isSingle() single value}, or {@link #isMultiple() multiple values}.
     * <p>
     * This method is a convenience method that is equivalent to <code>size() == 0</code>.
     * </p>
     * 
     * @return true if this property has no values, or false otherwise
     * @see #isMultiple()
     * @see #isSingle()
     */
    boolean isEmpty();

    /**
     * Get the type for this property.
     * 
     * @return the property's type, which is never null
     */
    PropertyType getPropertyType();

    /**
     * Obtain the property's values in their natural form, as defined by {@link #getPropertyType()}. This is equivalent to
     * calling {@link Iterable#iterator() iterator()}.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the values; never null
     * @see Iterable#iterator()
     * @see #getValuesAsArray()
     */
    Iterator<?> getValues();

    /**
     * Obtain the property's values as an array of objects in their natural form, as defined by {@link #getPropertyType()}.
     * <p>
     * A valid array is return if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}, or a
     * null value is returned if the property is {@link #isEmpty() empty}.
     * </p>
     * <p>
     * The resulting array is a copy, guaranteeing immutability for the property.
     * </p>
     * 
     * @return the array of values
     * @see Iterable#iterator()
     * @see #getValues()
     */
    Object[] getValuesAsArray();

    /**
     * Obtain the property's values in their natural form, converting the values to the supplied {@link PropertyType} if it is
     * different than this property's {@link #getPropertyType() property type}. Note that it is not always possible to convert
     * between PropertyTypes.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @param type the property type defining the form of the values to be returned; if null, the
     * {@link #getPropertyType() property's type} is used
     * @return an iterator over the values; never null
     */
    Iterator<?> getValues( PropertyType type );

    /**
     * Obtain the property's values as String objects, converting the values if the {@link #getPropertyType() property type} is
     * not {@link PropertyType#STRING}. Note that it is always possible to convert to a String value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the String values; never null
     */
    Iterator<String> getStringValues();

    /**
     * Obtain the property's values as Binary objects, converting the values if the {@link #getPropertyType() property type} is
     * not {@link PropertyType#BINARY}. Note that it is always possible to convert to a {@link Binary} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the {@link Binary} values; never null
     */
    Iterator<Binary> getBinaryValues();

    /**
     * Obtain the property's values as longs, converting the values if the {@link #getPropertyType() property type} is not
     * {@link PropertyType#LONG}. Note that it is not always possible to convert to a {@link PropertyType#LONG long} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the long values; never null
     */
    Iterator<Long> getLongValues();

    /**
     * Obtain the property's values as doubles, converting the values if the {@link #getPropertyType() property type} is not
     * {@link PropertyType#DOUBLE}. Note that it is not always possible to convert to a {@link PropertyType#DOUBLE double} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the double values; never null
     */
    Iterator<Double> getDoubleValues();

    /**
     * Obtain the property's values as decimal values, converting the values if the {@link #getPropertyType() property type} is
     * not {@link PropertyType#DECIMAL}. Note that it is not always possible to convert to a {@link PropertyType#DECIMAL decimal}
     * value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the decimal values; never null
     */
    Iterator<BigDecimal> getDecimalValues();

    /**
     * Obtain the property's values as {@link DateTime dates}, converting the values if the
     * {@link #getPropertyType() property type} is not {@link PropertyType#DATE}. Note that it is not always possible to convert
     * to a {@link PropertyType#DATE date} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the DateTime values; never null
     */
    Iterator<DateTime> getDateValues();

    /**
     * Obtain the property's values as booleans, converting the values if the {@link #getPropertyType() property type} is not
     * {@link PropertyType#BOOLEAN}. Note that it is not always possible to convert to a {@link PropertyType#BOOLEAN boolean}
     * value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the Boolean values; never null
     */
    Iterator<Boolean> getBooleanValues();

    /**
     * Obtain the property's values as {@link Name names}, converting the values if the {@link #getPropertyType() property type}
     * is not {@link PropertyType#NAME}. Note that it is not always possible to convert to a {@link PropertyType#NAME long}
     * value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the Name values; never null
     */
    Iterator<Name> getNameValues();

    /**
     * Obtain the property's values as {@link Path paths}, converting the values if the {@link #getPropertyType() property type}
     * is not {@link PropertyType#PATH}. Note that it is not always possible to convert to a {@link PropertyType#PATH path}
     * value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the Path values; never null
     */
    Iterator<Path> getPathValues();

    /**
     * Obtain the property's values as references, converting the values if the {@link #getPropertyType() property type} is not
     * {@link PropertyType#REFERENCE}. Note that it is not always possible to convert to a
     * {@link PropertyType#REFERENCE reference} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the Reference values; never null
     */
    Iterator<Reference> getReferenceValues();

    /**
     * Obtain the property's values as {@link URI URIs}, converting the values if the {@link #getPropertyType() property type} is
     * not {@link PropertyType#URI}. Note that it is not always possible to convert to a {@link PropertyType#URI URI} value.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the URI values; never null
     */
    Iterator<URI> getUriValues();

}

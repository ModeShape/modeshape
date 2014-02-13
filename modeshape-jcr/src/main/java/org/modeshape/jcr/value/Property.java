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
package org.modeshape.jcr.value;

import java.io.Serializable;
import java.util.Iterator;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.value.DateTime;

/**
 * Representation of a property consisting of a name and value(s). Note that this property is immutable, meaning that the property
 * values may not be changed through this interface.
 * <p>
 * This class is designed to be used with the {@link ValueFactories} interface and the particular {@link ValueFactory} that
 * corresponds to the type of value you'd like to use. The <code>ValueFactory</code> will then return the values (if no type
 * conversion is required) or will convert the values using the appropriate conversion algorithm.
 * </p>
 * <p>
 * The following example shows how to obtain the {@link String} representations of the {@link #getValues() property values}:
 * 
 * <pre>
 *   ValueFactories valueFactories = ...
 *   Property property = ...
 *   Iterator&lt;String&gt; iter = valueFactories.getStringFactory().create(property.getValues());
 *   while ( iter.hasNext() ) {
 *       System.out.println(iter.next());
 *   }
 * </pre>
 * 
 * Meanwhile, the {@link ValueFactories#getLongFactory() long value factory} converts the values to <code>long</code>, the
 * {@link ValueFactories#getDateFactory() date value factory} converts the values to {@link DateTime} instances, and so on.
 * </p>
 * <p>
 * This technique is much better and far safer than casting the values. It is possible that some Property instances contain
 * heterogeneous values, so casting may not always work. Also, this technique guarantees that the values are properly converted if
 * the type is not what you expected.
 * </p>
 */
@Immutable
public interface Property extends Iterable<Object>, Comparable<Property>, Readable, Serializable {

    /**
     * Get the name of the property.
     * 
     * @return the property name; never null
     */
    Name getName();

    /**
     * Get the number of actual values in this property. If the property allows {@link #isMultiple() multiple values}, then this
     * method may return a value greater than 1. If the property only allows a {@link #isSingle() single value}, then this method
     * will return either 0 or 1. This method may return 0 regardless of whether the property allows a {@link #isSingle() single
     * value}, or {@link #isMultiple() multiple values}.
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
     * Determine whether this property contains reference values, based upon the first value in the property. Note that
     * {@link PropertyType#SIMPLEREFERENCE} properties *are not* treated as references in order to avoid setting the
     * back-pointer.
     * 
     * @return true if this property is a reference property, or false otherwise.
     */
    boolean isReference();

    /**
     * Determine whether this property contains simple reference values, based upon the first value in the property.
     * @see PropertyType#SIMPLEREFERENCE
     *
     * @return true if this property is a simple reference property, or false otherwise.
     */
    boolean isSimpleReference();

    /**
     * Determine whether this property is a binary property or not.
     *
     * @return true if the property is a binary property, false otherwise.
     */
    boolean isBinary();

    /**
     * Obtain the property's first value in its natural form. This is equivalent to calling
     * <code>isEmpty() ? null : iterator().next()</code>
     * 
     * @return the first value, or null if the property is {@link #isEmpty() empty}
     * @see Iterable#iterator()
     * @see #getValues()
     * @see #getValuesAsArray()
     * @see #isEmpty()
     */
    Object getFirstValue();

    /**
     * Obtain the property's values in their natural form. This is equivalent to calling {@link Iterable#iterator() iterator()}.
     * <p>
     * A valid iterator is returned if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}.
     * </p>
     * <p>
     * The resulting iterator is immutable, and all property values are immutable.
     * </p>
     * 
     * @return an iterator over the values; never null
     * @see #getFirstValue()
     * @see Iterable#iterator()
     * @see #getValuesAsArray()
     * @see ValueFactory#create(Iterator)
     */
    Iterator<?> getValues();

    /**
     * Obtain the property's values as an array of objects in their natural form.
     * <p>
     * A valid array is return if the property has {@link #isSingle() single valued} or {@link #isMultiple() multi-valued}, or a
     * null value is returned if the property is {@link #isEmpty() empty}.
     * </p>
     * <p>
     * The resulting array is a copy, guaranteeing immutability for the property.
     * </p>
     * 
     * @return the array of values
     * @see #getFirstValue()
     * @see Iterable#iterator()
     * @see #getValues()
     * @see ValueFactory#create(Object[])
     */
    Object[] getValuesAsArray();

    /**
     * Returns the value of the property at the given index.
     *
     * @param index an {@code int} representing the index at which to retrieve the value. If the value is {@code 0}, this is
     * equivalent to calling {@link org.modeshape.jcr.value.Property#getFirstValue()}
     * @return the value of the property at the given index; may be {@code null} if the property is empty.
     * @throws IndexOutOfBoundsException if the given index is outside the interval of values of the property.
     */
    Object getValue(int index) throws IndexOutOfBoundsException;

    /**
     * Convert the values of this property to whatever type the given value factory is used to create.
     *
     * @param valueFactory a {@link org.modeshape.jcr.value.basic.AbstractValueFactory} representing the factory which will
     * be used to attempt the conversion of each value.
     * @return the array of values for this property converted (if possible) to given type.
     * @throws ValueFormatException if the conversion cannot be performed for any value in the array of values.
     */
    <T> T[] getValuesAsArray( ValueFactory<T> valueFactory ) throws ValueFormatException;

    /**
     * Convert the values of this property to the given type, using the specified type transformer.
     *
     * @param valueTypeTransformer a {@link ValueTypeTransformer} representing the transformer which will
     * be used to attempt the conversion of each value.
     * @param  type a {@link Class} indicating the type to which the transformation is performed; required because of type erasure.
     * @return the array of values for this property converted (if possible) to given type.
     * @throws ValueFormatException if the conversion cannot be performed for any value in the array of values.
     */
    <T> T[] getValuesAsArray( ValueTypeTransformer<T> valueTypeTransformer,
                              Class<T> type ) throws ValueFormatException;

    /**
     * Interface which allows the conversion of a property value to a given type.
     * @param <T> the generic type
     */
    public interface ValueTypeTransformer<T> {
        /**
         * Transforms the given value to a specific type.
         *
         * @param value the individual value of a property; never {@code null}
         * @return the value transformed to the given type.
         */
        T transform(Object value);
    }
}

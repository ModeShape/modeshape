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

import java.io.Serializable;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

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
}

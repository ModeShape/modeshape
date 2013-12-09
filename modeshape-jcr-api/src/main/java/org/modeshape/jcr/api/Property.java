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

package org.modeshape.jcr.api;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Extension of {@link javax.jcr.Property} with some custom methods for retrieving values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Property extends javax.jcr.Property {

    /**
     * Converts the value of the property (if possible) to the specified type.
     * The list of supported types is:
     * <ul>
     *     <li>{@link String}</li>
     *     <li>{@link Long}</li>
     *     <li>{@link Double}</li>
     *     <li>{@link Boolean}</li>
     *     <li>{@link java.math.BigDecimal}</li>
     *     <li>{@link java.util.Calendar}</li>
     *     <li>{@link java.util.Date}</li>
     *     <li>{@link org.modeshape.jcr.api.value.DateTime}</li>
     *     <li>{@link org.modeshape.jcr.api.Binary}</li>
     *     <li>{@link javax.jcr.Binary}</li>
     *     <li>{@link java.io.InputStream}</li>
     *     <li>{@link javax.jcr.Node}</li>
     *     <li>{@link javax.jcr.NodeIterator}</li>
     * </ul>
     * <p>
     * In case of <b>homogeneous</b> (all values are of the same type) multi-valued properties, the value type should be the
     * array version of one of the above types, e.g. {@code getAs(Long[].class)}.
     * The only exception from this rule is that in the case of multi-valued reference properties
     * a simple {@link javax.jcr.NodeIterator} type is expected: {@code getAs(NodeIterator.class)}.
     *
     * @param type a {@link Class} representing the type to which to convert the value of the property; may not be {@code null}
     * and is expected to be one of the above types.
     * @param <T> the type-argument for {@code type}
     * @return the value of this property converted to the given type.
     * @throws ValueFormatException if the conversion cannot be performed. This can occur for a number of reasons: type
     * incompatibility or passing in an array type for a single value property or passing in a non-array type for a multi-valued
     * property.
     * @throws RepositoryException if anything else unexpected fails when attempting to perform the conversion.
     */
    public <T> T getAs(Class<T> type) throws ValueFormatException, RepositoryException;

    /**
     * Converts the value of the property located at the given index to the specified type. This should only be used for multi-valued
     * properties and <b>array types are not supported</b>
     * <p>
     * For example: {@code getAs(Long.class, 1)}
     *
     * @param type a {@link Class} representing the type to which to convert the value of the property; may not be {@code null}
     * and is expected to be one of the types from {@link Property#getAs(Class)}.
     * @param index the position of the property in the array of values.
     * @param <T> the type-argument for {@code type}
     * @return the value at the position {@code index} converted to the given type.
     * @throws ValueFormatException if the conversion cannot be performed or if this method is called for single-valued properties.
     * @throws IndexOutOfBoundsException if {@code index} is outside the array of values for the property.
     * @throws RepositoryException if anything unexpected fails.
     */
    public <T> T getAs(Class<T> type, int index) throws ValueFormatException, IndexOutOfBoundsException, RepositoryException;
}

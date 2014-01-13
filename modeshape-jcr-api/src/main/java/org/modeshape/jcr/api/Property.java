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
package org.modeshape.jcr.api;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Extension of {@link javax.jcr.Property} with some custom methods for retrieving values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Property extends javax.jcr.Property {

    /**
     * Converts the value(s) of the property (if possible) to the specified type.
     * The list of supported types for single-valued properties is:
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
     * The list of supported types for multi-valued properties is:
     * <ul>
     *     <li>{@link String}{@code []}</li>
     *     <li>{@link Long}{@code []}</li>
     *     <li>{@link Double}{@code []}</li>
     *     <li>{@link Boolean}{@code []}</li>
     *     <li>{@link java.math.BigDecimal}{@code []}</li>
     *     <li>{@link java.util.Calendar}{@code []}</li>
     *     <li>{@link java.util.Date}{@code []}</li>
     *     <li>{@link org.modeshape.jcr.api.value.DateTime}{@code []}</li>
     *     <li>{@link org.modeshape.jcr.api.Binary}{@code []}</li>
     *     <li>{@link javax.jcr.Binary}{@code []}</li>
     *     <li>{@link java.io.InputStream}{@code []}</li>
     *     <li>{@link javax.jcr.Node}{@code []}</li>
     *     <li>{@link javax.jcr.NodeIterator}</li>
     * </ul>
     * <p>
     * For single-valued properties, this method attempts to convert the actual value into the specified type.
     * For multi-valued properties, this method attempts to convert the actual values into an array of the specified 
     * type; the only exception to this rule is that in the 
     * case of multi-valued reference properties a simple {@link javax.jcr.NodeIterator} type is expected: 
     * {@code getAs(NodeIterator.class)}.
     * </p>
     * <p>
     * For example, the following shows how to obtain the value of a single-valued property as a {@code String} value:
     * <pre>
     *     Property prop = ...
     *     String value = prop.getAs(String.class);
     * </pre>
     * If the property is multi-valued, then the values can be obtained by passing in the desired array type:
     * <pre>
     *     String[] values = prop.getAs(String[].class);
     * </pre>
     * This example will always work, since all property values can be converted to a {@code String}.
     * </p>
     * <p>
     * The following attempts to convert all values to {@link java.util.Date} instances:
     * <pre>
     *     Date[] values = prop.getAs(Date[].class);
     * </pre>
     * </p>
     * <p>
     * Finally, a single- or multi-valued property whose value(s) are references can easily be obtained as a
     * {@link NodeIterator}, which can represent one or multiple {@link Node} instances:
     * <pre>
     *     NodeIterator iter = prop.getAs(NodeIterator.class);
     * </pre>
     * </p>
     *
     * @param type a {@link Class} representing the type to which to convert the values of the property; may not be {@code null}
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
     * Converts the value of the property located at the given index to the specified type. This can be used for single-valued
     * properties if the <code>index</code> property is '0', or multi-valued properties. <b>Array types should not be supplied 
     * as the first parameter.</b>
     * <p>
     * For example, this is a property usage of this method to obtain the value of a single-valued property or 
     * the first value of a multi-valued property as a {@code long}:
     * <pre>
     *     Long value = prop.getAs(Long.class, 0)
     * </pre>
     * Likewise, the following usage will only work for multi-valued properties, since only a multi-valued property 
     * can have 2 or more values:
     * <pre>
     *     Long value = prop.getAs(Long.class, 1)
     * </pre>
     * </p>
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

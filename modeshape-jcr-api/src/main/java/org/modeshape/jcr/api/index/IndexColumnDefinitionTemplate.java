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

package org.modeshape.jcr.api.index;

/**
 * An immutable definition of a column used in an {@link IndexDefinition}.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexColumnDefinitionTemplate extends IndexColumnDefinition {

    /**
     * Get the name of the property for which this index column applies.
     *
     * @return the property name; null if the value has not yet been set on this template
     */
    @Override
    String getPropertyName();

    /**
     * Set the name of the property for which this index column applies.
     *
     * @param name the name of the property; may not be null
     * @return this instance for method chaining; never null
     */
    IndexColumnDefinitionTemplate setPropertyName( String name );

    /**
     * Set the type of value for this index column applies.
     *
     * @param propertyType the {@link javax.jcr.PropertyType property type} for this column
     * @return this instance for method chaining; never null
     * @throws IllegalArgumentException if the supplied property type is not one of the {@link javax.jcr.PropertyType} constants.
     */
    IndexColumnDefinitionTemplate setColumnType( int propertyType );
}

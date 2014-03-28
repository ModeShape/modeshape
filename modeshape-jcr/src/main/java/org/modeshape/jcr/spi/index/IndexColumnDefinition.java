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

package org.modeshape.jcr.spi.index;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.PropertyType;

/**
 * An immutable definition of a column used in an {@link IndexDefinition}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public interface IndexColumnDefinition {

    /**
     * Get the name of the property for which this index column applies.
     * 
     * @return the property name; never null
     */
    Name getPropertyName();

    /**
     * Get the type of value for this index column applies.
     * 
     * @return the type; never null
     */
    PropertyType getColumnType();
}

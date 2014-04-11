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

package org.modeshape.jcr;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.spi.index.IndexColumnDefinition;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.PropertyType;

@Immutable
class RepositoryIndexColumnDefinition implements IndexColumnDefinition {

    public static IndexColumnDefinition createFrom( IndexColumnDefinition other ) {
        return new RepositoryIndexColumnDefinition(other.getPropertyName(), other.getColumnType());
    }

    private final Name propertyTypeName;
    private final PropertyType columnType;

    RepositoryIndexColumnDefinition( Name propertyTypeName,
                                     PropertyType columnType ) {
        this.propertyTypeName = propertyTypeName;
        this.columnType = columnType;
    }

    @Override
    public Name getPropertyName() {
        return propertyTypeName;
    }

    @Override
    public PropertyType getColumnType() {
        return columnType;
    }
}

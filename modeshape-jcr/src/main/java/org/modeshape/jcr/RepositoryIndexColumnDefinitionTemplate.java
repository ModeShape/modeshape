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
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.index.IndexColumnDefinitionTemplate;
import org.modeshape.jcr.value.PropertyType;

@Immutable
class RepositoryIndexColumnDefinitionTemplate implements IndexColumnDefinitionTemplate {

    private String propertyName;
    private int columnType = javax.jcr.PropertyType.STRING;

    RepositoryIndexColumnDefinitionTemplate() {
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public int getColumnType() {
        return columnType;
    }

    @Override
    public IndexColumnDefinitionTemplate setPropertyName( String propertyName ) {
        CheckArg.isNotNull(propertyName, "propertyName");
        this.propertyName = propertyName;
        return this;
    }

    @Override
    public IndexColumnDefinitionTemplate setColumnType( int type ) {
        CheckArg.isNotNull(type, "type");
        if (PropertyType.OBJECT == PropertyType.valueFor(type)) {
            throw new IllegalArgumentException(JcrI18n.invalidPropertyType.text(type));
        }
        this.columnType = type;
        return this;
    }
}

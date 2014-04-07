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
package org.modeshape.web.jcr.rest.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;

@Immutable
public class QueryRow {

    private Map<String, String> queryTypes;
    private Map<String, Object> values;

    public QueryRow( Map<String, String> queryTypes,
                     Map<String, Object> values ) {
        super();
        // queryTypes is expected to already be an unmodifiable map
        this.queryTypes = queryTypes;
        this.values = Collections.unmodifiableMap(values);
    }

    public Collection<String> getColumnNames() {
        return queryTypes != null ? queryTypes.keySet() : values.keySet();
    }

    public Object getValue( String columnName ) {
        return values.get(columnName);
    }

    public String getColumnType( String columnName ) {
        return queryTypes.get(columnName);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}

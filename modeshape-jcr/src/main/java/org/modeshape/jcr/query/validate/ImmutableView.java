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
package org.modeshape.jcr.query.validate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.validate.Schemata.Column;
import org.modeshape.jcr.query.validate.Schemata.Key;
import org.modeshape.jcr.query.validate.Schemata.View;

/**
 * 
 */
@Immutable
class ImmutableView extends ImmutableTable implements View {

    private final QueryCommand definition;

    protected ImmutableView( SelectorName name,
                             Iterable<Column> columns,
                             boolean extraColumns,
                             QueryCommand definition ) {
        super(name, columns, extraColumns);
        this.definition = definition;
    }

    protected ImmutableView( SelectorName name,
                             Iterable<Column> columns,
                             boolean extraColumns,
                             QueryCommand definition,
                             @SuppressWarnings( "unchecked" ) Iterable<Column>... keyColumns ) {
        super(name, columns, extraColumns, keyColumns);
        this.definition = definition;
    }

    protected ImmutableView( SelectorName name,
                             Map<String, Column> columnsByName,
                             List<Column> columns,
                             boolean extraColumns,
                             QueryCommand definition,
                             Set<Key> keys ) {
        super(name, columnsByName, columns, keys, extraColumns, columnsByName, columns);
        this.definition = definition;
    }

    protected ImmutableView( SelectorName name,
                             Map<String, Column> columnsByName,
                             List<Column> columns,
                             boolean extraColumns,
                             QueryCommand definition,
                             Set<Key> keys,
                             Map<String, Column> selectStarColumnsByName,
                             List<Column> selectStarColumns ) {
        super(name, columnsByName, columns, keys, extraColumns, selectStarColumnsByName, selectStarColumns);
        this.definition = definition;
    }

    @Override
    public QueryCommand getDefinition() {
        return definition;
    }

    @Override
    public ImmutableView withColumnNotInSelectStar( String name ) {
        ImmutableTable result = super.withColumnNotInSelectStar(name);
        if (result == this) return this;
        return new ImmutableView(result.getName(), result.getColumnsByName(), result.getColumns(), result.hasExtraColumns(),
                                 definition, result.getKeySet(), result.getSelectAllColumnsByName(), result.getSelectAllColumns());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName().name());
        sb.append('(');
        boolean first = true;
        for (Column column : getColumns()) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
        }
        sb.append(") AS '");
        sb.append(Visitors.readable(definition));
        sb.append('\'');
        if (!getKeys().isEmpty()) {
            sb.append(" with keys ");
            first = true;
            for (Key key : getKeys()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(key);
            }
        }
        return sb.toString();
    }
}

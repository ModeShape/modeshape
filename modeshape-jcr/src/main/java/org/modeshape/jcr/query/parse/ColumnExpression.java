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
package org.modeshape.jcr.query.parse;

import org.infinispan.schematic.internal.HashCode;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.query.model.SelectorName;

/**
 * A representation of a column as expressed in the SQL query. Note that the selector name may not have been explicitly specified.
 */
@Immutable
class ColumnExpression {

    private final SelectorName selectorName;
    private final String propertyName;
    private final String columnName;
    private final Position position;

    /**
     * A column with the given name representing the named property on the node identified by the selector.
     * 
     * @param selectorName the selector name; may be null if no selector was explicitly used in the query
     * @param propertyName the name of the property
     * @param columnName the name of the column
     * @param position the position of the column in the query
     */
    ColumnExpression( SelectorName selectorName,
                      String propertyName,
                      String columnName,
                      Position position ) {
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(columnName, "columnName");
        CheckArg.isNotNull(position, "position");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.columnName = columnName;
        this.position = position;
    }

    /**
     * Get the column's position in the query.
     * 
     * @return the column's position; never null
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @return the name of the selector; may be null if no selector was explicitly used in the query
     */
    public final SelectorName getSelectorName() {
        return selectorName;
    }

    /**
     * Get the name of the property.
     * 
     * @return the property name; never null
     */
    public final String getPropertyName() {
        return propertyName;
    }

    /**
     * Get the name of the column.
     * 
     * @return the column name; never null
     */
    public final String getColumnName() {
        return columnName;
    }

    @Override
    public int hashCode() {
        return HashCode.compute(this.selectorName, this.propertyName, this.columnName);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ColumnExpression) {
            ColumnExpression that = (ColumnExpression)obj;
            if (!ObjectUtil.isEqualWithNulls(this.selectorName, that.selectorName)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.propertyName, that.propertyName)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.columnName, that.columnName)) return false;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (selectorName != null) {
            sb.append(selectorName.name());
            sb.append('.');
        }
        sb.append(propertyName);
        if (columnName != null) {
            sb.append(" AS ").append(columnName);
        }
        return sb.toString();
    }
}

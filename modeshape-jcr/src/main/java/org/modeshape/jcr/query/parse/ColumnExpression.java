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

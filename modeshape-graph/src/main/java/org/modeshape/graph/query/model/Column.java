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
package org.modeshape.graph.query.model;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ObjectUtil;

/**
 * 
 */
@Immutable
public class Column implements LanguageObject {
    private static final long serialVersionUID = 1L;

    private final SelectorName selectorName;
    private final String propertyName;
    private final String columnName;

    /**
     * Include a column for each of the single-valued, accessible properties on the node identified by the selector.
     * 
     * @param selectorName the selector name
     */
    public Column( SelectorName selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        this.selectorName = selectorName;
        this.propertyName = null;
        this.columnName = null;
    }

    /**
     * A column with the given name representing the named property on the node identified by the selector.
     * 
     * @param selectorName the selector name
     * @param propertyName the name of the property
     * @param columnName the name of the column
     */
    public Column( SelectorName selectorName,
                   String propertyName,
                   String columnName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(columnName, "columnName");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.columnName = columnName;
    }

    /**
     * Get the name of the selector for the node.
     * 
     * @return the selector name; never null
     */
    public final SelectorName selectorName() {
        return selectorName;
    }

    /**
     * Get the name of the property that this column represents.
     * 
     * @return the property name; or null if this represents all selectable columns on the {@link #selectorName() selector}
     */
    public final String propertyName() {
        return propertyName;
    }

    /**
     * Get the name of the column.
     * 
     * @return the column name; or null if this represents all selectable columsn on the {@link #selectorName() selector}
     */
    public final String columnName() {
        return columnName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return selectorName().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Column) {
            Column that = (Column)obj;
            if (!this.selectorName.equals(that.selectorName)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.propertyName, that.propertyName)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.columnName, that.columnName)) return false;
            return true;
        }
        return false;
    }

    /**
     * Create a copy of this Column except that uses the supplied selector name instead.
     * 
     * @param newSelectorName the new selector name
     * @return a new Column with the supplied selector name and the property and column names from this object; never null
     * @throws IllegalArgumentException if the supplied selector name is null
     */
    public Column with( SelectorName newSelectorName ) {
        return new Column(newSelectorName, propertyName, columnName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

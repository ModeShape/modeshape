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
package org.modeshape.jcr.query.model;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ObjectUtil;

/**
 * 
 */
@Immutable
public class Column implements LanguageObject, javax.jcr.query.qom.Column {
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

    @Override
    public String getSelectorName() {
        return selectorName.getString();
    }

    @Override
    public final String getPropertyName() {
        return propertyName;
    }

    @Override
    public final String getColumnName() {
        return columnName;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return selectorName().hashCode();
    }

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

    public Column withColumnName( String columnName ) {
        return new Column(selectorName, propertyName, columnName);
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

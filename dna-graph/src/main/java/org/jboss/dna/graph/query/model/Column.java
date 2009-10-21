/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.model;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.ObjectUtil;
import org.jboss.dna.graph.property.Name;

/**
 * 
 */
@Immutable
public class Column implements LanguageObject {

    private final SelectorName selectorName;
    private final Name propertyName;
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
                   Name propertyName,
                   String columnName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(columnName, "columnName");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.columnName = columnName;
    }

    /**
     * @return selectorName
     */
    public final SelectorName getSelectorName() {
        return selectorName;
    }

    /**
     * @return propertyName
     */
    public final Name getPropertyName() {
        return propertyName;
    }

    /**
     * @return columnName
     */
    public final String getColumnName() {
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
        return getSelectorName().hashCode();
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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitable#accept(org.jboss.dna.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

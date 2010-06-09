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
package org.modeshape.jcr.query.qom;

import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.SelectorName;

/**
 * An implementation of JCR's {@link javax.jcr.query.qom.Column} and specialization of the Graph API's {@link Column}.
 */
public class JcrColumn extends Column implements javax.jcr.query.qom.Column {

    private static final long serialVersionUID = 1L;

    /**
     * Include a column for each of the single-valued, accessible properties on the node identified by the selector.
     * 
     * @param selectorName the selector name
     */
    public JcrColumn( SelectorName selectorName ) {
        super(selectorName);
    }

    /**
     * A column with the given name representing the named property on the node identified by the selector.
     * 
     * @param selectorName the selector name
     * @param propertyName the name of the property
     * @param columnName the name of the column
     */
    public JcrColumn( SelectorName selectorName,
                      String propertyName,
                      String columnName ) {
        super(selectorName, propertyName, columnName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Column#getSelectorName()
     */
    @Override
    public String getSelectorName() {
        return selectorName().name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Column#getPropertyName()
     */
    @Override
    public String getPropertyName() {
        return propertyName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Column#getColumnName()
     */
    @Override
    public String getColumnName() {
        return columnName();
    }

    /**
     * Create a copy of this Column except that uses the supplied selector name instead.
     * 
     * @param newSelectorName the new selector name
     * @return a new Column with the supplied selector name and the property and column names from this object; never null
     * @throws IllegalArgumentException if the supplied selector name is null
     */
    @Override
    public Column with( SelectorName newSelectorName ) {
        return new JcrColumn(newSelectorName, getPropertyName(), getColumnName());
    }
}

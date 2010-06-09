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
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.SelectorName;

/**
 * Implementation of the equi-join condition for the JCR Query Object Model and the Graph API.
 */
public class JcrEquiJoinCondition extends EquiJoinCondition implements javax.jcr.query.qom.EquiJoinCondition, JcrJoinCondition {

    private static final long serialVersionUID = 1L;

    /**
     * Create an equi-join condition, given the names of the selector and property for the left- and right-hand-side of the join.
     * 
     * @param selector1Name the selector name appearing on the left-side of the join; never null
     * @param property1Name the property name for the left-side of the join; never null
     * @param selector2Name the selector name appearing on the right-side of the join; never null
     * @param property2Name the property name for the right-side of the join; never null
     */
    public JcrEquiJoinCondition( SelectorName selector1Name,
                                 String property1Name,
                                 SelectorName selector2Name,
                                 String property2Name ) {
        super(selector1Name, property1Name, selector2Name, property2Name);

    }

    /**
     * Create an equi-join condition, given the columns.
     * 
     * @param column1 the column for the left-side of the join; never null
     * @param column2 the column for the right-side of the join; never null
     */
    public JcrEquiJoinCondition( Column column1,
                                 Column column2 ) {
        super(column1, column2);

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.EquiJoinCondition#getProperty1Name()
     */
    @Override
    public String getProperty1Name() {
        return property1Name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.EquiJoinCondition#getProperty2Name()
     */
    @Override
    public String getProperty2Name() {
        return property2Name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.EquiJoinCondition#getSelector1Name()
     */
    @Override
    public String getSelector1Name() {
        return selector1Name().name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.EquiJoinCondition#getSelector2Name()
     */
    @Override
    public String getSelector2Name() {
        return selector2Name().name();
    }
}

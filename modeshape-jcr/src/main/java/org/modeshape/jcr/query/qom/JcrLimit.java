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

import org.modeshape.graph.query.model.Limit;

/**
 * Implementation of the equi-join condition for the extended JCR Query Object Model and the Graph API.
 */
public class JcrLimit extends Limit implements org.modeshape.jcr.api.query.qom.Limit {

    public static final JcrLimit NONE = new JcrLimit(Integer.MAX_VALUE, 0);

    private static final long serialVersionUID = 1L;

    /**
     * Create a limit on the number of rows.
     * 
     * @param rowLimit the maximum number of rows
     * @throws IllegalArgumentException if the row limit is negative
     */
    public JcrLimit( int rowLimit ) {
        super(rowLimit);
    }

    /**
     * Create a limit on the number of rows and the number of initial rows to skip.
     * 
     * @param rowLimit the maximum number of rows
     * @param offset the number of rows to skip before beginning the results
     * @throws IllegalArgumentException if the row limit is negative, or if the offset is negative
     */
    public JcrLimit( int rowLimit,
                     int offset ) {
        super(rowLimit, offset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.Limit#getOffset()
     */
    @Override
    public int getOffset() {
        return offset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.Limit#getRowLimit()
     */
    @Override
    public int getRowLimit() {
        return rowLimit();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Limit#withOffset(int)
     */
    @Override
    public JcrLimit withOffset( int offset ) {
        return new JcrLimit(rowLimit(), offset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Limit#withRowLimit(int)
     */
    @Override
    public JcrLimit withRowLimit( int rowLimit ) {
        return new JcrLimit(rowLimit, offset());
    }
}

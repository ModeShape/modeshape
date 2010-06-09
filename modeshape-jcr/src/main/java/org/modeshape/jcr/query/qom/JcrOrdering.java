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

import javax.jcr.query.qom.QueryObjectModelConstants;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;

/**
 * The implementation of the JCR Query Object Model's {@link javax.jcr.query.qom.Ordering}.
 */
public class JcrOrdering extends Ordering implements javax.jcr.query.qom.Ordering {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ordering specification, given the supplied operand and order.
     * 
     * @param operand the operand being ordered
     * @param order the order type
     * @throws IllegalArgumentException if the operand or order type is null
     */
    public JcrOrdering( JcrDynamicOperand operand,
                        Order order ) {
        super(operand, order);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Ordering#operand()
     */
    @Override
    public JcrDynamicOperand operand() {
        return (JcrDynamicOperand)super.operand();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Ordering#getOperand()
     */
    @Override
    public JcrDynamicOperand getOperand() {
        return operand();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Ordering#getOrder()
     */
    @Override
    public String getOrder() {
        switch (order()) {
            case ASCENDING:
                return QueryObjectModelConstants.JCR_ORDER_ASCENDING;
            case DESCENDING:
                return QueryObjectModelConstants.JCR_ORDER_DESCENDING;
        }
        assert false;
        return null;
    }
}

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

import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Operator;

/**
 * 
 */
public class JcrComparison extends Comparison implements JcrConstraint, javax.jcr.query.qom.Comparison {

    private static final long serialVersionUID = 1L;

    /**
     * @param operand1
     * @param operator
     * @param operand2
     */
    public JcrComparison( JcrDynamicOperand operand1,
                          Operator operator,
                          JcrStaticOperand operand2 ) {
        super(operand1, operator, operand2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Comparison#operand1()
     */
    @Override
    public JcrDynamicOperand operand1() {
        return (JcrDynamicOperand)super.operand1();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Comparison#operand2()
     */
    @Override
    public JcrStaticOperand operand2() {
        return (JcrStaticOperand)super.operand2();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Comparison#getOperand1()
     */
    @Override
    public DynamicOperand getOperand1() {
        return operand1();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Comparison#getOperand2()
     */
    @Override
    public StaticOperand getOperand2() {
        return operand2();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Comparison#getOperator()
     */
    @Override
    public String getOperator() {
        switch (operator()) {
            case EQUAL_TO:
                return QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
            case GREATER_THAN:
                return QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN;
            case GREATER_THAN_OR_EQUAL_TO:
                return QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
            case LESS_THAN:
                return QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN;
            case LESS_THAN_OR_EQUAL_TO:
                return QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
            case LIKE:
                return QueryObjectModelConstants.JCR_OPERATOR_LIKE;
            case NOT_EQUAL_TO:
                return QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO;
        }
        assert false;
        return null;
    }
}

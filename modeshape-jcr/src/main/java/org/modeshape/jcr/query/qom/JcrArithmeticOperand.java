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
import org.modeshape.graph.query.model.ArithmeticOperand;
import org.modeshape.graph.query.model.ArithmeticOperator;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * 
 */
public class JcrArithmeticOperand extends ArithmeticOperand
    implements org.modeshape.jcr.api.query.qom.ArithmeticOperand, JcrDynamicOperand {

    private static final long serialVersionUID = 1L;

    /**
     * Create a arithmetic dynamic operand that operates upon the supplied operand(s).
     * 
     * @param left the left-hand-side operand
     * @param operator the arithmetic operator; may not be null
     * @param right the right-hand-side operand
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public JcrArithmeticOperand( JcrDynamicOperand left,
                                 ArithmeticOperator operator,
                                 JcrDynamicOperand right ) {
        super(left, operator, right);

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.ArithmeticOperand#left()
     */
    @Override
    public JcrDynamicOperand left() {
        return (JcrDynamicOperand)super.left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.ArithmeticOperand#right()
     */
    @Override
    public JcrDynamicOperand right() {
        return (JcrDynamicOperand)super.right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.ArithmeticOperand#getLeft()
     */
    @Override
    public DynamicOperand getLeft() {
        return left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.ArithmeticOperand#getRight()
     */
    @Override
    public DynamicOperand getRight() {
        return right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.ArithmeticOperand#getOperator()
     */
    @Override
    public String getOperator() {
        switch (operator()) {
            case ADD:
                return QueryObjectModelConstants.JCR_ARITHMETIC_OPERATOR_ADD;
            case DIVIDE:
                return QueryObjectModelConstants.JCR_ARITHMETIC_OPERATOR_DIVIDE;
            case MULTIPLY:
                return QueryObjectModelConstants.JCR_ARITHMETIC_OPERATOR_MULTIPLY;
            case SUBTRACT:
                return QueryObjectModelConstants.JCR_ARITHMETIC_OPERATOR_SUBTRACT;
        }
        assert false;
        return null;
    }

}

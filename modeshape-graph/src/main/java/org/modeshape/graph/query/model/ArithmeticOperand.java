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

import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A dynamic operand that represents a (binary) arithmetic operation upon one or more other operands, used in {@link Comparison}
 * and {@link Ordering} components.
 */
@Immutable
public class ArithmeticOperand implements DynamicOperand {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;
    private final ArithmeticOperator operator;
    private final DynamicOperand left;
    private final DynamicOperand right;
    private final int hc;

    /**
     * Create a arithmetic dynamic operand that operates upon the supplied operand(s).
     * 
     * @param left the left-hand-side operand
     * @param operator the arithmetic operator; may not be null
     * @param right the right-hand-side operand
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public ArithmeticOperand( DynamicOperand left,
                              ArithmeticOperator operator,
                              DynamicOperand right ) {
        CheckArg.isNotNull(operator, "operator");
        this.selectorNames = SelectorName.nameSetFrom(left.selectorNames(), right.selectorNames());
        this.operator = operator;
        this.left = left;
        this.right = right;
        this.hc = HashCode.compute(left, operator, right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.DynamicOperand#selectorNames()
     */
    public Set<SelectorName> selectorNames() {
        return selectorNames;
    }

    /**
     * Get the operator for this binary operand.
     * 
     * @return the operator; never null
     */
    public ArithmeticOperator operator() {
        return operator;
    }

    /**
     * Get the left-hand operand.
     * 
     * @return the left-hand operator; never null
     */
    public DynamicOperand left() {
        return left;
    }

    /**
     * Get the right-hand operand.
     * 
     * @return the right-hand operator; never null
     */
    public DynamicOperand right() {
        return right;
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
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ArithmeticOperand) {
            ArithmeticOperand that = (ArithmeticOperand)obj;
            return this.operator() == that.operator() && this.left().equals(that.left()) && this.right().equals(that.right());
        }
        return false;
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

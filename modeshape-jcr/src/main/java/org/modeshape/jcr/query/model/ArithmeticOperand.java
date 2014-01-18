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

import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * A dynamic operand that represents a (binary) arithmetic operation upon one or more other operands, used in {@link Comparison}
 * and {@link Ordering} components.
 */
@Immutable
public class ArithmeticOperand implements DynamicOperand, org.modeshape.jcr.api.query.qom.ArithmeticOperand {
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

    @Override
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

    @Override
    public DynamicOperand getLeft() {
        return left;
    }

    @Override
    public DynamicOperand getRight() {
        return right;
    }

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

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ArithmeticOperand) {
            ArithmeticOperand that = (ArithmeticOperand)obj;
            return this.operator() == that.operator() && this.getLeft().equals(that.getLeft())
                   && this.getRight().equals(that.getRight());
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

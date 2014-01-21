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

import javax.jcr.query.qom.QueryObjectModelConstants;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.api.query.qom.Operator;

/**
 * A constraint that evaluates to true when the defined operation evaluates to true.
 */
@Immutable
public class Comparison implements Constraint, javax.jcr.query.qom.Comparison {

    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand1;
    private final StaticOperand operand2;
    private final Operator operator;
    private final int hc;

    public Comparison( DynamicOperand operand1,
                       Operator operator,
                       StaticOperand operand2 ) {
        CheckArg.isNotNull(operand1, "operand1");
        CheckArg.isNotNull(operator, "operator");
        CheckArg.isNotNull(operand2, "operand2");
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operator = operator;
        this.hc = HashCode.compute(this.operand1, this.operand2, this.operator);
    }

    @Override
    public DynamicOperand getOperand1() {
        return operand1;
    }

    @Override
    public StaticOperand getOperand2() {
        return operand2;
    }

    /**
     * Get the operator for this comparison
     * 
     * @return the operator; never null
     */
    public final Operator operator() {
        return operator;
    }

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
        if (obj instanceof Comparison) {
            Comparison that = (Comparison)obj;
            if (this.hc != that.hc) return false;
            if (!this.operator.equals(that.operator)) return false;
            if (!this.operand1.equals(that.operand1)) return false;
            if (!this.operand2.equals(that.operand2)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

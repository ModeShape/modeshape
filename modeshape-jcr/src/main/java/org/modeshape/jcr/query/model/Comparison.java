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

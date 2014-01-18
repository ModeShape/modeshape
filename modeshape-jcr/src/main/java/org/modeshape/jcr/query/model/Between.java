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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A constraint that evaluates to true when the value defined by the dynamic operand evaluates to be within the specified range.
 */
@Immutable
public class Between implements Constraint, org.modeshape.jcr.api.query.qom.Between {

    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;
    private final StaticOperand lowerBound;
    private final StaticOperand upperBound;
    private final boolean includeLowerBound;
    private final boolean includeUpperBound;
    private final int hc;

    /**
     * Create a constraint that the values of the supplied dynamic operand are between the lower and upper bounds (inclusive).
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Between( DynamicOperand operand,
                    StaticOperand lowerBound,
                    StaticOperand upperBound ) {
        this(operand, lowerBound, upperBound, true, true);
    }

    /**
     * Create a constraint that the values of the supplied dynamic operand are between the lower and upper bounds, specifying
     * whether the boundary values are to be included in the range.
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @param includeLowerBound true if the lower boundary value is not be included
     * @param includeUpperBound true if the upper boundary value is not be included
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Between( DynamicOperand operand,
                    StaticOperand lowerBound,
                    StaticOperand upperBound,
                    boolean includeLowerBound,
                    boolean includeUpperBound ) {
        CheckArg.isNotNull(operand, "operand");
        CheckArg.isNotNull(lowerBound, "lowerBound");
        CheckArg.isNotNull(upperBound, "upperBound");
        this.operand = operand;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.includeLowerBound = includeLowerBound;
        this.includeUpperBound = includeUpperBound;
        this.hc = HashCode.compute(this.operand, this.lowerBound, this.upperBound);
    }

    @Override
    public DynamicOperand getOperand() {
        return operand;
    }

    @Override
    public StaticOperand getLowerBound() {
        return lowerBound;
    }

    @Override
    public StaticOperand getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean isLowerBoundIncluded() {
        return includeLowerBound;
    }

    @Override
    public boolean isUpperBoundIncluded() {
        return includeUpperBound;
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
        if (obj instanceof Between) {
            Between that = (Between)obj;
            if (this.hc != that.hc) return false;
            if (!this.operand.equals(that.operand)) return false;
            if (!this.lowerBound.equals(that.lowerBound)) return false;
            if (!this.upperBound.equals(that.upperBound)) return false;
            if (this.includeLowerBound != that.includeLowerBound) return false;
            if (this.includeUpperBound != that.includeUpperBound) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

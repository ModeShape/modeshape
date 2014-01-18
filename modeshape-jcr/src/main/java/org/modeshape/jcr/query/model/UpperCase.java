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

/**
 * A dynamic operand that evaluates to the upper-case representation of the supplied operand, used in a {@link Comparison}
 * constraint.
 */
@Immutable
public class UpperCase implements DynamicOperand, javax.jcr.query.qom.UpperCase {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;

    /**
     * Create a dynamic operand that evaluates to the upper-case representation of the supplied operand.
     * 
     * @param operand the operand that is to be lower-cased
     */
    public UpperCase( DynamicOperand operand ) {
        this.operand = operand;
    }

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public SelectorName selectorName() {
        return operand.selectorNames().iterator().next();
    }

    @Override
    public Set<SelectorName> selectorNames() {
        return operand.selectorNames();
    }

    @Override
    public final DynamicOperand getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return getOperand().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof UpperCase) {
            UpperCase that = (UpperCase)obj;
            return this.operand.equals(that.operand);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

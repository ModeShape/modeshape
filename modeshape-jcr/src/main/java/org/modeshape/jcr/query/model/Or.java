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
 * A constraint that evaluates to true when either of the other constraints evaluates to true.
 */
@Immutable
public class Or implements Constraint, javax.jcr.query.qom.Or {
    private static final long serialVersionUID = 1L;

    private final Constraint left;
    private final Constraint right;
    private final int hc;

    /**
     * Create a constraint that evaluates to true if either of the two supplied constraints evaluates to true.
     * 
     * @param left the left constraint
     * @param right the right constraint
     * @throws IllegalArgumentException if the left or right constraints are null
     */
    public Or( Constraint left,
               Constraint right ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        this.left = left;
        this.right = right;
        this.hc = HashCode.compute(this.left, this.right);
    }

    /**
     * Get the left-hand constraint.
     * 
     * @return the left-hand constraint; never null
     */
    public Constraint left() {
        return left;
    }

    /**
     * Get the right-hand constraint.
     * 
     * @return the right-hand constraint; never null
     */
    public Constraint right() {
        return right;
    }

    @Override
    public Constraint getConstraint1() {
        return left;
    }

    @Override
    public Constraint getConstraint2() {
        return right;
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
        if (obj instanceof Or) {
            Or that = (Or)obj;
            if (this.hc != that.hc) return false;
            return this.left.equals(that.left) && this.right.equals(that.right);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

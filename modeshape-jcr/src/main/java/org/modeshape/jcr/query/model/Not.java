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

/**
 * A constraint that negates another constraint.
 */
@Immutable
public class Not implements Constraint, javax.jcr.query.qom.Not {
    private static final long serialVersionUID = 1L;

    private final Constraint constraint;

    /**
     * Create a constraint that negates another constraint.
     * 
     * @param constraint the constraint that is being negated
     * @throws IllegalArgumentException if the supplied constraint is null
     */
    public Not( Constraint constraint ) {
        CheckArg.isNotNull(constraint, "constraint");
        this.constraint = constraint;
    }

    @Override
    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return getConstraint().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Not) {
            Not that = (Not)obj;
            return this.constraint.equals(that.constraint);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

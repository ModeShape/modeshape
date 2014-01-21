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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A constraint that evaluates to true when the defined operation evaluates to true.
 */
@Immutable
public class SetCriteria implements Constraint, org.modeshape.jcr.api.query.qom.SetCriteria {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand left;
    private final Collection<? extends StaticOperand> setOperands;

    public SetCriteria( DynamicOperand left,
                        Collection<? extends StaticOperand> setOperands ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(setOperands, "setOperands");
        CheckArg.isNotEmpty(setOperands, "setOperands");
        this.left = left;
        this.setOperands = setOperands;
    }

    public SetCriteria( DynamicOperand left,
                        StaticOperand... setOperands ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(setOperands, "setOperands");
        CheckArg.isNotEmpty(setOperands, "setOperands");
        this.left = left;
        this.setOperands = Collections.unmodifiableList(Arrays.asList(setOperands));
    }

    /**
     * Get the dynamic operand to which the set constraint applies
     * 
     * @return the dynamic operand; never null
     */
    public final DynamicOperand leftOperand() {
        return left;
    }

    /**
     * Get the collection of static operands defining the constrained values.
     * 
     * @return the right-hand-side static operands; never null and never empty
     */
    public final Collection<? extends StaticOperand> rightOperands() {
        return setOperands;
    }

    @Override
    public DynamicOperand getOperand() {
        return leftOperand();
    }

    @Override
    public Collection<? extends javax.jcr.query.qom.StaticOperand> getValues() {
        return rightOperands();
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return left.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SetCriteria) {
            SetCriteria that = (SetCriteria)obj;
            if (!this.left.equals(that.left)) return false;
            if (!this.setOperands.equals(that.setOperands)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

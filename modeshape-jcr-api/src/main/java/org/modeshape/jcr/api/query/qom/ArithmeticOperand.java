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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Ordering;

/**
 * A dynamic operand that represents a (binary) arithmetic operation upon one or more other operands, used in {@link Comparison}
 * and {@link Ordering} components.
 */
public interface ArithmeticOperand extends DynamicOperand {

    /**
     * Get the operator for this binary operand.
     * 
     * @return the operator; never null
     */
    public String getOperator();

    /**
     * Get the left-hand operand.
     * 
     * @return the left-hand operator; never null
     */
    public DynamicOperand getLeft();

    /**
     * Get the right-hand operand.
     * 
     * @return the right-hand operator; never null
     */
    public DynamicOperand getRight();
}

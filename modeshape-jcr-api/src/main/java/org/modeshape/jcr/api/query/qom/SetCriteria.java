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

import java.util.Collection;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.StaticOperand;

/**
 * A constraint that evaluates to true when the value defined by the dynamic operand evaluates to be within the set of values
 * specified by the collection of values.
 */
public interface SetCriteria extends Constraint {

    /**
     * Get the dynamic operand specification for the left-hand side of the set criteria.
     * 
     * @return the dynamic operand; never null
     */
    public DynamicOperand getOperand();

    /**
     * Get the static operands for this set criteria.
     * 
     * @return the static operand; never null and never empty
     */
    public Collection<? extends StaticOperand> getValues();
}

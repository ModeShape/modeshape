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

import javax.jcr.Node;
import javax.jcr.query.qom.Comparison;

/**
 * A dynamic operand that evaluates to the {@link Node#getIdentifier() identifier} of a node given by a selector, used in a
 * {@link Comparison} constraint.
 */
public interface NodeId extends javax.jcr.query.qom.DynamicOperand {

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public String getSelectorName();
}

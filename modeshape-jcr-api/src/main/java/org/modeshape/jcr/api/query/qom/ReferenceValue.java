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

/**
 * A dynamic operand that evaluates to the value(s) of a single or any reference property on a selector, used in a
 * {@link Comparison} constraint.
 */
public interface ReferenceValue extends DynamicOperand {
    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public String getSelectorName();

    /**
     * Get the name of the one reference property.
     * 
     * @return the property name; or null if this operand applies to any reference property
     */
    public String getPropertyName();
}

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

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.StaticOperand;

/**
 * Tests whether the property value contains a regular expression that matches the supplied operand.
 */
public interface Relike extends Constraint {

    /**
     * Get the static operand that identifies the value to match.
     *
     * @return the static operand; never null
     */
    StaticOperand getOperand1();

    /**
     * Get the specification of the property.
     *
     * @return the property value; never null
     */
    PropertyValue getOperand2();
}

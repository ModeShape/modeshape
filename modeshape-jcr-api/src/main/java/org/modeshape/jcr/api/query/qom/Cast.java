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

import javax.jcr.query.qom.DynamicOperand;

/**
 * A dynamic operand that allows casting of another dynamic operand to a certain type. 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Cast extends javax.jcr.query.qom.DynamicOperand {

    /**
     * Get the name of the desired type to cast-to.
     *
     * @return a {@link String}, never {@code null}
     */
    public String getDesiredTypeName();

    /**
     * Get the inner operand.
     *
     * @return a {@link DynamicOperand} instance, never {@code null}
     */
    public DynamicOperand getOperand();

}

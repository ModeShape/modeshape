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
 * A dynamic operand used in a {@link Comparison} constraint.
 */
@Immutable
public interface DynamicOperand extends LanguageObject, javax.jcr.query.qom.DynamicOperand {

    /**
     * Get the selector symbols to which this operand applies.
     * 
     * @return the immutable ordered set of non-null selector names used by this operand; never null and never empty
     */
    public Set<SelectorName> selectorNames();
}

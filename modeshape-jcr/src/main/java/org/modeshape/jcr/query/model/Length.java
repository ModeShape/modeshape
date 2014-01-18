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
 * A dynamic operand that evaluates to the length of the supplied propety values, used in a {@link Comparison} constraint.
 */
@Immutable
public class Length implements DynamicOperand, javax.jcr.query.qom.Length {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;
    private final PropertyValue propertyValue;

    /**
     * Create a dynamic operand that evaluates to the length of the supplied property values.
     * 
     * @param propertyValue the property value operand
     */
    public Length( PropertyValue propertyValue ) {
        this.selectorNames = SelectorName.nameSetFrom(propertyValue.selectorName());
        this.propertyValue = propertyValue;
    }

    @Override
    public Set<SelectorName> selectorNames() {
        return selectorNames;
    }

    @Override
    public final PropertyValue getPropertyValue() {
        return propertyValue;
    }

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public SelectorName selectorName() {
        return selectorNames().iterator().next();
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return getPropertyValue().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Length) {
            Length that = (Length)obj;
            return this.propertyValue.equals(that.propertyValue);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

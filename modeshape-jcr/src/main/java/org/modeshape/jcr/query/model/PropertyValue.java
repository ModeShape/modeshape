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
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A dynamic operand that evaluates to the value(s) of a property on a selector, used in a {@link Comparison} constraint.
 */
@Immutable
public class PropertyValue implements DynamicOperand, javax.jcr.query.qom.PropertyValue {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;
    private final String propertyName;
    private final int hc;

    /**
     * Create a dynamic operand that evaluates to the property values of the node identified by the selector.
     * 
     * @param selectorName the name of the selector
     * @param propertyName the name of the property
     * @throws IllegalArgumentException if the selector name or property name are null
     */
    public PropertyValue( SelectorName selectorName,
                          String propertyName ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
        CheckArg.isNotNull(propertyName, "propertyName");
        this.propertyName = propertyName;
        this.hc = HashCode.compute(selectorName, this.propertyName);
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
    public Set<SelectorName> selectorNames() {
        return selectorNames;
    }

    @Override
    public String getSelectorName() {
        return selectorName().getString();
    }

    @Override
    public final String getPropertyName() {
        return propertyName;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyValue) {
            PropertyValue that = (PropertyValue)obj;
            if (this.hc != that.hc) return false;
            return this.selectorNames().equals(that.selectorNames()) && this.propertyName.equals(that.propertyName);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

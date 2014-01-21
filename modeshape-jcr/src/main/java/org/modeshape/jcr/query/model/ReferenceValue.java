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
import org.modeshape.common.util.HashCode;

/**
 * A dynamic operand that evaluates to the value(s) of a single or any reference property on a selector, used in a
 * {@link Comparison} constraint.
 */
@Immutable
public class ReferenceValue implements DynamicOperand, org.modeshape.jcr.api.query.qom.ReferenceValue {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;
    private final String propertyName;
    private final int hc;
    private final boolean includeWeakReferences;
    private final boolean includeSimpleReferences;

    /**
     * Create a dynamic operand that evaluates to all of the reference values of the node identified by the selector.
     * 
     * @param selectorName the name of the selector
     * @throws IllegalArgumentException if the selector name is null
     */
    public ReferenceValue( SelectorName selectorName ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
        this.propertyName = null;
        this.hc = HashCode.compute(selectorName, this.propertyName);
        this.includeWeakReferences = true;
        this.includeSimpleReferences = true;
    }

    /**
     * Create a dynamic operand that evaluates to the values of a single reference property of the node identified by the
     * selector.
     * 
     *
     * @param selectorName the name of the selector
     * @param propertyName the name of the property
     * @param includeWeakReferences true if weak references are to be included
     * @param includeSimpleReferences true if simple references are to be included
     * @throws IllegalArgumentException if the selector name is null
     */
    public ReferenceValue( SelectorName selectorName,
                           String propertyName,
                           boolean includeWeakReferences,
                           boolean includeSimpleReferences ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
        this.propertyName = propertyName;
        this.hc = HashCode.compute(selectorName, this.propertyName);
        this.includeWeakReferences = includeWeakReferences;
        this.includeSimpleReferences = includeSimpleReferences;
    }

    /**
     * Create a dynamic operand that evaluates to the values of a single reference property of the node identified by the
     * selector.
     * 
     * @param selectorName the name of the selector
     * @param propertyName the name of the property
     * @throws IllegalArgumentException if the selector name is null
     */
    public ReferenceValue( SelectorName selectorName,
                           String propertyName ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
        this.propertyName = propertyName;
        this.hc = HashCode.compute(selectorName, this.propertyName);
        this.includeWeakReferences = true;
        this.includeSimpleReferences = true;
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
    public String getSelectorName() {
        return selectorName().getString();
    }

    @Override
    public Set<SelectorName> selectorNames() {
        return selectorNames;
    }

    @Override
    public final String getPropertyName() {
        return propertyName;
    }

    /**
     * Get whether weak references should be included.
     * 
     * @return true if weak references should be included, or false otherwise
     */
    public boolean includesWeakReferences() {
        return includeWeakReferences;
    }

    /**
     * Return whether simple references should be included
     *
     * @return true if simple references should be included, false otherwise.
     */
    public boolean includeSimpleReferences() {
        return includeSimpleReferences;
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
        if (obj instanceof ReferenceValue) {
            ReferenceValue that = (ReferenceValue)obj;
            if (this.hc != that.hc) return false;
            if (!this.selectorNames().equals(that.selectorNames())) return false;
            if (this.propertyName != null) {
                return this.propertyName.equals(that.propertyName);
            }
            return that.propertyName == null;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

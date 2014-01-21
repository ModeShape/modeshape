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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A constraint that evaluates to true only when a named property exists on a node.
 */
@Immutable
public class PropertyExistence implements Constraint, javax.jcr.query.qom.PropertyExistence {
    private static final long serialVersionUID = 1L;

    private final SelectorName selectorName;
    private final String propertyName;
    private final int hc;

    /**
     * Create a constraint requiring that a property exist on a node.
     * 
     * @param selectorName the name of the node selector
     * @param propertyName the name of the property that must exist
     */
    public PropertyExistence( SelectorName selectorName,
                              String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.hc = HashCode.compute(this.selectorName, this.propertyName);
    }

    /**
     * Get the name of the selector.
     * 
     * @return the selector name; never null
     */
    public final SelectorName selectorName() {
        return selectorName;
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
        if (obj instanceof PropertyExistence) {
            PropertyExistence that = (PropertyExistence)obj;
            if (this.hc != that.hc) return false;
            return this.selectorName.equals(that.selectorName) && this.propertyName.equals(that.propertyName);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

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
import javax.jcr.Node;
import org.modeshape.common.annotation.Immutable;

/**
 * A dynamic operand that evaluates to the {@link Node#getIdentifier() identifier} of a node given by a selector, used in a
 * {@link Comparison} constraint.
 */
@Immutable
public class NodeId implements DynamicOperand, org.modeshape.jcr.api.query.qom.NodeId {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;

    /**
     * Create a dynamic operand that evaluates to the {@link Node#getIdentifier() identifier} of the node described by the
     * selector.
     * 
     * @param selectorName the name of the selector
     * @throws IllegalArgumentException if the selector name or property name are null
     */
    public NodeId( SelectorName selectorName ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
    }

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public SelectorName selectorName() {
        return selectorNames.iterator().next();
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
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return selectorNames().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeId) {
            NodeId that = (NodeId)obj;
            return this.selectorNames().equals(that.selectorNames());
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

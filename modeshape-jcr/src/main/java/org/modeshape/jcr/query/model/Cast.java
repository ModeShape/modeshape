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
 * Implementation of the {@link org.modeshape.jcr.api.query.qom.Cast} operand.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
public final class Cast implements DynamicOperand, org.modeshape.jcr.api.query.qom.Cast {

    private final DynamicOperand operand;
    private final String desiredTypeName;

    /**
     * Creates a new operand instance, wrapping another operand and the desired type.
     *
     * @param operand a {@link DynamicOperand} instance, never {@code null}
     * @param desiredTypeName a {@link String}, the name of the desired type to cast to; never null
     */
    public Cast( DynamicOperand operand, String desiredTypeName ) {
        this.operand = operand;
        this.desiredTypeName = desiredTypeName;
    }

    /**
     * Get the selector symbol upon which this operand applies.
     *
     * @return the one selector names used by this operand; never null
     */
    public SelectorName selectorName() {
        return operand.selectorNames().iterator().next();
    }

    @Override
    public String getDesiredTypeName() {
        return desiredTypeName;
    }

    @Override
    public Set<SelectorName> selectorNames() {
        return operand.selectorNames();
    }

    /**
     * Get the inner operand.
     * 
     * @return a {@link DynamicOperand} instance, never {@code null}
     */
    public DynamicOperand getOperand() {
        return operand;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);    
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return getOperand().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Cast) {
            Cast that = (Cast)obj;
            return this.operand.equals(that.operand);
        }
        return false;
    }

}

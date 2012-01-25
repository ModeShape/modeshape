/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.query.model;

import java.util.Set;
import org.modeshape.common.annotation.Immutable;

/**
 * A dynamic operand that evaluates to the lower-case representation of the supplied operand, used in a {@link Comparison}
 * constraint.
 */
@Immutable
public class LowerCase implements DynamicOperand, javax.jcr.query.qom.LowerCase {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;

    /**
     * Create a dynamic operand that evaluates to the lower-case representation of the supplied operand.
     * 
     * @param operand the operand that is to be lower-cased
     */
    public LowerCase( DynamicOperand operand ) {
        this.operand = operand;
    }

    @Override
    public final DynamicOperand getOperand() {
        return operand;
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
    public Set<SelectorName> selectorNames() {
        return operand.selectorNames();
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return operand.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof LowerCase) {
            LowerCase that = (LowerCase)obj;
            return this.operand.equals(that.operand);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

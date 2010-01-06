/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.model;

import net.jcip.annotations.Immutable;

/**
 * A dynamic operand that evaluates to the upper-case representation of the supplied operand, used in a {@link Comparison}
 * constraint.
 */
@Immutable
public class UpperCase extends DynamicOperand {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;

    /**
     * Create a dynamic operand that evaluates to the upper-case representation of the supplied operand.
     * 
     * @param operand the operand that is to be lower-cased
     */
    public UpperCase( DynamicOperand operand ) {
        super(operand);
        this.operand = operand;
    }

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public SelectorName getSelectorName() {
        return getSelectorNames().iterator().next();
    }

    /**
     * Get the operand that is being uppercased.
     * 
     * @return the operand being uppercased; never null
     */
    public final DynamicOperand getOperand() {
        return operand;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getOperand().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof UpperCase) {
            UpperCase that = (UpperCase)obj;
            return this.operand.equals(that.operand);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitable#accept(org.jboss.dna.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

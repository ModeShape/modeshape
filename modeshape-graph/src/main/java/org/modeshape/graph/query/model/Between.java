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
package org.modeshape.graph.query.model;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A constraint that evaluates to true when the value defined by the dynamic operand evaluates to be within the specified range.
 */
@Immutable
public class Between implements Constraint {

    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;
    private final StaticOperand lowerBound;
    private final StaticOperand upperBound;
    private final boolean includeLowerBound;
    private final boolean includeUpperBound;
    private final int hc;

    /**
     * Create a constraint that the values of the supplied dynamic operand are between the lower and upper bounds (inclusive).
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Between( DynamicOperand operand,
                    StaticOperand lowerBound,
                    StaticOperand upperBound ) {
        this(operand, lowerBound, upperBound, true, true);
    }

    /**
     * Create a constraint that the values of the supplied dynamic operand are between the lower and upper bounds, specifying
     * whether the boundary values are to be included in the range.
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @param includeLowerBound true if the lower boundary value is not be included
     * @param includeUpperBound true if the upper boundary value is not be included
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Between( DynamicOperand operand,
                    StaticOperand lowerBound,
                    StaticOperand upperBound,
                    boolean includeLowerBound,
                    boolean includeUpperBound ) {
        CheckArg.isNotNull(operand, "operand");
        CheckArg.isNotNull(lowerBound, "lowerBound");
        CheckArg.isNotNull(upperBound, "upperBound");
        this.operand = operand;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.includeLowerBound = includeLowerBound;
        this.includeUpperBound = includeUpperBound;
        this.hc = HashCode.compute(this.operand, this.lowerBound, this.upperBound);
    }

    /**
     * Get the dynamic operand specification.
     * 
     * @return the dynamic operand; never null
     */
    public DynamicOperand operand() {
        return operand;
    }

    /**
     * Get the lower bound operand.
     * 
     * @return the lower bound; never null
     */
    public StaticOperand lowerBound() {
        return lowerBound;
    }

    /**
     * Get the upper bound operand.
     * 
     * @return the upper bound; never null
     */
    public StaticOperand upperBound() {
        return upperBound;
    }

    /**
     * Return whether the lower bound is to be included in the results.
     * 
     * @return true if the {@link #lowerBound() lower bound} is to be included, or false otherwise
     */
    public boolean isLowerBoundIncluded() {
        return includeLowerBound;
    }

    /**
     * Return whether the upper bound is to be included in the results.
     * 
     * @return true if the {@link #upperBound() upper bound} is to be included, or false otherwise
     */
    public boolean isUpperBoundIncluded() {
        return includeUpperBound;
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
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Between) {
            Between that = (Between)obj;
            if (this.hc != that.hc) return false;
            if (!this.operand.equals(that.operand)) return false;
            if (!this.lowerBound.equals(that.lowerBound)) return false;
            if (!this.upperBound.equals(that.upperBound)) return false;
            if (this.includeLowerBound != that.includeLowerBound) return false;
            if (this.includeUpperBound != that.includeUpperBound) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A constraint that evaluates to true when either of the other constraints evaluates to true.
 */
@Immutable
public class Or implements Constraint, javax.jcr.query.qom.Or {
    private static final long serialVersionUID = 1L;

    private final Constraint left;
    private final Constraint right;
    private final int hc;

    /**
     * Create a constraint that evaluates to true if either of the two supplied constraints evaluates to true.
     * 
     * @param left the left constraint
     * @param right the right constraint
     * @throws IllegalArgumentException if the left or right constraints are null
     */
    public Or( Constraint left,
               Constraint right ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        this.left = left;
        this.right = right;
        this.hc = HashCode.compute(this.left, this.right);
    }

    /**
     * Get the left-hand constraint.
     * 
     * @return the left-hand constraint; never null
     */
    public Constraint left() {
        return left;
    }

    /**
     * Get the right-hand constraint.
     * 
     * @return the right-hand constraint; never null
     */
    public Constraint right() {
        return right;
    }

    @Override
    public Constraint getConstraint1() {
        return left;
    }

    @Override
    public Constraint getConstraint2() {
        return right;
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
        if (obj instanceof Or) {
            Or that = (Or)obj;
            if (this.hc != that.hc) return false;
            return this.left.equals(that.left) && this.right.equals(that.right);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

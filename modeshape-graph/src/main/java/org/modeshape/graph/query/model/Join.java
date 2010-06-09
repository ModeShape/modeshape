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
 * 
 */
@Immutable
public class Join implements Source {
    private static final long serialVersionUID = 1L;

    private final Source left;
    private final Source right;
    private final JoinType type;
    private final JoinCondition joinCondition;
    private final int hc;

    /**
     * Create a join of the left and right sources, using the supplied join condition. The outputs of the left and right sources
     * are expected to be equivalent.
     * 
     * @param left the left source being joined
     * @param type the type of join
     * @param right the right source being joined
     * @param joinCondition the join condition
     */
    public Join( Source left,
                 JoinType type,
                 Source right,
                 JoinCondition joinCondition ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        CheckArg.isNotNull(type, "type");
        CheckArg.isNotNull(joinCondition, "joinCondition");
        this.left = left;
        this.right = right;
        this.type = type;
        this.joinCondition = joinCondition;
        this.hc = HashCode.compute(this.left, this.right, this.type, this.joinCondition);
    }

    /**
     * Get the source that represents the left-hand-side of the join.
     * 
     * @return the left-side source; never null
     */
    public Source left() {
        return left;
    }

    /**
     * Get the source that represents the right-hand-side of the join.
     * 
     * @return the right-side source; never null
     */
    public Source right() {
        return right;
    }

    /**
     * Get the type of join.
     * 
     * @return the join type; never null
     */
    public final JoinType type() {
        return type;
    }

    /**
     * Get the join condition
     * 
     * @return the join condition; never null
     */
    public JoinCondition joinCondition() {
        return joinCondition;
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
        if (obj instanceof Join) {
            Join that = (Join)obj;
            if (this.hc != that.hc) return false;
            if (!this.type.equals(that.type)) return false;
            if (!this.left.equals(that.left)) return false;
            if (!this.right.equals(that.right)) return false;
            if (!this.joinCondition.equals(that.joinCondition)) return false;
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

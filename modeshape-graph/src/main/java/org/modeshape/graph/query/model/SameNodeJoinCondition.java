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
import org.modeshape.common.util.ObjectUtil;

/**
 * A join condition that tests whether two nodes are the same nodes (that is, have the same identifier or have the same relative
 * path from the nearest ancestor with an identifiers).
 */
@Immutable
public class SameNodeJoinCondition implements JoinCondition {
    private static final long serialVersionUID = 1L;

    private final SelectorName selector1Name;
    private final SelectorName selector2Name;
    private final String selector2Path;
    private final int hc;

    /**
     * Create a join condition that determines whether the node identified by the first selector is the same as the node at the
     * given path relative to the node identified by the second selector.
     * 
     * @param selector1Name the name of the first selector
     * @param selector2Name the name of the second selector
     * @param selector2Path the relative path from the second selector locating the node being compared with the first selector
     * @throws IllegalArgumentException if the path or either selector name is null
     */
    public SameNodeJoinCondition( SelectorName selector1Name,
                                  SelectorName selector2Name,
                                  String selector2Path ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        CheckArg.isNotNull(selector2Path, "selector2Path");
        this.selector1Name = selector1Name;
        this.selector2Name = selector2Name;
        this.selector2Path = selector2Path;
        this.hc = HashCode.compute(this.selector1Name, this.selector2Name, this.selector2Path);
    }

    /**
     * Create a join condition that determines whether the node identified by the first selector is the same as the node
     * identified by the second selector.
     * 
     * @param selector1Name the name of the first selector
     * @param selector2Name the name of the second selector
     * @throws IllegalArgumentException if either selector name is null
     */
    public SameNodeJoinCondition( SelectorName selector1Name,
                                  SelectorName selector2Name ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        this.selector1Name = selector1Name;
        this.selector2Name = selector2Name;
        this.selector2Path = null;
        this.hc = HashCode.compute(this.selector1Name, this.selector2Name, this.selector2Path);
    }

    /**
     * Get the selector name for the first side of the join condition.
     * 
     * @return the name of the first selector; never null
     */
    public final SelectorName selector1Name() {
        return selector1Name;
    }

    /**
     * Get the selector name for the second side of the join condition.
     * 
     * @return the name of the second selector; never null
     */
    public final SelectorName selector2Name() {
        return selector2Name;
    }

    /**
     * Get the path for the node being used, relative to the second selector.
     * 
     * @return the relative path to the node; may be null if the second selector is the node being used
     */
    public final String selector2Path() {
        return selector2Path;
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
        if (obj instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition that = (SameNodeJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.selector1Name.equals(that.selector1Name)) return false;
            if (!this.selector2Name.equals(that.selector2Name)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.selector2Path, that.selector2Path)) return false;
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

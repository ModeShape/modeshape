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
 * A join condition that evaluates to true only when the named child node is indeed a child of the named parent node.
 */
@Immutable
public class ChildNodeJoinCondition implements JoinCondition {
    private static final long serialVersionUID = 1L;

    private final SelectorName childSelectorName;
    private final SelectorName parentSelectorName;
    private final int hc;

    /**
     * Create a join condition that determines whether the node identified by the child selector is a child of the node identified
     * by the parent selector.
     * 
     * @param parentSelectorName the first selector
     * @param childSelectorName the second selector
     */
    public ChildNodeJoinCondition( SelectorName parentSelectorName,
                                   SelectorName childSelectorName ) {
        CheckArg.isNotNull(childSelectorName, "childSelectorName");
        CheckArg.isNotNull(parentSelectorName, "parentSelectorName");
        this.childSelectorName = childSelectorName;
        this.parentSelectorName = parentSelectorName;
        this.hc = HashCode.compute(this.childSelectorName, this.parentSelectorName);
    }

    /**
     * Get the name of the selector that represents the child.
     * 
     * @return the selector name of the child node; never null
     */
    public final SelectorName childSelectorName() {
        return childSelectorName;
    }

    /**
     * Get the name of the selector that represents the parent.
     * 
     * @return the selector name of the parent node; never null
     */
    public final SelectorName parentSelectorName() {
        return parentSelectorName;
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
        if (obj instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition that = (ChildNodeJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.childSelectorName.equals(that.childSelectorName)) return false;
            if (!this.parentSelectorName.equals(that.parentSelectorName)) return false;
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

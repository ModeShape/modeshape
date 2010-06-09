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
 * A join condition that evaluates to true only when the named node is a descendant of another named node.
 */
@Immutable
public class DescendantNodeJoinCondition implements JoinCondition {
    private static final long serialVersionUID = 1L;

    private final SelectorName descendantSelectorName;
    private final SelectorName ancestorSelectorName;
    private final int hc;

    /**
     * Create a join condition that determines whether the node identified by the descendant selector is indeed a descendant of
     * the node identified by the ancestor selector.
     * 
     * @param ancestorSelectorName the name of the ancestor selector
     * @param descendantSelectorName the name of the descendant selector
     */
    public DescendantNodeJoinCondition( SelectorName ancestorSelectorName,
                                        SelectorName descendantSelectorName ) {
        CheckArg.isNotNull(descendantSelectorName, "descendantSelectorName");
        CheckArg.isNotNull(ancestorSelectorName, "ancestorSelectorName");
        this.descendantSelectorName = descendantSelectorName;
        this.ancestorSelectorName = ancestorSelectorName;
        this.hc = HashCode.compute(this.descendantSelectorName, this.ancestorSelectorName);
    }

    /**
     * Get the name of the selector for the descedant node.
     * 
     * @return the selector name of the descendant node; never null
     */
    public final SelectorName descendantSelectorName() {
        return descendantSelectorName;
    }

    /**
     * Get the name of the selector for the ancestor node.
     * 
     * @return the selector name of the ancestor node; never null
     */
    public final SelectorName ancestorSelectorName() {
        return ancestorSelectorName;
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
        if (obj instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition that = (DescendantNodeJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.descendantSelectorName.equals(that.descendantSelectorName)) return false;
            if (!this.ancestorSelectorName.equals(that.ancestorSelectorName)) return false;
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

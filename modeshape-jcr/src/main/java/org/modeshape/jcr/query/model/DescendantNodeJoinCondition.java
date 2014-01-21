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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A join condition that evaluates to true only when the named node is a descendant of another named node.
 */
@Immutable
public class DescendantNodeJoinCondition implements JoinCondition, javax.jcr.query.qom.DescendantNodeJoinCondition {
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

    @Override
    public String getAncestorSelectorName() {
        return ancestorSelectorName.getString();
    }

    @Override
    public String getDescendantSelectorName() {
        return descendantSelectorName.getString();
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
        if (obj instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition that = (DescendantNodeJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.descendantSelectorName.equals(that.descendantSelectorName)) return false;
            if (!this.ancestorSelectorName.equals(that.ancestorSelectorName)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

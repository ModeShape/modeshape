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
 * A join condition that evaluates to true only when the named child node is indeed a child of the named parent node.
 */
@Immutable
public class ChildNodeJoinCondition implements JoinCondition, javax.jcr.query.qom.ChildNodeJoinCondition {
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

    @Override
    public String getChildSelectorName() {
        return childSelectorName.getString();
    }

    @Override
    public String getParentSelectorName() {
        return parentSelectorName.getString();
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
        if (obj instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition that = (ChildNodeJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.childSelectorName.equals(that.childSelectorName)) return false;
            if (!this.parentSelectorName.equals(that.parentSelectorName)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

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
import org.modeshape.common.util.ObjectUtil;

/**
 * A join condition that tests whether two nodes are the same nodes (that is, have the same identifier or have the same relative
 * path from the nearest ancestor with an identifiers).
 */
@Immutable
public class SameNodeJoinCondition implements JoinCondition, javax.jcr.query.qom.SameNodeJoinCondition {
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

    @Override
    public String getSelector1Name() {
        return selector1Name().getString();
    }

    @Override
    public String getSelector2Name() {
        return selector2Name().getString();
    }

    @Override
    public final String getSelector2Path() {
        return selector2Path;
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

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

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
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * 
 */
@Immutable
public class Join implements Source, javax.jcr.query.qom.Join {
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

    @Override
    public Source getLeft() {
        return left;
    }

    @Override
    public Source getRight() {
        return right;
    }

    @Override
    public String getJoinType() {
        switch (type()) {
            case CROSS:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_CROSS;
            case INNER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_INNER;
            case FULL_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_FULL_OUTER;
            case LEFT_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER;
            case RIGHT_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER;
        }
        assert false;
        return null;
    }

    /**
     * Get the type of join.
     * 
     * @return the join type; never null
     */
    public final JoinType type() {
        return type;
    }

    @Override
    public JoinCondition getJoinCondition() {
        return joinCondition;
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

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

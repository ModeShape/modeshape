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
 * A join condition that tests whether a property on a node is equal to a property on another node. A node-tuple satisfies the
 * constraint only if:
 * <ul>
 * <li>the {@code selector1Name} node has a property named {@code property1Name}, and</li>
 * <li>the {@code selector2Name} node has a property named {@code property2Name}, and</li>
 * <li>the value of property {@code property1Name} is equal to the value of property {@code property2Name}</li>
 * </ul>
 */
@Immutable
public class EquiJoinCondition implements JoinCondition, javax.jcr.query.qom.EquiJoinCondition {
    private static final long serialVersionUID = 1L;

    private final SelectorName selector1Name;
    private final String property1Name;
    private final SelectorName selector2Name;
    private final String property2Name;
    private final int hc;

    /**
     * Create an equi-join condition, given the names of the selector and property for the left- and right-hand-side of the join.
     * 
     * @param selector1Name the selector name appearing on the left-side of the join; never null
     * @param property1Name the property name for the left-side of the join; never null
     * @param selector2Name the selector name appearing on the right-side of the join; never null
     * @param property2Name the property name for the right-side of the join; never null
     */
    public EquiJoinCondition( SelectorName selector1Name,
                              String property1Name,
                              SelectorName selector2Name,
                              String property2Name ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(property1Name, "property1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        CheckArg.isNotNull(property2Name, "property2Name");
        this.selector1Name = selector1Name;
        this.property1Name = property1Name;
        this.selector2Name = selector2Name;
        this.property2Name = property2Name;
        this.hc = HashCode.compute(this.selector1Name, this.property1Name, this.selector2Name, this.property2Name);
    }

    /**
     * Create an equi-join condition, given the columns.
     * 
     * @param column1 the column for the left-side of the join; never null
     * @param column2 the column for the right-side of the join; never null
     */
    public EquiJoinCondition( Column column1,
                              Column column2 ) {
        this(column1.selectorName(), column1.getPropertyName(), column2.selectorName(), column2.getPropertyName());
    }

    /**
     * Get the name of the selector that appears on the left-side of the join.
     * 
     * @return the selector name appearing on the left-side of the join; never null
     */
    public final SelectorName selector1Name() {
        return selector1Name;
    }

    /**
     * Get the name of the selector that appears on the right-side of the join.
     * 
     * @return the selector name appearing on the right-side of the join; never null
     */
    public final SelectorName selector2Name() {
        return selector2Name;
    }

    @Override
    public String getSelector1Name() {
        return selector1Name.getString();
    }

    @Override
    public String getSelector2Name() {
        return selector2Name.getString();
    }

    @Override
    public final String getProperty1Name() {
        return property1Name;
    }

    @Override
    public final String getProperty2Name() {
        return property2Name;
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
        if (obj instanceof EquiJoinCondition) {
            EquiJoinCondition that = (EquiJoinCondition)obj;
            if (this.hc != that.hc) return false;
            if (!this.selector1Name.equals(that.selector1Name)) return false;
            if (!this.selector2Name.equals(that.selector2Name)) return false;
            if (!this.property1Name.equals(that.property1Name)) return false;
            if (!this.property2Name.equals(that.property2Name)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

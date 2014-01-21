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

/**
 * A constraint requiring that the selected node is a descendant of the node reachable by the supplied absolute path
 */
@Immutable
public class DescendantNode implements Constraint, javax.jcr.query.qom.DescendantNode {
    private static final long serialVersionUID = 1L;

    private final SelectorName selectorName;
    private final String ancestorPath;

    /**
     * Create a constraint requiring that the node identified by the selector is a descendant of the node reachable by the
     * supplied absolute path.
     * 
     * @param selectorName the name of the selector
     * @param ancestorPath the absolute path to the ancestor
     */
    public DescendantNode( SelectorName selectorName,
                           String ancestorPath ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(ancestorPath, "ancestorPath");
        this.selectorName = selectorName;
        this.ancestorPath = ancestorPath;
    }

    /**
     * Get the name of the selector for the node.
     * 
     * @return the selector name; never null
     */
    public final SelectorName selectorName() {
        return selectorName;
    }

    @Override
    public String getSelectorName() {
        return selectorName.getString();
    }

    @Override
    public final String getAncestorPath() {
        return ancestorPath;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return selectorName().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof DescendantNode) {
            DescendantNode that = (DescendantNode)obj;
            if (!this.selectorName.equals(that.selectorName)) return false;
            if (!this.ancestorPath.equals(that.ancestorPath)) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

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
import org.modeshape.common.util.ObjectUtil;

/**
 * A selector that represents a source that returns all nodes.
 */
@Immutable
public class AllNodes extends Selector {

    private static final long serialVersionUID = 1L;

    public static final SelectorName ALL_NODES_NAME = new SelectorName("__ALLNODES__");

    public AllNodes() {
        super(ALL_NODES_NAME);
    }

    /**
     * Create a selector with the supplied alias.
     * 
     * @param alias the alias for this selector; may be null
     */
    public AllNodes( SelectorName alias ) {
        super(ALL_NODES_NAME, alias);
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Selector) {
            Selector that = (Selector)obj;
            return ObjectUtil.isEqualWithNulls(this.name(), that.name())
                   && ObjectUtil.isEqualWithNulls(this.alias(), that.alias());
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

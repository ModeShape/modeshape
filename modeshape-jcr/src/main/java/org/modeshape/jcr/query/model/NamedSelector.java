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
 * A Selector that has a name.
 */
@Immutable
public class NamedSelector extends Selector {
    private static final long serialVersionUID = 1L;

    /**
     * Create a selector with a name.
     * 
     * @param name the name for this selector
     * @throws IllegalArgumentException if the selector name is null
     */
    public NamedSelector( SelectorName name ) {
        super(name);
    }

    /**
     * Create a selector with the supplied name and alias.
     * 
     * @param name the name for this selector
     * @param alias the alias for this selector; may be null
     * @throws IllegalArgumentException if the selector name is null
     */
    public NamedSelector( SelectorName name,
                          SelectorName alias ) {
        super(name, alias);
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
            if (!this.name().equals(that.name())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.alias(), that.alias())) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}

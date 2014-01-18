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
 * 
 */
@Immutable
public abstract class Selector implements Source, javax.jcr.query.qom.Selector {
    private static final long serialVersionUID = 1L;

    private final SelectorName name;
    private final SelectorName alias;

    /**
     * Create a selector with a name.
     * 
     * @param name the name for this selector
     * @throws IllegalArgumentException if the selector name is null
     */
    protected Selector( SelectorName name ) {
        CheckArg.isNotNull(name, "name");
        this.name = name;
        this.alias = null;
    }

    /**
     * Create a selector with the supplied name and alias.
     * 
     * @param name the name for this selector
     * @param alias the alias for this selector; may be null
     * @throws IllegalArgumentException if the selector name is null
     */
    protected Selector( SelectorName name,
                        SelectorName alias ) {
        CheckArg.isNotNull(name, "name");
        this.name = name;
        this.alias = alias;
    }

    /**
     * Get the name for this selector.
     * 
     * @return the selector name; never null
     */
    public SelectorName name() {
        return name;
    }

    /**
     * Get the alias name for this source, if there is one.
     * 
     * @return the alias name, or null if there is none.
     */
    public SelectorName alias() {
        return alias;
    }

    /**
     * Get the alias if this selector has one, or the name.
     * 
     * @return the alias or name; never null
     */
    public SelectorName aliasOrName() {
        return alias != null ? alias : name;
    }

    /**
     * Determine if this selector has an alias.
     * 
     * @return true if this selector has an alias, or false otherwise.
     */
    public boolean hasAlias() {
        return alias != null;
    }

    @Override
    public String getNodeTypeName() {
        return name().getString();
    }

    @Override
    public String getSelectorName() {
        return alias().name();
    }

}

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

/**
 * 
 */
@Immutable
public abstract class Selector implements Source {
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
}

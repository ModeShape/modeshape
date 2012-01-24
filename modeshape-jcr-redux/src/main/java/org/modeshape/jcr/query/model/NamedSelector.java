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

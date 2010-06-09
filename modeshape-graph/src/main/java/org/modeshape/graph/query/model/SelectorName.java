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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A representation of a qualified or expanded name.
 */
@Immutable
public class SelectorName implements Readable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;

    public SelectorName( String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
    }

    /**
     * The raw name of the selector.
     * 
     * @return the raw name; never null and never empty
     */
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Readable#getString()
     */
    public String getString() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SelectorName) {
            SelectorName that = (SelectorName)obj;
            return this.name.equals(that.name());
        }
        return false;
    }

    public static Set<SelectorName> nameSetFrom( SelectorName name ) {
        return Collections.singleton(name);
    }

    public static Set<SelectorName> nameSetFrom( SelectorName firstName,
                                                 SelectorName... names ) {
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.add(firstName);
        for (SelectorName name : names) {
            if (name != null) result.add(name);
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<SelectorName> nameSetFrom( Set<SelectorName> firstSet,
                                                 Set<SelectorName> secondSet ) {
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.addAll(firstSet);
        if (secondSet != null) result.addAll(secondSet);
        return Collections.unmodifiableSet(result);
    }
}

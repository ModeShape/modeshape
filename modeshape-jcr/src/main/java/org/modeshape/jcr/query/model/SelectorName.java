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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
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

    @Override
    public String getString() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SelectorName) {
            SelectorName that = (SelectorName)obj;
            return this.name.equals(that.name());
        }
        return false;
    }

    /**
     * Create a set that contains the supplied SelectName object.
     * 
     * @param name the name to include in the set; may be null
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( SelectorName name ) {
        if (name == null) return Collections.emptySet();
        return Collections.singleton(name);
    }

    /**
     * Create a set that contains the supplied SelectName object.
     * 
     * @param firstName the first name; may be null
     * @param names the remaining names; may be null
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( SelectorName firstName,
                                                 SelectorName... names ) {
        if (firstName == null && (names == null || names.length == 0)) {
            return Collections.emptySet();
        }
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.add(firstName);
        for (SelectorName name : names) {
            if (name != null) result.add(name);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Create a set that contains the SelectName objects in the supplied sets.
     * 
     * @param firstSet the first set of names; may be null or empty
     * @param secondSet the second set of names; may be null or empty
     * @return the set; never null
     */
    public static Set<SelectorName> nameSetFrom( Set<SelectorName> firstSet,
                                                 Set<SelectorName> secondSet ) {
        if ((firstSet == null || firstSet.isEmpty()) && (secondSet == null || secondSet.isEmpty())) {
            return Collections.emptySet();
        }
        Set<SelectorName> result = new LinkedHashSet<SelectorName>();
        result.addAll(firstSet);
        if (secondSet != null) result.addAll(secondSet);
        return Collections.unmodifiableSet(result);
    }
}

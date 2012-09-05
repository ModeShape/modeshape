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
package org.modeshape.sequencer.teiid;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.util.CheckArg;

/**
 * A simple POJO that is used to represent the information for an overridden translator read in from a VDB manifest ("vdb.xml").
 */
public class VdbTranslator implements Comparable<VdbTranslator> {

    private String description;
    private final String name;
    private final String type;
    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * @param name the translator override name (cannot be <code>null</code> or empty)
     * @param type the translator type being overridden (cannot be <code>null</code> or empty)
     */
    public VdbTranslator( final String name,
                          final String type ) {
        CheckArg.isNotEmpty(name, "name");
        CheckArg.isNotEmpty(type, "type");

        this.name = name;
        this.type = type;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final VdbTranslator that ) {
        CheckArg.isNotNull(that, "that");

        if (this == that) {
            return 0;
        }

        // order by name
        return this.name.compareTo(that.name);
    }

    /**
     * @return the description (never <code>null</code> but can be empty)
     */
    public String getDescription() {
        return ((this.description == null) ? "" : this.description);
    }

    /**
     * @return the translator override name (never <code>null</code> or empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the overridden properties (never <code>null</code>)
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * @return the type of translator being overridden (never <code>null</code> or empty)
     */
    public String getType() {
        return this.type;
    }

    /**
     * @param newValue the new description value (can be <code>null</code> or empty)
     */
    public void setDescription( final String newValue ) {
        this.description = newValue;
    }
}

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
package org.modeshape.common.util;

import java.util.List;
import java.util.Map;

/**
 * Interface that defines methods to work with Java bean property representations.
 */
public interface Reflective {

    /**
     * Get the representation of the Java property with the supplied name on this object.
     * 
     * @param propertyName the property name; may not be null
     * @return the property representation, or null if there is no property with that name
     */
    public ObjectProperty getObjectProperty( String propertyName );

    /**
     * Set the Java property on this object.
     * 
     * @param property the new property
     * @return true if the property was successfully set, or false otherwise
     */
    public boolean setObjectProperty( ObjectProperty property );

    /**
     * Get representations for all of the Java properties on this object.
     * 
     * @return the list of all properties; never null
     */
    public List<ObjectProperty> getAllObjectProperties();

    /**
     * Get representations for all of the Java properties on this object.
     * 
     * @return the map of all properties keyed by their name; never null
     */
    public Map<String, ObjectProperty> getAllObjectPropertiesByName();
}

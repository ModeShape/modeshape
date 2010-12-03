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
package org.modeshape.graph.property;

import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * A factory for creating {@link Property} objects.
 */
@ThreadSafe
public interface PropertyFactory {
    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Object... values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterable<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterator<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Object... values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterable<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterator<?> values );

    /**
     * Create a property with the supplied name and {@link Path} value. This method is provided because Path implements
     * Iterable&lt;Segment>.
     * 
     * @param name the property name; may not be null
     * @param value the path value
     * @return the resulting property
     */
    Property create( Name name,
                     Path value );
}

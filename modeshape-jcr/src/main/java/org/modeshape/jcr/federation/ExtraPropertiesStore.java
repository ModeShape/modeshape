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
package org.modeshape.jcr.federation;

import java.util.Collections;
import java.util.Map;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Store for extra properties, which a {@link Connector} implementation can use to store and retrieve "extra" properties on a node
 * that cannot be persisted in the external system. Generally, a connector should store as much as possible in the external
 * system. However, not all systems are capable of persisting any and all properties that a JCR client may put on a node. In such
 * cases, the connector can store these "extra" properties (that it does not persist) in this properties store.
 */
public interface ExtraPropertiesStore {

    public static final Map<Name, Property> NO_PROPERTIES = Collections.emptyMap();

    /**
     * Store the supplied extra properties for the node with the supplied ID. This will overwrite any properties that were
     * previously stored for the node with the specified ID.
     * 
     * @param id the identifier for the node; may not be null
     * @param properties the extra properties for the node that should be stored in this storage area, keyed by their name
     */
    void storeProperties( String id,
                          Map<Name, Property> properties );

    /**
     * Update the supplied extra properties for the node with the supplied ID.
     * 
     * @param id the identifier for the node; may not be null
     * @param properties the extra properties for the node that should be stored in this storage area, keyed by their name; any
     *        entry that contains a null Property will define a property that should be removed
     */
    void updateProperties( String id,
                           Map<Name, Property> properties );

    /**
     * Retrieve the extra properties that were stored for the node with the supplied ID.
     * 
     * @param id the identifier for the node; may not be null
     * @return the map of properties keyed by their name; may be empty, but never null
     */
    Map<Name, Property> getProperties( String id );

    /**
     * Remove all of the extra properties that were stored for the node with the supplied ID.
     * 
     * @param id the identifier for the node; may not be null
     * @return true if there were properties stored for the node and now removed, or false if there were no extra properties
     *         stored for the node with the supplied key
     */
    boolean removeProperties( String id );

}

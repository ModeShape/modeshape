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
package org.modeshape.jcr.spi.federation;

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

    static final Map<Name, Property> NO_PROPERTIES = Collections.emptyMap();

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

    /**
     * Check if this store contains any extra properties for the node with the given ID.
     *
     * @param id the identifier for the node; may not be null
     * @return {@code true} if this store contains extra properties, {@code false} otherwise.
     */
    boolean contains(String id);

}

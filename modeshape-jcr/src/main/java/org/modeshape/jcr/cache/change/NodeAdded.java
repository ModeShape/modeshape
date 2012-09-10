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
package org.modeshape.jcr.cache.change;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * 
 */
public class NodeAdded extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;
    private static final Map<Name, Property> EMPTY_PROPERTIES = Collections.emptyMap();

    private final NodeKey parentKey;
    private final Map<Name, Property> properties;

    public NodeAdded( NodeKey key,
                      NodeKey parentKey,
                      Path path,
                      Map<Name, Property> properties ) {
        super(key, path);
        this.parentKey = parentKey;
        assert this.parentKey != null;
        if (properties == null || properties.isEmpty()) {
            this.properties = EMPTY_PROPERTIES;
        } else {
            this.properties = Collections.unmodifiableMap(new HashMap<Name, Property>(properties));
        }
    }

    /**
     * Get the key for the parent under which the new node was added.
     * 
     * @return the key for the parent; never null
     */
    public NodeKey getParentKey() {
        return parentKey;
    }

    /**
     * Get the immutable map of properties that were added as part of this node.
     * 
     * @return the properties keyed by their name; never null but possibly empty
     */
    public Map<Name, Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Added node '" + this.getKey() + "' at \"" + path + "\" under '" + parentKey + "'";
    }
}

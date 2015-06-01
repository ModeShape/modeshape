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
package org.modeshape.jcr.cache.change;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * Change representing the addition of a node.
 */
public class NodeAdded extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;
    private static final Map<Name, Property> EMPTY_PROPERTIES = Collections.emptyMap();

    private final NodeKey parentKey;
    private final Map<Name, Property> properties;

    public NodeAdded( NodeKey key,
                      NodeKey parentKey,
                      Path path,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      Map<Name, Property> properties ) {
        super(key, path, primaryType, mixinTypes);
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

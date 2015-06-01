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

import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Change representing the removal of a node.
 */
public class NodeRemoved extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final NodeKey parentKey;
    private final Name parentPrimaryType;
    private final Set<Name> parentMixinTypes;

    public NodeRemoved( NodeKey key,
                        NodeKey parentKey,
                        Path path,
                        Name primaryType,
                        Set<Name> mixinTypes,
                        Name parentPrimaryType,
                        Set<Name> parentMixinTypes ) {
        super(key, path, primaryType, mixinTypes);
        this.parentKey = parentKey;
        this.parentPrimaryType = parentPrimaryType;
        this.parentMixinTypes = parentMixinTypes;
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
     * Get the name of the primary type of the parent node.
     * 
     * @return either the {@link Name} of the parent node or {@code null} if this information was not available.
     */
    public Name getParentPrimaryType() {
        return parentPrimaryType;
    }

    /**
     * Get the mixin names of the parent node.
     *
     * @return either the {@link Set} of mixins or {@code null} if this information was not available.
     */
    public Set<Name> getParentMixinTypes() {
        return parentMixinTypes;
    }

    @Override
    public String toString() {
        return "Removed node '" + this.getKey() + "' at \"" + path + "\" from under " + parentKey;
    }
}

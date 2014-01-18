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
import org.modeshape.jcr.value.Property;

/**
 * Internal event fired when a new property is added.
 */
public class PropertyAdded extends AbstractPropertyChange {

    private static final long serialVersionUID = 1L;

    protected PropertyAdded( NodeKey key,
                             Name nodePrimaryType,
                             Set<Name> nodeMixinTypes,
                             Path nodePath,
                             Property property ) {
        super(key, nodePrimaryType, nodeMixinTypes, nodePath, property);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Added property ");
        stringBuilder.append(property.getName());
        stringBuilder.append(" to node '");
        stringBuilder.append(this.getKey());
        stringBuilder.append("' at path ");
        stringBuilder.append(getPathToNode().getString());
        stringBuilder.append(" with values:");
        stringBuilder.append(property);

        return stringBuilder.toString();
    }
}

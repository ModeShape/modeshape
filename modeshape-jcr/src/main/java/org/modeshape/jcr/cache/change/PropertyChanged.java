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
 * Internal event fired when a property is changed on a node
 */
public class PropertyChanged extends AbstractPropertyChange {

    private static final long serialVersionUID = 1L;

    private final Property oldProperty;

    protected PropertyChanged( NodeKey key,
                               Name nodePrimaryType,
                               Set<Name> nodeMixinTypes,
                               Path nodePath,
                               Property newProperty,
                               Property oldProperty ) {
        super(key, nodePrimaryType, nodeMixinTypes, nodePath, newProperty);
        this.oldProperty = oldProperty;
    }

    /**
     * @return newPath
     */
    public Property getNewProperty() {
        return property;
    }

    /**
     * @return oldPath
     */
    public Property getOldProperty() {
        return oldProperty;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Changed property ");
        stringBuilder.append(property.getName());
        stringBuilder.append(" on node '");
        stringBuilder.append(this.getKey());
        stringBuilder.append("' at path ");
        stringBuilder.append(getPathToNode().getString());
        stringBuilder.append(" from:");
        stringBuilder.append(oldProperty);
        stringBuilder.append(" to:");
        stringBuilder.append(property);

        return stringBuilder.toString();
    }
}

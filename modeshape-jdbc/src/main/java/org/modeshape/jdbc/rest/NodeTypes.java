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
package org.modeshape.jdbc.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;

/**
 * A collection of {@link NodeType} returned by the client
 */
@Immutable
public final class NodeTypes implements Iterable<NodeType> {

    private final Map<String, NodeType> typesByName = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected NodeTypes(JSONObject object) {
        try {
            if (!object.has("children")) {
                throw new RuntimeException("Invalid JSON object: " + object);
            }
            JSONObject children = object.getJSONObject("children");
            for (Iterator<String> itr = children.keys(); itr.hasNext(); ) {
                String key = itr.next();
                JSONObject child = children.getJSONObject(key);
                if (child != null) {
                    // and that means that each child is a JSONObject ...
                    NodeType nodeType = new NodeType(child, this);
                    typesByName.put(nodeType.getName(), nodeType);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected NodeType getNodeType( String name ) {
        return typesByName.get(name);
    }

    @Override
    public Iterator<NodeType> iterator() {
        return nodeTypes().iterator();
    }

    /**
     * Checks if there are any node types registered.
     *
     * @return {@code true} if there are any node types, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return typesByName.isEmpty();
    }

    protected Collection<NodeType> nodeTypes() {
        return typesByName.values();
    }

    protected NodeType[] toNodeTypes( Collection<String> nodeTypeNames ) {
        if (this.typesByName.isEmpty()) {
            return new NodeType[0];
        }
        int numValues = nodeTypeNames.size();
        int i = 0;
        NodeType[] result = new NodeType[numValues];
        for (String typeName : nodeTypeNames) {
            result[i++] = getNodeType(typeName);
        }
        return result;
    }

}

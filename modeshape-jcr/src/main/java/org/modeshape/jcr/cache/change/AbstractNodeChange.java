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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Abstract base class for all internal events.
 */
public abstract class AbstractNodeChange extends Change {

    private static final long serialVersionUID = 1L;

    private final NodeKey key;

    /**
     * An array which will contain both the primary type (on position 0) and the mixin types (on the following positions)
     */
    private final Name[] types;

    protected final Path path;

    protected AbstractNodeChange( NodeKey key,
                                  Path path,
                                  Name primaryType,
                                  Set<Name> mixinTypes ) {
        assert key != null;
        assert path != null;

        this.key = key;
        this.path = path;
        int typesCount = (mixinTypes != null ? mixinTypes.size() : 0) + 1;
        this.types = new Name[typesCount];
        this.types[0] = primaryType;
        if (typesCount > 1) {
            assert mixinTypes != null;
            System.arraycopy(mixinTypes.toArray(new Name[0]), 0, types, 1, mixinTypes.size());
        }
    }

    /**
     * Get the path to the node involved in the change.
     * 
     * @return the path; may not be null
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return key
     */
    public NodeKey getKey() {
        return key;
    }

    /**
     * Returns the primary type of the node
     * 
     * @return a {@link Name} instance; never {@code null}
     */
    public Name getPrimaryType() {
        return types[0];
    }

    /**
     * Returns the mixins for this node.
     * 
     * @return a {@link Set}; never {@code null} but possibly empty.
     */
    public Set<Name> getMixinTypes() {
        if (types.length == 1) {
            return Collections.emptySet();
        }
        return new HashSet<Name>(Arrays.asList(Arrays.copyOfRange(types, 1, types.length)));
    }

    /**
     * Determine if the {@link #getPrimaryType() primary type} or any of the {@link #getMixinTypes() mixin types} of the changed
     * node exactly matches at least one of the supplied node types.
     * 
     * @param nodeTypeName the name of the node types to be considered; may not be null
     * @param nodeTypes the immutable snapshot of node types; may not be null
     * @return true if the changed node is of the supplied node type or one of its supertypes, or false if none match
     */
    public boolean isType( Name nodeTypeName,
                           NodeTypes nodeTypes ) {
        return nodeTypes.isTypeOrSubtype(types, nodeTypeName);
    }

}

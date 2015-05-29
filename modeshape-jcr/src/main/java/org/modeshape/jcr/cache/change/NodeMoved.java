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
 * Change representing the moving of a node.
 */
public class NodeMoved extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final NodeKey oldParent;
    private final NodeKey newParent;
    private final Path oldPath;

    public NodeMoved( NodeKey key,
                      Name primaryType,
                      Set<Name> mixinTypes,
                      NodeKey oldParent,
                      NodeKey newParent,
                      Path newPath,
                      Path oldPath ) {
        super(key, newPath, primaryType, mixinTypes);
        this.oldParent = oldParent;
        this.newParent = newParent;
        this.oldPath = oldPath;
        assert this.oldParent != null;
        assert this.newParent != null;
        // assert this.newParent != this.oldParent; // we sometimes use NodeMoved to signify a change in path when a parent moves
    }

    /**
     * Get the parent under which the node now appears.
     * 
     * @return the new parent; never null
     */
    public NodeKey getNewParent() {
        return newParent;
    }

    /**
     * Get the parent under which the node formerly appeared.
     * 
     * @return the old parent; never null
     */
    public NodeKey getOldParent() {
        return oldParent;
    }

    /**
     * Get the new path for the node, if it is known
     * 
     * @return the new path; may be null if it is not known
     */
    public Path getNewPath() {
        return path;
    }

    /**
     * Get the old path for the node, if it is known
     * 
     * @return the old path; may be null if it is not known
     */
    public Path getOldPath() {
        return oldPath;
    }

    @Override
    public String toString() {
        return "Moved node '" + this.getKey() + "' to \"" + path + "\" (under '" + newParent + "') from \"" + oldPath
               + "\" (under '" + oldParent + "')";
    }
}

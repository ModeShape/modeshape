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
package org.modeshape.jcr.cache;

/**
 * An exception signalling that a node does not exist in the specified parent.
 */
public class NodeNotFoundInParentException extends NodeNotFoundException {

    private static final long serialVersionUID = 1L;

    private final NodeKey parentKey;

    /**
     * @param key the key for the node that does not exist
     * @param parentKey the key for the parent node
     */
    public NodeNotFoundInParentException( NodeKey key,
                                          NodeKey parentKey ) {
        super(key, "Cannot locate child node: " + key + " within parent: " + parentKey);
        this.parentKey = parentKey;
    }

    /**
     * Get the key for the parent node.
     * 
     * @return the key for the parent node
     */
    public NodeKey getParentKey() {
        return parentKey;
    }
}

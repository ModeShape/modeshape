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

import org.modeshape.jcr.value.Path;

/**
 * An exception signalling that a node at a supplied path does not exist.
 */
public class PathNotFoundException extends DocumentNotFoundException {

    private static final long serialVersionUID = 1L;

    private final NodeKey lowestExistingKey;
    private final Path lowestExistingPath;
    private final Path pathNotFound;

    /**
     * @param pathNotFound the path that was not found
     * @param keyForLowestExistingAncestor the key for the lowest node along the path that was found
     * @param pathForLowestExistingAncestor the path for the lowest node along the path that was found
     */
    public PathNotFoundException( Path pathNotFound,
                                  NodeKey keyForLowestExistingAncestor,
                                  Path pathForLowestExistingAncestor ) {
        super(pathNotFound.toString());
        this.lowestExistingKey = keyForLowestExistingAncestor;
        this.lowestExistingPath = pathForLowestExistingAncestor;
        this.pathNotFound = pathNotFound;
    }

    /**
     * @param pathNotFound the path that was not found
     * @param keyForLowestExistingAncestor the key for the lowest node along the path that was found
     * @param pathForLowestExistingAncestor the path for the lowest node along the path that was found
     * @param message the message
     */
    public PathNotFoundException( Path pathNotFound,
                                  NodeKey keyForLowestExistingAncestor,
                                  Path pathForLowestExistingAncestor,
                                  String message ) {
        super(pathNotFound.toString(), message);
        this.lowestExistingKey = keyForLowestExistingAncestor;
        this.lowestExistingPath = pathForLowestExistingAncestor;
        this.pathNotFound = pathNotFound;
    }

    /**
     * @param pathNotFound the path that was not found
     * @param keyForLowestExistingAncestor the key for the lowest node along the path that was found
     * @param pathForLowestExistingAncestor the path for the lowest node along the path that was found
     * @param cause the cause of this exception
     */
    public PathNotFoundException( Path pathNotFound,
                                  NodeKey keyForLowestExistingAncestor,
                                  Path pathForLowestExistingAncestor,
                                  Throwable cause ) {
        super(pathNotFound.toString(), cause);
        this.lowestExistingKey = keyForLowestExistingAncestor;
        this.lowestExistingPath = pathForLowestExistingAncestor;
        this.pathNotFound = pathNotFound;
    }

    /**
     * @param pathNotFound the path that was not found
     * @param keyForLowestExistingAncestor the key for the lowest node along the path that was found
     * @param pathForLowestExistingAncestor the path for the lowest node along the path that was found
     * @param message the message
     * @param cause the cause of this exception
     */
    public PathNotFoundException( Path pathNotFound,
                                  NodeKey keyForLowestExistingAncestor,
                                  Path pathForLowestExistingAncestor,
                                  String message,
                                  Throwable cause ) {
        super(pathNotFound.toString(), message, cause);
        this.lowestExistingKey = keyForLowestExistingAncestor;
        this.lowestExistingPath = pathForLowestExistingAncestor;
        this.pathNotFound = pathNotFound;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * @return lowestExistingKey
     */
    public NodeKey getLowestExistingKey() {
        return lowestExistingKey;
    }

    /**
     * @return lowestExistingPath
     */
    public Path getLowestExistingPath() {
        return lowestExistingPath;
    }

    /**
     * @return pathNotFound
     */
    public Path getPathNotFound() {
        return pathNotFound;
    }
}

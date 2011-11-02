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

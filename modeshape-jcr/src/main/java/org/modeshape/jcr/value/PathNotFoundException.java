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
package org.modeshape.jcr.value;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.GraphI18n;

/**
 * A runtime exception denoting that a node or property at a supplied {@link Path path} was not found. This exception does contain
 * the lowest ancestor of the path that was found to exist.
 */
@Immutable
public class PathNotFoundException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = -3703984046286975978L;

    private final Location location;
    private final Path lowestAncestorThatDoesExist;

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist ) {
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param message
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  String message ) {
        super(message);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param cause
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  Throwable cause ) {
        super(cause);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param message
     * @param cause
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  String message,
                                  Throwable cause ) {
        super(message, cause);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getMessage() {
        if (this.lowestAncestorThatDoesExist != null) {
            String locationPart = location.hasPath() ? location.getPath().toString() : location.toString();
            return GraphI18n.pathNotFoundExceptionLowestExistingLocationFound.text(locationPart, this.lowestAncestorThatDoesExist);
        }
        return super.getMessage();
    }

    /**
     * Get the path that was not found
     * 
     * @return the path that was not found
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the lowest (closest) existing {@link Path#getParent() ancestor} of the {@link #getLocation() non-existant location}.
     * 
     * @return the lowest ancestor that does exist
     */
    public Path getLowestAncestorThatDoesExist() {
        return lowestAncestorThatDoesExist;
    }
}

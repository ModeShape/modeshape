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
package org.modeshape.graph.request;

import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Instruction to rename an existing node (but keep it under the same parent). The same-name-sibling index will be determined
 * automatically, based upon it's current location within the list of children.
 */
public class RenameNodeRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location at;
    private final String workspaceName;
    private final Name newName;
    private Location actualOldLocation;
    private Location actualNewLocation;

    /**
     * Create a request to rename the node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param newName the new name for the node
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public RenameNodeRequest( Location at,
                              String workspaceName,
                              Name newName ) {
        CheckArg.isNotNull(at, "at");
        CheckArg.isNotNull(newName, "newName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.at = at;
        this.newName = newName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Get the location defining the node that is to be read.
     * 
     * @return the location of the node; never null
     */
    public Location at() {
        return at;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the new name for the node.
     * 
     * @return the new name; never null
     */
    public Name toName() {
        return newName;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param oldLocation the actual location of the node before being renamed
     * @param newLocation the actual location of the node after being renamed
     * @throws IllegalArgumentException if the either location is null or is missing its path, or if the new location does not
     *         have the same {@link Path.Segment#getName() name} on {@link Path#getLastSegment() last segment} as that
     *         {@link #toName() specified on the request}
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocations( Location oldLocation,
                                    Location newLocation ) {
        checkNotFrozen();
        CheckArg.isNotNull(oldLocation, "oldLocation");
        CheckArg.isNotNull(newLocation, "newLocation");
        if (!oldLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(oldLocation));
        }
        if (!newLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(newLocation));
        }
        Path newPath = newLocation.getPath();
        if (!newPath.getLastSegment().getName().equals(toName())) {
            String msg = GraphI18n.actualNewLocationMustHaveSameNameAsRequest.text(newLocation, toName());
            throw new IllegalArgumentException(msg);
        }
        this.actualOldLocation = oldLocation;
        this.actualNewLocation = newLocation;
    }

    /**
     * Get the actual location of the node before being renamed.
     * 
     * @return the actual location of the node before being renamed, or null if the actual location was not set
     */
    public Location getActualLocationBefore() {
        return actualOldLocation;
    }

    /**
     * Get the actual location of the node after being renamed.
     * 
     * @return the actual location of the node after being renamed, or null if the actual location was not set
     */
    public Location getActualLocationAfter() {
        return actualNewLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && at.hasPath() && at.getPath().getParent().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the {@link #getActualLocationAfter()} location, or if null the {@link #at()} location.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualNewLocation != null ? actualNewLocation : at;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualNewLocation = null;
        this.actualOldLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(at, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            RenameNodeRequest that = (RenameNodeRequest)obj;
            if (!this.at().isSame(that.at())) return false;
            if (!this.toName().equals(that.toName())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "rename node at " + at() + " in the \"" + workspaceName + "\" workspace to " + toName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not clone the results.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#clone()
     */
    @Override
    public RenameNodeRequest clone() {
        RenameNodeRequest request = new RenameNodeRequest(actualOldLocation != null ? actualOldLocation : at, workspaceName,
                                                          newName);
        request.setActualLocations(actualOldLocation, actualNewLocation);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.RENAME_NODE;
    }
}

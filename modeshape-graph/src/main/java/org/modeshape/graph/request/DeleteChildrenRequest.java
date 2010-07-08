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

import java.util.LinkedList;
import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Instruction that all nodes below a supplied node be deleted. This is similar to {@link DeleteBranchRequest}, except that the
 * parent node (top node in the branch) is not deleted.
 */
public class DeleteChildrenRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location at;
    private final String workspaceName;
    private Location actualLocation;
    private List<Location> actualChildrenDeleted = new LinkedList<Location>();

    /**
     * Create a request to delete all children of the supplied node. The supplied parent node will not be deleted.
     * 
     * @param at the location of the parent node
     * @param workspaceName the name of the workspace containing the parent
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public DeleteChildrenRequest( Location at,
                                  String workspaceName ) {
        CheckArg.isNotNull(at, "at");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.at = at;
    }

    /**
     * Get the location defining the top of the branch to be deleted
     * 
     * @return the location of the branch; never null
     */
    public Location at() {
        return at;
    }

    /**
     * Get the name of the workspace in which the branch exists.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
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
     * Sets the actual and complete location of the node being deleted. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being deleted, or null if the {@link #at() current location} should be used
     * @throws IllegalArgumentException if the actual location null or does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
    }

    /**
     * Add to the list of children that has been read the supplied children with the given path and identification properties. The
     * children are added in order.
     * 
     * @param children the locations of the children that were read
     * @throws IllegalArgumentException if the parameter is null
     * @throws IllegalStateException if the request is frozen
     * @see #addDeletedChild(Location)
     * @see #addDeletedChild(Path, Property)
     * @see #addDeletedChild(Path, Property, Property...)
     */
    public void addDeletedChildren( Iterable<Location> children ) {
        checkNotFrozen();
        CheckArg.isNotNull(children, "children");
        for (Location child : children) {
            if (child != null) this.actualChildrenDeleted.add(child);
        }
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param child the location of the child that was read
     * @throws IllegalArgumentException if the location is null
     * @throws IllegalStateException if the request is frozen
     * @see #addDeletedChild(Path, Property)
     * @see #addDeletedChild(Path, Property, Property...)
     */
    public void addDeletedChild( Location child ) {
        checkNotFrozen();
        CheckArg.isNotNull(child, "child");
        this.actualChildrenDeleted.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param firstIdProperty the first identification property of the child that was just read
     * @param remainingIdProperties the remaining identification properties of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @throws IllegalStateException if the request is frozen
     * @see #addDeletedChild(Location)
     * @see #addDeletedChild(Path, Property)
     */
    public void addDeletedChild( Path pathToChild,
                                 Property firstIdProperty,
                                 Property... remainingIdProperties ) {
        checkNotFrozen();
        Location child = Location.create(pathToChild, firstIdProperty, remainingIdProperties);
        this.actualChildrenDeleted.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification property. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param idProperty the identification property of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @throws IllegalStateException if the request is frozen
     * @see #addDeletedChild(Location)
     * @see #addDeletedChild(Path, Property, Property...)
     */
    public void addDeletedChild( Path pathToChild,
                                 Property idProperty ) {
        checkNotFrozen();
        Location child = Location.create(pathToChild, idProperty);
        this.actualChildrenDeleted.add(child);
    }

    /**
     * Get the list of the actual children that were deleted.
     * 
     * @return the actual children, or empty if there were no children (if frozen)
     */
    public List<Location> getActualChildrenDeleted() {
        return actualChildrenDeleted;
    }

    /**
     * Get the actual location of the node that was deleted.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && at.hasPath() && at.getPath().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocation != null ? actualLocation : at;
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            DeleteChildrenRequest that = (DeleteChildrenRequest)obj;
            if (!this.at().isSame(that.at())) return false;
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
        return "delete nodes below " + at() + " in the \"" + workspaceName + "\" workspace";
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
    public DeleteChildrenRequest clone() {
        return new DeleteChildrenRequest(at, workspaceName);
    }

    @Override
    public RequestType getType() {
        return RequestType.DELETE_CHILDREN;
    }

}

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
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Instruction that a branch be moved from one location into another.
 */
public class MoveBranchRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location from;
    private final Location into;
    private final Location before;
    private final String workspaceName;
    private final Name desiredNameForNode;
    private final NodeConflictBehavior conflictBehavior;
    private Location actualOldLocation;
    private Location actualNewLocation;

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest( Location from,
                              Location into,
                              String workspaceName ) {
        this(from, into, null, workspaceName, null, DEFAULT_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param newNameForMovedNode the new name for the node being moved, or null if the name of the original should be used
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest( Location from,
                              Location into,
                              String workspaceName,
                              Name newNameForMovedNode ) {
        this(from, into, null, workspaceName, newNameForMovedNode, DEFAULT_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest( Location from,
                              Location into,
                              String workspaceName,
                              NodeConflictBehavior conflictBehavior ) {
        this(from, into, null, workspaceName, null, conflictBehavior);
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param before the location of the child of the {@code into} node that the branch should be placed before; null indicates
     *        that the branch should be the last child of its new parent
     * @param workspaceName the name of the workspace
     * @param newNameForMovedNode the new name for the node being moved, or null if the name of the original should be used
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest( Location from,
                              Location into,
                              Location before,
                              String workspaceName,
                              Name newNameForMovedNode,
                              NodeConflictBehavior conflictBehavior ) {
        CheckArg.isNotNull(from, "from");
        // CheckArg.isNotNull(into, "into");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        this.from = from;
        this.into = into;
        this.before = before;
        this.workspaceName = workspaceName;
        this.desiredNameForNode = newNameForMovedNode;
        this.conflictBehavior = conflictBehavior;
    }

    /**
     * Get the location defining the top of the branch to be moved
     * 
     * @return the from location; never null
     */
    public Location from() {
        return from;
    }

    /**
     * Get the location defining the parent where the branch is to be placed
     * 
     * @return the to location; or null if the node is being reordered within the same parent
     */
    public Location into() {
        return into;
    }

    /**
     * Get the location defining the node before which the branch is to be placed
     * 
     * @return the to location; null indicates that the branch should be the last child node of its new parent
     */
    public Location before() {
        return before;
    }

    /**
     * Get the name of the workspace in which the branch exists.
     * 
     * @return the name of the workspace containing the branch; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the name of the copy if it is to be different than that of the original.
     * 
     * @return the desired name of the copy, or null if the name of the original is to be used
     */
    public Name desiredName() {
        return desiredNameForNode;
    }

    /**
     * Get the expected behavior when copying the branch and the {@link #into() destination} already has a node with the same
     * name.
     * 
     * @return the behavior specification
     */
    public NodeConflictBehavior conflictBehavior() {
        return conflictBehavior;
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
     * Determine whether this move request can be determined to have no effect.
     * <p>
     * A move is known to have no effect when all of the following conditions are true:
     * <ul>
     * <li>the {@link #into() into} location has a {@link Location#hasPath() path} but no {@link Location#hasIdProperties()
     * identification properties};</li>
     * <li>the {@link #from() from} location has a {@link Location#getPath() path}; and</li>
     * <li>the {@link #from() from} location's {@link Path#getParent() parent} is the same as the {@link #into() into} location's
     * path.</li>
     * </ul>
     * If all of these conditions are not true, this method returns false.
     * </p>
     * 
     * @return true if this move request really doesn't change the parent of the node, or false if it cannot be determined
     */
    public boolean hasNoEffect() {
        if (into != null && into.hasPath() && into.hasIdProperties() == false && from.hasPath()) {
            if (!from.getPath().getParent().equals(into.getPath())) return false;
            if (desiredName() != null && !desiredName().equals(from.getPath().getLastSegment().getName())) return false;
            if (before != null) return false;
            return true;
        }
        // Can't be determined for certain
        return false;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param oldLocation the actual location of the node before being moved
     * @param newLocation the actual new location of the node
     * @throws IllegalArgumentException if the either location is null, or if the either location does not have a path
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
        Name actualNewName = newLocation.getPath().getLastSegment().getName();
        Name expectedNewName = desiredName() != null ? desiredName() : oldLocation.getPath().getLastSegment().getName();
        if (!actualNewName.equals(expectedNewName)) {
            throw new IllegalArgumentException(GraphI18n.actualLocationNotEqualToInputLocation.text(newLocation, into));
        }
        this.actualOldLocation = oldLocation;
        this.actualNewLocation = newLocation;
    }

    /**
     * Get the actual location of the node before being moved.
     * 
     * @return the actual location of the node before being moved, or null if the actual location was not set
     */
    public Location getActualLocationBefore() {
        return actualOldLocation;
    }

    /**
     * Get the actual location of the node after being moved.
     * 
     * @return the actual location of the node after being moved, or null if the actual location was not set
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
        if (this.into() != null) {
            return this.workspaceName.equals(workspace)
                   && (into.hasPath() && into.getPath().isAtOrBelow(path) || from.hasPath() && from.getPath().isAtOrBelow(path));
        }
        // into or before must be non-null
        assert before() != null;
        return this.workspaceName.equals(workspace)
               && (before.hasPath() && before.getPath().getParent().isAtOrBelow(path) || from.hasPath()
                                                                                         && from.getPath().isAtOrBelow(path));

    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the {@link #getActualLocationAfter()} location, or if null the {@link #into()} location.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualNewLocation != null ? actualNewLocation : into != null ? into : before;
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
        this.actualOldLocation = null;
        this.actualNewLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(from, workspaceName, into);
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
            MoveBranchRequest that = (MoveBranchRequest)obj;
            if (!this.from().isSame(that.from())) return false;
            if (!this.into().isSame(that.into())) return false;
            if (!this.conflictBehavior().equals(that.conflictBehavior())) return false;
            if (!this.workspaceName.equals(that.workspaceName)) return false;
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
        if (desiredName() != null) {
            return "move branch " + from() + " in the \"" + inWorkspace() + "\" workspace "
                   + (into() == null ? "before " + before() : "into " + into()) + " with name " + desiredName();
        }
        return "move branch " + from() + " in the \"" + inWorkspace() + "\" workspace into "
               + (into() == null ? "before " + before() : "into " + into());
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
    public MoveBranchRequest clone() {
        MoveBranchRequest request = new MoveBranchRequest(actualOldLocation != null ? actualOldLocation : from, into, before,
                                                          workspaceName, desiredNameForNode, conflictBehavior);
        request.setActualLocations(actualOldLocation, actualNewLocation);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.MOVE_BRANCH;
    }
}

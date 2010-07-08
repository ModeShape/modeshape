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
 * Instruction that a branch be copied from one location into another. This request can copy a branch in one workspace into
 * another workspace, or it can copy a branch in the same workspace. Copying a branch always results in the generation of new
 * UUIDs for the copied nodes. {@link CloneBranchRequest Cloning a branch} provides functionality similar to copy, but with the
 * ability to preserve UUIDs in the move.
 * 
 * @see CloneBranchRequest
 */
public class CopyBranchRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_NODE_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location from;
    private final Location into;
    private final String fromWorkspace;
    private final String intoWorkspace;
    private final Name desiredNameForCopy;
    private final NodeConflictBehavior nodeConflictBehavior;
    private Location actualFromLocation;
    private Location actualIntoLocation;

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the copy should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be copied, or null if the source's
     *        default workspace is to be used
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CopyBranchRequest( Location from,
                              String fromWorkspace,
                              Location into,
                              String intoWorkspace ) {
        this(from, fromWorkspace, into, intoWorkspace, null, DEFAULT_NODE_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the copy should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be copied
     * @param nameForCopy the desired name for the node that results from the copy, or null if the name of the original should be
     *        used
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CopyBranchRequest( Location from,
                              String fromWorkspace,
                              Location into,
                              String intoWorkspace,
                              Name nameForCopy ) {
        this(from, fromWorkspace, into, intoWorkspace, nameForCopy, DEFAULT_NODE_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the copy should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be copied
     * @param nameForCopy the desired name for the node that results from the copy, or null if the name of the original should be
     *        used
     * @param nodeConflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CopyBranchRequest( Location from,
                              String fromWorkspace,
                              Location into,
                              String intoWorkspace,
                              Name nameForCopy,
                              NodeConflictBehavior nodeConflictBehavior ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(into, "into");
        CheckArg.isNotNull(fromWorkspace, "fromWorkspace");
        CheckArg.isNotNull(intoWorkspace, "intoWorkspace");
        CheckArg.isNotNull(nodeConflictBehavior, "nodeConflictBehavior");
        this.from = from;
        this.into = into;
        this.fromWorkspace = fromWorkspace;
        this.intoWorkspace = intoWorkspace;
        this.desiredNameForCopy = nameForCopy;
        this.nodeConflictBehavior = nodeConflictBehavior;
    }

    /**
     * Get the location defining the top of the branch to be copied
     * 
     * @return the from location; never null
     */
    public Location from() {
        return from;
    }

    /**
     * Get the location defining the parent where the new copy is to be placed
     * 
     * @return the to location; never null
     */
    public Location into() {
        return into;
    }

    /**
     * Get the name of the workspace containing the branch to be copied.
     * 
     * @return the name of the workspace containing the branch to be copied; never null
     */
    public String fromWorkspace() {
        return fromWorkspace;
    }

    /**
     * Get the name of the workspace where the copy is to be placed
     * 
     * @return the name of the workspace where the copy is to be placed; never null
     */
    public String intoWorkspace() {
        return intoWorkspace;
    }

    /**
     * Determine whether this copy operation is within the same workspace.
     * 
     * @return true if this operation is to be performed within the same workspace, or false if the workspace of the
     *         {@link #from() original} is different than that of the {@link #into() copy}
     */
    public boolean isSameWorkspace() {
        return fromWorkspace.equals(intoWorkspace);
    }

    /**
     * Get the name of the copy if it is to be different than that of the original.
     * 
     * @return the desired name of the copy, or null if the name of the original is to be used
     */
    public Name desiredName() {
        return desiredNameForCopy;
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
     * Get the expected behavior when copying the branch and the {@link #into() destination} already has a node with the same
     * name.
     * 
     * @return the behavior specification
     */
    public NodeConflictBehavior nodeConflictBehavior() {
        return nodeConflictBehavior;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param fromLocation the actual location of the node being copied
     * @param intoLocation the actual location of the new copy of the node
     * @throws IllegalArgumentException if the either location is null; or if the either location does not have a path
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocations( Location fromLocation,
                                    Location intoLocation ) {
        checkNotFrozen();
        CheckArg.isNotNull(fromLocation, "intoLocation");
        CheckArg.isNotNull(intoLocation, "intoLocation");
        if (!fromLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(fromLocation));
        }
        if (!intoLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(intoLocation));
        }
        this.actualFromLocation = fromLocation;
        this.actualIntoLocation = intoLocation;
    }

    /**
     * Get the actual location of the node before being copied.
     * 
     * @return the actual location of the node before being moved, or null if the actual location was not set
     */
    public Location getActualLocationBefore() {
        return actualFromLocation;
    }

    /**
     * Get the actual location of the node after being copied.
     * 
     * @return the actual location of the node after being copied, or null if the actual location was not set
     */
    public Location getActualLocationAfter() {
        return actualIntoLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.intoWorkspace.equals(workspace) && into.hasPath() && into.getPath().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualIntoLocation != null ? actualIntoLocation : into;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return intoWorkspace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(from, fromWorkspace, into, intoWorkspace);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualFromLocation = null;
        this.actualIntoLocation = null;
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
            CopyBranchRequest that = (CopyBranchRequest)obj;
            if (!this.from().isSame(that.from())) return false;
            if (!this.into().isSame(that.into())) return false;
            if (!this.nodeConflictBehavior().equals(that.nodeConflictBehavior())) return false;
            if (!this.fromWorkspace.equals(that.fromWorkspace)) return false;
            if (!this.intoWorkspace.equals(that.intoWorkspace)) return false;
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
        if (fromWorkspace.equals(intoWorkspace)) {
            if (desiredNameForCopy != null) {
                return "copy branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into() + " with name "
                       + desiredNameForCopy;
            }
            return "copy branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into();
        }
        if (desiredNameForCopy != null) {
            return "copy branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into() + " with name "
                   + desiredNameForCopy + " in the \"" + intoWorkspace + "\" workspace";
        }
        return "copy branch " + from() + " in the \"" + fromWorkspace + "\" workspace into " + into() + " in the \""
               + intoWorkspace + "\" workspace";
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
    public CopyBranchRequest clone() {
        CopyBranchRequest result = new CopyBranchRequest(actualFromLocation != null ? actualFromLocation : from, fromWorkspace,
                                                         actualIntoLocation != null ? actualIntoLocation : into, intoWorkspace,
                                                         desiredNameForCopy, nodeConflictBehavior);
        result.setActualLocations(actualFromLocation, actualIntoLocation);
        return result;
    }

    @Override
    public RequestType getType() {
        return RequestType.COPY_BRANCH;
    }
}

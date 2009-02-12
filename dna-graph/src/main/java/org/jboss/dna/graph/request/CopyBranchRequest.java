/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.request;

import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.property.Name;

/**
 * Instruction that a branch be copied from one location into another. This request can copy a branch in one workspace into
 * another workspace, or it can copy a branch in the same workspace.
 * 
 * @author Randall Hauch
 */
public class CopyBranchRequest extends Request {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location from;
    private final Location into;
    private final String fromWorkspace;
    private final String intoWorkspace;
    private final Name desiredNameForCopy;
    private final NodeConflictBehavior conflictBehavior;
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
        this(from, fromWorkspace, into, intoWorkspace, null, DEFAULT_CONFLICT_BEHAVIOR);
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
        this(from, fromWorkspace, into, intoWorkspace, nameForCopy, DEFAULT_CONFLICT_BEHAVIOR);
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
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CopyBranchRequest( Location from,
                              String fromWorkspace,
                              Location into,
                              String intoWorkspace,
                              Name nameForCopy,
                              NodeConflictBehavior conflictBehavior ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(into, "into");
        CheckArg.isNotNull(fromWorkspace, "fromWorkspace");
        CheckArg.isNotNull(intoWorkspace, "intoWorkspace");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        this.from = from;
        this.into = into;
        this.fromWorkspace = fromWorkspace;
        this.intoWorkspace = intoWorkspace;
        this.desiredNameForCopy = nameForCopy;
        this.conflictBehavior = conflictBehavior;
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
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
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
    public NodeConflictBehavior conflictBehavior() {
        return conflictBehavior;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param fromLocation the actual location of the node being copied
     * @param intoLocation the actual location of the new copy of the node
     * @throws IllegalArgumentException if the either location is null; if the old location does not represent the
     *         {@link Location#isSame(Location) same location} as the {@link #from() from location}; if the new location does not
     *         represent the {@link Location#isSame(Location) same location} as the {@link #into() into location}; if the either
     *         location does not have a path
     */
    public void setActualLocations( Location fromLocation,
                                    Location intoLocation ) {
        if (!from.isSame(fromLocation)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(fromLocation, from));
        }
        CheckArg.isNotNull(intoLocation, "intoLocation");
        assert fromLocation != null;
        assert intoLocation != null;
        if (!fromLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(fromLocation));
        }
        if (!intoLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(intoLocation));
        }
        // The 'into' should be the parent of the 'newLocation' ...
        if (into.hasPath() && !intoLocation.getPath().getParent().equals(into.getPath())) {
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotChildOfInputLocation.text(intoLocation, into));
        }
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            CopyBranchRequest that = (CopyBranchRequest)obj;
            if (!this.from().equals(that.from())) return false;
            if (!this.into().equals(that.into())) return false;
            if (!this.conflictBehavior().equals(that.conflictBehavior())) return false;
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
}

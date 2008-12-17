/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.requests;

import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;

/**
 * Instruction that a branch be copied from one location into another.
 * 
 * @author Randall Hauch
 */
public class CopyBranchRequest extends Request {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location from;
    private final Location into;
    private final NodeConflictBehavior conflictBehavior;
    private Location actualOldLocation;
    private Location actualNewLocation;

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param into the location of the existing node into which the copy should be placed
     * @throws IllegalArgumentException if <code>from</code> or <code>into</code> are null
     */
    public CopyBranchRequest( Location from,
                              Location into ) {
        this(from, into, DEFAULT_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param into the location of the existing node into which the copy should be placed
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public CopyBranchRequest( Location from,
                              Location into,
                              NodeConflictBehavior conflictBehavior ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(into, "into");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        this.from = from;
        this.into = into;
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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.Request#isReadOnly()
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
     * @param oldLocation the actual location of the node before being renamed
     * @param newLocation the actual location of the new copy of the node
     * @throws IllegalArgumentException if the either location is null, if the old location does not represent the
     *         {@link Location#isSame(Location) same location} as the {@link #from() from location}, if the new location does not
     *         represent the {@link Location#isSame(Location) same location} as the {@link #into() into location}, or if the
     *         either location does not have a path
     */
    public void setActualLocations( Location oldLocation,
                                    Location newLocation ) {
        if (!from.isSame(oldLocation)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(oldLocation, from));
        }
        CheckArg.isNotNull(newLocation, "newLocation");
        assert oldLocation != null;
        assert newLocation != null;
        if (!oldLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(oldLocation));
        }
        if (!newLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(newLocation));
        }
        // The 'into' should be the parent of the 'newLocation' ...
        if (into.hasPath() && !newLocation.getPath().getParent().equals(into.getPath())) {
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotChildOfInputLocation.text(newLocation, into));
        }
        this.actualNewLocation = newLocation;
    }

    /**
     * Get the actual location of the node before being copied.
     * 
     * @return the actual location of the node before being moved, or null if the actual location was not set
     */
    public Location getActualLocationBefore() {
        return actualOldLocation;
    }

    /**
     * Get the actual location of the node after being copied.
     * 
     * @return the actual location of the node after being moved, or null if the actual location was not set
     */
    public Location getActualLocationAfter() {
        return actualNewLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            CopyBranchRequest that = (CopyBranchRequest)obj;
            if (!this.from().equals(that.from())) return false;
            if (!this.into().equals(that.into())) return false;
            if (!this.conflictBehavior().equals(that.conflictBehavior())) return false;
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
        return "copy branch " + from() + " into " + into();
    }
}

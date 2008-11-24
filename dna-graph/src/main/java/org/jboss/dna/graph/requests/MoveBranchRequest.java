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
import org.jboss.dna.graph.properties.Path;

/**
 * Instruction that a branch be moved from one location into another.
 * 
 * @author Randall Hauch
 */
public class MoveBranchRequest extends Request {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location from;
    private final Location into;
    private final NodeConflictBehavior conflictBehavior;
    private Location actualOldLocation;
    private Location actualNewLocation;

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @throws IllegalArgumentException if <code>from</code> or <code>into</code> are null
     */
    public MoveBranchRequest( Location from,
                              Location into ) {
        this(from, into, DEFAULT_CONFLICT_BEHAVIOR);
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest( Location from,
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
     * @return the to location; never null
     */
    public Location into() {
        return into;
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
     * @see org.jboss.dna.graph.requests.Request#isReadOnly()
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
        if (into.hasPath() && into.hasIdProperties() == false && from.hasPath()) {
            return from.getPath().getParent().equals(into.getPath());
        }
        // Can't be determined for certain
        return false;
    }

    /**
     * Sets the actual and complete location of the node being renamed and its new location. This method must be called when
     * processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param oldLocation the actual location of the node before being renamed
     * @param newLocation the actual location of the node after being renamed
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
        if (!into.isSame(newLocation, false)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(newLocation, into));
        }
        assert oldLocation != null;
        assert newLocation != null;
        if (!oldLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(oldLocation));
        }
        if (!newLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(newLocation));
        }
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            MoveBranchRequest that = (MoveBranchRequest)obj;
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
        return "move branch " + from() + " into " + into();
    }
}

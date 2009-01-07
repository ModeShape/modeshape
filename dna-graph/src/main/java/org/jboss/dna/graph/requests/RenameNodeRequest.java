/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;

/**
 * Instruction to rename an existing node (but keep it under the same parent). The same-name-sibling index will be determined
 * automatically, based upon it's current location within the list of children.
 * 
 * @author Randall Hauch
 */
public class RenameNodeRequest extends Request {

    private static final long serialVersionUID = 1L;

    private final Location at;
    private final Name newName;
    private Location actualOldLocation;
    private Location actualNewLocation;

    /**
     * Create a request to rename the node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param newName the new name for the node
     * @throws IllegalArgumentException if the location is null
     */
    public RenameNodeRequest( Location at,
                              Name newName ) {
        CheckArg.isNotNull(at, "at");
        CheckArg.isNotNull(newName, "newName");
        this.at = at;
        this.newName = newName;
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
     * Get the location defining the node that is to be read.
     * 
     * @return the location of the node; never null
     */
    public Location at() {
        return at;
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
     * @throws IllegalArgumentException if the either location is null or is missing its path, if the old location does not
     *         represent the {@link Location#isSame(Location) same location} as the {@link #at() current location}, if the new
     *         location does not have the same parent as the old location, or if the new location does not have the same
     *         {@link Path.Segment#getName() name} on {@link Path#getLastSegment() last segment} as that {@link #toName()
     *         specified on the request}
     */
    public void setActualLocations( Location oldLocation,
                                    Location newLocation ) {
        if (!at.isSame(oldLocation)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(oldLocation, at));
        }
        assert oldLocation != null;
        if (newLocation == null) {
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(newLocation, at));
        }
        if (!oldLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(oldLocation));
        }
        if (!newLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualNewLocationMustHavePath.text(newLocation));
        }
        Path newPath = newLocation.getPath();
        if (!newPath.getParent().equals(oldLocation.getPath().getParent())) {
            String msg = GraphI18n.actualNewLocationMustHaveSameParentAsOldLocation.text(newLocation, oldLocation);
            throw new IllegalArgumentException(msg);
        }
        if (!newPath.getLastSegment().getName().equals(toName())) {
            String msg = GraphI18n.actualNewLocationMustHaveSameNameAsRequest.text(newLocation, toName());
            throw new IllegalArgumentException(msg);
        }
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            RenameNodeRequest that = (RenameNodeRequest)obj;
            if (!this.at().equals(that.at())) return false;
            if (!this.toName().equals(that.toName())) return false;
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
        return "rename node at " + at() + " to " + toName();
    }

}

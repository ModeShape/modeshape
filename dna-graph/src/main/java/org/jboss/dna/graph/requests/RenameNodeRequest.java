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
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;

/**
 * Instruction to rename an existing node (but keep it under the same parent). The same-name-sibling index will be determined
 * automatically, based upon it's current location within the list of children.
 * 
 * @author Randall Hauch
 */
public class RenameNodeRequest extends Request {

    private final Location at;
    private final Name newName;

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

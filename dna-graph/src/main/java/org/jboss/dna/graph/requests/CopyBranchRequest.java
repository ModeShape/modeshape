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

/**
 * Instruction that a branch be copied from one location into another.
 * 
 * @author Randall Hauch
 */
public class CopyBranchRequest extends Request {

    private final Location from;
    private final Location into;

    /**
     * Create a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param into the location of the existing node into which the copy should be placed
     * @throws IllegalArgumentException if <code>from</code> or <code>into</code> are null
     */
    public CopyBranchRequest( Location from,
                              Location into ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(into, "into");
        this.from = from;
        this.into = into;
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            CopyBranchRequest that = (CopyBranchRequest)obj;
            if (!this.from().equals(that.from())) return false;
            if (!this.into().equals(that.into())) return false;
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

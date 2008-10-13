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
 * Instruction that a branch be deleted.
 * 
 * @author Randall Hauch
 */
public class DeleteBranchRequest extends Request {

    private final Location at;

    /**
     * Create a request to delete a branch.
     * 
     * @param at the location of the top node in the existing branch that is to be deleted
     * @throws IllegalArgumentException if the location is null
     */
    public DeleteBranchRequest( Location at ) {
        CheckArg.isNotNull(at, "at");
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
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            DeleteBranchRequest that = (DeleteBranchRequest)obj;
            if (!this.at().equals(that.at())) return false;
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
        return "delete branch " + at();
    }
}

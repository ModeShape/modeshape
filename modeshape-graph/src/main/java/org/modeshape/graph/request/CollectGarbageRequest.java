/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

/**
 * Request that garbage collection be performed. Processors may disregard this request.
 */
public final class CollectGarbageRequest extends Request {

    private static final long serialVersionUID = 1L;

    private boolean additionalPassRequired = false;

    /**
     * Create a request to destroy an existing workspace.
     */
    public CollectGarbageRequest() {
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
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) return true;
        return false;
    }

    /**
     * After collecting garbage during one pass (per this request), set whether additional passes are still required.
     * 
     * @param additionalPassRequired true if this pass did not collect all known gargabe and additional passes are required, or
     *        false otherwise
     * @throws IllegalStateException if the request is frozen
     */
    public void setAdditionalPassRequired( boolean additionalPassRequired ) {
        checkNotFrozen();
        this.additionalPassRequired = additionalPassRequired;
    }

    /**
     * Determine whether additional garbage collection passes are still required after this pass. In other words, if 'true', then
     * this pass did not completely collect all known garbage.
     * 
     * @return true if this pass did not collect all known gargabe and additional passes are required, or false otherwise
     */
    public boolean isAdditionalPassRequired() {
        return additionalPassRequired;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "collect garbage";
    }

    @Override
    public RequestType getType() {
        return RequestType.COLLECT_GARBAGE;
    }
}

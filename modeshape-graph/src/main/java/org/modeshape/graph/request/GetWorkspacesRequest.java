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

import java.util.Set;

/**
 * A request to obtain the information about the workspaces that are available.
 */
public class GetWorkspacesRequest extends CacheableRequest {

    private static final long serialVersionUID = 1L;

    private Set<String> availableWorkspaceNames;

    /**
     * Create a request to obtain the information about the available workspaces.
     */
    public GetWorkspacesRequest() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Get the names of workspaces that are available (at least to the current user)
     * 
     * @return the set of workspace names, or null if the request has not been completed
     */
    public Set<String> getAvailableWorkspaceNames() {
        return availableWorkspaceNames;
    }

    /**
     * Set the names of the workspaces that are available (at least to the current user)
     * 
     * @param availableWorkspaceNames Sets availableWorkspaceNames to the specified value.
     * @throws IllegalStateException if the request is frozen
     */
    public void setAvailableWorkspaceNames( Set<String> availableWorkspaceNames ) {
        checkNotFrozen();
        this.availableWorkspaceNames = availableWorkspaceNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.availableWorkspaceNames = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
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
            // All workspace requests are consider equal
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
        return "request workspaces";
    }

    @Override
    public RequestType getType() {
        return RequestType.GET_WORKSPACES;
    }

}

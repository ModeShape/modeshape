/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;

/**
 * Request that an existing workspace with the supplied name be destroyed.
 */
public final class DestroyWorkspaceRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final String workspaceName;
    private Location actualLocationOfRoot;

    /**
     * Create a request to destroy an existing workspace.
     * 
     * @param workspaceName the name of the workspace that is to be destroyed
     * @throws IllegalArgumentException if the workspace name is null
     */
    public DestroyWorkspaceRequest( String workspaceName ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
    }

    /**
     * Get the name for the workspace that is to be destroyed.
     * 
     * @return the name for the workspace; never null
     */
    public String workspaceName() {
        return workspaceName;
    }

    /**
     * Get the actual location of the root node in the new workspace, or null if the workspace was not (yet) created.
     * 
     * @return the actual location of the root node in the new workspace, or null if the workspace was not (yet) created
     */
    public Location getActualLocationOfRoot() {
        return actualLocationOfRoot;
    }

    /**
     * Set the actual location of the root node in the new workspace.
     * 
     * @param actualLocationOfRoot the actual location of the workspace's root node.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualRootLocation( Location actualLocationOfRoot ) {
        checkNotFrozen();
        this.actualLocationOfRoot = actualLocationOfRoot;
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
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return workspaceName.hashCode();
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
            DestroyWorkspaceRequest that = (DestroyWorkspaceRequest)obj;
            if (!this.workspaceName.equals(that.workspaceName())) return false;
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
        return "destroy workspace \"" + workspaceName() + "\"";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocationOfRoot;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changes(java.lang.String, org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return workspaceName().equals(workspace);
    }
}

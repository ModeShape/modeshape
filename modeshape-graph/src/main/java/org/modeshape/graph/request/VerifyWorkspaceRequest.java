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

import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.Location;

/**
 * Verify that a workspace exists with the supplied name. This is useful to determine the name of the "default" workspace for a
 * source.
 */
public final class VerifyWorkspaceRequest extends Request {

    private static final long serialVersionUID = 1L;

    private final String workspaceName;
    private Location actualLocationOfRoot;
    private String actualWorkspaceName;

    /**
     * Create a request to verify the existance of the named workspace.
     * 
     * @param workspaceName the desired name of the workspace, or null if the source's default workspace should be used
     */
    public VerifyWorkspaceRequest( String workspaceName ) {
        this.workspaceName = workspaceName;
    }

    /**
     * Get the desired name for the workspace.
     * 
     * @return the desired name for the workspace, or null if the source's default workspace is to be verified
     */
    public String workspaceName() {
        return workspaceName;
    }

    /**
     * Get the actual name of the workspace.
     * 
     * @return the actual name of the workspace, or null if a workspace was not verified (yet)
     */
    public String getActualWorkspaceName() {
        return actualWorkspaceName;
    }

    /**
     * Set the actual name of the workspace.
     * 
     * @param actualWorkspaceName the actual name of the workspace; never null
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualWorkspaceName( String actualWorkspaceName ) {
        checkNotFrozen();
        CheckArg.isNotNull(actualWorkspaceName, "actualWorkspaceName");
        this.actualWorkspaceName = actualWorkspaceName;
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
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocationOfRoot = null;
        this.actualWorkspaceName = null;
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
            VerifyWorkspaceRequest that = (VerifyWorkspaceRequest)obj;
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
        return "verify workspace \"" + workspaceName() + "\"";
    }

    @Override
    public RequestType getType() {
        return RequestType.VERIFY_WORKSPACE;
    }
}

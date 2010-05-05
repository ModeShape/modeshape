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
import org.modeshape.graph.property.Path;

/**
 * Request that a new workspace be created with the supplied name. The request also specifies the desired
 * {@link #conflictBehavior() behavior} for the recipient if a workspace already exists with the name.
 */
public final class CreateWorkspaceRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    /**
     * The options for the behavior when a request specifies a workspace name that already is used by an existing workspace.
     */
    public static enum CreateConflictBehavior {
        /** Do not create the workspace, and record as an {@link Request#setError(Throwable) error} on the request. */
        DO_NOT_CREATE,

        /** Create the workspace by adjusting the name so that it is unique. */
        CREATE_WITH_ADJUSTED_NAME
    }

    /**
     * The default {@link CreateConflictBehavior} that will be used if it is unspecified.
     */
    public static final CreateConflictBehavior DEFAULT_CREATE_CONFLICT_BEHAVIOR = CreateConflictBehavior.DO_NOT_CREATE;

    private final String desiredNameOfNewWorkspace;
    private final CreateConflictBehavior createConflictBehavior;
    private String actualWorkspaceName;
    private Location actualLocationOfRoot;

    /**
     * Create a request to create a new workspace, and specify the behavior should a workspace already exists with a name that
     * matches the desired name for the new workspace.
     * 
     * @param desiredNameOfNewWorkspace the desired name of the new workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name
     */
    public CreateWorkspaceRequest( String desiredNameOfNewWorkspace,
                                   CreateConflictBehavior createConflictBehavior ) {
        CheckArg.isNotNull(desiredNameOfNewWorkspace, "desiredNameOfNewWorkspace");
        this.desiredNameOfNewWorkspace = desiredNameOfNewWorkspace;
        this.createConflictBehavior = createConflictBehavior != null ? createConflictBehavior : DEFAULT_CREATE_CONFLICT_BEHAVIOR;
    }

    /**
     * Get the desired name for the new workspace.
     * 
     * @return the desired name for the new workspace; never null
     */
    public String desiredNameOfNewWorkspace() {
        return desiredNameOfNewWorkspace;
    }

    /**
     * Get the desired behavior if a workspace already exists with the {@link #desiredNameOfNewWorkspace() desired workspace name}
     * .
     * 
     * @return the desired behavior; never null
     */
    public CreateConflictBehavior conflictBehavior() {
        return createConflictBehavior;
    }

    /**
     * Get the actual name of the workspace that was created. This will be the same as the {@link #desiredNameOfNewWorkspace()
     * desired name} unless there was a conflict and the {@link #conflictBehavior() desired behavior} was to
     * {@link CreateConflictBehavior#CREATE_WITH_ADJUSTED_NAME alter the name}.
     * 
     * @return the actual name of the workspace that was created, or null if a workspace was not created (yet)
     */
    public String getActualWorkspaceName() {
        return actualWorkspaceName;
    }

    /**
     * Set the actual name of the workspace that was created. This should be the same as the {@link #desiredNameOfNewWorkspace()
     * desired name} unless there was a conflict and the {@link #conflictBehavior() desired behavior} was to
     * {@link CreateConflictBehavior#CREATE_WITH_ADJUSTED_NAME alter the name}.
     * 
     * @param actualWorkspaceName the actual name of the workspace that was created, or null if a workspace was not created
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualWorkspaceName( String actualWorkspaceName ) {
        checkNotFrozen();
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
        this.actualWorkspaceName = null;
        this.actualLocationOfRoot = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return desiredNameOfNewWorkspace.hashCode();
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
            CreateWorkspaceRequest that = (CreateWorkspaceRequest)obj;
            if (!this.desiredNameOfNewWorkspace.equals(that.desiredNameOfNewWorkspace())) return false;
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
        return "create new workspace \"" + desiredNameOfNewWorkspace() + "\"";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocationOfRoot;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return actualWorkspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return actualWorkspaceName != null && actualWorkspaceName.equals(workspace);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not clone the results.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#clone()
     */
    @Override
    public CreateWorkspaceRequest clone() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                                                                    actualWorkspaceName != null ? actualWorkspaceName : desiredNameOfNewWorkspace,
                                                                    createConflictBehavior);
        request.setActualWorkspaceName(actualWorkspaceName);
        request.setActualRootLocation(actualLocationOfRoot);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.CREATE_WORKSPACE;
    }
}

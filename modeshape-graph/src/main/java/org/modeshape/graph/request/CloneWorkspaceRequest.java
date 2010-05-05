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
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * Request that an existing workspace be cloned into a target workspace with the supplied name. If the target workspace exists,
 * the {@link #targetConflictBehavior() target conflict behavior} defines the behavior to be followed. If the workspace being
 * cloned does not exist, the {@link #cloneConflictBehavior() clone conflict behavior} defines the behavior to be followed.
 */
public final class CloneWorkspaceRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    /**
     * The options for the behavior when a request specifies the name of the workspace to clone, but the cloned workspace does not
     * exist.
     */
    public enum CloneConflictBehavior {
        /** Do not perform the clone, and record as an {@link Request#setError(Throwable) error} on the request. */
        DO_NOT_CLONE,

        /** The clone operation is skipped quietly, resulting in an empty new workspace. */
        SKIP_CLONE,
    }

    /**
     * The default {@link CloneConflictBehavior} that will be used if it is unspecified.
     */
    public static final CloneConflictBehavior DEFAULT_CLONE_CONFLICT_BEHAVIOR = CloneConflictBehavior.DO_NOT_CLONE;

    /**
     * The default {@link CreateConflictBehavior} that will be used if it is unspecified.
     */
    public static final CreateConflictBehavior DEFAULT_CREATE_CONFLICT_BEHAVIOR = CreateConflictBehavior.DO_NOT_CREATE;

    private final String nameOfWorkspaceToBeCloned;
    private final String desiredNameOfTargetWorkspace;
    private final CreateConflictBehavior createConflictBehavior;
    private final CloneConflictBehavior cloneConflictBehavior;
    private String actualWorkspaceName;
    private Location actualLocationOfRoot;

    /**
     * Create a request to clone an existing workspace to create a new workspace, and specify the behavior should a workspace
     * already exists with a name that matches the desired name for the new workspace.
     * 
     * @param nameOfWorkspaceToBeCloned the name of the existing workspace that is to be cloned
     * @param desiredNameOfTargetWorkspace the desired name of the target workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name
     * @param cloneConflictBehavior the behavior if the workspace to be cloned does not exist
     * @throws IllegalArgumentException if the either workspace name is null
     */
    public CloneWorkspaceRequest( String nameOfWorkspaceToBeCloned,
                                  String desiredNameOfTargetWorkspace,
                                  CreateConflictBehavior createConflictBehavior,
                                  CloneConflictBehavior cloneConflictBehavior ) {
        CheckArg.isNotNull(nameOfWorkspaceToBeCloned, "nameOfWorkspaceToBeCloned");
        CheckArg.isNotNull(desiredNameOfTargetWorkspace, "desiredNameOfTargetWorkspace");
        this.nameOfWorkspaceToBeCloned = nameOfWorkspaceToBeCloned;
        this.desiredNameOfTargetWorkspace = desiredNameOfTargetWorkspace;
        this.createConflictBehavior = createConflictBehavior != null ? createConflictBehavior : DEFAULT_CREATE_CONFLICT_BEHAVIOR;
        this.cloneConflictBehavior = cloneConflictBehavior != null ? cloneConflictBehavior : DEFAULT_CLONE_CONFLICT_BEHAVIOR;
    }

    /**
     * Get the name of the existing workspace that is to be cloned into the new workspace.
     * 
     * @return the name of the existing workspace that is to be cloned; never null
     */
    public String nameOfWorkspaceToBeCloned() {
        return nameOfWorkspaceToBeCloned;
    }

    /**
     * Get the desired name for the target workspace.
     * 
     * @return the desired name for the new workspace; never null
     */
    public String desiredNameOfTargetWorkspace() {
        return desiredNameOfTargetWorkspace;
    }

    /**
     * Get the desired behavior if a workspace already exists with the {@link #desiredNameOfTargetWorkspace() desired workspace
     * name} .
     * 
     * @return the desired behavior; never null
     */
    public CreateConflictBehavior targetConflictBehavior() {
        return createConflictBehavior;
    }

    /**
     * Get the desired behavior if the {@link #nameOfWorkspaceToBeCloned() cloned workspace} does not exist.
     * 
     * @return the desired behavior; never null
     */
    public CloneConflictBehavior cloneConflictBehavior() {
        return cloneConflictBehavior;
    }

    /**
     * Get the actual name of the workspace that was created. This will be the same as the {@link #desiredNameOfTargetWorkspace()
     * desired target name} unless there was a conflict and the {@link #targetConflictBehavior() desired behavior} was to
     * {@link CreateConflictBehavior#CREATE_WITH_ADJUSTED_NAME alter the name}.
     * 
     * @return the actual name of the workspace that was created, or null if a workspace was not created (yet)
     */
    public String getActualWorkspaceName() {
        return actualWorkspaceName;
    }

    /**
     * Set the actual name of the workspace that was created. This should be the same as the
     * {@link #desiredNameOfTargetWorkspace() desired target name} unless there was a conflict and the
     * {@link #targetConflictBehavior() desired behavior} was to {@link CreateConflictBehavior#CREATE_WITH_ADJUSTED_NAME alter the
     * name}.
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
        return HashCode.compute(nameOfWorkspaceToBeCloned, desiredNameOfTargetWorkspace);
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
            CloneWorkspaceRequest that = (CloneWorkspaceRequest)obj;
            if (!this.nameOfWorkspaceToBeCloned.equals(that.nameOfWorkspaceToBeCloned())) return false;
            if (!this.desiredNameOfTargetWorkspace.equals(that.desiredNameOfTargetWorkspace())) return false;
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
        return "clone workspace \"" + nameOfWorkspaceToBeCloned() + "\" as workspace \"" + desiredNameOfTargetWorkspace() + "\"";
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
    public CloneWorkspaceRequest clone() {
        CloneWorkspaceRequest request = new CloneWorkspaceRequest(
                                                                  nameOfWorkspaceToBeCloned,
                                                                  actualWorkspaceName != null ? actualWorkspaceName : desiredNameOfTargetWorkspace,
                                                                  createConflictBehavior, cloneConflictBehavior);
        request.setActualRootLocation(actualLocationOfRoot);
        request.setActualWorkspaceName(actualWorkspaceName);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.CLONE_WORKSPACE;
    }
}

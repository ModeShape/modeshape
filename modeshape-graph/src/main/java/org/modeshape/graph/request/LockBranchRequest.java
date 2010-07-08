/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;

/**
 * Instruction to lock an existing node or branch. Connectors that do not support locking must ignore this request.
 */
public class LockBranchRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    public enum LockScope {
        SELF_ONLY,
        SELF_AND_DESCENDANTS;
    }

    private final Location at;
    private final String workspaceName;
    private final LockScope isDeep;
    private final long lockTimeoutInMillis;
    private Location actualLocation;

    /**
     * Create a request to lock the node or branch at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param isDeep whether the lock should be deep (i.e., should include all of the node's descendants)
     * @param lockTimeoutInMillis the number of milliseconds that the lock should last before the lock times out; zero (0)
     *        indicates that the connector default should be used
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public LockBranchRequest( Location at,
                              String workspaceName,
                              LockScope isDeep,
                              long lockTimeoutInMillis ) {
        CheckArg.isNotNull(at, "at");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.at = at;
        this.isDeep = isDeep;
        this.lockTimeoutInMillis = lockTimeoutInMillis;
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
     * Get the location defining the node that is to be read.
     * 
     * @return the location of the node; never null
     */
    public Location at() {
        return at;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get whether the lock should include all of the descendants of the node as well as the node itself
     * 
     * @return {@link LockScope#SELF_AND_DESCENDANTS} if the lock should include all of the descendants of the node,
     *         {@link LockScope#SELF_ONLY} otherwise
     */
    public LockScope lockScope() {
        return isDeep;
    }

    /**
     * Gets the maximum length of the lock in milliseconds
     * 
     * @return the number of milliseconds that the lock should last before the lock times out; zero (0) indicates that the
     *         connector default should be used
     */
    public long lockTimeoutInMillis() {
        return lockTimeoutInMillis;
    }

    /**
     * Sets the actual and complete location of the node being locked. This method must be called when processing the request, and
     * the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actualLocation the actual location of the node before being locked
     * @throws IllegalArgumentException if the either location is null or is missing its path
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocation( Location actualLocation ) {
        checkNotFrozen();
        CheckArg.isNotNull(actualLocation, "actualLocation");
        if (!actualLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualOldLocationMustHavePath.text(actualLocation));
        }
        this.actualLocation = actualLocation;
    }

    /**
     * Get the actual location of the node that was locked.
     * 
     * @return the actual location of the node being locked, or null if the actual location was not set
     */
    public Location getActualLocation() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && at.hasPath() && at.getPath().getParent().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocation != null ? actualLocation : at;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(at, workspaceName, lockScope(), lockTimeoutInMillis);
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
            LockBranchRequest that = (LockBranchRequest)obj;
            if (this.lockTimeoutInMillis() != that.lockTimeoutInMillis()) return false;
            if (!this.at().isSame(that.at())) return false;
            if (this.lockScope() != that.lockScope()) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
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
        return "lock " + (LockScope.SELF_AND_DESCENDANTS == lockScope() ? "branch rooted" : "node") + " at " + at()
               + " in the \"" + workspaceName + "\" workspace";
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
    public LockBranchRequest clone() {
        LockBranchRequest request = new LockBranchRequest(actualLocation != null ? actualLocation : at, workspaceName, isDeep,
                                                          lockTimeoutInMillis);
        request.setActualLocation(actualLocation);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.LOCK_BRANCH;
    }
}

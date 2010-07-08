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
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Instruction to remove the property with the supplied name from the node at the given location. This request has no net effect
 * if the node does not contain a property with the supplied name.
 */
public class RemovePropertyRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location from;
    private final String workspaceName;
    private final Name propertyName;
    private Location actualLocation;

    /**
     * Create a request to remove a named property from the node at the supplied location.
     * 
     * @param from the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyName the name of the property to be removed
     * @throws IllegalArgumentException if the location, workspace name, or property name is null
     */
    public RemovePropertyRequest( Location from,
                                  String workspaceName,
                                  Name propertyName ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.from = from;
        this.propertyName = propertyName;
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
     * Get the location defining the node from which the property is to be removed.
     * 
     * @return the location of the node; never null
     */
    public Location from() {
        return from;
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
     * Get the name of the property that is being removed.
     * 
     * @return the property name; never null
     */
    public Name propertyName() {
        return propertyName;
    }

    /**
     * Sets the actual and complete location of the node being updated. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being updated, or null if the {@link #from() current location} should be used
     * @throws IllegalArgumentException if the actual location is null or does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
    }

    /**
     * Get the actual location of the node that was updated.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
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
        return this.workspaceName.equals(workspace) && from.hasPath() && from.getPath().isAtOrBelow(path);
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
        return HashCode.compute(from, workspaceName, propertyName);
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
            RemovePropertyRequest that = (RemovePropertyRequest)obj;
            if (!this.from().isSame(that.from())) return false;
            if (!this.propertyName().equals(that.propertyName())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocation != null ? actualLocation : from;
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "remove property " + propertyName() + " from " + from() + " in the \"" + workspaceName + "\" workspace";
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
    public RemovePropertyRequest clone() {
        RemovePropertyRequest request = new RemovePropertyRequest(actualLocation != null ? actualLocation : from, workspaceName,
                                                                  propertyName);
        request.setActualLocationOfNode(actualLocation);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.REMOVE_PROPERTY;
    }
}

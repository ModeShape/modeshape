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

import java.util.Arrays;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Instruction to set a particular property on the node at the specified location. This request <i>never</i> removes the node,
 * even if the property is empty.
 */
public class SetPropertyRequest extends ChangeRequest implements PropertyChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location on;
    private final String workspaceName;
    private final Property property;
    private Location actualLocation;
    private boolean actualCreation;

    /**
     * Create a request to set the property on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param property the new property on the node
     * @throws IllegalArgumentException if the location, workspace name, or property is null
     */
    public SetPropertyRequest( Location on,
                               String workspaceName,
                               Property property ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(property, "property");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.on = on;
        this.property = property;
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
     * Get the location defining the node that is to be updated.
     * 
     * @return the location of the node; never null
     */
    public Location on() {
        return on;
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
     * Get the property that is being set.
     * 
     * @return the new property; never null
     */
    public Property property() {
        return property;
    }

    /**
     * Sets the actual and complete location of the node being updated. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being updated, or null if the {@link #on() current location} should be used
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
     * Record that the property did not exist prior to the processing of this request and was actually created by this request.
     * This method must be called when processing the request, and the actual location must have a {@link Location#getPath() path}
     * .
     * 
     * @param created true if the property was created by this request, or false if this request updated an existing property
     * @throws IllegalStateException if the request is frozen
     */
    public void setNewProperty( boolean created ) {
        this.actualCreation = created;
    }

    /**
     * Get whether the {@link #property() property} was created.
     * 
     * @return true if this request created the property, or false if this request changed an existing property
     */
    public boolean isNewProperty() {
        return actualCreation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.ChangeRequest#changes(java.lang.String, org.modeshape.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && on.hasPath() && on.getPath().isAtOrBelow(path);
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
        return HashCode.compute(on, workspaceName, property.getName());
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
            SetPropertyRequest that = (SetPropertyRequest)obj;
            if (!this.on().isSame(that.on())) return false;
            if (!this.property().equals(that.property())) return false;
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
        return actualLocation != null ? actualLocation : on;
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
        Object[] values = property().getValuesAsArray();
        return "set property " + property().getName() + " on " + on() + " in the \"" + workspaceName + "\" workspace to "
               + (values == null ? "null" : Arrays.asList(values).toString());
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
    public SetPropertyRequest clone() {
        SetPropertyRequest request = new SetPropertyRequest(actualLocation != null ? actualLocation : on, workspaceName, property);
        request.setActualLocationOfNode(actualLocation);
        request.setNewProperty(actualCreation);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.SET_PROPERTY;
    }
}

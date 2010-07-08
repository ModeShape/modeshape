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
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * Instruction to read a single property on the node at the specified location.
 */
public class ReadPropertyRequest extends CacheableRequest {

    private static final long serialVersionUID = 1L;

    private final Location on;
    private final String workspaceName;
    private final Name propertyName;
    private Property property;
    private Location actualLocation;

    /**
     * Create a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyName the name of the property to read
     * @throws IllegalArgumentException if the location, workspace name, or property name are null
     */
    public ReadPropertyRequest( Location on,
                                String workspaceName,
                                Name propertyName ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.on = on;
        this.propertyName = propertyName;
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
     * Get the location defining the node that is to be read.
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
     * Get the name of the property that is to be read
     * 
     * @return the property name; never null
     */
    public Name named() {
        return propertyName;
    }

    /**
     * Get the property that was read.
     * 
     * @return the property, or null if the property was not read or did not exist on the node
     */
    public Property getProperty() {
        return property;
    }

    /**
     * Set the property on the node as read from the {@link RepositoryConnection}
     * 
     * @param property the property that was read
     * @throws IllegalArgumentException if the property's name does not match the {@link #named() name of the property} that was
     *         to be read
     * @throws IllegalStateException if the request is frozen
     */
    public void setProperty( Property property ) {
        checkNotFrozen();
        if (property != null) CheckArg.isEquals(property.getName(), "property's name", named(), "property name");
        this.property = property;
    }

    /**
     * Sets the actual and complete location of the node whose property has been read. This method must be called when processing
     * the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being read, or null if the {@link #on() current location} should be used
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
     * Get the actual location of the node whose property was read.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
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
        this.property = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(on, workspaceName, propertyName);
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
            ReadPropertyRequest that = (ReadPropertyRequest)obj;
            if (!this.on().isSame(that.on())) return false;
            if (!this.named().equals(that.named())) return false;
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
        return "read " + named() + " property on " + on() + " in the \"" + workspaceName + "\" workspace";
    }

    @Override
    public RequestType getType() {
        return RequestType.READ_PROPERTY;
    }
}

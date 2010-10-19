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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Instruction to update the properties on the node at the specified location.
 * <p>
 * This request is capable of specifying that certain properties are to have new values and that other properties are to be
 * removed. The request has a single map of properties keyed by their name. If a property is to be set with new values, the map
 * will contain an entry with the property keyed by its name. However, if a property is to be removed, the entry will contain the
 * property name for the key but will have a null entry value.
 * </p>
 * <p>
 * The use of the map also ensures that a single property appears only once in the request (it either has new values or it is to
 * be removed).
 * </p>
 * <p>
 * Note that the number of values in a property (e.g., {@link Property#size()}, {@link Property#isEmpty()},
 * {@link Property#isSingle()}, and {@link Property#isMultiple()}) has no influence on whether the property should be removed. It
 * is possible for a property to have no values.
 * </p>
 */
public class UpdatePropertiesRequest extends ChangeRequest implements PropertyChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location on;
    private final String workspaceName;
    private final Map<Name, Property> properties;
    private final boolean removeOtherProperties;
    private Set<Name> createdPropertyNames;
    private Location actualLocation;

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param properties the map of properties (keyed by their name), which is reused without copying
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    String workspaceName,
                                    Map<Name, Property> properties ) {
        this(on, workspaceName, properties, false);
    }

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param properties the map of properties (keyed by their name), which is reused without copying
     * @param removeOtherProperties if any properties not being updated should be removed
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    String workspaceName,
                                    Map<Name, Property> properties,
                                    boolean removeOtherProperties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotEmpty(properties, "properties");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.on = on;
        this.properties = Collections.unmodifiableMap(properties);
        this.removeOtherProperties = removeOtherProperties;
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
     * Get the map of properties for the node, keyed by property name. Any property to be removed will have a map entry with a
     * null value.
     * 
     * @return the properties being updated; never null and never empty
     */
    public Map<Name, Property> properties() {
        return properties;
    }

    /**
     * Return whether any properties not being updated should be removed.
     * 
     * @return true if the node's existing properties not updated with this request should be removed, or false if this request
     *         should leave other properties unchanged
     */
    public boolean removeOtherProperties() {
        return removeOtherProperties;
    }

    /**
     * Sets the actual and complete location of the node being updated. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being updated, or null if the {@link #on() current location} should be used
     * @throws IllegalArgumentException if the actual location is null or does not have a path.
     * @throws IllegalStateException if the request is frozen
     * @see #setNewProperties(Iterable)
     * @see #setNewProperties(Name...)
     * @see #setNewProperty(Name)
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
     * Record that the named property did not exist prior to the processing of this request and was actually created by this
     * request. This method (or one of its sibling methods) must be called at least once when processing the request, and may be
     * called repeatedly for additional properties.
     * 
     * @param nameOfCreatedProperty the name of one of the {@link #properties() properties} that was created by this request
     * @throws IllegalStateException if the request is frozen
     * @throws IllegalArgumentException if the name is null or if it is not the name of one of the {@link #properties()
     *         properties}
     * @see #setActualLocationOfNode(Location)
     * @see #setNewProperties(Name...)
     * @see #setNewProperties(Iterable)
     */
    public void setNewProperty( Name nameOfCreatedProperty ) {
        CheckArg.isNotNull(nameOfCreatedProperty, "nameOfCreatedProperty");
        checkNotFrozen();
        if (!properties().containsKey(nameOfCreatedProperty)) {
            throw new IllegalStateException(GraphI18n.propertyIsNotPartOfRequest.text(nameOfCreatedProperty, this));
        }
        if (createdPropertyNames == null) createdPropertyNames = new HashSet<Name>();
        createdPropertyNames.add(nameOfCreatedProperty);
    }

    /**
     * Record that the named properties did not exist prior to the processing of this request and were actually created by this
     * request. This method (or one of its sibling methods) must be called at least once when processing the request, and may be
     * called repeatedly for additional properties.
     * 
     * @param nameOfCreatedProperties the names of the {@link #properties() properties} that were created by this request
     * @throws IllegalStateException if the request is frozen
     * @throws IllegalArgumentException if the name is null or if it is not the name of one of the {@link #properties()
     *         properties}
     * @see #setActualLocationOfNode(Location)
     * @see #setNewProperties(Iterable)
     * @see #setNewProperty(Name)
     */
    public void setNewProperties( Name... nameOfCreatedProperties ) {
        checkNotFrozen();
        if (createdPropertyNames == null) createdPropertyNames = new HashSet<Name>();
        for (Name name : nameOfCreatedProperties) {
            if (name != null) {
                if (!properties().containsKey(name)) {
                    throw new IllegalStateException(GraphI18n.propertyIsNotPartOfRequest.text(name, this));
                }
                createdPropertyNames.add(name);
            }
        }
    }

    /**
     * Record that the named properties did not exist prior to the processing of this request and were actually created by this
     * request. This method (or one of its sibling methods) must be called at least once when processing the request, and may be
     * called repeatedly for additional properties.
     * 
     * @param nameOfCreatedProperties the names of the {@link #properties() properties} that were created by this request
     * @throws IllegalStateException if the request is frozen
     * @throws IllegalArgumentException if any of the names are not in the updated {@link #properties() properties}
     * @see #setActualLocationOfNode(Location)
     * @see #setNewProperties(Name...)
     * @see #setNewProperty(Name)
     */
    public void setNewProperties( Iterable<Name> nameOfCreatedProperties ) {
        checkNotFrozen();
        if (nameOfCreatedProperties == null) return;
        if (createdPropertyNames == null) createdPropertyNames = new HashSet<Name>();
        for (Name name : nameOfCreatedProperties) {
            if (name != null) {
                if (!properties().containsKey(name)) {
                    throw new IllegalStateException(GraphI18n.propertyIsNotPartOfRequest.text(name, this));
                }
                createdPropertyNames.add(name);
            }
        }
    }

    /**
     * Get the names of the {@link #properties() properties} that were created by this request.
     * 
     * @return the names of the properties
     */
    public Set<Name> getNewPropertyNames() {
        return createdPropertyNames;
    }

    /**
     * Determine whether the named property was created by this request
     * 
     * @param name the property name
     * @return true if the named property was created by the request, or false otherwise
     */
    public boolean isNewProperty( Name name ) {
        return createdPropertyNames != null && createdPropertyNames.contains(name);
    }

    /**
     * Determine whether this request only added the properties.
     * 
     * @return true if the properties being updated were all new properties, or false otherwise
     */
    public boolean isAllNewProperties() {
        if (createdPropertyNames != null && createdPropertyNames.containsAll(properties.values())) return true;
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#freeze()
     */
    @Override
    public boolean freeze() {
        if (super.freeze()) {
            if (createdPropertyNames != null) {
                if (createdPropertyNames.isEmpty()) {
                    createdPropertyNames = Collections.emptySet();
                } else if (createdPropertyNames.size() == 1) {
                    createdPropertyNames = Collections.singleton(createdPropertyNames.iterator().next());
                } else {
                    createdPropertyNames = Collections.unmodifiableSet(createdPropertyNames);
                }
            }
            return true;
        }
        return false;
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
        return HashCode.compute(on, workspaceName);
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
            UpdatePropertiesRequest that = (UpdatePropertiesRequest)obj;
            if (!this.on().isSame(that.on())) return false;
            if (!this.properties().equals(that.properties())) return false;
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
        if (removeOtherProperties) {
            return "update (and remove other) properties on " + on() + " in the \"" + workspaceName + "\" workspace to "
                   + properties();
        }
        return "update properties on " + on() + " in the \"" + workspaceName + "\" workspace to " + properties();
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
    public UpdatePropertiesRequest clone() {
        UpdatePropertiesRequest request = new UpdatePropertiesRequest(actualLocation != null ? actualLocation : on,
                                                                      workspaceName, properties, removeOtherProperties);
        request.setActualLocationOfNode(actualLocation);
        request.setNewProperties(createdPropertyNames);
        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.UPDATE_PROPERTIES;
    }
}

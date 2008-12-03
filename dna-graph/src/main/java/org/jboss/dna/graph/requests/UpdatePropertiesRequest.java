/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.requests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;

/**
 * Instruction to update the properties on the node at the specified location. Any property with no values will be removed.
 * 
 * @author Randall Hauch
 */
public class UpdatePropertiesRequest extends Request implements Iterable<Property> {

    private static final long serialVersionUID = 1L;

    private final Location on;
    private final List<Property> properties;
    private Location actualLocation;

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param properties the new properties on the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    Property... properties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotEmpty(properties, "properties");
        this.on = on;
        this.properties = Collections.unmodifiableList(Arrays.asList(properties));
    }

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param properties the new properties on the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    Iterable<Property> properties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(properties, "properties");
        this.on = on;
        List<Property> props = new LinkedList<Property>();
        for (Property property : properties) {
            if (property != null) props.add(property);
        }
        this.properties = Collections.unmodifiableList(props);
        CheckArg.isNotEmpty(this.properties, "properties");
    }

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param properties the new properties on the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    Iterator<Property> properties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(properties, "properties");
        this.on = on;
        List<Property> props = new LinkedList<Property>();
        while (properties.hasNext()) {
            Property property = properties.next();
            if (property != null) props.add(property);
        }
        this.properties = Collections.unmodifiableList(props);
        CheckArg.isNotEmpty(this.properties, "properties");
    }

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param properties the new properties on the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to update
     */
    private UpdatePropertiesRequest( Location on,
                                     List<Property> properties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(properties, "properties");
        this.on = on;
        this.properties = properties;
        CheckArg.isNotEmpty(this.properties, "properties");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.Request#isReadOnly()
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
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Property> iterator() {
        return this.properties.iterator();
    }

    /**
     * Get the properties for the node.
     * 
     * @return the collection of properties; never null and never empty
     */
    public Collection<Property> properties() {
        return properties;
    }

    /**
     * Sets the actual and complete location of the node being updated. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being updated, or null if the {@link #on() current location} should be used
     * @throws IllegalArgumentException if the actual location does represent the {@link Location#isSame(Location) same location}
     *         as the {@link #on() current location}, or if the actual location does not have a path.
     */
    public void setActualLocationOfNode( Location actual ) {
        if (!on.isSame(actual)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(actual, on));
        }
        assert actual != null;
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            UpdatePropertiesRequest that = (UpdatePropertiesRequest)obj;
            if (!this.on().equals(that.on())) return false;
            if (!this.properties().equals(that.properties())) return false;
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
        return "update properties on " + on() + " to " + properties();
    }

    /**
     * Merge these updates with those in the supplied request, with the supplied changes overwriting any similar changes on this
     * node.
     * 
     * @param other the other updates that are to be merged with these
     * @return the merged request
     */
    public UpdatePropertiesRequest mergeWith( UpdatePropertiesRequest other ) {
        if (other == null) return this;
        if (other.properties().size() == 1) {
            Property newProp = other.properties.get(0);
            List<Property> newProps = new LinkedList<Property>();
            for (Property prop : this.properties) {
                if (!prop.getName().equals(newProp.getName())) {
                    newProps.add(prop);
                }
            }
            newProps.add(newProp);
            return new UpdatePropertiesRequest(on, Collections.unmodifiableList(newProps));
        }
        Set<Name> otherNames = new HashSet<Name>();
        for (Property prop : other.properties()) {
            otherNames.add(prop.getName());
        }
        List<Property> newProps = new LinkedList<Property>();
        for (Property prop : this.properties) {
            if (!otherNames.contains(prop.getName())) {
                newProps.add(prop);
            }
        }
        newProps.addAll(other.properties);
        return new UpdatePropertiesRequest(on, Collections.unmodifiableList(newProps));

    }

}

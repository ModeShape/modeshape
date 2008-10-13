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

import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;

/**
 * Instruction to read a single property on the node at the specified location.
 * 
 * @author Randall Hauch
 */
public class ReadPropertyRequest extends Request {

    private final Location on;
    private final Name propertyName;
    private Property property;

    /**
     * Create a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param propertyName the name of the property to read
     * @throws IllegalArgumentException if the location or property name are null
     */
    public ReadPropertyRequest( Location on,
                                Name propertyName ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotNull(propertyName, "propertyName");
        this.on = on;
        this.propertyName = propertyName;
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
     */
    public void setProperty( Property property ) {
        if (property != null) CheckArg.isEquals(property.getName(), "property's name", named(), "property name");
        this.property = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            ReadPropertyRequest that = (ReadPropertyRequest)obj;
            if (!this.on().equals(that.on())) return false;
            if (!this.named().equals(that.named())) return false;
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
        return "read " + named() + " property at " + on();
    }

}

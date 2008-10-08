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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.properties.Property;

/**
 * Instruction to create the node at the specified location. This command will create the node and set the initial properties.
 * 
 * @author Randall Hauch
 */
public class CreateNodeRequest extends Request implements Iterable<Property> {

    private final Location at;
    private final List<Property> properties;

    /**
     * Create a request to create a node with the given properties at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param properties the properties of the new node, which should not include the location's
     *        {@link Location#getIdProperties() identification properties}
     * @throws IllegalArgumentException if the location is null
     */
    public CreateNodeRequest( Location at,
                              Property... properties ) {
        CheckArg.isNotNull(at, "at");
        this.at = at;
        int number = properties.length + (at.hasIdProperties() ? at.getIdProperties().size() : 0);
        List<Property> props = new ArrayList<Property>(number);
        for (Property property : properties) {
            if (property != null) props.add(property);
        }
        // Add in the location properties ...
        if (at.hasIdProperties()) {
            for (Property property : at.getIdProperties()) {
                if (property != null) props.add(property);
            }
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Create a request to create a node with the given properties at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param properties the properties of the new node, which should not include the location's
     *        {@link Location#getIdProperties() identification properties}
     * @throws IllegalArgumentException if the location is null
     */
    public CreateNodeRequest( Location at,
                              Iterable<Property> properties ) {
        CheckArg.isNotNull(at, "at");
        this.at = at;
        List<Property> props = new LinkedList<Property>();
        for (Property property : properties) {
            if (property != null) props.add(property);
        }
        // Add in the location properties ...
        if (at.hasIdProperties()) {
            for (Property property : at.getIdProperties()) {
                if (property != null) props.add(property);
            }
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Create a request to create a node with the given properties at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param properties the properties of the new node, which should not include the location's
     *        {@link Location#getIdProperties() identification properties}
     * @throws IllegalArgumentException if the location is null
     */
    public CreateNodeRequest( Location at,
                              Iterator<Property> properties ) {
        CheckArg.isNotNull(at, "at");
        this.at = at;
        List<Property> props = new LinkedList<Property>();
        while (properties.hasNext()) {
            Property property = properties.next();
            if (property != null) props.add(property);
        }
        // Add in the location properties ...
        if (at.hasIdProperties()) {
            for (Property property : at.getIdProperties()) {
                if (property != null) props.add(property);
            }
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Get the location defining the node that is to be created.
     * 
     * @return the location of the node; never null
     */
    public Location at() {
        return at;
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
     * Get the properties for the node. If the node's {@link #at() location} has identification properties, the resulting
     * properties will include the {@link Location#getIdProperties() identification properties}.
     * 
     * @return the collection of properties; never null
     */
    public Collection<Property> properties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            CreateNodeRequest that = (CreateNodeRequest)obj;
            if (!this.at().equals(that.at())) return false;
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
        return "create node at " + at() + " with properties " + StringUtil.readableString(properties());
    }

}

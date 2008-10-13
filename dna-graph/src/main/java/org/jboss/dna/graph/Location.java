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
package org.jboss.dna.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.basic.BasicSingleValueProperty;

/**
 * The location of a node, as specified by either its path, UUID, and/or identification properties.
 * 
 * @author Randall Hauch
 */
@Immutable
public class Location {

    private final Path path;
    private final List<Property> idProperties;

    /**
     * Create a location defined by a path.
     * 
     * @param path the path
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public Location( Path path ) {
        CheckArg.isNotNull(path, "path");
        this.path = path;
        this.idProperties = null;
    }

    /**
     * Create a location defined by a UUID.
     * 
     * @param uuid the UUID
     * @throws IllegalArgumentException if <code>uuid</code> is null
     */
    public Location( UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        this.path = null;
        Property idProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by a path and an UUID.
     * 
     * @param path the path
     * @param uuid the UUID
     * @throws IllegalArgumentException if <code>uuid</code> is null
     */
    public Location( Path path,
                     UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        CheckArg.isNotNull(path, "path");
        this.path = path;
        Property idProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by a path and a single identification property.
     * 
     * @param path the path
     * @param idProperty the identification property
     * @throws IllegalArgumentException if <code>path</code> or <code>idProperty</code> is null
     */
    public Location( Path path,
                     Property idProperty ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(idProperty, "idProperty");
        this.path = path;
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by a path and multiple identification properties.
     * 
     * @param path the path
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Path path,
                     Property firstIdProperty,
                     Property... remainingIdProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        this.path = path;
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            idProperties.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idProperties);
    }

    /**
     * Create a location defined by a single identification property.
     * 
     * @param idProperty the identification property
     * @throws IllegalArgumentException if <code>idProperty</code> is null
     */
    public Location( Property idProperty ) {
        CheckArg.isNotNull(idProperty, "idProperty");
        this.path = null;
        this.idProperties = Collections.singletonList(idProperty);
    }

    /**
     * Create a location defined by multiple identification properties.
     * 
     * @param firstIdProperty the first identification property
     * @param remainingIdProperties the remaining identification property
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public Location( Property firstIdProperty,
                     Property... remainingIdProperties ) {
        CheckArg.isNotNull(firstIdProperty, "firstIdProperty");
        CheckArg.isNotNull(remainingIdProperties, "remainingIdProperties");
        this.path = null;
        List<Property> idProperties = new ArrayList<Property>(1 + remainingIdProperties.length);
        idProperties.add(firstIdProperty);
        for (Property property : remainingIdProperties) {
            idProperties.add(property);
        }
        this.idProperties = Collections.unmodifiableList(idProperties);
    }

    /**
     * Create a location defined by multiple identification properties.
     * 
     * @param idProperties the identification properties
     * @throws IllegalArgumentException if <code>idProperties</code> is null or empty
     */
    public Location( List<Property> idProperties ) {
        CheckArg.isNotEmpty(idProperties, "idProperties");
        this.path = null;
        this.idProperties = idProperties;
    }

    /**
     * Create a location defined by a path and multiple identification properties.
     * 
     * @param path the path
     * @param idProperties the identification properties
     * @throws IllegalArgumentException if <code>path</code> or <code>idProperties</code> is null, or if <code>idProperties</code>
     *         is empty
     */
    public Location( Path path,
                     List<Property> idProperties ) {
        CheckArg.isNotNull(path, "path");
        CheckArg.isNotEmpty(idProperties, "idProperties");
        this.path = path;
        this.idProperties = idProperties;
    }

    /**
     * Get the path that (at least in part) defines this location.
     * 
     * @return the path, or null if this location is not defined with a path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Return whether this location is defined (at least in part) by a path.
     * 
     * @return true if a {@link #getPath() path} helps define this location
     */
    public boolean hasPath() {
        return path != null;
    }

    /**
     * Get the identification properties that (at least in part) define this location.
     * 
     * @return the identification properties, or null if this location is not defined with identification properties
     */
    public List<Property> getIdProperties() {
        return idProperties;
    }

    /**
     * Return whether this location is defined (at least in part) with identification properties.
     * 
     * @return true if a {@link #getIdProperties() identification properties} help define this location
     */
    public boolean hasIdProperties() {
        return idProperties != null && idProperties.size() != 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(path, idProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof Location) {
            Location that = (Location)obj;
            if (this.hasPath()) {
                if (!this.getPath().equals(that.getPath())) return false;
            } else {
                if (that.hasPath()) return false;
            }
            if (this.hasIdProperties()) {
                if (!this.getIdProperties().equals(that.getIdProperties())) return false;
            } else {
                if (that.hasIdProperties()) return false;
            }
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
        StringBuilder sb = new StringBuilder();
        if (this.hasPath()) {
            if (this.hasIdProperties()) sb.append("[ ");
            sb.append(this.getPath());
            if (this.hasIdProperties()) sb.append(" && ");
        }
        if (this.hasIdProperties()) {
            sb.append(StringUtil.readableString(this.getIdProperties()));
            if (this.hasPath()) sb.append(" ]");
        }
        return sb.toString();
    }
}

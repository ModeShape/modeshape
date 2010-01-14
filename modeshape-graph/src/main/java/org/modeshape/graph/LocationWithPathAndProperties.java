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
package org.modeshape.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.basic.BasicSingleValueProperty;

/**
 * General purpose location type that supports a path and zero or more properties. This class should never be directly
 * instantiated by users of the ModeShape framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
class LocationWithPathAndProperties extends LocationWithPath {

    private static final long serialVersionUID = 1L;

    private final List<Property> idProperties;

    /**
     * Create a new location with a given path and set of identification properties.
     * 
     * @param path the path
     * @param idProperties the identification properties
     */
    LocationWithPathAndProperties( Path path,
                                   List<Property> idProperties ) {
        super(path);
        assert idProperties != null;
        assert !idProperties.isEmpty();
        this.idProperties = Collections.unmodifiableList(idProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getIdProperties()
     */
    @Override
    public final List<Property> getIdProperties() {
        return idProperties;
    }

    /**
     * Get the first UUID that is in one of the {@link #getIdProperties() identification properties}.
     * 
     * @return the UUID for this location, or null if there is no such identification property
     */
    @Override
    public UUID getUuid() {
        Property property = getIdProperty(ModeShapeLexicon.UUID);
        if (property != null && !property.isEmpty()) {
            Object value = property.getFirstValue();
            if (value instanceof UUID) return (UUID)value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#hasIdProperties()
     */
    @Override
    public boolean hasIdProperties() {
        return idProperties.size() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */
    @Override
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;

        List<Property> newIdProperties;

        if (hasIdProperties()) {
            newIdProperties = new ArrayList<Property>(idProperties.size() + 1);
            for (Property property : idProperties) {
                if (!newIdProperty.getName().equals(property.getName())) newIdProperties.add(property);
            }
            newIdProperties.add(newIdProperty);
            newIdProperties = Collections.unmodifiableList(newIdProperties);
            return create(getPath(), newIdProperties);
        }
        return create(getPath(), newIdProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */
    @Override
    public Location with( Path newPath ) {
        if (newPath == null || newPath.equals(this.getPath())) return this;
        return create(newPath, idProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        if (uuid == null) return this;
        Property newProperty = new BasicSingleValueProperty(ModeShapeLexicon.UUID, uuid);
        if (this.hasIdProperties()) {
            Property existing = this.getIdProperty(ModeShapeLexicon.UUID);
            if (existing != null && existing.equals(newProperty)) return this;
        }

        List<Property> newIdProperties = new ArrayList<Property>(idProperties.size() + 1);
        newIdProperties.addAll(idProperties);
        newIdProperties.add(newProperty);
        return create(getPath(), newIdProperties);
    }
}

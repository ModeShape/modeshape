/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;

/**
 * General purpose location type that supports a path and a UUID property. This class should never be directly instantiated by
 * users of the DNA framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
final class LocationWithPathAndUuid extends Location {

    private final Path path;
    private final List<Property> idProperties;

    private final int hashCode;

    /**
     * Create a new location with a given path and identification property.
     * 
     * @param path the path
     * @param uuid the UUID
     */
    LocationWithPathAndUuid( Path path,
                             UUID uuid ) {
        assert path != null;
        assert uuid != null;
        this.path = path;
        this.idProperties = Collections.singletonList((Property)new BasicSingleValueProperty(DnaLexicon.UUID, uuid));

        // Paths are immutable, Properties are immutable, the idProperties list
        // is wrapped in an unmodifiableList by the Location factory methods...
        // ... so we can cache the hash code.
        hashCode = HashCode.compute(this.path, idProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getPath()
     */
    @Override
    public final Path getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#hasPath()
     */
    @Override
    public final boolean hasPath() {
        return true;
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
     * {@inheritDoc}
     * 
     * @see Location#hasIdProperties()
     */
    @Override
    public final boolean hasIdProperties() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getIdProperty(Name)
     */
    @Override
    public final Property getIdProperty( Name name ) {
        CheckArg.isNotNull(name, "name");
        Property property = idProperties.get(0); // this is fast
        return property.getName().equals(name) ? property : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getUuid()
     */
    @Override
    public final UUID getUuid() {
        return (UUID)idProperties.get(0).getFirstValue(); // this is fast, and we know this is a UUID object
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */
    @Override
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;
        Property idProperty = idProperties.get(0); // fast
        if (newIdProperty.getName().equals(idProperty.getName())) {
            return new LocationWithPathAndProperty(path, newIdProperty);
        }
        return Location.create(path, idProperty, newIdProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */
    @Override
    public Location with( Path newPath ) {
        if (newPath == null) return Location.create(idProperties);
        if (path.equals(newPath)) return this;
        Property idProperty = idProperties.get(0); // fast
        return new LocationWithPathAndProperty(newPath, idProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        if (uuid == null) return Location.create(path);
        if (uuid.equals(getUuid())) return this;
        return new LocationWithPathAndUuid(path, uuid);
    }
}

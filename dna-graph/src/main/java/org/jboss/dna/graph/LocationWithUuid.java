/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;

/**
 * Special location type optimized for use when only a {@link UUID} is specified. This class should never be directly instantiated
 * by users of the DNA framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
final class LocationWithUuid extends Location {

    private static final long serialVersionUID = 1L;

    private final UUID uuid;
    private final int hashCode;
    private final List<Property> properties;

    LocationWithUuid( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
        Property uuidProperty = new BasicSingleValueProperty(DnaLexicon.UUID, uuid);
        this.properties = Collections.singletonList(uuidProperty);
        this.hashCode = HashCode.compute(null, this.properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#getIdProperties()
     */
    @Override
    public List<Property> getIdProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#getPath()
     */
    @Override
    public Path getPath() {
        return null;
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
     * @see org.jboss.dna.graph.Location#hasPath()
     */
    @Override
    public boolean hasPath() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#getUuid()
     */
    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#with(org.jboss.dna.graph.property.Property)
     */
    @Override
    public Location with( Property newIdProperty ) {
        return Location.create(getIdProperties().get(0), newIdProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#with(org.jboss.dna.graph.property.Path)
     */
    @Override
    public Location with( Path newPath ) {
        return Location.create(newPath, uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#with(java.util.UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        return Location.create(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.Location#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }
}

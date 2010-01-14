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

import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Special location type optimized for use when only a path is specified. This class should never be directly instantiated by
 * users of the ModeShape framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
class LocationWithPath extends Location {

    private static final long serialVersionUID = 1L;

    private final Path path;
    private final int hashCode;

    LocationWithPath( Path path ) {
        assert path != null;
        this.path = path;

        // Paths are immutable, so PathLocations are immutable...
        // ... so we can cache the hash code.
        hashCode = HashCode.compute(path);
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
     * @see Location#hasPath()
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
    public List<Property> getIdProperties() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#hasIdProperties()
     */
    @Override
    public boolean hasIdProperties() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getUuid()
     */
    @Override
    public UUID getUuid() {
        return null;
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
     * @see Location#getString(NamespaceRegistry, TextEncoder, TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(path.getString(namespaceRegistry, encoder, delimiterEncoder));
        sb.append(" }");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */
    @Override
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;
        return create(path, newIdProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */
    @Override
    public Location with( Path newPath ) {
        if (newPath == null || path.equals(newPath)) return this;
        return create(newPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        if (uuid == null) return this;
        return create(path, uuid);
    }
}

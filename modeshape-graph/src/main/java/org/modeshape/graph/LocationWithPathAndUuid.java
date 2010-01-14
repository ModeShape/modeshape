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

import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.basic.BasicSingleValueProperty;

/**
 * General purpose location type that supports a path and a UUID property. This class should never be directly instantiated by
 * users of the ModeShape framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
final class LocationWithPathAndUuid extends LocationWithPathAndProperty {

    private static final long serialVersionUID = 1L;

    private final UUID uuid;

    /**
     * Create a new location with a given path and identification property.
     * 
     * @param path the path
     * @param uuid the UUID
     */
    LocationWithPathAndUuid( Path path,
                             UUID uuid ) {
        super(path, new BasicSingleValueProperty(ModeShapeLexicon.UUID, uuid));
        assert uuid != null;
        this.uuid = uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getUuid()
     */
    @Override
    public final UUID getUuid() {
        return uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */
    @Override
    public Location with( Property idProperty ) {
        if (idProperty == null || idProperty.isEmpty()) return this;
        if (ModeShapeLexicon.UUID.equals(idProperty.getName())) {
            if (idProperty.isSingle() && uuid.equals(idProperty.getFirstValue())) return this;
            return Location.create(getPath(), idProperty);
        }
        return Location.create(getPath(), getIdProperties().get(0), idProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */
    @Override
    public Location with( Path path ) {
        if (path == null) return Location.create(uuid);
        if (getPath().equals(path)) return this;
        return Location.create(path, uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        if (uuid == null) return Location.create(getPath());
        if (uuid.equals(this.uuid)) return this;
        return Location.create(getPath(), uuid);
    }
}

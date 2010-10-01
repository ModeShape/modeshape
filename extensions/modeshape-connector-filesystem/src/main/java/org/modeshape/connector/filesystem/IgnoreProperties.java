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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * A {@link CustomPropertiesFactory} implementation that ignores the "extra" or "custom" properties for 'nt:file', 'nt:folder',
 * and 'nt:resource' nodes.
 */
public class IgnoreProperties extends BasePropertiesFactory {

    private static final long serialVersionUID = 1L;

    /**
     * Create an instance of this factory.
     */
    public IgnoreProperties() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getDirectoryProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File)
     */
    @Override
    public Collection<Property> getDirectoryProperties( ExecutionContext context,
                                                        Location location,
                                                        File directory ) {
        return NO_PROPERTIES_COLLECTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getFileProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File)
     */
    @Override
    public Collection<Property> getFileProperties( ExecutionContext context,
                                                   Location location,
                                                   File file ) {
        return NO_PROPERTIES_COLLECTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#getResourceProperties(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.Location, java.io.File, java.lang.String)
     */
    @Override
    public Collection<Property> getResourceProperties( ExecutionContext context,
                                                       Location location,
                                                       File file,
                                                       String mimeType ) {
        return NO_PROPERTIES_COLLECTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordDirectoryProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordDirectoryProperties( ExecutionContext context,
                                                String sourceName,
                                                Location location,
                                                File file,
                                                Map<Name, Property> properties ) throws RepositorySourceException {
        return NO_NAMES;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordFileProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordFileProperties( ExecutionContext context,
                                           String sourceName,
                                           Location location,
                                           File file,
                                           Map<Name, Property> properties ) throws RepositorySourceException {
        return NO_NAMES;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.connector.filesystem.CustomPropertiesFactory#recordResourceProperties(org.modeshape.graph.ExecutionContext,
     *      java.lang.String, org.modeshape.graph.Location, java.io.File, java.util.Map)
     */
    @Override
    public Set<Name> recordResourceProperties( ExecutionContext context,
                                               String sourceName,
                                               Location location,
                                               File file,
                                               Map<Name, Property> properties ) throws RepositorySourceException {
        return NO_NAMES;
    }
}

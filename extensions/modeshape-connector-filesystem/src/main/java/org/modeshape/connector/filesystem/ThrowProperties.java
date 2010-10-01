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
import org.modeshape.graph.property.ValueFactory;

/**
 * A {@link CustomPropertiesFactory} implementation that throws an exception when attempting to store the "extra" or "custom"
 * properties for 'nt:file', 'nt:folder', and 'nt:resource' nodes.
 */
public class ThrowProperties extends BasePropertiesFactory {

    private static final long serialVersionUID = 1L;

    /**
     * Create an instance of this factory.
     */
    public ThrowProperties() {
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
        int numProperties = properties.size();
        if (numProperties != 0) {
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            StringBuilder names = new StringBuilder();
            boolean first = true;
            for (Property property : properties.values()) {
                if (STANDARD_PROPERTIES_FOR_FILE_OR_FOLDER.contains(property.getName())) continue;
                if (first) first = false;
                else names.append(",");
                String name = strings.create(property.getName());
                names.append(name);
            }
            String msg = null;
            if (properties.size() == 1) {
                msg = FileSystemI18n.couldNotStoreProperties.text(names, file.getPath(), sourceName);
            } else {
                msg = FileSystemI18n.couldNotStoreProperty.text(names, file.getPath(), sourceName);
            }
            throw new RepositorySourceException(sourceName, msg);
        }
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
        int numProperties = properties.size();
        if (numProperties != 0) {
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            StringBuilder names = new StringBuilder();
            boolean first = true;
            for (Property property : properties.values()) {
                if (STANDARD_PROPERTIES_FOR_FILE_OR_FOLDER.contains(property.getName())) continue;
                if (first) first = false;
                else names.append(",");
                String name = strings.create(property.getName());
                names.append(name);
            }
            String msg = null;
            if (properties.size() == 1) {
                msg = FileSystemI18n.couldNotStoreProperties.text(names, file.getPath(), sourceName);
            } else {
                msg = FileSystemI18n.couldNotStoreProperty.text(names, file.getPath(), sourceName);
            }
            throw new RepositorySourceException(sourceName, msg);
        }
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
        int numProperties = properties.size();
        if (numProperties != 0) {
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            StringBuilder names = new StringBuilder();
            boolean first = true;
            for (Property property : properties.values()) {
                if (STANDARD_PROPERTIES_FOR_CONTENT.contains(property.getName())) continue;
                if (first) first = false;
                else names.append(",");
                String name = strings.create(property.getName());
                names.append(name);
            }
            String msg = null;
            if (properties.size() == 1) {
                msg = FileSystemI18n.couldNotStoreProperties.text(names, file.getPath(), sourceName);
            } else {
                msg = FileSystemI18n.couldNotStoreProperty.text(names, file.getPath(), sourceName);
            }
            throw new RepositorySourceException(sourceName, msg);
        }
        return NO_NAMES;
    }
}

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
package org.modeshape.jcr.federation.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.ExtraPropertiesStore;

/**
 * An {@link ExtraPropertiesStore} implementation that stores extra properties in BSON sidecar files adjacent to the actual file
 * or directory corresponding to the external node.
 */
class FileSystemConnectorBsonSidecarStorage extends FileSystemConnectorJsonSidecarStorage {

    public static final String DEFAULT_EXTENSION = ".modeshape.bson";
    public static final String DEFAULT_RESOURCE_EXTENSION = ".content.modeshape.bson";

    protected FileSystemConnectorBsonSidecarStorage( FileSystemConnector connector ) {
        super(connector);
    }

    @Override
    protected String getExclusionPattern() {
        return "(.+)\\.(content\\.)?modeshape\\.bson$";

    }

    @Override
    protected Document read( InputStream stream ) throws IOException {
        return Bson.read(stream);
    }

    @Override
    protected void write( Document document,
                          OutputStream stream ) throws IOException {
        Bson.write(document, stream);
    }

    @Override
    protected String extension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    protected String resourceExtension() {
        return DEFAULT_EXTENSION;
    }
}

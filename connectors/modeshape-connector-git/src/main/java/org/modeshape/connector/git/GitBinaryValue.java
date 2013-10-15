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
package org.modeshape.connector.git;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinary;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * A {@link BinaryValue} implementation used to read the content of a specific object ID from the supplied repository. This class
 * computes the {@link AbstractBinary#getMimeType() MIME type} lazily or upon serialization.
 */
public class GitBinaryValue extends ExternalBinaryValue {
    private static final long serialVersionUID = 1L;

    private transient ObjectLoader loader;

    public GitBinaryValue( ObjectId id,
                           ObjectLoader loader,
                           String sourceName,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(id.getName()), sourceName, id.getName(), loader.getSize(), nameHint, mimeTypeDetector);
        this.loader = loader;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return new BufferedInputStream(loader.openStream());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}

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
package org.modeshape.jcr.mimetype;

import java.io.IOException;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;

/**
 * MIME-type detection libraries must provide thread-safe implementations of this interface to enable ModeShape to use the
 * libraries to return MIME-types for data sources. Implementors are expected to have a public, no-arg constructor.
 */

public interface MimeTypeDetector {

    /**
     * Returns the MIME-type of a binary value, using its supplied content and/or its supplied name, depending upon the
     * implementation. If the MIME-type cannot be determined, either a "default" MIME-type or <code>null</code> may be returned,
     * where the former will prevent earlier registered MIME-type detectors from being consulted.
     * 
     * @param name The name of the data source; may be <code>null</code>.
     * @param binaryValue The value which contains the raw data for which the mime type should be returned; may be
     *        <code>null</code>.
     * @return The MIME-type of the data source, or optionally <code>null</code> if the MIME-type could not be determined.
     * @throws IOException If an error occurs reading the supplied content.
     * @throws RepositoryException if any error occurs while attempting to read the stream from the binary value
     */
    String mimeTypeOf( String name,
                       Binary binaryValue ) throws RepositoryException, IOException;
}

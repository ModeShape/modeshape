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
package org.modeshape.mimetype.aperture;

import java.io.IOException;
import java.io.InputStream;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.semanticdesktop.aperture.util.IOUtil;

/**
 * A {@link MimeTypeDetector} that uses the Aperture library.
 */
public class ApertureMimeTypeDetector implements MimeTypeDetector {

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @see org.modeshape.graph.mimetype.MimeTypeDetector#mimeTypeOf(java.lang.String, java.io.InputStream)
     */
    public String mimeTypeOf( String name,
                              InputStream content ) throws IOException {
        /*
            MimeTypes identifier = TikaConfig.getDefaultConfig().getMimeRepository();
            MimeTypeDetectors mimeType = identifier.getMimeType(path.getLastSegment().getName().getLocalName(), stream);
            return mimeType == null ? null : mimeType.getName();
        */
        MimeTypeIdentifier identifier = new MagicMimeTypeIdentifier();
        // Read as many bytes of the file as desired by the MIME-type identifier
        int minimumArrayLength = identifier.getMinArrayLength();
        byte[] bytes = IOUtil.readBytes(content, minimumArrayLength);
        // let the MimeTypeIdentifier determine the MIME-type of this file
        return identifier.identify(bytes, name, null);
    }
}

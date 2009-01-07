/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.mimetype;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.semanticdesktop.aperture.util.IOUtil;

/**
 * @author jverhaeg
 */
public class ApertureMimeTypeDetector implements MimeTypeDetector {

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @see org.jboss.dna.graph.mimetype.MimeTypeDetector#mimeTypeOf(java.lang.String, java.io.InputStream)
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

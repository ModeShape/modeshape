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
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrI18n;

/**
 * Implementation of {@link MimeTypeDetector} that can be used to detect MIME types. Internally, this detector uses the
 * {@link TikaMimeTypeDetector} if it is available, or the {@link NullMimeTypeDetector}.
 */
@ThreadSafe
public final class MimeTypeDetectors implements MimeTypeDetector {

    private static final Logger LOGGER = Logger.getLogger(MimeTypeDetectors.class);

    private final MimeTypeDetector delegate;

    public MimeTypeDetectors() {
        this(null);
    }

    public MimeTypeDetectors( Environment environment ) {
        ClassLoader defaultLoader = getClass().getClassLoader();
        // the extra classpath entry is the package name of the tika extractor, so it can be located inside AS7 (see
        // RepositoryService)
        ClassLoader classLoader = environment != null ? environment.getClassLoader(defaultLoader, "org.modeshape.extractor.tika") : defaultLoader;
        MimeTypeDetector delegate = null;
        try {
            delegate = new TikaMimeTypeDetector(classLoader);
        } catch (Throwable e) {
            delegate = NullMimeTypeDetector.INSTANCE;
            LOGGER.warn(JcrI18n.noMimeTypeDetectorsFound);
        }
        this.delegate = delegate;
    }

    /**
     * Returns the first non-null result of iterating over the registered MIME-type detectors If the MIME-type cannot be
     * determined by any registered detector, "text/plain" or "application/octet-stream" will be returned, the former only if it
     * is determined the stream contains no nulls.
     * 
     * @see MimeTypeDetector#mimeTypeOf(String, javax.jcr.Binary)
     */
    @Override
    public String mimeTypeOf( String name,
                              Binary binaryValue ) throws RepositoryException, IOException {
        String mime = delegate.mimeTypeOf(name, binaryValue);
        LOGGER.trace("MIME type for '" + name + "' ==> " + mime);
        return mime;
    }
}

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
package org.jboss.dna.repository.mimetype;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.dna.common.component.ClassLoaderFactory;

/**
 * Convenience class for working with the default {@link MimeTypeDetectors MIME-type detectors}.
 * 
 * @author jverhaeg
 */
public final class MimeType {

    public static final MimeTypeDetectors DEFAULT_DETECTORS = new MimeTypeDetectors();

    /**
     * @param config See {@link MimeTypeDetectors#addDetector(MimeTypeDetectorConfig)}.
     * @return See {@link MimeTypeDetectors#addDetector(MimeTypeDetectorConfig)}.
     * @see MimeTypeDetectors#addDetector(MimeTypeDetectorConfig)
     */
    public static boolean addDetector( MimeTypeDetectorConfig config ) {
        return DEFAULT_DETECTORS.addDetector(config);
    }

    /**
     * @return See {@link MimeTypeDetectors#getClassLoaderFactory()}.
     * @see MimeTypeDetectors#getClassLoaderFactory()
     */
    public static ClassLoaderFactory getClassLoaderFactory() {
        return DEFAULT_DETECTORS.getClassLoaderFactory();
    }

    /**
     * @param name See {@link MimeTypeDetectors#mimeTypeOf(String, InputStream)}.
     * @param content See {@link MimeTypeDetectors#mimeTypeOf(String, InputStream)}.
     * @return See {@link MimeTypeDetectors#mimeTypeOf(String, InputStream)}.
     * @throws IOException See {@link MimeTypeDetectors#mimeTypeOf(String, InputStream)}.
     */
    public static String of( String name,
                             InputStream content ) throws IOException {
        return DEFAULT_DETECTORS.mimeTypeOf(name, content);
    }

    /**
     * @param config See {@link MimeTypeDetectors#removeDetector(MimeTypeDetectorConfig)}.
     * @return See {@link MimeTypeDetectors#removeDetector(MimeTypeDetectorConfig)}.
     * @see MimeTypeDetectors#removeDetector(MimeTypeDetectorConfig)
     */
    public static boolean removeDetector( MimeTypeDetectorConfig config ) {
        return DEFAULT_DETECTORS.removeDetector(config);
    }

    /**
     * @param classLoaderFactory See {@link MimeTypeDetectors#setClassLoaderFactory(ClassLoaderFactory)}.
     * @see MimeTypeDetectors#setClassLoaderFactory(ClassLoaderFactory)
     */
    public static void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        DEFAULT_DETECTORS.setClassLoaderFactory(classLoaderFactory);
    }
}

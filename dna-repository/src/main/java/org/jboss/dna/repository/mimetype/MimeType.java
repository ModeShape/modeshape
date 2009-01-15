/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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

    /**
     * The set of
     */
    public static final MimeTypeDetectors DEFAULT_DETECTORS = MimeTypeDetectors.DEFAULT_DETECTORS;

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

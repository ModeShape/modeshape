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

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.RepositoryI18n;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OCTET_STREAM;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TEXT_PLAIN;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link MimeTypeDetector} which holds an inner list of different {@link MimeTypeDetector} implementations and
 * queries each of them, in order to determine a mime-type.
 *
 * @author Horia Chiorean
 */
@ThreadSafe
public final class MimeTypeDetectors extends MimeTypeDetector {
    private static final Logger LOGGER = Logger.getLogger(MimeTypeDetectors.class);

    private List<MimeTypeDetector> detectors = new ArrayList<MimeTypeDetector>();

    public MimeTypeDetectors() {
        this(null);
    }

    public MimeTypeDetectors( Environment environment ) {
        ClassLoader defaultLoader = getClass().getClassLoader();
        //the extra classpath entry is the package name of the tika extractor, so it can be located inside AS7 (see RepositoryService)
        ClassLoader classLoader = environment != null ? environment.getClassLoader(defaultLoader,
                                                                                   "org.modeshape.extractor.tika")
                : defaultLoader;
        loadDetectors(classLoader);
    }

    private void loadDetectors( ClassLoader classLoader ) {
        //tika
        if (tryToLoadClass(classLoader, "org.apache.tika.detect.DefaultDetector",
                           "org.modeshape.extractor.tika.TikaMimeTypeDetector")) {
            addDetector(classLoader, "org.modeshape.extractor.tika.TikaMimeTypeDetector");
        }

        //aperture
        if (tryToLoadClass(classLoader, "org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier")) {
            addDetector(classLoader, "org.modeshape.jcr.mimetype.ApertureMimeTypeDetector");
        }

        //extension-based (is always present)
        detectors.add(ExtensionBasedMimeTypeDetector.INSTANCE);
    }

    private void addDetector( ClassLoader classLoader,
                              String fqnClassName ) {
        try {
            MimeTypeDetector detector = (MimeTypeDetector)classLoader.loadClass(fqnClassName).newInstance();
            detectors.add(detector);
        } catch (Exception e) {
            LOGGER.error(e, RepositoryI18n.unableToLoadMimeTypeDetector, fqnClassName, e.getMessage());
        }
    }

    private boolean tryToLoadClass( ClassLoader classLoader,
                                    String... fqnClassNames ) {
        try {
            for (String fqnClassName : fqnClassNames) {
                Class.forName(fqnClassName, false, classLoader);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the first non-null result of iterating over the registered MIME-type detectors
     * If the MIME-type cannot be determined by any registered detector, "text/plain" or "application/octet-stream" will be returned, the
     * former only if it is determined the stream contains no nulls.
     *
     * @see MimeTypeDetector#mimeTypeOf(String, javax.jcr.Binary)
     */
    @Override
    public String mimeTypeOf( String name,
                              Binary binaryValue ) throws RepositoryException, IOException {
        String detectedMimeType = detectMimeTypeUsingDetectors(name, binaryValue);
        return detectedMimeType != null ? detectedMimeType : detectFallbackMimeType(binaryValue);
    }

    private String detectFallbackMimeType( Binary binaryValue ) throws RepositoryException, IOException {
        return processStream(binaryValue, new StreamOperation<String>() {
            @Override
            public String execute( InputStream stream ) throws IOException {
                try {
                    for (int chr = stream.read(); chr >= 0; chr = stream.read()) {
                        if (chr == 0) {
                            return OCTET_STREAM;
                        }
                    }
                } catch (IOException meansTooManyBytesRead) {
                    return OCTET_STREAM;
                }
                return TEXT_PLAIN;
            }
        });
    }

    private String detectMimeTypeUsingDetectors( String name,
                                                 Binary binary ) throws RepositoryException, IOException {
        for (MimeTypeDetector detector : detectors) {
            String mimeType = detector.mimeTypeOf(name, binary);
            if (mimeType != null) {
                return mimeType;
            }
        }
        return null;
    }
}

/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.mimetype;

import java.io.IOException;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Problems;
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
        this(null, null);
    }

    /**
     * Creates a new instance with a given environment and optional problems collector.
     *
     * @param environment an {@link Environment}; possibly null
     * @param problems an {@link Problems} instance; possibly null;
     */
    public MimeTypeDetectors( Environment environment, Problems problems ) {
        ClassLoader defaultLoader = getClass().getClassLoader();
        // the extra classpath entry is the package name of the tika extractor, so it can be located inside AS7 (see
        // RepositoryService)
        ClassLoader classLoader = environment != null ? environment.getClassLoader(defaultLoader, "org.modeshape.extractor.tika") : defaultLoader;
        MimeTypeDetector delegate = null;
        try {
            delegate = new TikaMimeTypeDetector(classLoader);
        } catch (Throwable e) {
            delegate = NullMimeTypeDetector.INSTANCE;
            LOGGER.warn(e, JcrI18n.noMimeTypeDetectorsFound);
            if (problems != null) {
                problems.addWarning(e, JcrI18n.noMimeTypeDetectorsFound);
            }
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

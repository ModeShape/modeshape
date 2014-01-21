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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.mime.MimeTypesFactory;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;

/**
 * A {@link MimeTypeDetector} that uses the Tika library.
 */
@Immutable
@ThreadSafe
public final class TikaMimeTypeDetector implements MimeTypeDetector {

    private static final Logger LOGGER = Logger.getLogger(TikaMimeTypeDetector.class);

    protected final MimeTypes mimetypes;
    private final CompositeDetector allDetectors;
    private final Detector nameOnlyDetector;

    public TikaMimeTypeDetector( ClassLoader classLoader ) {

        // Add these files in this order, since those read in later will overwrite the entries read in previously,
        // and we want ModeShape's custom MIME types file to override everything else.
        List<URL> validUrls = new ArrayList<URL>(3);
        validUrls.add(classLoader.getResource("org/apache/tika/mime/tika-mimetypes.xml"));
        validUrls.add(classLoader.getResource("org/apache/tika/mime/custom-tika-mimetypes.xml"));
        validUrls.add(classLoader.getResource("org/modeshape/custom-mimetypes.xml"));

        // Remove any null URL or one that is not in the correct format ...
        Iterator<URL> iter = validUrls.iterator();
        while (iter.hasNext()) {
            URL url = iter.next();
            if (url == null) {
                iter.remove();
                continue;
            }
            try {
                // Read in the URLs, with the most custom ones last as they override the MIME type patterns already read in ...
                MimeTypesFactory.create(url);
            } catch (Exception e) {
                LOGGER.warn(e, JcrI18n.unableToReadMediaTypeRegistry, url, e.getMessage());
                iter.remove();
            }
        }

        URL[] urls = validUrls.toArray(new URL[validUrls.size()]);
        try {
            mimetypes = MimeTypesFactory.create(urls);
        } catch (Exception e) {
            throw new SystemFailureException(JcrI18n.unableToInitializeMimeTypeDetector.text(urls, e.getMessage()), e);
        }
        // Create the detectors ...
        // this.allDetectors = new DefaultDetector(classLoader);
        this.allDetectors = new DefaultDetector(mimetypes, classLoader);
        this.nameOnlyDetector = mimetypes;

        LOGGER.debug("Initializing the Tika MIME type detectors");
        for (Detector detector : allDetectors.getDetectors()) {
            LOGGER.debug(" - Found detector: " + detector.getClass().getName());
        }
    }

    @Override
    public String mimeTypeOf( final String name,
                              final Binary binaryValue ) throws RepositoryException, IOException {
        Metadata metadata = new Metadata();
        if (!StringUtil.isBlank(name)) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, name);
        }
        MediaType autoDetectedMimeType = null;
        if (binaryValue == null) {
            if (name == null) {
                return null;
            }
            // Otherwise there is a name and no content ...
            autoDetectedMimeType = nameOnlyDetector.detect(null, metadata);
        } else {
            InputStream stream = binaryValue.getStream();
            TemporaryResources tmp = new TemporaryResources();
            try {
                TikaInputStream tikaInputStream = TikaInputStream.get(stream, tmp);
                // There is content and possibly a name ...
                autoDetectedMimeType = allDetectors.detect(tikaInputStream, metadata);
            } finally {
                try {
                    tmp.close();
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }
            }
        }
        return autoDetectedMimeType != null ? autoDetectedMimeType.toString() : null;
    }
}

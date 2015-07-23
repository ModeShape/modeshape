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

import java.io.InputStream;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrI18n;

/**
 * {@link MimeTypeDetector} implementation which uses Apache Tika to determine the mimetype of a given binary, based only
 * on the name of the binary property.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
@ThreadSafe
public final class NameOnlyDetector extends TikaMimeTypeDetector {
    
    private Detector detector;

    /**
     * Creates a new name based detector
     *
     * @param environment the {@link Environment} to use for class loading; may not be {@code null}
     */
    public NameOnlyDetector( Environment environment ) {
        super(environment);
    }
    
    @Override
    protected void initDetector( ClassLoader loader ) {
        // this will also load ModeShape's custom-mimetypes.xml because it's placed in a org.apache.tika.mime package
        try {
            this.detector = MimeTypes.getDefaultMimeTypes(loader);
        } catch (Throwable t) {
            throw new SystemFailureException(JcrI18n.unableToInitializeMimeTypeDetector.text(t.getMessage()), t);
        }
    }

    @Override
    protected String detect( InputStream inputStream, Metadata metadata ) {
        try {
            // we never care about the stream here
            MediaType detectedMimeType = detector.detect(null, metadata);
            if (logger.isTraceEnabled()) {
                logger.trace("MIME type for '" + metadata.get(Metadata.RESOURCE_NAME_KEY) + "' ==> " + detectedMimeType);
            }
            return detectedMimeType.toString();
        } catch (Exception e) {
            logger.debug(e, "Unable to extract mime-type");
            return null;
        }
    }
}

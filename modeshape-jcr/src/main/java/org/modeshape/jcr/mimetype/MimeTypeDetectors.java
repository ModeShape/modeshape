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

import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.mimetype.tika.TikaContentDetector;
import org.modeshape.jcr.mimetype.tika.TikaNameOnlyDetector;

/**
 * Class which decides which {@link MimeTypeDetector} implementation to use at runtime
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class MimeTypeDetectors {
    
    private static final Logger LOGGER = Logger.getLogger(MimeTypeDetector.class);
    private static final boolean TIKA_AVAILABLE; 
    
    static {
        boolean tikaAvailable = true;
        ClassLoader classLoader = MimeTypeDetector.class.getClassLoader();
        try {
            Class.forName("org.apache.tika.detect.DefaultDetector", true, classLoader);
            Class.forName("org.apache.tika.metadata.Metadata", true, classLoader);
        } catch (Throwable t) {
            tikaAvailable = false;
        }
        TIKA_AVAILABLE = tikaAvailable;
        if (LOGGER.isDebugEnabled()) {
            if (TIKA_AVAILABLE) {
                LOGGER.debug("Tika is available in the CP; will be used for mimetype detection");
            } else {
                LOGGER.debug("Tika is not available in the CP; ModeShape will use a DefaultMimetypeDetector");
            }
        }
    }
    
    private MimeTypeDetectors() {
    }
    
    /**
     * Returns a new mime type detector implementation based on 
     * the repository {@link org.modeshape.jcr.RepositoryConfiguration.FieldName#MIMETYPE_DETECTION} configuration 
     *  
     * @param mimeTypeDetectionConfig a {@code String}, may not be null
     * @param environment an {@link Environment} instance specific to a repository 
     * @return a {@link MimeTypeDetector} instance
     */
    public static MimeTypeDetector createDetectorFor(String mimeTypeDetectionConfig, Environment environment) {
        switch (mimeTypeDetectionConfig.toLowerCase()) {
            case RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_CONTENT: {
                return TIKA_AVAILABLE ? new TikaContentDetector(environment) : new DefaultMimeTypeDetector();
            }
            case RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_NAME: {
                return TIKA_AVAILABLE ? new TikaNameOnlyDetector(environment) : new DefaultMimeTypeDetector();
            }
            case RepositoryConfiguration.FieldValue.MIMETYPE_DETECTION_NONE: {
                return NullMimeTypeDetector.INSTANCE;
            }
            default: {
                throw new IllegalArgumentException("Unknown mime-type detector setting: " + mimeTypeDetectionConfig);
            }
        }   
    }
}

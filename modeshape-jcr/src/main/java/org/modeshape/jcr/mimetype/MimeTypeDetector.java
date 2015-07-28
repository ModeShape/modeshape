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

/**
 * MIME-type detection libraries must provide thread-safe implementations of this interface to enable ModeShape to use the
 * libraries to return MIME-types for data sources.
 */
public interface MimeTypeDetector {

    /**
     * Returns the MIME-type of a binary value, using its supplied content and/or its supplied name, depending upon the
     * implementation. If the MIME-type cannot be determined, either a "default" MIME-type or <code>null</code> may be returned,
     * where the former will prevent earlier registered MIME-type detectors from being consulted.
     * 
     * @param name The name of the data source; may be <code>null</code>.
     * @param binaryValue The value which contains the raw data for which the mime type should be returned; may be
     *        <code>null</code>.
     * @return The MIME-type of the data source, or optionally <code>null</code> if the MIME-type could not be determined.
     * @throws IOException If an error occurs reading the supplied content.
     * @throws RepositoryException if any error occurs while attempting to read the stream from the binary value
     */
    String mimeTypeOf( String name,
                       Binary binaryValue ) throws RepositoryException, IOException;
}

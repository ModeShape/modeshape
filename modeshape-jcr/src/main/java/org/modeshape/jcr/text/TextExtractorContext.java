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
package org.modeshape.jcr.text;

import java.io.IOException;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.mimetype.MimeTypeDetector;

/**
 * A context for extracting the content.
 */
public final class TextExtractorContext implements TextExtractor.Context {

    private final MimeTypeDetector detector;

    public TextExtractorContext( MimeTypeDetector detector ) {
        this.detector = detector;
        assert this.detector != null;
    }

    @Override
    public String mimeTypeOf( String name,
                              Binary binaryValue ) throws RepositoryException, IOException {
        return detector.mimeTypeOf(name, binaryValue);
    }
}

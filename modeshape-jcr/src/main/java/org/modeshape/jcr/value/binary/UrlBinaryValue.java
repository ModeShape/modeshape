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
package org.modeshape.jcr.value.binary;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryValue} implementation used to read the content of a resolvable URL. This class computes the
 * {@link AbstractBinary#getMimeType() MIME type} lazily.
 */
public class UrlBinaryValue extends ExternalBinaryValue {
    private static final long serialVersionUID = 1L;

    private URL url;

    public UrlBinaryValue( String sha1,
                           String sourceName,
                           URL content,
                           long size,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(sha1), sourceName, content.toExternalForm(), size, nameHint, mimeTypeDetector);
        this.url = content;
    }

    protected URL toUrl() {
        return url;
    }

    @Override
    protected InputStream internalStream() throws IOException {
        return new BufferedInputStream(url.openStream());
    }
}

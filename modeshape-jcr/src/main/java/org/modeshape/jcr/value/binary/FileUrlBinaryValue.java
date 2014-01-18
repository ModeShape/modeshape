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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A subclass of {@link UrlBinaryValue} to lazily compute a contentBased Hash when the key is a URI based Hash
 */
@ThreadSafe
public class FileUrlBinaryValue extends UrlBinaryValue {
    private static final long serialVersionUID = 1L;

    private BinaryKey hash;
    private URL content;

    public FileUrlBinaryValue( String sha1,
                               String sourceName,
                               URL content,
                               long size,
                               String nameHint,
                               MimeTypeDetector mimeTypeDetector ) {
        super(sha1, sourceName, content, size, nameHint, mimeTypeDetector);
        this.content = content;
    }

    @Override
    public byte[] getHash() {
        return generateHash().toBytes();
    }

    @Override
    public String getHexHash() {
        return generateHash().toString();
    }

    private synchronized BinaryKey generateHash() {
        if (this.hash == null) {
            try {
                byte[] hashBytes = SecureHash.getHash(SecureHash.Algorithm.SHA_1, convertURLtoFile(this.content));
                this.hash = new BinaryKey(hashBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this.hash;
    }

    private File convertURLtoFile( URL url ) {
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }

}

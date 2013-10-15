package org.modeshape.jcr.value.binary;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.RepositoryException;
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

    public UrlBinaryValue( String sourceName,
                           URL content,
                           long size,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(content.toString()), sourceName, content.toExternalForm(), size, nameHint, mimeTypeDetector);
        this.url = content;
    }

    protected URL toUrl() {
        return url;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return new BufferedInputStream(url.openStream());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}

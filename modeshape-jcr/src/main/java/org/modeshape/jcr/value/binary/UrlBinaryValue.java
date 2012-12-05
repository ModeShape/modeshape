package org.modeshape.jcr.value.binary;

import java.io.BufferedInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryValue} implementation used to read the content of a resolvable URL. This class computes the
 * {@link AbstractBinary#getMimeType() MIME type} lazily or upon serialization.
 */
public class UrlBinaryValue extends AbstractBinary implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient MimeTypeDetector mimeTypeDetector;
    private transient String nameHint; // only needed for MIME type detection; not needed once MIME type is known
    private String mimeType;
    private URL url;
    private long size;
    private boolean detectedMimeType = false;

    public UrlBinaryValue( BinaryKey binaryKey,
                           URL content,
                           long size,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(binaryKey);
        this.url = content;
        this.size = size;
        this.nameHint = nameHint;
        this.mimeTypeDetector = mimeTypeDetector;
    }

    protected URL toUrl() {
        return url;
    }

    protected void setMimeType( String mimeType ) {
        this.mimeType = mimeType;
    }

    protected boolean hasMimeType() {
        return mimeType != null;
    }

    @Override
    public String getMimeType() {
        if (!detectedMimeType && mimeTypeDetector != null) {
            try {
                mimeType = mimeTypeDetector.mimeTypeOf(nameHint, this);
            } catch (Throwable t) {
                Logger.getLogger(getClass()).debug("Unable to compute MIME Type for file at {0}", toUrl());
                throw new RuntimeException(t);
            } finally {
                detectedMimeType = true;
            }
        }
        return mimeType;
    }

    @Override
    public String getMimeType( String name ) {
        return getMimeType();
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return new BufferedInputStream(url.openStream());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        this.mimeType = in.readUTF();
        this.detectedMimeType = in.readBoolean();
        this.url = (URL)in.readObject();
        this.size = in.readLong();
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeUTF(getMimeType());
        out.writeBoolean(detectedMimeType);
        out.writeObject(this.url);
        out.writeLong(this.size);
    }
}

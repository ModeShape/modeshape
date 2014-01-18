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

import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;

/**
 * {@link org.modeshape.jcr.value.BinaryValue} implementation that represents a binary value that resides outside of ModeShape's
 * binary store. Typically this will be subclasses by {@code Connector} implementations that wish to provide their own binaries
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "serial" )
public abstract class ExternalBinaryValue extends AbstractBinary {

    private transient MimeTypeDetector mimeTypeDetector;
    private transient String nameHint; // only needed for MIME type detection; not needed once MIME type is known

    private final String id;
    private String mimeType;
    private long size;
    private boolean detectedMimeType = false;
    private String sourceName;

    /**
     * Creates a new instance, with the given params
     * 
     * @param key the binary key, never {@code null}
     * @param sourceName name of the external source which owns the value, {@code never null}
     * @param id the source-specific identifier of the binary, never {@code null}
     * @param size the length of the binary
     * @param nameHint optional name which can help with mime-type detection
     * @param mimeTypeDetector the repository's {@link MimeTypeDetector}
     */
    protected ExternalBinaryValue( BinaryKey key,
                                   String sourceName,
                                   String id,
                                   long size,
                                   String nameHint,
                                   MimeTypeDetector mimeTypeDetector ) {
        super(key);
        assert id != null;
        this.id = id;
        this.sourceName = sourceName;
        this.size = size;
        this.nameHint = nameHint;
        this.mimeTypeDetector = mimeTypeDetector;
    }

    /**
     * Returns this binary's source-specific identifier.
     * 
     * @return a non-null string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the external source, to which this binary belongs.
     * 
     * @return a non-null string
     */
    public String getSourceName() {
        return sourceName;
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
                Logger.getLogger(getClass()).debug("Unable to compute MIME Type for external binary with id {0}", getId());
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
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ExternalBinaryValue");
        sb.append("(sourceName='").append(sourceName).append('\'');
        sb.append(", id='").append(getId()).append('\'');
        sb.append(')');
        return sb.toString();
    }
}

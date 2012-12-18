/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
public abstract class ExternalBinaryValue extends AbstractBinary {

    private transient MimeTypeDetector mimeTypeDetector;
    private transient String nameHint; // only needed for MIME type detection; not needed once MIME type is known

    private String mimeType;
    private long size;
    private boolean detectedMimeType = false;
    private String sourceName;

    /**
     * Creates a new instance, with the given params
     * @param id the binary id, never {@code null}
     * @param sourceName name of the external source which owns the value, {@code never null}
     * @param size the length of the binary
     * @param nameHint optional name which can help with mime-type detection
     * @param mimeTypeDetector the repository's {@link MimeTypeDetector}
     */
    public ExternalBinaryValue( String id,
                                String sourceName,
                                long size,
                                String nameHint,
                                MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(id));

        this.sourceName = sourceName;
        this.size = size;
        this.nameHint = nameHint;
        this.mimeTypeDetector = mimeTypeDetector;
    }

    /**
     * Returns this binary's id.
     *
     * @return a non-null string
     */
    public String getId() {
        return getKey().toString();
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

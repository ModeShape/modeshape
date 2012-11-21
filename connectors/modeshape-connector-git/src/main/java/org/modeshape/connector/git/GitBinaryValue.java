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
package org.modeshape.connector.git;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.jcr.RepositoryException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinary;

/**
 * A {@link BinaryValue} implementation used to read the content of a specific object ID from the supplied repository. This class
 * computes the {@link AbstractBinary#getMimeType() MIME type} lazily or upon serialization.
 */
public class GitBinaryValue extends AbstractBinary implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient MimeTypeDetector mimeTypeDetector;
    private transient String nameHint; // only needed for MIME type detection; not needed once MIME type is known
    private transient ObjectLoader loader;
    private transient ObjectId id;
    private String mimeType;
    private long size = -1L;
    private boolean detectedMimeType = false;
    private byte[] serializedBytes;

    public GitBinaryValue( ObjectId id,
                           ObjectLoader loader,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(id.getName()));
        this.id = id;
        this.loader = loader;
        this.size = loader.getSize();
        this.nameHint = nameHint;
        this.mimeTypeDetector = mimeTypeDetector;
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
                Logger.getLogger(getClass()).debug("Unable to compute MIME Type for file {0}", id);
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
            if (serializedBytes != null) {
                return new ByteArrayInputStream(serializedBytes);
            }
            return new BufferedInputStream(loader.openStream());
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException {
        this.mimeType = in.readUTF();
        this.detectedMimeType = in.readBoolean();
        this.size = in.readLong();
        int numBytes = in.readInt();
        this.serializedBytes = new byte[numBytes];
        in.read(this.serializedBytes);
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeUTF(getMimeType());
        out.writeBoolean(detectedMimeType);
        out.writeLong(getSize());
        byte[] bytes = loader.getCachedBytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}

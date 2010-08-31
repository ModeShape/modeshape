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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import org.modeshape.graph.property.Binary;

/**
 * An implementation of JCR 2.0 Binary that wraps a graph {@link Binary} value object.
 */
class JcrBinary implements javax.jcr.Binary, org.modeshape.jcr.api.Binary {

    private final Binary binary;

    JcrBinary( Binary binary ) {
        this.binary = binary;
        assert this.binary != null;
    }

    Binary binary() {
        return this.binary;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.Binary#dispose()
     */
    @Override
    public void dispose() {
        this.binary.release();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.Binary#getSize()
     */
    @Override
    public long getSize() {
        try {
            this.binary.acquire();
            return this.binary.getSize();
        } finally {
            this.binary.release();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.Binary#getStream()
     */
    @Override
    public InputStream getStream() {
        return new SelfClosingInputStream(this.binary);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.Binary#read(byte[], long)
     */
    @Override
    public int read( byte[] b,
                     long position ) throws IOException {
        if (getSize() <= position) return -1;
        InputStream stream = null;
        IOException error = null;
        try {
            stream = getStream();
            // Read/skip the next 'position' bytes ...
            long skip = position;
            while (skip > 0) {
                long skipped = stream.skip(skip);
                if (skipped <= 0) return -1;
                skip -= skipped;
            }
            return stream.read(b);
        } catch (IOException e) {
            error = e;
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (RuntimeException t) {
                    // Only throw if we've not already thrown an exception ...
                    if (error == null) throw t;
                } catch (IOException t) {
                    // Only throw if we've not already thrown an exception ...
                    if (error == null) throw t;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return binary.toString();
    }

}

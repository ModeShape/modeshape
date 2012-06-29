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


import javax.jcr.RepositoryException;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * An in-memory implementation of a {@link javax.jcr.Binary} which should used for tests.
 *
 * @author Horia Chiorean
 */
public final class InMemoryTestBinary implements Binary {

    private byte[] bytes;
    private byte[] hash;

    public InMemoryTestBinary( byte[] bytes ) {
        try {
            this.bytes = bytes;
            this.hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public InMemoryTestBinary(InputStream is) throws IOException {
        this(IoUtil.readBytes(is));
    }

    @Override
    public void dispose() {
        this.bytes = null;
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public int read( byte[] b,
                     long position ) throws IOException, RepositoryException {
        if (getSize() <= position) {
            return -1;
        }
        InputStream stream = null;
        IOException error = null;
        try {
            stream = getStream();
            // Read/skip the next 'position' bytes ...
            long skip = position;
            while (skip > 0) {
                long skipped = stream.skip(skip);
                if (skipped <= 0) {
                    return -1;
                }
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
                }
                catch (IOException t) {
                    // Only throw if we've not already thrown an exception ...
                    if (error == null) {
                        throw t;
                    }
                }
            }
        }
    }

    @Override
    public long getSize() throws RepositoryException {
        return bytes.length;
    }

    @Override
    public byte[] getHash() {
        return hash;
    }

    @Override
    public String getHexHash() {
        return StringUtil.getHexString(hash);
    }

    @Override
    public String getMimeType() throws IOException, RepositoryException {
        return null;
    }

    @Override
    public String getMimeType( String name ) throws IOException, RepositoryException {
        return null;
    }
}

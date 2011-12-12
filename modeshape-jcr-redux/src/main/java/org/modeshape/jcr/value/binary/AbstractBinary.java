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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import javax.jcr.RepositoryException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.ValueComparators;

/**
 * An abstract implementation of {@link Binary} that provides some common capabilities for other implementations.
 */
@Immutable
public abstract class AbstractBinary implements Binary {

    protected static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    public static BinaryKey keyFor( byte[] sha1 ) {
        try {
            byte[] hash = SecureHash.getHash(Algorithm.SHA_1, sha1);
            return new BinaryKey(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

    private final BinaryKey key;

    protected AbstractBinary( BinaryKey key ) {
        this.key = key;
        assert this.key != null;
    }

    @Override
    public int read( byte[] b,
                     long position ) throws IOException, RepositoryException {
        if (getSize() <= position) return -1;
        InputStream stream = null;
        Exception error = null;
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
        } catch (RepositoryException e) {
            error = e;
            throw e;
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

    @Override
    public BinaryKey getKey() {
        return key;
    }

    @Override
    public byte[] getHash() {
        return key.toBytes();
    }

    @Override
    public String getHexHash() {
        return key.toString();
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public int compareTo( Binary o ) {
        if (o == this) return 0;
        return ValueComparators.BINARY_COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Binary) {
            Binary that = (Binary)obj;
            return ValueComparators.BINARY_COMPARATOR.compare(this, that) == 0;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("binary (");
        sb.append(getReadableSize());
        sb.append(", SHA1=");
        sb.append(getHexHash());
        sb.append(')');
        return sb.toString();
    }

    public String getReadableSize() {
        long size = getSize();
        float decimalInKb = size / 1024.0f;
        if (decimalInKb < 1) return Long.toString(size) + "B";
        float decimalInMb = decimalInKb / 1024.0f;
        if (decimalInMb < 1) return new DecimalFormat("#,##0.00").format(decimalInKb) + "KB";
        float decimalInGb = decimalInMb / 1024.0f;
        if (decimalInGb < 1) return new DecimalFormat("#,##0.00").format(decimalInMb) + "MB";
        float decimalInTb = decimalInGb / 1024.0f;
        if (decimalInTb < 1) return new DecimalFormat("#,##0.00").format(decimalInGb) + "GB";
        return new DecimalFormat("#,##0.00").format(decimalInTb) + "TB";
    }

}

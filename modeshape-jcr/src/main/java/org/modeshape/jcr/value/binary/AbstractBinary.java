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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import javax.jcr.RepositoryException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.SelfClosingInputStream;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.ValueComparators;

/**
 * An abstract implementation of {@link BinaryValue} that provides some common capabilities for other implementations.
 */
@Immutable
public abstract class AbstractBinary implements BinaryValue {

    protected static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    private final BinaryKey key;

    public static BinaryKey keyFor( byte[] sha1 ) {
        try {
            byte[] hash = SecureHash.getHash(Algorithm.SHA_1, sha1);
            return new BinaryKey(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

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
    public int compareTo( BinaryValue o ) {
        if (o == this) return 0;
        return ValueComparators.BINARY_COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof BinaryValue) {
            BinaryValue that = (BinaryValue)obj;
            return ValueComparators.BINARY_COMPARATOR.compare(this, that) == 0;
        }
        return false;
    }

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

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return new SelfClosingInputStream(internalStream());
        } catch (RepositoryException re) {
            throw re;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    protected abstract InputStream internalStream() throws Exception;
}

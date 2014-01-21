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
package org.modeshape.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * The JDBC {@link Blob} wrapper around a JCR binary {@link Value} object.
 */
public class JcrBlob implements Blob {

    private final Value value;
    private final long length;

    /**
     * Create a JDBC Blob object around the supplied JCR Value, specifying the length. Note the length can be determined from the
     * {@link Property} via {@link Property#getLength()} or {@link Property#getLengths()}.
     * 
     * @param value the JCR value; may not be null
     * @param length the length
     */
    protected JcrBlob( Value value,
                       long length ) {
        this.value = value;
        this.length = length;
        assert this.value != null;
    }

    @Override
    public void free() throws SQLException {
        try {
            // Get the binary value ...
            Binary binary = value.getBinary();
            binary.dispose();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        try {
            return value.getBinary().getStream();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public InputStream getBinaryStream( long pos,
                                        long length ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public byte[] getBytes( long pos,
                            int length ) throws SQLException {
        SQLException error = null;
        try {
            byte[] data = new byte[length];
            int numRead = 0;
            // Get the binary value ...
            Binary binary = value.getBinary();
            try {
                numRead = binary.read(data, pos);
            } finally {
                binary.dispose();
            }

            // We may have read less than the desired length ...
            if (numRead < length) {
                // create a shortened array ...
                byte[] shortData = new byte[numRead];
                System.arraycopy(data, 0, shortData, 0, numRead);
                data = shortData;
            }
            return data;
        } catch (IOException e) {
            error = new SQLException(e.getLocalizedMessage(), e);
            throw error;
        } catch (RepositoryException e) {
            error = new SQLException(e.getLocalizedMessage(), e);
            throw error;
        }
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long position( byte[] pattern,
                          long start ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public long position( Blob pattern,
                          long start ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public OutputStream setBinaryStream( long pos ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int setBytes( long pos,
                         byte[] bytes ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int setBytes( long pos,
                         byte[] bytes,
                         int offset,
                         int len ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void truncate( long len ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

}

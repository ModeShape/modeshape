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

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * The JDBC {@link Blob} wrapper around a JCR binary {@link Value} object.
 */
public class JcrBlob implements Blob {

    private final Binary binary;

    /**
     * Creates a new {@link java.sql.Blob} by wrapping a JCR binary
     * 
     * @param binary a {@link javax.jcr.Binary} instance; may not be null
     */
    public JcrBlob( Binary binary ) {
        this.binary = binary;
        assert this.binary != null;
    }

    @Override
    public void free() {
        binary.dispose();
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        try {
            return binary.getStream();
        } catch (RepositoryException e) {
            throw new SQLException(e);
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
        try {
            byte[] data = new byte[length];
            int numRead = 0;
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
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public long length() throws SQLException {
        try {
            return binary.getSize();
        } catch (RepositoryException e) {
            throw new SQLException(e);
        }
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

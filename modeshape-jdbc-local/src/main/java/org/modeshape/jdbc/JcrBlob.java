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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#free()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#getBinaryStream()
     */
    @Override
    public InputStream getBinaryStream() throws SQLException {
        try {
            return value.getBinary().getStream();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#getBinaryStream(long, long)
     */
    @Override
    public InputStream getBinaryStream( long pos,
                                        long length ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#getBytes(long, int)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#length()
     */
    @Override
    public long length() {
        return length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#position(byte[], long)
     */
    @Override
    public long position( byte[] pattern,
                          long start ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#position(java.sql.Blob, long)
     */
    @Override
    public long position( Blob pattern,
                          long start ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#setBinaryStream(long)
     */
    @Override
    public OutputStream setBinaryStream( long pos ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#setBytes(long, byte[])
     */
    @Override
    public int setBytes( long pos,
                         byte[] bytes ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#setBytes(long, byte[], int, int)
     */
    @Override
    public int setBytes( long pos,
                         byte[] bytes,
                         int offset,
                         int len ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Blob#truncate(long)
     */
    @Override
    public void truncate( long len ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

}

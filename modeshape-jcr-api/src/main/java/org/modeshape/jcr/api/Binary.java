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
package org.modeshape.jcr.api;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;

/**
 * Replicates JCR 2.0's javax.jcr.Binary interface.
 */
public interface Binary {

    /**
     * Returns an InputStream representation of this value. Each call to getStream() returns a new stream. The API consumer is
     * responsible for calling close() on the returned stream. If dispose() has been called on this Binary object, then this
     * method will throw the runtime exception IllegalStateException.
     * 
     * @return A stream representation of this value.
     * @throws RepositoryException if an error occurs.
     */
    public InputStream getStream() throws RepositoryException;

    /**
     * Reads successive bytes from the specified position in this Binary into the passed byte array until either the byte array is
     * full or the end of the Binary is encountered. If dispose() has been called on this Binary object, then this method will
     * throw the runtime exception IllegalStateException.
     * 
     * @param b - the buffer into which the data is read.
     * @param position - the position in this Binary from which to start reading bytes.
     * @return the number of bytes read into the buffer, or -1 if there is no more data because the end of the Binary has been
     *         reached.
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if b is null.
     * @throws IllegalArgumentException if offset is negative.
     * @throws RepositoryException if another error occurs.
     */
    public int read( byte[] b,
                     long position ) throws IOException, RepositoryException;

    /**
     * Returns the size of this Binary value in bytes. If dispose() has been called on this Binary object, then this method will
     * throw the runtime exception IllegalStateException.
     * 
     * @return the size of this value in bytes.
     * @throws RepositoryException if an error occurs.
     */
    public long getSize() throws RepositoryException;

    /**
     * Releases all resources associated with this Binary object and informs the repository that these resources may now be
     * reclaimed. An application should call this method when it is finished with the Binary object.
     */
    public void dispose();

}

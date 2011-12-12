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

import java.security.MessageDigest;

/**
 * An extension of the standard {@link javax.jcr.Binary} interface, with methods to obtain the SHA-1 hash of the binary value.
 */
public interface Binary extends javax.jcr.Binary {

    /**
     * Get the SHA-1 hash of the contents. This hash can be used to determine whether two Binary instances contain the same
     * content.
     * <p>
     * Repeatedly calling this method should generally be efficient, as it most implementations will compute the hash only once.
     * </p>
     * 
     * @return the hash of the contents as a byte array, or an empty array if the hash could not be computed.
     * @see MessageDigest#digest(byte[])
     * @see MessageDigest#getInstance(String)
     * @see #getHexHash()
     */
    public byte[] getHash();

    /**
     * Get the hexadecimal form of the SHA-1 hash of the contents. This hash can be used to determine whether two Binary instances
     * contain the same content.
     * <p>
     * Repeatedly calling this method should generally be efficient, as it most implementations will compute the hash only once.
     * </p>
     * 
     * @return the hexadecimal form of the {@link #getHash()}, or a null string if the hash could not be computed or is not known
     * @see MessageDigest#digest(byte[])
     * @see MessageDigest#getInstance(String)
     * @see #getHash()
     */
    public String getHexHash();

}

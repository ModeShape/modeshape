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
package org.modeshape.jcr.api;

import java.io.IOException;
import java.security.MessageDigest;
import javax.jcr.RepositoryException;

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

    /**
     * Get the MIME type for this binary value.
     * 
     * @return the MIME type, or null if it cannot be determined (e.g., the Binary is empty)
     * @throws IOException if there is a problem reading the binary content
     * @throws RepositoryException if an error occurs.
     */
    public String getMimeType() throws IOException, RepositoryException;

    /**
     * Get the MIME type for this binary value.
     * 
     * @param name the name of the binary value, useful in helping to determine the MIME type
     * @return the MIME type, or null if it cannot be determined (e.g., the Binary is empty)
     * @throws IOException if there is a problem reading the binary content
     * @throws RepositoryException if an error occurs.
     */
    public String getMimeType( String name ) throws IOException, RepositoryException;

}

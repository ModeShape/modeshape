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
package org.modeshape.graph.property;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import net.jcip.annotations.ThreadSafe;

/**
 * A factory for creating {@link Binary} instances. This interface extends the {@link ValueFactory} generic interface and adds
 * specific methods for creating binary objects.
 */
@ThreadSafe
public interface BinaryFactory extends ValueFactory<Binary> {

    /**
     * Create a value from the binary content given by the supplied stream, the approximate length, and the SHA-1 secure hash of
     * the content. If the secure hash is null, then a secure hash is computed from the content. If the secure hash is not null,
     * it is assumed to be the hash for the content and may not be checked.
     * 
     * @param stream the stream containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @param secureHash the secure hash of the content in the <code>stream</code>; if null, the secure hash is computed from the
     *        content, and if not null it is assumed to be the correct secure hash (and is not checked)
     * @return the value, or null if the supplied stream is null
     * @throws ValueFormatException if the conversion from an input stream could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied stream (such as an {@link IOException}).
     * @throws IllegalArgumentException if the secure hash was discovered to be incorrect
     */
    Binary create( InputStream stream,
                   long approximateLength,
                   byte[] secureHash ) throws ValueFormatException, IoException;

    /**
     * Create a value from the binary content given by the supplied reader, the approximate length, and the SHA-1 secure hash of
     * the content. If the secure hash is null, then a secure hash is computed from the content. If the secure hash is not null,
     * it is assumed to be the hash for the content and may not be checked.
     * 
     * @param reader the reader containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @param secureHash the secure hash of the content in the <code>stream</code>; if null, the secure hash is computed from the
     *        content, and if not null it is assumed to be the correct secure hash (and is not checked)
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a reader could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied reader (such as an {@link IOException}).
     * @throws IllegalArgumentException if the secure hash was discovered to be incorrect
     */
    Binary create( Reader reader,
                   long approximateLength,
                   byte[] secureHash ) throws ValueFormatException, IoException;

    /**
     * Create a binary value from the given file.
     * 
     * @param file the file containing the content to be used
     * @return the binary value, or null if the file parameter was null
     * @throws ValueFormatException if the conversion from the file could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied file (such as an {@link IOException}).
     */
    Binary create( File file ) throws ValueFormatException, IoException;

    /**
     * Find an existing binary value given the supplied secure hash. If no such binary value exists, null is returned. This method
     * can be used when the caller knows the secure hash (e.g., from a previously-held Binary object), and would like to reuse an
     * existing binary value (if possible) rather than recreate the binary value by processing the stream contents. This is
     * especially true when the size of the binary is quite large.
     * 
     * @param secureHash the secure hash of the binary content, which was probably {@link Binary#getHash() obtained} from a
     *        previously-held {@link Binary} object; a null or empty value is allowed, but will always result in returning null
     * @return the existing Binary value that has the same secure hash, or null if there is no such value available at this time
     */
    Binary find( byte[] secureHash );
}

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
package org.modeshape.common.text;

import java.security.NoSuchAlgorithmException;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;

/**
 * A text encoder that performs a secure hash of the input text and returns that hash as the encoded text. This encoder can be
 * configured to use different secure hash algorithms and to return a fixed number of characters from the hash.
 */
public class SecureHashTextEncoder implements TextEncoder {

    private final Algorithm algorithm;
    private final int maxLength;

    /**
     * Create an encoder that uses the supplied algorithm and returns only the supplied number of characters in the hash.
     * 
     * @param algorithm the algorithm that should be used
     * @throws IllegalArgumentException if the algorithm is null
     */
    public SecureHashTextEncoder( Algorithm algorithm ) {
        this(algorithm, Integer.MAX_VALUE);
    }

    /**
     * Create an encoder that uses the supplied algorithm and returns only the supplied number of characters in the hash.
     * 
     * @param algorithm the algorithm that should be used
     * @param maxLength the maximumLength, or a non-positive number (or {@link Integer#MAX_VALUE}) if the full hash should be used
     * @throws IllegalArgumentException if the algorithm is null
     */
    public SecureHashTextEncoder( Algorithm algorithm,
                                  int maxLength ) {
        CheckArg.isNotNull(algorithm, "algorithm");
        this.algorithm = algorithm;
        this.maxLength = maxLength < 1 ? Integer.MAX_VALUE : maxLength;
    }

    /**
     * Get the maximum length of the encoded string, or {@link Integer#MAX_VALUE} if there is no maximum.
     * 
     * @return the maximum encoded string length; always positive
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Return the secure hash algorithm used by this encoder.
     * 
     * @return the algorithm; never null
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextEncoder#encode(java.lang.String)
     */
    public String encode( String text ) {
        try {
            byte[] hash = SecureHash.getHash(algorithm, text.getBytes());
            String result = Base64.encodeBytes(hash);
            return result.length() < maxLength ? result : result.substring(0, maxLength);
        } catch (NoSuchAlgorithmException e) {
            return text;
        }
    }

}

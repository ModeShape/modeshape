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
    @Override
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

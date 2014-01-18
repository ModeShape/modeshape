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
package org.modeshape.jcr.value;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;

/**
 * The internal key used to identify a unique BINARY value.
 */
@Immutable
public class BinaryKey implements Serializable, Comparable<BinaryKey> {
    private static final long serialVersionUID = 1L;

    private static final SecureHash.Algorithm ALGORITHM = SecureHash.Algorithm.SHA_1;

    public static int maxHexadecimalLength() {
        return ALGORITHM.getHexadecimalStringLength();
    }

    public static int minHexadecimalLength() {
        return ALGORITHM.getHexadecimalStringLength();
    }

    /**
     * Determine if the supplied hexadecimal string is potentially a binary key by checking the format of the string.
     * 
     * @param hexadecimalStr the hexadecimal string; may be null
     * @return true if the supplied string is a properly formatted hexadecimal representation of a binary key, or false otherwise
     */
    public static boolean isProperlyFormattedKey( String hexadecimalStr ) {
        if (hexadecimalStr == null) return false;
        // Length is expected to be the same as the digest ...
        final int length = hexadecimalStr.length();
        if (length != ALGORITHM.getHexadecimalStringLength()) return false;
        // The characters all must be hexadecimal digits ...
        return StringUtil.isHexString(hexadecimalStr);
    }

    public static BinaryKey keyFor( byte[] content ) {
        try {
            byte[] hash = SecureHash.getHash(ALGORITHM, content);
            return new BinaryKey(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

    private final String key;

    public BinaryKey( String key ) {
        assert key != null;
        assert key.length() > 0;
        this.key = key;
    }

    public BinaryKey( byte[] hash ) {
        this(StringUtil.getHexString(hash));
    }

    /**
     * Get this binary key in the form of a byte array.
     * 
     * @return the bytes that make up this key; never null and always a copy to prevent modification
     */
    public byte[] toBytes() {
        return StringUtil.fromHexString(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof BinaryKey) {
            BinaryKey that = (BinaryKey)obj;
            return this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public int compareTo( BinaryKey other ) {
        if (other == this) return 0;
        return this.key.compareTo(other.key);
    }

    @Override
    public String toString() {
        return key;
    }
}

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

    public static BinaryKey keyFor( byte[] content ) {
        try {
            byte[] hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, content);
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

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
package org.modeshape.common.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.jcip.annotations.Immutable;

/**
 * A simple utility to generate various kinds of secure hashes.
 */
@Immutable
public class SecureHash {

    /**
     * Commonly-used hashing algorithms.
     */
    public enum Algorithm {
        MD2("MD2", "The MD2 message digest algorithm as defined in RFC 1319"),
        MD5("MD5", "The MD5 message digest algorithm as defined in RFC 1321"),
        SHA_1("SHA-1", "The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1"),
        SHA_256(
                "SHA-256",
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  SHA-256 is a 256-bit hash function intended to provide 128 bits of "
                + "security against collision attacks."),
        SHA_384(
                "SHA-384",
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  A 384-bit hash may be obtained by truncating the SHA-512 output."),
        SHA_512(
                "SHA-512",
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  SHA-512 is a 512-bit hash function intended to provide 256 bits of security.");
        private String name;
        private String description;

        private Algorithm( String name,
                           String description ) {
            this.name = name;
            this.description = description;
        }

        public String digestName() {
            return this.name;
        }

        public String description() {
            return this.description;
        }

        @Override
        public String toString() {
            return digestName();
        }
    }

    /**
     * Get the hash of the supplied content, using the supplied digest algorithm.
     * 
     * @param algorithm the hashing function algorithm that should be used
     * @param content the content to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     * @throws IllegalArgumentException if the algorithm is null
     */
    public static byte[] getHash( Algorithm algorithm,
                                  byte[] content ) throws NoSuchAlgorithmException {
        CheckArg.isNotNull(algorithm, "algorithm");
        return getHash(algorithm.digestName(), content);
    }

    /**
     * Get the hash of the supplied content, using the supplied digest algorithm.
     * 
     * @param algorithm the hashing function algorithm that should be used
     * @param file the file containing the content to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     * @throws IllegalArgumentException if the algorithm is null
     * @throws IOException if there is an error reading the file
     */
    public static byte[] getHash( Algorithm algorithm,
                                  File file ) throws NoSuchAlgorithmException, IOException {
        CheckArg.isNotNull(algorithm, "algorithm");
        return getHash(algorithm.digestName(), file);
    }

    /**
     * Get the hash of the supplied content, using the supplied digest algorithm.
     * 
     * @param algorithm the hashing function algorithm that should be used
     * @param stream the stream containing the content to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     * @throws IllegalArgumentException if the algorithm is null
     * @throws IOException if there is an error reading the stream
     */
    public static byte[] getHash( Algorithm algorithm,
                                  InputStream stream ) throws NoSuchAlgorithmException, IOException {
        CheckArg.isNotNull(algorithm, "algorithm");
        return getHash(algorithm.digestName(), stream);
    }

    /**
     * Get the hash of the supplied content, using the digest identified by the supplied name.
     * 
     * @param digestName the name of the hashing function (or {@link MessageDigest message digest}) that should be used
     * @param content the content to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     */
    public static byte[] getHash( String digestName,
                                  byte[] content ) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digestName);
        assert digest != null;
        return digest.digest(content);
    }

    /**
     * Get the hash of the supplied content, using the digest identified by the supplied name.
     * 
     * @param digestName the name of the hashing function (or {@link MessageDigest message digest}) that should be used
     * @param file the file whose content is to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     * @throws IOException if there is an error reading the file
     */
    public static byte[] getHash( String digestName,
                                  File file ) throws NoSuchAlgorithmException, IOException {
        CheckArg.isNotNull(file, "file");
        MessageDigest digest = MessageDigest.getInstance(digestName);
        assert digest != null;
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        boolean error = false;
        try {
            int bufSize = 1024;
            byte[] buffer = new byte[bufSize];
            int n = in.read(buffer, 0, bufSize);
            int count = 0;
            while (n != -1) {
                count += n;
                digest.update(buffer, 0, n);
                n = in.read(buffer, 0, bufSize);
            }
        } catch (IOException e) {
            error = true;
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        return digest.digest();
    }

    /**
     * Get the hash of the supplied content, using the digest identified by the supplied name. Note that this method never closes
     * the supplied stream.
     * 
     * @param digestName the name of the hashing function (or {@link MessageDigest message digest}) that should be used
     * @param stream the stream containing the content to be hashed; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException if the supplied algorithm could not be found
     * @throws IOException if there is an error reading the stream
     */
    public static byte[] getHash( String digestName,
                                  InputStream stream ) throws NoSuchAlgorithmException, IOException {
        CheckArg.isNotNull(stream, "stream");
        MessageDigest digest = MessageDigest.getInstance(digestName);
        assert digest != null;
        int bufSize = 1024;
        byte[] buffer = new byte[bufSize];
        int n = stream.read(buffer, 0, bufSize);
        int count = 0;
        while (n != -1) {
            count += n;
            digest.update(buffer, 0, n);
            n = stream.read(buffer, 0, bufSize);
        }
        return digest.digest();
    }

    /**
     * Create an InputStream instance that wraps another stream and that computes the secure hash (using the algorithm with the
     * supplied name) as the returned stream is used. This can be used to compute the hash of a stream while the stream is being
     * processed by another reader, and saves from having to process the same stream twice.
     * 
     * @param algorithm the hashing function algorithm that should be used
     * @param inputStream the stream containing the content that is to be hashed
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static HashingInputStream createHashingStream( Algorithm algorithm,
                                                          InputStream inputStream ) throws NoSuchAlgorithmException {
        return createHashingStream(algorithm.digestName(), inputStream);
    }

    /**
     * Create an InputStream instance that wraps another stream and that computes the secure hash (using the algorithm with the
     * supplied name) as the returned stream is used. This can be used to compute the hash of a stream while the stream is being
     * processed by another reader, and saves from having to process the same stream twice.
     * 
     * @param digestName the name of the hashing function (or {@link MessageDigest message digest}) that should be used
     * @param inputStream the stream containing the content that is to be hashed
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static HashingInputStream createHashingStream( String digestName,
                                                          InputStream inputStream ) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digestName);
        return new HashingInputStream(digest, inputStream);
    }

    /**
     * Get the string representation of the supplied binary hash.
     * 
     * @param hash the binary hash
     * @return the hex-encoded representation of the binary hash, or null if the hash is null
     */
    public static String asHexString( byte[] hash ) {
        return hash != null ? StringUtil.getHexString(hash) : null;
    }

    public static class HashingInputStream extends InputStream {
        private final MessageDigest digest;
        private final InputStream stream;
        private byte[] hash;

        protected HashingInputStream( MessageDigest digest,
                                      InputStream input ) {
            this.digest = digest;
            this.stream = input;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.InputStream#read()
         */
        @Override
        public int read() throws IOException {
            int result = stream.read();
            if (result != -1) {
                digest.update((byte)result);
            }
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.InputStream#read(byte[], int, int)
         */
        @Override
        public int read( byte[] b,
                         int off,
                         int len ) throws IOException {
            // Read from the stream ...
            int n = stream.read(b, off, len);
            if (n != -1) {
                digest.update(b, off, n);
            }
            return n;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.InputStream#read(byte[])
         */
        @Override
        public int read( byte[] b ) throws IOException {
            int n = stream.read(b);
            if (n != -1) {
                digest.update(b, 0, n);
            }
            return n;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.InputStream#close()
         */
        @Override
        public void close() throws IOException {
            stream.close();
            if (hash == null) hash = digest.digest();
        }

        /**
         * Get the hash of the content read by this stream. This method will return null if the stream has not yet been closed.
         * 
         * @return the hash of the contents as a byte array, or null if the stream has not yet been closed
         */
        public byte[] getHash() {
            return hash;
        }

        /**
         * Get the string representation of the binary hash of the content read by this stream. This method will return null if
         * the stream has not yet been closed.
         * 
         * @return the hex-encoded representation of the binary hash of the contents, or null if the stream has not yet been
         *         closed
         */
        public String getHashAsHexString() {
            return SecureHash.asHexString(hash);
        }
    }
}

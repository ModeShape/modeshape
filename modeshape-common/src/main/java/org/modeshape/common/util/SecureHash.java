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
package org.modeshape.common.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;

/**
 * A simple utility to generate various kinds of secure hashes.
 */
@Immutable
public class SecureHash {

    /**
     * Commonly-used hashing algorithms.
     */
    @Immutable
    public enum Algorithm {
        MD2("MD2", 128, "The MD2 message digest algorithm as defined in RFC 1319"),
        MD5("MD5", 128, "The MD5 message digest algorithm as defined in RFC 1321"),
        SHA_1("SHA-1", 160, "The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1"),
        SHA_256(
                "SHA-256",
                256,
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  SHA-256 is a 256-bit hash function intended to provide 128 bits of "
                + "security against collision attacks."),
        SHA_384(
                "SHA-384",
                384,
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  A 384-bit hash may be obtained by truncating the SHA-512 output."),
        SHA_512(
                "SHA-512",
                512,
                "New hash algorithms for which the draft Federal Information Processing Standard 180-2, "
                + "Secure Hash Standard (SHS) is now available.  SHA-512 is a 512-bit hash function intended to provide 256 bits of security.");
        private final String name;
        private final String description;
        private final int numberOfBits;
        private final int numberOfBytes;
        private final int numberOfHexChars;

        private Algorithm( String name,
                           int numberOfBits,
                           String description ) {
            assert numberOfBits % 8 == 0;
            this.name = name;
            this.description = description;
            this.numberOfBits = numberOfBits;
            this.numberOfBytes = this.numberOfBits / 8;
            this.numberOfHexChars = this.numberOfBits / 4;
        }

        public String digestName() {
            return this.name;
        }

        public String description() {
            return this.description;
        }

        /**
         * Get the length of the hexadecimal representation.
         * 
         * @return the number of hexadecimal characters
         */
        public int getHexadecimalStringLength() {
            return numberOfHexChars;
        }

        /**
         * Get the length of the hexadecimal representation.
         * 
         * @return the number of hexadecimal characters
         */
        public int getNumberOfBytes() {
            return numberOfBytes;
        }

        /**
         * Get the number of bits that make up a digest.
         * 
         * @return the number of bits
         */
        public int getNumberOfBits() {
            return numberOfBits;
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
            while (n != -1) {
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
        while (n != -1) {
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
     * Create an Reader instance that wraps another reader and that computes the secure hash (using the algorithm with the
     * supplied name) as the returned Reader is used. This can be used to compute the hash while the content is being processed,
     * and saves from having to process the same content twice.
     * 
     * @param algorithm the hashing function algorithm that should be used
     * @param reader the reader containing the content that is to be hashed
     * @param charset the character set used within the supplied reader; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static HashingReader createHashingReader( Algorithm algorithm,
                                                     Reader reader,
                                                     Charset charset ) throws NoSuchAlgorithmException {
        return createHashingReader(algorithm.digestName(), reader, charset);
    }

    /**
     * Create an Reader instance that wraps another reader and that computes the secure hash (using the algorithm with the
     * supplied name) as the returned Reader is used. This can be used to compute the hash while the content is being processed,
     * and saves from having to process the same content twice.
     * 
     * @param digestName the name of the hashing function (or {@link MessageDigest message digest}) that should be used
     * @param reader the reader containing the content that is to be hashed
     * @param charset the character set used within the supplied reader; may not be null
     * @return the hash of the contents as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static HashingReader createHashingReader( String digestName,
                                                     Reader reader,
                                                     Charset charset ) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(digestName);
        return new HashingReader(digest, reader, charset);
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

    /**
     * Computes the sha1 value for the given string.
     *
     * @param string a non-null string
     * @return the SHA1 value for the given string.
     */
    public static String sha1( String string ) {
        try {
            byte[] sha1 = SecureHash.getHash(SecureHash.Algorithm.SHA_1, string.getBytes());
            return SecureHash.asHexString(sha1);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
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

        @Override
        public int read() throws IOException {
            int result = stream.read();
            if (result != -1) {
                digest.update((byte)result);
            }
            return result;
        }

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

        @Override
        public int read( byte[] b ) throws IOException {
            int n = stream.read(b);
            if (n != -1) {
                digest.update(b, 0, n);
            }
            return n;
        }

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

    public static class HashingReader extends Reader {
        private final MessageDigest digest;
        private final Reader stream;
        private byte[] hash;
        private final CharsetEncoder encoder;

        protected HashingReader( MessageDigest digest,
                                 Reader input,
                                 Charset charset ) {
            this.digest = digest;
            this.stream = input;
            this.encoder = charset.newEncoder();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.Reader#read()
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
         * @see java.io.Reader#read(char[], int, int)
         */
        @Override
        public int read( char[] b,
                         int off,
                         int len ) throws IOException {
            // Read from the stream ...
            int n = stream.read(b, off, len);
            if (n != -1) {
                byte[] bytes = encoder.encode(CharBuffer.wrap(b)).array();
                digest.update(bytes, off, n);
            }
            return n;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.io.Reader#read(char[])
         */
        @Override
        public int read( char[] b ) throws IOException {
            int n = stream.read(b);
            if (n != -1) {
                byte[] bytes = encoder.encode(CharBuffer.wrap(b)).array();
                digest.update(bytes, 0, n);
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
         * Get the hash of the content read by this reader. This method will return null if the reader has not yet been closed.
         * 
         * @return the hash of the contents as a byte array, or null if the reader has not yet been closed
         */
        public byte[] getHash() {
            return hash;
        }

        /**
         * Get the string representation of the binary hash of the content read by this reader. This method will return null if
         * the reader has not yet been closed.
         * 
         * @return the hex-encoded representation of the binary hash of the contents, or null if the reader has not yet been
         *         closed
         */
        public String getHashAsHexString() {
            return SecureHash.asHexString(hash);
        }
    }
}

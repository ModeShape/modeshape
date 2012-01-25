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
package org.modeshape.jcr.value.binary;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.jcr.value.BinaryKey;

public class BinaryKeyTest {

    private static final String CONTENT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vel felis tellus, at pellentesque sem. Praesent semper quam odio, a volutpat diam. Pellentesque ornare aliquet pellentesque. Nunc elit purus, accumsan eget semper id, pulvinar at ipsum. Quisque sagittis nisi at dui imperdiet id rhoncus nunc dictum. Vivamus semper leo sit.";
    private static final byte[] CONTENT_SHA1_BYTES;
    private static final String CONTENT_SHA1;

    static {
        String sha1 = null;
        byte[] bytes = null;
        try {
            bytes = SecureHash.getHash(Algorithm.SHA_1, new ByteArrayInputStream(CONTENT.getBytes()));
            sha1 = SecureHash.asHexString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        CONTENT_SHA1_BYTES = bytes;
        CONTENT_SHA1 = sha1;
    }

    private BinaryKey key;

    @Before
    public void beforeEach() {
        key = new BinaryKey(CONTENT_SHA1);
    }

    @Test
    public void shouldExposeSha1AsString() {
        assertThat(key.toString(), is(CONTENT_SHA1));
    }

    @Test
    public void shouldExposeSha1AsBytes() {
        assertThat(key.toBytes(), is(CONTENT_SHA1_BYTES));
    }

    @Test
    public void shouldEqualAnotherBinaryKeyWithSameSha1() {
        BinaryKey key2 = new BinaryKey(CONTENT_SHA1);
        assertThat(key2.equals(key), is(true));
        assertThat(key2.compareTo(key), is(0));
    }
}

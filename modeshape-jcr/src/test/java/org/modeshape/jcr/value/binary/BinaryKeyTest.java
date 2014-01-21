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

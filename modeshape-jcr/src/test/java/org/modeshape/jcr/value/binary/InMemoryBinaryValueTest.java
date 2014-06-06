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
import static org.mockito.Mockito.mock;
import static org.modeshape.jcr.value.basic.BinaryContains.hasContent;
import static org.modeshape.jcr.value.basic.BinaryContains.hasNoContent;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @author Randall Hauch
 */
public class InMemoryBinaryValueTest {

    private byte[] validByteArrayContent;
    private String validStringContent;
    private InMemoryBinaryValue binary;
    private BinaryStore store;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        validStringContent = "This is a valid string content";
        validByteArrayContent = this.validStringContent.getBytes("UTF-8");
        store = mock(BinaryStore.class);
        binary = new InMemoryBinaryValue(store, validByteArrayContent);
    }

    @Test
    public void shouldConstructFromByteArray() {
        binary = new InMemoryBinaryValue(store, validByteArrayContent);
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
        assertThat(binary, hasContent(validByteArrayContent));
    }

    @Test
    public void shouldConstructFromEmptyByteArray() {
        validByteArrayContent = new byte[0];
        binary = new InMemoryBinaryValue(store, validByteArrayContent);
        assertThat(binary.getSize(), is(0l));
        assertThat(binary, hasNoContent());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructFromNullByteArray() {
        new InMemoryBinaryValue(store, null);
    }

    @Test
    public void shouldHaveSizeThatMatchesContentLength() {
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
    }

    @Test
    public void shouldProvideInputStreamToContent() throws Exception {
        InputStream stream = binary.getStream();
        byte[] actual = IoUtil.readBytes(stream); // closes the stream
        assertThat(actual.length, is(validByteArrayContent.length));
        for (int i = 0, len = actual.length; i != len; ++i) {
            assertThat(actual[i], is(validByteArrayContent[i]));
        }
    }

    @Test
    public void shouldConsiderEquivalentThoseInstancesWithSameContent() {
        BinaryValue another = new InMemoryBinaryValue(store, validByteArrayContent);
        assertThat(binary.equals(another), is(true));
        assertThat(binary.compareTo(another), is(0));
        assertThat(binary, is(another));
        assertThat(binary, hasContent(validByteArrayContent));
        assertThat(another, hasContent(validByteArrayContent));
    }

    @Test
    public void shouldUseSizeWhenComparing() {
        byte[] shorterContent = new byte[validByteArrayContent.length - 2];
        for (int i = 0; i != shorterContent.length; ++i) {
            shorterContent[i] = validByteArrayContent[i];
        }
        BinaryValue another = new InMemoryBinaryValue(store, shorterContent);
        assertThat(binary.equals(another), is(false));
        assertThat(binary.compareTo(another) > 0, is(true));
        assertThat(another.compareTo(binary) < 0, is(true));
        assertThat(another, hasContent(shorterContent));
    }

    @Test
    public void shouldComputeSha1HashOfEmptyContent() throws Exception {
        validByteArrayContent = new byte[0];
        binary = new InMemoryBinaryValue(store, validByteArrayContent);
        assertThat(binary.getSize(), is(0l));
        assertThat(binary, hasNoContent());
        byte[] hash = binary.getHash();
        assertThat(hash.length, is(20));
        assertThat(StringUtil.getHexString(hash), is("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    }

    @Test
    public void shouldComputeSha1HashOfNonEmptyContent() throws Exception {
        binary = new InMemoryBinaryValue(store, validByteArrayContent);
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
        byte[] hash = binary.getHash();
        assertThat(hash.length, is(20));
        assertThat(StringUtil.getHexString(hash), is("14abe696257e85ba18b7c784d6c7855f46ce50ea"));
    }

}

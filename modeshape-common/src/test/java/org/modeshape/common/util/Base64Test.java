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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.xml.bind.DatatypeConverter;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * @author Randall Hauch
 */
public class Base64Test {

    // =========================================================================
    // H E L P E R M E T H O D S
    // =========================================================================

    // =========================================================================
    // T E S T C A S E S
    // =========================================================================

    @Test
    public void testBasicExamples() throws IOException {
        // Make up some source objects
        byte[] originalBytes = {(byte)-2, (byte)-1, (byte)0, (byte)1, (byte)2};

        // Display original array
        System.out.println("\n\nOriginal array: ");
        for (int i = 0; i < originalBytes.length; i++)
            System.out.print(originalBytes[i] + " ");
        System.out.println();

        // Encode serialized bytes
        String encBytes = Base64.encodeBytes(originalBytes);

        // Print encoded bytes
        System.out.println("Bytes, encoded ( " + encBytes.getBytes().length + " bytes):\n" + encBytes);

        // Decode bytes
        byte[] decBytes = Base64.decode(encBytes);

        // Display decoded bytes
        System.out.println("Encoded Bytes -> decoded: ");
        for (int i = 0; i < decBytes.length; i++)
            System.out.print(decBytes[i] + " ");
        System.out.println();
    }

    @Test
    public void shouldEncodeStringValue() throws UnsupportedEncodingException, IOException {
        String actualValue = "propertyValue";
        String encoded = Base64.encodeBytes(actualValue.getBytes("UTF-8"));
        byte[] decoded = Base64.decode(encoded);
        String decodedValue = new String(decoded, "UTF-8");
        assertThat(decodedValue, is(actualValue));
    }

    @Test
    public void shouldEncodeStreamableValue() throws IOException {
        String actualValue = "propertyValue";
        byte[] actualBytes = actualValue.getBytes();
        InputStream actualStream = new ByteArrayInputStream(actualBytes);
        String encoded = Base64.encode(actualStream);
        String encoded2 = Base64.encodeBytes(actualBytes);
        assertThat(encoded, is(encoded2));
        byte[] decoded = Base64.decode(encoded);
        String decodedValue = new String(decoded);
        assertThat(decodedValue, is(actualValue));
    }

    @Test( expected = NullPointerException.class )
    public void testEncodeNullByteArray() {
        Base64.encodeBytes(null);
    }

    @Test
    public void testEncodeEmptyByteArray() {
        String result = Base64.encodeBytes(new byte[] {});
        assertThat(result, is(notNullValue()));
        assertThat(result.length(), is(0));
    }
    
    @Test
    @FixFor("MODE-2413")
    public void shouldSupportReadingFromSelfClosingInputStream() throws Exception {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        URL resource = getClass().getResource("simple.json");
        try (Base64.InputStream is = new Base64.InputStream(new SelfClosingInputStream(resource.openStream()), Base64.ENCODE)) {
            int read;
            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, read);
            }                        
        }
        // until Java 8, use this....
        String expectedString = DatatypeConverter.printBase64Binary(IoUtil.readBytes(resource.openStream()));
        assertEquals("Incorrect Base64 encoding", expectedString, new String(bos.toByteArray()));
    }
}

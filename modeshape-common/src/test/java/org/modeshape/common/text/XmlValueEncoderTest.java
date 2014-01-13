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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class XmlValueEncoderTest {

    private XmlValueEncoder encoder = new XmlValueEncoder();

    @Before
    public void beforeEach() {
    }

    protected void checkEncoding( String input,
                                  String expected ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(expected, output);
        assertThat(output.length(), is(expected.length()));
        assertThat(output, is(expected));

        checkDecoding(output, input);
    }

    protected void checkForNoEncoding( String input ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(input, output);
        assertThat(output.length(), is(input.length()));
        assertThat(output, is(input));

        checkForNoDecoding(input);
    }

    protected void checkForNoDecoding( String input ) {
        String output = this.encoder.decode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(input, output);
        assertThat(output.length(), is(input.length()));
        assertThat(output, is(input));
    }

    protected void checkDecoding( String input,
                                  String output ) {
        String decoded = this.encoder.decode(input);
        assertEquals(output, decoded);
        assertThat(decoded.length(), is(output.length()));
        assertThat(decoded, is(output));
    }

    @Test 
    public void shouldEncodeStringWithNoSpecialChars() {
        checkForNoEncoding("The quick brown fox jumped over the lazy dog.?+=!@#$%^*()_+-[]{}|\\");
    }

    @Test
    public void shouldEncodeStringWithSpecialChars() {
        checkEncoding("<>&'\"", "&lt;&gt;&amp;&#039;&quot;");
    }

    @Test
    public void shouldHandleTrivialCase() {
        assertNull(encoder.encode(null));
        assertNull(encoder.decode(null));
        checkEncoding("", "");

    }

    @Test
    public void shouldDecodeStringWithInvalidMappings() {
        checkDecoding("&amp", "&amp");
        checkDecoding("&quot", "&quot");
        checkDecoding("&gt", "&gt");
        checkDecoding("&lt", "&lt");
        checkDecoding("amp;", "amp;");
        checkDecoding("quot;", "quot;");
        checkDecoding("gt;", "gt;");
        checkDecoding("lt;", "lt;");
        checkDecoding("&;", "&;");
        checkDecoding("&amp;&", "&&");
    }
}

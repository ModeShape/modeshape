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

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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class UrlEncoderTest {

    private UrlEncoder encoder = new UrlEncoder();

    @Before
    public void beforeEach() {
    }

    protected void checkEncoding( String input, String expected ) {
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

        checkDecoding(output, input);
    }

    protected void checkDecoding( String input, String output ) {
        String decoded = this.encoder.decode(input);
        assertEquals(output, decoded);
        assertThat(decoded.length(), is(output.length()));
        assertThat(decoded, is(output));
    }

    @Test
    public void shouldNotEncodeForwardSlashByDefault() {
        checkEncoding("/", "%2f");
        this.encoder.setSlashEncoded(false);
        checkForNoEncoding("/");
    }

    @Test
    public void shouldEncodePercent() {
        checkEncoding("%", "%25");
        this.encoder.setSlashEncoded(false);
        checkEncoding("%", "%25");
    }

    @Test
    public void shouldNotEncodeAlphabeticCharacters() {
        checkForNoEncoding("abcdefghijklmnopqrstuvwxyz");
        checkForNoEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        this.encoder.setSlashEncoded(false);
        checkForNoEncoding("abcdefghijklmnopqrstuvwxyz");
        checkForNoEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldNotEncodeNumericCharacters() {
        checkForNoEncoding("0123456789");
        this.encoder.setSlashEncoded(false);
        checkForNoEncoding("0123456789");
    }

    @Test
    public void shouldNotEncodeReservedPunctuationCharacters() {
        checkForNoEncoding("-_.!~*\'()");
        this.encoder.setSlashEncoded(false);
        checkForNoEncoding("-_.!~*\'()");
    }

    @Test
    public void shouldNotDecodePercentIfNotFollowedByValidHexNumber() {
        checkDecoding("%", "%");
        checkDecoding("%2", "%2");
        checkDecoding("%2G", "%2G");
        checkDecoding("%2f", "/");
        checkDecoding("%25", "%");
    }

    @Test
    public void shouldEncodeSpaceUsingHexFormat() {
        checkEncoding(" ", "%20");
    }

    @Test
    public void shouldEncodePunctuationUsingHexFormat() {
        checkEncoding("`", "%60");
        checkEncoding("@", "%40");
        checkEncoding("#", "%23");
        checkEncoding("$", "%24");
        checkEncoding("^", "%5e");
        checkEncoding("&", "%26");
        checkEncoding("{", "%7b");
        checkEncoding("[", "%5b");
        checkEncoding("}", "%7d");
        checkEncoding("]", "%5d");
        checkEncoding("|", "%7c");
        checkEncoding(":", "%3a");
        checkEncoding(";", "%3b");
        checkEncoding("\"", "%22");
        checkEncoding("<", "%3c");
        checkEncoding(",", "%2c");
        checkEncoding(">", "%3e");
        checkEncoding("?", "%3f");
    }

    @Test
    public void shouldEncodeAndDecodeUrlsCorrectly() {
        this.encoder.setSlashEncoded(false);
        checkEncoding("http://acme.com/this is %something?get=true;something=false", "http%3a//acme.com/this%20is%20%25something%3fget%3dtrue%3bsomething%3dfalse");
    }
}

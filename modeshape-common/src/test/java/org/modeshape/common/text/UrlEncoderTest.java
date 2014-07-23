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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

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

    @Test
    @FixFor( "MODE-2258" )
    public void shouldEncodeNonReservedNonAsciiCharacter() {
        this.encoder.setSlashEncoded(false);
        checkEncoding(":Тест:的", "%3a%d0%a2%d0%b5%d1%81%d1%82%3a%e7%9a%84");
    }
}

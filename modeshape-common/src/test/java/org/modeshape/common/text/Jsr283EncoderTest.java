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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class Jsr283EncoderTest {

    private Jsr283Encoder encoder = new Jsr283Encoder();

    @Before
    public void beforeEach() {

    }

    protected void checkSingleCharacterEncoding( char input, char expected ) {
        String inputString = new String(new char[] {input});
        String output = this.encoder.encode(inputString);
        assertThat(output, is(notNullValue()));
        assertThat(output.length(), is(1));
        assertThat(output.charAt(0), is(expected));

        String decoded = this.encoder.decode(output);
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded.length(), is(1));
        assertThat(decoded.charAt(0), is(input));
    }

    protected void checkForNoEncoding( String input ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertThat(output.length(), is(input.length()));
        assertThat(output, is(input));

        String decoded = this.encoder.decode(output);
        assertThat(decoded.length(), is(input.length()));
        assertThat(decoded, is(input));
    }

    @Test
    public void shouldEncodeAsterisk() {
        checkSingleCharacterEncoding('*', '\uF02A');
    }

    @Test
    public void shouldEncodeForwardSlash() {
        checkSingleCharacterEncoding('/', '\uF02F');
    }

    @Test
    public void shouldEncodeColon() {
        checkSingleCharacterEncoding(':', '\uF03A');
    }

    @Test
    public void shouldEncodeOpenBracket() {
        checkSingleCharacterEncoding('[', '\uF05B');
    }

    @Test
    public void shouldEncodeCloseBracket() {
        checkSingleCharacterEncoding(']', '\uF05D');
    }

    @Test
    public void shouldEncodePipe() {
        checkSingleCharacterEncoding('|', '\uF07C');
    }

    @Test
    public void shouldNotEncodeAlphabeticCharacters() {
        checkForNoEncoding("abcdefghijklmnopqrstuvwxyz");
        checkForNoEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldNotEncodeNumericCharacters() {
        checkForNoEncoding("0123456789");
    }

    @Test
    public void shouldNotEncodePunctuationCharacters() {
        checkForNoEncoding("~`!@#$%^&()-_+={}\\;\"'<,>.?");
    }
}

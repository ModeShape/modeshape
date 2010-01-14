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

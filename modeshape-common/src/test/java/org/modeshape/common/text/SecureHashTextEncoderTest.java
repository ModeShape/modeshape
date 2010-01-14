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
import java.util.HashSet;
import java.util.Set;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SecureHashTextEncoderTest {

    private SecureHashTextEncoder encoder = new SecureHashTextEncoder(Algorithm.SHA_1);
    private SecureHashTextEncoder shortEncoder = new SecureHashTextEncoder(Algorithm.SHA_1, 4);
    private SecureHashTextEncoder md5Encoder = new SecureHashTextEncoder(Algorithm.MD5);
    private Set<String> alreadyEncoded = new HashSet<String>();
    private Set<String> alreadyEncodedShort = new HashSet<String>();
    private Set<String> alreadyEncodedMd5 = new HashSet<String>();

    @Before
    public void beforeEach() {

    }

    protected void checkEncoding( String input ) {
        assertThat(alreadyEncoded.add(checkEncoding(encoder, input)), is(true));
        assertThat(alreadyEncodedShort.add(checkEncoding(shortEncoder, input)), is(true));
        assertThat(alreadyEncodedMd5.add(checkEncoding(md5Encoder, input)), is(true));
    }

    protected String checkEncoding( SecureHashTextEncoder encoder,
                                    String input ) {
        String output = encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertThat(output.length() <= encoder.getMaxLength(), is(true));
        return output;
    }

    @Test
    public void shouldEncodeAlphabeticCharacters() {
        checkEncoding("abcdefghijklmnopqrstuvwxyz");
        checkEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldEncodeNumericCharacters() {
        checkEncoding("0123456789");
    }

    @Test
    public void shouldEncodePunctuationCharacters() {
        checkEncoding("~`!@#$%^&()-_+={}\\;\"'<,>.?");
    }

    @Test
    public void shouldEncodeUrlsAndHaveNoDuplicates() {
        checkEncoding("http://www.jboss.org");
        checkEncoding("http://www.jboss.org/");
        checkEncoding("http://www.modeshape.org");
        checkEncoding("http://www.modeshape.org/1.0");
        checkEncoding("http://www.modeshape.org/internal/1.0");
        checkEncoding("http://www.jcp.org/jcr/1.0");
        checkEncoding("http://www.jcp.org/jcr/nt/1.0");
        checkEncoding("http://www.jcp.org/jcr/mix/1.0");
        checkEncoding("http://www.jcp.org/jcr/sv/1.0");
        checkEncoding("http://www.acme.com/this/is/a/really/long/url/this/is/a/really/long/url/this/is/a/really/long/url/this/is/a/really/long/url/21?x=1&z=2");
        checkEncoding("http://www.acme.com/this/is/a/really/long/url/this/is/a/really/long/url/this/is/a/really/long/url/this/is/a/really/long/url/21?x=1&z=3");
        // System.out.println(alreadyEncoded);
        // System.out.println(alreadyEncodedShort);
        // System.out.println(alreadyEncodedMd5);
    }
}

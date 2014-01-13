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

/*
 *
 */
package org.jboss.dna.common.text;

import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class NoOpEncoderTest {

    private NoOpEncoder encoder = new NoOpEncoder();

    @Before
    public void beforeEach() {
    }

    protected void checkForNoEncoding( String input ) {
        String output = this.encoder.encode(input);
        assertThat(output, is(notNullValue()));
        assertEquals(input, output);

        String decoded = this.encoder.decode(output);
        assertEquals(output, decoded);
        assertEquals(input, decoded);
    }

    @Test
    public void shouldReturnNullIfPassedNull() {
        assertThat(this.encoder.encode(null), is(nullValue()));
        assertThat(this.encoder.decode(null), is(nullValue()));
    }

    @Test
    public void shouldNeverEncodeAnyString() {
        checkForNoEncoding("%");
        checkForNoEncoding("abcdefghijklmnopqrstuvwxyz");
        checkForNoEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        checkForNoEncoding("0123456789");
        checkForNoEncoding("-_.!~*\'()");
        checkForNoEncoding("http://acme.com/this is %something?get=true;something=false");
    }
}

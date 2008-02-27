/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

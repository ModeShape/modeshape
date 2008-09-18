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
package org.jboss.dna.graph.properties.basic;

import static org.jboss.dna.graph.properties.basic.BinaryContains.hasContent;
import static org.jboss.dna.graph.properties.basic.BinaryContains.hasNoContent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.basic.InMemoryBinary;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class InMemoryBinaryTest {

    private byte[] validByteArrayContent;
    private String validStringContent;
    private InMemoryBinary binary;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        validStringContent = "This is a valid string content";
        validByteArrayContent = this.validStringContent.getBytes("UTF-8");
        binary = new InMemoryBinary(validByteArrayContent);
    }

    @Test
    public void shouldConstructFromByteArray() {
        binary = new InMemoryBinary(validByteArrayContent);
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
        assertThat(binary, hasContent(validByteArrayContent));
    }

    @Test
    public void shouldConstructFromEmptyByteArray() {
        validByteArrayContent = new byte[0];
        binary = new InMemoryBinary(validByteArrayContent);
        assertThat(binary.getSize(), is(0l));
        assertThat(binary, hasNoContent());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructFromNullByteArray() {
        new InMemoryBinary(null);
    }

    @Test
    public void shouldHaveSizeThatMatchesContentLength() {
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
    }

    @Test
    public void shouldProvideInputStreamToContent() throws IOException {
        InputStream stream = binary.getStream();
        byte[] actual = IoUtil.readBytes(stream); // closes the stream
        assertThat(actual.length, is(validByteArrayContent.length));
        for (int i = 0, len = actual.length; i != len; ++i) {
            assertThat(actual[i], is(validByteArrayContent[i]));
        }
    }

    @Test
    public void shouldConsiderEquivalentThoseInstancesWithSameContent() {
        Binary another = new InMemoryBinary(validByteArrayContent);
        assertThat(binary.equals(another), is(true));
        assertThat(binary.compareTo(another), is(0));
        assertThat(binary, is(another));
        assertThat(binary, hasContent(validByteArrayContent));
        assertThat(another, hasContent(validByteArrayContent));
    }

    @Test
    public void shouldUseSizeWhenComparing() {
        byte[] shorterContent = new byte[validByteArrayContent.length - 2];
        for (int i = 0; i != shorterContent.length; ++i) {
            shorterContent[i] = validByteArrayContent[i];
        }
        Binary another = new InMemoryBinary(shorterContent);
        assertThat(binary.equals(another), is(false));
        assertThat(binary.compareTo(another), is(1));
        assertThat(another.compareTo(binary), is(-1));
        assertThat(another, hasContent(shorterContent));
    }

    @Test
    public void shouldComputeSha1HashOfEmptyContent() throws Exception {
        validByteArrayContent = new byte[0];
        binary = new InMemoryBinary(validByteArrayContent);
        assertThat(binary.getSize(), is(0l));
        assertThat(binary, hasNoContent());
        byte[] hash = binary.getHash();
        assertThat(hash.length, is(20));
        assertThat(StringUtil.getHexString(hash), is("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    }

    @Test
    public void shouldComputeSha1HashOfNonEmptyContent() throws Exception {
        binary = new InMemoryBinary(validByteArrayContent);
        assertThat(binary.getSize(), is((long)validByteArrayContent.length));
        byte[] hash = binary.getHash();
        assertThat(hash.length, is(20));
        assertThat(StringUtil.getHexString(hash), is("14abe696257e85ba18b7c784d6c7855f46ce50ea"));
    }

}

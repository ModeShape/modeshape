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
package org.jboss.dna.spi.graph.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class DecimalValueFactoryTest {

    private DecimalValueFactory factory;
    private StringValueFactory stringFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(Path.URL_DECODER, Path.DEFAULT_ENCODER);
        factory = new DecimalValueFactory(Path.URL_DECODER, stringFactory);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDecimalFromBooleanValue() {
        factory.create(true);
    }

    @Test
    public void shouldCreateDecimalFromString() {
        assertThat(factory.create("1"), is(BigDecimal.valueOf(1)));
        assertThat(factory.create("-1.0"), is(BigDecimal.valueOf(-1.0d)));
        assertThat(factory.create("100.000101"), is(BigDecimal.valueOf(100.000101d)));
    }

    @Test
    public void shouldCreateDecimalFromStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  1  "), is(BigDecimal.valueOf(1)));
        assertThat(factory.create("  -1.0  "), is(BigDecimal.valueOf(-1.0d)));
        assertThat(factory.create("  100.000101  "), is(BigDecimal.valueOf(100.000101d)));
    }

    @Test
    public void shouldCreateDecimalFromIntegerValue() {
        assertThat(factory.create(1), is(BigDecimal.valueOf(1)));
    }

    @Test
    public void shouldCreateDecimalFromLongValue() {
        assertThat(factory.create(1l), is(BigDecimal.valueOf(1l)));
    }

    @Test
    public void shouldCreateDecimalFromFloatValue() {
        assertThat(factory.create(1.0f), is(BigDecimal.valueOf(1.0f)));
    }

    @Test
    public void shouldCreateDecimalFromDoubleValue() {
        assertThat(factory.create(1.0d), is(BigDecimal.valueOf(1.0d)));
    }

    @Test
    public void shouldCreateDecimalFromBigDecimal() {
        BigDecimal value = new BigDecimal(100);
        assertThat(factory.create(value), is(value));
    }

    @Test
    public void shouldCreateDecimalFromDate() {
        Date value = new Date();
        assertThat(factory.create(value), is(BigDecimal.valueOf(value.getTime())));
    }

    @Test
    public void shouldCreateDecimalFromCalendar() {
        Calendar value = Calendar.getInstance();
        assertThat(factory.create(value), is(BigDecimal.valueOf(value.getTimeInMillis())));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDecimalFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDecimalFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDecimalFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDecimalFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateDecimalFromByteArrayContainingUtf8EncodingOfStringWithDecimal() throws Exception {
        assertThat(factory.create("1".getBytes("UTF-8")), is(BigDecimal.valueOf(1l)));
        assertThat(factory.create("-1.0".getBytes("UTF-8")), is(BigDecimal.valueOf(-1.d)));
        assertThat(factory.create("100.000101".getBytes("UTF-8")), is(BigDecimal.valueOf(100.000101d)));
    }

    @Test
    public void shouldCreateDecimalFromInputStreamContainingUtf8EncodingOfStringWithDecimal() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream("1".getBytes("UTF-8"))), is(BigDecimal.valueOf(1l)));
        assertThat(factory.create(new ByteArrayInputStream("-1.0".getBytes("UTF-8"))), is(BigDecimal.valueOf(-1.d)));
        assertThat(factory.create(new ByteArrayInputStream("100.000101".getBytes("UTF-8"))), is(BigDecimal.valueOf(100.000101d)));
    }

    @Test
    public void shouldCreateDecimalFromReaderContainingStringWithDecimal() throws Exception {
        assertThat(factory.create(new StringReader("1")), is(BigDecimal.valueOf(1l)));
        assertThat(factory.create(new StringReader("-1.0")), is(BigDecimal.valueOf(-1.d)));
        assertThat(factory.create(new StringReader("100.000101")), is(BigDecimal.valueOf(100.000101d)));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDecimalFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanDecimal() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDecimalFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanDecimal() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDecimalFromReaderContainingStringWithContentsOtherThanDecimal() throws Exception {
        factory.create(new StringReader("something"));
    }
}

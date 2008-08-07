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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class DoubleValueFactoryTest {

    private DoubleValueFactory factory;
    private StringValueFactory stringFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(Path.URL_DECODER, Path.URL_ENCODER);
        factory = new DoubleValueFactory(Path.URL_DECODER, stringFactory);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDoubleFromBooleanValue() {
        factory.create(true);
    }

    @Test
    public void shouldCreateDoubleFromString() {
        assertThat(factory.create("1"), is(Double.valueOf(1)));
        assertThat(factory.create("-1.0"), is(Double.valueOf(-1.0d)));
        assertThat(factory.create("100.000101"), is(Double.valueOf(100.000101d)));
    }

    @Test
    public void shouldCreateDoubleFromStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  1  "), is(Double.valueOf(1)));
        assertThat(factory.create("  -1.0  "), is(Double.valueOf(-1.0d)));
        assertThat(factory.create("  100.000101  "), is(Double.valueOf(100.000101d)));
    }

    @Test
    public void shouldNotCreateDoubleFromIntegerValue() {
        assertThat(factory.create(1), is(1.0d));
    }

    @Test
    public void shouldNotCreateDoubleFromLongValue() {
        assertThat(factory.create(1l), is(1.0d));
    }

    @Test
    public void shouldNotCreateDoubleFromFloatValue() {
        assertThat(factory.create(1.0f), is(1.0d));
    }

    @Test
    public void shouldNotCreateDoubleFromDoubleValue() {
        assertThat(factory.create(1.0d), is(1.0d));
    }

    @Test
    public void shouldCreateDoubleFromBigDecimal() {
        BigDecimal value = new BigDecimal(100);
        assertThat(factory.create(value), is(value.doubleValue()));
    }

    @Test
    public void shouldCreateDoubleFromDate() {
        Date value = new Date();
        assertThat(factory.create(value), is(Double.valueOf(value.getTime())));
    }

    @Test
    public void shouldCreateDoubleFromCalendar() {
        Calendar value = Calendar.getInstance();
        assertThat(factory.create(value), is(Double.valueOf(value.getTimeInMillis())));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDoubleFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDoubleFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDoubleFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateDoubleFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateDoubleFromByteArrayContainingUtf8EncodingOfStringWithDouble() throws Exception {
        assertThat(factory.create("0.1".getBytes("UTF-8")), is(0.1d));
        assertThat(factory.create("1".getBytes("UTF-8")), is(1.0d));
        assertThat(factory.create("-1.03".getBytes("UTF-8")), is(-1.03));
        assertThat(factory.create("1003044".getBytes("UTF-8")), is(1003044.d));
    }

    @Test
    public void shouldCreateDoubleFromInputStreamContainingUtf8EncodingOfStringWithDouble() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream("0.1".getBytes("UTF-8"))), is(0.1d));
        assertThat(factory.create(new ByteArrayInputStream("1".getBytes("UTF-8"))), is(1.0d));
        assertThat(factory.create(new ByteArrayInputStream("-1.03".getBytes("UTF-8"))), is(-1.03));
        assertThat(factory.create(new ByteArrayInputStream("1003044".getBytes("UTF-8"))), is(1003044.d));
    }

    @Test
    public void shouldCreateDoubleFromReaderContainingStringWithDouble() {
        assertThat(factory.create(new StringReader("0.1")), is(0.1d));
        assertThat(factory.create(new StringReader("1")), is(1.0d));
        assertThat(factory.create(new StringReader("-1.03")), is(-1.03));
        assertThat(factory.create(new StringReader("1003044")), is(1003044.d));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDoubleFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanDouble() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDoubleFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanDouble() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateDoubleFromReaderContainingStringWithContentsOtherThanDouble() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i)
            values.add("" + i);
        Iterator<Double> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

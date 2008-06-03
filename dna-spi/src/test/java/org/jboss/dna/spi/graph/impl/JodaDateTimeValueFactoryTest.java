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
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFormatException;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JodaDateTimeValueFactoryTest {

    public static final DateTime TODAY;
    public static final DateTime LAST_YEAR;

    static {
        org.joda.time.DateTime now = new org.joda.time.DateTime();
        TODAY = new JodaDateTime(now);
        LAST_YEAR = new JodaDateTime(now.minusYears(1));
    }

    private JodaDateTimeValueFactory factory;
    private StringValueFactory stringFactory;
    private Mockery context;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(Path.URL_DECODER, Path.URL_ENCODER);
        factory = new JodaDateTimeValueFactory(Path.URL_DECODER, stringFactory);
        context = new Mockery();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromBoolean() {
        factory.create(true);
    }

    @Test
    public void shouldCreateDateFromString() {
        assertThat(factory.create(TODAY.getString()), is(TODAY));
        assertThat(factory.create(LAST_YEAR.getString()), is(LAST_YEAR));
    }

    @Test
    public void shouldCreateDateFromStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  " + TODAY.getString() + "  "), is(TODAY));
        assertThat(factory.create("  " + LAST_YEAR.getString() + "  "), is(LAST_YEAR));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromStringThatIsNotInTheStandardFormat() {
        factory.create("something");
    }

    @Test
    public void shouldNotCreateDateFromIntegerValue() {
        assertThat(factory.create(10000), is((DateTime)new JodaDateTime(10000)));
    }

    @Test
    public void shouldNotCreateDateFromLongValue() {
        assertThat(factory.create(10000l), is((DateTime)new JodaDateTime(10000l)));
    }

    @Test
    public void shouldNotCreateDateFromFloatValue() {
        assertThat(factory.create(10000.12345f), is((DateTime)new JodaDateTime(10000)));
    }

    @Test
    public void shouldNotCreateDateFromDoubleValue() {
        assertThat(factory.create(10000.12345d), is((DateTime)new JodaDateTime(10000)));
    }

    @Test
    public void shouldCreateDateFromBigDecimal() {
        assertThat(factory.create(new BigDecimal(10000)), is((DateTime)new JodaDateTime(10000)));
    }

    @Test
    public void shouldCreateDateFromDate() {
        Calendar value = Calendar.getInstance();
        assertThat(factory.create(value.getTime()), is((DateTime)new JodaDateTime(value.getTime())));
    }

    @Test
    public void shouldCreateDateFromCalendar() {
        Calendar value = Calendar.getInstance();
        value.setTimeInMillis(10000);
        assertThat(factory.create(value), is((DateTime)new JodaDateTime(value)));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromName() {
        factory.create(context.mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromPath() {
        factory.create(context.mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromReference() {
        factory.create(context.mock(Reference.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateDateFromByteArrayContainingUtf8EncodingOfStringWithWellFormedDate() throws Exception {
        assertThat(factory.create(TODAY.getString().getBytes("UTF-8")), is(TODAY));
        assertThat(factory.create(LAST_YEAR.getString().getBytes("UTF-8")), is(LAST_YEAR));
    }

    @Test
    public void shouldCreateDateFromInputStreamContainingUtf8EncodingOfStringWithWellFormedDate() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream(TODAY.getString().getBytes("UTF-8"))), is(TODAY));
        assertThat(factory.create(new ByteArrayInputStream(LAST_YEAR.getString().getBytes("UTF-8"))), is(LAST_YEAR));
    }

    @Test
    public void shouldCreateDateFromReaderContainingStringWithWellFormedDate() throws Exception {
        assertThat(factory.create(new StringReader(TODAY.getString())), is(TODAY));
        assertThat(factory.create(new StringReader(LAST_YEAR.getString())), is(LAST_YEAR));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromByteArrayContainingUtf8EncodingOfStringWithContentThatIsNotWellFormedDate() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromInputStreamContainingUtf8EncodingOfStringWithContentThatIsNotWellFormedDate() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromReaderContainingStringWithContentThatIsNotWellFormedDate() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }
}

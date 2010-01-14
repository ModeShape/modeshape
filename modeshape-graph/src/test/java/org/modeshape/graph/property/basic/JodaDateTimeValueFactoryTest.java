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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFormatException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(new SimpleNamespaceRegistry(), Path.URL_DECODER, Path.URL_ENCODER);
        factory = new JodaDateTimeValueFactory(Path.URL_DECODER, stringFactory);
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
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromReference() {
        factory.create(mock(Reference.class));
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
    public void shouldCreateDateFromReaderContainingStringWithWellFormedDate() {
        assertThat(factory.create(new StringReader(TODAY.getString())), is(TODAY));
        assertThat(factory.create(new StringReader(LAST_YEAR.getString())), is(LAST_YEAR));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromByteArrayContainingUtf8EncodingOfStringWithContentThatIsNotWellFormedDate()
        throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromInputStreamContainingUtf8EncodingOfStringWithContentThatIsNotWellFormedDate()
        throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDateFromReaderContainingStringWithContentThatIsNotWellFormedDate() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            values.add(new JodaDateTime(10000 + i).toString());
        }
        Iterator<DateTime> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

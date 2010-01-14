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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
public class LongValueFactoryTest {

    private LongValueFactory factory;
    private StringValueFactory stringFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(new SimpleNamespaceRegistry(), Path.URL_DECODER, Path.URL_ENCODER);
        factory = new LongValueFactory(Path.URL_DECODER, stringFactory);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromBooleanValue() {
        factory.create(true);
    }

    @Test
    public void shouldCreateLongFromString() {
        assertThat(factory.create("1"), is(Long.valueOf(1)));
        assertThat(factory.create("-10"), is(Long.valueOf(-10)));
        assertThat(factory.create("100000101"), is(Long.valueOf(100000101)));
    }

    @Test
    public void shouldCreateLongFromStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  1  "), is(Long.valueOf(1)));
        assertThat(factory.create("  -10  "), is(Long.valueOf(-10)));
        assertThat(factory.create("  100000101  "), is(Long.valueOf(100000101)));
    }

    @Test
    public void shouldNotCreateLongFromIntegerValue() {
        assertThat(factory.create(1), is(1l));
    }

    @Test
    public void shouldNotCreateLongFromLongValue() {
        assertThat(factory.create(1l), is(1l));
    }

    @Test
    public void shouldNotCreateLongFromFloatValue() {
        assertThat(factory.create(1.0f), is(1l));
        assertThat(factory.create(1.023f), is(1l));
        assertThat(factory.create(1.923f), is(1l));
    }

    @Test
    public void shouldNotCreateLongFromDoubleValue() {
        assertThat(factory.create(1.0122d), is(1l));
    }

    @Test
    public void shouldCreateLongFromBigDecimal() {
        BigDecimal value = new BigDecimal(100);
        assertThat(factory.create(value), is(value.longValue()));
    }

    @Test
    public void shouldCreateLongFromDate() {
        Date value = new Date();
        assertThat(factory.create(value), is(Long.valueOf(value.getTime())));
    }

    @Test
    public void shouldCreateLongFromCalendar() {
        Calendar value = Calendar.getInstance();
        assertThat(factory.create(value), is(Long.valueOf(value.getTimeInMillis())));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateLongFromByteArrayContainingUtf8EncodingOfStringWithLong() throws Exception {
        assertThat(factory.create("0".getBytes("UTF-8")), is(0l));
        assertThat(factory.create("10".getBytes("UTF-8")), is(10l));
        assertThat(factory.create("-103".getBytes("UTF-8")), is(-103l));
        assertThat(factory.create("1003044".getBytes("UTF-8")), is(1003044l));
    }

    @Test
    public void shouldCreateLongFromInputStreamContainingUtf8EncodingOfStringWithLong() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream("0".getBytes("UTF-8"))), is(0l));
        assertThat(factory.create(new ByteArrayInputStream("10".getBytes("UTF-8"))), is(10l));
        assertThat(factory.create(new ByteArrayInputStream("-103".getBytes("UTF-8"))), is(-103l));
        assertThat(factory.create(new ByteArrayInputStream("1003044".getBytes("UTF-8"))), is(1003044l));
    }

    @Test
    public void shouldCreateLongFromReaderContainingStringWithLong() {
        assertThat(factory.create(new StringReader("0")), is(0l));
        assertThat(factory.create(new StringReader("10")), is(10l));
        assertThat(factory.create(new StringReader("-103")), is(-103l));
        assertThat(factory.create(new StringReader("1003044")), is(1003044l));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanLong() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanLong() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateLongFromReaderContainingStringWithContentsOtherThanLong() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i)
            values.add(" " + i);
        Iterator<Long> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

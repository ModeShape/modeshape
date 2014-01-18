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
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class JodaDateTimeValueFactoryTest extends BaseValueFactoryTest {

    public static final DateTime TODAY;
    public static final DateTime LAST_YEAR;

    static {
        org.joda.time.DateTime now = new org.joda.time.DateTime();
        TODAY = new JodaDateTime(now);
        LAST_YEAR = new JodaDateTime(now.minusYears(1));
    }

    private JodaDateTimeValueFactory factory;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        factory = new JodaDateTimeValueFactory(Path.URL_DECODER, valueFactories);
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

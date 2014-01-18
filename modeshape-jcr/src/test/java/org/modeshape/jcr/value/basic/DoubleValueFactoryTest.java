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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class DoubleValueFactoryTest extends BaseValueFactoryTest {

    private DoubleValueFactory factory;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        factory = new DoubleValueFactory(Path.URL_DECODER, valueFactories);
    }

    @Test( expected = ValueFormatException.class )
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

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = ValueFormatException.class )
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

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanDouble() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanDouble() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
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

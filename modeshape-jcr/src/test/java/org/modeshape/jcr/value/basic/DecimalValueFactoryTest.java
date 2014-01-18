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
import java.io.StringReader;
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
public class DecimalValueFactoryTest extends BaseValueFactoryTest {

    private DecimalValueFactory factory;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        factory = new DecimalValueFactory(Path.URL_DECODER, valueFactories);
    }

    @Test( expected = ValueFormatException.class )
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

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = ValueFormatException.class )
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

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanDecimal() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanDecimal()
        throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDecimalFromReaderContainingStringWithContentsOtherThanDecimal() {
        factory.create(new StringReader("something"));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i)
            values.add("" + i);
        Iterator<BigDecimal> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

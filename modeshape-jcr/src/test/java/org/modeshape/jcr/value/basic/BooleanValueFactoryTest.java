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
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
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
public class BooleanValueFactoryTest extends BaseValueFactoryTest {

    private BooleanValueFactory factory;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        factory = new BooleanValueFactory(Path.URL_DECODER, valueFactories);
    }

    @Test
    public void shouldCreateBooleanFromBoolean() {
        assertThat(factory.create(true), is(true));
        assertThat(factory.create(false), is(false));
    }

    @Test
    public void shouldCreateBooleanFromTrueAndFalseStringRegardlessOfCase() {
        assertThat(factory.create("true"), is(true));
        assertThat(factory.create("false"), is(false));
        assertThat(factory.create("TRUE"), is(true));
        assertThat(factory.create("FALSE"), is(false));
    }

    @Test
    public void shouldCreateBooleanFromTrueAndFalseStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  true  "), is(true));
        assertThat(factory.create("  false  "), is(false));
        assertThat(factory.create("  TRUE  "), is(true));
        assertThat(factory.create("  FALSE  "), is(false));
    }

    @Test
    public void shouldCreateFalseFromStringContainingOneOrZero() {
        assertThat(factory.create("1"), is(false));
        assertThat(factory.create("0"), is(false));
        assertThat(factory.create("  0  "), is(false));
        assertThat(factory.create("  1  "), is(false));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromIntegerValue() {
        factory.create(1);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromLongValue() {
        factory.create(1l);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromFloatValue() {
        factory.create(1.0f);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromDoubleValue() {
        factory.create(1.0d);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromBigDecimal() {
        factory.create(1.0d);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromDate() {
        factory.create(new Date());
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromCalendar() {
        factory.create(Calendar.getInstance());
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateBooleanFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateBooleanFromByteArrayContainingUtf8EncodingOfTrueOrFalseStringRegardlessOfCase() throws Exception {
        assertThat(factory.create("true".getBytes("UTF-8")), is(true));
        assertThat(factory.create("false".getBytes("UTF-8")), is(false));
        assertThat(factory.create("TRUE".getBytes("UTF-8")), is(true));
        assertThat(factory.create("FALSE".getBytes("UTF-8")), is(false));
        assertThat(factory.create("something else".getBytes("UTF-8")), is(false));
    }

    @Test
    public void shouldCreateBooleanFromInputStreamContainingUtf8EncodingOfTrueOrFalseStringRegardlessOfCase() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream("true".getBytes("UTF-8"))), is(true));
        assertThat(factory.create(new ByteArrayInputStream("false".getBytes("UTF-8"))), is(false));
        assertThat(factory.create(new ByteArrayInputStream("TRUE".getBytes("UTF-8"))), is(true));
        assertThat(factory.create(new ByteArrayInputStream("FALSE".getBytes("UTF-8"))), is(false));
        assertThat(factory.create(new ByteArrayInputStream("something else".getBytes("UTF-8"))), is(false));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i)
            values.add((i % 2 == 0 ? "true" : "false"));
        Iterator<Boolean> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

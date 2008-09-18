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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Reference;
import org.jboss.dna.graph.properties.ValueFormatException;
import org.jboss.dna.graph.properties.basic.BooleanValueFactory;
import org.jboss.dna.graph.properties.basic.StringValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class BooleanValueFactoryTest {

    private BooleanValueFactory factory;
    private StringValueFactory stringFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(Path.URL_DECODER, Path.DEFAULT_ENCODER);
        factory = new BooleanValueFactory(Path.URL_DECODER, stringFactory);
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
    public void shouldCreateBooleanFromReaderContainingTrueOrFalseStringRegardlessOfCase() {
        assertThat(factory.create(new StringReader("true")), is(true));
        assertThat(factory.create(new StringReader("false")), is(false));
        assertThat(factory.create(new StringReader("TRUE")), is(true));
        assertThat(factory.create(new StringReader("FALSE")), is(false));
        assertThat(factory.create(new StringReader("something else")), is(false));
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

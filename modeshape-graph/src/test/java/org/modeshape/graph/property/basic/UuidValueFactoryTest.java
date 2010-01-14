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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFormatException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class UuidValueFactoryTest {

    private UuidValueFactory factory;
    private StringValueFactory stringFactory;
    private UUID uuid;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(new SimpleNamespaceRegistry(), Path.URL_DECODER, Path.URL_ENCODER);
        factory = new UuidValueFactory(Path.URL_DECODER, stringFactory);
        uuid = UUID.randomUUID();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateDoubleFromBooleanValue() {
        factory.create(true);
    }

    @Test
    public void shouldReturnUuidWhenCreatingFromUuid() {
        assertThat(factory.create(uuid), is(sameInstance(uuid)));
    }

    @Test
    public void shouldCreateUuidWithNoArguments() {
        assertThat(factory.create(), is(instanceOf(UUID.class)));
    }

    @Test
    public void shouldCreateUuidFromString() {
        assertThat(factory.create(uuid.toString()), is(uuid));
    }

    @Test
    public void shouldCreateUuidFromStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  " + uuid.toString() + "  "), is(uuid));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromIntegerValue() {
        factory.create(1);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromLongValue() {
        factory.create(1L);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromDoubleValue() {
        factory.create(1.0d);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromFloatValue() {
        factory.create(1.0f);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromBooleanValue() {
        factory.create(true);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromCalendarValue() {
        factory.create(Calendar.getInstance());
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromPath() {
        factory.create(mock(Path.class));
    }

    @Test
    public void shouldCreateUuidFromReference() {
        UuidReference ref = new UuidReference(uuid);
        assertThat(factory.create(ref), is(uuid));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateUuidFromByteArrayContainingUtf8EncodingOfStringWithUuid() throws Exception {
        assertThat(factory.create(uuid.toString().getBytes("UTF-8")), is(uuid));
    }

    @Test
    public void shouldCreateUuidFromInputStreamContainingUtf8EncodingOfStringWithUuid() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream(uuid.toString().getBytes("UTF-8"))), is(uuid));
    }

    @Test
    public void shouldCreateUuidFromReaderContainingStringWithUuid() {
        assertThat(factory.create(new StringReader(uuid.toString())), is(uuid));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuidFromByteArrayContainingUtf8EncodingOfStringWithContentsOtherThanUuid() throws Exception {
        factory.create("something".getBytes("UTF-8"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuuidFromInputStreamContainingUtf8EncodingOfStringWithContentsOtherThanUuuid() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateUuuidFromReaderContainingStringWithContentsOtherThanUuuid() throws Exception {
        factory.create(new ByteArrayInputStream("something".getBytes("UTF-8")));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            values.add(" " + UUID.randomUUID());
        }
        Iterator<UUID> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

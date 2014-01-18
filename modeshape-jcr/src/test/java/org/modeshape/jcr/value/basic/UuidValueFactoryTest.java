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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class UuidValueFactoryTest extends BaseValueFactoryTest {

    private UuidValueFactory factory;
    private UUID uuid;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        factory = new UuidValueFactory(Path.URL_DECODER, valueFactories);
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

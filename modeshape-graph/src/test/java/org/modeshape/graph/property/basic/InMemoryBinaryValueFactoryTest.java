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
import static org.modeshape.graph.property.basic.BinaryContains.hasContent;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class InMemoryBinaryValueFactoryTest {

    private InMemoryBinaryValueFactory factory;
    private StringValueFactory stringFactory;
    private NameValueFactory nameFactory;
    private NamespaceRegistry namespaceRegistry;
    private PathValueFactory pathFactory;
    private TextEncoder encoder;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        encoder = Path.URL_ENCODER;
        namespaceRegistry = new SimpleNamespaceRegistry();
        namespaceRegistry.register("jboss", "http://www.jboss.org");
        namespaceRegistry.register("dna", "http://www.modeshape.org");
        stringFactory = new StringValueFactory(namespaceRegistry, Path.URL_DECODER, encoder);
        nameFactory = new NameValueFactory(namespaceRegistry, Path.URL_DECODER, stringFactory);
        pathFactory = new PathValueFactory(Path.URL_DECODER, stringFactory, nameFactory);

        // The binary factory should convert between names and paths without using prefixes ...
        StringValueFactory noNamespaceStringFactory = new StringValueFactory(Path.URL_DECODER, encoder);
        factory = new InMemoryBinaryValueFactory(Path.URL_DECODER, noNamespaceStringFactory);
    }

    @Test
    public void shouldCreateBinaryFromBooleanValue() {
        factory.create(true);
    }

    @Test
    public void shouldCreateDecimalFromString() throws UnsupportedEncodingException {
        assertThat(factory.create("1"), hasContent("1"));
        assertThat(factory.create("-1.0"), hasContent("-1.0"));
        assertThat(factory.create("100.000101"), hasContent("100.000101"));
        assertThat(factory.create("quick brown fox."), hasContent("quick brown fox."));
    }

    @Test
    public void shouldCreateBinaryFromStringIncludingfLeadingAndTrailingWhitespace() throws UnsupportedEncodingException {
        assertThat(factory.create("   1   "), hasContent("   1   "));
        assertThat(factory.create("   -1.0   "), hasContent("   -1.0   "));
        assertThat(factory.create("   100.000101   "), hasContent("   100.000101   "));
        assertThat(factory.create("   quick brown fox.   "), hasContent("   quick brown fox.   "));
    }

    @Test
    public void shouldCreateBinaryFromIntegerValue() throws UnsupportedEncodingException {
        assertThat(factory.create(1), hasContent(Integer.toString(1)));
        assertThat(factory.create(-1), hasContent(Integer.toString(-1)));
        assertThat(factory.create(123456), hasContent(Integer.toString(123456)));
    }

    @Test
    public void shouldCreateBinaryFromLongValue() throws UnsupportedEncodingException {
        assertThat(factory.create(1l), hasContent(Long.toString(1)));
        assertThat(factory.create(-1l), hasContent(Long.toString(-1)));
        assertThat(factory.create(123456l), hasContent(Long.toString(123456)));
    }

    @Test
    public void shouldCreateBinaryFromFloatValue() throws UnsupportedEncodingException {
        assertThat(factory.create(1.0f), hasContent(Float.toString(1.0f)));
        assertThat(factory.create(-1.0f), hasContent(Float.toString(-1.0f)));
        assertThat(factory.create(123456.23f), hasContent(Float.toString(123456.23f)));
    }

    @Test
    public void shouldCreateBinaryFromDoubleValue() throws UnsupportedEncodingException {
        assertThat(factory.create(1.0d), hasContent(Double.toString(1.0d)));
        assertThat(factory.create(-1.0d), hasContent(Double.toString(-1.0)));
        assertThat(factory.create(123456.23d), hasContent(Double.toString(123456.23)));
    }

    @Test
    public void shouldCreateBinaryFromBigDecimal() throws UnsupportedEncodingException {
        BigDecimal value = new BigDecimal(100);
        assertThat(factory.create(value), hasContent(value.toString()));
    }

    @Test
    public void shouldCreateBinaryFromDate() throws UnsupportedEncodingException {
        Date value = new Date();
        assertThat(factory.create(value), hasContent(new JodaDateTime(value).toString()));
    }

    @Test
    public void shouldCreateBinaryFromCalendar() throws UnsupportedEncodingException {
        Calendar value = Calendar.getInstance();
        assertThat(factory.create(value), hasContent(new JodaDateTime(value).toString()));
    }

    @Test
    public void shouldCreateBinaryFromName() throws UnsupportedEncodingException {
        Name value = nameFactory.create("jboss:localName");
        assertThat(factory.create(value), hasContent("{" + encoder.encode("http://www.jboss.org") + "}"
                                                     + encoder.encode("localName")));
    }

    @Test
    public void shouldCreateBinaryFromPath() throws UnsupportedEncodingException {
        Path value = pathFactory.create("/a/b/c/jboss:localName");
        assertThat(factory.create(value), hasContent("/{}a/{}b/{}c/{" + encoder.encode("http://www.jboss.org") + "}"
                                                     + encoder.encode("localName")));
    }

    @Test
    public void shouldCreateBinaryFromReference() throws UnsupportedEncodingException {
        UUID uuid = UUID.randomUUID();
        Reference value = new UuidReference(uuid);
        assertThat(factory.create(value), hasContent(uuid.toString()));
    }

    @Test
    public void shouldCreateBinaryFromUri() throws Exception {
        URI value = new URI("http://www.jboss.org");
        assertThat(factory.create(value), hasContent("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateBinaryFromByteArray() throws Exception {
        byte[] value = "Some byte string".getBytes("UTF-8");
        assertThat(factory.create(value), hasContent(value));
    }

    @Test
    public void shouldCreateBinaryFromInputStream() throws Exception {
        String value = "Some test string";
        assertThat(factory.create(new ByteArrayInputStream(value.getBytes("UTF-8"))), hasContent(value));
    }

    @Test
    public void shouldCreateBinaryFromReader() throws Exception {
        String value = "Some test string";
        assertThat(factory.create(new StringReader(value)), hasContent(value));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i)
            values.add("some string" + i);
        Iterator<Binary> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

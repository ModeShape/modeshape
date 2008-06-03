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
package org.jboss.dna.spi.graph.impl;

import static org.jboss.dna.spi.graph.impl.BinaryContains.hasContent;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class InMemoryBinaryValueFactoryTest {

    private InMemoryBinaryValueFactory factory;
    private StringValueFactory stringFactory;
    private NameValueFactory nameFactory;
    private BasicNamespaceRegistry namespaceRegistry;
    private PathValueFactory pathFactory;
    private TextEncoder encoder;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        encoder = Path.URL_ENCODER;
        stringFactory = new StringValueFactory(Path.URL_DECODER, encoder);
        factory = new InMemoryBinaryValueFactory(Path.URL_DECODER, stringFactory);
        namespaceRegistry = new BasicNamespaceRegistry();
        namespaceRegistry.register("jboss", "http://www.jboss.org");
        namespaceRegistry.register("dna", "http://www.jboss.org/dna");
        nameFactory = new NameValueFactory(namespaceRegistry, Path.URL_DECODER, stringFactory);
        pathFactory = new PathValueFactory(Path.URL_DECODER, stringFactory, nameFactory);
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
        assertThat(factory.create(value), hasContent("{" + encoder.encode("http://www.jboss.org") + "}" + encoder.encode("localName")));
    }

    @Test
    public void shouldCreateBinaryFromPath() throws UnsupportedEncodingException {
        Path value = pathFactory.create("/a/b/c/jboss:localName");
        assertThat(factory.create(value), hasContent("/{}a/{}b/{}c/{" + encoder.encode("http://www.jboss.org") + "}" + encoder.encode("localName")));
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
}

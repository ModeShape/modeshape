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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class NameValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private NamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory factory;
    private TextEncoder encoder;
    private TextDecoder decoder;
    private Name name;

    @Before
    public void beforeEach() {
        this.registry = new SimpleNamespaceRegistry();
        this.registry.register("dna", "http://www.modeshape.org/namespace");
        this.encoder = Path.DEFAULT_ENCODER;
        this.decoder = Path.DEFAULT_DECODER;
        this.stringValueFactory = new StringValueFactory(registry, decoder, encoder);
        this.factory = new NameValueFactory(registry, decoder, stringValueFactory);
    }

    @Test
    public void shouldCreateNameFromSingleStringInPrefixedNamespaceFormatWithoutPrefix() {
        name = factory.create("a");
        assertThat(name.getLocalName(), is("a"));
        assertThat(name.getNamespaceUri(), is(this.registry.getNamespaceForPrefix("")));
    }

    @Test
    public void shouldCreateNameFromSingleStringInPrefixedNamespaceFormat() {
        name = factory.create("dna:something");
        assertThat(name.getLocalName(), is("something"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}something"));
    }

    @Test
    public void shouldCreateNameFromSingleEncodedStringInPrefixedNamespaceFormat() {
        name = factory.create(encoder.encode("dna") + ":" + encoder.encode("some/thing"));
        assertThat(name.getLocalName(), is("some/thing"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}some/thing"));
    }

    @Test
    public void shouldCreateNameFromSingleStringInStandardFullNamespaceFormat() {
        name = factory.create("{http://www.modeshape.org/namespace}something");
        assertThat(name.getLocalName(), is("something"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}something"));
    }

    @Test
    public void shouldCreateNameFromSingleEncodedStringInStandardFullNamespaceFormat() {
        name = factory.create("{" + encoder.encode("http://www.modeshape.org/namespace") + "}" + encoder.encode("some/thing"));
        assertThat(name.getLocalName(), is("some/thing"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}some/thing"));
    }

    @Test
    public void shouldProvideAccessToNamespaceRegistryPassedInConstructor() {
        assertThat(factory.getNamespaceRegistry(), is(registry));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullLocalName() {
        factory.create("a", (String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullLocalNameWithEncoder() {
        factory.create("a", null, decoder);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyLocalName() {
        factory.create("a", "");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyLocalNameWithEncoder() {
        factory.create("a", "", decoder);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateNameFromStringWithMultipleNonEscapedColons() {
        // This is a requirement of JCR, per the JCR TCK
        factory.create("a:b:c");
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            values.add("dna:something" + i);
        }
        Iterator<Name> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }
}

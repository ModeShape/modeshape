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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class NameValueFactoryTest extends BaseValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private Name name;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.registry.register("dna", "http://www.modeshape.org/namespace");
    }

    @Test
    public void shouldCreateNameFromSingleStringInPrefixedNamespaceFormatWithoutPrefix() {
        name = nameFactory.create("a");
        assertThat(name.getLocalName(), is("a"));
        assertThat(name.getNamespaceUri(), is(this.registry.getNamespaceForPrefix("")));
    }

    @Test
    public void shouldCreateNameFromSingleStringInPrefixedNamespaceFormat() {
        name = nameFactory.create("dna:something");
        assertThat(name.getLocalName(), is("something"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}something"));
    }

    @Test
    public void shouldCreateNameFromSingleEncodedStringInPrefixedNamespaceFormat() {
        name = nameFactory.create(encoder.encode("dna") + ":" + encoder.encode("some/thing"));
        assertThat(name.getLocalName(), is("some/thing"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}some/thing"));
    }

    @Test
    public void shouldCreateNameFromSingleStringInStandardFullNamespaceFormat() {
        name = nameFactory.create("{http://www.modeshape.org/namespace}something");
        assertThat(name.getLocalName(), is("something"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}something"));
    }

    @Test
    public void shouldCreateNameFromSingleEncodedStringInStandardFullNamespaceFormat() {
        name = nameFactory.create("{" + encoder.encode("http://www.modeshape.org/namespace") + "}" + encoder.encode("some/thing"));
        assertThat(name.getLocalName(), is("some/thing"));
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}some/thing"));
    }

    @Test
    public void shouldProvideAccessToNamespaceRegistryPassedInConstructor() {
        assertThat(nameFactory.getNamespaceRegistry(), is(registry));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullLocalName() {
        nameFactory.create("a", (String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullLocalNameWithEncoder() {
        nameFactory.create("a", null, decoder);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyLocalName() {
        nameFactory.create("a", "");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyLocalNameWithEncoder() {
        nameFactory.create("a", "", decoder);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotCreateNameFromStringWithMultipleNonEscapedColons() {
        // This is a requirement of JCR, per the JCR TCK
        nameFactory.create("a:b:c");
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotAllowStringWithUnclosedBrace() {
        nameFactory.create("{something");
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotAllowLocalNameWithBlankPrefix() {
        name = nameFactory.create(":something");
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            values.add("dna:something" + i);
        }
        Iterator<Name> iter = nameFactory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(nameFactory.create(valueIter.next())));
        }
    }

    @Test
    public void shouldCreateNameWithBlankUri() {
        name = nameFactory.create("{}something");
        assertThat(name.getNamespaceUri(), is(""));
        assertThat(name.getLocalName(), is("something"));
        assertThat(name.getString(NO_OP_ENCODER), is("something"));
    }

    @Test
    public void shouldCreateNameWithBlankUriAndBlankName() {
        name = nameFactory.create("{}");
        assertThat(name.getNamespaceUri(), is(""));
        assertThat(name.getLocalName(), is(""));
        assertThat(name.getString(NO_OP_ENCODER), is(""));
    }

    @Test
    public void shouldCreateNameWithBlankPrefixAndBlankLocalName() {
        name = nameFactory.create(":");
        assertThat(name.getNamespaceUri(), is(""));
        assertThat(name.getLocalName(), is(""));
        assertThat(name.getString(NO_OP_ENCODER), is(""));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateNameWithUriAndBlankLocalName() {
        name = nameFactory.create("{http://www.modeshape.org/namespace}");
    }

    @Test
    public void shouldCreateNameWithPrefixAndBlankLocalName() {
        name = nameFactory.create("dna:");
        assertThat(name.getNamespaceUri(), is("http://www.modeshape.org/namespace"));
        assertThat(name.getLocalName(), is(""));
        assertThat(name.getString(NO_OP_ENCODER), is("{http://www.modeshape.org/namespace}"));
    }

    @Ignore
    @Test
    public void shouldCreateFromNonEncodedString() {
        // Warm up ...
        for (int i = 0; i != 10; ++i) {
            name = nameFactory.create("dna:something");
        }
        Stopwatch sw = new Stopwatch();
        int count = 20000000;
        sw.start();
        for (int i = 0; i != count; ++i) {
            name = nameFactory.create("dna:something");
        }
        sw.stop();
        System.out.println("Total duration for " + count + " calls: " + sw.getTotalDuration());
    }
}

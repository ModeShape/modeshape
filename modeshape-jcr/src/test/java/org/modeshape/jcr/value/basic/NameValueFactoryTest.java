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

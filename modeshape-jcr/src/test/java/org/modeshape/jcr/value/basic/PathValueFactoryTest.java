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
import static org.modeshape.jcr.value.basic.IsPathContaining.hasSegments;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 */
public class PathValueFactoryTest extends BaseValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private PathValueFactory factory;
    private Path path;
    private Path path2;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.registry.register("dna", "http://www.modeshape.org/namespace");
        this.factory = new PathValueFactory(Path.DEFAULT_DECODER, valueFactories);
    }

    protected List<Path.Segment> getSegments( String... segments ) {
        List<Path.Segment> result = new ArrayList<Path.Segment>();
        for (String segmentStr : segments) {
            Name name = nameFactory.create(segmentStr);
            BasicPathSegment segment = new BasicPathSegment(name);
            result.add(segment);
        }
        return result;
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndNoParentOrSelfReferences() {
        path = factory.create("/a/b/c/d/dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "d", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndNoParentOrSelfReferences() {
        path = factory.create("a/b/c/d/dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "d", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndParentReference() {
        path = factory.create("/a/b/c/../dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "..", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndParentReference() {
        path = factory.create("a/b/c/../dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "..", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndSelfReference() {
        path = factory.create("/a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndSelfReference() {
        path = factory.create("a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathBeginningWithSelfReference() {
        path = factory.create("./a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, ".", "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateEquivalentPathsWhetherOrNotThereIsATrailingDelimiter() {
        path = factory.create("/a/b/c/d/dna:e/dna:f");
        path2 = factory.create("/a/b/c/d/dna:e/dna:f/");
        assertThat(path.equals(path2), is(true));
        assertThat(path.compareTo(path2), is(0));
    }

    @Test
    public void shouldCreateIteratorOverValuesWhenSuppliedIteratorOfUnknownObjects() {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i != 10; ++i) {
            values.add("/a/b/c/d/dna:e/dna:f" + i);
        }
        Iterator<Path> iter = factory.create(values.iterator());
        Iterator<String> valueIter = values.iterator();
        while (iter.hasNext()) {
            assertThat(iter.next(), is(factory.create(valueIter.next())));
        }
    }

    @Test
    public void shouldSplitPathWithExpandedNamespace() {
        String[] splits = factory.splitPath("/{http://www.jcp.org/jcr/1.0}foo/bar/{blah}baz");
        String[] correctSplits = new String[] {"{http://www.jcp.org/jcr/1.0}foo", "bar", "{blah}baz"};

        assertThat(splits, is(correctSplits));
    }

    @Test
    public void shouldSplitPathWithoutExpandedNamespace() {
        String[] splits = factory.splitPath("/jcr:foo/bar/blah:baz");
        String[] correctSplits = new String[] {"jcr:foo", "bar", "blah:baz"};

        assertThat(splits, is(correctSplits));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldThrowValueFormatExceptionOnMalformedName() {
        factory.splitPath("/jcr:foo/{foobar");
    }
}

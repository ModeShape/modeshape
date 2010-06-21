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
import static org.modeshape.graph.property.basic.IsPathContaining.hasSegments;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * @author Randall Hauch
 */
public class PathValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private SimpleNamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory nameFactory;
    private PathValueFactory factory;
    private Path path;
    private Path path2;

    @Before
    public void beforeEach() {
        this.registry = new SimpleNamespaceRegistry();
        this.registry.register("dna", "http://www.modeshape.org/namespace");
        this.stringValueFactory = new StringValueFactory(registry, Path.DEFAULT_DECODER, Path.DEFAULT_ENCODER);
        this.nameFactory = new NameValueFactory(registry, Path.DEFAULT_DECODER, stringValueFactory);
        this.factory = new PathValueFactory(Path.DEFAULT_DECODER, stringValueFactory, nameFactory);
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

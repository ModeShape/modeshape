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

import static org.jboss.dna.graph.properties.basic.IsPathContaining.hasSegments;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.graph.properties.basic.BasicNamespaceRegistry;
import org.jboss.dna.graph.properties.basic.BasicPathSegment;
import org.jboss.dna.graph.properties.basic.NameValueFactory;
import org.jboss.dna.graph.properties.basic.PathValueFactory;
import org.jboss.dna.graph.properties.basic.StringValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class PathValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private BasicNamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory nameFactory;
    private PathValueFactory factory;
    private Path path;
    private Path path2;

    @Before
    public void beforeEach() {
        this.registry = new BasicNamespaceRegistry();
        this.registry.register("dna", "http://www.jboss.org/dna/namespace");
        this.stringValueFactory = new StringValueFactory(Path.DEFAULT_DECODER, Path.DEFAULT_ENCODER);
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

}

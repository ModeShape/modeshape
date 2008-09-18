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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.ValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class BasicPathSegmentTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private BasicNamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory nameFactory;
    private PathValueFactory factory;
    private Name validName;
    private Path.Segment segment;
    private Path.Segment segment2;

    @Before
    public void beforeEach() {
        this.registry = new BasicNamespaceRegistry();
        this.registry.register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        this.stringValueFactory = new StringValueFactory(Path.DEFAULT_DECODER, Path.DEFAULT_ENCODER);
        this.nameFactory = new NameValueFactory(registry, Path.DEFAULT_DECODER, stringValueFactory);
        this.validName = nameFactory.create("dna:something");
        this.factory = new PathValueFactory(Path.DEFAULT_DECODER, stringValueFactory, nameFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeIndex() {
        new BasicPathSegment(validName, -2);
    }

    @Test
    public void shouldConsiderEqualTwoSegmentsWithSameNameAndIndex() {
        segment = new BasicPathSegment(validName);
        segment2 = new BasicPathSegment(validName);
        assertThat(segment.equals(segment2), is(true));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName(), is(validName));
    }

    @Test
    public void shouldHaveIndexSpecifiedInConstructor() {
        segment = new BasicPathSegment(validName, 3);
        assertThat(segment.getIndex(), is(3));
        assertThat(segment.hasIndex(), is(true));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName(), is(validName));
    }

    @Test
    public void shouldNotHaveIndexByDefault() {
        segment = new BasicPathSegment(validName);
        assertThat(segment.getIndex(), is(Path.NO_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName(), is(validName));
    }

    @Test
    public void shouldCreateSelfReferenceSegmentIfPassedSelfReferenceStringRegardlessOfIndex() {
        segment = factory.createSegment(Path.SELF);
        assertThat(segment.getIndex(), is(Path.NO_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(true));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName().getLocalName(), is(Path.SELF));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));

        segment = factory.createSegment(Path.SELF, 1);
        assertThat(segment.getIndex(), is(Path.NO_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(true));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName().getLocalName(), is(Path.SELF));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));
    }

    @Test
    public void shouldCreateParentReferenceSegmentIfPassedParentReferenceStringRegardlessOfIndex() {
        segment = factory.createSegment(Path.PARENT);
        assertThat(segment.getIndex(), is(Path.NO_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(true));
        assertThat(segment.getName().getLocalName(), is(Path.PARENT));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));

        segment = factory.createSegment(Path.PARENT, 1);
        assertThat(segment.getIndex(), is(Path.NO_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(true));
        assertThat(segment.getName().getLocalName(), is(Path.PARENT));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));
    }

    @Test
    public void shouldConsiderSegmentCreatedWithSelfReferenceToBeEqualToStaticSingleton() {
        segment = factory.createSegment(Path.SELF);
        assertThat(segment.equals(Path.SELF_SEGMENT), is(true));
    }

    @Test
    public void shouldConsiderSegmentCreatedWithParentReferenceToBeEqualToStaticSingleton() {
        segment = factory.createSegment(Path.PARENT);
        assertThat(segment.equals(Path.PARENT_SEGMENT), is(true));
    }

    @Test
    public void shouldConsiderSegmentWithSameNameSiblingIndexOfOneToBeEqualToSegmentWithSameNameButNoIndex() {
        segment = new BasicPathSegment(validName, Path.NO_INDEX);
        Path.Segment segment2 = new BasicPathSegment(validName, 1);
        assertThat(segment, is(segment2));
    }

    @Test
    public void shouldConsiderSegmentWithSameNameSiblingIndexOfTwoOrMoreToNotBeEqualToSegmentWithSameNameButNoIndex() {
        segment = new BasicPathSegment(validName, Path.NO_INDEX);
        Path.Segment segment2 = new BasicPathSegment(validName, 2);
        assertThat(segment, is(not(segment2)));
        segment2 = new BasicPathSegment(validName, 3);
        assertThat(segment, is(not(segment2)));
    }

}

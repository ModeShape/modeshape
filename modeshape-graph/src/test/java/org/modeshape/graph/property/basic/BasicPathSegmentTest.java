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
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class BasicPathSegmentTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private NamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory nameFactory;
    private PathValueFactory factory;
    private Name validName;
    private Path.Segment segment;
    private Path.Segment segment2;

    @Before
    public void beforeEach() {
        this.registry = new SimpleNamespaceRegistry();
        this.registry.register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        this.stringValueFactory = new StringValueFactory(registry, Path.DEFAULT_DECODER, Path.DEFAULT_ENCODER);
        this.nameFactory = new NameValueFactory(registry, Path.DEFAULT_DECODER, stringValueFactory);
        this.validName = nameFactory.create("mode:something");
        this.factory = new PathValueFactory(Path.DEFAULT_DECODER, stringValueFactory, nameFactory);
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
        assertThat(segment.getIndex(), is(Path.DEFAULT_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName(), is(validName));
    }

    @Test
    public void shouldCreateSelfReferenceSegmentIfPassedSelfReferenceStringRegardlessOfIndex() {
        segment = factory.createSegment(Path.SELF);
        assertThat(segment.getIndex(), is(Path.DEFAULT_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(true));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName().getLocalName(), is(Path.SELF));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));

        segment = factory.createSegment(Path.SELF, 1);
        assertThat(segment.getIndex(), is(Path.DEFAULT_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(true));
        assertThat(segment.isParentReference(), is(false));
        assertThat(segment.getName().getLocalName(), is(Path.SELF));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));
    }

    @Test
    public void shouldCreateParentReferenceSegmentIfPassedParentReferenceStringRegardlessOfIndex() {
        segment = factory.createSegment(Path.PARENT);
        assertThat(segment.getIndex(), is(Path.DEFAULT_INDEX));
        assertThat(segment.hasIndex(), is(false));
        assertThat(segment.isSelfReference(), is(false));
        assertThat(segment.isParentReference(), is(true));
        assertThat(segment.getName().getLocalName(), is(Path.PARENT));
        assertThat(segment.getName().getNamespaceUri().length(), is(0));

        segment = factory.createSegment(Path.PARENT, 1);
        assertThat(segment.getIndex(), is(Path.DEFAULT_INDEX));
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
        segment = new BasicPathSegment(validName, Path.DEFAULT_INDEX);
        Path.Segment segment2 = new BasicPathSegment(validName, 1);
        assertThat(segment, is(segment2));
    }

    @Test
    public void shouldConsiderSegmentWithSameNameSiblingIndexOfTwoOrMoreToNotBeEqualToSegmentWithSameNameButNoIndex() {
        segment = new BasicPathSegment(validName, Path.DEFAULT_INDEX);
        Path.Segment segment2 = new BasicPathSegment(validName, 2);
        assertThat(segment, is(not(segment2)));
        segment2 = new BasicPathSegment(validName, 3);
        assertThat(segment, is(not(segment2)));
    }

    @Test
    public void shouldUseDelimiterEncoderToEncodeDelimiterBetweenPrefixAndLocalPart() {
        TextEncoder encoder = new Jsr283Encoder();
        validName = new BasicName(ModeShapeLexicon.Namespace.URI, "some:name:with:colons");
        segment = new BasicPathSegment(validName, Path.DEFAULT_INDEX);
        TextEncoder delimiterEncoder = new TextEncoder() {
            public String encode( String text ) {
                if (":".equals(text)) return "\\:";
                if ("{".equals(text)) return "\\{";
                if ("}".equals(text)) return "\\}";
                return text;
            }
        };
        assertThat(segment.getString(registry, encoder, delimiterEncoder), is("mode\\:some\uf03aname\uf03awith\uf03acolons"));
        assertThat(segment.getString(null, encoder, delimiterEncoder), is("\\{" + encoder.encode(ModeShapeLexicon.Namespace.URI)
                                                                          + "\\}some\uf03aname\uf03awith\uf03acolons"));
    }
}

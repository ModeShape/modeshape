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
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * @author Randall Hauch
 */
public class BasicPathSegmentTest extends BaseValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private PathValueFactory factory;
    private Name validName;
    private Path.Segment segment;
    private Path.Segment segment2;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        this.registry.register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        this.validName = nameFactory.create("mode:something");
        this.factory = new PathValueFactory(Path.DEFAULT_DECODER, valueFactories);
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
            @Override
            public String encode( String text ) {
                if (":".equals(text)) return "\\:";
                if ("{".equals(text)) return "\\{";
                if ("}".equals(text)) return "\\}";
                return text;
            }
        };
        assertThat(segment.getString(registry, encoder, delimiterEncoder), is("mode\\:some\uf03aname\uf03awith\uf03acolons"));
        assertThat(segment.getString(null, encoder, delimiterEncoder), is("mode:some\uf03aname\uf03awith\uf03acolons"));
    }
}

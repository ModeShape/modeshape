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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ChildPathTest extends AbstractPathTest {

    protected Path parent;
    protected Path root;
    protected Path.Segment childSegment;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        // parent = mock(Path.class);
        parent = path("/a/b/c");
        childSegment = segment("d");
        path = new ChildPath(parent, childSegment);
        root = RootPath.INSTANCE;
    }

    protected Path path( String path ) {
        path = path.trim();
        if ("/".equals(path)) return RootPath.INSTANCE;
        boolean absolute = path.startsWith("/");
        path = path.replaceAll("^/+", "").replaceAll("/+$", ""); // remove leading and trailing slashes
        String[] segmentStrings = path.split("/");
        List<Path.Segment> segments = new ArrayList<Path.Segment>(segmentStrings.length);
        for (String segmentString : segmentStrings) {
            Name name = new BasicName("", segmentString);
            Path.Segment segment = new BasicPathSegment(name);
            segments.add(segment);
        }
        return new BasicPath(segments, absolute);
    }

    protected Path.Segment segment( String segment ) {
        Name name = new BasicName("", segment);
        return new BasicPathSegment(name);
    }

    @Test
    public void shouldReturnParentForAncestorOfDegreeOne() {
        assertThat(path.getAncestor(1), is(sameInstance(parent)));
        assertThat(path.getParent(), is(sameInstance(parent)));
    }

    @Test
    public void shouldDelegateToParentForAncestorOfDegreeGreaterThanOne() {
        parent = mock(Path.class);
        when(parent.getAncestor(anyInt())).thenReturn(null);
        path = new ChildPath(parent, segment("d"));
        for (int i = 2; i != 10; ++i) {
            path.getAncestor(i);
            verify(parent).getAncestor(i - 1);
        }
    }

    @Test
    public void shouldConsiderChildPathToBeDecendantOfParent() {
        assertThat(path.isDecendantOf(parent), is(true));
    }

    @Test
    public void shouldConsiderChildPathToNotBeAncestorOfParent() {
        assertThat(path.isAncestorOf(parent), is(false));
    }

    @Test
    public void shouldConsiderParentNotBeDecendantOfChildPath() {
        assertThat(parent.isDecendantOf(path), is(false));
    }

    @Test
    public void shouldConsiderPathDecendantOfOtherPathIfParentIsAtOrBelowOtherPath() {
        parent = mock(Path.class);
        path = new ChildPath(parent, segment("d"));
        Path other = mock(Path.class);
        when(parent.isAtOrBelow(other)).thenReturn(true);
        assertThat(path.isDecendantOf(other), is(true));
        verify(parent).isAtOrBelow(other);

        when(parent.isAtOrBelow(other)).thenReturn(false);
        assertThat(path.isDecendantOf(other), is(false));
        verify(parent, times(2)).isAtOrBelow(other);
    }

    @Test
    public void shouldConsiderPathDecendantOfOtherParentPath() {
        assertThat(path.isDecendantOf(parent), is(true));
    }

    @Test
    public void shouldReturnChildSegmentFromGetLastSegment() {
        assertThat(path.getLastSegment(), is(sameInstance(childSegment)));
    }

    @Test
    public void shouldReturnChildSegmentFromGetSegmentWithIndexOfSizeMinusOne() {
        assertThat(path.getSegment(path.size() - 1), is(sameInstance(childSegment)));
    }

    @Test
    public void shouldDelegateGetSegmentToParentIfIndexNotEqualToSizeMinusOne() {
        Path.Segment segment = mock(Path.Segment.class);
        parent = mock(Path.class);
        when(parent.size()).thenReturn(10);
        path = new ChildPath(parent, segment("d"));
        when(parent.getSegment(anyInt())).thenReturn(segment);
        for (int i = 0; i < path.size() - 1; ++i) {
            assertThat(path.getSegment(i), is(sameInstance(segment)));
        }
        verify(parent, times(parent.size())).getSegment(anyInt());
    }

    @Test
    public void shouldReturnParentInstanceFromGetParent() {
        assertThat(path.getParent(), is(sameInstance(parent)));
    }

    @Test
    public void shouldConsiderAsNotNormalizedAPathWithParentSegmentAtEnd() {
        path = new ChildPath(parent, Path.PARENT_SEGMENT);
        assertThat(path.isAbsolute(), is(parent.isAbsolute()));
        assertThat(path.isNormalized(), is(false));
    }

    @Test
    public void shouldConsiderAsNormalizedARelativePathWithParentSegmentAtFront() {
        parent = path("../../a/b/c/d");
        path = new ChildPath(parent, segment("e"));
        assertThat(path.isNormalized(), is(true));
    }

    @Test
    public void shouldConsiderAsNormalizedAnAbsolutePathWithParentSegmentAtFront() {
        parent = path("/../a/b");
        path = new ChildPath(parent, segment("c"));
        assertThat(path.isNormalized(), is(false));
    }

    @Test
    public void shouldConsiderAsNormalizedPathWithAllParentReferences() {
        parent = path("../../../../..");
        path = new ChildPath(parent, Path.PARENT_SEGMENT);
        assertThat(path.isNormalized(), is(true));
    }

    @Test
    public void shouldConsiderAsNotNormalizedPathWithMostParentReferencesAndOneNonParentReferenceInMiddle() {
        parent = path("../../a/b/../..");
        path = new ChildPath(parent, Path.PARENT_SEGMENT);
        assertThat(path.isNormalized(), is(false));
    }

    // @Test
    // public void shouldReturnRootForLowestCommonAncestorWithAnyNodePath() {
    // Path other = mock(Path.class);
    // when(other.isRoot()).thenReturn(true);
    // assertThat(root.getCommonAncestor(other).isRoot(), is(true));
    //
    // when(other.isRoot()).thenReturn(false);
    // assertThat(root.getCommonAncestor(other).isRoot(), is(true));
    // }
    //
    // @Test
    // public void shouldConsiderRootToBeAncestorOfEveryNodeExceptRoot() {
    // Path other = mock(Path.class);
    // when(other.size()).thenReturn(1);
    // assertThat(root.isAncestorOf(other), is(true));
    // assertThat(root.isAncestorOf(root), is(false));
    // }
    //
    // @Test
    // public void shouldNotConsiderRootNodeToBeDecendantOfAnyNode() {
    // Path other = mock(Path.class);
    // assertThat(root.isDecendantOf(other), is(false));
    // assertThat(root.isDecendantOf(root), is(false));
    // }
    //
    // @Test
    // public void shouldConsiderTwoRootNodesToHaveSameAncestor() {
    // assertThat(root.hasSameAncestor(root), is(true));
    // }
    //
    // @Test
    // public void shouldBeNormalized() {
    // assertThat(root.isNormalized(), is(true));
    // }
    //
    // @Test
    // public void shouldReturnSelfForGetNormalized() {
    // assertThat(root.getNormalizedPath(), is(sameInstance(root)));
    // }
    //
    // @Test
    // public void shouldReturnSelfForGetCanonicalPath() {
    // assertThat(root.getCanonicalPath(), is(sameInstance(root)));
    // }
    //
    // @Test
    // public void shouldReturnSizeOfZero() {
    // assertThat(root.size(), is(0));
    // }
    //
    // @Test
    // public void shouldReturnEmptyIteratorOverSegments() {
    // assertThat(root.iterator(), is(notNullValue()));
    // assertThat(root.iterator().hasNext(), is(false));
    // }
    //
    // @Test( expected = IndexOutOfBoundsException.class )
    // public void shouldFailToReturnSegmentAtIndexZero() {
    // root.getSegment(0);
    // }
    //
    // @Test( expected = IndexOutOfBoundsException.class )
    // public void shouldFailToReturnSegmentAtPositiveIndex() {
    // root.getSegment(1);
    // }
    //
    // @Test
    // public void shouldReturnEmptySegmentsArray() {
    // assertThat(root.getSegmentsArray(), is(notNullValue()));
    // assertThat(root.getSegmentsArray().length, is(0));
    // }
    //
    // @Test
    // public void shouldReturnEmptySegmentsList() {
    // assertThat(root.getSegmentsList(), is(notNullValue()));
    // assertThat(root.getSegmentsList().isEmpty(), is(true));
    // }
    //
    // @Test
    // public void shouldAlwaysReturnPathWithSingleSlashForGetString() {
    // NamespaceRegistry registry = mock(NamespaceRegistry.class);
    // TextEncoder encoder = mock(TextEncoder.class);
    // when(encoder.encode("/")).thenReturn("/");
    // assertThat(root.getString(), is("/"));
    // assertThat(root.getString(registry), is("/"));
    // assertThat(root.getString(registry, encoder), is("/"));
    // assertThat(root.getString(registry, encoder, encoder), is("/"));
    // assertThat(root.getString(encoder), is("/"));
    // }
    //
    // @Test
    // public void shouldAllowNullNamespaceRegistryWithNonNullTextEncodersSinceRegistryIsNotNeeded() {
    // TextEncoder encoder = mock(TextEncoder.class);
    // when(encoder.encode("/")).thenReturn("/");
    // assertThat(root.getString((NamespaceRegistry)null, encoder, encoder), is("/"));
    // }
    //
    // @Test
    // public void shouldAllowNullTextEncoder() {
    // assertThat(root.getString((TextEncoder)null), is("/"));
    // }
    //
    // @Test
    // public void shouldNotAllowNullTextEncoderWithNonNullNamespaceRegistry() {
    // NamespaceRegistry registry = mock(NamespaceRegistry.class);
    // assertThat(root.getString(registry, (TextEncoder)null), is("/"));
    // }
    //
    // @Test
    // public void shouldNotAllowNullTextEncoderForDelimiterWithNonNullNamespaceRegistry() {
    // NamespaceRegistry registry = mock(NamespaceRegistry.class);
    // TextEncoder encoder = mock(TextEncoder.class);
    // assertThat(root.getString(registry, encoder, (TextEncoder)null), is("/"));
    // }
    //
    // @Test( expected = IndexOutOfBoundsException.class )
    // public void shouldNotAllowSubpathStartingAtOne() {
    // root.subpath(1);
    // }
    //
    // @Test( expected = IndexOutOfBoundsException.class )
    // public void shouldNotAllowSubpathStartingAtMoreThanOne() {
    // root.subpath(2);
    // }
    //
    // @Test( expected = IndexOutOfBoundsException.class )
    // public void shouldNotAllowSubpathEndingAtMoreThanZero() {
    // root.subpath(0, 1);
    // }
    //
    // @Test
    // public void shouldReturnRelativePathConsistingOfSameNumberOfParentReferencesAsSizeOfSuppliedPath() {
    // List<Path.Segment> segments = new ArrayList<Path.Segment>();
    // segments.add(new BasicPathSegment(new BasicName("http://example.com", "a")));
    // Path other = new BasicPath(segments, true);
    //
    // assertThat(root.relativeTo(other).toString(), is(".."));
    //
    // segments.add(new BasicPathSegment(new BasicName("http://example.com", "b")));
    // other = new BasicPath(segments, true);
    // assertThat(root.relativeTo(other).toString(), is("../.."));
    //
    // String expected = "..";
    // segments.clear();
    // for (int i = 1; i != 100; ++i) {
    // segments.add(new BasicPathSegment(new BasicName("http://example.com", "b" + i)));
    // other = new BasicPath(segments, true);
    // assertThat(root.relativeTo(other).toString(), is(expected));
    // expected = expected + "/..";
    // }
    // }
    //
    // @Test
    // public void shouldResolveAllRelativePathsToTheirAbsolutePath() {
    // List<Path.Segment> segments = new ArrayList<Path.Segment>();
    // segments.add(new BasicPathSegment(new BasicName("http://example.com", "a")));
    // Path other = mock(Path.class);
    // when(other.isAbsolute()).thenReturn(false);
    // when(other.getSegmentsList()).thenReturn(segments);
    // Path resolved = root.resolve(other);
    // assertThat(resolved.getSegmentsList(), is(segments));
    // assertThat(resolved.isAbsolute(), is(true));
    // }
    //
    // @Test( expected = InvalidPathException.class )
    // public void shouldNotResolveRelativePathUsingAnAbsolutePath() {
    // Path other = mock(Path.class);
    // when(other.isAbsolute()).thenReturn(true);
    // root.resolve(other);
    // }
    //
    // @Test
    // public void shouldAlwaysConsiderRootAsLessThanAnyPathOtherThanRoot() {
    // Path other = mock(Path.class);
    // when(other.isRoot()).thenReturn(false);
    // assertThat(root.compareTo(other), is(-1));
    // assertThat(root.equals(other), is(false));
    // }
    //
    // @Test
    // public void shouldAlwaysConsiderRootAsEqualToAnyOtherRoot() {
    // Path other = mock(Path.class);
    // when(other.isRoot()).thenReturn(true);
    // assertThat(root.compareTo(other), is(0));
    // assertThat(root.equals(other), is(true));
    // assertThat(root.equals(root), is(true));
    // }

}

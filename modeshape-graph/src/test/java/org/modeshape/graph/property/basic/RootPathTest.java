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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class RootPathTest extends AbstractPathTest {

    protected Path root;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        super.path = RootPath.INSTANCE;
        root = path;
    }

    @Test
    public void shouldReturnRootForLowestCommonAncestorWithAnyNodePath() {
        Path other = mock(Path.class);
        when(other.isRoot()).thenReturn(true);
        assertThat(root.getCommonAncestor(other).isRoot(), is(true));

        when(other.isRoot()).thenReturn(false);
        assertThat(root.getCommonAncestor(other).isRoot(), is(true));
    }

    @Test
    public void shouldConsiderRootToBeAncestorOfEveryNodeExceptRoot() {
        Path other = mock(Path.class);
        when(other.size()).thenReturn(1);
        assertThat(root.isAncestorOf(other), is(true));
        assertThat(root.isAncestorOf(root), is(false));
    }

    @Test
    public void shouldNotConsiderRootNodeToBeDecendantOfAnyNode() {
        Path other = mock(Path.class);
        assertThat(root.isDecendantOf(other), is(false));
        assertThat(root.isDecendantOf(root), is(false));
    }

    @Test
    public void shouldConsiderTwoRootNodesToHaveSameAncestor() {
        assertThat(root.hasSameAncestor(root), is(true));
    }

    @Test
    public void shouldBeNormalized() {
        assertThat(root.isNormalized(), is(true));
    }

    @Test
    public void shouldReturnSelfForGetNormalized() {
        assertThat(root.getNormalizedPath(), is(sameInstance(root)));
    }

    @Test
    public void shouldReturnSelfForGetCanonicalPath() {
        assertThat(root.getCanonicalPath(), is(sameInstance(root)));
    }

    @Test
    public void shouldReturnSizeOfZero() {
        assertThat(root.size(), is(0));
    }

    @Override
    @Test( expected = IllegalStateException.class )
    public void shouldReturnImmutableSegmentsIterator() {
        root.iterator().remove();
    }

    @Test
    public void shouldReturnEmptyIteratorOverSegments() {
        assertThat(root.iterator(), is(notNullValue()));
        assertThat(root.iterator().hasNext(), is(false));
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSegmentAtIndexZero() {
        root.getSegment(0);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSegmentAtPositiveIndex() {
        root.getSegment(1);
    }

    @Test
    public void shouldReturnEmptySegmentsArray() {
        assertThat(root.getSegmentsArray(), is(notNullValue()));
        assertThat(root.getSegmentsArray().length, is(0));
    }

    @Test
    public void shouldReturnEmptySegmentsList() {
        assertThat(root.getSegmentsList(), is(notNullValue()));
        assertThat(root.getSegmentsList().isEmpty(), is(true));
    }

    @Test
    public void shouldAlwaysReturnPathWithSingleSlashForGetString() {
        NamespaceRegistry registry = mock(NamespaceRegistry.class);
        TextEncoder encoder = mock(TextEncoder.class);
        when(encoder.encode("/")).thenReturn("/");
        assertThat(root.getString(), is("/"));
        assertThat(root.getString(registry), is("/"));
        assertThat(root.getString(registry, encoder), is("/"));
        assertThat(root.getString(registry, encoder, encoder), is("/"));
        assertThat(root.getString(encoder), is("/"));
    }

    @Test
    public void shouldAllowNullNamespaceRegistryWithNonNullTextEncodersSinceRegistryIsNotNeeded() {
        TextEncoder encoder = mock(TextEncoder.class);
        when(encoder.encode("/")).thenReturn("/");
        assertThat(root.getString((NamespaceRegistry)null, encoder, encoder), is("/"));
    }

    @Test
    public void shouldAllowNullTextEncoder() {
        assertThat(root.getString((TextEncoder)null), is("/"));
    }

    @Test
    public void shouldNotAllowNullTextEncoderWithNonNullNamespaceRegistry() {
        NamespaceRegistry registry = mock(NamespaceRegistry.class);
        assertThat(root.getString(registry, (TextEncoder)null), is("/"));
    }

    @Test
    public void shouldNotAllowNullTextEncoderForDelimiterWithNonNullNamespaceRegistry() {
        NamespaceRegistry registry = mock(NamespaceRegistry.class);
        TextEncoder encoder = mock(TextEncoder.class);
        assertThat(root.getString(registry, encoder, (TextEncoder)null), is("/"));
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldNotAllowSubpathStartingAtOne() {
        root.subpath(1);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldNotAllowSubpathStartingAtMoreThanOne() {
        root.subpath(2);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldNotAllowSubpathEndingAtMoreThanZero() {
        root.subpath(0, 1);
    }

    @Test
    public void shouldReturnRelativePathConsistingOfSameNumberOfParentReferencesAsSizeOfSuppliedPath() {
        List<Path.Segment> segments = new ArrayList<Path.Segment>();
        segments.add(new BasicPathSegment(new BasicName("http://example.com", "a")));
        Path other = new BasicPath(segments, true);

        assertThat(root.relativeTo(other).toString(), is(".."));

        segments.add(new BasicPathSegment(new BasicName("http://example.com", "b")));
        other = new BasicPath(segments, true);
        assertThat(root.relativeTo(other).toString(), is("../.."));

        String expected = "..";
        segments.clear();
        for (int i = 1; i != 100; ++i) {
            segments.add(new BasicPathSegment(new BasicName("http://example.com", "b" + i)));
            other = new BasicPath(segments, true);
            assertThat(root.relativeTo(other).toString(), is(expected));
            expected = expected + "/..";
        }
    }

    @Test
    public void shouldResolveAllRelativePathsToTheirAbsolutePath() {
        List<Path.Segment> segments = new ArrayList<Path.Segment>();
        segments.add(new BasicPathSegment(new BasicName("http://example.com", "a")));
        Path other = mock(Path.class);
        when(other.isAbsolute()).thenReturn(false);
        when(other.getSegmentsList()).thenReturn(segments);
        when(other.getNormalizedPath()).thenReturn(other);
        Path resolved = root.resolve(other);
        assertThat(resolved.getSegmentsList(), is(segments));
        assertThat(resolved.isAbsolute(), is(true));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotResolveRelativePathUsingAnAbsolutePath() {
        Path other = mock(Path.class);
        when(other.isAbsolute()).thenReturn(true);
        root.resolve(other);
    }

    @Test
    public void shouldAlwaysConsiderRootAsLessThanAnyPathOtherThanRoot() {
        Path other = mock(Path.class);
        when(other.isRoot()).thenReturn(false);
        assertThat(root.compareTo(other), is(-1));
        assertThat(root.equals(other), is(false));
    }

    @Test
    public void shouldAlwaysConsiderRootAsEqualToAnyOtherRoot() {
        Path other = mock(Path.class);
        when(other.isRoot()).thenReturn(true);
        assertThat(root.compareTo(other), is(0));
        assertThat(root.equals(other), is(true));
        assertThat(root.equals(root), is(true));
    }

    @Test
    public void shouldReturnIteratorWithRootPathFromPathsFromRoot() {
        Iterator<Path> iter = root.pathsFromRoot();
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(root));
        assertThat(iter.hasNext(), is(false));
    }

}

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
import static org.hamcrest.core.IsNull.nullValue;
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
import org.modeshape.graph.property.Path.Segment;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public abstract class AbstractPathTest {

    protected Path path;

    @Before
    public void beforeEach() {
    }

    @Test
    public void shouldReturnNoAncestorForRoot() {
        if (path.isRoot()) assertThat(path.getParent(), nullValue());
    }

    @Test
    public void shouldReturnRootForAnyAncestorExactDegreeFromRoot() {
        assertThat(path.getAncestor(path.size()).isRoot(), is(true));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotAllowAncestorDegreeLargerThanSize() {
        path.getAncestor(path.size() + 1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeAncestorDegree() {
        path.getAncestor(-1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGettingAncestorOfNullPath() {
        path.getCommonAncestor(null);
    }

    @Test
    public void shouldNotConsiderNodeToBeAncestorOfItself() {
        assertThat(path.isAncestorOf(path), is(false));
    }

    @Test
    public void shouldNotConsiderNodeToBeDecendantOfItself() {
        assertThat(path.isDecendantOf(path), is(false));
    }

    @Test
    public void shouldConsiderANodeToHaveSameAncestorAsItself() {
        assertThat(path.hasSameAncestor(path), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailForSameAncestorOfNullPath() {
        path.hasSameAncestor(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailForDecendantOfNullPath() {
        path.isDecendantOf(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailForAtOrAboveNullPath() {
        path.isAtOrAbove(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailForAtOrBelowNullPath() {
        path.isAtOrBelow(null);
    }

    @Test
    public void shouldConsiderNodeToBeAtOrAboveItself() {
        assertThat(path.isAtOrAbove(path), is(true));
    }

    @Test
    public void shouldConsiderNodeToBeAtOrBelowItself() {
        assertThat(path.isAtOrBelow(path), is(true));
    }

    @Test
    public void shouldReturnNullForLastSegmentOfRoot() {
        if (path.isRoot()) {
            assertThat(path.getLastSegment(), is(nullValue()));
        }
    }

    @Test
    public void shouldNeverReturnNullForLastSegmentOfNonRoot() {
        if (!path.isRoot()) {
            assertThat(path.getLastSegment(), is(notNullValue()));
        }
    }

    @Test
    public void shouldReturnNonNullIteratorOverSegments() {
        assertThat(path.iterator(), is(notNullValue()));
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSegmentAtIndexEqualToSize() {
        path.getSegment(path.size());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToReturnSegmentAtNegativeIndex() {
        path.getSegment(-1);
    }

    @Test
    public void shouldReturnNonNullSegmentsArray() {
        assertThat(path.getSegmentsArray(), is(notNullValue()));
    }

    @Test
    public void shouldReturnNonNullSegmentsList() {
        assertThat(path.getSegmentsList(), is(notNullValue()));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableSegmentsList() {
        path.getSegmentsList().add(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableSegmentsIterator() {
        Iterator<Segment> iter = path.iterator();
        if (iter.hasNext()) {
            iter.remove();
        }
    }

    @Test
    public void shouldAlwaysReturnNonNullStringForGetString() {
        assertThat(path.getString(), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNamespaceRegistry() {
        path.getString((NamespaceRegistry)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNamespaceRegistryWithNonNullTextEncoder() {
        TextEncoder encoder = mock(TextEncoder.class);
        path.getString((NamespaceRegistry)null, encoder);
    }

    @Test
    public void shouldReturnSelfForSubpathStartingAtZero() {
        assertThat(path.subpath(0), is(sameInstance(path)));
        assertThat(path.subpath(0, path.size()), is(sameInstance(path)));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowSubpathStartingAtNegativeNumber() {
        path.subpath(-1);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldNotAllowSubpathStartingAtIndexEqualToSize() {
        if (path.isRoot()) path.subpath(1);
        path.subpath(path.size());
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldNotAllowSubpathEndingAtMoreThanSize() {
        path.subpath(0, path.size() + 1);
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotFindRelativePathToAnotherRelativePath() {
        Path other = mock(Path.class);
        when(other.isAbsolute()).thenReturn(false);
        path.relativeTo(other);
    }

    @Test
    public void shouldAlwaysConsiderPathEqualToItself() {
        Path other = mock(Path.class);
        when(other.isRoot()).thenReturn(true);
        assertThat(path.compareTo(path), is(0));
        assertThat(path.equals(path), is(true));
    }

    @Test
    public void shouldAlwaysReturnNonNullToString() {
        assertThat(path.toString(), is(notNullValue()));
    }

    @Test
    public void shouldGetPathsFromRoot() {
        Iterator<Path> iter = path.pathsFromRoot();
        List<Path.Segment> segments = path.getSegmentsList();
        List<Path.Segment> lastSegments = new ArrayList<Path.Segment>();
        while (iter.hasNext()) {
            Path next = iter.next();
            assertThat(next, is(notNullValue()));
            if (!next.isRoot()) lastSegments.add(next.getLastSegment());
        }
        assertThat(lastSegments.size(), is(path.size()));
        assertThat(lastSegments, is(segments));
    }

}

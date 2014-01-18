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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public abstract class AbstractPathTest extends BaseValueFactoryTest {

    protected Path path;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
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
        assertThat(path.isDescendantOf(path), is(false));
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
        path.isDescendantOf(null);
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

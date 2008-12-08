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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.Iterator;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.properties.InvalidPathException;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Path.Segment;
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
        stub(other.isAbsolute()).toReturn(false);
        path.relativeTo(other);
    }

    @Test
    public void shouldAlwaysConsiderPathEqualToItself() {
        Path other = mock(Path.class);
        stub(other.isRoot()).toReturn(true);
        assertThat(path.compareTo(path), is(0));
        assertThat(path.equals(path), is(true));
    }

    @Test
    public void shouldAlwaysReturnNonNullToString() {
        assertThat(path.toString(), is(notNullValue()));
    }

}

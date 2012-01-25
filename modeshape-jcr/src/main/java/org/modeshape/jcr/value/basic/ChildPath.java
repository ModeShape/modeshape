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
package org.modeshape.jcr.value.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.ImmutableAppendedList;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.Path;

/**
 * Implementation of a {@link Path} that has the information for the last segment but that points to another Path for the parent
 * information.
 */
@Immutable
public class ChildPath extends AbstractPath {

    /**
     * The serializable version. Version {@value}
     */
    private static final long serialVersionUID = 1L;

    private final Path parent;
    private final Path.Segment child;
    private final int size;
    private transient List<Segment> cachedSegmentList;

    public ChildPath( Path parent,
                      Path.Segment child ) {
        assert parent != null;
        assert child != null;
        this.parent = parent;
        this.child = child;
        this.size = this.parent.size() + 1;
    }

    @Override
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) return this;
        if (degree == 1) return parent;
        return parent.getAncestor(degree - 1);
    }

    @Override
    protected Iterator<Segment> getSegmentsOfParent() {
        return parent.iterator();
    }

    @Override
    public Segment getLastSegment() {
        return child;
    }

    @Override
    public Path getParent() {
        return parent;
    }

    @Override
    public Segment getSegment( int index ) {
        if (index == (size - 1)) return child;
        return parent.getSegment(index);
    }

    @Override
    public List<Segment> getSegmentsList() {
        if (cachedSegmentList == null) {
            // No need to synchronize, since this is idempotent and thus the list will be as well
            List<Segment> segments = null;
            if (parent.isRoot()) {
                segments = Collections.singletonList(child); // already immutable
            } else if (size < 4) {
                segments = new ArrayList<Segment>(size);
                for (Segment segment : parent) {
                    segments.add(segment);
                }
                segments.add(child);
                segments = Collections.unmodifiableList(segments);
            } else {
                segments = new ImmutableAppendedList<Segment>(parent.getSegmentsList(), child);
            }
            cachedSegmentList = segments;
        }
        return cachedSegmentList;
    }

    @Override
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        if (parent.equals(that.getParent())) return true;
        return super.hasSameAncestor(that);
    }

    @Override
    public boolean isAbsolute() {
        return parent.isAbsolute();
    }

    @Override
    public boolean isAtOrBelow( Path other ) {
        if (this == other || parent == other) return true;
        return super.isAtOrBelow(other);
    }

    @Override
    public boolean isDescendantOf( Path ancestor ) {
        if (parent == ancestor) return true; // same instance
        return parent.isAtOrBelow(ancestor);
    }

    @Override
    public boolean isNormalized() {
        if (child.isSelfReference()) return false;
        if (!parent.isNormalized()) return false;
        // Otherwise, the parent is normalized, so this child will be normalized if this child is not a parent reference ...
        if (!child.isParentReference()) return true;
        // The path ends with a parent reference. It is normalized only if all other path segments are parent references ...
        for (Path.Segment segment : parent) {
            if (!segment.isParentReference()) return false;
        }
        return true;
    }

    @Override
    public boolean isRoot() {
        if (child.isParentReference()) return parent.isRoot();
        return false;
    }

    @Override
    @SuppressWarnings( "synthetic-access" )
    public Iterator<Segment> iterator() {
        if (parent.isRoot()) {
            return new Iterator<Segment>() {
                boolean finished = false;

                @Override
                public boolean hasNext() {
                    return !finished;
                }

                @Override
                public Segment next() {
                    if (finished) throw new NoSuchElementException();
                    finished = true;
                    return ChildPath.this.child;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        final Iterator<Segment> parentIterator = parent.iterator();
        return new Iterator<Segment>() {
            boolean finished = false;

            @Override
            public boolean hasNext() {
                return parentIterator.hasNext() || !finished;
            }

            @Override
            public Segment next() {
                if (parentIterator.hasNext()) return parentIterator.next();
                if (finished) throw new NoSuchElementException();
                finished = true;
                return ChildPath.this.child;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Path subpath( int beginIndex,
                         int endIndex ) {
        if (beginIndex == 0 && endIndex == (size - 1)) return parent;
        return super.subpath(beginIndex, endIndex);
    }

}

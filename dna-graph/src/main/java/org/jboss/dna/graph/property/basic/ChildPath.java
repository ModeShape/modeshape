/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jboss.dna.common.collection.ImmutableAppendedList;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Path;

/**
 * Implementation of a {@link Path} that has the information for the last segment but that points to another Path for the parent
 * information.
 * 
 * @author Randall Hauch
 */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#getAncestor(int)
     */
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) return this;
        if (degree == 1) return parent;
        return parent.getAncestor(degree - 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#getLastSegment()
     */
    @Override
    public Segment getLastSegment() {
        return child;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#getParent()
     */
    @Override
    public Path getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#getSegment(int)
     */
    @Override
    public Segment getSegment( int index ) {
        if (index == (size - 1)) return child;
        return parent.getSegment(index - 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#getSegmentsList()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#hasSameAncestor(org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        if (parent.equals(that.getParent())) return true;
        return super.hasSameAncestor(that);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#isAbsolute()
     */
    public boolean isAbsolute() {
        return parent.isAbsolute();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#isAtOrBelow(org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean isAtOrBelow( Path other ) {
        if (this == other || parent == other) return true;
        return super.isAtOrBelow(other);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#isDecendantOf(org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean isDecendantOf( Path ancestor ) {
        if (parent == ancestor) return true; // same instance
        return parent.isAtOrBelow(ancestor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#isNormalized()
     */
    public boolean isNormalized() {
        if (child.isParentReference() || child.isSelfReference()) return false;
        return parent.isNormalized();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#isRoot()
     */
    public boolean isRoot() {
        if (child.isParentReference()) return parent.isRoot();
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#iterator()
     */
    @Override
    @SuppressWarnings( "synthetic-access" )
    public Iterator<Segment> iterator() {
        if (parent.isRoot()) {
            return new Iterator<Segment>() {
                boolean finished = false;

                public boolean hasNext() {
                    return !finished;
                }

                public Segment next() {
                    if (finished) throw new NoSuchElementException();
                    finished = true;
                    return ChildPath.this.child;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        final Iterator<Segment> parentIterator = parent.iterator();
        return new Iterator<Segment>() {
            boolean finished = false;

            public boolean hasNext() {
                return parentIterator.hasNext() || !finished;
            }

            public Segment next() {
                if (parentIterator.hasNext()) return parentIterator.next();
                if (finished) throw new NoSuchElementException();
                finished = true;
                return ChildPath.this.child;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#size()
     */
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.Path#subpath(int, int)
     */
    @Override
    public Path subpath( int beginIndex,
                         int endIndex ) {
        if (beginIndex == 0 && endIndex == (size - 1)) return parent;
        return super.subpath(beginIndex, endIndex);
    }

}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * An abstract foundation for different {@link Path} implementations. This class does not manage any of the {@link Path}'s state,
 * but it does provide implementations for most of the methods based upon a few abstract methods. For example, any implementaton
 * that requires the {@link Path.Segment path's segments} are written to use the {@link #iterator()}, since that is likely more
 * efficient for the majority of implementations.
 */
@Immutable
public abstract class AbstractPath implements Path {

    /**
     * The initial serializable version. Version {@value}
     */
    private static final long serialVersionUID = 1L;

    public static final Path SELF_PATH = new BasicPath(Collections.singletonList(Path.SELF_SEGMENT), false);

    protected static Iterator<Path.Segment> EMPTY_PATH_ITERATOR = new Iterator<Segment>() {
        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Segment next() {
            throw new NoSuchElementException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    @NotThreadSafe
    protected static class SingleIterator<T> implements Iterator<T> {
        private T value;

        protected SingleIterator( T value ) {
            this.value = value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return value != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public T next() {
            if (value == null) throw new NoSuchElementException();
            T next = value;
            value = null;
            return next;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private transient int hc = 0;

    protected boolean isNormalized( List<Segment> segments ) {
        boolean nonParentReference = false;
        boolean first = isAbsolute(); // only care about first one when it's absolute
        for (Segment segment : segments) {
            if (segment.isSelfReference()) return false;
            if (segment.isParentReference()) {
                if (nonParentReference || first) return false;
            } else {
                nonParentReference = true;
            }
            first = false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isIdentifier()
     */
    public boolean isIdentifier() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getCanonicalPath()
     */
    public Path getCanonicalPath() {
        if (!this.isAbsolute()) {
            String msg = GraphI18n.pathIsNotAbsolute.text(this);
            throw new InvalidPathException(msg);
        }
        if (this.isNormalized()) return this;
        return this.getNormalizedPath();
    }

    /**
     * {@inheritDoc}
     */
    public Path getCommonAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        if (that.isRoot()) return that;
        Path normalizedPath = this.getNormalizedPath();
        int lastIndex = 0;
        Iterator<Segment> thisIter = normalizedPath.iterator();
        Iterator<Segment> thatIter = that.getNormalizedPath().iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            Segment thisSeg = thisIter.next();
            Segment thatSeg = thatIter.next();
            if (thisSeg.equals(thatSeg)) {
                ++lastIndex;
            } else {
                break;
            }
        }
        if (lastIndex == 0) return RootPath.INSTANCE;
        return normalizedPath.subpath(0, lastIndex);
    }

    /**
     * {@inheritDoc}
     */
    public Path.Segment getLastSegment() {
        return this.getSegmentsList().get(size() - 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#endsWith(org.modeshape.graph.property.Name)
     */
    public boolean endsWith( Name nameOfLastSegment ) {
        Segment segment = getLastSegment();
        return segment != null && segment.getName().equals(nameOfLastSegment) && !segment.hasIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#endsWith(org.modeshape.graph.property.Name, int)
     */
    public boolean endsWith( Name nameOfLastSegment,
                             int snsIndex ) {
        Segment segment = getLastSegment();
        return segment != null && segment.getName().equals(nameOfLastSegment) && segment.getIndex() == snsIndex;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getParent()
     */
    public Path getParent() {
        return getAncestor(1);
    }

    /**
     * {@inheritDoc}
     */
    public Segment getSegment( int index ) {
        CheckArg.isNonNegative(index, "index");
        return this.getSegmentsList().get(index);
    }

    /**
     * {@inheritDoc}
     */
    public Segment[] getSegmentsArray() {
        // By default, make a new array every time since arrays are mutable, and use the iterator
        // since that is probably more efficient than creating a list ...
        Segment[] result = new Path.Segment[size()];
        int i = 0;
        for (Segment segment : this) {
            result[i] = segment;
            ++i;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Path getNormalizedPath() {
        if (this.isNormalized()) return this;
        LinkedList<Segment> newSegments = new LinkedList<Segment>();
        for (Segment segment : this) {
            if (segment.isSelfReference()) continue;
            if (segment.isParentReference()) {
                if (newSegments.isEmpty()) {
                    if (this.isAbsolute()) {
                        throw new InvalidPathException(CommonI18n.pathCannotBeNormalized.text(this));
                    }
                } else if (!newSegments.getLast().isParentReference()) {
                    newSegments.removeLast();
                    continue;
                }
            }
            newSegments.add(segment);
        }
        if (newSegments.isEmpty()) {
            if (this.isAbsolute()) return RootPath.INSTANCE;
            // Otherwise relative and it had contained nothing but self references ...
            return SELF_PATH;
        }
        return new BasicPath(newSegments, this.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return doGetString(null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        return doGetString(null, encoder, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return doGetString(namespaceRegistry, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return doGetString(namespaceRegistry, encoder, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder, org.modeshape.common.text.TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        return doGetString(namespaceRegistry, encoder, delimiterEncoder);
    }

    /**
     * Method that creates the string representation. This method works two different ways depending upon whether the namespace
     * registry is provided.
     * 
     * @param namespaceRegistry
     * @param encoder
     * @param delimiterEncoder
     * @return this path as a string
     */
    protected String doGetString( NamespaceRegistry namespaceRegistry,
                                  TextEncoder encoder,
                                  TextEncoder delimiterEncoder ) {
        if (encoder == null) encoder = DEFAULT_ENCODER;
        final String delimiter = delimiterEncoder != null ? delimiterEncoder.encode(DELIMITER_STR) : DELIMITER_STR;

        // Since the segments are immutable, this code need not be synchronized because concurrent threads
        // may just compute the same value (with no harm done)
        StringBuilder sb = new StringBuilder();
        if (this.isAbsolute()) sb.append(delimiter);
        boolean first = true;
        for (Segment segment : this) {
            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            assert segment != null;
            sb.append(segment.getString(namespaceRegistry, encoder, delimiterEncoder));
        }
        String result = sb.toString();
        // Save the result to the internal string if this the default encoder is used.
        // This is not synchronized, but it's okay
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        if (that.size() != this.size()) return false;
        if (this.size() == 1) return true; // both nodes are just under the root
        for (int i = this.size() - 2; i >= 0; --i) {
            Path.Segment thisSegment = this.getSegment(i);
            Path.Segment thatSegment = that.getSegment(i);
            if (!thisSegment.equals(thatSegment)) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAncestorOf( Path decendant ) {
        CheckArg.isNotNull(decendant, "that");
        if (this == decendant) return false;
        if (this.size() >= decendant.size()) return false;

        Iterator<Path.Segment> thisIter = this.iterator();
        Iterator<Path.Segment> thatIter = decendant.iterator();
        while (thisIter.hasNext()) {
            Path.Segment thisSeg = thisIter.next();
            Path.Segment thatSeg = thatIter.next();
            if (!thisSeg.equals(thatSeg)) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAtOrBelow(org.modeshape.graph.property.Path)
     */
    public boolean isAtOrBelow( Path other ) {
        CheckArg.isNotNull(other, "other");
        if (this == other) return true;
        if (other.isRoot()) return true;
        if (other.size() > this.size()) return false;
        Iterator<Segment> thisIter = iterator();
        Iterator<Segment> thatIter = other.iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            if (!thisIter.next().equals(thatIter.next())) return false;
        }
        if (thatIter.hasNext()) return false; // The other still has segments, but this doesn't
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAtOrAbove(org.modeshape.graph.property.Path)
     */
    public boolean isAtOrAbove( Path other ) {
        CheckArg.isNotNull(other, "other");
        if (this == other) return true;
        if (this.size() > other.size()) return false;
        Iterator<Segment> thisIter = iterator();
        Iterator<Segment> thatIter = other.iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            if (!thisIter.next().equals(thatIter.next())) return false;
        }
        if (thisIter.hasNext()) return false; // This still has segments, but other doesn't
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDecendantOf( Path ancestor ) {
        CheckArg.isNotNull(ancestor, "ancestor");
        return ancestor.isAncestorOf(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameAs( Path other ) {
        return other != null && this.compareTo(other) == 0;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Segment> iterator() {
        return getSegmentsList().iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#pathsFromRoot()
     */
    public Iterator<Path> pathsFromRoot() {
        LinkedList<Path> paths = new LinkedList<Path>();
        Path path = this;
        while (path != null) {
            paths.addFirst(path);
            if (path.isRoot()) break;
            path = path.getParent();
        }
        return paths.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#relativeToRoot()
     */
    public Path relativeToRoot() {
        return new BasicPath(getSegmentsList(), false);
    }

    /**
     * {@inheritDoc}
     */
    public Path relativeTo( Path startingPath ) {
        CheckArg.isNotNull(startingPath, "to");
        if (!this.isAbsolute()) {
            String msg = GraphI18n.pathIsNotAbsolute.text(this);
            throw new InvalidPathException(msg);
        }
        if (startingPath.isRoot()) {
            // We just want a relative path containing the same segments ...
            return relativeToRoot();
        }
        if (!startingPath.isAbsolute()) {
            String msg = GraphI18n.pathIsNotAbsolute.text(startingPath);
            throw new InvalidPathException(msg);
        }

        // Count the number of segments up to the common ancestor (relative path is what remains) ...
        int lengthOfCommonAncestor = 0;
        Iterator<Segment> thisIter = this.getNormalizedPath().iterator();
        Iterator<Segment> toIter = startingPath.getNormalizedPath().iterator();
        while (thisIter.hasNext() && toIter.hasNext()) {
            Segment thisSeg = thisIter.next();
            Segment toSeg = toIter.next();
            if (thisSeg.equals(toSeg)) {
                ++lengthOfCommonAncestor;
            } else {
                break;
            }
        }
        // Create the relative path, starting with parent references to the common ancestor ...
        int numberOfParentReferences = startingPath.size() - lengthOfCommonAncestor;
        List<Segment> relativeSegments = new ArrayList<Segment>();
        for (int i = 0; i != numberOfParentReferences; ++i) {
            relativeSegments.add(Path.PARENT_SEGMENT);
        }
        // Add the segments of this path from the common ancestor ...
        for (int i = lengthOfCommonAncestor; i < this.size(); ++i) {
            relativeSegments.add(getSegment(i));
        }
        if (relativeSegments.isEmpty()) {
            relativeSegments.add(Path.SELF_SEGMENT);
        }
        return new BasicPath(relativeSegments, false);
    }

    /**
     * {@inheritDoc}
     */
    public Path resolve( Path relativePath ) {
        CheckArg.isNotNull(relativePath, "relative path");
        if (!this.isAbsolute()) {
            String msg = GraphI18n.pathIsNotAbsolute.text(this);
            throw new InvalidPathException(msg);
        }
        if (relativePath.isAbsolute()) {
            String msg = GraphI18n.pathIsNotRelative.text(relativePath);
            throw new InvalidPathException(msg);
        }
        // If the relative path is the self or parent reference ...
        relativePath = relativePath.getNormalizedPath();
        if (relativePath.size() == 1) {
            Segment onlySegment = relativePath.getSegment(0);
            if (onlySegment.isSelfReference()) return this;
            if (onlySegment.isParentReference()) return this.getParent();
        }
        List<Segment> segments = new ArrayList<Segment>(this.size() + relativePath.size());
        for (Segment segment : this) {
            segments.add(segment);
        }
        for (Segment segment : relativePath) {
            segments.add(segment);
        }
        return new BasicPath(segments, true).getNormalizedPath();
    }

    /**
     * {@inheritDoc}
     */
    public Path resolveAgainst( Path absolutePath ) {
        CheckArg.isNotNull(absolutePath, "absolute path");
        return absolutePath.resolve(this);
    }

    /**
     * {@inheritDoc}
     */
    public Path subpath( int beginIndex ) {
        return subpath(beginIndex, size());
    }

    /**
     * {@inheritDoc}
     */
    public Path subpath( int beginIndex,
                         int endIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        CheckArg.isNonNegative(endIndex, "endIndex");
        int size = size();
        if (beginIndex == 0) {
            if (endIndex == 0) return RootPath.INSTANCE;
            if (endIndex == size) return this;
        }
        if (beginIndex >= size) {
            throw new IndexOutOfBoundsException(
                                                GraphI18n.unableToCreateSubpathBeginIndexGreaterThanOrEqualToSize.text(beginIndex,
                                                                                                                       size));
        }
        if (beginIndex > endIndex) {
            throw new IndexOutOfBoundsException(
                                                GraphI18n.unableToCreateSubpathBeginIndexGreaterThanOrEqualToEndingIndex.text(beginIndex,
                                                                                                                              endIndex));
        }
        // This reuses the same list, so it's pretty efficient ...
        return new BasicPath(createSegmentsSubList(beginIndex, endIndex), this.isAbsolute());
    }

    protected List<Segment> createSegmentsSubList( int validBeginIndex,
                                                   int validEndIndex ) {
        return this.getSegmentsList().subList(validBeginIndex, validEndIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (hc == 0) {
            int hashCode = 1;
            for (Segment segment : this) {
                hashCode = 31 * hashCode + segment.hashCode();
            }
            hc = hashCode;
        }
        return hc;
    }

    /**
     * Method used by {@link AbstractPath#equals(Object)} implementation to quickly get an Iterator over the segments in the
     * parent.
     * 
     * @return the iterator over the segments; never null, but may not have any elements
     */
    protected abstract Iterator<Segment> getSegmentsOfParent();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path) {
            Path that = (Path)obj;
            // First check whether the paths are roots ...
            if (this.isRoot()) return that.isRoot();
            else if (that.isRoot()) return false;
            // Now check the hash code and size ...
            if (this.hashCode() != that.hashCode()) return false;
            if (this.size() != that.size()) return false;
            // Check the last segments, since these will often differ anyway ...
            if (!this.getLastSegment().equals(that.getLastSegment())) return false;
            if (this.size() == 1) return true;
            // Check the rest of the names ...
            Iterator<Segment> thisIter = that instanceof AbstractPath ? this.getSegmentsOfParent() : this.iterator();
            Iterator<Segment> thatIter = that instanceof AbstractPath ? ((AbstractPath)that).getSegmentsOfParent() : that.iterator();
            while (thisIter.hasNext()) {
                Segment thisSegment = thisIter.next();
                Segment thatSegment = thatIter.next();
                if (!thisSegment.equals(thatSegment)) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Path that ) {
        if (this == that) return 0;
        Iterator<Segment> thisIter = getSegmentsList().iterator();
        Iterator<Segment> thatIter = that.iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            Segment thisSegment = thisIter.next();
            Segment thatSegment = thatIter.next();
            int diff = thisSegment.compareTo(thatSegment);
            if (diff != 0) return diff;
        }
        if (thisIter.hasNext()) return 1;
        if (thatIter.hasNext()) return -1;
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getString(Path.NO_OP_ENCODER);
    }
}

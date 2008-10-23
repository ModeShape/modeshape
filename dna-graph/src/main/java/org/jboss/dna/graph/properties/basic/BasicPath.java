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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.properties.InvalidPathException;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;

/**
 * A basic implementation of {@link Path}.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class BasicPath implements Path {

    /**
     */
    private static final long serialVersionUID = 8488295345524209746L;

    private static final List<Segment> EMPTY_SEGMENTS = Collections.emptyList();

    public static final Path ROOT = new BasicPath(EMPTY_SEGMENTS, true);

    public static final Path SELF_PATH = new BasicPath(Collections.singletonList(Path.SELF_SEGMENT), false);

    private final List<Segment> segments;
    private final boolean absolute;
    private final boolean normalized;
    private transient String path;

    /**
     * @param segments the segments
     * @param absolute true if this path is absolute, or false otherwise
     */
    public BasicPath( List<Segment> segments,
                      boolean absolute ) {
        CheckArg.isNotNull(segments, "segments");
        this.segments = segments.isEmpty() ? EMPTY_SEGMENTS : Collections.unmodifiableList(segments);
        this.absolute = absolute;
        this.normalized = isNormalized(this.segments);
    }

    protected boolean isNormalized( List<Segment> segments ) {
        for (Segment segment : segments) {
            if (segment.isSelfReference() || segment.isParentReference()) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Path getParent() {
        if (this.isRoot()) return null;
        if (this.segments.size() == 1) return ROOT;
        return subpath(0, this.segments.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) return this;
        if (this.isRoot()) return null;
        int endIndex = this.segments.size() - degree;
        if (endIndex < 0) {
            String msg = GraphI18n.pathAncestorDegreeIsInvalid.text(this.getString(), Inflector.getInstance().ordinalize(degree));
            throw new InvalidPathException(msg);
        }
        return subpath(0, endIndex);
    }

    /**
     * {@inheritDoc}
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
        if (that == null) return null;
        if (this.isRoot() || that.isRoot()) return ROOT;
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
        if (lastIndex == 0) return ROOT;
        return normalizedPath.subpath(0, lastIndex);
    }

    /**
     * {@inheritDoc}
     */
    public Path.Segment getLastSegment() {
        if (this.isRoot()) return null;
        return this.segments.get(size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public Path getNormalizedPath() {
        if (this.isNormalized()) return this; // ROOT is normalized already
        LinkedList<Segment> newSegments = new LinkedList<Segment>();
        for (Segment segment : segments) {
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
            if (this.isAbsolute()) return ROOT;
            // Otherwise relative and it had contained nothing but self references ...
            return SELF_PATH;
        }
        return new BasicPath(newSegments, this.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public Segment getSegment( int index ) {
        return this.segments.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public Segment[] getSegmentsArray() {
        return this.segments.toArray(new Path.Segment[this.segments.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public List<Segment> getSegmentsList() {
        return this.segments;
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return doGetString(null, DEFAULT_ENCODER);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        return doGetString(null, encoder);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return doGetString(namespaceRegistry, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return doGetString(namespaceRegistry, encoder);
    }

    /**
     * Method that creates the string representation. This method works two different ways depending upon whether the namespace
     * registry is provided.
     * 
     * @param namespaceRegistry
     * @param encoder
     * @return this path as a string
     */
    protected String doGetString( NamespaceRegistry namespaceRegistry,
                                  TextEncoder encoder ) {
        if (encoder == null) encoder = DEFAULT_ENCODER;
        if (encoder == DEFAULT_ENCODER && this.path != null) return this.path;

        // Since the segments are immutable, this code need not be synchronized because concurrent threads
        // may just compute the same value (with no harm done)
        StringBuilder sb = new StringBuilder();
        if (this.isAbsolute()) sb.append(DELIMITER);
        boolean first = true;
        for (Segment segment : this.segments) {
            if (first) {
                first = false;
            } else {
                sb.append(DELIMITER);
            }
            assert segment != null;
            if (namespaceRegistry != null) {
                sb.append(segment.getString(namespaceRegistry, encoder));
            } else {
                sb.append(segment.getString(encoder));
            }
        }
        String result = sb.toString();
        // Save the result to the internal string if this the default encoder is used.
        // This is not synchronized, but it's okay
        if (encoder == DEFAULT_ENCODER && this.path == null) this.path = result;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasSameAncestor( Path that ) {
        if (that == null) return false;
        if (this.isRoot() && that.isRoot()) return true;
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
    public boolean isAbsolute() {
        return this.absolute;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAncestorOf( Path decendant ) {
        if (decendant == null) return false;
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
     * @see org.jboss.dna.graph.properties.Path#isAtOrBelow(org.jboss.dna.graph.properties.Path)
     */
    public boolean isAtOrBelow( Path other ) {
        if (other == null) return false;
        if (this == other) return true;
        Iterator<Segment> thisIter = this.segments.iterator();
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
     * @see org.jboss.dna.graph.properties.Path#isAtOrAbove(org.jboss.dna.graph.properties.Path)
     */
    public boolean isAtOrAbove( Path other ) {
        if (other == null) return false;
        if (this == other) return true;
        Iterator<Segment> thisIter = this.segments.iterator();
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
        if (ancestor == null) return false;
        return ancestor.isAncestorOf(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNormalized() {
        return this.normalized;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRoot() {
        return this == ROOT || this.segments.isEmpty();
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
        return this.segments.iterator();
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
            relativeSegments.add(this.segments.get(i));
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
            String msg = GraphI18n.pathIsAlreadyAbsolute.text(this.path);
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
        segments.addAll(this.segments);
        segments.addAll(relativePath.getSegmentsList());
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
    public int size() {
        return this.segments.size();
    }

    /**
     * {@inheritDoc}
     */
    public Path subpath( int beginIndex ) {
        if (beginIndex == 0) return this;
        int size = size();
        if (beginIndex >= size) {
            throw new IndexOutOfBoundsException(
                                                GraphI18n.unableToCreateSubpathBeginIndexGreaterThanOrEqualToSize.text(beginIndex,
                                                                                                                       size));
        }
        if (size == 0) return ROOT;
        return new BasicPath(this.segments.subList(beginIndex, size), this.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public Path subpath( int beginIndex,
                         int endIndex ) {
        int size = size();
        if (beginIndex == 0) {
            if (endIndex == 0) return ROOT;
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
        return new BasicPath(this.segments.subList(beginIndex, endIndex), this.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.segments.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path) {
            Path that = (Path)obj;
            return this.segments.equals(that.getSegmentsList());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Path that ) {
        if (this == that) return 0;
        Iterator<Segment> thisIter = this.segments.iterator();
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
        return getString(Path.URL_ENCODER);
    }

}

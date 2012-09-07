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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.SingleIterator;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;

/**
 * Optimized implementation of {@link Path} that serves as the root path.
 */
@Immutable
public class RootPath extends AbstractPath {

    /**
     * The serializable version. Version {@value}
     */
    private static final long serialVersionUID = 1L;

    public static final Path INSTANCE = new RootPath();

    private static final Path.Segment[] EMPTY_SEGMENT_ARRAY = new Path.Segment[] {};
    private static final List<Path.Segment> EMPTY_SEGMENT_LIST = Collections.emptyList();

    private RootPath() {
    }

    @Override
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) {
            return this;
        }
        String msg = GraphI18n.pathAncestorDegreeIsInvalid.text(this.getString(), Inflector.getInstance().ordinalize(degree));
        throw new InvalidPathException(msg);
    }

    @Override
    protected Iterator<Segment> getSegmentsOfParent() {
        return EMPTY_PATH_ITERATOR;
    }

    @Override
    public Path getCanonicalPath() {
        return this;
    }

    @Override
    public Path getCommonAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return this;
    }

    @Override
    public Segment getLastSegment() {
        return null;
    }

    @Override
    public boolean endsWith( Name nameOfLastSegment ) {
        return false;
    }

    @Override
    public boolean endsWith( Name nameOfLastSegment,
                             int snsIndex ) {
        return false;
    }

    @Override
    public Path getNormalizedPath() {
        return this;
    }

    @Override
    public Path relativeToRoot() {
        return BasicPath.SELF_PATH;
    }

    @Override
    public Path resolve( Path relativePath ) {
        CheckArg.isNotNull(relativePath, "relative path");
        if (relativePath.isAbsolute()) {
            String msg = GraphI18n.pathIsNotRelative.text(relativePath);
            throw new InvalidPathException(msg);
        }
        // Make an absolute path out of the supplied relative path ...
        return new BasicPath(relativePath.getSegmentsList(), true).getNormalizedPath();
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public Segment getSegment( int index ) {
        CheckArg.isNonNegative(index, "index");
        EMPTY_SEGMENT_LIST.get(index); // throws IndexOutOfBoundsException
        return null;
    }

    @Override
    public Segment[] getSegmentsArray() {
        // Can return the same array every time, since it's empty ...
        return EMPTY_SEGMENT_ARRAY;
    }

    @Override
    public List<Segment> getSegmentsList() {
        return EMPTY_SEGMENT_LIST;
    }

    @Override
    public String getString() {
        return Path.DELIMITER_STR;
    }

    @Override
    public String getString( TextEncoder encoder ) {
        return Path.DELIMITER_STR;
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return Path.DELIMITER_STR;
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return Path.DELIMITER_STR;
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        return (delimiterEncoder == null) ? DELIMITER_STR : delimiterEncoder.encode(DELIMITER_STR);
    }

    @Override
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return true;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public boolean isAncestorOf( Path descendant ) {
        CheckArg.isNotNull(descendant, "descendant");
        return !descendant.isRoot();
    }

    @Override
    public boolean isAtOrAbove( Path other ) {
        CheckArg.isNotNull(other, "other");
        return true;
    }

    @Override
    public boolean isAtOrBelow( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isRoot();
    }

    @Override
    public boolean isDescendantOf( Path ancestor ) {
        CheckArg.isNotNull(ancestor, "ancestor");
        return false;
    }

    @Override
    public boolean isNormalized() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public boolean isSameAs( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isRoot();
    }

    @Override
    public Iterator<Segment> iterator() {
        return EMPTY_SEGMENT_LIST.iterator();
    }

    @Override
    public Iterator<Path> pathsFromRoot() {
        return new SingleIterator<Path>(this);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Path subpath( int beginIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        if (beginIndex == 0) return this;
        EMPTY_SEGMENT_LIST.get(1); // throws IndexOutOfBoundsException
        return null;
    }

    @Override
    public Path subpath( int beginIndex,
                         int endIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        CheckArg.isNonNegative(endIndex, "endIndex");
        if (endIndex >= 1) {
            EMPTY_SEGMENT_LIST.get(endIndex); // throws IndexOutOfBoundsException
        }
        return this;
    }

    @Override
    public int compareTo( Path other ) {
        return other.isRoot() ? 0 : -1;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path) {
            Path that = (Path)obj;
            return that.isRoot();
        }
        return false;
    }

}

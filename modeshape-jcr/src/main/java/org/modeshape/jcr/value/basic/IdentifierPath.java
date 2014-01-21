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
 * Optimized implementation of {@link Path} that serves as a JCR identifier path.
 */
@Immutable
public class IdentifierPath extends AbstractPath {

    /**
     * The serializable version. Version {@value}
     */
    private static final long serialVersionUID = 1L;

    private final IdentifierPathSegment idSegment;
    private transient List<Segment> segments;

    public IdentifierPath( IdentifierPathSegment idSegment ) {
        this.idSegment = idSegment;
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
        return new SingleIterator<Segment>(idSegment);
    }

    @Override
    public Path getCanonicalPath() {
        return this;
    }

    @Override
    public Path getCommonAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return this.equals(that) ? this : RootPath.INSTANCE;
    }

    @Override
    public Segment getLastSegment() {
        return idSegment;
    }

    @Override
    public boolean endsWith( Name nameOfLastSegment ) {
        return idSegment.getName().equals(nameOfLastSegment);
    }

    @Override
    public boolean endsWith( Name nameOfLastSegment,
                             int snsIndex ) {
        return snsIndex == 1 && idSegment.getName().equals(nameOfLastSegment);
    }

    @Override
    public Path getNormalizedPath() {
        return this; // already normalized
    }

    @Override
    public Path relativeToRoot() {
        return new BasicPath(getSegmentsList(), false);
    }

    @Override
    public Path getParent() {
        return RootPath.INSTANCE;
    }

    @Override
    public Segment getSegment( int index ) {
        CheckArg.isNonNegative(index, "index");
        if (index == 0) return idSegment;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Segment[] getSegmentsArray() {
        return new Segment[] {idSegment};
    }

    @Override
    public List<Segment> getSegmentsList() {
        if (segments == null) {
            // Idempotent, so can do this without locking ...
            segments = Collections.singletonList((Segment)idSegment);
        }
        return segments;
    }

    @Override
    public String getString() {
        return idSegment.getString();
    }

    @Override
    public String getString( TextEncoder encoder ) {
        return idSegment.getString(encoder);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return idSegment.getString(namespaceRegistry);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return idSegment.getString(namespaceRegistry, encoder);
    }

    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        return (delimiterEncoder == null) ? getString(namespaceRegistry, encoder) : delimiterEncoder.encode(getString(namespaceRegistry,
                                                                                                                      encoder));
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
    public boolean isIdentifier() {
        return true;
    }

    @Override
    public boolean isNormalized() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isSameAs( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isIdentifier() && idSegment.equals(other.getLastSegment());
    }

    @Override
    public boolean isAncestorOf( Path descendant ) {
        CheckArg.isNotNull(descendant, "descendant");
        return false;
    }

    @Override
    public boolean isDescendantOf( Path ancestor ) {
        CheckArg.isNotNull(ancestor, "ancestor");
        return false;
    }

    @Override
    public boolean isAtOrAbove( Path other ) {
        CheckArg.isNotNull(other, "other");
        return this.isSameAs(other);
    }

    @Override
    public boolean isAtOrBelow( Path other ) {
        CheckArg.isNotNull(other, "other");
        return this.isSameAs(other);
    }

    @Override
    public Path resolve( Path relativePath ) {
        CheckArg.isNotNull(relativePath, "relative path");
        String msg = GraphI18n.unableToResolvePathRelativeToIdentifierPath.text(relativePath, this);
        throw new InvalidPathException(msg);
    }

    @Override
    public Iterator<Segment> iterator() {
        return new SingleIterator<Segment>(idSegment);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Path subpath( int beginIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        if (beginIndex == 0) return this;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Path subpath( int beginIndex,
                         int endIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        CheckArg.isNonNegative(endIndex, "endIndex");
        if (beginIndex != 0) {
            throw new IndexOutOfBoundsException();
        }
        if (endIndex >= 1) {
            throw new IndexOutOfBoundsException();
        }
        return this;
    }

    @Override
    public int compareTo( Path other ) {
        return other.isIdentifier() ? idSegment.compareTo(other.getLastSegment()) : super.compareTo(other);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path) {
            Path that = (Path)obj;
            return that.isIdentifier() && idSegment.equals(that.getLastSegment());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

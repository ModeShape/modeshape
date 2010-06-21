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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * Optimized implementation of {@link Path} that serves as the root path.
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getAncestor(int)
     */
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) {
            return this;
        }
        String msg = GraphI18n.pathAncestorDegreeIsInvalid.text(this.getString(), Inflector.getInstance().ordinalize(degree));
        throw new InvalidPathException(msg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#getSegmentsOfParent()
     */
    @Override
    protected Iterator<Segment> getSegmentsOfParent() {
        return new SingleIterator<Segment>(idSegment);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getCanonicalPath()
     */
    @Override
    public Path getCanonicalPath() {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getCommonAncestor(org.modeshape.graph.property.Path)
     */
    @Override
    public Path getCommonAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return this.equals(that) ? this : RootPath.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getLastSegment()
     */
    @Override
    public Segment getLastSegment() {
        return idSegment;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#endsWith(org.modeshape.graph.property.Name)
     */
    @Override
    public boolean endsWith( Name nameOfLastSegment ) {
        return idSegment.getName().equals(nameOfLastSegment);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#endsWith(org.modeshape.graph.property.Name, int)
     */
    @Override
    public boolean endsWith( Name nameOfLastSegment,
                             int snsIndex ) {
        return snsIndex == 1 && idSegment.getName().equals(nameOfLastSegment);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getNormalizedPath()
     */
    @Override
    public Path getNormalizedPath() {
        return this; // already normalized
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#relativeToRoot()
     */
    @Override
    public Path relativeToRoot() {
        return new BasicPath(getSegmentsList(), false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getParent()
     */
    @Override
    public Path getParent() {
        return RootPath.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getSegment(int)
     */
    @Override
    public Segment getSegment( int index ) {
        CheckArg.isNonNegative(index, "index");
        if (index == 0) return idSegment;
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getSegmentsArray()
     */
    @Override
    public Segment[] getSegmentsArray() {
        return new Segment[] {idSegment};
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getSegmentsList()
     */
    public List<Segment> getSegmentsList() {
        if (segments == null) {
            // Idempotent, so can do this without locking ...
            segments = Collections.singletonList((Segment)idSegment);
        }
        return segments;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString()
     */
    @Override
    public String getString() {
        return idSegment.getString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.common.text.TextEncoder)
     */
    @Override
    public String getString( TextEncoder encoder ) {
        return idSegment.getString(encoder);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.graph.property.NamespaceRegistry)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return idSegment.getString(namespaceRegistry);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return idSegment.getString(namespaceRegistry, encoder);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#getString(org.modeshape.graph.property.NamespaceRegistry,
     *      org.modeshape.common.text.TextEncoder, org.modeshape.common.text.TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        return (delimiterEncoder == null) ? getString(namespaceRegistry, encoder) : delimiterEncoder.encode(getString(namespaceRegistry,
                                                                                                                      encoder));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#hasSameAncestor(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAbsolute()
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#isIdentifier()
     */
    @Override
    public boolean isIdentifier() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isNormalized()
     */
    public boolean isNormalized() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isRoot()
     */
    public boolean isRoot() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isSameAs(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean isSameAs( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isIdentifier() && idSegment.equals(other.getLastSegment());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAncestorOf(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean isAncestorOf( Path decendant ) {
        CheckArg.isNotNull(decendant, "decendant");
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isDecendantOf(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean isDecendantOf( Path ancestor ) {
        CheckArg.isNotNull(ancestor, "ancestor");
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAtOrAbove(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean isAtOrAbove( Path other ) {
        CheckArg.isNotNull(other, "other");
        return this.isSameAs(other);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#isAtOrBelow(org.modeshape.graph.property.Path)
     */
    @Override
    public boolean isAtOrBelow( Path other ) {
        CheckArg.isNotNull(other, "other");
        return this.isSameAs(other);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractPath#resolve(org.modeshape.graph.property.Path)
     */
    @Override
    public Path resolve( Path relativePath ) {
        CheckArg.isNotNull(relativePath, "relative path");
        String msg = GraphI18n.unableToResolvePathRelativeToIdentifierPath.text(relativePath, this);
        throw new InvalidPathException(msg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#iterator()
     */
    @Override
    public Iterator<Segment> iterator() {
        return new SingleIterator<Segment>(idSegment);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#size()
     */
    public int size() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#subpath(int)
     */
    @Override
    public Path subpath( int beginIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        if (beginIndex == 0) return this;
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.Path#subpath(int, int)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( Path other ) {
        return other.isIdentifier() ? idSegment.compareTo(other.getLastSegment()) : super.compareTo(other);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Path) {
            Path that = (Path)obj;
            return that.isIdentifier() && idSegment.equals(that.getLastSegment());
        }
        return false;
    }

}

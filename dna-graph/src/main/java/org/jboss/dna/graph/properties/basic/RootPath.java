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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.properties.InvalidPathException;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;

/**
 * Optimized implementation of {@link Path} that serves as the root path.
 * 
 * @author Randall Hauch
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getAncestor(int)
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
     * @see org.jboss.dna.graph.properties.Path#getCanonicalPath()
     */
    @Override
    public Path getCanonicalPath() {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getCommonAncestor(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public Path getCommonAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getLastSegment()
     */
    @Override
    public Segment getLastSegment() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getNormalizedPath()
     */
    @Override
    public Path getNormalizedPath() {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.basic.AbstractPath#resolve(org.jboss.dna.graph.properties.Path)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getParent()
     */
    @Override
    public Path getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getSegment(int)
     */
    @Override
    public Segment getSegment( int index ) {
        CheckArg.isNonNegative(index, "index");
        EMPTY_SEGMENT_LIST.get(index); // throws IndexOutOfBoundsException
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getSegmentsArray()
     */
    @Override
    public Segment[] getSegmentsArray() {
        // Can return the same array every time, since it's empty ...
        return EMPTY_SEGMENT_ARRAY;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getSegmentsList()
     */
    public List<Segment> getSegmentsList() {
        return EMPTY_SEGMENT_LIST;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getString()
     */
    @Override
    public String getString() {
        return Path.DELIMITER_STR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getString(org.jboss.dna.common.text.TextEncoder)
     */
    @Override
    public String getString( TextEncoder encoder ) {
        return Path.DELIMITER_STR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getString(org.jboss.dna.graph.properties.NamespaceRegistry)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return Path.DELIMITER_STR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getString(org.jboss.dna.graph.properties.NamespaceRegistry,
     *      org.jboss.dna.common.text.TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        return Path.DELIMITER_STR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#getString(org.jboss.dna.graph.properties.NamespaceRegistry,
     *      org.jboss.dna.common.text.TextEncoder, org.jboss.dna.common.text.TextEncoder)
     */
    @Override
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        return (delimiterEncoder == null) ? DELIMITER_STR : delimiterEncoder.encode(DELIMITER_STR);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#hasSameAncestor(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean hasSameAncestor( Path that ) {
        CheckArg.isNotNull(that, "that");
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isAbsolute()
     */
    public boolean isAbsolute() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isAncestorOf(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean isAncestorOf( Path decendant ) {
        CheckArg.isNotNull(decendant, "decendant");
        return !decendant.isRoot();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isAtOrAbove(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean isAtOrAbove( Path other ) {
        CheckArg.isNotNull(other, "other");
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isAtOrBelow(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean isAtOrBelow( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isRoot();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isDecendantOf(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean isDecendantOf( Path ancestor ) {
        CheckArg.isNotNull(ancestor, "ancestor");
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isNormalized()
     */
    public boolean isNormalized() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isRoot()
     */
    public boolean isRoot() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#isSameAs(org.jboss.dna.graph.properties.Path)
     */
    @Override
    public boolean isSameAs( Path other ) {
        CheckArg.isNotNull(other, "other");
        return other.isRoot();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#iterator()
     */
    @Override
    public Iterator<Segment> iterator() {
        return EMPTY_SEGMENT_LIST.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#size()
     */
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#subpath(int)
     */
    @Override
    public Path subpath( int beginIndex ) {
        CheckArg.isNonNegative(beginIndex, "beginIndex");
        if (beginIndex == 0) return this;
        EMPTY_SEGMENT_LIST.get(1); // throws IndexOutOfBoundsException
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Path#subpath(int, int)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( Path other ) {
        return other.isRoot() ? 0 : -1;
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
            return that.isRoot();
        }
        return false;
    }

}

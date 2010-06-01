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
package org.modeshape.graph.property;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.graph.property.basic.BasicName;
import org.modeshape.graph.property.basic.BasicPathSegment;

/**
 * An object representation of a node path within a repository.
 * <p>
 * A path consists of zero or more segments that can contain any characters, although the string representation may require some
 * characters to be encoded. For example, if a path contains a segment with a forward slash, then this forward slash must be
 * escaped when writing the whole path to a string (since a forward slash is used as the {@link #DELIMITER delimiter} between
 * segments).
 * </p>
 * <p>
 * Because of this encoding and decoding issue, there is no standard representation of a path as a string. Instead, this class
 * uses {@link TextEncoder text encoders} to escape certain characters when writing to a string or unescaping the string
 * representation. These encoders and used only with individual segments, and therefore are not used to encode the
 * {@link #DELIMITER delimiter}. Three standard encoders are provided, although others can certainly be used:
 * <ul>
 * <li>{@link #JSR283_ENCODER Jsr283Encoder} - an encoder and decoder that is compliant with <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a> by converting the reserved characters (namely '*', '/', ':', '[', ']'
 * and '|') to their unicode equivalent.</td>
 * </li>
 * <li>{@link #URL_ENCODER UrlEncoder} - an encoder and decoder that is useful for converting text to be used within a URL, as
 * defined by Section 2.3 of <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>. This encoder does encode many characters
 * (including '`', '@', '#', '$', '^', '&', '{', '[', '}', ']', '|', ':', ';', '\', '"', '<', ',', '>', '?', '/', and ' '), while
 * others are not encoded (including '-', '_', '.', '!', '~', '*', '\', ''', '(', and ')'). Note that only the '*' character is
 * the only character reserved by JSR-283 that is not encoded by the URL encoder.</li>
 * <li>{@link #NO_OP_ENCODER NoOpEncoder} - an {@link TextEncoder encoder} implementation that does nothing.</li>
 * </ul>
 * </p>
 * <p>
 * This class simplifies working with paths and using a <code>Path</code> is often more efficient that processing and
 * manipulating the equivalent <code>String</code>. This class can easily {@link #iterator() iterate} over the segments, return
 * the {@link #size() number of segments}, {@link #compareTo(Path) compare} with other paths, {@link #resolve(Path) resolve}
 * relative paths, return the {@link #getParent() ancestor (or parent)}, determine whether one path is an
 * {@link #isAncestorOf(Path) ancestor} or {@link #isDecendantOf(Path) decendent} of another path, and
 * {@link #getCommonAncestor(Path) finding a common ancestor}.
 * </p>
 */
@Immutable
public interface Path extends Comparable<Path>, Iterable<Path.Segment>, Serializable, Readable {

    /**
     * The text encoder that does nothing.
     */
    public static final TextEncoder NO_OP_ENCODER = new NoOpEncoder();

    /**
     * The text encoder that encodes according to JSR-283.
     */
    public static final TextEncoder JSR283_ENCODER = new Jsr283Encoder();

    /**
     * The text encoder that encodes text according to the rules of <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
     */
    public static final TextEncoder URL_ENCODER = new UrlEncoder().setSlashEncoded(true);

    /**
     * The text decoder that does nothing.
     */
    public static final TextDecoder NO_OP_DECODER = new NoOpEncoder();

    /**
     * The text decoder that decodes according to JSR-283.
     */
    public static final TextDecoder JSR283_DECODER = new Jsr283Encoder();

    /**
     * The text decoder that decodes text according to the rules of <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
     */
    public static final TextDecoder URL_DECODER = new UrlEncoder().setSlashEncoded(true);

    /**
     * The default text encoder to be used when none is otherwise specified. This is currently the {@link #JSR283_ENCODER JSR-283
     * encoder}.
     */
    public static final TextEncoder DEFAULT_ENCODER = JSR283_ENCODER;

    /**
     * The default text decoder to be used when none is otherwise specified. This is currently the {@link #JSR283_ENCODER JSR-283
     * encoder}.
     */
    public static final TextDecoder DEFAULT_DECODER = JSR283_DECODER;

    /**
     * The delimiter character used to separate segments within a path.
     */
    public static final char DELIMITER = '/';

    /**
     * The character used to begin an identifier segment within a path.
     */
    public static final char IDENTIFIER_LEADING_TERMINAL = '[';

    /**
     * The character used to end an identifier segment within a path.
     */
    public static final char IDENTIFIER_TRAILING_TERMINAL = ']';

    /**
     * String form of the delimiter used to separate segments within a path.
     */
    public static final String DELIMITER_STR = new String(new char[] {DELIMITER});

    /**
     * String representation of the segment that references a parent.
     */
    public static final String PARENT = "..";

    /**
     * String representation of the segment that references the same segment.
     */
    public static final String SELF = ".";

    /**
     * The default index for a {@link Segment}.
     */
    public static final int DEFAULT_INDEX = 1;

    /**
     * Representation of the segments that occur within a path.
     * 
     * @author Randall Hauch
     */
    @Immutable
    public static interface Segment extends Cloneable, Comparable<Segment>, Serializable, Readable {

        /**
         * Get the name component of this segment.
         * 
         * @return the segment's name
         */
        public Name getName();

        /**
         * Get the index for this segment, which will be 1 by default.
         * 
         * @return the index
         */
        public int getIndex();

        /**
         * Return whether this segment has an index.
         * 
         * @return true if this segment has an index, or false otherwise.
         */
        public boolean hasIndex();

        /**
         * Return whether this segment is a self-reference.
         * 
         * @return true if the segment is a self-reference, or false otherwise.
         */
        public boolean isSelfReference();

        /**
         * Return whether this segment is a reference to a parent.
         * 
         * @return true if the segment is a parent-reference, or false otherwise.
         */
        public boolean isParentReference();

        /**
         * Return whether this segment is an identifier segment.
         * 
         * @return true if the segment is an identifier segment, or false otherwise.
         */
        public boolean isIdentifier();

        /**
         * Get the raw string form of the segment using the {@link Path#NO_OP_ENCODER no-op encoder}. This is equivalent to
         * calling <code>getString(Path.NO_OP_ENCODER)</code>.
         * 
         * @return the un-encoded string
         * @see #getString(TextEncoder)
         */
        public String getUnencodedString();
    }

    /**
     * Singleton instance of the name referencing a self, provided as a convenience.
     */
    public static final Name SELF_NAME = new BasicName(null, SELF);

    /**
     * Singleton instance of the name referencing a parent, provided as a convenience.
     */
    public static final Name PARENT_NAME = new BasicName(null, PARENT);

    /**
     * Singleton instance of the path segment referencing a parent, provided as a convenience.
     */
    public static final Path.Segment SELF_SEGMENT = new BasicPathSegment(SELF_NAME);

    /**
     * Singleton instance of the path segment referencing a parent, provided as a convenience.
     */
    public static final Path.Segment PARENT_SEGMENT = new BasicPathSegment(PARENT_NAME);

    /**
     * Return the number of segments in this path.
     * 
     * @return the number of path segments
     */
    public int size();

    /**
     * Return whether this path represents the root path.
     * 
     * @return true if this path is the root path, or false otherwise
     */
    public boolean isRoot();

    /**
     * Returns whether this path represents an identifier path. An identifier path is absolute, and contains a single
     * {@link Segment#isIdentifier() identifier Segment} that contains an identifier in the name.
     * 
     * @return true if this path is an identifier path, or false otherwise.
     */
    public boolean isIdentifier();

    /**
     * Determine whether this path represents the same as the supplied path. This is equivalent to calling <code>
     * this.compareTo(other) == 0 </code>
     * .
     * 
     * @param other the other path to compare with this path; may be null
     * @return true if the paths are equivalent, or false otherwise
     */
    public boolean isSameAs( Path other );

    /**
     * Determine whether this path is the {@link #isSameAs(Path) same as} to or a {@link #isAncestorOf(Path) ancestor of} the
     * supplied path. This method is equivalent to (but may be more efficient than) calling <code>isSame(other) ||
     * isAncestor(other)</code>, and is a convenience method that is identical to calling <code>other.isAtOrBelow(this)</code>.
     * 
     * @param other the other path to compare with this path; may be null
     * @return true if the paths are equivalent or if this path is considered an ancestor of the other path, or false otherwise
     */
    public boolean isAtOrAbove( Path other );

    /**
     * Determine whether this path is the {@link #isSameAs(Path) same as} to or a {@link #isDecendantOf(Path) decendant of} the
     * supplied path. This method is equivalent to (but may be more efficient than) calling <code>isSame(other) ||
     * isAncestor(other)</code>.
     * 
     * @param other the other path to compare with this path; may be null
     * @return true if the paths are equivalent or if this path is considered a decendant of the other path, or false otherwise
     */
    public boolean isAtOrBelow( Path other );

    /**
     * Determine whether this path is an ancestor of the supplied path. A path is considered an ancestor of another path if the
     * the ancestor path appears in its entirety at the beginning of the decendant path, and where the decendant path contains at
     * least one additional segment.
     * 
     * @param decendant the path that may be the decendant; may be null
     * @return true if this path is an ancestor of the supplied path, or false otherwise
     */
    public boolean isAncestorOf( Path decendant );

    /**
     * Determine whether this path is an decendant of the supplied path. A path is considered a decendant of another path if the
     * the decendant path starts exactly with the entire ancestor path but contains at least one additional segment.
     * 
     * @param ancestor the path that may be the ancestor; may be null
     * @return true if this path is an decendant of the supplied path, or false otherwise
     */
    public boolean isDecendantOf( Path ancestor );

    /**
     * Return whether this path is an absolute path. A path is either relative or {@link #isAbsolute() absolute}. An absolute path
     * starts with a "/".
     * 
     * @return true if the path is absolute, or false otherwise
     */
    public boolean isAbsolute();

    /**
     * Return whether this path is normalized and contains no unnecessary "." segments and as few ".." segments as possible. For
     * example, the path "../a" is normalized, while "/a/b/c/../d" is not normalized.
     * 
     * @return true if this path is normalized, or false otherwise
     */
    public boolean isNormalized();

    /**
     * Get a normalized path with as many ".." segments and all "." resolved. The relative path ".", however, will return itself
     * as the normalized path, since it cannot be resolved any further.
     * 
     * @return the normalized path, or this object if this path is already normalized
     * @throws InvalidPathException if the normalized form would result in a path with negative length (e.g., "/a/../../..")
     */
    public Path getNormalizedPath();

    /**
     * Get the canonical form of this path. A canonical path has is {@link #isAbsolute() absolute} and {@link #isNormalized()}.
     * 
     * @return the canonical path, or this object if it is already in its canonical form
     * @throws InvalidPathException if the path is not absolute and cannot be canonicalized
     */
    public Path getCanonicalPath();

    /**
     * Obtain a path that is relative to the root node. This is equivalent to calling {@link #relativeTo(Path)} with the root
     * path.
     * 
     * @return the relative path from the root node; never null
     */
    public Path relativeToRoot();

    /**
     * Get a relative path from the supplied path to this path.
     * 
     * @param startingPath the path specifying the starting point for the new relative path; may not be null
     * @return the relative path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws PathNotFoundException if both this path and the supplied path are not absolute
     */
    public Path relativeTo( Path startingPath );

    /**
     * Get the absolute path by resolving the supplied relative (non-absolute) path against this absolute path.
     * 
     * @param relativePath the relative path that is to be resolved against this path
     * @return the absolute and normalized path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the this path is not absolute or if the supplied path is not relative.
     */
    public Path resolve( Path relativePath );

    /**
     * Get the absolute path by resolving this relative (non-absolute) path against the supplied absolute path.
     * 
     * @param absolutePath the absolute path to which this relative path should be resolve
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the supplied path is not absolute or if this path is not relative.
     */
    public Path resolveAgainst( Path absolutePath );

    /**
     * Return the path to the parent, or this path if it is the {@link #isRoot() root}. This is an efficient operation that does
     * not require copying any data.
     * 
     * @return the parent path, or this null if it is already the root
     */
    public Path getParent();

    /**
     * Return the path to the ancestor of the supplied degree. An ancestor of degree <code>x</code> is the path that is <code>x
     * </code>
     * levels up along the path. For example, <code>degree = 0</code> returns this path, while <code>degree = 1</code> returns the
     * parent of this path, <code>degree = 2</code> returns the grandparent of this path, and so on. Note that the result may be
     * unexpected if this path is not {@link #isNormalized() normalized}, as a non-normalized path contains ".." and "." segments.
     * 
     * @param degree
     * @return the ancestor of the supplied degree
     * @throws IllegalArgumentException if the degree is negative
     * @throws InvalidPathException if the degree is greater than the {@link #size() length} of this path
     */
    public Path getAncestor( int degree );

    /**
     * Determine whether this path and the supplied path have the same immediate ancestor. In other words, this method determines
     * whether the node represented by this path is a sibling of the node represented by the supplied path.
     * 
     * @param that the other path
     * @return true if this path and the supplied path have the same immediate ancestor.
     * @throws IllegalArgumentException if the supplied path is null
     */
    public boolean hasSameAncestor( Path that );

    /**
     * Find the lowest common ancestor of this path and the supplied path.
     * 
     * @param that the other path
     * @return the lowest common ancestor, which may be the root path if there is no other.
     * @throws IllegalArgumentException if the supplied path is null
     */
    public Path getCommonAncestor( Path that );

    /**
     * Get the last segment in this path.
     * 
     * @return the last segment, or null if the path is empty
     */
    public Segment getLastSegment();

    /**
     * Determine if the path's {@link #getLastSegment()} has the supplied name and no {@link Segment#getIndex() SNS index}.
     * 
     * @param nameOfLastSegment the name
     * @return return true if the supplied name is the name in the path's last segment (and there is no SNS index), or false
     *         otherwise
     */
    public boolean endsWith( Name nameOfLastSegment );

    /**
     * Determine if the path's {@link #getLastSegment()} has the supplied name and {@link Segment#getIndex() SNS index}.
     * 
     * @param nameOfLastSegment the name
     * @param snsIndex the SNS index
     * @return return true if the path's last segment has the supplied name and SNS, or false otherwise
     */
    public boolean endsWith( Name nameOfLastSegment,
                             int snsIndex );

    /**
     * Get the segment at the supplied index.
     * 
     * @param index the index
     * @return the segment
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Segment getSegment( int index );

    /**
     * Return a new path consisting of the segments starting at <code>beginIndex</code> index (inclusive). This is equivalent to
     * calling <code>path.subpath(beginIndex,path.size()-1)</code>.
     * 
     * @param beginIndex the beginning index, inclusive.
     * @return the specified subpath
     * @exception IndexOutOfBoundsException if the <code>beginIndex</code> is negative or larger than the length of this <code>
     *            Path</code>
     *            object
     */
    public Path subpath( int beginIndex );

    /**
     * Return a new path consisting of the segments between the <code>beginIndex</code> index (inclusive) and the <code>endIndex
     * </code> index
     * (exclusive).
     * 
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return the specified subpath
     * @exception IndexOutOfBoundsException if the <code>beginIndex</code> is negative, or <code>endIndex</code> is larger than
     *            the length of this <code>Path</code> object, or <code>beginIndex</code> is larger than <code>endIndex</code>.
     */
    public Path subpath( int beginIndex,
                         int endIndex );

    /**
     * {@inheritDoc}
     */
    public Iterator<Segment> iterator();

    /**
     * Return an iterator that walks the paths from the root path down to this path. This method always returns at least one path
     * (the root returns an iterator containing itself).
     * 
     * @return the path iterator; never null
     */
    public Iterator<Path> pathsFromRoot();

    /**
     * Obtain a copy of the segments in this path. None of the segments are encoded.
     * 
     * @return the array of segments as a copy
     */
    public Segment[] getSegmentsArray();

    /**
     * Get an unmodifiable list of the path segments.
     * 
     * @return the unmodifiable list of path segments; never null
     */
    public List<Segment> getSegmentsList();

}

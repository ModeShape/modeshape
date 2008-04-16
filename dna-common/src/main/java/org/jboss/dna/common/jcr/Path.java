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
package org.jboss.dna.common.jcr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.CoreI18n;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.text.Jsr283Encoder;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.text.UrlEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.common.util.StringUtil;

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
 * relative paths, return the {@link #getAncestor() ancestor (or parent)}, determine whether one path is an
 * {@link #isAncestorOf(Path) ancestor} or {@link #isDecendantOf(Path) decendent} of another path, and creating
 * {@link #append(String) subpaths}.
 * </p>
 * @author Randall Hauch
 */
@Immutable
public class Path implements Cloneable, Comparable<Path>, Iterable<Path.Segment>, Serializable {

    public static final TextEncoder NO_OP_ENCODER = new NoOpEncoder();

    public static final TextEncoder JSR283_ENCODER = new Jsr283Encoder();

    public static final TextEncoder URL_ENCODER = new UrlEncoder().setSlashEncoded(true);

    public static final TextEncoder DEFAULT_ENCODER = JSR283_ENCODER;

    public static final char DELIMITER = '/';
    public static final String DELIMITER_STR = new String(new char[] {DELIMITER});
    public static final String PARENT = "..";
    public static final String SELF = ".";
    public static final Path ROOT = new Path(new ArrayList<Segment>(), true);

    protected static final Pattern DELIMITER_PATTERN = Pattern.compile(DELIMITER_STR);
    /**
     * Regular expression used to identify the different parts of a segment. The expression is
     * 
     * <pre>
     * ([&circ;*:/\[\]|]+)(:([&circ;*:/\[\]|]+))?(\[(\d+)])?
     * </pre>
     * 
     * where the first part is accessed with group 1, the second part is accessed with group 3, and the index is accessed with
     * group 5.
     */
    protected static final Pattern SEGMENT_PATTERN = Pattern.compile("([^:/]+)(:([^/\\[\\]]+))?(\\[(\\d+)])?");

    protected static final int NO_INDEX = -1;

    protected static final Segment PARENT_SEGMENT = new Segment("", PARENT, NO_INDEX);
    protected static final Segment SELF_SEGMENT = new Segment("", SELF, NO_INDEX);
    protected static final List<Segment> PARENT_SEGMENT_LIST = Collections.singletonList(PARENT_SEGMENT);
    protected static final List<Segment> SELF_SEGMENT_LIST = Collections.singletonList(SELF_SEGMENT);

    /**
     * A segment of a path.
     * @author Randall Hauch
     */
    @Immutable
    public static class Segment implements Cloneable, Comparable<Segment>, Serializable {

        private final String prefix;
        private final String name;
        private final int index;
        private final int hc;

        protected Segment( String segment, TextEncoder encoder ) {
            Matcher matcher = SEGMENT_PATTERN.matcher(segment);
            if (!matcher.matches()) throw new InvalidPathException("Invalid path segment: " + segment);
            String part1 = matcher.group(1);
            String part2 = matcher.group(3);
            String indexPart = matcher.group(5);
            name = part2 != null ? encoder.decode(part2) : encoder.decode(part1);
            prefix = part2 != null ? encoder.decode(part1) : "";
            index = indexPart != null ? Integer.parseInt(indexPart) : NO_INDEX;
            hc = HashCode.compute(this.prefix, this.name);
        }

        protected Segment( String prefix, String name, int index ) {
            this.prefix = prefix;
            this.name = name;
            this.index = index;
            this.hc = HashCode.compute(this.prefix, this.name);
            assert this.prefix != null;
            assert this.name != null;
        }

        /**
         * @return prefix
         */
        public String getPrefix() {
            return this.prefix;
        }

        /**
         * @return name
         */
        public String getName() {
            return this.name;
        }

        public String getQualifiedName( boolean includeIndex ) {
            StringBuilder sb = new StringBuilder();
            if (this.hasPrefix()) {
                sb.append(this.prefix);
                sb.append(":");
            }
            sb.append(this.name);
            if (includeIndex && this.hasIndex()) {
                sb.append("[").append(this.index).append("]");
            }
            return sb.toString();
        }

        /**
         * @return indices
         */
        public int getIndex() {
            return this.index;
        }

        public boolean hasIndex() {
            return this.index != NO_INDEX;
        }

        public boolean hasPrefix() {
            return this.prefix.length() != 0;
        }

        public boolean isSelfReference() {
            return this.equals(SELF_SEGMENT);
        }

        public boolean isParentReference() {
            return this.equals(PARENT_SEGMENT);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo( Segment that ) {
            if (this == that) return 0;
            int diff = this.prefix.compareTo(that.prefix);
            if (diff != 0) return diff;
            diff = this.name.compareTo(that.name);
            if (diff != 0) return diff;
            return this.index - that.index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object clone() {
            return new Segment(this.prefix, this.name, this.index);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Segment) {
                Segment that = (Segment)obj;
                if (this.hc != that.hc) return false;
                if (!this.prefix.equals(that.prefix)) return false;
                if (!this.name.equals(that.name)) return false;
                if (this.index != that.index) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getQualifiedName(true);
        }
    }

    protected static List<Segment> resolveIntoSegments( Path relativeTo, String relativePath, TextEncoder encoder ) {
        if (relativeTo == null) relativeTo = ROOT;
        if (encoder == null) encoder = DEFAULT_ENCODER;
        assert relativePath != null;
        assert relativePath.trim().equals(relativePath);
        // remove the leading delimiter ...
        int length = relativePath.length();
        if (length > 0 && relativePath.charAt(0) == DELIMITER) {
            relativePath = length > 1 ? relativePath.substring(1) : "";
            length = relativePath.length();
        }
        // remove the trailing delimiter ...
        if (length > 0 && relativePath.charAt(length - 1) == DELIMITER) {
            relativePath = length > 1 ? relativePath.substring(0, length - 1) : "";
            length = relativePath.length();
        }

        List<Segment> segs = new ArrayList<Segment>(relativeTo.segments);
        if (length == 0) return segs;

        int index = segs.size() - 1;
        String[] pathSegments = DELIMITER_PATTERN.split(relativePath);
        if (pathSegments.length == 0) {
            String msg = "A valid path may not contain an empty segment (supplied path: " + relativePath + ")";
            throw new InvalidPathException(msg);
        }
        for (String segment : pathSegments) {
            assert segment != null;
            segment = segment.trim();
            if (segment.length() == 0) {
                String msg = "A valid path may not contain an empty segment (supplied path: " + relativePath + ")";
                throw new InvalidPathException(msg);
            }
            // Look for the different parts ...
            segs.add(new Segment(segment, encoder));
            ++index;
        }
        if (segs.isEmpty()) return relativeTo.segments;
        return Collections.unmodifiableList(segs);
    }

    private final List<Segment> segments;
    private final boolean absolute;
    private final boolean normalized;
    private transient String path;

    /**
     * Create a path by parsing the supplied absolute or relative path. Successive {@link #DELIMITER delimiters} are treated as a
     * single delimiter.
     * @param path the string containing the path with {@link #DELIMITER delimiters} separating the different path segments
     * @throws InvalidPathException if the path is null, empty, or invalid
     */
    public Path( String path ) throws InvalidPathException {
        this(path, DEFAULT_ENCODER);
    }

    /**
     * Create a path by parsing the supplied absolute or relative path. Successive {@link #DELIMITER delimiters} are treated as a
     * single delimiter.
     * @param path the string containing the path with {@link #DELIMITER delimiters} separating the different path segments
     * @param encoder the encoder that should be used to convert escaped characters
     * @throws InvalidPathException if the path is null, empty, or invalid
     */
    public Path( String path, TextEncoder encoder ) throws InvalidPathException {
        if (path == null) throw new InvalidPathException("A null path expression is not valid");
        path = path.trim();
        if (path.length() == 0) throw new InvalidPathException("The path expression may not be blank or contain only whitespace");
        this.segments = resolveIntoSegments(ROOT, path, encoder);
        this.absolute = path.startsWith(DELIMITER_STR);
        this.normalized = isNormalized(this.segments);
    }

    /**
     * Copy constructor for paths. Since paths are immutable, this constructor references the same immutable list of segments used
     * by the supplied path.
     * @param path the original path
     */
    protected Path( Path path ) {
        assert path != null;
        this.segments = path.segments;
        this.absolute = path.absolute;
        this.normalized = path.normalized;
    }

    /*
     * Only to be called by createPath(...), allowing subclasses to be created.
     */
    private Path( List<Segment> segments, boolean isAbsolute ) {
        assert segments != null;
        this.segments = Collections.unmodifiableList(segments);
        this.absolute = isAbsolute;
        this.normalized = isNormalized(this.segments);
    }

    /**
     * Create a path with the list of segments and a flag specifying whether this is absolute. The supplied list is wrapped in an
     * immutable list, so the caller must not hold onto the list. (A copy is not made for performance reaons.) As such, this
     * constructor is made private.
     * @param segments the list of segments.
     * @param isAbsolute whether the path is absolute
     * @return the new path
     */
    protected Path createPath( List<Segment> segments, boolean isAbsolute ) {
        if (segments.isEmpty()) return ROOT;
        return new Path(segments, isAbsolute);
    }

    /**
     * Return the number of segments in this path.
     * @return the number of path segments
     */
    public int size() {
        return this.segments.size();
    }

    /**
     * Return whether this path represents the {@link #ROOT root} path.
     * @return true if this path is the root path, or false otherwise
     */
    public boolean isRoot() {
        return this.segments.isEmpty();
    }

    /**
     * Determine whether this path represents the same as the supplied path. This is equivalent to calling
     * <code>this.compareTo(other) == 0 </code>.
     * @param other the other path to compare with this path
     * @return true if the paths are equivalent, or false otherwise
     */
    public boolean isSame( Path other ) {
        return this.compareTo(other) == 0;
    }

    /**
     * Determine whether this path is an ancestor of the supplied path. A path is considered an ancestor of another path if the
     * the ancestor path appears in its entirety at the beginning of the decendant path, and where the decendant path contains at
     * least one additional segment.
     * @param decendant the path that may be the decendant
     * @return true if this path is an ancestor of the supplied path, or false otherwise
     */
    public boolean isAncestorOf( Path decendant ) {
        if (decendant == null) return false;
        if (this == decendant) return false;
        if (this.size() >= decendant.size()) return false;
        return decendant.toString().startsWith(this.toString());
    }

    /**
     * Determine whether this path is an decendant of the supplied path. A path is considered a decendant of another path if the
     * the decendant path starts exactly with the entire ancestor path but contains at least one additional segment.
     * @param ancestor the path that may be the ancestor
     * @return true if this path is an decendant of the supplied path, or false otherwise
     */
    public boolean isDecendantOf( Path ancestor ) {
        if (ancestor == null) return false;
        if (this == ancestor) return false;
        if (this.size() <= ancestor.size()) return false;
        return this.toString().startsWith(ancestor.toString());
    }

    /**
     * Return whether this path is an absolute path. A path is either relative or {@link #isAbsolute() absolute}. An absolute
     * path starts with a "/".
     * @return true if the path is absolute, or false otherwise
     */
    public boolean isAbsolute() {
        return this.absolute;
    }

    protected boolean isNormalized( List<Segment> segments ) {
        for (Segment segment : segments) {
            if (segment.hasPrefix()) continue;
            if (segment.getName().equals(".") || segment.getName().equals(PARENT)) return false;
        }
        return true;
    }

    /**
     * Return whether this path is normalized and contains no "." segments and as few ".." segments as possible. For example, the
     * path "../a" is normalized, while "/a/b/c/../d" is not normalized.
     * @return true if this path is normalized, or false otherwise
     */
    public boolean isNormalized() {
        return this.normalized;
    }

    /**
     * Get a normalized path with as many ".." segments and all "." resolved.
     * @return the normalized path, or this object if this path is already normalized
     * @throws InvalidPathException if the normalized form would result in a path with negative length (e.g., "/a/../../..")
     */
    public Path getNormalizedPath() {
        if (this.isNormalized()) return this; // ROOT is normalized already
        LinkedList<Segment> newSegments = new LinkedList<Segment>();
        for (Segment segment : segments) {
            if (segment.equals(SELF_SEGMENT)) continue;
            if (segment.equals(PARENT_SEGMENT)) {
                if (newSegments.size() <= 0) {
                    if (this.isAbsolute()) {
                        throw new InvalidPathException(CoreI18n.pathCannotBeNormalized.text(this));
                    }
                }
                if (newSegments.size() > 0 && !newSegments.getLast().equals(PARENT_SEGMENT)) {
                    newSegments.removeLast();
                    continue;
                }
            }
            newSegments.add(segment);
        }
        if (newSegments.isEmpty()) {
            if (this.isAbsolute()) return ROOT;
            // Otherwise relative and it had contained nothing but self references ...
            return createPath(SELF_SEGMENT_LIST, false);
        }
        return createPath(newSegments, this.isAbsolute());
    }

    /**
     * Get the canonical form of this path. A canonical path has is {@link #isAbsolute() absolute} and {@link #isNormalized()}.
     * @return the canonical path, or this object if it is already in its canonical form
     * @throws InvalidPathException if the path is not absolute and cannot be canonicalized
     */
    public Path getCanonicalPath() {
        if (!this.isAbsolute()) {
            String msg = CoreI18n.pathIsNotAbsolute.text(this);
            throw new InvalidPathException(msg);
        }
        if (this.isNormalized()) return this;
        return this.getNormalizedPath();
    }

    /**
     * Get a relative path from the supplied path to this path.
     * @param startingPath the path specifying the starting point for the new relative path; may not be null
     * @return the relative path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws PathNotFoundException if both this path and the supplied path are not absolute
     */
    public Path relativeTo( Path startingPath ) {
        ArgCheck.isNotNull(startingPath, "to");
        if (!this.isAbsolute()) {
            String msg = CoreI18n.pathIsNotAbsolute.text(this);
            throw new InvalidPathException(msg);
        }
        if (!startingPath.isAbsolute()) {
            String msg = CoreI18n.pathIsNotAbsolute.text(startingPath);
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
            relativeSegments.add(PARENT_SEGMENT);
        }
        // Add the segments of this path from the common ancestor ...
        for (int i = lengthOfCommonAncestor; i < this.size(); ++i) {
            relativeSegments.add(this.segments.get(i));
        }
        if (relativeSegments.isEmpty()) {
            relativeSegments.add(SELF_SEGMENT);
        }
        return createPath(relativeSegments, false);
    }

    /**
     * Get the absolute path by resolving the supplied relative (non-absolute) path against this absolute path.
     * @param relativePath the relative path that is to be resolved against this path
     * @return the absolute and normalized path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the this path is not absolute or if the supplied path is not relative.
     * @see #resolve(String)
     * @see #resolve(String, TextEncoder)
     */
    public Path resolve( Path relativePath ) {
        ArgCheck.isNotNull(relativePath, "relative path");
        if (!this.isAbsolute()) {
            String msg = CoreI18n.pathIsAlreadyAbsolute.text(this.path);
            throw new InvalidPathException(msg);
        }
        if (relativePath.isAbsolute()) {
            String msg = CoreI18n.pathIsNotRelative.text(relativePath);
            throw new InvalidPathException(msg);
        }
        // If the relative path is the self or parent reference ...
        relativePath = relativePath.getNormalizedPath();
        if (relativePath.size() == 1) {
            Segment onlySegment = relativePath.getSegment(0);
            if (onlySegment.isSelfReference()) return this;
            if (onlySegment.isParentReference()) return this.getAncestor();
        }
        List<Segment> segments = new ArrayList<Segment>(this.size() + relativePath.size());
        segments.addAll(this.segments);
        segments.addAll(relativePath.segments);
        return createPath(segments, true).getNormalizedPath();
    }

    /**
     * Get the absolute path by resolving the supplied relative (non-absolute) path against this absolute path.
     * @param relativePath the relative path that is to be resolved against this path
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the this path is not absolute or if the supplied path is not relative.
     * @see #resolve(String)
     * @see #resolve(String, TextEncoder)
     */
    public Path resolve( String relativePath ) {
        return resolve(relativePath, null);
    }

    /**
     * Get the absolute path by resolving the supplied relative (non-absolute) path against this absolute path.
     * @param relativePath the relative path that is to be resolved against this path
     * @param encoder the encoder to use, or null if the {@link #NO_OP_ENCODER default encoder} should be used
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the this path is not absolute or if the supplied path is not relative.
     * @see #resolve(String)
     * @see #resolve(String, TextEncoder)
     */
    public Path resolve( String relativePath, TextEncoder encoder ) {
        return resolve(new Path(relativePath, encoder));
    }

    /**
     * Get the absolute path by resolving this relative (non-absolute) path against the supplied absolute path.
     * @param absolutePath the absolute path to which this relative path should be resolve
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the supplied path is not absolute or if this path is not relative.
     * @see #resolveAgainst(String)
     * @see #resolveAgainst(String, TextEncoder)
     */
    public Path resolveAgainst( Path absolutePath ) {
        ArgCheck.isNotNull(absolutePath, "absolute path");
        return absolutePath.resolve(this);
    }

    /**
     * Get the absolute path by resolving this relative (non-absolute) path against the supplied absolute path.
     * @param absolutePath the absolute path to which this relative path should be resolve
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the supplied path is not absolute or if this path is not relative.
     * @see #resolveAgainst(Path)
     * @see #resolveAgainst(String, TextEncoder)
     */
    public Path resolveAgainst( String absolutePath ) {
        return resolveAgainst(absolutePath, null);
    }

    /**
     * Get the absolute path by resolving this relative (non-absolute) path against the supplied absolute path.
     * @param absolutePath the absolute path to which this relative path should be resolve
     * @param encoder the encoder to use, or null if the {@link #NO_OP_ENCODER default encoder} should be used
     * @return the absolute path resolved from this path and the supplied absolute path
     * @throws IllegalArgumentException if the supplied path is null
     * @throws InvalidPathException if the supplied path is not absolute or if this path is not relative.
     * @see #resolveAgainst(Path)
     * @see #resolveAgainst(String)
     */
    public Path resolveAgainst( String absolutePath, TextEncoder encoder ) {
        return resolveAgainst(new Path(absolutePath, encoder));
    }

    /**
     * Return the path to the parent, or this path if it is the {@link #isRoot() root}. This is an efficient operation that does
     * not require copying any data.
     * @return the parent path, or this path if it is already the root
     */
    public Path getAncestor() {
        if (this.isRoot()) return this;
        if (this.segments.size() == 1) return ROOT;
        return createPath(this.segments.subList(0, size() - 1), this.isAbsolute());
    }

    /**
     * Return the path to the ancestor of the supplied degree. An ancestor of degree <code>x</code> is the path that is
     * <code>x</code> levels up along the path. For example, <code>degree = 0</code> returns this path, while
     * <code>degree = 1</code> returns the parent of this path, <code>degree = 2</code> returns the grandparent of this path,
     * and so on. Note that the result may be unexpected if this path is not {@link #isNormalized() normalized}, as a
     * non-normalized path contains ".." and "." segments.
     * @param degree
     * @return the ancestor of the supplied degree
     * @throws IllegalArgumentException if the degree is negative
     * @throws PathNotFoundException if the degree is greater than the {@link #size() length} of this path
     */
    public Path getAncestor( int degree ) {
        ArgCheck.isNonNegative(degree, "degree");
        if (this.isRoot()) return this;
        int endIndex = this.segments.size() - degree;
        if (endIndex < 0) {
            String msg = CoreI18n.pathAncestorDegreeIsInvalid.text(this.path, Inflector.getInstance().ordinalize(degree));
            throw new PathNotFoundException(msg);
        }
        return createPath(this.segments.subList(0, endIndex), this.isAbsolute());
    }

    public boolean hasSameAncestor( Path that ) {
        if (that == null) return false;
        if (that.size() != this.size()) return false;
        return (this.getAncestor().equals(that.getAncestor()));
    }

    public Path getCommonAncestor( Path that ) {
        if (that == null) return null;
        List<Segment> commonSegments = new ArrayList<Segment>();
        Iterator<Segment> thisIter = this.getNormalizedPath().iterator();
        Iterator<Segment> thatIter = that.getNormalizedPath().iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            Segment thisSeg = thisIter.next();
            Segment thatSeg = thatIter.next();
            if (thisSeg.equals(thatSeg)) {
                commonSegments.add(thisSeg);
            } else {
                break;
            }
        }
        if (commonSegments.isEmpty()) return ROOT;
        return createPath(commonSegments, this.isAbsolute());
    }

    /**
     * Determine whether the last segment matches that supplied.
     * @param segment the value for the last segment; if null, false is returned
     * @return the last segment, or null if the path is empty
     */
    public boolean endsWith( String segment ) {
        return endsWith(segment, NO_OP_ENCODER);
    }

    /**
     * Determine whether the last segment matches that supplied.
     * @param segment the value for the last segment; if null, false is returned
     * @param encoder the encoder to use, or null if the {@link #NO_OP_ENCODER default encoder} should be used
     * @return the last segment, or null if the path is empty
     */
    public boolean endsWith( String segment, TextEncoder encoder ) {
        if (segment == null || this.segments.isEmpty()) return false;
        if (encoder == null) encoder = NO_OP_ENCODER;
        return (getLastSegment().equals(new Segment(segment, encoder)));
    }

    /**
     * Get the last segment in this path.
     * @return the last segment, or null if the path is empty
     */
    public Segment getLastSegment() {
        if (this.segments.isEmpty()) return null;
        return this.segments.get(this.segments.size() - 1);
    }

    /**
     * Get the segment at the supplied index.
     * @param index the index
     * @return the segment
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Segment getSegment( int index ) {
        return this.segments.get(index);
    }

    /**
     * Create a path by appending a single segment without decoding any of the characters. Any segment that is null will be
     * ignored. This is equivalent to calling <code>append(segment,null)</code>.
     * @param segment the segment to append to this path
     * @return the new path
     */
    public Path append( String segment ) {
        return append(segment, NO_OP_ENCODER);
    }

    /**
     * Create a path by appending a single segment without decoding any of the characters. Any segment that is null will be
     * ignored. This is equivalent to calling <code>append(segment,null)</code>.
     * @param segments the segments to append to the path
     * @return the new path
     */
    public Path append( String... segments ) {
        return append(segments, NO_OP_ENCODER);
    }

    /**
     * Create a path by appending a single segment. Any segment that is null will be ignored. Whitespaces are not removed, but are
     * decoded according to the supplied <code>encoder</code>.
     * @param segment the segment to append to this path
     * @param encoder the encoder to use, or null if the {@link #NO_OP_ENCODER default encoder} should be used
     * @return the new path
     * @throws InvalidPathException if the segment is empty or blank
     */
    public Path append( String segment, TextEncoder encoder ) {
        if (segment == null) return this;
        if (segment.trim().length() == 0) {
            String msg = "A valid path may not contain an empty segment";
            throw new InvalidPathException(msg);
        }
        // Don't used the trimmed version
        if (encoder == null) encoder = NO_OP_ENCODER;
        List<Segment> segments = new ArrayList<Segment>(this.size() + 1);
        segments.addAll(this.segments);
        segments.add(new Segment(encoder.encode(segment), encoder));
        return createPath(segments, this.isAbsolute());
    }

    /**
     * Create a path by appending multiple segment. Any segment that is null will be ignored. Whitespaces are not removed, but are
     * decoded according to the supplied <code>encoder</code>.
     * @param segments the segments to append to this path
     * @param encoder the encoder to use, or null if the {@link #NO_OP_ENCODER default encoder} should be used
     * @return the new path
     * @throws InvalidPathException if a segment is empty or blank
     */
    public Path append( String[] segments, TextEncoder encoder ) {
        if (segments == null || segments.length == 0) return this;
        if (encoder == null) encoder = NO_OP_ENCODER;
        List<Segment> segs = new ArrayList<Segment>(this.size() + segments.length);
        segs.addAll(this.segments);
        for (String segment : segments) {
            if (segment == null) continue;
            if (segment.trim().length() == 0) {
                String msg = "A valid path may not contain an empty segment: " + StringUtil.readableString(segments);
                throw new InvalidPathException(msg);
            }
            // Don't use the trimmed version ...
            segs.add(new Segment(encoder.encode(segment), encoder));
        }
        return createPath(segs, this.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Segment> iterator() {
        return this.segments.iterator();
    }

    /**
     * Obtain a copy of the segments in this path. None of the segments are encoded.
     * @return the array of segments as a copy
     */
    public Segment[] toArray() {
        return this.segments.toArray(new Segment[this.segments.size()]);
    }

    /**
     * Obtain a copy of the segments in this path. None of the segments are encoded.
     * @return the array of segments as a copy
     */
    public String[] toStringArray() {
        String[] result = new String[this.segments.size()];
        int i = 0;
        for (Segment segment : this.segments) {
            result[i++] = segment.toString();
        }
        return result;
    }

    /**
     * Get an unmodifiable list of the path segments.
     * @return the unmodifiable list of path segments; never null
     */
    public List<Segment> toList() {
        return this.segments;
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
            if (this.hashCode() != that.hashCode()) return false;
            return this.segments.equals(that.segments);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Path that ) {
        if (that == null) return 1;
        if (this == that) return 0;
        Iterator<Segment> thisIter = this.segments.iterator();
        Iterator<Segment> thatIter = that.segments.iterator();
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
    public Path clone() {
        return new Path(this);
    }

    /**
     * Get the string form of the path. The {@link #DEFAULT_ENCODER default encoder} is used to encode characters in each of the
     * path segments.
     * @return the encoded string
     * @see #getString(TextEncoder)
     */
    public String getString() {
        return getString(DEFAULT_ENCODER);
    }

    /**
     * Get the encoded string form of the path, using the supplied encoder to encode characters in each of the path segments.
     * @param encoder the encoder to use, or null if the {@link #DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @see #getString()
     */
    public String getString( TextEncoder encoder ) {
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
            sb.append(encoder.encode(segment.toString()));
        }
        String result = sb.toString();
        // Save the result to the internal string if this the default encoder is used ...
        if (encoder == DEFAULT_ENCODER && this.path == null) this.path = result;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this.path == null) {
            this.path = getString(DEFAULT_ENCODER);
        }
        return this.path;
    }

}

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

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#NAME} values.
 */
@Immutable
public class PathValueFactory extends AbstractValueFactory<Path> implements PathFactory {

    /**
     * Regular expression used to identify the different segments in a path, using the standard '/' delimiter. The expression is
     * simply:
     * 
     * <pre>
     * /
     * </pre>
     */
    protected static final Pattern DELIMITER_PATTERN = Pattern.compile("/");

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

    private final ValueFactory<Name> nameValueFactory;

    public PathValueFactory( TextDecoder decoder,
                             ValueFactory<String> stringValueFactory,
                             ValueFactory<Name> nameValueFactory ) {
        super(PropertyType.PATH, decoder, stringValueFactory);
        CheckArg.isNotNull(nameValueFactory, "nameValueFactory");
        this.nameValueFactory = nameValueFactory;
    }

    /**
     * @return nameValueFactory
     */
    protected ValueFactory<Name> getNameValueFactory() {
        return this.nameValueFactory;
    }

    @Override
    public Path createRootPath() {
        return RootPath.INSTANCE;
    }

    @Override
    public Path create( String value ) {
        return create(value, getDecoder());
    }

    @Override
    public Path create( final String value,
                        TextDecoder decoder ) {
        if (value == null) return null;
        String trimmedValue = value.trim();
        int length = trimmedValue.length();
        boolean absolute = false;
        if (length == 0) {
            return BasicPath.EMPTY_RELATIVE;
        }
        if (length == 1) {
            if (Path.DELIMITER_STR.equals(trimmedValue)) return RootPath.INSTANCE;
            if (Path.SELF.equals(trimmedValue)) return BasicPath.SELF_PATH;
        } else if (length == 2) {
            if (Path.PARENT.equals(trimmedValue)) return BasicPath.PARENT_PATH;
        }

        // Check for an identifier ...
        char firstChar = trimmedValue.charAt(0);
        if (firstChar == Path.IDENTIFIER_LEADING_TERMINAL && trimmedValue.charAt(length - 1) == Path.IDENTIFIER_TRAILING_TERMINAL) {
            // This is an identifier path, so read the identifier ...
            String id = trimmedValue.substring(1, length - 1);
            // And create a name and segment ...
            Name idName = this.nameValueFactory.create(id);
            return new IdentifierPath(new IdentifierPathSegment(idName));
        }

        // Remove the leading delimiter ...
        if (firstChar == Path.DELIMITER) {
            trimmedValue = length > 1 ? trimmedValue.substring(1) : "";
            --length;
            absolute = true;
        }
        // remove the trailing delimiter ...
        if (length > 0 && trimmedValue.charAt(length - 1) == Path.DELIMITER) {
            trimmedValue = length > 1 ? trimmedValue.substring(0, length - 1) : "";
            length = trimmedValue.length();
        }
        if (length == 0) {
            return RootPath.INSTANCE;
        }

        // Parse the path into its segments ...
        List<Segment> segments = new ArrayList<Segment>();
        // String[] pathSegments = DELIMITER_PATTERN.split(trimmedValue);
        String[] pathSegments = splitPath(trimmedValue);
        if (pathSegments.length == 0) {
            throw new ValueFormatException(value, getPropertyType(), GraphI18n.validPathMayNotContainEmptySegment.text(value));
        }
        if (decoder == null) decoder = getDecoder();
        assert pathSegments.length != 0;
        assert decoder != null;
        for (String segment : pathSegments) {
            assert segment != null;
            segment = segment.trim();
            if (segment.length() == 0) {
                throw new ValueFormatException(value, getPropertyType(), GraphI18n.validPathMayNotContainEmptySegment.text(value));
            }
            // Create the name and add a segment with it ...
            segments.add(createSegment(segment, decoder));
        }

        if (absolute && segments.size() == 1) {
            // Special case of a single-segment name ...
            return new ChildPath(RootPath.INSTANCE, segments.get(0));
        }
        // Create a path constructed from the supplied segments ...
        return new BasicPath(segments, absolute);
    }

    String[] splitPath( String rawPath ) {
        List<String> segments = new LinkedList<String>();

        int curr = 0;
        int nextBrace = rawPath.indexOf('{');
        int nextSlash = rawPath.indexOf('/');

        while (true) {
            // Next brace comes before next slash
            if ((nextSlash == -1 && nextBrace > -1) || (nextBrace > -1 && nextBrace < nextSlash)) {
                int nextClosingBrace = rawPath.indexOf('}', nextBrace);

                if (nextClosingBrace == -1) {
                    throw new ValueFormatException(rawPath, getPropertyType(), GraphI18n.missingClosingBrace.text(rawPath));
                }

                // Move the next checks past the end of the braced section, but don't add to the result set yet
                nextSlash = rawPath.indexOf('/', nextClosingBrace + 1);
                nextBrace = rawPath.indexOf('{', nextClosingBrace + 1);
            }
            // Next slash comes before next brace
            else if ((nextBrace == -1 && nextSlash > -1) || (nextSlash > -1 && nextSlash < nextBrace)) {
                if (nextSlash > 0) {
                    // Don't add an empty string from the beginning of an absolute path
                    segments.add(rawPath.substring(curr, nextSlash));
                }

                curr = nextSlash + 1;

                nextSlash = rawPath.indexOf('/', curr);
            }
            // No more slashes or braces
            else {
                segments.add(rawPath.substring(curr));
                return segments.toArray(new String[segments.size()]);
            }
        }
    }

    @Override
    public Path create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( Name value ) {
        if (value == null) return null;
        try {
            return new ChildPath(RootPath.INSTANCE, new BasicPathSegment(value));
        } catch (IllegalArgumentException e) {
            throw new ValueFormatException(value, getPropertyType(), e);
        }
    }

    @Override
    public Path create( Path value ) {
        return value;
    }

    @Override
    public Path create( Path.Segment value ) {
        return createAbsolutePath(value);
    }

    @Override
    public Path createAbsolutePath( Name... segmentNames ) {
        if (segmentNames == null || segmentNames.length == 0) return RootPath.INSTANCE;
        List<Segment> segments = new ArrayList<Segment>(segmentNames.length);
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                CheckArg.containsNoNulls(segmentNames, "segment names");
            }
            segments.add(new BasicPathSegment(segmentName));
        }
        if (segments.size() == 1) {
            // Special case of a single-segment name ...
            return new ChildPath(RootPath.INSTANCE, segments.get(0));
        }
        return new BasicPath(segments, true);
    }

    @Override
    public Path createAbsolutePath( Segment... segments ) {
        if (segments == null || segments.length == 0) return RootPath.INSTANCE;
        List<Segment> segmentsList = new ArrayList<Segment>(segments.length);
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        if (segmentsList.size() == 1) {
            // Special case of a single-segment name ...
            return new ChildPath(RootPath.INSTANCE, segmentsList.get(0));
        }
        return new BasicPath(segmentsList, true);
    }

    @Override
    public Path createAbsolutePath( Iterable<Segment> segments ) {
        List<Segment> segmentsList = new LinkedList<Segment>();
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        if (segmentsList.isEmpty()) return RootPath.INSTANCE;
        if (segmentsList.size() == 1) {
            // Special case of a single-segment name ...
            return new ChildPath(RootPath.INSTANCE, segmentsList.get(0));
        }
        return new BasicPath(segmentsList, true);
    }

    @Override
    public Path createRelativePath() {
        return BasicPath.EMPTY_RELATIVE;
    }

    @Override
    public Path createRelativePath( Name... segmentNames ) {
        if (segmentNames == null || segmentNames.length == 0) return BasicPath.EMPTY_RELATIVE;
        List<Segment> segments = new ArrayList<Segment>(segmentNames.length);
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                CheckArg.containsNoNulls(segmentNames, "segment names");
            }
            segments.add(new BasicPathSegment(segmentName));
        }
        return new BasicPath(segments, false);
    }

    @Override
    public Path createRelativePath( Segment... segments ) {
        if (segments == null || segments.length == 0) return BasicPath.EMPTY_RELATIVE;
        List<Segment> segmentsList = new ArrayList<Segment>(segments.length);
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            assert segment != null;
            if (segment.isIdentifier()) {
                throw new InvalidPathException(
                                               GraphI18n.unableToCreateRelativePathWithIdentifierSegment.text(segments.toString()));
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, false);
    }

    @Override
    public Path createRelativePath( Iterable<Segment> segments ) {
        List<Segment> segmentsList = new LinkedList<Segment>();
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            assert segment != null;
            if (segment.isIdentifier()) {
                throw new InvalidPathException(
                                               GraphI18n.unableToCreateRelativePathWithIdentifierSegment.text(segments.toString()));
            }
            segmentsList.add(segment);
        }
        if (segmentsList.isEmpty()) return BasicPath.EMPTY_RELATIVE;
        return new BasicPath(segmentsList, false);
    }

    @Override
    public Path create( Path parentPath,
                        Path childPath ) {
        CheckArg.isNotNull(parentPath, "parent path");
        CheckArg.isNotNull(childPath, "child path");
        if (childPath.size() == 0) return parentPath;
        if (parentPath.size() == 0) {
            // Just need to return the child path, but it must be absolute if the parent is ...
            if (childPath.isAbsolute() == parentPath.isAbsolute()) return childPath;
            // They aren't the same absoluteness, so create a new one ...
            return new BasicPath(childPath.getSegmentsList(), parentPath.isAbsolute());
        }
        List<Segment> segments = new ArrayList<Segment>(parentPath.size() + childPath.size());
        for (Segment seg : parentPath) {
            segments.add(seg);
        }
        for (Segment seg : childPath) {
            segments.add(seg);
        }
        return new BasicPath(segments, parentPath.isAbsolute());
    }

    @Override
    public Path create( Path parentPath,
                        Name segmentName,
                        int index ) {
        CheckArg.isNotNull(parentPath, "parent path");
        CheckArg.isNotNull(segmentName, "segment name");
        return new ChildPath(parentPath, new BasicPathSegment(segmentName, index));
    }

    @Override
    public Path create( Path parentPath,
                        String segmentName,
                        int index ) {
        return create(parentPath, nameValueFactory.create(segmentName), index);
    }

    @Override
    public Path create( Path parentPath,
                        Name... segmentNames ) {
        CheckArg.isNotNull(parentPath, "parent path");
        if (segmentNames == null || segmentNames.length == 0) return parentPath;
        if (segmentNames.length == 1 && segmentNames[0] != null) {
            return new ChildPath(parentPath, new BasicPathSegment(segmentNames[0]));
        }

        List<Segment> segments = new ArrayList<Segment>(parentPath.size() + 1);
        segments.addAll(parentPath.getSegmentsList());
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                CheckArg.containsNoNulls(segmentNames, "segment names");
            }
            segments.add(new BasicPathSegment(segmentName));
        }
        return new BasicPath(segments, parentPath.isAbsolute());
    }

    @Override
    public Path create( Path parentPath,
                        Segment... segments ) {
        CheckArg.isNotNull(parentPath, "parent path");
        if (segments == null || segments.length == 0) return RootPath.INSTANCE;
        if (segments.length == 1 && segments[0] != null) {
            if (parentPath.isIdentifier()) {
                throw new InvalidPathException(GraphI18n.unableToCreatePathBasedUponIdentifierPath.text(parentPath,
                                                                                                        segments.toString()));
            }
            if (segments[0].isIdentifier()) {
                throw new InvalidPathException(
                                               GraphI18n.unableToCreatePathUsingIdentifierPathAndAnotherPath.text(parentPath,
                                                                                                                  segments.toString()));
            }
            return new ChildPath(parentPath, segments[0]);
        }

        List<Segment> segmentsList = new ArrayList<Segment>(parentPath.size() + 1);
        segmentsList.addAll(parentPath.getSegmentsList());
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            assert segment != null;
            if (segment.isIdentifier()) {
                throw new InvalidPathException(
                                               GraphI18n.unableToCreatePathUsingIdentifierPathAndAnotherPath.text(parentPath,
                                                                                                                  segments.toString()));
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, parentPath.isAbsolute());
    }

    @Override
    public Path create( Path parentPath,
                        Iterable<Segment> segments ) {
        CheckArg.isNotNull(parentPath, "parent path");
        if (parentPath.isIdentifier()) {
            throw new InvalidPathException(GraphI18n.unableToCreatePathBasedUponIdentifierPath.text(parentPath,
                                                                                                    segments.toString()));
        }

        List<Segment> segmentsList = new LinkedList<Segment>();
        segmentsList.addAll(parentPath.getSegmentsList());
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            assert segment != null;
            if (segment.isIdentifier()) {
                throw new InvalidPathException(
                                               GraphI18n.unableToCreatePathUsingIdentifierPathAndAnotherPath.text(parentPath,
                                                                                                                  segments.toString()));
            }
            segmentsList.add(segment);
        }
        if (segmentsList.isEmpty()) return RootPath.INSTANCE;
        if (segmentsList.size() == 0) return new ChildPath(parentPath, segmentsList.get(0));
        return new BasicPath(segmentsList, parentPath.isAbsolute());
    }

    @Override
    public Path create( Path parentPath,
                        String subpath ) {
        CheckArg.isNotNull(parentPath, "parentPath");
        CheckArg.isNotNull(subpath, "subpath");
        if (parentPath.isIdentifier()) {
            throw new InvalidPathException(GraphI18n.unableToCreatePathBasedUponIdentifierPath.text(parentPath, subpath));
        }
        subpath = subpath.trim();
        boolean singleChild = subpath.indexOf(Path.DELIMITER) == -1;
        if (!singleChild && subpath.startsWith("./")) {
            if (subpath.length() == 2) return parentPath; // self reference
            // Remove the leading parent reference and try again to see if single child ...
            subpath = subpath.substring(2);
            singleChild = subpath.indexOf(Path.DELIMITER) == -1;
        }
        if (singleChild) {
            try {
                Path.Segment childSegment = createSegment(subpath);
                if (childSegment.isIdentifier()) {
                    throw new InvalidPathException(GraphI18n.unableToCreatePathUsingIdentifierPathAndAnotherPath.text(parentPath,
                                                                                                                      subpath));
                }
                return new ChildPath(parentPath, childSegment);
            } catch (IllegalArgumentException t) {
                // Catch and eat, letting the slower implementation catch anything ...
            }
        }
        // It is a subpath with more than one segment, so create a relative path for the subpath ...
        Path relativeSubpath = create(subpath);
        return create(parentPath, relativeSubpath);

    }

    @Override
    public Segment createSegment( Name segmentName ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName);
    }

    @Override
    public Segment createSegment( Name segmentName,
                                  int index ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName, index);
    }

    @Override
    public Segment createSegment( String segmentName ) {
        return createSegment(segmentName, getDecoder());
    }

    @Override
    public Segment createSegment( String segmentName,
                                  TextDecoder decoder ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        int startBracketNdx = segmentName.lastIndexOf('[');
        if (startBracketNdx < 0) {
            return new BasicPathSegment(this.nameValueFactory.create(segmentName, decoder));
        }
        int endBracketNdx = segmentName.lastIndexOf(']', segmentName.length() - 1);
        if (endBracketNdx < 0) {
            throw new IllegalArgumentException(GraphI18n.missingEndBracketInSegmentName.text(segmentName));
        }
        if (startBracketNdx == 0 && endBracketNdx == (segmentName.length() - 1)) {
            // This is an identifier segment ...
            Name id = this.nameValueFactory.create(segmentName.substring(1, endBracketNdx));
            return new IdentifierPathSegment(id);
        }
        String ndx = segmentName.substring(startBracketNdx + 1, endBracketNdx);
        try {
            return new BasicPathSegment(this.nameValueFactory.create(segmentName.substring(0, startBracketNdx), decoder),
                                        Integer.parseInt(ndx));
        } catch (NumberFormatException err) {
            throw new ValueFormatException(segmentName, getPropertyType(), GraphI18n.invalidIndexInSegmentName.text(ndx,
                                                                                                                    segmentName));
        }
    }

    @Override
    public Segment createSegment( String segmentName,
                                  int index ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(this.nameValueFactory.create(segmentName), index);
    }

    @Override
    public Path create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( URI value ) {
        if (value == null) return null;
        String asciiString = value.toASCIIString();
        // Remove any leading "./" ...
        if (asciiString.startsWith("./") && asciiString.length() > 2) {
            asciiString = asciiString.substring(2);
        }
        if (asciiString.indexOf('/') == -1) {
            return create(asciiString);
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(URI.class.getSimpleName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Path create( NodeKey value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          NodeKey.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Path create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Path create( BinaryValue value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Path create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    protected Path[] createEmptyArray( int length ) {
        return new Path[length];
    }

}

/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.IoException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.Path.Segment;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#NAME} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
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

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createRootPath()
     */
    public Path createRootPath() {
        return RootPath.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public Path create( String value ) {
        return create(value, getDecoder());
    }

    /**
     * {@inheritDoc}
     */
    public Path create( final String value,
                        TextDecoder decoder ) {
        if (value == null) return null;
        String trimmedValue = value.trim();
        int length = trimmedValue.length();
        boolean absolute = false;
        if (length == 0) {
            return BasicPath.EMPTY_RELATIVE;
        }
        if (Path.DELIMITER_STR.equals(trimmedValue)) return RootPath.INSTANCE;
        if (Path.SELF.equals(trimmedValue)) return BasicPath.SELF_PATH;
        if (Path.PARENT.equals(trimmedValue)) return BasicPath.PARENT_PATH;

        // Remove the leading delimiter ...
        if (trimmedValue.charAt(0) == Path.DELIMITER) {
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
        String[] pathSegments = DELIMITER_PATTERN.split(trimmedValue);
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

    /**
     * {@inheritDoc}
     */
    public Path create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.DateTime)
     */
    public Path create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Name value ) {
        if (value == null) return null;
        try {
            return new ChildPath(RootPath.INSTANCE, new BasicPathSegment(value));
        } catch (IllegalArgumentException e) {
            throw new ValueFormatException(value, getPropertyType(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Path value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createAbsolutePath(java.lang.Iterable)
     */
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

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createRelativePath()
     */
    public Path createRelativePath() {
        return BasicPath.EMPTY_RELATIVE;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Path createRelativePath( Segment... segments ) {
        if (segments == null || segments.length == 0) return BasicPath.EMPTY_RELATIVE;
        List<Segment> segmentsList = new ArrayList<Segment>(segments.length);
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createRelativePath(java.lang.Iterable)
     */
    public Path createRelativePath( Iterable<Segment> segments ) {
        List<Segment> segmentsList = new LinkedList<Segment>();
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        if (segmentsList.isEmpty()) return BasicPath.EMPTY_RELATIVE;
        return new BasicPath(segmentsList, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#create(org.jboss.dna.graph.property.Path, org.jboss.dna.graph.property.Path)
     */
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

    /**
     * {@inheritDoc}
     */
    public Path create( Path parentPath,
                        Name segmentName,
                        int index ) {
        CheckArg.isNotNull(parentPath, "parent path");
        CheckArg.isNotNull(segmentName, "segment name");
        return new ChildPath(parentPath, new BasicPathSegment(segmentName, index));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#create(org.jboss.dna.graph.property.Path, java.lang.String, int)
     */
    public Path create( Path parentPath,
                        String segmentName,
                        int index ) {
        return create(parentPath, nameValueFactory.create(segmentName), index);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Path create( Path parentPath,
                        Segment... segments ) {
        CheckArg.isNotNull(parentPath, "parent path");
        if (segments == null || segments.length == 0) return RootPath.INSTANCE;
        if (segments.length == 1 && segments[0] != null) {
            return new ChildPath(parentPath, segments[0]);
        }

        List<Segment> segmentsList = new ArrayList<Segment>(parentPath.size() + 1);
        segmentsList.addAll(parentPath.getSegmentsList());
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, parentPath.isAbsolute());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#create(org.jboss.dna.graph.property.Path, java.lang.Iterable)
     */
    public Path create( Path parentPath,
                        Iterable<Segment> segments ) {
        CheckArg.isNotNull(parentPath, "parent path");

        List<Segment> segmentsList = new LinkedList<Segment>();
        segmentsList.addAll(parentPath.getSegmentsList());
        for (Segment segment : segments) {
            if (segment == null) {
                CheckArg.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        if (segmentsList.isEmpty()) return RootPath.INSTANCE;
        if (segmentsList.size() == 0) return new ChildPath(parentPath, segmentsList.get(0));
        return new BasicPath(segmentsList, parentPath.isAbsolute());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#create(org.jboss.dna.graph.property.Path, java.lang.String)
     */
    public Path create( Path parentPath,
                        String subpath ) {
        CheckArg.isNotNull(parentPath, "parentPath");
        CheckArg.isNotNull(subpath, "subpath");
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
                return new ChildPath(parentPath, childSegment);
            } catch (IllegalArgumentException t) {
                // Catch and eat, letting the slower implementation catch anything ...
            }
        }
        // It is a subpath with more than one segment, so create a relative path for the subpath ...
        Path relativeSubpath = create(subpath);
        return create(parentPath, relativeSubpath);

    }

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( Name segmentName ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName);
    }

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( Name segmentName,
                                  int index ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName, index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createSegment(java.lang.String)
     */
    public Segment createSegment( String segmentName ) {
        return createSegment(segmentName, getDecoder());
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.graph.property.PathFactory#createSegment(java.lang.String, org.jboss.dna.common.text.TextDecoder)
     */
    public Segment createSegment( String segmentName,
                                  TextDecoder decoder ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        int startBracketNdx = segmentName.indexOf('[');
        if (startBracketNdx < 0) {
            return new BasicPathSegment(this.nameValueFactory.create(segmentName, decoder));
        }
        int endBracketNdx = segmentName.indexOf(']', startBracketNdx);
        if (endBracketNdx < 0) {
            throw new IllegalArgumentException(GraphI18n.missingEndBracketInSegmentName.text(segmentName));
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

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( String segmentName,
                                  int index ) {
        CheckArg.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(this.nameValueFactory.create(segmentName), index);
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(java.util.UUID)
     */
    public Path create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.Binary)
     */
    public Path create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( InputStream stream,
                        long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Reader reader,
                        long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path[] createEmptyArray( int length ) {
        return new Path[length];
    }

}

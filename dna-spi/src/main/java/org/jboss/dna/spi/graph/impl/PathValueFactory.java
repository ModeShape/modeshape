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
package org.jboss.dna.spi.graph.impl;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.Path.Segment;

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
        ArgCheck.isNotNull(nameValueFactory, "nameValueFactory");
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
     * @see org.jboss.dna.spi.graph.PathFactory#createRootPath()
     */
    public Path createRootPath() {
        return BasicPath.ROOT;
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
            return BasicPath.ROOT;
        }

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
            return BasicPath.ROOT;
        }

        // Parse the path into its segments ...
        List<Segment> segments = new ArrayList<Segment>();
        String[] pathSegments = DELIMITER_PATTERN.split(trimmedValue);
        if (pathSegments.length == 0) {
            throw new IllegalArgumentException(SpiI18n.validPathMayNotContainEmptySegment.text(value));
        }
        if (decoder == null) decoder = getDecoder();
        assert pathSegments.length != 0;
        assert decoder != null;
        for (String segment : pathSegments) {
            assert segment != null;
            segment = segment.trim();
            if (segment.length() == 0) {
                throw new IllegalArgumentException(SpiI18n.validPathMayNotContainEmptySegment.text(value));
            }
            // Create the name and add a segment with it ...
            segments.add(createSegment(segment, decoder));
        }

        // Create a path constructed from the supplied segments ...
        return new BasicPath(segments, absolute);
    }

    /**
     * {@inheritDoc}
     */
    public Path create( int value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( long value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( boolean value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( float value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( double value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( BigDecimal value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Calendar value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Date value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Name value ) {
        if (value == null) return null;
        List<Path.Segment> segments = new ArrayList<Path.Segment>(1);
        segments.add(new BasicPathSegment(value));
        return new BasicPath(segments, true);
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
        if (segmentNames == null || segmentNames.length == 0) return BasicPath.ROOT;
        List<Segment> segments = new ArrayList<Segment>(segmentNames.length);
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                ArgCheck.containsNoNulls(segmentNames, "segment names");
            }
            segments.add(new BasicPathSegment(segmentName));
        }
        return new BasicPath(segments, true);
    }

    /**
     * {@inheritDoc}
     */
    public Path createAbsolutePath( Segment... segments ) {
        if (segments == null || segments.length == 0) return BasicPath.ROOT;
        List<Segment> segmentsList = new ArrayList<Segment>(segments.length);
        for (Segment segment : segments) {
            if (segment == null) {
                ArgCheck.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, true);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.spi.graph.PathFactory#createRelativePath()
     */
    public Path createRelativePath() {
        return BasicPath.SELF_PATH;
    }

    /**
     * {@inheritDoc}
     */
    public Path createRelativePath( Name... segmentNames ) {
        if (segmentNames == null || segmentNames.length == 0) return BasicPath.ROOT;
        List<Segment> segments = new ArrayList<Segment>(segmentNames.length);
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                ArgCheck.containsNoNulls(segmentNames, "segment names");
            }
            segments.add(new BasicPathSegment(segmentName));
        }
        return new BasicPath(segments, false);
    }

    /**
     * {@inheritDoc}
     */
    public Path createRelativePath( Segment... segments ) {
        if (segments == null || segments.length == 0) return BasicPath.ROOT;
        List<Segment> segmentsList = new ArrayList<Segment>(segments.length);
        for (Segment segment : segments) {
            if (segment == null) {
                ArgCheck.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, false);
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Path parentPath,
                        Name segmentName,
                        int index ) {
        ArgCheck.isNotNull(parentPath, "parent path");
        ArgCheck.isNotNull(segmentName, "segment name");
        List<Segment> segments = new ArrayList<Segment>(parentPath.size() + 1);
        segments.addAll(parentPath.getSegmentsList());
        segments.add(new BasicPathSegment(segmentName, index));
        return new BasicPath(segments, parentPath.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Path parentPath,
                        Name... segmentNames ) {
        ArgCheck.isNotNull(parentPath, "parent path");
        if (segmentNames == null || segmentNames.length == 0) return parentPath;

        List<Segment> segments = new ArrayList<Segment>(parentPath.size() + 1);
        segments.addAll(parentPath.getSegmentsList());
        for (Name segmentName : segmentNames) {
            if (segmentName == null) {
                ArgCheck.containsNoNulls(segmentNames, "segment names");
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
        ArgCheck.isNotNull(parentPath, "parent path");
        if (segments == null || segments.length == 0) return BasicPath.ROOT;

        List<Segment> segmentsList = new ArrayList<Segment>(parentPath.size() + 1);
        segmentsList.addAll(parentPath.getSegmentsList());
        for (Segment segment : segments) {
            if (segment == null) {
                ArgCheck.containsNoNulls(segments, "segments");
            }
            segmentsList.add(segment);
        }
        return new BasicPath(segmentsList, parentPath.isAbsolute());
    }

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( Name segmentName ) {
        ArgCheck.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName);
    }

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( Name segmentName,
                                  int index ) {
        ArgCheck.isNotNull(segmentName, "segment name");
        if (Path.SELF_NAME.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT_NAME.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(segmentName, index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.PathFactory#createSegment(java.lang.String)
     */
    public Segment createSegment( String segmentName ) {
        return createSegment(segmentName, getDecoder());
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.spi.graph.PathFactory#createSegment(java.lang.String, org.jboss.dna.common.text.TextDecoder)
     */
    public Segment createSegment( String segmentName,
                                  TextDecoder decoder ) {
        ArgCheck.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        int startBracketNdx = segmentName.indexOf('[');
        if (startBracketNdx < 0) {
            return new BasicPathSegment(this.nameValueFactory.create(segmentName, decoder));
        }
        int endBracketNdx = segmentName.indexOf(']', startBracketNdx);
        if (endBracketNdx < 0) {
            throw new IllegalArgumentException(SpiI18n.missingEndBracketInSegmentName.text(segmentName));
        }
        String ndx = segmentName.substring(startBracketNdx + 1, endBracketNdx);
        try {
            return new BasicPathSegment(this.nameValueFactory.create(segmentName.substring(0, startBracketNdx), decoder),
                                        Integer.parseInt(ndx));
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException(SpiI18n.invalidIndexInSegmentName.text(ndx, segmentName));
        }
    }

    /**
     * {@inheritDoc}
     */
    public Segment createSegment( String segmentName,
                                  int index ) {
        ArgCheck.isNotNull(segmentName, "segment name");
        if (Path.SELF.equals(segmentName)) return Path.SELF_SEGMENT;
        if (Path.PARENT.equals(segmentName)) return Path.PARENT_SEGMENT;
        return new BasicPathSegment(this.nameValueFactory.create(segmentName), index);
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Reference value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
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
        throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(URI.class.getSimpleName(),
                                                                            Path.class.getSimpleName(),
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
     */
    public Path create( InputStream stream,
                        int approximateLength ) {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Path create( Reader reader,
                        int approximateLength ) {
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

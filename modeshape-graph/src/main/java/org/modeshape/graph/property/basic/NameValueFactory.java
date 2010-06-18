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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.IoException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceException;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#NAME} values.
 */
@Immutable
public class NameValueFactory extends AbstractValueFactory<Name> implements NameFactory {

    // Non-escaped pattern: (\{([^}]*)\})?(.*)
    protected static final String FULLY_QUALFIED_NAME_PATTERN_STRING = "\\{([^}]*)\\}(.*)";
    protected static final Pattern FULLY_QUALIFIED_NAME_PATTERN = Pattern.compile(FULLY_QUALFIED_NAME_PATTERN_STRING);

    // Original pattern: (([^:/]*):)?([^:]*)
    private static final String PREFIXED_NAME_PATTERN_STRING = "(([^:/]+):)?([^:]*)";
    private static final Pattern PREFIXED_NAME_PATTERN = Pattern.compile(PREFIXED_NAME_PATTERN_STRING);

    private static Name BLANK_NAME;
    private static Name ANY_NAME;

    private final NamespaceRegistry namespaceRegistry;

    public NameValueFactory( NamespaceRegistry namespaceRegistry,
                             TextDecoder decoder,
                             ValueFactory<String> stringValueFactory ) {
        super(PropertyType.NAME, decoder, stringValueFactory);
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.namespaceRegistry = namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public Name create( String value ) {
        return create(value, getDecoder());
    }

    /**
     * {@inheritDoc}
     */
    public Name create( String value,
                        TextDecoder decoder ) {
        if (value == null) return null;
        if (decoder == null) decoder = getDecoder();
        try {
            if (value.length() == 0) {
                if (BLANK_NAME == null) BLANK_NAME = new BasicName("", "");
                return BLANK_NAME;
            }
            char firstChar = value.charAt(0);
            if (value.length() == 1 && firstChar == '*') {
                if (ANY_NAME == null) ANY_NAME = new BasicName("", "*");
                return ANY_NAME;
            }
            if (firstChar != '{') {
                // First, see whether the value fits the prefixed name pattern ...
                Matcher matcher = PREFIXED_NAME_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String prefix = matcher.group(2);
                    String localName = matcher.group(3);
                    // Decode the parts ...
                    prefix = prefix == null ? "" : decoder.decode(prefix);
                    localName = decoder.decode(localName);
                    // Look for a namespace match ...
                    String namespaceUri = this.namespaceRegistry.getNamespaceForPrefix(prefix);
                    // Fail if no namespace is found ...
                    if (namespaceUri == null) {
                        throw new NamespaceException(GraphI18n.noNamespaceRegisteredForPrefix.text(prefix));
                    }
                    return new BasicName(namespaceUri, localName);
                }
            }
            // If it doesn't fit the prefixed pattern, then try the internal pattern
            Matcher matcher = FULLY_QUALIFIED_NAME_PATTERN.matcher(value);
            if (matcher.matches()) {
                String namespaceUri = matcher.group(1);
                String localName = matcher.group(2);
                // Decode the parts ...
                return create(namespaceUri, localName, decoder);
            }
        } catch (NamespaceException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              Name.class.getSimpleName(),
                                                                              value), err);
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( String namespaceUri,
                        String localName ) {
        return create(namespaceUri, localName, getDecoder());
    }

    /**
     * {@inheritDoc}
     */
    public Name create( String namespaceUri,
                        String localName,
                        TextDecoder decoder ) {
        CheckArg.isNotEmpty(localName, "localName");
        if (decoder == null) decoder = getDecoder();
        namespaceUri = namespaceUri != null ? decoder.decode(namespaceUri.trim()) : null;
        localName = decoder.decode(localName.trim());
        return new BasicName(namespaceUri, localName);
    }

    /**
     * {@inheritDoc}
     */
    public Name create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.DateTime)
     */
    public Name create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Name value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Path value ) {
        if (value == null) return null;
        if (!value.isAbsolute() && value.size() == 1) {
            // A relative name of length 1 is converted to a name
            Path.Segment segment = value.getLastSegment();
            // Can only convert if the path has no SNS index ...
            if (!segment.hasIndex()) return segment.getName();
        }
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.errorConvertingType.text(Path.class.getSimpleName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Path.Segment segment ) {
        if (segment == null) return null;
        // Can only convert if the path has no SNS index ...
        if (!segment.hasIndex()) return segment.getName();
        throw new ValueFormatException(segment, getPropertyType(),
                                       GraphI18n.errorConvertingType.text(Path.Segment.class.getSimpleName(),
                                                                          Name.class.getSimpleName(),
                                                                          segment));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( URI value ) {
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
     * @see org.modeshape.graph.property.ValueFactory#create(java.util.UUID)
     */
    public Name create( UUID value ) throws IoException {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.Binary)
     */
    public Name create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( InputStream stream,
                        long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Reader reader,
                        long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.modeshape.graph.property.NameFactory#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Name[] createEmptyArray( int length ) {
        return new Name[length];
    }
}

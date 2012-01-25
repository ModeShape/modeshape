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
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceException;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

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

    @Override
    public Name create( String value ) {
        return create(value, getDecoder());
    }

    @Override
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

    @Override
    public Name create( String namespaceUri,
                        String localName ) {
        return create(namespaceUri, localName, getDecoder());
    }

    @Override
    public Name create( String namespaceUri,
                        String localName,
                        TextDecoder decoder ) {
        CheckArg.isNotEmpty(localName, "localName");
        if (decoder == null) decoder = getDecoder();
        namespaceUri = namespaceUri != null ? decoder.decode(namespaceUri.trim()) : null;
        localName = decoder.decode(localName.trim());
        return new BasicName(namespaceUri, localName);
    }

    @Override
    public Name create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( Name value ) {
        return value;
    }

    @Override
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

    @Override
    public Name create( Path.Segment segment ) {
        if (segment == null) return null;
        // Can only convert if the path has no SNS index ...
        if (!segment.hasIndex()) return segment.getName();
        throw new ValueFormatException(segment, getPropertyType(),
                                       GraphI18n.errorConvertingType.text(Path.Segment.class.getSimpleName(),
                                                                          Name.class.getSimpleName(),
                                                                          segment));
    }

    @Override
    public Name create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    @Override
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

    @Override
    public Name create( UUID value ) throws IoException {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Name create( NodeKey value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          NodeKey.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Name create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Name create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Name create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    @Override
    protected Name[] createEmptyArray( int length ) {
        return new Name[length];
    }
}

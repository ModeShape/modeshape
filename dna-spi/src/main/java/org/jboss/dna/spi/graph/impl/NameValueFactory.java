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
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.NamespaceException;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#NAME} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class NameValueFactory extends AbstractValueFactory<Name> implements NameFactory {

    // Non-escaped pattern: (\{([^}]*)\})?(.*)
    protected static final String FULLY_QUALFIED_NAME_PATTERN_STRING = "\\{([^}]*)\\}(.*)";
    protected static final Pattern FULLY_QUALIFIED_NAME_PATTERN = Pattern.compile(FULLY_QUALFIED_NAME_PATTERN_STRING);

    // Original pattern: (([^:/]*):)?(.*)
    private static final String PREFIXED_NAME_PATTERN_STRING = "(([^:/]*):)?(.*)";
    private static final Pattern PREFIXED_NAME_PATTERN = Pattern.compile(PREFIXED_NAME_PATTERN_STRING);

    private final NamespaceRegistry namespaceRegistry;

    public NameValueFactory( NamespaceRegistry namespaceRegistry,
                             TextDecoder decoder,
                             ValueFactory<String> stringValueFactory ) {
        super(PropertyType.NAME, decoder, stringValueFactory);
        ArgCheck.isNotNull(namespaceRegistry, "namespaceRegistry");
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
            // First see whether the value fits the internal pattern ...
            Matcher matcher = FULLY_QUALIFIED_NAME_PATTERN.matcher(value);
            if (matcher.matches()) {
                String namespaceUri = matcher.group(1);
                String localName = matcher.group(2);
                // Decode the parts ...
                namespaceUri = decoder.decode(namespaceUri);
                localName = decoder.decode(localName);
                return new BasicName(namespaceUri, localName);
            }
            // Second, see whether the value fits the prefixed name pattern ...
            matcher = PREFIXED_NAME_PATTERN.matcher(value);
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
                    throw new NamespaceException(SpiI18n.noNamespaceRegisteredForPrefix.text(prefix));
                }
                return new BasicName(namespaceUri, localName);
            }
        } catch (NamespaceException err) {
            throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                Name.class.getSimpleName(),
                                                                                value), err);
        }
        throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(String.class.getSimpleName(),
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
        ArgCheck.isNotEmpty(localName, "localName");
        if (decoder == null) decoder = getDecoder();
        namespaceUri = namespaceUri != null ? decoder.decode(namespaceUri.trim()) : null;
        localName = decoder.decode(localName.trim());
        return new BasicName(namespaceUri, localName);
    }

    /**
     * {@inheritDoc}
     */
    public Name create( int value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( long value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( boolean value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( float value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( double value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( BigDecimal value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Calendar value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Date value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
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
            return value.getSegment(0).getName();
        }
        throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(Path.class.getSimpleName(),
                                                                            Name.class.getSimpleName(),
                                                                            value));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Reference value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
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
        throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(URI.class.getSimpleName(),
                                                                            Path.class.getSimpleName(),
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
     */
    public Name create( InputStream stream,
                        int approximateLength ) {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Name create( Reader reader,
                        int approximateLength ) {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.jboss.dna.spi.graph.NameFactory#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }
}

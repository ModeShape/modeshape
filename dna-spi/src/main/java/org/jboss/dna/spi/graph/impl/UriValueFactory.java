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
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.IoException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#URI} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class UriValueFactory extends AbstractValueFactory<URI> {

    private final NamespaceRegistry namespaceRegistry;

    public UriValueFactory( NamespaceRegistry namespaceRegistry,
                            TextDecoder decoder,
                            ValueFactory<String> stringValueFactory ) {
        super(PropertyType.URI, decoder, stringValueFactory);
        ArgCheck.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.namespaceRegistry = namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public URI create( String value ) {
        if (value == null) return null;
        try {
            return new URI(value);
        } catch (URISyntaxException err) {
            throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                URI.class.getSimpleName(),
                                                                                value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URI create( String value,
                       TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( int value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( long value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( boolean value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( float value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( double value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( BigDecimal value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Calendar value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Date value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Date.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Name value ) {
        if (value == null) return null;
        return create("./" + value.getString(this.namespaceRegistry));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Path value ) {
        if (value == null) return null;
        if (value.isAbsolute()) {
            return create("/" + value.getString(this.namespaceRegistry));
        }
        return create("./" + value.getString(this.namespaceRegistry));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Reference value ) {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 Reference.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.ValueFactory#create(java.util.UUID)
     */
    public URI create( UUID value ) throws IoException {
        throw new UnsupportedOperationException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                 UUID.class.getSimpleName(),
                                                                                 value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( URI value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public URI create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( InputStream stream,
                       int approximateLength ) {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public URI create( Reader reader,
                       int approximateLength ) {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URI[] createEmptyArray( int length ) {
        return new URI[length];
    }

}

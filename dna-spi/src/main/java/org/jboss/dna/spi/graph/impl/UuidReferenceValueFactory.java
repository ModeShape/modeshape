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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#REFERENCE} values.
 * 
 * @author Randall Hauch
 */
@Immutable
public class UuidReferenceValueFactory extends AbstractValueFactory<Reference> {

    public UuidReferenceValueFactory( TextDecoder decoder, ValueFactory<String> stringValueFactory ) {
        super(PropertyType.REFERENCE, decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value ) throws ValueFormatException {
        if (value == null) return null;
        try {
            UUID uuid = UUID.fromString(value);
            return new UuidReference(uuid);
        } catch (IllegalArgumentException t) {
            throw new ValueFormatException(SpiI18n.errorCreatingValue.text(getPropertyType().getName(), String.class.getSimpleName(), value), t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value, TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( int value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( long value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( boolean value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( float value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( double value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( BigDecimal value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Calendar value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Date value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Name value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Path value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reference value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( URI value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Date.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( byte[] value ) throws ValueFormatException {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( InputStream stream, int approximateLength ) throws IOException, ValueFormatException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reader reader, int approximateLength ) throws IOException, ValueFormatException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

}

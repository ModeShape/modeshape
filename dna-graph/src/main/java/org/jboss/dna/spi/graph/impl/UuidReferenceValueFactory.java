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
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.DateTime;
import org.jboss.dna.spi.graph.IoException;
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
 * @author John Verhaeg
 */
@Immutable
public class UuidReferenceValueFactory extends AbstractValueFactory<Reference> {

    public UuidReferenceValueFactory( TextDecoder decoder,
                                      ValueFactory<String> stringValueFactory ) {
        super(PropertyType.REFERENCE, decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value ) {
        if (value == null) return null;
        try {
            UUID uuid = UUID.fromString(value);
            return new UuidReference(uuid);
        } catch (IllegalArgumentException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           SpiI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                            Reference.class.getSimpleName(),
                                                                            value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value,
                             TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( int value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Integer.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Long.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Boolean.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Float.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Double.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                        BigDecimal.class.getSimpleName(),
                                                                        value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Calendar.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Date.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.ValueFactory#create(org.jboss.dna.spi.graph.DateTime)
     */
    public Reference create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  DateTime.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Name.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Path.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reference value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.ValueFactory#create(java.util.UUID)
     */
    public Reference create( UUID value ) {
        if (value == null) return null;
        return new UuidReference(value);
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), SpiI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Date.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.ValueFactory#create(org.jboss.dna.spi.graph.Binary)
     */
    public Reference create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( InputStream stream,
                             long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reader reader,
                             long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Reference[] createEmptyArray( int length ) {
        return new Reference[length];
    }

}

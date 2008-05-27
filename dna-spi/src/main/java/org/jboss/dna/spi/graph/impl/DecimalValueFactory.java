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
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#DECIMAL} values.
 * @author Randall Hauch
 */
@Immutable
public class DecimalValueFactory extends AbstractValueFactory<BigDecimal> {

    public DecimalValueFactory( TextEncoder encoder, ValueFactory<String> stringValueFactory ) {
        super(PropertyType.DECIMAL, encoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( String value ) throws ValueFormatException {
        if (value == null) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ValueFormatException(SpiI18n.errorCreatingValue.text(getPropertyType().getName(), String.class.getSimpleName(), value), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( String value, TextEncoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getEncoder(decoder).decode(value.trim()));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( int value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( long value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( boolean value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Boolean.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( float value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( double value ) {
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( BigDecimal value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Calendar value ) {
        if (value == null) return null;
        return create(value.getTimeInMillis());
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Date value ) {
        if (value == null) return null;
        return create(value.getTime());
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Name value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Name.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Path value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Path.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Reference value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), Reference.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( URI value ) throws ValueFormatException {
        throw new ValueFormatException(SpiI18n.unableToCreateValue.text(getPropertyType().getName(), URI.class.getSimpleName(), value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( byte[] value ) throws ValueFormatException {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( InputStream stream, int approximateLength ) throws IOException, ValueFormatException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal create( Reader reader, int approximateLength ) throws IOException, ValueFormatException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

}

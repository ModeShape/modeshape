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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.ValueFormatException;

/**
 * Teh standard {@link ValueFactory} for {@link PropertyType#BINARY} values.
 * @author Randall Hauch
 */
@Immutable
public class InMemoryBinaryValueFactory extends AbstractValueFactory<Binary> {

    private static final String CHAR_SET_NAME = "UTF-8";

    public InMemoryBinaryValueFactory( TextEncoder encoder, ValueFactory<String> stringValueFactory ) {
        super(PropertyType.BINARY, encoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( String value ) throws ValueFormatException {
        if (value == null) return null;
        try {
            return create(value.getBytes(CHAR_SET_NAME));
        } catch (UnsupportedEncodingException e) {
            throw new ValueFormatException(SpiI18n.errorConvertingBinaryValueToString.text());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( String value, TextEncoder decoder ) throws ValueFormatException {
        if (value == null) return null;
        return create(getEncoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( int value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( long value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( boolean value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( float value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( double value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( BigDecimal value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Calendar value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Date value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Name value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Path value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Reference value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( URI value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( byte[] value ) throws ValueFormatException {
        return new InMemoryBinary(value);
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( InputStream stream, int approximateLength ) throws IOException {
        if (stream == null) return null;
        byte[] value = IoUtil.readBytes(stream);
        return create(value);
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( Reader reader, int approximateLength ) throws IOException {
        if (reader == null) return null;
        // Convert the value to a string, then to a binary ...
        String value = IoUtil.read(reader);
        return create(this.getStringValueFactory().create(value));
    }

}

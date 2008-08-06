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
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.spi.SpiI18n;
import org.jboss.dna.spi.graph.IoException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#STRING} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class StringValueFactory extends AbstractValueFactory<String> {

    private final TextEncoder encoder;

    public StringValueFactory( TextDecoder decoder,
                               TextEncoder encoder ) {
        super(PropertyType.STRING, decoder, null);
        ArgCheck.isNotNull(encoder, "encoder");
        this.encoder = encoder;
    }

    /**
     * @return encoder
     */
    public TextEncoder getEncoder() {
        return this.encoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueFactory<String> getStringValueFactory() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public String create( String value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public String create( String value,
                          TextDecoder decoder ) {
        if (value == null) return value;
        if (decoder == null) decoder = getDecoder();
        return decoder.decode(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( int value ) {
        return Integer.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( long value ) {
        return Long.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( boolean value ) {
        return Boolean.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( float value ) {
        return Float.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( double value ) {
        return Double.toString(value);
    }

    /**
     * {@inheritDoc}
     */
    public String create( BigDecimal value ) {
        if (value == null) return null;
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String create( Calendar value ) {
        if (value == null) return null;
        return new JodaDateTime(value).getString();
    }

    /**
     * {@inheritDoc}
     */
    public String create( Date value ) {
        if (value == null) return null;
        return new JodaDateTime(value).getString();
    }

    /**
     * {@inheritDoc}
     */
    public String create( Name value ) {
        if (value == null) return null;
        return value.getString(getEncoder());
    }

    /**
     * {@inheritDoc}
     */
    public String create( Path value ) {
        if (value == null) return null;
        return value.getString(getEncoder());
    }

    /**
     * {@inheritDoc}
     */
    public String create( Reference value ) {
        if (value == null) return null;
        return value.getString(getEncoder());
    }

    /**
     * {@inheritDoc}
     */
    public String create( URI value ) {
        if (value == null) return null;
        return value.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.ValueFactory#create(java.util.UUID)
     */
    public String create( UUID value ) throws IoException {
        if (value == null) return null;
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String create( byte[] value ) {
        if (value == null) return null;
        try {
            return new String(value, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(byte[].class.getSimpleName(),
                                                                                String.class.getSimpleName(),
                                                                                value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String create( InputStream stream,
                          int approximateLength ) {
        if (stream == null) return null;
        byte[] value = null;
        try {
            value = IoUtil.readBytes(stream);
            return new String(value, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalArgumentException(SpiI18n.errorConvertingType.text(InputStream.class.getSimpleName(),
                                                                                String.class.getSimpleName(),
                                                                                value), err);
        } catch (IOException err) {
            throw new IoException(
                                  SpiI18n.errorConvertingIo.text(InputStream.class.getSimpleName(), String.class.getSimpleName()),
                                  err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String create( Reader reader,
                          int approximateLength ) {
        if (reader == null) return null;
        try {
            return IoUtil.read(reader);
        } catch (IOException err) {
            throw new IoException(SpiI18n.errorConvertingIo.text(Reader.class.getSimpleName(), String.class.getSimpleName()), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] createEmptyArray( int length ) {
        return new String[length];
    }

}

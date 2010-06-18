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
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.IoException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#STRING} values.
 */
@Immutable
public class StringValueFactory extends AbstractValueFactory<String> {

    private final TextEncoder encoder;
    private final NamespaceRegistry namespaceRegistry;

    public StringValueFactory( TextDecoder decoder,
                               TextEncoder encoder ) {
        super(PropertyType.STRING, decoder, null);
        CheckArg.isNotNull(encoder, "encoder");
        this.encoder = encoder;
        this.namespaceRegistry = null;
    }

    public StringValueFactory( NamespaceRegistry namespaceRegistry,
                               TextDecoder decoder,
                               TextEncoder encoder ) {
        super(PropertyType.STRING, decoder, null);
        CheckArg.isNotNull(encoder, "encoder");
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.encoder = encoder;
        this.namespaceRegistry = namespaceRegistry;
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
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.DateTime)
     */
    public String create( DateTime value ) throws ValueFormatException {
        if (value == null) return null;
        return value.getString(); // ISO representation
    }

    /**
     * {@inheritDoc}
     */
    public String create( Name value ) {
        if (value == null) return null;
        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
        return value.getString(getEncoder());
    }

    /**
     * {@inheritDoc}
     */
    public String create( Path value ) {
        if (value == null) return null;
        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
        return value.getString(getEncoder());
    }

    /**
     * {@inheritDoc}
     */
    public String create( Path.Segment value ) {
        if (value == null) return null;
        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
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
     * @see org.modeshape.graph.property.ValueFactory#create(java.util.UUID)
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
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(byte[].class.getSimpleName(),
                                                                              String.class.getSimpleName(),
                                                                              value), err);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.Binary)
     */
    public String create( Binary value ) throws ValueFormatException, IoException {
        if (value == null) return null;
        try {
            value.acquire();
            InputStream stream = value.getStream();
            try {
                return create(stream, value.getSize());
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    Logger.getLogger(getClass()).debug(e, "Error closing the stream while converting from Binary to String");
                }
            }
        } finally {
            value.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String create( InputStream stream,
                          long approximateLength ) throws IoException {
        if (stream == null) return null;
        byte[] value = null;
        try {
            value = IoUtil.readBytes(stream);
            return new String(value, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(InputStream.class.getSimpleName(),
                                                                              String.class.getSimpleName(),
                                                                              value), err);
        } catch (IOException err) {
            throw new IoException(GraphI18n.errorConvertingIo.text(InputStream.class.getSimpleName(),
                                                                   String.class.getSimpleName()), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String create( Reader reader,
                          long approximateLength ) throws IoException {
        if (reader == null) return null;
        try {
            return IoUtil.read(reader);
        } catch (IOException err) {
            throw new IoException(GraphI18n.errorConvertingIo.text(Reader.class.getSimpleName(), String.class.getSimpleName()),
                                  err);
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

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

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

    @Override
    protected ValueFactory<String> getStringValueFactory() {
        return this;
    }

    @Override
    public String create( String value ) {
        return value;
    }

    @Override
    public String create( String value,
                          TextDecoder decoder ) {
        if (value == null) return value;
        if (decoder == null) decoder = getDecoder();
        return decoder.decode(value);
    }

    @Override
    public String create( int value ) {
        return Integer.toString(value);
    }

    @Override
    public String create( long value ) {
        return Long.toString(value);
    }

    @Override
    public String create( boolean value ) {
        return Boolean.toString(value);
    }

    @Override
    public String create( float value ) {
        return Float.toString(value);
    }

    @Override
    public String create( double value ) {
        return Double.toString(value);
    }

    @Override
    public String create( BigDecimal value ) {
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public String create( Calendar value ) {
        if (value == null) return null;
        return new JodaDateTime(value).getString();
    }

    @Override
    public String create( Date value ) {
        if (value == null) return null;
        return new JodaDateTime(value).getString();
    }

    @Override
    public String create( DateTime value ) throws ValueFormatException {
        if (value == null) return null;
        return value.getString(); // ISO representation
    }

    @Override
    public String create( Name value ) {
        if (value == null) return null;
        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
        return value.getString(getEncoder());
    }

    @Override
    public String create( Path value ) {
        if (value == null) return null;
        if (value.isIdentifier()) {
            // Get the identifier segment ...
            Segment segment = value.getLastSegment();
            assert segment.isIdentifier();
            try {
                // The local part of the segment's name should be the identifier ...
                return segment.getString(getEncoder());
            } catch (IllegalArgumentException err) {
                throw new ValueFormatException(value, PropertyType.UUID,
                                               GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                  Path.class.getSimpleName(),
                                                                                  value));
            }
        }

        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
        return value.getString(getEncoder());
    }

    @Override
    public String create( Path.Segment value ) {
        if (value == null) return null;
        if (value.isIdentifier()) {
            try {
                // The local part of the segment's name should be the identifier, though it may not be a UUID ...
                return value.getName().getLocalName();
            } catch (IllegalArgumentException err) {
                throw new ValueFormatException(value, PropertyType.UUID,
                                               GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                  Path.Segment.class.getSimpleName(),
                                                                                  value));
            }
        }
        if (this.namespaceRegistry != null) {
            return value.getString(this.namespaceRegistry, getEncoder());
        }
        return value.getString(getEncoder());
    }

    @Override
    public String create( Reference value ) {
        if (value == null) return null;
        return value.getString(getEncoder());
    }

    @Override
    public String create( URI value ) {
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public String create( UUID value ) throws IoException {
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public String create( NodeKey value ) throws ValueFormatException {
        if (value == null) return null;
        return value.toString();
    }

    @Override
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

    @Override
    public String create( BinaryValue value ) throws ValueFormatException, IoException {
        if (value == null) return null;
        try {
            InputStream stream = value.getStream();
            try {
                return create(stream);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    Logger.getLogger(getClass()).debug(e, "Error closing the stream while converting from Binary to String");
                }
            }
        } catch (RepositoryException e) {
            throw new IoException(e);
        } finally {
            value.dispose();
        }
    }

    @Override
    public String create( InputStream stream ) throws IoException {
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

    @Override
    protected String[] createEmptyArray( int length ) {
        return new String[length];
    }

}

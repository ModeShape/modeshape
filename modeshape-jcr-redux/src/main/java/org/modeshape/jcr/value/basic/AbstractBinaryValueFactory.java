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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An abstract {@link BinaryFactory} implementation that contains many general methods that are likely to be appropriate for many
 * concrete implementations.
 */
@Immutable
public abstract class AbstractBinaryValueFactory extends AbstractValueFactory<Binary> implements BinaryFactory {

    private static final String CHAR_SET_NAME = "UTF-8";

    protected AbstractBinaryValueFactory( TextDecoder decoder,
                                          ValueFactory<String> stringValueFactory ) {
        super(PropertyType.BINARY, decoder, stringValueFactory);
    }

    @Override
    public Binary create( String value ) {
        if (value == null) return null;
        try {
            return create(value.getBytes(CHAR_SET_NAME));
        } catch (UnsupportedEncodingException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              Binary.class.getSimpleName(),
                                                                              value), err);
        }
    }

    @Override
    public Binary create( String value,
                          TextDecoder decoder ) {
        if (value == null) return null;
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public Binary create( int value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( long value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( boolean value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( float value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( double value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( BigDecimal value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Calendar value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Date value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( DateTime value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Name value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Path value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Path.Segment value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Reference value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( URI value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( UUID value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( NodeKey value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public Binary create( Binary value ) throws ValueFormatException, IoException {
        return value;
    }

    @Override
    public Binary create( InputStream stream,
                          long approximateLength ) throws IoException {
        if (stream == null) return null;
        if (stream instanceof FileInputStream) {
            return new FileInputStreamBinary((FileInputStream)stream);
        }
        try {
            byte[] value = IoUtil.readBytes(stream);
            return create(value);
        } catch (IOException err) {
            throw new IoException(GraphI18n.errorConvertingIo.text(InputStream.class.getSimpleName(),
                                                                   Binary.class.getSimpleName()), err);
        }
    }

    @Override
    public Binary create( Reader reader,
                          long approximateLength ) throws IoException {
        if (reader == null) return null;
        // Convert the value to a string, then to a binary ...
        try {
            String value = IoUtil.read(reader);
            return create(this.getStringValueFactory().create(value));
        } catch (IOException err) {
            throw new IoException(GraphI18n.errorConvertingIo.text(Reader.class.getSimpleName(), Binary.class.getSimpleName()),
                                  err);
        }
    }

    @Override
    public Binary create( File file ) throws ValueFormatException, IoException {
        if (file == null) return null;
        if (!file.canRead()) return null;
        if (!file.isFile()) return null;
        try {
            byte[] value = IoUtil.readBytes(file);
            return create(value);
        } catch (IOException err) {
            throw new IoException(GraphI18n.errorConvertingIo.text(file, Binary.class.getSimpleName()), err);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does not manage or share the in-memory binary values, so this implementation is identical to calling
     * {@link #create(Reader, long)}.
     * </p>
     * 
     * @see org.modeshape.jcr.value.BinaryFactory#create(java.io.Reader, long, byte[])
     */
    @Override
    public Binary create( Reader reader,
                          long approximateLength,
                          byte[] secureHash ) throws ValueFormatException, IoException {
        return create(reader, approximateLength);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does not manage or share the in-memory binary values, so this implementation is identical to calling
     * {@link #create(InputStream, long)}.
     * </p>
     * 
     * @see org.modeshape.jcr.value.BinaryFactory#create(java.io.InputStream, long, byte[])
     */
    @Override
    public Binary create( InputStream stream,
                          long approximateLength,
                          byte[] secureHash ) throws ValueFormatException, IoException {
        return create(stream, approximateLength);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns null, since the in-memory binary values are not managed or shared.
     * </p>
     * 
     * @see org.modeshape.jcr.value.BinaryFactory#find(byte[])
     */
    @Override
    public Binary find( byte[] secureHash ) {
        return null;
    }

    @Override
    protected Binary[] createEmptyArray( int length ) {
        return new Binary[length];
    }
}

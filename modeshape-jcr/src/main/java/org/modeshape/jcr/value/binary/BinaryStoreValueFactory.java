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
package org.modeshape.jcr.value.binary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;
import org.modeshape.jcr.value.basic.AbstractValueFactory;

/**
 * An abstract {@link BinaryFactory} implementation that contains many general methods that are likely to be appropriate for many
 * concrete implementations.
 */
@Immutable
public class BinaryStoreValueFactory extends AbstractValueFactory<BinaryValue> implements BinaryFactory {

    private static final String CHAR_SET_NAME = "UTF-8";
    private static final BinaryValue[] EMPTY_BINARY_ARRAY = new BinaryValue[] {};

    private final BinaryStore store;

    /**
     * Create a factory instance that finds persisted binary values in the supplied store, and that uses the supplied decoder and
     * string value factory to convert string values into binary values.
     * 
     * @param store the binary store; may not be null
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param stringValueFactory the string value factory; may not be null
     */
    public BinaryStoreValueFactory( BinaryStore store,
                                    TextDecoder decoder,
                                    ValueFactory<String> stringValueFactory ) {
        super(PropertyType.BINARY, decoder, stringValueFactory);
        this.store = store;
    }

    /**
     * Return a new binary value factory that is identical to this store but which uses the supplied store.
     * 
     * @param store the binary store; may not be null
     * @return the new binary value factory; never null
     */
    public BinaryStoreValueFactory with( BinaryStore store ) {
        return new BinaryStoreValueFactory(store, super.getDecoder(), super.getStringValueFactory());
    }

    @Override
    protected BinaryValue[] createEmptyArray( int length ) {
        return EMPTY_BINARY_ARRAY;
    }

    @Override
    public BinaryValue create( String value ) {
        if (value == null) return null;
        try {
            return create(value.getBytes(CHAR_SET_NAME));
        } catch (UnsupportedEncodingException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              BinaryValue.class.getSimpleName(),
                                                                              value), err);
        }
    }

    @Override
    public BinaryValue create( String value,
                          TextDecoder decoder ) {
        if (value == null) return null;
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public BinaryValue create( int value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( long value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( boolean value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( float value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( double value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( BigDecimal value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Calendar value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Date value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( DateTime value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Name value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Path value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Path.Segment value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( Reference value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( URI value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( UUID value ) {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( NodeKey value ) throws ValueFormatException {
        // Convert the value to a string, then to a binary ...
        return create(this.getStringValueFactory().create(value));
    }

    @Override
    public BinaryValue create( BinaryValue value ) throws ValueFormatException, IoException {
        return value;
    }

    @Override
    public BinaryValue create( byte[] value ) throws ValueFormatException {
        if (value.length <= store.getMinimumBinarySizeInBytes()) {
            // It's small enough, so just create an in-memory value ...
            return new InMemoryBinaryValue(store, value);
        }
        try {
            // Store the value in the store ...
            return store.storeValue(new ByteArrayInputStream(value));
        } catch (BinaryStoreException e) {
            throw new ValueFormatException(e, PropertyType.BINARY);
        }
    }

    @Override
    public BinaryValue create( InputStream stream ) throws IoException {
        if (stream == null) return null;
        try {
            // Store the value in the store ...
            return store.storeValue(stream);
        } catch (BinaryStoreException e) {
            throw new ValueFormatException(e, PropertyType.BINARY);
        }
    }

    @SuppressWarnings( "unused" )
    @Override
    public BinaryValue find( BinaryKey secureHash,
                        long size ) throws BinaryStoreException {
        // In-memory binaries never need to be found, so it must be stored ...
        return new StoredBinaryValue(store, secureHash, size);
    }
}

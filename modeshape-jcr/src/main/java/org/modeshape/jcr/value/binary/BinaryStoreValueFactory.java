/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
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

    private final BinaryStore store;
    private final ValueFactory<String> stringFactory;

    /**
     * Create a factory instance that finds persisted binary values in the supplied store, and that uses the supplied decoder and
     * string value factory to convert string values into binary values.
     * 
     * @param store the binary store; may not be null
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param factories the set of value factories, used to obtain the {@link ValueFactories#getStringFactory() string value
     *        factory}; may not be null
     * @param stringFactory the optional string factory that should be used in place of the one in the supplied ValueFactories
     *        parameter; may be null
     */
    public BinaryStoreValueFactory( BinaryStore store,
                                    TextDecoder decoder,
                                    ValueFactories factories,
                                    ValueFactory<String> stringFactory ) {
        super(PropertyType.BINARY, decoder, factories);
        CheckArg.isNotNull(store, "store");
        this.store = store;
        this.stringFactory = stringFactory;
    }

    @Override
    public BinaryFactory with( ValueFactories valueFactories ) {
        return super.valueFactories == valueFactories ? this : new BinaryStoreValueFactory(store, super.getDecoder(),
                                                                                           valueFactories, stringFactory);
    }

    @Override
    public BinaryStoreValueFactory with( BinaryStore store ) {
        if (this.store == store) return this;
        return new BinaryStoreValueFactory(store, super.getDecoder(), super.valueFactories, stringFactory);
    }

    @Override
    protected ValueFactory<String> getStringValueFactory() {
        return stringFactory != null ? stringFactory : super.getStringValueFactory();
    }

    @Override
    public BinaryValue[] createEmptyArray( int length ) {
        return new BinaryValue[length];
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
        if (value == null) return null;
        if (value.length <= store.getMinimumBinarySizeInBytes()) {
            // It's small enough, so just create an in-memory value ...
            return new InMemoryBinaryValue(store, value);
        }
        try {
            // Store the value in the store but mark it as unused, as it will become used only on tx commit
            return store.storeValue(new ByteArrayInputStream(value), true);
        } catch (BinaryStoreException e) {
            throw new ValueFormatException(PropertyType.BINARY,
                                           GraphI18n.errorConvertingType.text(byte[].class.getSimpleName(),
                                                                              BinaryValue.class.getSimpleName(),
                                                                              value), e);
        }
    }

    @Override
    public BinaryValue create( InputStream stream ) throws IoException {
        if (stream == null) return null;
        try {
            // Store the value in the store but mark it as unused, as it will become used only on tx commit
            return store.storeValue(stream, true);
        } catch (BinaryStoreException e) {
            throw new ValueFormatException(PropertyType.BINARY,
                                           GraphI18n.errorConvertingIo.text(InputStream.class.getSimpleName(),
                                                                            BinaryValue.class.getSimpleName()), e);
        }
    }

    @Override
    public BinaryValue create( InputStream stream, String hint ) throws IoException {
        if (stream == null) return null;
        try {
            // Store the value in the store but mark it as unused, as it will become used only on tx commit
            return store.storeValue(stream, hint, true);
        } catch (BinaryStoreException e) {
            throw new ValueFormatException(PropertyType.BINARY,
                    GraphI18n.errorConvertingIo.text(InputStream.class.getSimpleName(),
                            BinaryValue.class.getSimpleName()), e);
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

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
package org.modeshape.jcr.value.basic;

import java.io.InputStream;
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
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#URI} values.
 */
@Immutable
public class UuidValueFactory extends AbstractValueFactory<UUID> implements UuidFactory {

    /**
     * Create a new instance.
     * 
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param factories the set of value factories, used to obtain the {@link ValueFactories#getStringFactory() string value
     *        factory}; may not be null
     */
    public UuidValueFactory( TextDecoder decoder,
                             ValueFactories factories ) {
        super(PropertyType.UUID, decoder, factories);
    }

    @Override
    public UuidFactory with( ValueFactories valueFactories ) {
        return super.valueFactories == valueFactories ? this : new UuidValueFactory(super.getDecoder(), valueFactories);
    }

    @Override
    public UUID create() {
        return UUID.randomUUID();
    }

    @Override
    public UUID create( String value ) {
        if (value == null) return null;
        value = value.trim();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException err) {
            throw new ValueFormatException(value, PropertyType.UUID,
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              URI.class.getSimpleName(),
                                                                              value), err);
        }
    }

    @Override
    public UUID create( String value,
                        TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public UUID create( int value ) {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( long value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( boolean value ) {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( float value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( double value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( BigDecimal value ) {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( Calendar value ) {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( Date value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( Name value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( Path value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( Path.Segment value ) {
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Path.Segment.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( Reference value ) {
        if (value instanceof UuidReference) {
            UuidReference ref = (UuidReference)value;
            return ref.getUuid();
        }
        throw new ValueFormatException(value, PropertyType.UUID,
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public UUID create( URI value ) {
        throw new ValueFormatException(value, PropertyType.UUID, GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    URI.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public UUID create( UUID value ) {
        return value;
    }

    @Override
    public UUID create( NodeKey value ) throws ValueFormatException {
        if (value == null) return null;
        try {
            return UUID.fromString(value.getIdentifier());
        } catch (IllegalArgumentException e) {
            throw new ValueFormatException(value, PropertyType.UUID,
                                           GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                              NodeKey.class.getSimpleName(),
                                                                              value));
        }
    }

    @Override
    public UUID create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public UUID create( BinaryValue value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public UUID create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    public UUID[] createEmptyArray( int length ) {
        return new UUID[length];
    }
}

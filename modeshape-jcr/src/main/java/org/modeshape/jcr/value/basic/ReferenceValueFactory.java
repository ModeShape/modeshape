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
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#REFERENCE} values.
 */
@Immutable
public class ReferenceValueFactory extends AbstractValueFactory<Reference> implements ReferenceFactory {

    protected final boolean weak;
    protected final boolean simple;

    /**
     * Create a new instance.
     * 
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param factories the set of value factories, used to obtain the {@link ValueFactories#getStringFactory() string value
     *        factory}; may not be null
     * @param weak true if this factory should create weak references, or false if it should create strong references
     * @param simple true if this factory should create simple references, false otherwise
     * @return the new reference factory; never null
     */
    public static ReferenceValueFactory newInstance( TextDecoder decoder,
                                                     ValueFactories factories,
                                                     boolean weak,
                                                     boolean simple ) {
        if (simple) {
            return new ReferenceValueFactory(PropertyType.SIMPLEREFERENCE, decoder, factories, weak, simple);
        }
        return new ReferenceValueFactory(weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE, decoder, factories, weak,
                                         simple);
    }

    protected ReferenceValueFactory( PropertyType type,
                                     TextDecoder decoder,
                                     ValueFactories valueFactories,
                                     boolean weak,
                                     boolean simple ) {
        super(type, decoder, valueFactories);
        this.weak = weak;
        this.simple = simple;
    }

    @Override
    public ReferenceFactory with( ValueFactories valueFactories ) {
        return super.valueFactories == valueFactories ? this : new ReferenceValueFactory(super.getPropertyType(),
                                                                                         super.getDecoder(), valueFactories,
                                                                                         weak, simple);
    }

    @Override
    public Reference create( String value ) {
        if (value == null) {
            return null;
        }
        if (!NodeKey.isValidFormat(value)) {
            // references should only be created from node keys
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                              String.class.getSimpleName(),
                                                                              value));
        }
        return new NodeKeyReference(new NodeKey(value), weak, false, simple);
    }

    @Override
    public Reference create( String value,
                             TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public Reference create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( Path.Segment value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Path.Segment.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( Reference value ) {
        return value;
    }

    @Override
    public Reference create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          UUID.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Reference create( NodeKey value ) throws ValueFormatException {
        return new NodeKeyReference(value, weak, false, simple);
    }

    @Override
    public Reference create( NodeKey value,
                             boolean foreign ) throws ValueFormatException {
        return new NodeKeyReference(value, weak, foreign, simple);
    }

    @Override
    public Reference[] create( NodeKey[] values,
                               boolean foreign ) throws ValueFormatException {
        if (values == null) {
            return null;
        }
        final int length = values.length;
        Reference[] result = createEmptyArray(length);
        for (int i = 0; i != length; ++i) {
            result[i] = create(values[i], foreign);
        }
        return result;
    }

    @Override
    public Reference create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Reference create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Reference create( BinaryValue value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Reference create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    public Reference[] createEmptyArray( int length ) {
        return new Reference[length];
    }

}

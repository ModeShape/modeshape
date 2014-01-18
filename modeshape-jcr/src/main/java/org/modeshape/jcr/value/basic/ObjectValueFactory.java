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
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#OBJECT} values.
 */
@Immutable
public class ObjectValueFactory extends AbstractValueFactory<Object> {

    /**
     * Create a new instance.
     * 
     * @param decoder the text decoder; may be null if the default decoder should be used
     * @param factories the set of value factories, used to obtain the {@link ValueFactories#getStringFactory() string value
     *        factory}; may not be null
     */
    public ObjectValueFactory( TextDecoder decoder,
                               ValueFactories factories ) {
        super(PropertyType.OBJECT, decoder, factories);
    }

    @Override
    public ValueFactory<Object> with( ValueFactories valueFactories ) {
        return super.valueFactories == valueFactories ? this : new ObjectValueFactory(super.getDecoder(), valueFactories);
    }

    /**
     * @return binaryValueFactory
     */
    protected final ValueFactory<BinaryValue> getBinaryValueFactory() {
        return valueFactories.getBinaryFactory();
    }

    @Override
    public Object create( String value ) {
        return this.getStringValueFactory().create(value);
    }

    @Override
    public Object create( String value,
                          TextDecoder decoder ) {
        return this.getStringValueFactory().create(value, decoder);
    }

    @Override
    public Object create( int value ) {
        return Integer.valueOf(value);
    }

    @Override
    public Object create( long value ) {
        return Long.valueOf(value);
    }

    @Override
    public Object create( boolean value ) {
        return Boolean.valueOf(value);
    }

    @Override
    public Object create( float value ) {
        return Float.valueOf(value);
    }

    @Override
    public Object create( double value ) {
        return Double.valueOf(value);
    }

    @Override
    public Object create( BigDecimal value ) {
        return value;
    }

    @Override
    public Object create( Calendar value ) {
        return value;
    }

    @Override
    public Object create( Date value ) {
        return value;
    }

    @Override
    public Object create( DateTime value ) {
        return value;
    }

    @Override
    public Object create( Name value ) {
        return value;
    }

    @Override
    public Object create( Path value ) {
        return value;
    }

    @Override
    public Object create( Path.Segment value ) {
        return value;
    }

    @Override
    public Object create( Reference value ) {
        return value;
    }

    @Override
    public Object create( URI value ) {
        return value;
    }

    @Override
    public Object create( UUID value ) {
        return value;
    }

    @Override
    public Object create( NodeKey value ) throws ValueFormatException {
        return value;
    }

    @Override
    public Object create( Object value ) {
        return value;
    }

    @Override
    public Object[] create( Object[] values ) {
        return values;
    }

    @Override
    public Object create( byte[] value ) {
        return getBinaryValueFactory().create(value);
    }

    @Override
    public Object create( BinaryValue value ) throws ValueFormatException, IoException {
        return value;
    }

    @Override
    public Object create( InputStream stream ) {
        return getBinaryValueFactory().create(stream);
    }

    @Override
    public Object[] createEmptyArray( int length ) {
        return new Object[length];
    }
}

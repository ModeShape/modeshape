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

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#DOUBLE} values.
 */
@Immutable
public class DoubleValueFactory extends AbstractValueFactory<Double> {

    public DoubleValueFactory( TextDecoder decoder,
                               ValueFactory<String> stringValueFactory ) {
        super(PropertyType.DOUBLE, decoder, stringValueFactory);
    }

    @Override
    public Double create( String value ) {
        if (value == null) return null;
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                              Double.class.getSimpleName(),
                                                                              value), err);
        }
    }

    @Override
    public Double create( String value,
                          TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public Double create( int value ) {
        return Double.valueOf(value);
    }

    @Override
    public Double create( long value ) {
        return new Double(value);
    }

    @Override
    public Double create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Double create( float value ) {
        return Double.valueOf(value);
    }

    @Override
    public Double create( double value ) {
        return value;
    }

    @Override
    public Double create( BigDecimal value ) {
        if (value == null) return null;
        double result = value.doubleValue();
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(BigDecimal.class.getSimpleName(),
                                                                              Double.class.getSimpleName(),
                                                                              value));
        }
        return result;
    }

    @Override
    public Double create( Calendar value ) {
        if (value == null) return null;
        return create(value.getTimeInMillis());
    }

    @Override
    public Double create( Date value ) {
        if (value == null) return null;
        return create(value.getTime());
    }

    @Override
    public Double create( DateTime value ) throws ValueFormatException {
        if (value == null) return null;
        return create(value.getMilliseconds());
    }

    @Override
    public Double create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Double create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Double create( Path.Segment value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Path.Segment.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Double create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Double create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    URI.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Double create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public Double create( NodeKey value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          NodeKey.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public Double create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Double create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public Double create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    protected Double[] createEmptyArray( int length ) {
        return new Double[length];
    }

}

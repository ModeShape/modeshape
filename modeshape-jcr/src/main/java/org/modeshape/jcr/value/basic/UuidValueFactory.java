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
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#URI} values.
 */
@Immutable
public class UuidValueFactory extends AbstractValueFactory<UUID> implements UuidFactory {

    public UuidValueFactory( TextDecoder decoder,
                             ValueFactory<String> stringValueFactory ) {
        super(PropertyType.UUID, decoder, stringValueFactory);
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
    public UUID create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public UUID create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    protected UUID[] createEmptyArray( int length ) {
        return new UUID[length];
    }
}

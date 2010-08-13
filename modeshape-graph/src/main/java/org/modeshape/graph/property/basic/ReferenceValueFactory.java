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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.IoException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#REFERENCE} values.
 */
@Immutable
public class ReferenceValueFactory extends AbstractValueFactory<Reference> implements ReferenceFactory {

    private final boolean weak;

    public ReferenceValueFactory( TextDecoder decoder,
                                  ValueFactory<String> stringValueFactory,
                                  boolean weak ) {
        super(weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE, decoder, stringValueFactory);
        this.weak = weak;
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value ) {
        if (value == null) return null;
        try {
            UUID uuid = UUID.fromString(value);
            return new UuidReference(uuid, weak);
        } catch (IllegalArgumentException err) {
            return new StringReference(value, weak);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( String value,
                             TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( int value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Integer.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Long.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Boolean.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Float.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Double.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          BigDecimal.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Calendar.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.DateTime)
     */
    public Reference create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          DateTime.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Path.Segment value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Path.Segment.class.getSimpleName(),
                                                                          value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reference value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(java.util.UUID)
     */
    public Reference create( UUID value ) {
        if (value == null) return null;
        return new UuidReference(value);
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactory#create(org.modeshape.graph.property.Binary)
     */
    public Reference create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( InputStream stream,
                             long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Reference create( Reader reader,
                             long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Reference[] createEmptyArray( int length ) {
        return new Reference[length];
    }

}

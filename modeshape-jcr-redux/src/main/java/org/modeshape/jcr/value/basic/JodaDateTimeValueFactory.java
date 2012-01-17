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
import org.joda.time.DateTimeZone;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#DATE} values.
 */
@Immutable
public class JodaDateTimeValueFactory extends AbstractValueFactory<DateTime> implements DateTimeFactory {

    public JodaDateTimeValueFactory( TextDecoder decoder,
                                     ValueFactory<String> stringValueFactory ) {
        super(PropertyType.DATE, decoder, stringValueFactory);
    }

    @Override
    public DateTime create( String value ) {
        if (value == null) return null;
        try {
            return new JodaDateTime(value.trim());
        } catch (IllegalArgumentException err) {
            // See if this string represents a LONG value ...
            try {
                Long longValue = Long.parseLong(value);
                return new JodaDateTime(longValue);
            } catch (NumberFormatException e) {
                // Guess it wasn't a long value ...
                throw new ValueFormatException(value, getPropertyType(),
                                               GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                                  DateTime.class.getSimpleName(),
                                                                                  value), err);
            }
        }
    }

    @Override
    public DateTime create( String value,
                            TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    @Override
    public DateTime create( int value ) {
        return create((long)value);
    }

    @Override
    public DateTime create( long value ) {
        return new JodaDateTime(value);
    }

    @Override
    public DateTime create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Date.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public DateTime create( float value ) {
        return create((long)value);
    }

    @Override
    public DateTime create( double value ) {
        return create((long)value);
    }

    @Override
    public DateTime create( BigDecimal value ) {
        if (value == null) return null;
        return create(value.longValue());
    }

    @Override
    public DateTime create( Calendar value ) {
        if (value == null) return null;
        return new JodaDateTime(value);
    }

    @Override
    public DateTime create( Date value ) {
        if (value == null) return null;
        return new JodaDateTime(value);
    }

    @Override
    public DateTime create( DateTime value ) throws ValueFormatException {
        return value;
    }

    @Override
    public DateTime create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Name.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public DateTime create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    Path.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public DateTime create( Path.Segment value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Path.Segment.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public DateTime create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          Reference.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public DateTime create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    URI.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public DateTime create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                    UUID.class.getSimpleName(),
                                                                                                    value));
    }

    @Override
    public DateTime create( NodeKey value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                          NodeKey.class.getSimpleName(),
                                                                          value));
    }

    @Override
    public DateTime create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public DateTime create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    @Override
    public DateTime create( InputStream stream ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream));
    }

    @Override
    public DateTime create() {
        return new JodaDateTime();
    }

    @Override
    public DateTime createUtc() {
        return new JodaDateTime(DateTimeZone.UTC);
    }

    @Override
    public DateTime create( int year,
                            int monthOfYear,
                            int dayOfMonth,
                            int hourOfDay,
                            int minuteOfHour,
                            int secondOfMinute,
                            int millisecondsOfSecond ) {
        return new JodaDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond);
    }

    @Override
    public DateTime create( int year,
                            int monthOfYear,
                            int dayOfMonth,
                            int hourOfDay,
                            int minuteOfHour,
                            int secondOfMinute,
                            int millisecondsOfSecond,
                            int timeZoneOffsetHours ) {
        return new JodaDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond,
                                timeZoneOffsetHours);
    }

    @Override
    public DateTime create( int year,
                            int monthOfYear,
                            int dayOfMonth,
                            int hourOfDay,
                            int minuteOfHour,
                            int secondOfMinute,
                            int millisecondsOfSecond,
                            String timeZoneId ) {
        return new JodaDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond,
                                timeZoneId);
    }

    @Override
    public DateTime create( DateTime original,
                            long offsetInMillis ) {
        assert original != null;
        if (offsetInMillis == 0l) return original;
        long newMillis = original.getMilliseconds() + offsetInMillis;
        return new JodaDateTime(newMillis, original.getTimeZoneId());
    }

    @Override
    protected DateTime[] createEmptyArray( int length ) {
        return new DateTime[length];
    }
}

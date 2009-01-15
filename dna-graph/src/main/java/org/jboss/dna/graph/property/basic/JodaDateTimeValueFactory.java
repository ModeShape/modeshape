/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.property.basic;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.IoException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.joda.time.DateTimeZone;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#DATE} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class JodaDateTimeValueFactory extends AbstractValueFactory<DateTime> implements DateTimeFactory {

    public JodaDateTimeValueFactory( TextDecoder decoder,
                                     ValueFactory<String> stringValueFactory ) {
        super(PropertyType.DATE, decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( String value ) {
        if (value == null) return null;
        try {
            return new JodaDateTime(value.trim());
        } catch (IllegalArgumentException err) {
            throw new ValueFormatException(value, getPropertyType(),
                                           GraphI18n.errorConvertingType.text(String.class.getSimpleName(),
                                                                            DateTime.class.getSimpleName(),
                                                                            value), err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( String value,
                            TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( int value ) {
        return create((long)value);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( long value ) {
        return new JodaDateTime(value);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( boolean value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Date.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( float value ) {
        return create((long)value);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( double value ) {
        return create((long)value);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( BigDecimal value ) {
        if (value == null) return null;
        return create(value.longValue());
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Calendar value ) {
        if (value == null) return null;
        return new JodaDateTime(value);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Date value ) {
        if (value == null) return null;
        return new JodaDateTime(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.DateTime)
     */
    public DateTime create( DateTime value ) throws ValueFormatException {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Name.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Path.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                        Reference.class.getSimpleName(),
                                                                        value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  URI.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(java.util.UUID)
     */
    public DateTime create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  UUID.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( byte[] value ) {
        // First attempt to create a string from the value, then a long from the string ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.Binary)
     */
    public DateTime create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( InputStream stream,
                            long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( Reader reader,
                            long approximateLength ) throws IoException {
        // First attempt to create a string from the value, then a double from the string ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create() {
        return new JodaDateTime();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTimeFactory#createUtc()
     */
    public DateTime createUtc() {
        return new JodaDateTime(DateTimeZone.UTC);
    }

    /**
     * {@inheritDoc}
     */
    public DateTime create( int year,
                            int monthOfYear,
                            int dayOfMonth,
                            int hourOfDay,
                            int minuteOfHour,
                            int secondOfMinute,
                            int millisecondsOfSecond ) {
        return new JodaDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTimeFactory#create(org.jboss.dna.graph.property.DateTime, long)
     */
    public DateTime create( DateTime original,
                            long offsetInMillis ) {
        assert original != null;
        if (offsetInMillis == 0l) return original;
        long newMillis = original.getMilliseconds() + offsetInMillis;
        return new JodaDateTime(newMillis, original.getTimeZoneId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DateTime[] createEmptyArray( int length ) {
        return new DateTime[length];
    }
}

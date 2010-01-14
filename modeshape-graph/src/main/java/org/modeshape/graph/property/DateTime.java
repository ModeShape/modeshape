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
package org.modeshape.graph.property;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;

/**
 * An immutable date-time class that represents an instance in time. This class is designed to hide the horrible implementations
 * of the JDK date and calendar classes, which are being overhauled (and replaced) under <a
 * href="http://jcp.org/en/jsr/detail?id=310">JSR-310</a>, which will be based upon <a
 * href="http://joda-time.sourceforge.net/">Joda-Time</a>. This class serves as a stable migration path toward the new JSR 310
 * classes.
 */
@Immutable
public interface DateTime extends Comparable<DateTime>, Serializable {

    /**
     * Get the ISO-8601 representation of this instance in time. The month-based ISO-8601 representation is the most common format
     * of ISO8601, and is the format used in the XML standards for passing dates and times:
     * 
     * <pre>
     * yyyy-mm-ddTHH:MM:SS.SSSZ
     * </pre>
     * 
     * The fields are separated by dashes and consist of:
     * <ul>
     * <li>four digit year;</li>
     * <li>two digit month, where 01 is Janurary and 12 is December;</li>
     * <li>two digit day of month, from 01 to 31;</li>
     * <li>two digit hour, from 00 to 23;</li>
     * <li>two digit minute, from 00 to 59;</li>
     * <li>two digit second, from 00 to 59;</li>
     * <li>three decimal places for milliseconds, if required;</li>
     * <li>time zone offset of the form <code>�HH:mm</code> (or '0' if UTC)</li>
     * </ul>
     * 
     * @return the string representation; never null
     */
    String getString();

    /**
     * Get the number of milliseconds from 1970-01-01T00:00Z. This value is consistent with the JDK {@link java.util.Date Date}
     * and {@link java.util.Calendar Calendar} classes.
     * 
     * @return the number of milliseconds from 1970-01-01T00:00Z
     */
    long getMilliseconds();

    /**
     * Get the number of milliseconds from 1970-01-01T00:00Z with this time converted to UTC. This value is consistent with the
     * JDK {@link java.util.Date Date} and {@link java.util.Calendar Calendar} classes.
     * 
     * @return the number of milliseconds from 1970-01-01T00:00Z in the UTC time zone
     */
    long getMillisecondsInUtc();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Date} instance. Note that this conversion loses the time
     * zone information, as the standard JDK {@link java.util.Date} does not represent time zones.
     * 
     * @return this instance in time as a JDK Date; never null
     */
    java.util.Date toDate();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the {@link Locale#getDefault()
     * default locale}.
     * 
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the specified {@link Locale locale}
     * .
     * 
     * @param locale the locale in which the Calendar instance is desired; may be null if the {@link Locale#getDefault() default
     *        locale} is to be used.
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar( Locale locale );

    /**
     * Get this instance represented as a standard JDK {@link java.util.GregorianCalendar} instance.
     * 
     * @return this instance in time as a JDK GregorianCalendar; never null
     */
    java.util.GregorianCalendar toGregorianCalendar();

    /**
     * Get the era of this instance in time.
     * 
     * @return the era
     */
    int getEra();

    /**
     * Get the era of this instance in time.
     * 
     * @return the era
     */
    int getYear();

    /**
     * Get the era of this instance in time.
     * 
     * @return the era
     */
    int getWeekyear();

    /**
     * Get the era of this instance in time.
     * 
     * @return the era
     */
    int getCenturyOfEra();

    /**
     * Get the year of the era of this instance in time.
     * 
     * @return the year of the era
     */
    int getYearOfEra();

    /**
     * Get the year of this century of this instance in time.
     * 
     * @return the year of the century
     */
    int getYearOfCentury();

    /**
     * Get the month of the year of this instance in time.
     * 
     * @return the month number
     */
    int getMonthOfYear();

    /**
     * Get the week of the weekyear of this instance in time.
     * 
     * @return the week of the weekyear
     */
    int getWeekOfWeekyear();

    /**
     * Get the day of the year of this instance in time.
     * 
     * @return the day of the year
     */
    int getDayOfYear();

    /**
     * Get the day of the month value of this instance in time.
     * 
     * @return the day of the month
     */
    int getDayOfMonth();

    /**
     * Get the day of the week value of this instance in time.
     * 
     * @return the day of the week
     */
    int getDayOfWeek();

    /**
     * Get the hour of the day of this instance in time.
     * 
     * @return the hour of the day
     */
    int getHourOfDay();

    /**
     * Get the minute of this instance in time.
     * 
     * @return the minute of the hour
     */
    int getMinuteOfHour();

    /**
     * Get the seconds of the minute value of this instance in time.
     * 
     * @return the seconds of the minute
     */
    int getSecondOfMinute();

    /**
     * Get the milliseconds of the second value of this instance in time.
     * 
     * @return the milliseconds
     */
    int getMillisOfSecond();

    /**
     * Get the number of hours that this time zone is offset from UTC.
     * 
     * @return the number of hours
     */
    int getTimeZoneOffsetHours();

    /**
     * Get the identifier of the time zone in which this instant is defined
     * 
     * @return the time zone identifier; never null
     */
    String getTimeZoneId();

    /**
     * Convert this time to the same instant in the UTC time zone.
     * 
     * @return this instant in time in the specified time zone
     */
    DateTime toUtcTimeZone();

    /**
     * Convert this time to the time zone given by the supplied identifier.
     * 
     * @param timeZoneId the time zone identifier
     * @return the instant in the specified time zone
     * @throws IllegalArgumentException if the time zone identifier is null or is invalid
     */
    DateTime toTimeZone( String timeZoneId );

    /**
     * Return whether this date-time is earlier than the supplied date-time.
     * 
     * @param other the date-time to compare with
     * @return true if this date-time is earliar than the other, or false otherwise
     * @see #compareTo(DateTime)
     * @see #isSameAs(DateTime)
     * @see #isAfter(DateTime)
     */
    boolean isBefore( DateTime other );

    /**
     * Return whether this date-time is later than the supplied date-time.
     * 
     * @param other the date-time to compare with
     * @return true if this date-time is later than the other, or false otherwise
     * @see #compareTo(DateTime)
     * @see #isBefore(DateTime)
     * @see #isSameAs(DateTime)
     */
    boolean isAfter( DateTime other );

    /**
     * Return whether this date-time is exactly the the same as the supplied date-time. This differs from {@link #equals(Object)
     * the equals method} in that it can be arbitrarily more strict, checking, for example, not only the logical equivalence of
     * the other date time, but also arbitrary additional fields such as the time zone.
     * 
     * @param other the date-time to compare with
     * @return true if this date-time is later than the other, or false otherwise
     * @see #compareTo(DateTime)
     * @see #isBefore(DateTime)
     * @see #isAfter(DateTime)
     */
    boolean isSameAs( DateTime other );

    /**
     * Subtract the specified about of time in the supplied units.
     * 
     * @param timeAmount the amount of time to subtract
     * @param unit the units of the amount of time; may not be null
     * @return the instance in time the specified number of time before this instant
     */
    DateTime minus( long timeAmount,
                    TimeUnit unit );

    /**
     * Subtract the specified number of days from this time instant.
     * 
     * @param days the number of days to subtract
     * @return the instance in time the specified number of days before this instant
     */
    DateTime minusDays( int days );

    /**
     * Subtract the specified number of hours from this time instant.
     * 
     * @param hours the number of hours to subtract
     * @return the instance in time the specified number of hours before this instant
     */
    DateTime minusHours( int hours );

    /**
     * Subtract the specified number of milliseconds from this time instant.
     * 
     * @param milliseconds the number of milliseconds to subtract
     * @return the instance in time the specified number of milliseconds before this instant
     */
    DateTime minusMillis( int milliseconds );

    /**
     * Subtract the specified number of minutes from this time instant.
     * 
     * @param minutes the number of minutes to subtract
     * @return the instance in time the specified number of minutes before this instant
     */
    DateTime minusMinutes( int minutes );

    /**
     * Subtract the specified number of months from this time instant.
     * 
     * @param months the number of months to subtract
     * @return the instance in time the specified number of months before this instant
     */
    DateTime minusMonths( int months );

    /**
     * Subtract the specified number of seconds from this time instant.
     * 
     * @param seconds the number of seconds to subtract
     * @return the instance in time the specified number of seconds before this instant
     */
    DateTime minusSeconds( int seconds );

    /**
     * Subtract the specified number of weeks from this time instant.
     * 
     * @param weeks the number of weeks to subtract
     * @return the instance in time the specified number of weeks before this instant
     */
    DateTime minusWeeks( int weeks );

    /**
     * Subtract the specified number of years from this time instant.
     * 
     * @param years the number of years to subtract
     * @return the instance in time the specified number of years before this instant
     */
    DateTime minusYears( int years );

    /**
     * Add the specified about of time in the supplied units.
     * 
     * @param timeAmount the amount of time to add
     * @param unit the units of the amount of time; may not be null
     * @return the instance in time the specified number of time after this instant
     */
    DateTime plus( long timeAmount,
                   TimeUnit unit );

    /**
     * Add the specified number of days from this time instant.
     * 
     * @param days the number of days to add
     * @return the instance in time the specified number of days after this instant
     */
    DateTime plusDays( int days );

    /**
     * Add the specified number of hours from this time instant.
     * 
     * @param hours the number of hours to add
     * @return the instance in time the specified number of hours after this instant
     */
    DateTime plusHours( int hours );

    /**
     * Add the specified number of milliseconds from this time instant.
     * 
     * @param milliseconds the number of milliseconds to add
     * @return the instance in time the specified number of milliseconds after this instant
     */
    DateTime plusMillis( int milliseconds );

    /**
     * Add the specified number of minutes from this time instant.
     * 
     * @param minutes the number of minutes to add
     * @return the instance in time the specified number of minutes after this instant
     */
    DateTime plusMinutes( int minutes );

    /**
     * Add the specified number of months from this time instant.
     * 
     * @param months the number of months to add
     * @return the instance in time the specified number of months after this instant
     */
    DateTime plusMonths( int months );

    /**
     * Add the specified number of seconds from this time instant.
     * 
     * @param seconds the number of seconds to add
     * @return the instance in time the specified number of seconds after this instant
     */
    DateTime plusSeconds( int seconds );

    /**
     * Add the specified number of weeks from this time instant.
     * 
     * @param weeks the number of weeks to add
     * @return the instance in time the specified number of weeks after this instant
     */
    DateTime plusWeeks( int weeks );

    /**
     * Add the specified number of years from this time instant.
     * 
     * @param years the number of years to add
     * @return the instance in time the specified number of years after this instant
     */
    DateTime plusYears( int years );

}

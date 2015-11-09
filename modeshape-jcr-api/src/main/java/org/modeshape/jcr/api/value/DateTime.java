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
package org.modeshape.jcr.api.value;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * An immutable date-time class that represents an instance in time specific to a certain timezone. 
 * This class is based on the new JDK 8 date-time implementation and if not explicitly provided, will use "UTC" as the default
 * timezone.
 * <p>
 * If any date-time capabilities are required past what the current interface offers, clients are encouraged to use 
 * the {@link #toZonedDateTime()} and {@link #toLocalDateTime()} and use the standard JDK java.time API.
 * </p>
 * 
 * @since 5.0
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
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
     * <li>time zone offset of the form <code>HH:mm</code> (or '0' if UTC)</li>
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
     * Get this instance represented as a standard JDK {@link ZonedDateTime} instance, which preserves the TZ information.
     *
     * @return this instance in time as a JDK Date; never null
     */    
    ZonedDateTime toZonedDateTime();

    /**
     * Get this instance represented as a standard JDK {@link LocalDateTime} instance, which does not have any TZ information.
     *
     * @return this instance in time as a JDK Date; never null
     */
    LocalDateTime toLocalDateTime();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the {@link Locale#getDefault()
     * default locale}.
     * 
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the specified {@link Locale locale}.
     * 
     * @param locale the locale in which the Calendar instance is desired; may be null if the {@link Locale#getDefault() default
     *        locale} is to be used.
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar( Locale locale );

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
     * Subtract the specified amount.
     * 
     * @param duration the amount of time; may not be null
     * @return the instance in time the specified number of time before this instant
     */
    DateTime minus( Duration duration );

    /**
     * Add the specified amount of time.
     * 
     * @param duration the amount of time; may not be null
     * @return the instance in time the specified number of time after this instant
     */
    DateTime plus( Duration duration );

    @Override
    int compareTo( DateTime other );
}

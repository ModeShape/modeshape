/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph;

import java.io.Serializable;
import java.util.Locale;
import net.jcip.annotations.Immutable;

/**
 * An immutable date-time class that represents an instance in time. This class is designed to hide the horrible implementations
 * of the JDK date and calendar classes, which are being overhauled (and replaced) under <a
 * href="http://jcp.org/en/jsr/detail?id=310">JSR-310</a>, which will be based upon <a
 * href="http://joda-time.sourceforge.net/">Joda-Time</a>. This class serves as a stable migration path toward the new JSR 310
 * classes.
 * @author Randall Hauch
 */
@Immutable
public interface DateTime extends Comparable<DateTime>, Serializable {

    /**
     * Get the ISO-8601 representation of this instance in time.
     * @return the string representation; never null
     */
    String getString();

    /**
     * Get the number of milliseconds from 1970-01-01T00:00Z. This value is consistent with the JDK {@link java.util.Date Date}
     * and {@link java.util.Calendar Calendar} classes.
     * @return the number of milliseconds from 1970-01-01T00:00Z
     */
    long getMilliseconds();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Date} instance. Note that this conversion loses the time
     * zone information, as the standard JDK {@link java.util.Date} does not represent time zones.
     * @return this instance in time as a JDK Date; never null
     */
    java.util.Date toDate();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the
     * {@link Locale#getDefault() default locale}.
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar();

    /**
     * Get this instance represented as a standard JDK {@link java.util.Calendar} instance, in the specified {@link Locale locale}.
     * @param locale the locale in which the Calendar instance is desired; may be null if the
     * {@link Locale#getDefault() default locale} is to be used.
     * @return this instance in time as a JDK Calendar; never null
     */
    java.util.Calendar toCalendar( Locale locale );

    /**
     * Get this instance represented as a standard JDK {@link java.util.GregorianCalendar} instance.
     * @return this instance in time as a JDK GregorianCalendar; never null
     */
    java.util.GregorianCalendar toGregorianCalendar();

    /**
     * Get the era of this instance in time.
     * @return the era
     */
    int getEra();

    /**
     * Get the era of this instance in time.
     * @return the era
     */
    int getYear();

    /**
     * Get the era of this instance in time.
     * @return the era
     */
    int getWeekyear();

    /**
     * Get the era of this instance in time.
     * @return the era
     */
    int getCenturyOfEra();

    /**
     * Get the year of the era of this instance in time.
     * @return the year of the era
     */
    int getYearOfEra();

    /**
     * Get the year of this century of this instance in time.
     * @return the year of the century
     */
    int getYearOfCentury();

    /**
     * Get the month of the year of this instance in time.
     * @return the month number
     */
    int getMonthOfYear();

    /**
     * Get the week of the weekyear of this instance in time.
     * @return the week of the weekyear
     */
    int getWeekOfWeekyear();

    /**
     * Get the day of the year of this instance in time.
     * @return the day of the year
     */
    int getDayOfYear();

    /**
     * Get the day of the month value of this instance in time.
     * @return the day of the month
     */
    int getDayOfMonth();

    /**
     * Get the day of the week value of this instance in time.
     * @return the day of the week
     */
    int getDayOfWeek();

    /**
     * Get the hour of the day of this instance in time.
     * @return the hour of the day
     */
    int getHourOfDay();

    /**
     * Get the minute of this instance in time.
     * @return the minute of the hour
     */
    int getMinuteOfHour();

    /**
     * Get the seconds of the minute value of this instance in time.
     * @return the seconds of the minute
     */
    int getSecondOfMinute();

    /**
     * Get the milliseconds of the second value of this instance in time.
     * @return the milliseconds
     */
    int getMillisOfSecond();

    /**
     * Get the number of hours that this time zone is offset from UTC.
     * @return the number of hours
     */
    int getTimeZoneOffsetHours();

    /**
     * Get the identifier of the time zone in which this instant is defined
     * @return the time zone identifier; never null
     */
    String getTimeZoneId();

    /**
     * Convert this time to the same instant in the UTC time zone.
     * @return this instant in time in the specified time zone
     */
    DateTime toUtcTimeZone();

    /**
     * Convert this time to the time zone given by the supplied identifier.
     * @param timeZoneId the time zone identifier
     * @return the instant in the specified time zone
     * @throws IllegalArgumentException if the time zone identifier is null or is invalid
     */
    DateTime toTimeZone( String timeZoneId );

}

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

/**
 * @author Randall Hauch
 */
public interface DateTimeFactory extends ValueFactory<DateTime> {

    /**
     * Create a date-time instance for the current time.
     * @return the date-time instance
     */
    DateTime create();

    /**
     * Create a date-time instance given the individual values for the fields
     * @param year the year of the era
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisecondsOfSecond the milliseconds of the second
     * @return the date-time instance
     */
    DateTime create( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond );

    /**
     * Create a date-time instance given the individual values for the fields
     * @param year the year of the era
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisecondsOfSecond the milliseconds of the second
     * @param timeZoneOffsetHours the number of hours offset from UTC for the time zone
     * @return the date-time instance
     */
    DateTime create( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, int timeZoneOffsetHours );

    /**
     * Create a date-time instance given the individual values for the fields
     * @param year the year of the era
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisecondsOfSecond the milliseconds of the second
     * @param timeZoneId the ID of the time zone (e.g, "PST", "UTC", "EDT"); may not be null
     * @return the date-time instance
     */
    DateTime create( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, String timeZoneId );

}

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
package org.modeshape.jcr.value;

import org.modeshape.jcr.api.value.DateTime;

/**
 * A factory for creating {@link DateTime date-time instants}. This interface extends the {@link ValueFactory} generic interface
 * and adds specific methods for creating instants for the current time (and time zone) as well as various combinations of
 * individual field values. <h2>ISO-8601</h2>
 * <p>
 * The factory creates date-time instants from strings that are in the standard ISO-8601 format. The format this factory supports
 * is month-based. The month-based representation is the most common format of ISO8601, and is the format used in the XML standards for passing
 * dates and times:
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
 * </p>
 */
public interface DateTimeFactory extends ValueFactory<DateTime> {

    @Override
    DateTimeFactory with( ValueFactories valueFactories );

    /**
     * Create a date-time instance for the current time in the local time zone.
     * 
     * @return the current date-time instance
     * @see #createUtc()
     */
    DateTime create();

    /**
     * Create a date-time instance for the current time in UTC.
     * 
     * @return the current date-time instance (in UTC)
     * @see #create()
     */
    DateTime createUtc();

    /**
     * Create a date-time instance that is offset from the original by the specified amount.
     * 
     * @param original the original {@link DateTime}
     * @param offsetInMillis the offset in milliseconds (positive or negative)
     * @return the offset date-time instance
     */
    DateTime create( DateTime original,
                     long offsetInMillis );

    /**
     * Create a date-time instance given the individual values for the fields
     * 
     * @param year the year of the era
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisecondsOfSecond the milliseconds of the second
     * @return the date-time instance
     */
    DateTime create( int year,
                     int monthOfYear,
                     int dayOfMonth,
                     int hourOfDay,
                     int minuteOfHour,
                     int secondOfMinute,
                     int millisecondsOfSecond );
}

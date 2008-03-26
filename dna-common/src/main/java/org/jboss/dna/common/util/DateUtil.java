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
package org.jboss.dna.common.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.dna.common.CoreI18n;
import net.jcip.annotations.ThreadSafe;

/**
 * Utilities for working with dates.
 * <p>
 * Many of the methods that convert dates to and from strings utilize the <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO
 * 8601:2004</a> standard string format <code>yyyy-MM-ddTHH:mm:ss.SSSZ</code>, where <blockquote>
 * 
 * <pre>
 * Symbol   Meaning                 Presentation        Example
 * ------   -------                 ------------        -------
 * y        year                    (Number)            1996
 * M        month in year           (Number)            07
 * d        day in month            (Number)            10
 * h        hour in am/pm (1&tilde;12)    (Number)            12
 * H        hour in day (0&tilde;23)      (Number)            0
 * m        minute in hour          (Number)            30
 * s        second in minute        (Number)            55
 * S        millisecond             (Number)            978
 * Z        time zone               (Number)            -0600
 * </pre>
 * 
 * </blockquote>
 * </p>
 * <p>
 * This class is written to be thread safe. As {@link SimpleDateFormat} is not threadsafe, no shared instances are used.
 * </p>
 * @author Randall Hauch
 */
@ThreadSafe
public class DateUtil {

    public static final String ISO_8601_2004_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Parse the date contained in the supplied string. The date must follow one of the standard ISO 8601 formats, of the form
     * <code><i>datepart</i>T<i>timepart</i></code>, where <code><i>datepart</i></code> is one of the following forms:
     * <p>
     * <dl>
     * <dt>YYYYMMDD</dt>
     * <dd>The 4-digit year, the 2-digit month (00-12), and the 2-digit day of the month (00-31). The month and day are optional,
     * but the month is required if the day is given.</dd>
     * <dt>YYYY-MM-DD</dt>
     * <dd>The 4-digit year, the 2-digit month (00-12), and the 2-digit day of the month (00-31). The month and day are optional,
     * but the month is required if the day is given.</dd>
     * <dt>YYYY-Www-D</dt>
     * <dd>The 4-digit year followed by 'W', the 2-digit week number (00-53), and the day of the week (1-7). The day of week
     * number is optional.</dd>
     * <dt>YYYYWwwD</dt>
     * <dd>The 4-digit year followed by 'W', the 2-digit week number (00-53), and the day of the week (1-7). The day of week
     * number is optional.</dd>
     * <dt>YYYY-DDD</dt>
     * <dd>The 4-digit year followed by the 3-digit day of the year (000-365)</dd>
     * <dt>YYYYDDD</dt>
     * <dd>The 4-digit year followed by the 3-digit day of the year (000-365)</dd>
     * </dl>
     * </p>
     * <p>
     * The <code><i>timepart</i></code> consists of one of the following forms that contain the 2-digit hour (00-24), the
     * 2-digit minutes (00-59), the 2-digit seconds (00-59), and the 1-to-3 digit milliseconds. The minutes, seconds and
     * milliseconds are optional, but any component is required if it is followed by another component (e.g., minutes are required
     * if the seconds are given).
     * <dl>
     * <dt>hh:mm:ss.SSS</dt>
     * <dt>hhmmssSSS</dt>
     * </dl>
     * </p>
     * <p>
     * followed by one of the following time zone definitions:
     * <dt>Z</dt>
     * <dd>The uppercase or lowercase 'Z' to denote UTC time</dd>
     * <dt>&#177;hh:mm</dt>
     * <dd>The 2-digit hour and the 2-digit minute offset from UTC</dd>
     * <dt>&#177;hhmm</dt>
     * <dd>The 2-digit hour and the 2-digit minute offset from UTC</dd>
     * <dt>&#177;hh</dt>
     * <dd>The 2-digit hour offset from UTC</dd>
     * <dt>hh:mm</dt>
     * <dd>The 2-digit hour and the 2-digit minute offset from UTC</dd>
     * <dt>hhmm</dt>
     * <dd>The 2-digit hour and the 2-digit minute offset from UTC</dd>
     * <dt>hh</dt>
     * <dd>The 2-digit hour offset from UTC</dd>
     * </dl>
     * </p>
     * @param dateString the string containing the date to be parsed
     * @return the parsed date as a {@link Calendar} object.
     * @throws ParseException if there is a problem parsing the string
     */
    public static Calendar getCalendarFromStandardString( final String dateString ) throws ParseException {
        // Example: 2008-02-16T12:30:45.123-0600
        // Example: 2008-W06-6
        // Example: 2008-053
        //
        // Group Optional Field Description
        // ----- -------- --------- ------------------------------------------
        // 1 no 2008 4 digit year as a number
        // 2 yes 02-16 or W06-6 or 053
        // 3 yes W06-6
        // 4 yes 06 2 digit week number (00-59)
        // 5 yes 6 1 digit day of week as a number (1-7)
        // 6 yes 02-16
        // 7 yes 02 2 digit month as a number (00-19)
        // 8 yes -16
        // 9 yes 16 2 digit day of month as a number (00-39)
        // 10 yes 02 2 digit month as a number (00-19)
        // 11 yes 16 2 digit day of month as a number (00-39)
        // 12 yes 234 3 digit day of year as a number (000-399)
        // 13 yes T12:30:45.123-0600
        // 14 yes 12 2 digit hour as a number (00-29)
        // 15 yes 30 2 digit minute as a number (00-59)
        // 16 yes :45.123
        // 17 yes 45 2 digit second as a number (00-59)
        // 18 yes .123
        // 19 yes 123 1, 2 or 3 digit milliseconds as a number (000-999)
        // 20 yes -0600
        // 21 yes Z The letter 'Z' if in UTC
        // 22 yes -06 1 or 2 digit time zone hour offset as a signed number
        // 23 yes + the plus or minus in the time zone offset
        // 24 yes 00 1 or 2 digit time zone hour offset as an unsigned number (00-29)
        // 25 yes 00 1 or 2 digit time zone minute offset as a number (00-59)
        final String regex =
            "^(\\d{4})-?(([wW]([012345]\\d)-?([1234567])?)|(([01]\\d)(-([0123]\\d))?)|([01]\\d)([0123]\\d)|([0123]\\d\\d))?(T([012]\\d):?([012345]\\d)(:?([012345]\\d)(.(\\d{1,3}))?)?((Z)|(([+-])(\\d{2})):?(\\d{2})?)?)?$";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(dateString);
        if (!matcher.matches()) {
            throw new ParseException(CoreI18n.dateParsingFailure.text(dateString), 0);
        }
        String year = matcher.group(1);
        String week = matcher.group(4);
        String dayOfWeek = matcher.group(5);
        String month = matcher.group(7);
        if (month == null) month = matcher.group(10);
        String dayOfMonth = matcher.group(9);
        if (dayOfMonth == null) dayOfMonth = matcher.group(11);
        String dayOfYear = matcher.group(12);
        String hourOfDay = matcher.group(14);
        String minutesOfHour = matcher.group(15);
        String seconds = matcher.group(17);
        String milliseconds = matcher.group(19);
        String timeZoneSign = matcher.group(23);
        String timeZoneHour = matcher.group(24);
        String timeZoneMinutes = matcher.group(25);
        if (matcher.group(21) != null) {
            timeZoneHour = "00";
            timeZoneMinutes = "00";
        }

        // Create the calendar object and start setting the fields ...
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.clear();

        // And start setting the fields. Note that Integer.parseInt should never fail, since we're checking for null and the
        // regular expression should only have digits in these strings!
        if (year != null) calendar.set(Calendar.YEAR, Integer.parseInt(year));
        if (month != null) {
            calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1); // month is zero-based!
            if (dayOfMonth != null) calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
        } else if (week != null) {
            calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(week));
            if (dayOfWeek != null) calendar.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dayOfWeek));
        } else if (dayOfYear != null) {
            calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(dayOfYear));
        }
        if (hourOfDay != null) calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourOfDay));
        if (minutesOfHour != null) calendar.set(Calendar.MINUTE, Integer.parseInt(minutesOfHour));
        if (seconds != null) calendar.set(Calendar.SECOND, Integer.parseInt(seconds));
        if (milliseconds != null) calendar.set(Calendar.MILLISECOND, Integer.parseInt(milliseconds));
        if (timeZoneHour != null) {
            int zoneOffsetInMillis = Integer.parseInt(timeZoneHour) * 60 * 60 * 1000;
            if ("-".equals(timeZoneSign)) zoneOffsetInMillis *= -1;
            if (timeZoneMinutes != null) {
                int minuteOffsetInMillis = Integer.parseInt(timeZoneMinutes) * 60 * 1000;
                if (zoneOffsetInMillis < 0) {
                    zoneOffsetInMillis -= minuteOffsetInMillis;
                } else {
                    zoneOffsetInMillis += minuteOffsetInMillis;
                }
            }
            calendar.set(Calendar.ZONE_OFFSET, zoneOffsetInMillis);
        }
        return calendar;
    }

    /**
     * Parse the date contained in the supplied string. This method simply calls {@link Calendar#getTime()} on the result of
     * {@link #getCalendarFromStandardString(String)}.
     * @param dateString the string containing the date to be parsed
     * @return the parsed date as a {@link Calendar} object.
     * @throws ParseException if there is a problem parsing the string
     * @see #getCalendarFromStandardString(String)
     */
    public static Date getDateFromStandardString( String dateString ) throws ParseException {
        return getCalendarFromStandardString(dateString).getTime();
    }

    /**
     * Obtain an ISO 8601:2004 string representation of the date given the supplied milliseconds since the epoch.
     * @param millisecondsSinceEpoch the milliseconds for the date
     * @return the string in the {@link #ISO_8601_2004_FORMAT standard format}
     * @see #getDateAsStandardString(Date)
     * @see #getDateFromStandardString(String)
     * @see #getCalendarFromStandardString(String)
     */
    public static String getDateAsStandardString( final long millisecondsSinceEpoch ) {
        return getDateAsStandardString(new Date(millisecondsSinceEpoch));
    }

    /**
     * Obtain an ISO 8601:2004 string representation of the date given the supplied milliseconds since the epoch.
     * @param date the date in calendar form
     * @return the string in the {@link #ISO_8601_2004_FORMAT standard format}
     * @see #getDateAsStandardString(Date)
     * @see #getDateFromStandardString(String)
     * @see #getCalendarFromStandardString(String)
     */
    public static String getDateAsStandardString( final Calendar date ) {
        return getDateAsStandardString(date.getTime());
    }

    /**
     * Obtain an ISO 8601:2004 string representation of the supplied date.
     * @param date the date
     * @return the string in the {@link #ISO_8601_2004_FORMAT standard format}
     * @see #getDateAsStandardString(long)
     * @see #getDateFromStandardString(String)
     * @see #getCalendarFromStandardString(String)
     */
    public static String getDateAsStandardString( final java.util.Date date ) {
        return new SimpleDateFormat(ISO_8601_2004_FORMAT).format(date);
    }

    public static String getDateAsStringForCurrentLocale( final java.util.Date date ) {
        return getDateAsStringForLocale(date, Locale.getDefault());
    }

    public static String getDateAsStringForLocale( final java.util.Date date, Locale locale ) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(date);
    }

    private DateUtil() {
        // Prevent instantiation
    }

}

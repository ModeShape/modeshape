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
package org.modeshape.jdbc.util;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utility methods for SQL Timestamps, Time, and Dates with time zones as UTC This is intended to take incoming Strings or Dates
 * that have accurate Calendar fields and give the UTC time by interpretting those fields in the target time zone. Use of the
 * Calendar object passed in will not be thread safe, but it will not alter the contents of the Calendar. Note that normalization
 * occurs only for the transition from one type to another.
 */
public class TimestampWithTimezone {

    public static DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
    public static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    public static DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss a"); //$NON-NLS-1$

    private static ThreadLocal<Calendar> CALENDAR = new ThreadLocal<Calendar>() {
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    };

    public static Calendar getCalendar() {
        return CALENDAR.get();
    }

    public static void resetCalendar( TimeZone tz ) {
        TimeZone.setDefault(tz);
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(tz);
        CALENDAR.set(cal);
    }

    public static Timestamp createTimestamp( Calendar initial,
                                             Calendar target ) {
        if (target == null) {
            target = getCalendar();
        }

        long time = target.getTimeInMillis();

        Calendar new_target = adjustCalendarForTimeStamp(initial, target);

        Timestamp tsInTz = createTimestamp(new_target);

        target.setTimeInMillis(time);
        return tsInTz;
    }

    public static Time createTime( Calendar initial,
                                   Calendar target ) {
        if (target == null) {
            // if target is null, then obtain current calendar
            target = getCalendar();
        }

        long time = target.getTimeInMillis();

        adjustCalendarForTime(initial, target);

        target.set(Calendar.MILLISECOND, 0);

        Time result = createTime(target);

        target.setTimeInMillis(time);

        return result;
    }

    public static Date createDate( Calendar initial,
                                   Calendar target ) {
        if (target == null) {
            return createDate(initial);
        }

        long time = target.getTimeInMillis();

        target = adjustCalendarForDate(initial, target);

        Date result = normalizeDate(target, true);

        target.setTimeInMillis(time);

        return result;
    }

    /**
     * Creates normalized SQL Time Object based on the target Calendar.
     * 
     * @param target Calendar
     * @return Time
     */
    public static Time createTime( Calendar target ) {
        return new Time(target.getTimeInMillis());
    }

    /**
     * Creates normalized SQL Date Object based on the target Calendar
     * 
     * @param target Calendar
     * @return Date
     */
    public static Date createDate( Calendar target ) {
        return new java.sql.Date(target.getTime().getTime());
    }

    /**
     * Creates normalized SQL Timestamp Object based on the target Calendar
     * 
     * @param target Calendar
     * @return Timestamp
     */
    public static Timestamp createTimestamp( Calendar target ) {
        return new Timestamp(target.getTime().getTime());
    }

    private static Date normalizeDate( Calendar target,
                                       boolean isDate ) {
        if (isDate) {
            target.set(Calendar.HOUR_OF_DAY, 0);
            target.set(Calendar.MINUTE, 0);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
        }
        return createDate(target);
    }

    private static void adjustCalendarForTime( Calendar initial,
                                               Calendar target ) {
        assert initial != null;

        if (initial.getTimeZone().hasSameRules(target.getTimeZone())) {
            target.setTime(initial.getTime());
            return;
        }

        target.clear();
        for (int i = 0; i <= Calendar.MILLISECOND; i++) {
            target.set(i, initial.get(i));
        }
    }

    private static Calendar adjustCalendarForDate( Calendar initial,
                                                   Calendar target ) {
        assert initial != null;

        if (initial.getTimeZone().hasSameRules(target.getTimeZone())) {
            target.setTime(initial.getTime());
            return target;
        }

        Calendar ntarget = new GregorianCalendar(target.getTimeZone());
        ntarget.setTimeInMillis(initial.getTimeInMillis());

        return ntarget;
    }

    private static Calendar adjustCalendarForTimeStamp( Calendar initial,
                                                        Calendar target ) {
        assert initial != null;

        if (initial.getTimeZone().hasSameRules(target.getTimeZone())) {
            target.setTime(initial.getTime());
            return target;
        }

        TimeZone targetTimeZone = target.getTimeZone();
        Calendar ret = new GregorianCalendar(targetTimeZone);
        ret.setTimeInMillis(initial.getTimeInMillis() + targetTimeZone.getOffset(initial.getTimeInMillis())
                            - initial.getTimeZone().getOffset(initial.getTimeInMillis()));
        ret.getTime();
        return ret;
    }
}

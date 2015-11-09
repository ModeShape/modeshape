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
package org.modeshape.jcr.value.basic;

import static org.modeshape.common.util.DateTimeUtil.UTC;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.DateTimeUtil;
import org.modeshape.jcr.api.value.DateTime;

/**
 * Implementation of DateTime based on JDK 8 functionality.
 */
@Immutable
public class ModeShapeDateTime implements DateTime {

    private static final long serialVersionUID = -730188225988292422L;
    private static final int SECONDS_IN_HOUR = 60 * 60;

    private final ZonedDateTime instance;
    private final long millisInUtc;

    public ModeShapeDateTime() {
        this.instance = ZonedDateTime.now();
        this.millisInUtc = utcMillis(instance);
    }

    public ModeShapeDateTime( String iso8601 ) {
        this.instance = DateTimeUtil.jodaParse(iso8601);
        this.millisInUtc = utcMillis(instance);
    }

    public ModeShapeDateTime( String iso8601,
                              String timeZoneId ) {
        this.instance = DateTimeUtil.jodaParse(iso8601).withZoneSameInstant(ZoneId.of(timeZoneId));
        this.millisInUtc = utcMillis(instance);
    }

    public ModeShapeDateTime( long milliseconds ) {
        this.instance = ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), UTC);
        this.millisInUtc = utcMillis(instance);
    }

    public ModeShapeDateTime( long milliseconds,
                              String timeZoneId ) {
        this.instance = ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.of(timeZoneId));
        this.millisInUtc = utcMillis(instance);
    }

    public ModeShapeDateTime( int year,
                              int monthOfYear,
                              int dayOfMonth,
                              int hourOfDay,
                              int minuteOfHour,
                              int secondOfMinute,
                              int millisecondsOfSecond ) {
        int nano = (int) TimeUnit.NANOSECONDS.convert(millisecondsOfSecond, TimeUnit.MILLISECONDS);
        this.instance = ZonedDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, nano, UTC);
        this.millisInUtc = instance.toInstant().toEpochMilli();
    }

    public ModeShapeDateTime( java.util.Date jdkDate ) {
        this.instance = ZonedDateTime.ofInstant(jdkDate.toInstant(), TimeZone.getDefault().toZoneId());
        this.millisInUtc = utcMillis(this.instance);
    }

    public ModeShapeDateTime( java.util.Calendar jdkCalendar ) {
        this.instance = ZonedDateTime.ofInstant(jdkCalendar.toInstant(), jdkCalendar.getTimeZone().toZoneId());
        this.millisInUtc = utcMillis(instance);
    }
    
    protected ModeShapeDateTime( ZonedDateTime zonedDateTime ) {
        this.instance = zonedDateTime;
        this.millisInUtc =  utcMillis(instance);
    }

    @Override
    public int getMillisOfSecond() {
        return this.instance.get(ChronoField.MILLI_OF_SECOND);
    }

    @Override
    public long getMilliseconds() {
        return this.instance.toInstant().toEpochMilli();
    }

    @Override
    public long getMillisecondsInUtc() {
        return millisInUtc;
    }

    @Override
    public String getString() {
        return DateTimeUtil.jodaFormat(instance);
    }

    @Override
    public int getTimeZoneOffsetHours() {
        return this.instance.getOffset().getTotalSeconds()/ SECONDS_IN_HOUR;
    }

    @Override
    public String getTimeZoneId() {
        return this.instance.getZone().getId();
    }

    @Override
    public Calendar toCalendar() {
        return toCalendar(null);
    }

    @Override
    public Calendar toCalendar( Locale locale ) { 
        Calendar calendar = locale != null ? Calendar.getInstance(locale) : Calendar.getInstance();
        calendar.setTimeInMillis(getMilliseconds());
        return calendar;
    }

    @Override
    public Date toDate() {
        return new Date(getMilliseconds());
    }

    @Override
    public ZonedDateTime toZonedDateTime() {
        return instance;
    }

    @Override
    public LocalDateTime toLocalDateTime() {
        return instance.toLocalDateTime();
    }

    @Override
    public int compareTo( org.modeshape.jcr.api.value.DateTime that ) {
        long diff = this.getMillisecondsInUtc() - that.getMillisecondsInUtc();
        return diff == 0 ? 0 : diff > 0 ? 1 : -1;
    }

    @Override
    public int hashCode() {
        return (int)this.millisInUtc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof org.modeshape.jcr.api.value.DateTime) {
            org.modeshape.jcr.api.value.DateTime that = (org.modeshape.jcr.api.value.DateTime)obj;
            return this.getMillisecondsInUtc() == that.getMillisecondsInUtc();
        }
        if (obj instanceof ZonedDateTime) {
            ZonedDateTime that = (ZonedDateTime)obj;
            return this.getMillisecondsInUtc() == that.withZoneSameInstant(UTC).toInstant().toEpochMilli();
        }
        return false;
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public ModeShapeDateTime toUtcTimeZone() {
        if (this.instance.getZone().equals(UTC)) {
            return this;
        }
        return new ModeShapeDateTime(this.instance.withZoneSameInstant(UTC));
    }

    @Override
    public ModeShapeDateTime toTimeZone( String timeZoneId ) {
        CheckArg.isNotNull(timeZoneId, "time zone identifier");
        ZoneId zoneId = ZoneId.of(timeZoneId);
        if (this.instance.getZone().equals(zoneId)) {
            return this;
        }
        return new ModeShapeDateTime(this.instance.withZoneSameInstant(zoneId));
    }

    @Override
    public boolean isBefore( DateTime other ) {
        return this.compareTo(other) < 0;
    }

    @Override
    public boolean isSameAs( DateTime other ) {
        return other == this || instance.isEqual(other.toZonedDateTime());
    }

    @Override
    public boolean isAfter( DateTime other ) {
        return this.compareTo(other) > 0;
    }

    @Override
    public DateTime minus( Duration duration ) {
        CheckArg.isNotNull(duration, "unit");
        return new ModeShapeDateTime(this.instance.minus(duration));
    }

    @Override
    public DateTime plus( Duration duration ) {
        CheckArg.isNotNull(duration, "unit");
        return new ModeShapeDateTime(this.instance.plus(duration));
    }

    private long utcMillis(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(UTC).toInstant().toEpochMilli();
    }
}

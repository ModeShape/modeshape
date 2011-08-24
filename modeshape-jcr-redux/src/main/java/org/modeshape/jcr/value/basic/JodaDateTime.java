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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * Implementation of DateTime based upon the Joda-Time library.
 */
@Immutable
public class JodaDateTime implements org.modeshape.jcr.value.DateTime {

    private static final DateTimeZone UTC_ZONE = DateTimeZone.forID("UTC");

    private static final long serialVersionUID = -730188225988292422L;

    private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;

    private final DateTime instance;
    private final long millisInUtc;

    public JodaDateTime() {
        this.instance = new DateTime();
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( String iso8601 ) {
        this.instance = new DateTime(iso8601);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( String iso8601,
                         String timeZoneId ) {
        this.instance = new DateTime(iso8601, DateTimeZone.forID(timeZoneId));
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( long milliseconds ) {
        this.instance = new DateTime(milliseconds);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( long milliseconds,
                         Chronology chronology ) {
        this.instance = new DateTime(milliseconds, chronology);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( long milliseconds,
                         String timeZoneId ) {
        this.instance = new DateTime(milliseconds, DateTimeZone.forID(timeZoneId));
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( DateTimeZone dateTimeZone ) {
        this.instance = new DateTime(dateTimeZone);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond,
                         Chronology chronology ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
                                     millisecondsOfSecond, chronology);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond,
                         DateTimeZone dateTimeZone ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
                                     millisecondsOfSecond, dateTimeZone);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond,
                         int timeZoneOffsetHours ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
                                     millisecondsOfSecond, DateTimeZone.forOffsetHours(timeZoneOffsetHours));
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond,
                         String timeZoneId ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
                                     millisecondsOfSecond, DateTimeZone.forID(timeZoneId));
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( java.util.Date jdkDate ) {
        this.instance = new DateTime(jdkDate);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( java.util.Calendar jdkCalendar ) {
        this.instance = new DateTime(jdkCalendar);
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    public JodaDateTime( DateTime dateTime ) {
        this.instance = dateTime; // it's immutable, so just hold onto the supplied instance
        this.millisInUtc = instance.withZone(UTC_ZONE).getMillis();
    }

    @Override
    public int getCenturyOfEra() {
        return this.instance.getCenturyOfEra();
    }

    @Override
    public int getDayOfMonth() {
        return this.instance.getDayOfMonth();
    }

    @Override
    public int getDayOfWeek() {
        return this.instance.getDayOfWeek();
    }

    @Override
    public int getDayOfYear() {
        return this.instance.getDayOfYear();
    }

    @Override
    public int getEra() {
        return this.instance.getEra();
    }

    @Override
    public int getHourOfDay() {
        return this.instance.getHourOfDay();
    }

    @Override
    public int getMillisOfSecond() {
        return this.instance.getMillisOfSecond();
    }

    @Override
    public long getMilliseconds() {
        return this.instance.getMillis();
    }

    @Override
    public long getMillisecondsInUtc() {
        return millisInUtc;
    }

    @Override
    public int getMinuteOfHour() {
        return this.instance.getMinuteOfHour();
    }

    @Override
    public int getMonthOfYear() {
        return this.instance.getMonthOfYear();
    }

    @Override
    public int getSecondOfMinute() {
        return this.instance.getSecondOfMinute();
    }

    @Override
    public String getString() {
        return this.instance.toString(org.joda.time.format.ISODateTimeFormat.dateTime());
    }

    @Override
    public int getWeekOfWeekyear() {
        return this.instance.getWeekOfWeekyear();
    }

    @Override
    public int getWeekyear() {
        return this.instance.getWeekyear();
    }

    @Override
    public int getYear() {
        return this.instance.getYear();
    }

    @Override
    public int getYearOfCentury() {
        return this.instance.getYearOfCentury();
    }

    @Override
    public int getYearOfEra() {
        return this.instance.getYearOfEra();
    }

    @Override
    public int getTimeZoneOffsetHours() {
        // return this.instance.getZone().toTimeZone().getRawOffset() / MILLIS_IN_HOUR;
        return this.instance.getZone().getOffset(this.instance.getMillis()) / MILLIS_IN_HOUR;
    }

    @Override
    public String getTimeZoneId() {
        return this.instance.getZone().getID();
    }

    @Override
    public Calendar toCalendar() {
        return toCalendar(null);
    }

    @Override
    public Calendar toCalendar( Locale locale ) {
        return this.instance.toCalendar(locale);
    }

    @Override
    public Date toDate() {
        return this.instance.toDate();
    }

    @Override
    public GregorianCalendar toGregorianCalendar() {
        return this.instance.toGregorianCalendar();
    }

    @Override
    public int compareTo( org.modeshape.jcr.value.DateTime that ) {
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
        if (obj instanceof org.modeshape.jcr.value.DateTime) {
            org.modeshape.jcr.value.DateTime that = (org.modeshape.jcr.value.DateTime)obj;
            return this.getMillisecondsInUtc() == that.getMillisecondsInUtc();
        }
        if (obj instanceof DateTime) {
            DateTime that = (DateTime)obj;
            return this.getMillisecondsInUtc() == that.withZone(UTC_ZONE).getMillis();
        }
        return false;
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public org.modeshape.jcr.value.DateTime toUtcTimeZone() {
        DateTimeZone utc = DateTimeZone.forID("UTC");
        if (this.instance.getZone().equals(utc)) return this;
        DateTime jodaTime = this.instance.withZone(utc);
        return new JodaDateTime(jodaTime);
    }

    @Override
    public org.modeshape.jcr.value.DateTime toTimeZone( String timeZoneId ) {
        CheckArg.isNotNull(timeZoneId, "time zone identifier");
        DateTime jodaTime = this.instance.withZone(DateTimeZone.forID(timeZoneId));
        return new JodaDateTime(jodaTime);
    }

    @Override
    public boolean isBefore( org.modeshape.jcr.value.DateTime other ) {
        return this.compareTo(other) < 0;
    }

    @Override
    public boolean isSameAs( org.modeshape.jcr.value.DateTime other ) {
        if (other == this) return true;
        if (other instanceof JodaDateTime) {
            JodaDateTime that = (JodaDateTime)other;

            /*
             * The equals semantics for JodaDateTimes are very strict, implying that not only are the two instants represented
             * by the JodaDateTimes logically equivalent, but also that the Chronology and DateTimeZone are the same.  
             * Here we use equals to ensure that the two DateTimes are equivalent.
             */
            return this.instance.equals(that.instance);
        }
        return this.instance.equals(other);
    }

    @Override
    public boolean isAfter( org.modeshape.jcr.value.DateTime other ) {
        return this.compareTo(other) > 0;
    }

    @Override
    public org.modeshape.jcr.value.DateTime minus( long timeAmount,
                                                   TimeUnit unit ) {
        CheckArg.isNotNull(unit, "unit");
        return new JodaDateTime(this.instance.minus(TimeUnit.MILLISECONDS.convert(timeAmount, unit)));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusDays( int days ) {
        return new JodaDateTime(this.instance.minusDays(days));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusHours( int hours ) {
        return new JodaDateTime(this.instance.minusHours(hours));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusMillis( int milliseconds ) {
        return new JodaDateTime(this.instance.minusMillis(milliseconds));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusMinutes( int minutes ) {
        return new JodaDateTime(this.instance.minusMinutes(minutes));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusMonths( int months ) {
        return new JodaDateTime(this.instance.minusMonths(months));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusSeconds( int seconds ) {
        return new JodaDateTime(this.instance.minusSeconds(seconds));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusWeeks( int weeks ) {
        return new JodaDateTime(this.instance.minusWeeks(weeks));
    }

    @Override
    public org.modeshape.jcr.value.DateTime minusYears( int years ) {
        return new JodaDateTime(this.instance.minusYears(years));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plus( long timeAmount,
                                                  TimeUnit unit ) {
        CheckArg.isNotNull(unit, "unit");
        return new JodaDateTime(this.instance.plus(TimeUnit.MILLISECONDS.convert(timeAmount, unit)));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusDays( int days ) {
        return new JodaDateTime(this.instance.plusDays(days));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusHours( int hours ) {
        return new JodaDateTime(this.instance.plusHours(hours));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusMillis( int milliseconds ) {
        return new JodaDateTime(this.instance.plusMillis(milliseconds));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusMinutes( int minutes ) {
        return new JodaDateTime(this.instance.plusMinutes(minutes));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusMonths( int months ) {
        return new JodaDateTime(this.instance.plusMonths(months));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusSeconds( int seconds ) {
        return new JodaDateTime(this.instance.plusSeconds(seconds));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusWeeks( int weeks ) {
        return new JodaDateTime(this.instance.plusWeeks(weeks));
    }

    @Override
    public org.modeshape.jcr.value.DateTime plusYears( int years ) {
        return new JodaDateTime(this.instance.plusYears(years));
    }

}

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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Implementation of DateTime based upon the Joda-Time library.
 * 
 * @author Randall Hauch
 */
@Immutable
public class JodaDateTime implements org.jboss.dna.graph.property.DateTime {

    /**
     */
    private static final long serialVersionUID = -730188225988292422L;

    private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;

    private final DateTime instance;

    public JodaDateTime() {
        this.instance = new DateTime();
    }

    public JodaDateTime( String iso8601 ) {
        this.instance = new DateTime(iso8601);
    }

    public JodaDateTime( String iso8601,
                         String timeZoneId ) {
        this.instance = new DateTime(iso8601, DateTimeZone.forID(timeZoneId));
    }

    public JodaDateTime( long milliseconds ) {
        this.instance = new DateTime(milliseconds);
    }

    public JodaDateTime( long milliseconds,
                         Chronology chronology ) {
        this.instance = new DateTime(milliseconds, chronology);
    }

    public JodaDateTime( long milliseconds,
                         String timeZoneId ) {
        this.instance = new DateTime(milliseconds, DateTimeZone.forID(timeZoneId));
    }

    public JodaDateTime( DateTimeZone dateTimeZone ) {
        this.instance = new DateTime(dateTimeZone);
    }

    public JodaDateTime( int year,
                         int monthOfYear,
                         int dayOfMonth,
                         int hourOfDay,
                         int minuteOfHour,
                         int secondOfMinute,
                         int millisecondsOfSecond ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond);
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
    }

    public JodaDateTime( java.util.Date jdkDate ) {
        this.instance = new DateTime(jdkDate);
    }

    public JodaDateTime( java.util.Calendar jdkCalendar ) {
        this.instance = new DateTime(jdkCalendar);
    }

    public JodaDateTime( DateTime dateTime ) {
        this.instance = dateTime; // it's immutable, so just hold onto the supplied instance
    }

    /**
     * {@inheritDoc}
     */
    public int getCenturyOfEra() {
        return this.instance.getCenturyOfEra();
    }

    /**
     * {@inheritDoc}
     */
    public int getDayOfMonth() {
        return this.instance.getDayOfMonth();
    }

    /**
     * {@inheritDoc}
     */
    public int getDayOfWeek() {
        return this.instance.getDayOfWeek();
    }

    /**
     * {@inheritDoc}
     */
    public int getDayOfYear() {
        return this.instance.getDayOfYear();
    }

    /**
     * {@inheritDoc}
     */
    public int getEra() {
        return this.instance.getEra();
    }

    /**
     * {@inheritDoc}
     */
    public int getHourOfDay() {
        return this.instance.getHourOfDay();
    }

    /**
     * {@inheritDoc}
     */
    public int getMillisOfSecond() {
        return this.instance.getMillisOfSecond();
    }

    /**
     * {@inheritDoc}
     */
    public long getMilliseconds() {
        return this.instance.getMillis();
    }

    /**
     * {@inheritDoc}
     */
    public int getMinuteOfHour() {
        return this.instance.getMinuteOfHour();
    }

    /**
     * {@inheritDoc}
     */
    public int getMonthOfYear() {
        return this.instance.getMonthOfYear();
    }

    /**
     * {@inheritDoc}
     */
    public int getSecondOfMinute() {
        return this.instance.getSecondOfMinute();
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return this.instance.toString(org.joda.time.format.ISODateTimeFormat.dateTime());
    }

    /**
     * {@inheritDoc}
     */
    public int getWeekOfWeekyear() {
        return this.instance.getWeekOfWeekyear();
    }

    /**
     * {@inheritDoc}
     */
    public int getWeekyear() {
        return this.instance.getWeekyear();
    }

    /**
     * {@inheritDoc}
     */
    public int getYear() {
        return this.instance.getYear();
    }

    /**
     * {@inheritDoc}
     */
    public int getYearOfCentury() {
        return this.instance.getYearOfCentury();
    }

    /**
     * {@inheritDoc}
     */
    public int getYearOfEra() {
        return this.instance.getYearOfEra();
    }

    /**
     * {@inheritDoc}
     */
    public int getTimeZoneOffsetHours() {
        // return this.instance.getZone().toTimeZone().getRawOffset() / MILLIS_IN_HOUR;
        return this.instance.getZone().getOffset(this.instance.getMillis()) / MILLIS_IN_HOUR;
    }

    /**
     * {@inheritDoc}
     */
    public String getTimeZoneId() {
        return this.instance.getZone().getID();
    }

    /**
     * {@inheritDoc}
     */
    public Calendar toCalendar() {
        return toCalendar(null);
    }

    /**
     * {@inheritDoc}
     */
    public Calendar toCalendar( Locale locale ) {
        return this.instance.toCalendar(locale);
    }

    /**
     * {@inheritDoc}
     */
    public Date toDate() {
        return this.instance.toDate();
    }

    /**
     * {@inheritDoc}
     */
    public GregorianCalendar toGregorianCalendar() {
        return this.instance.toGregorianCalendar();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( org.jboss.dna.graph.property.DateTime that ) {
        if (that instanceof JodaDateTime) {
            return this.instance.compareTo(((JodaDateTime)that).instance);
        }
        long diff = this.toUtcTimeZone().getMilliseconds() - that.toUtcTimeZone().getMilliseconds();
        return (int)diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.instance.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof JodaDateTime) {
            JodaDateTime that = (JodaDateTime)obj;
            return this.instance.equals(that.instance);
        }
        if (obj instanceof DateTime) {
            return this.instance.equals(obj);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getString();
    }

    /**
     * {@inheritDoc}
     */
    public org.jboss.dna.graph.property.DateTime toUtcTimeZone() {
        DateTime jodaTime = this.instance.withZone(DateTimeZone.forID("UTC"));
        return new JodaDateTime(jodaTime);
    }

    /**
     * {@inheritDoc}
     */
    public org.jboss.dna.graph.property.DateTime toTimeZone( String timeZoneId ) {
        CheckArg.isNotNull(timeZoneId, "time zone identifier");
        DateTime jodaTime = this.instance.withZone(DateTimeZone.forID(timeZoneId));
        return new JodaDateTime(jodaTime);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#isBefore(org.jboss.dna.graph.property.DateTime)
     */
    public boolean isBefore( org.jboss.dna.graph.property.DateTime other ) {
        return this.compareTo(other) < 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#isSameAs(org.jboss.dna.graph.property.DateTime)
     */
    public boolean isSameAs( org.jboss.dna.graph.property.DateTime other ) {
        return this.compareTo(other) == 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#isAfter(org.jboss.dna.graph.property.DateTime)
     */
    public boolean isAfter( org.jboss.dna.graph.property.DateTime other ) {
        return this.compareTo(other) > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minus(long, java.util.concurrent.TimeUnit)
     */
    public org.jboss.dna.graph.property.DateTime minus( long timeAmount,
                                                          TimeUnit unit ) {
        CheckArg.isNotNull(unit, "unit");
        return new JodaDateTime(this.instance.minus(TimeUnit.MILLISECONDS.convert(timeAmount, unit)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusDays(int)
     */
    public org.jboss.dna.graph.property.DateTime minusDays( int days ) {
        return new JodaDateTime(this.instance.minusDays(days));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusHours(int)
     */
    public org.jboss.dna.graph.property.DateTime minusHours( int hours ) {
        return new JodaDateTime(this.instance.minusHours(hours));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusMillis(int)
     */
    public org.jboss.dna.graph.property.DateTime minusMillis( int milliseconds ) {
        return new JodaDateTime(this.instance.minusMillis(milliseconds));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusMinutes(int)
     */
    public org.jboss.dna.graph.property.DateTime minusMinutes( int minutes ) {
        return new JodaDateTime(this.instance.minusMinutes(minutes));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusMonths(int)
     */
    public org.jboss.dna.graph.property.DateTime minusMonths( int months ) {
        return new JodaDateTime(this.instance.minusMonths(months));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusSeconds(int)
     */
    public org.jboss.dna.graph.property.DateTime minusSeconds( int seconds ) {
        return new JodaDateTime(this.instance.minusSeconds(seconds));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusWeeks(int)
     */
    public org.jboss.dna.graph.property.DateTime minusWeeks( int weeks ) {
        return new JodaDateTime(this.instance.minusWeeks(weeks));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#minusYears(int)
     */
    public org.jboss.dna.graph.property.DateTime minusYears( int years ) {
        return new JodaDateTime(this.instance.minusYears(years));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plus(long, java.util.concurrent.TimeUnit)
     */
    public org.jboss.dna.graph.property.DateTime plus( long timeAmount,
                                                         TimeUnit unit ) {
        CheckArg.isNotNull(unit, "unit");
        return new JodaDateTime(this.instance.plus(TimeUnit.MILLISECONDS.convert(timeAmount, unit)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusDays(int)
     */
    public org.jboss.dna.graph.property.DateTime plusDays( int days ) {
        return new JodaDateTime(this.instance.plusDays(days));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusHours(int)
     */
    public org.jboss.dna.graph.property.DateTime plusHours( int hours ) {
        return new JodaDateTime(this.instance.plusHours(hours));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusMillis(int)
     */
    public org.jboss.dna.graph.property.DateTime plusMillis( int milliseconds ) {
        return new JodaDateTime(this.instance.plusMillis(milliseconds));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusMinutes(int)
     */
    public org.jboss.dna.graph.property.DateTime plusMinutes( int minutes ) {
        return new JodaDateTime(this.instance.plusMinutes(minutes));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusMonths(int)
     */
    public org.jboss.dna.graph.property.DateTime plusMonths( int months ) {
        return new JodaDateTime(this.instance.plusMonths(months));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusSeconds(int)
     */
    public org.jboss.dna.graph.property.DateTime plusSeconds( int seconds ) {
        return new JodaDateTime(this.instance.plusSeconds(seconds));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusWeeks(int)
     */
    public org.jboss.dna.graph.property.DateTime plusWeeks( int weeks ) {
        return new JodaDateTime(this.instance.plusWeeks(weeks));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.DateTime#plusYears(int)
     */
    public org.jboss.dna.graph.property.DateTime plusYears( int years ) {
        return new JodaDateTime(this.instance.plusYears(years));
    }

}

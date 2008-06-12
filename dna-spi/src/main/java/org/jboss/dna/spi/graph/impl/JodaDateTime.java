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
package org.jboss.dna.spi.graph.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Implementation of DateTime based upon the Joda-Time library.
 * @author Randall Hauch
 */
@Immutable
public class JodaDateTime implements org.jboss.dna.spi.graph.DateTime {

    /**
     */
    private static final long serialVersionUID = -730188225988292422L;

    private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;

    private final DateTime instance;
    private transient String formattedString;

    public JodaDateTime() {
        this.instance = new DateTime();
    }

    public JodaDateTime( String iso8601 ) {
        this.instance = new DateTime(iso8601);
    }

    public JodaDateTime( String iso8601, String timeZoneId ) {
        this.instance = new DateTime(iso8601, DateTimeZone.forID(timeZoneId));
    }

    public JodaDateTime( long milliseconds ) {
        this.instance = new DateTime(milliseconds);
    }

    public JodaDateTime( long milliseconds, Chronology chronology ) {
        this.instance = new DateTime(milliseconds, chronology);
    }

    public JodaDateTime( DateTimeZone dateTimeZone ) {
        this.instance = new DateTime(dateTimeZone);
    }

    public JodaDateTime( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond);
    }

    public JodaDateTime( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, Chronology chronology ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond, chronology);
    }

    public JodaDateTime( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, DateTimeZone dateTimeZone ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond, dateTimeZone);
    }

    public JodaDateTime( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, int timeZoneOffsetHours ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond, DateTimeZone.forOffsetHours(timeZoneOffsetHours));
    }

    public JodaDateTime( int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond, String timeZoneId ) {
        this.instance = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisecondsOfSecond, DateTimeZone.forID(timeZoneId));
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
        if (this.formattedString == null) {
            // This is transient and can be done multiple times by concurrent threads (without locking),
            // since the object is immutable
            this.formattedString = this.instance.toString(org.joda.time.format.ISODateTimeFormat.dateTime());
        }
        return this.formattedString;
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
    public int compareTo( org.jboss.dna.spi.graph.DateTime that ) {
        return this.instance.compareTo(that);
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
    public org.jboss.dna.spi.graph.DateTime toUtcTimeZone() {
        DateTime jodaTime = this.instance.withZone(DateTimeZone.forID("UTC"));
        return new JodaDateTime(jodaTime);
    }

    /**
     * {@inheritDoc}
     */
    public org.jboss.dna.spi.graph.DateTime toTimeZone( String timeZoneId ) {
        ArgCheck.isNotNull(timeZoneId, "time zone identifier");
        DateTime jodaTime = this.instance.withZone(DateTimeZone.forID(timeZoneId));
        return new JodaDateTime(jodaTime);
    }

}

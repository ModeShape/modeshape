/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.modeshape.jdbc.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TimestampWithTimezoneTest  {
	
    public static DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
    public static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    public static DateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss a"); //$NON-NLS-1$


    @Before
    public void setUp() {
        TimestampWithTimezone.resetCalendar(TimeZone.getTimeZone("America/Chicago")); //$NON-NLS-1$ 
    }

    @After
    public void tearDown() {
        TimestampWithTimezone.resetCalendar(null);
    }
    
    /**
     * Validate the conversion of the initial calendar to the targeted calendar should match the expected date result.
     * 
     * @param initial
     * @param target
     * @param expected
     * @throws Exception 
     */
    public void helpIsDateSame( Calendar initial,
    						  Calendar target,
    						  Date expected) throws Exception {
            
           Date d =  TimestampWithTimezone.createDate(initial, target);
           
           DATE_FORMAT.setCalendar(target); 
           String result = DATE_FORMAT.format(d);
           
           String expresult = DATE_FORMAT.format(expected);
            
          assertThat(result, is(expresult));
     }
    
    /**
     * Validate the conversion of the initial calendar to the targeted calendar should match the expected date result.
     * 
     * @param initial
     * @param target
     * @param time
     * @throws Exception 
     */
    public void helpIsTimeSame( Calendar initial,
    						  Calendar target,
    						  Time time) throws Exception {
            
           Time d =  TimestampWithTimezone.createTime(initial, target);
           
           TIME_FORMAT.setCalendar(target); 
           String result = TIME_FORMAT.format(d);
           
           String expresult = TIME_FORMAT.format(time);
           
           System.out.println(time.toString() + " - " + expresult);
            
          assertThat(result, is(expresult));
     }    

    @Test
    public void testDateTimezoneSame() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        cal_initial.set(2004, Calendar.JUNE, 23, 0, 0, 00);
        
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("GMT-06:00"));

        Date d = java.sql.Date.valueOf("2004-06-23");
        helpIsDateSame(cal_initial, cal_target, d);
    }    
    
    @Test
    public void testDateDiffTimezone1() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        cal_initial.set(2004, Calendar.JUNE, 23, 0, 0, 00);
       
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));

        Date d = java.sql.Date.valueOf("2004-06-23");
        helpIsDateSame(cal_initial, cal_target, d);
    	
    }
 
    @Test
    public void testDateDiffTimezone2() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        cal_initial.set(2004, Calendar.JUNE, 23, 23, 15, 00);
       
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));

        Date d = java.sql.Date.valueOf("2004-06-24");
        helpIsDateSame(cal_initial, cal_target, d);
    	
    }  
    
    @Test
    public void testTimeTimezoneSame() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        cal_initial.set(2004, Calendar.JUNE, 23, 15, 39, 10);
       
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("GMT-06:00"));

        Time d = java.sql.Time.valueOf("15:39:10");
        helpIsTimeSame(cal_initial, cal_target, d);
    }    
    
    @Ignore
    @Test
    public void testTimeDiffTimezone1() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));
        cal_initial.set(2004, Calendar.JUNE, 23, 22, 39, 10);
       
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));

        Time d = java.sql.Time.valueOf("03:39:10");
        helpIsTimeSame(cal_initial, cal_target, d);
    	
    } 
    
    @Ignore
    @Test
    public void testTimeDiffTimezone2() throws Exception {
        GregorianCalendar cal_initial = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));
        cal_initial.set(2004, Calendar.JUNE, 23, 03, 39, 10);
       
        GregorianCalendar cal_target = new GregorianCalendar(TimeZone.getTimeZone("America/Chicago"));

        Time d = java.sql.Time.valueOf("22:39:10");
        helpIsTimeSame(cal_initial, cal_target, d);
    	
    }     
    
    
//    @Test
//    public void testTimezone2() throws Exception {
//        helpTestSame("2004-06-29 15:39:10", 1, "GMT-08:00", //$NON-NLS-1$ //$NON-NLS-2$
//                     "GMT-06:00"); //$NON-NLS-1$ 
//    }
//    @Test
//    public void testTimezone3() throws Exception {
//        helpTestSame("2004-08-31 18:25:54", 1, "Europe/London", //$NON-NLS-1$ //$NON-NLS-2$
//                     "GMT"); //$NON-NLS-1$ 
//    }
//    @Test
//    public void testTimezoneOverMidnight() throws Exception {
//        helpTestSame("2004-06-30 23:39:10", 1, "America/Los_Angeles", //$NON-NLS-1$ //$NON-NLS-2$
//                     "America/Chicago"); //$NON-NLS-1$ 
//    }
//    @Test
//    public void testCase2852() throws Exception {
//        helpTestSame("2005-05-17 22:35:33", 508659, "GMT", //$NON-NLS-1$ //$NON-NLS-2$
//                     "America/New_York"); //$NON-NLS-1$ 
//    }
    @Test
    public void testCreateDate() {        
        GregorianCalendar cal_target = new GregorianCalendar();
        cal_target.set(2004, Calendar.JUNE, 30, 0, 0, 00);
        cal_target.set(Calendar.MILLISECOND, 0);
        
        Date date = TimestampWithTimezone.createDate(cal_target);

        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(date.getTime());

        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat(cal.get(Calendar.MINUTE), is(0));
        assertThat(cal.get(Calendar.SECOND), is(0));
        assertThat(cal.get(Calendar.MILLISECOND), is(0));
        assertThat(cal.get(Calendar.YEAR), is(2004));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JUNE));
        assertThat(cal.get(Calendar.DATE), is(30));

    }

    public void testCreateTime() {
        GregorianCalendar cal_target = new GregorianCalendar();
        cal_target.set(2004, Calendar.JUNE, 30, 23, 39, 10);
        cal_target.set(Calendar.MILLISECOND, 120);
        
        Time date = TimestampWithTimezone.createTime(cal_target);

        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(date.getTime());

        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(23));
        assertThat(cal.get(Calendar.MINUTE), is(39));
        assertThat(cal.get(Calendar.SECOND), is(10));
        assertThat(cal.get(Calendar.MILLISECOND), is(0));
        assertThat(cal.get(Calendar.YEAR), is(2004));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JUNE));
        assertThat(cal.get(Calendar.DATE), is(30));
        
    }
    
    public void testCreateTimeStamp() {
        GregorianCalendar cal_target = new GregorianCalendar();
        cal_target.set(2004, Calendar.JUNE, 30, 23, 39, 10);
        cal_target.set(Calendar.MILLISECOND, 120);
        
        Timestamp date = TimestampWithTimezone.createTimestamp(cal_target);

        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(date.getTime());

        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(23));
        assertThat(cal.get(Calendar.MINUTE), is(39));
        assertThat(cal.get(Calendar.SECOND), is(10));
        assertThat(cal.get(Calendar.MILLISECOND), is(120));
        assertThat(cal.get(Calendar.YEAR), is(2004));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JUNE));
        assertThat(cal.get(Calendar.DATE), is(30));
        
    }    
    
    
    @Test
    public void testDateToDateConversion2() {
        Calendar localTime = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
        
        localTime.set(Calendar.MONTH, Calendar.JUNE);
        localTime.set(Calendar.DAY_OF_MONTH, 22);
        localTime.set(Calendar.YEAR, 2004);
        localTime.set(Calendar.HOUR, 23);
	        localTime.set(Calendar.MINUTE, 15);
	        localTime.set(Calendar.SECOND, 20);
	        localTime.set(Calendar.AM_PM, Calendar.PM);

        Date converted = TimestampWithTimezone.createDate(localTime, Calendar.getInstance(TimeZone.getTimeZone("Europe/London"))); //$NON-NLS-1$ //$NON-NLS-2$        
        Calendar cal = Calendar.getInstance();
        cal.setTime(converted);

        assertThat(cal.get(Calendar.MILLISECOND), is(0));
    }
   

}

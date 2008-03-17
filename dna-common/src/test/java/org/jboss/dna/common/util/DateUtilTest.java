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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import java.util.Calendar;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class DateUtilTest {

    private Calendar calendar;
    private Date validDate;

    @Before
    public void beforeEach() {
        this.calendar = Calendar.getInstance();
        this.calendar.set(2008, 1, 3, 14, 22, 49);
        this.validDate = this.calendar.getTime();
    }

    @Test
    public void shouldConvertDateToStringUsingStandardFormat() {
        assertThat(DateUtil.getDateAsStandardString(this.validDate), containsString("2008-02-03T14:22:49"));
    }

    @Test
    public void shouldConvertDateToStringAndBack() throws Exception {
        String str = DateUtil.getDateAsStandardString(this.validDate);
        Date output = DateUtil.getDateFromStandardString(str);
        assertThat(output, is(this.validDate));
        String outputStr = DateUtil.getDateAsStandardString(output);
        assertThat(outputStr, is(str));
    }

    // @Test
    // public void shouldConvertStringToCalendarAndBack() throws Exception {
    // String input = "2008-02";
    // Calendar cal = DateUtil.getCalendarFromStandardString(input);
    // String output = DateUtil.getDateAsStandardString(cal);
    // assertThat(output, is(input));
    // }

    @Test
    public void shouldConvertStringToDateWithDelimitersAndNoTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("2008-02-03T14:22:49.111");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(false));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
    }

    @Test
    public void shouldConvertStringToDateWithNoDelimitersAndNoTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("20080203T142249.111");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(false));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
    }

    @Test
    public void shouldConvertStringToDateWithDelimitersAndUtcTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("2008-02-03T14:22:49.111Z");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(true));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
        assertThat(cal.get(Calendar.ZONE_OFFSET), is(0)); // in milliseconds
    }

    @Test
    public void shouldConvertStringToDateWithDelimitersAndHourTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("2008-02-03T14:22:49.111-06");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(true));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
        assertThat(cal.get(Calendar.ZONE_OFFSET), is(-6 * 60 * 60 * 1000)); // in milliseconds
    }

    @Test
    public void shouldConvertStringToDateWithDelimitersAndHourAndMinuteTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("2008-02-03T14:22:49.111-0622");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(true));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
        assertThat(cal.get(Calendar.ZONE_OFFSET), is((-6 * 60 - 22) * 60 * 1000)); // in milliseconds
    }

    @Test
    public void shouldConvertStringToDateWithDelimitersAndPositiveHourAndMinuteTimeZone() throws Exception {
        Calendar cal = DateUtil.getCalendarFromStandardString("2008-02-03T14:22:49.111+0622");
        assertThat(cal.isSet(Calendar.ZONE_OFFSET), is(true));
        assertThat(cal.isSet(Calendar.WEEK_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_YEAR), is(false));
        assertThat(cal.isSet(Calendar.DAY_OF_WEEK), is(false));
        assertThat(cal.get(Calendar.YEAR), is(2008));
        assertThat(cal.get(Calendar.MONTH), is(2 - 1)); // zero-based month!
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(14));
        assertThat(cal.get(Calendar.MINUTE), is(22));
        assertThat(cal.get(Calendar.SECOND), is(49));
        assertThat(cal.get(Calendar.MILLISECOND), is(111));
        assertThat(cal.get(Calendar.ZONE_OFFSET), is((+6 * 60 + 22) * 60 * 1000)); // in milliseconds
    }

    @Test
    public void shouldConvertDateStringsToCalendar() throws Exception {
        assertThat(DateUtil.getCalendarFromStandardString("2008").get(Calendar.YEAR), is(2008));
        assertThat(DateUtil.getCalendarFromStandardString("2008-02").get(Calendar.MONTH), is(2 - 1));
        assertThat(DateUtil.getCalendarFromStandardString("200802").get(Calendar.MONTH), is(2 - 1));
        assertThat(DateUtil.getCalendarFromStandardString("2008-02-16").get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(DateUtil.getCalendarFromStandardString("20080216").get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(DateUtil.getCalendarFromStandardString("2008216").get(Calendar.DAY_OF_YEAR), is(216));
        assertThat(DateUtil.getCalendarFromStandardString("2008-216").get(Calendar.DAY_OF_YEAR), is(216));
        assertThat(DateUtil.getCalendarFromStandardString("2008W216").get(Calendar.WEEK_OF_YEAR), is(21));
        assertThat(DateUtil.getCalendarFromStandardString("2008-W216").get(Calendar.WEEK_OF_YEAR), is(21));
    }

}

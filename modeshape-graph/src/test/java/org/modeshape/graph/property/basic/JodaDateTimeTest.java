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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.common.text.StringMatcher.startsWith;
import org.modeshape.graph.property.basic.JodaDateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JodaDateTimeTest {

    private JodaDateTime instant;
    private String iso8601instance;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        iso8601instance = "2008-05-10T13:22:04.678";
    }

    @Test
    public void shouldConstructWithIso8601FormattedStringWithoutZone() {
        instant = new JodaDateTime(iso8601instance, "UTC");
        assertThat(instant.getString(), startsWith(iso8601instance));
        assertThat(instant.getYearOfCentury(), is(8));
        assertThat(instant.getYear(), is(2008));
        assertThat(instant.getMonthOfYear(), is(5));
        assertThat(instant.getDayOfMonth(), is(10));
        assertThat(instant.getDayOfWeek(), is(6));
        assertThat(instant.getHourOfDay(), is(13));
        assertThat(instant.getMinuteOfHour(), is(22));
        assertThat(instant.getSecondOfMinute(), is(04));
        assertThat(instant.getMillisOfSecond(), is(678));
        assertThat(instant.getTimeZoneId(), is("UTC"));
        assertThat(instant.getTimeZoneOffsetHours(), is(0));
    }

    @Test
    public void shouldConstructWithIso8601FormattedString() {
        iso8601instance = "2008-05-10T13:22:04.678-04:00";
        instant = new JodaDateTime(iso8601instance);
        instant = (JodaDateTime)instant.toTimeZone("UTC");
        assertThat(instant.getString(), is("2008-05-10T17:22:04.678Z"));
        assertThat(instant.getYearOfCentury(), is(8));
        assertThat(instant.getYear(), is(2008));
        assertThat(instant.getMonthOfYear(), is(5));
        assertThat(instant.getDayOfMonth(), is(10));
        assertThat(instant.getDayOfWeek(), is(6));
        assertThat(instant.getHourOfDay(), is(17));
        assertThat(instant.getMinuteOfHour(), is(22));
        assertThat(instant.getSecondOfMinute(), is(04));
        assertThat(instant.getMillisOfSecond(), is(678));
        assertThat(instant.getTimeZoneId(), is("UTC"));
        assertThat(instant.getTimeZoneOffsetHours(), is(0));
    }

}

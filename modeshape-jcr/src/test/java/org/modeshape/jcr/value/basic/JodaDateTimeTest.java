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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.common.text.StringMatcher.startsWith;
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

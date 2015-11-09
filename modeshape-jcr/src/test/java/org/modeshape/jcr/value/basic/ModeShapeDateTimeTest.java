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
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Unit test for {@link ModeShapeDateTime}
 * 
 * @author Randall Hauch
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ModeShapeDateTimeTest {

    @Test
    public void shouldConstructWithIso8601FormattedStringWithoutZone() {
        String iso8601instance = "2008-05-10T13:22:04.678";
        ModeShapeDateTime instant = new ModeShapeDateTime(iso8601instance, "UTC");
        assertThat(instant.getString(), startsWith(iso8601instance));
        LocalDateTime localDateTime = instant.toLocalDateTime();
        assertThat(localDateTime.getYear(), is(2008));
        assertThat(localDateTime.getMonthValue(), is(5));
        assertThat(localDateTime.getDayOfMonth(), is(10));
        assertThat(localDateTime.getDayOfWeek().getValue(), is(6));
        assertThat(localDateTime.getHour(), is(13));
        assertThat(localDateTime.getMinute(), is(22));
        assertThat(localDateTime.getSecond(), is(04));
        assertThat(instant.getMillisOfSecond(), is(678));
        assertThat(instant.getTimeZoneId(), is("UTC"));
        assertThat(instant.getTimeZoneOffsetHours(), is(0));
    }

    @Test
    public void shouldConstructWithIso8601FormattedStringWithoutTime() {
        ModeShapeDateTime instant = new ModeShapeDateTime("2008-05-10");
        LocalDateTime localDateTime = instant.toLocalDateTime();
        assertThat(localDateTime.getYear(), is(2008));
        assertThat(localDateTime.getMonthValue(), is(5));
        assertThat(localDateTime.getDayOfMonth(), is(10));
        assertThat(localDateTime.getDayOfWeek().getValue(), is(6));
        assertThat(localDateTime.getHour(), is(0));
        assertThat(localDateTime.getMinute(), is(0));
        assertThat(localDateTime.getSecond(), is(0));
        assertThat(instant.getMillisOfSecond(), is(0));
        assertThat(instant.getTimeZoneId(), is("UTC"));
        assertThat(instant.getTimeZoneOffsetHours(), is(0));
    }

    @Test
    public void shouldConstructWithIso8601FormattedString() {
        String iso8601instance = "2008-05-10T13:22:04.678-04:00";
        ModeShapeDateTime instant = new ModeShapeDateTime(iso8601instance);
        instant = instant.toTimeZone("UTC");
        assertThat(instant.getString(), is("2008-05-10T17:22:04.678Z"));
        LocalDateTime localDateTime = instant.toLocalDateTime();
        assertThat(localDateTime.getYear(), is(2008));
        assertThat(localDateTime.getMonthValue(), is(5));
        assertThat(localDateTime.getDayOfMonth(), is(10));
        assertThat(localDateTime.getDayOfWeek().getValue(), is(6));
        assertThat(localDateTime.getHour(), is(17));
        assertThat(localDateTime.getMinute(), is(22));
        assertThat(localDateTime.getSecond(), is(04));
        assertThat(instant.getMillisOfSecond(), is(678));
        assertThat(instant.getTimeZoneId(), is("UTC"));
        assertThat(instant.getTimeZoneOffsetHours(), is(0));
    }

}

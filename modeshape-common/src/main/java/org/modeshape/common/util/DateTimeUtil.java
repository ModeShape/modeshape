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
package org.modeshape.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * Utility for interacting with various date and time objects.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class DateTimeUtil {

    /**
     * UTC zone id
     */
    public static final ZoneId UTC = ZoneId.of("UTC");
    
    /**
     * ISO 8601 formatter which attempts to be as close to the previous behavior (JODA) as possible. JDK 8 has some
     * significant differences especially when it comes to milliseconds, which it doesn't support out-of-the-box. 
     * 
     * However, because of this bug in JDK 8: 
     * 
     * - https://bugs.openjdk.java.net/browse/JDK-8031085 this expression
     * - http://bugs.java.com/view_bug.do?bug_id=8032491
     * 
     * is WAY MORE COMPLICATED than it should be (in reality is should use the .SSS pattern)
     */
    private static DateTimeFormatter JODA_ISO8601_FORMATTER = new DateTimeFormatterBuilder()
            .parseLenient()
            .appendPattern("uuuu-MM-dd['T'HH:mm:ss][.")
            .appendValue(ChronoField.MILLI_OF_SECOND, 3, 3, SignStyle.NEVER).optionalEnd()
            .appendPattern("[XXXXX]")
            .toFormatter();
                                                                                                    
    private DateTimeUtil() {
    }

    /**
     * Creates a {@link ZonedDateTime} instance based on the given pattern in ISO 8601 format, compatible with the Joda date-time
     * library. 
     * <p>
     * Note that there is no direct correspondence between the JODA-style dates and the new JDK 8 date, especially 
     * when it comes to handling milliseconds.
     * </p>
     * 
     * @param iso8601 a {@link String} representing a date and/or time pattern, may not be null
     * @return a {@link ZonedDateTime} instance, never {@code null}
     * 
     * @throws java.time.format.DateTimeParseException if the given pattern cannot be parsed
     */
    public static ZonedDateTime jodaParse( String iso8601 ) throws DateTimeParseException {
        CheckArg.isNotNull(iso8601, "iso8601");
        TemporalAccessor parse = JODA_ISO8601_FORMATTER.parse(iso8601);
        LocalDate localDate = LocalDate.from(parse);
        LocalTime localTime = parse.isSupported(ChronoField.HOUR_OF_DAY) ? LocalTime.from(parse) : LocalTime.MIDNIGHT;
        ZoneId zoneId = parse.isSupported(ChronoField.OFFSET_SECONDS) ? ZoneId.from(parse) : UTC;
        return ZonedDateTime.of(localDate, localTime, zoneId);    
    }

    /**
     * Returns the ISO8601 string of a given date-time instance with timezone information, trying to be as closed as possible
     * to what the JODA date-time library would return.
     * 
     * @param dateTime a {@link ZonedDateTime} instance, may not be null
     * @return a {@link String} representation of the date instance according to the ISO8601 standard
     */
    public static String jodaFormat( ZonedDateTime dateTime ) {
        CheckArg.isNotNull(dateTime, "dateTime");
        return dateTime.format(JODA_ISO8601_FORMATTER);
    }

    /**
     * Creates a new UTC {@link LocalDateTime} instance  based on the given millis value
     *
     * @param millis a positive amount of millis
     * @return a {@link LocalDateTime} instance.
     */
    public static LocalDateTime localDateTimeUTC( long millis ) {
        CheckArg.isPositive(millis, "millis");
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), UTC);
    }
}

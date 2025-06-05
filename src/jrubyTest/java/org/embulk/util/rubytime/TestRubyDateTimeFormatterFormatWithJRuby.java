/*
 * Copyright 2020 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests formatting by RubyDateTimeFormatter with JRuby's org.jruby.util.RubyDateFormat.
 *
 * <p>The main purpose of these tests is to confirm it works almost the same with org.jruby.util.RubyDateFormat.
 * Embulk's legacy {@code TimestampFormatter} has used {@code org.jruby.util.RubyDateFormat} for a long time.
 */
public class TestRubyDateTimeFormatterFormatWithJRuby {
    @ParameterizedTest
    @MethodSource("provideZonedDateTime")
    public void testZonedDateTime(final ZonedDateTime datetime) {
        assertRubyDateFormat(datetime);
    }

    @ParameterizedTest
    @MethodSource("provideOffsetDateTime")
    public void testOffsetDateTime(final OffsetDateTime datetime) {
        assertRubyDateFormat(datetime);
    }

    @SuppressWarnings("deprecation")  // For use of org.jruby.util.RubyDateFormat.
    private void assertRubyDateFormat(final ZonedDateTime datetime) {
        final String format = "%Y-%m-%dT%H:%M:%S %Z %#Z";

        final org.jruby.util.RubyDateFormat jrubyFormat = new org.jruby.util.RubyDateFormat(format, Locale.ROOT, true);
        final DateTime jodaDateTime = new DateTime(
                datetime.toInstant().toEpochMilli(),
                DateTimeZone.forTimeZone(TimeZone.getTimeZone(datetime.getZone())));
        jrubyFormat.setDateTime(jodaDateTime);
        jrubyFormat.setNSec(datetime.getNano() / 1000);
        final String expected = jrubyFormat.format(null);

        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        final String actual = formatter.formatWithZoneNameStyle(datetime, RubyDateTimeFormatter.ZoneNameStyle.SHORT);

        System.out.println("Expected: " + expected);
        System.out.println("  Actual: " + actual);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("deprecation")  // For use of org.jruby.util.RubyDateFormat.
    private void assertRubyDateFormat(final OffsetDateTime datetime) {
        final String format = "%Y-%m-%dT%H:%M:%S %Z %#Z";

        final org.jruby.util.RubyDateFormat jrubyFormat = new org.jruby.util.RubyDateFormat(format, Locale.ROOT, true);
        final DateTime jodaDateTime = new DateTime(
                datetime.toInstant().toEpochMilli(),
                DateTimeZone.forOffsetMillis(datetime.getOffset().getTotalSeconds() * 1000));
        jrubyFormat.setDateTime(jodaDateTime);
        jrubyFormat.setNSec(datetime.getNano() / 1000);
        final String expected = jrubyFormat.format(null);

        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        final String actual = formatter.formatWithZoneNameStyle(datetime, RubyDateTimeFormatter.ZoneNameStyle.SHORT);

        System.out.println("Expected: " + expected);
        System.out.println("  Actual: " + actual);
        assertEquals(expected, actual);
    }

    private static Stream<ZonedDateTime> provideZonedDateTime() {
        return Arrays.stream(ZONED_DATE_TIMES);
    }

    private static Stream<OffsetDateTime> provideOffsetDateTime() {
        return Arrays.stream(OFFSET_DATE_TIMES);
    }

    private static final ZonedDateTime[] ZONED_DATE_TIMES = {
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Tokyo")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Tokyo")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("America/Los_Angeles")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("America/Los_Angeles")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("America/Chicago")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("America/Chicago")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Harbin")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Harbin")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("Europe/London")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("Europe/London")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("Africa/Windhoek")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("Africa/Windhoek")),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneId.of("America/Grand_Turk")),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneId.of("America/Grand_Turk")),

        // The tzdb embedded in JRuby 9.1.15.0 / Joda-Time 2.9.2 may be just too old.
        // They don't catch up with the latest daylight saving time status -- then testing with older date.
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("America/Santiago")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("America/Santiago")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Baku")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Baku")),

        // The timezone of Asia/Choibalsan was updated retroactively (like "backdate").
        // https://github.com/eggert/tz/commit/7eb5bf887927e35079e5cc12e2252819a7c47bb0
        //
        // The tzdb had believed :
        // "Asia/Choibalsan had been in UTC+9 until 2008-03-31"
        //
        // However, the tzdb was updated retroactively between 2024a and 2024b so that :
        // "Asia/Choibalsan has been in UTC+8 in all the time since 1979"
        //
        // Then, the interpretation of "1996-01-03T02:00:45 [Asia/Choibalsan]" has changed :
        // - Before: 1996-01-03T02:00:45 +09:00 [Asia/Choibalsan]
        // - After:  1996-01-03T02:00:45 +08:00 [Asia/Choibalsan]
        //
        // It'd be discussed whether the behavior of embulk-util-rubytime should change or not.
        // Until then, we disable the test cases about Asia/Choibalsan, and test it with later date.
        // ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Choibalsan")),
        // ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Choibalsan")),
        ZonedDateTime.of(2009, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Choibalsan")),
        ZonedDateTime.of(2009, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Choibalsan")),

        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Hovd")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Hovd")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Istanbul")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Istanbul")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Ulaanbaatar")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Ulaanbaatar")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Asia/Ulan_Bator")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Asia/Ulan_Bator")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Chile/Continental")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Chile/Continental")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Chile/EasterIsland")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Chile/EasterIsland")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Europe/Istanbul")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Europe/Istanbul")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Pacific/Easter")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Pacific/Easter")),
        ZonedDateTime.of(1996, 1, 3, 2, 0, 45, 0, ZoneId.of("Turkey")),
        ZonedDateTime.of(1996, 7, 3, 2, 0, 45, 0, ZoneId.of("Turkey")),

        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.UTC),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneOffset.UTC),
        ZonedDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.ofHours(9)),
        ZonedDateTime.of(2017, 7, 3, 2, 0, 45, 0, ZoneOffset.ofHours(9)),
    };

    private static final OffsetDateTime[] OFFSET_DATE_TIMES = {
        OffsetDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.UTC),
        OffsetDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.ofTotalSeconds(0)),
        OffsetDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.ofTotalSeconds(7)),
        OffsetDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.ofHoursMinutes(11, 15)),
        OffsetDateTime.of(2017, 1, 3, 2, 0, 45, 0, ZoneOffset.ofHours(9)),
    };
}

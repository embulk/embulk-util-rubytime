/*
 * Copyright 2018 The Embulk project
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing by RubyDateTimeFormatter.
 */
public class TestRubyDateTimeFormatterParse {
    @Test
    public void testMultipleEpochs() {
        final TemporalAccessor parsed1 = strptime("123456789 12849124", "%Q %s");
        assertEquals(1000L * 12849124L, parsed1.getLong(RubyChronoFields.Field.INSTANT_MILLIS));

        final TemporalAccessor parsed2 = strptime("123456789 12849124", "%s %Q");
        assertEquals(1000L * 3212281L / 250L, parsed2.getLong(RubyChronoFields.Field.INSTANT_MILLIS));
    }

    @Test
    public void testEpochWithFraction() {
        assertParsedTime("1500000000.123456789", "%s.%N", Instant.ofEpochSecond(1500000000L, 123456789));
        assertParsedTime("1500000000456.111111111", "%Q.%N", Instant.ofEpochSecond(1500000000L, 567111111));
        assertParsedTime("1500000000.123", "%s.%L", Instant.ofEpochSecond(1500000000L, 123000000));
        assertParsedTime("1500000000456.111", "%Q.%L", Instant.ofEpochSecond(1500000000L, 567000000));
    }

    // Alternative of TestRubyDateTimeFormatterParseWithJRuby#testTestTimeExtension_test_strptime_s_N that is ignored
    // for the precision of the fraction part.
    @Test
    public void test_Ruby_test_time_test_strptime_s_N() {
        assertParsedTime("1.5", "%s.%N", Instant.ofEpochSecond(1L, 500000000));
        assertParsedTime("-1.5", "%s.%N", Instant.ofEpochSecond(-2L, 500000000));
        assertParsedTime("1.000000001", "%s.%N", Instant.ofEpochSecond(1L, 1));
        assertParsedTime("-1.000000001", "%s.%N", Instant.ofEpochSecond(-2L, 999999999));
    }

    @Test
    public void testLeapSeconds() {
        assertParsedTime("2008-12-31T23:56:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767760L, 0));
        assertParsedTime("2008-12-31T23:59:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767940L, 0));
        assertParsedTime("2008-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230767999L, 0));
        assertParsedTime("2008-12-31T23:59:60", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768000L, 0));
        assertParsedTime("2009-01-01T00:00:01", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768001L, 0));
        assertParsedTime("2009-01-01T00:01:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768060L, 0));
        assertParsedTime("2009-01-01T00:03:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1230768180L, 0));
    }

    @Test
    public void testLarge() {
        assertParsedTime("-999999999-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(-31557014135596800L, 0));
        // assertFailParse("-1000000000-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S");
        assertParsedTime("999999999-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(31556889832780799L, 0));
        // assertFailParse("1000000000-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertParsedTime("9223372036854775", "%s", Instant.ofEpochSecond(9223372036854775L, 0));
        // assertFailParse("9223372036854776", "%s");
        assertParsedTime("-9223372036854775", "%s", Instant.ofEpochSecond(-9223372036854775L, 0));
        // assertFailParse("-9223372036854776", "%s");

        assertParsedTime("9223372036854775807", "%Q", Instant.ofEpochSecond(9223372036854775L, 807000000));
        // assertFailParse("9223372036854775808", "%Q");
        assertParsedTime("-9223372036854775807", "%Q", Instant.ofEpochSecond(-9223372036854776L, 193000000));
        // assertFailParse("-9223372036854775808", "%Q");
    }

    @Test
    public void testSubseconds() {
        // assertFailParse("2007-08-01T00:00:00.", "%Y-%m-%dT%H:%M:%S.%N");
        // assertFailParse("2007-08-01T00:00:00.-777777777", "%Y-%m-%dT%H:%M:%S.%N");
        assertParsedTime("2007-08-01T00:00:00.777777777",
                         "%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
        assertParsedTime("2007-08-01T00:00:00.77777777777777",
                         "%Y-%m-%dT%H:%M:%S.%N",
                         Instant.ofEpochSecond(1185926400L, 777777777));
    }

    @Test
    public void testDateTimeFromInstant() {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern("%Q.%N");
        final TemporalAccessor parsedResolved = formatter.parse("1500000000456.111111111");

        final OffsetDateTime datetime = OffsetDateTime.from(parsedResolved);
        assertEquals(OffsetDateTime.of(2017, 7, 14, 02, 40, 00, 567111111, ZoneOffset.UTC), datetime);
    }

    private static TemporalAccessor strptime(final String string, final String format) {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        return formatter.parseUnresolved(string);
    }

    private static void assertParsedTime(final String string, final String format, final Instant expected) {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        final TemporalAccessor parsedResolved = formatter.parse(string);

        final Instant actualInstant = Instant.from(parsedResolved);
        assertEquals(expected, actualInstant);
    }
}

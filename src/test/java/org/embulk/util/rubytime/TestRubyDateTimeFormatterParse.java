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
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing by RubyDateTimeFormatter.
 */
public class TestRubyDateTimeFormatterParse {
    @Test
    public void testMultipleEpochs() {
        final TemporalAccessor parsed1 = strptime("123456789 12849124", "%Q %s");
        assertEquals(12849124L, parsed1.getLong(ChronoField.INSTANT_SECONDS));
        assertEquals(0, parsed1.get(RubyChronoFields.NANO_OF_INSTANT_SECONDS));

        final TemporalAccessor parsed2 = strptime("123456789 12849124", "%s %Q");
        assertEquals(12849L, parsed2.getLong(ChronoField.INSTANT_SECONDS));
        assertEquals(124000000, parsed2.get(RubyChronoFields.NANO_OF_INSTANT_SECONDS));
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

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_iso8601() {
        // "Ruby" timestamp parser takes second = 60 as next second.
        assertParsedTime("2001-02-03", "%Y-%m-%d", 981158400L);
        assertParsedTime("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S", 981244800L);
        assertParsedTime("2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 981212400L);
        assertParsedTime("-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -125309754000L);
        assertParsedTime("+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", 327406287600L);
        assertParsedTime("-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", -451734829200L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_date1() {
        // The time zone ID "EST" is recognized in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y", 933284381L);

        // The time zone IDs "MET", "AMT", "AST", and "DST" are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 AMT 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 AMT -1999", "%a %b %d %H:%M:%S %Z %Y", -125231383219L);
        assertParsedTime("Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y", -125231383219L);

        // All "GMT", "GMT+..." and "GMT-..." are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);

        // String-ish time zone IDs are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
        assertParsedTime("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y", 933266381L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_rfc822() {
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);

        // "PDT" (Pacific Daylight Time) is correctly recognized as -07:00 in Ruby parser, not like the legacy parser.
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z", 933267261L);

        assertParsedTime("Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z", 933242061L);
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z", 933209661L);
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z", 933225861L);
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", 933258261L);
        assertParsedTime("Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", -125231391339L);
    }

    @Test  // Imported from test__strptime__3 in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__3_etc() {
        assertParsedTime("06-DEC-99", "%d-%b-%y", 944438400L);
        assertParsedTime("sUnDay oCtoBer 31 01", "%A %B %d %y", 1004486400L);
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        assertParsedTime("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y", 939945600L);
        assertParsedTime("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y", 939945600L);

        assertParsedTime("09:02:11 AM", "%I:%M:%S %p", 32531L);
        assertParsedTime("09:02:11 A.M.", "%I:%M:%S %p", 32531L);
        assertParsedTime("09:02:11 PM", "%I:%M:%S %p", 75731L);
        assertParsedTime("09:02:11 P.M.", "%I:%M:%S %p", 75731L);

        assertParsedTime("12:33:44 AM", "%r", 2024L);
        assertParsedTime("01:33:44 AM", "%r", 5624L);
        assertParsedTime("11:33:44 AM", "%r", 41624L);
        assertParsedTime("12:33:44 PM", "%r", 45224L);
        assertParsedTime("01:33:44 PM", "%r", 48824L);
        assertParsedTime("11:33:44 PM", "%r", 84824L);

        assertParsedTime("11:33:44 PM AMT", "%I:%M:%S %p %Z", 84824L);
        assertParsedTime("11:33:44 P.M. AMT", "%I:%M:%S %p %Z", 84824L);
        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "-04:00" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        assertParsedTime("11:33:44 PM -04:00", "%I:%M:%S %p %Z", 99224L);
        assertParsedTime("11:33:44 P.M. -04:00", "%I:%M:%S %p %Z", 99224L);

        assertParsedTime("fri1feb034pm+5", "%a%d%b%y%H%p%Z", 1044115200L);
        // The time zone offset is just "+5" in Ruby v2.3.1's tests, but "+05" is used here instead.
        // "+5" is not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("fri1feb034pm+05", "%a%d%b%y%H%p%Z", 1044097200L);
    }

    @Test  // Imported from test__strptime__width in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__width() {
        // Default dates are always 1970-01-01 in Ruby parser. If only the year is specified, the date is 01-01.
        assertParsedTime("99", "%y", 915148800L);
        assertParsedTime("01", "%y", 978307200L);
        assertParsedTime("19 99", "%C %y", 915148800L);
        assertParsedTime("20 01", "%C %y", 978307200L);
        assertParsedTime("30 99", "%C %y", 35627817600L);
        assertParsedTime("30 01", "%C %y", 32535216000L);
        assertParsedTime("1999", "%C%y", 915148800L);
        assertParsedTime("2001", "%C%y", 978307200L);
        assertParsedTime("3099", "%C%y", 35627817600L);
        assertParsedTime("3001", "%C%y", 32535216000L);

        assertParsedTime("20060806", "%Y", 632995724851200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("20060806", "%Y ", 632995724851200L);
        assertParsedTime("20060806", "%Y%m%d", 1154822400L);
        assertParsedTime("2006908906", "%Y9%m9%d", 1154822400L);
        assertParsedTime("12006 08 06", "%Y %m %d", 316724342400L);
        assertParsedTime("12006-08-06", "%Y-%m-%d", 316724342400L);
        assertParsedTime("200608 6", "%Y%m%e", 1154822400L);

        // Day of the year (yday; DAY_OF_YEAR) is not recognized, and handled as January 1, in Ruby parser.
        assertParsedTime("2006333", "%Y%j", 1136073600L);
        assertParsedTime("20069333", "%Y9%j", 1136073600L);
        assertParsedTime("12006 333", "%Y %j", 316705593600L);
        assertParsedTime("12006-333", "%Y-%j", 316705593600L);

        assertParsedTime("232425", "%H%M%S", 84265L);
        assertParsedTime("23924925", "%H9%M9%S", 84265L);
        assertParsedTime("23 24 25", "%H %M %S", 84265L);
        assertParsedTime("23:24:25", "%H:%M:%S", 84265L);
        assertParsedTime(" 32425", "%k%M%S", 12265L);
        assertParsedTime(" 32425", "%l%M%S", 12265L);

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    @Test  // Imported from test__strptime__fail in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby__strptime__fail() {
        assertParsedTime("2001.", "%Y.", 978307200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("2001. ", "%Y.", 978307200L);
        assertParsedTime("2001.", "%Y. ", 978307200L);
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("2001. ", "%Y. ", 978307200L);

        // failRubyToParse("2001", "%Y.");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        // failRubyToParse("2001 ", "%Y.");
        // failRubyToParse("2001", "%Y. ");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        // failRubyToParse("2001 ", "%Y. ");

        // failRubyToParse("2001-13-31", "%Y-%m-%d");
        // failRubyToParse("2001-12-00", "%Y-%m-%d");
        // failRubyToParse("2001-12-32", "%Y-%m-%d");
        // failRubyToParse("2001-12-00", "%Y-%m-%e");
        // failRubyToParse("2001-12-32", "%Y-%m-%e");
        // failRubyToParse("2001-12-31", "%y-%m-%d");

        // failRubyToParse("2004-000", "%Y-%j");
        // failRubyToParse("2004-367", "%Y-%j");
        // failRubyToParse("2004-366", "%y-%j");

        assertParsedTime("24:59:59", "%H:%M:%S", 89999L);
        assertParsedTime("24:59:59", "%k:%M:%S", 89999L);
        assertParsedTime("24:59:60", "%H:%M:%S", 90000L);
        assertParsedTime("24:59:60", "%k:%M:%S", 90000L);

        // failRubyToParse("24:60:59", "%H:%M:%S");
        // failRubyToParse("24:60:59", "%k:%M:%S");
        // failRubyToParse("24:59:61", "%H:%M:%S");
        // failRubyToParse("24:59:61", "%k:%M:%S");
        // failRubyToParse("00:59:59", "%I:%M:%S");
        // failRubyToParse("13:59:59", "%I:%M:%S");
        // failRubyToParse("00:59:59", "%l:%M:%S");
        // failRubyToParse("13:59:59", "%l:%M:%S");

        // TODO: assertParsedTime("0", "%U", 0L);
        // failRubyToParse("54", "%U");
        // TODO: assertParsedTime("0", "%W", 0L);
        // failRubyToParse("54", "%W");
        // failRubyToParse("0", "%V");
        // failRubyToParse("54", "%V");
        // failRubyToParse("0", "%u");
        // TODO: assertParsedTime("7", "%u", 0L);
        // TODO: assertParsedTime("0", "%w", 0L);
        // failRubyToParse("7", "%w");

        // failRubyToParse("Sanday", "%A");
        // failRubyToParse("Jenuary", "%B");
        // TODO: assertParsedTime("Sundai", "%A", 0L);
        assertParsedTime("Januari", "%B", 0L);
        // failRubyToParse("Sundai,", "%A,");
        // failRubyToParse("Januari,", "%B,");
    }

    @Test  // Imported partially from test_strptime in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby_strptime() {
        assertParsedTime("2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z", 1016104953L);
        assertParsedTime("2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z", 1016072553L);
        assertParsedTime("2002-03-14T11:22:33-09:00", "%FT%T%Z", 1016137353L);
        assertParsedTime("2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z", 1016137353L, 123456789);
    }

    @Test  // Imported from test_strptime__minus in Ruby v2.3.1's test/date/test_date_strptime.rb.
    public void testRuby_strptime__minus() {
        assertParsedTime("-1", "%s", -1L);
        assertParsedTime("-86400", "%s", -86400L);

        // In |java.time.Instant|, it is always 0 <= nanoAdjustment < 1,000,000,000.
        // -0.9s is represented like -1s + 100ms.
        assertParsedTime("-999", "%Q", -1L, 1000000);
        // TODO: assertParsedTime("-1000", "%Q", -1L);
    }

    @Test
    public void testRubyEpochWithFraction() {
        assertParsedTime("1500000000.123456789", "%s.%N", 1500000000L, 123456789);
        assertParsedTime("1500000000456.111111111", "%Q.%N", 1500000000L, 567111111);
        assertParsedTime("1500000000.123", "%s.%L", 1500000000L, 123000000);
        assertParsedTime("1500000000456.111", "%Q.%L", 1500000000L, 567000000);

        assertParsedTime("1.5", "%s.%N", 1L, 500000000);
        assertParsedTime("-1.5", "%s.%N", -2L, 500000000);
        assertParsedTime("1.000000001", "%s.%N", 1L, 1);
        assertParsedTime("-1.000000001", "%s.%N", -2L, 999999999);
    }

    @Test
    public void testRubyExcessDate() {
        // failRubyToParse("2018-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        // failRubyToParse("2016-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        // failRubyToParse("2018-11-31T00:00:00", "%Y-%m-%dT%H:%M:%S");

        // failRubyToParse("2018-10-32T00:00:00", "%Y-%m-%dT%H:%M:%S");
        // failRubyToParse("2018-13-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        // failRubyToParse("2018-00-01T00:00:00", "%Y-%m-%dT%H:%M:%S");
        // failRubyToParse("2018-01-00T00:00:00", "%Y-%m-%dT%H:%M:%S");
    }

    private static TemporalAccessor strptime(final String string, final String format) {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        return formatter.parseUnresolved(string);
    }

    private static void assertParsedTime(final String string, final String format, final long seconds, final int nano) {
        assertParsedTime(string, format, Instant.ofEpochSecond(seconds, nano));
    }

    private static void assertParsedTime(final String string, final String format, final long expected) {
        assertParsedTime(string, format, Instant.ofEpochSecond(expected));
    }

    private static void assertParsedTime(final String string, final String format, final Instant expected) {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        final TemporalAccessor parsedResolved = formatter.parse(string);

        final Instant actualInstant = Instant.from(parsedResolved);
        assertEquals(expected, actualInstant);
    }
}

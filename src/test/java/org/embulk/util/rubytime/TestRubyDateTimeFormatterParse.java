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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing by RubyDateTimeFormatter.
 */
public class TestRubyDateTimeFormatterParse {
    /**
     * Tests ISO8601.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L123">test__strptime__3</a>
     */
    @Test
    public void testIso8601() {
        assertParsedTime("2001-02-03", "%Y-%m-%d", Instant.ofEpochSecond(981158400L));
    }

    /**
     * Tests ISO8601 leap seconds.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L124-L128">test__strptime__3</a>
     */
    @Test
    public void testIso8601LeapSeconds() {
        // Leap seconds are considered to be the next second with the default resolver.
        assertParsedTime("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(981244800L));
        assertParsedTime("2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(981212400L));
        assertParsedTime("-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(-125309754000L));
        assertParsedTime("+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(327406287600L));
        assertParsedTime("-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(-451734829200L));
    }

    /**
     * Tests ctime(3), asctime(3).
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L130-L132">test__strptime__3</a>
     */
    @Test
    public void testCtimeAsctime() {
        assertParsedTime("Thu Jul 29 14:47:19 1999", "%c", Instant.ofEpochSecond(933259639L));
        assertParsedTime("Thu Jul 29 14:47:19 -1999", "%c", Instant.ofEpochSecond(-125231389961L));
    }

    /**
     * Tests date(1).
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L134-L148">test__strptime__3</a>
     */
    @Test
    public void testDate() {
        assertParsedTime("Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933284381L));

        // The time zone IDs "MET", "AMT", "AST", and "DST" are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 AMT 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 AMT -1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(-125231383219L));
        assertParsedTime("Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(-125231383219L));

        // All "GMT", "GMT+..." and "GMT-..." are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y", Instant.ofEpochSecond(933266381L));

        // String-ish time zone IDs are not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y",
                         Instant.ofEpochSecond(933266381L));
        assertParsedTime("Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y",
                         Instant.ofEpochSecond(933266381L));
    }

    /**
     * Tests RFC822.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L150-L158">test__strptime__3</a>
     */
    @Test
    public void testRfc822() {
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933242061L));
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933242061L));

        // "PDT" (Pacific Daylight Time) is correctly recognized as -07:00 in Ruby parser, not like the legacy parser.
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933267261L));

        assertParsedTime("Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933242061L));
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933209661L));
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933225861L));
        assertParsedTime("Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(933258261L));
        assertParsedTime("Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z", Instant.ofEpochSecond(-125231391339L));
    }

    /**
     * Tests etc.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L160-L181">test__strptime__3</a>
     */
    @Test
    public void testEtc() {
        assertParsedTime("06-DEC-99", "%d-%b-%y", Instant.ofEpochSecond(944438400L));
        assertParsedTime("sUnDay oCtoBer 31 01", "%A %B %d %y", Instant.ofEpochSecond(1004486400L));
        // Their "\u000b" are actually "\v" in Ruby v2.3.1's tests. "\v" is not recognized as a character in Java.
        assertParsedTime("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y", Instant.ofEpochSecond(939945600L));
        assertParsedTime("October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y", Instant.ofEpochSecond(939945600L));

        assertParsedTime("09:02:11 AM", "%I:%M:%S %p", Instant.ofEpochSecond(32531L));
        assertParsedTime("09:02:11 A.M.", "%I:%M:%S %p", Instant.ofEpochSecond(32531L));
        assertParsedTime("09:02:11 PM", "%I:%M:%S %p", Instant.ofEpochSecond(75731L));
        assertParsedTime("09:02:11 P.M.", "%I:%M:%S %p", Instant.ofEpochSecond(75731L));

        assertParsedTime("12:33:44 AM", "%r", Instant.ofEpochSecond(2024L));
        assertParsedTime("01:33:44 AM", "%r", Instant.ofEpochSecond(5624L));
        assertParsedTime("11:33:44 AM", "%r", Instant.ofEpochSecond(41624L));
        assertParsedTime("12:33:44 PM", "%r", Instant.ofEpochSecond(45224L));
        assertParsedTime("01:33:44 PM", "%r", Instant.ofEpochSecond(48824L));
        assertParsedTime("11:33:44 PM", "%r", Instant.ofEpochSecond(84824L));

        assertParsedTime("11:33:44 PM AMT", "%I:%M:%S %p %Z", Instant.ofEpochSecond(84824L));
        assertParsedTime("11:33:44 P.M. AMT", "%I:%M:%S %p %Z", Instant.ofEpochSecond(84824L));
        // Their time zones are "AMT" actually in Ruby v2.3.1's tests, but "-04:00" is used here instead.
        // "AMT" is not recognized even by Ruby v2.3.1's zonetab.
        assertParsedTime("11:33:44 PM -04:00", "%I:%M:%S %p %Z", Instant.ofEpochSecond(99224L));
        assertParsedTime("11:33:44 P.M. -04:00", "%I:%M:%S %p %Z", Instant.ofEpochSecond(99224L));

        assertParsedTime("fri1feb034pm+5", "%a%d%b%y%H%p%Z", Instant.ofEpochSecond(1044115200L));
        // The time zone offset is just "+5" in Ruby v2.3.1's tests, but "+05" is used here instead.
        // "+5" is not recognized, and handled as "UTC", in Ruby parser.
        assertParsedTime("fri1feb034pm+05", "%a%d%b%y%H%p%Z", Instant.ofEpochSecond(1044097200L));
    }

    /**
     * Tests width.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L193-L239">test__strptime__width</a>
     */
    @Test
    public void testWidth() {
        // Default dates are always 1970-01-01 in Ruby parser. If only the year is specified, the date is 01-01.
        assertParsedTime("99", "%y", Instant.ofEpochSecond(915148800L));
        assertParsedTime("01", "%y", Instant.ofEpochSecond(978307200L));
        assertParsedTime("19 99", "%C %y", Instant.ofEpochSecond(915148800L));
        assertParsedTime("20 01", "%C %y", Instant.ofEpochSecond(978307200L));
        assertParsedTime("30 99", "%C %y", Instant.ofEpochSecond(35627817600L));
        assertParsedTime("30 01", "%C %y", Instant.ofEpochSecond(32535216000L));
        assertParsedTime("1999", "%C%y", Instant.ofEpochSecond(915148800L));
        assertParsedTime("2001", "%C%y", Instant.ofEpochSecond(978307200L));
        assertParsedTime("3099", "%C%y", Instant.ofEpochSecond(35627817600L));
        assertParsedTime("3001", "%C%y", Instant.ofEpochSecond(32535216000L));

        assertParsedTime("20060806", "%Y", Instant.ofEpochSecond(632995724851200L));
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("20060806", "%Y ", Instant.ofEpochSecond(632995724851200L));
        assertParsedTime("20060806", "%Y%m%d", Instant.ofEpochSecond(1154822400L));
        assertParsedTime("2006908906", "%Y9%m9%d", Instant.ofEpochSecond(1154822400L));
        assertParsedTime("12006 08 06", "%Y %m %d", Instant.ofEpochSecond(316724342400L));
        assertParsedTime("12006-08-06", "%Y-%m-%d", Instant.ofEpochSecond(316724342400L));
        assertParsedTime("200608 6", "%Y%m%e", Instant.ofEpochSecond(1154822400L));

        // Day of the year (yday; DAY_OF_YEAR) is not recognized, and handled as January 1, in Ruby parser.
        assertParsedTime("2006333", "%Y%j", Instant.ofEpochSecond(1136073600L));
        assertParsedTime("20069333", "%Y9%j", Instant.ofEpochSecond(1136073600L));
        assertParsedTime("12006 333", "%Y %j", Instant.ofEpochSecond(316705593600L));
        assertParsedTime("12006-333", "%Y-%j", Instant.ofEpochSecond(316705593600L));

        assertParsedTime("232425", "%H%M%S", Instant.ofEpochSecond(84265L));
        assertParsedTime("23924925", "%H9%M9%S", Instant.ofEpochSecond(84265L));
        assertParsedTime("23 24 25", "%H %M %S", Instant.ofEpochSecond(84265L));
        assertParsedTime("23:24:25", "%H:%M:%S", Instant.ofEpochSecond(84265L));
        assertParsedTime(" 32425", "%k%M%S", Instant.ofEpochSecond(12265L));
        assertParsedTime(" 32425", "%l%M%S", Instant.ofEpochSecond(12265L));

        // They are intentionally skipped as a month and a day of week are not sufficient to build a timestamp.
        // [['FriAug', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FriAug', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%A%B'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
        // [['FridayAugust', '%a%b'], [nil,8,nil,nil,nil,nil,nil,nil,5], __LINE__],
    }

    /**
     * Tests fail.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L241-L294">test__strptime__fail</a>
     */
    @Test
    public void testFail() {
        assertParsedTime("2001.", "%Y.", Instant.ofEpochSecond(978307200L));
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("2001. ", "%Y.", Instant.ofEpochSecond(978307200L));
        assertParsedTime("2001.", "%Y. ", Instant.ofEpochSecond(978307200L));
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertParsedTime("2001. ", "%Y. ", Instant.ofEpochSecond(978307200L));

        assertFailToParse("2001", "%Y.");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertFailToParse("2001 ", "%Y.");
        assertFailToParse("2001", "%Y. ");
        // Its " " is actually "\s" in Ruby v2.3.1's tests. "\s" is not recognized as a character in Java.
        assertFailToParse("2001 ", "%Y. ");

        assertFailToParse("2001-13-31", "%Y-%m-%d");
        assertFailToParse("2001-12-00", "%Y-%m-%d");
        assertFailToParse("2001-12-32", "%Y-%m-%d");
        assertFailToParse("2001-12-00", "%Y-%m-%e");
        assertFailToParse("2001-12-32", "%Y-%m-%e");
        assertFailToParse("2001-12-31", "%y-%m-%d");

        assertFailToParse("2004-000", "%Y-%j");
        assertFailToParse("2004-367", "%Y-%j");
        assertFailToParse("2004-366", "%y-%j");

        assertParsedTime("24:59:59", "%H:%M:%S", Instant.ofEpochSecond(89999L));
        assertParsedTime("24:59:59", "%k:%M:%S", Instant.ofEpochSecond(89999L));
        assertParsedTime("24:59:60", "%H:%M:%S", Instant.ofEpochSecond(90000L));
        assertParsedTime("24:59:60", "%k:%M:%S", Instant.ofEpochSecond(90000L));

        assertFailToParse("24:60:59", "%H:%M:%S");
        assertFailToParse("24:60:59", "%k:%M:%S");
        assertFailToParse("24:59:61", "%H:%M:%S");
        assertFailToParse("24:59:61", "%k:%M:%S");
        assertFailToParse("00:59:59", "%I:%M:%S");
        assertFailToParse("13:59:59", "%I:%M:%S");
        assertFailToParse("00:59:59", "%l:%M:%S");
        assertFailToParse("13:59:59", "%l:%M:%S");

        assertFailToParse("0", "%U");  // To success?
        assertFailToParse("54", "%U");
        assertFailToParse("0", "%W");  // To success?
        assertFailToParse("54", "%W");
        assertFailToParse("0", "%V");
        assertFailToParse("54", "%V");
        assertFailToParse("0", "%u");
        assertFailToParse("7", "%u");  // To success?
        assertFailToParse("0", "%w");  // To success?
        assertFailToParse("7", "%w");

        assertFailToParse("Sanday", "%A");
        assertFailToParse("Jenuary", "%B");
        assertFailToParse("Sundai", "%A");  // To success?
        assertParsedTime("Januari", "%B", Instant.ofEpochSecond(0L));
        assertFailToParse("Sundai,", "%A,");
        assertFailToParse("Januari,", "%B,");
    }

    /**
     * Tests ordinary cases.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L296-L314">test_strptime</a>
     */
    @Test
    public void testOrdinary() {
        assertParsedTime("2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(1016104953L));
        assertParsedTime("2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z", Instant.ofEpochSecond(1016072553L));
        assertParsedTime("2002-03-14T11:22:33-09:00", "%FT%T%Z", Instant.ofEpochSecond(1016137353L));
        assertParsedTime("2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z", Instant.ofEpochSecond(1016137353L, 123456789));
    }

    /**
     * Tests minus.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L367-L381">test_strptime__minus</a>
     */
    @Test
    public void testMinus() {
        assertParsedTime("-1", "%s", Instant.ofEpochSecond(-1L));
        assertParsedTime("-86400", "%s", Instant.ofEpochSecond(-86400L));

        // In |java.time.Instant|, it is always 0 <= nanoAdjustment < 1,000,000,000.
        // -0.9s is represented like -1s + 100ms.
        assertParsedTime("-999", "%Q", Instant.ofEpochSecond(-1L, 1_000_000));
        assertParsedTime("-1000", "%Q", Instant.ofEpochSecond(-1L));
    }

    /**
     * Tests comp.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L383-L451">test_strptime__comp</a>
     */
    @Test
    public void testComp() {
        assertFailToParse("073", "%j");
        assertParsedTime("13", "%d", Instant.ofEpochSecond(1036800L));

        assertParsedTime("Mar", "%b", Instant.ofEpochSecond(5097600L));
        assertParsedTime("2004", "%Y", Instant.ofEpochSecond(1072915200L));

        assertParsedTime("Mar 13", "%b %d", Instant.ofEpochSecond(6134400L));
        assertParsedTime("Mar 2004", "%b %Y", Instant.ofEpochSecond(1078099200L));
        assertParsedTime("23:55", "%H:%M", Instant.ofEpochSecond(86100L));
        assertParsedTime("23:55:30", "%H:%M:%S", Instant.ofEpochSecond(86130L));

        assertParsedTime("Sun 23:55", "%a %H:%M", Instant.ofEpochSecond(86100L));
        assertParsedTime("Aug 23:55", "%b %H:%M", Instant.ofEpochSecond(18402900L));

        assertFailToParse("2004", "%G");  // To success? %G is not considered in Time.strptime.
        assertFailToParse("11", "%V");  // To success? %V is not considered in Time.strptime.
        assertFailToParse("6", "%u");  // To success? %u is not considered in Time.strptime.

        assertFailToParse("11-6", "%V-%u");  // To success?
        assertFailToParse("2004-11", "%G-%V");  // To success?

        assertFailToParse("11-6", "%U-%w");  // To success?
        assertParsedTime("2004-11", "%Y-%U", Instant.ofEpochSecond(1072915200L));

        assertFailToParse("11-6", "%W-%w");  // To success?
        assertParsedTime("2004-11", "%Y-%W", Instant.ofEpochSecond(1072915200L));
    }

    /**
     * Tests exceptions.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/test/date/test_date_strptime.rb#L461-L486">test_strptime__ex</a>
     */
    @Test
    public void testEx() {
        assertFailToParse("2001-02-29", "%F");
        assertFailToParse("2001-02-29T23:59:60", "%FT%T");
        assertParsedTime("2001-03-01T23:59:60", "%FT%T", Instant.ofEpochSecond(983491200L));  // Okay to success.
        assertFailToParse("2001-03-01T23:59:61", "%FT%T");
        assertParsedTime("23:55", "%H:%M", Instant.ofEpochSecond(86100L));  // Okay to success.
        assertFailToParse("01-31-2011", "%m/%d/%Y");
    }

    @Test
    public void testMultipleEpochs() {
        final TemporalAccessor parsed1 = strptime("123456789 12849124", "%Q %s");
        assertEquals(12849124L, parsed1.getLong(ChronoField.INSTANT_SECONDS));
        assertFalse(parsed1.isSupported(RubyChronoFields.INSTANT_MILLIS));

        final TemporalAccessor parsed2 = strptime("123456789 12849124", "%s %Q");
        assertEquals(12849L, parsed2.getLong(ChronoField.INSTANT_SECONDS));
        assertEquals(12849124L, parsed2.getLong(RubyChronoFields.INSTANT_MILLIS));
    }

    @Test
    public void testExcessDate() {
        assertParsedTime("2016-02-29T00:00:00", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(1456704000L));
        assertFailToParse("2018-02-29T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertFailToParse("2018-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        assertFailToParse("2016-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S");
        assertFailToParse("2018-11-31T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertFailToParse("2018-10-32T00:00:00", "%Y-%m-%dT%H:%M:%S");
        assertFailToParse("2018-13-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertFailToParse("2018-00-01T00:00:00", "%Y-%m-%dT%H:%M:%S");
        assertFailToParse("2018-01-00T00:00:00", "%Y-%m-%dT%H:%M:%S");
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
        assertFailToParse("-1000000000-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S");
        assertParsedTime("999999999-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("1000000000-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S");

        assertParsedTime("9223372036854775", "%s", Instant.ofEpochSecond(9223372036854775L, 0));
        assertParsedTime("9223372036854776", "%s", Instant.ofEpochSecond(9223372036854776L, 0));
        assertParsedTime("31556889832780799", "%s", Instant.ofEpochSecond(31556889832780799L, 0));
        assertFailToParse("31556889832780800", "%s");  // To succeed? 999999999-12-31T23:59:59 + 1s
        assertFailToParse("31556889864403199", "%s");  // To succeed? Instant.MAX.
        assertFailToParse("31556889864403200", "%s");

        assertParsedTime("-9223372036854775", "%s", Instant.ofEpochSecond(-9223372036854775L, 0));
        assertParsedTime("-9223372036854776", "%s", Instant.ofEpochSecond(-9223372036854776L, 0));
        assertParsedTime("-31556889832780799", "%s", Instant.ofEpochSecond(-31556889832780799L, 0));
        assertParsedTime("-31556889832780800", "%s", Instant.ofEpochSecond(-31556889832780800L, 0));  // Sure

        assertParsedTime( "-31556889864403199", "%s", Instant.ofEpochSecond(-31556889864403199L, 0));
        assertFailToParse("-31556889864403200", "%s");  // To succeed? -(Instant.MAX + 1)
        assertFailToParse("-31557014135596799", "%s");  // To succeed? -999999999-01-01T00:00:00
        assertFailToParse("-31557014167219200", "%s");  // To succeed? Instant.MIN.
        assertFailToParse("-31557014167219201", "%s");

        assertParsedTime("9223372036854775807", "%Q", Instant.ofEpochSecond(9223372036854775L, 807000000));
        assertFailToParse("9223372036854775808", "%Q");
        assertParsedTime("-9223372036854775807", "%Q", Instant.ofEpochSecond(-9223372036854776L, 193000000));
        assertFailToParse("-9223372036854775808", "%Q");
    }

    @Test
    public void testSubseconds() {
        assertFailToParse("2007-08-01T00:00:00.", "%Y-%m-%dT%H:%M:%S.%N");
        assertFailToParse("2007-08-01T00:00:00.-777777777", "%Y-%m-%dT%H:%M:%S.%N");
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

    @Test
    public void testOffsets() {
        assertParsedTime("2019-05-03T00:00:00-00:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Just "-5" should not work.
        assertParsedTime("2019-05-03T00:00:00-5", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());

        assertParsedTime("2019-05-03T00:15:24-01:23:45", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 1, 39, 9, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-01:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 3, 8, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T23:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 4, 6, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-05-03T01:14:19+07:49:12", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2019, 5, 2, 17, 25, 7, 0, ZoneOffset.UTC).toInstant());

        // Go forward through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-02-28T23:00:00-05", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 3, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Go back through the leap year day (Feb 28 or Feb 29).
        assertParsedTime("2000-03-01T05:00:00+09", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2001-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2004-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2100-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());

        // Carry up to the year.
        assertParsedTime("2019-01-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2018, 12, 31, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertParsedTime("2019-12-31T21:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
                         OffsetDateTime.of(2020, 1, 1, 4, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    public void testDefaultOffsets() {
        final RubyDateTimeFormatter formatter = createOffsetFormatter("%Y-%m-%dT%H:%M:%S", ZoneOffset.ofHours(9));
        assertEquals(Instant.from(formatter.parse("2000-03-01T05:00:00")),
                     OffsetDateTime.of(2000, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(Instant.from(formatter.parse("2001-03-01T05:00:00")),
                     OffsetDateTime.of(2001, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(Instant.from(formatter.parse("2004-03-01T05:00:00")),
                     OffsetDateTime.of(2004, 2, 29, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
        assertEquals(Instant.from(formatter.parse("2100-03-01T05:00:00")),
                     OffsetDateTime.of(2100, 2, 28, 20, 0, 0, 0, ZoneOffset.UTC).toInstant());
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
        assertEquals(string, parsedResolved.query(RubyTemporalQueries.originalText()));
    }

    private static void assertFailToParse(final String string, final String format) {
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(format);
        try {
            formatter.parse(string);
        } catch (final DateTimeParseException ex) {
            return;
        } catch (final DateTimeException ex) {
            return;
        }
        fail();
    }

    private static RubyDateTimeFormatter createOffsetFormatter(final String format, final ZoneOffset offset) {
        return RubyDateTimeFormatter
                       .ofPattern(format)
                       .withResolver(RubyDateTimeResolver.withDefaultZoneOffset(offset));
    }
}

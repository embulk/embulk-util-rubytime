/*
 * Copyright 2019 The Embulk project
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestDateTimeParse {
    /**
     * Tests compared with CRuby's {@code Date._strptime}.
     *
     * @see <a href="https://docs.ruby-lang.org/en/2.6.0/Date.html#method-c-_strptime">Date._strptime</a>
     */
    @ParameterizedTest
    @MethodSource("provideStrptimeArgs")
    public void testDate_Strptime(final String date, final String format) throws Exception {
        assumeTrue(!STRPTIME_ARGS_EXPECTED_TO_FAIL.contains(date));
        assertDate_Strptime(date, format);
    }

    private static void assertDate_Strptime(final String date, final String format) throws Exception {
        final String reference = runCRubyDate_Strptime(date, format);
        final String actual = parseUnresolved(date, format);

        assertEquals(reference, actual);
        System.out.println(actual);
    }

    private static String runCRubyDate_Strptime(final String date, final String format) throws Exception {
        final String oneLiner = String.format(
                "require 'date'; " +
                "hashDateTime = Date._strptime(\"%s\", \"%s\"); " +
                "if hashDateTime.nil? then print \"could-not-parse\"; exit end; " +
                // For some cases, CRuby returns { offset: nil }.
                "if hashDateTime.include?(:offset) && hashDateTime[:offset].nil? then hashDateTime.delete(:offset) end; " +
                // For some cases, CRuby returns Rational { offset: -12600/1 }.
                "if hashDateTime.include?(:offset) && hashDateTime[:offset].is_a?(Rational) then hashDateTime[:offset] = hashDateTime[:offset].to_i end; " +
                "print \"{\"; " +
                "hashDateTime.sort.each { | key, value | print \"#{key}@#{value}#\" }; " +
                "print \"}\"",
                date.replace("\r", "\\r").replace("\n", "\\n"), format);
        final List<String> crubyResult;
        try {
            crubyResult = cruby.callOneLiner(oneLiner);
        } catch (final Exception ex) {
            throw ex;
        }
        assertEquals(1, crubyResult.size());
        return crubyResult.get(0);
    }

    private static String parseUnresolved(final String date, final String format) {
        final TemporalAccessor parsed;
        try {
            parsed = RubyDateTimeFormatter.ofPattern(format).parseUnresolved(date);
        } catch (final RubyDateTimeParseException ex) {
            return "could-not-parse";
        }
        final Map<String, Object> mapParsed = parsed.query(RubyDateTimeParsedElementsQuery.with(
                new FractionalSecondToRationalStringConverter(), new MillisecondToRationalStringConverter()));
        final StringBuilder actualBuilder = new StringBuilder();
        actualBuilder.append("{");
        mapParsed.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> actualBuilder.append(e.getKey()).append("@").append(e.getValue()).append("#"));
        actualBuilder.append("}");
        return actualBuilder.toString();
    }

    @BeforeAll
    public static void loadCRubyCaller() {
        cruby = new CRubyCaller();
    }

    private static Stream<Arguments> provideStrptimeArgs() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (int i = 0; i < STRPTIME_ARGS.length; i += 2) {
            builder.add(Arguments.of(STRPTIME_ARGS[i], STRPTIME_ARGS[i + 1]));
        }
        return builder.build();
    }

    private static final String[] STRPTIME_ARGS = {
        // Came from: https://github.com/ruby/ruby/blob/v2_6_5/test/date/test_date_strftime.rb
        "2001-02-03", "%Y-%m-%d",
        "2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S",
        "2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "-2001-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "+012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "-012345-02-03T23:59:60+09:00", "%Y-%m-%dT%H:%M:%S%Z",

        "Thu Jul 29 14:47:19 1999", "%c",
        "Thu Jul 29 14:47:19 -1999", "%c",

        "Thu Jul 29 16:39:41 EST 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 MET DST 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 AMT 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 AMT -1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 AST 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 AST -1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT+09 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT+0908 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT+090807 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT-09 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT-09:08 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT-09:08:07 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT-3.5 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 GMT-3,5 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 Mountain Daylight Time 1999", "%a %b %d %H:%M:%S %Z %Y",
        "Thu Jul 29 16:39:41 E. Australia Standard Time 1999", "%a %b %d %H:%M:%S %Z %Y",

        "Thu, 29 Jul 1999 09:54:21 UT", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 GMT", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 PDT", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 z", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 +0900", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 +0430", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul 1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z",
        "Thu, 29 Jul -1999 09:54:21 -0430", "%a, %d %b %Y %H:%M:%S %Z",

        "06-DEC-99", "%d-%b-%y",
        "sUnDay oCtoBer 31 01", "%A %B %d %y",
        "October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B %d, %y",
        "October\t\n\u000b\f\r 15,\t\n\u000b\f\r99", "%B%t%d,%n%y",

        "09:02:11 AM", "%I:%M:%S %p",
        "09:02:11 A.M.", "%I:%M:%S %p",
        "09:02:11 PM", "%I:%M:%S %p",
        "09:02:11 P.M.", "%I:%M:%S %p",

        "12:33:44 AM", "%r",
        "01:33:44 AM", "%r",
        "11:33:44 AM", "%r",
        "12:33:44 PM", "%r",
        "01:33:44 PM", "%r",
        "11:33:44 PM", "%r",

        "11:33:44 PM AMT", "%I:%M:%S %p %Z",
        "11:33:44 P.M. AMT", "%I:%M:%S %p %Z",

        "fri1feb034pm+5", "%a%d%b%y%H%p%Z",

        "99", "%y",
        "01", "%y",
        "19 99", "%C %y",
        "20 01", "%C %y",
        "30 99", "%C %y",
        "30 01", "%C %y",
        "1999", "%C%y",
        "2001", "%C%y",
        "3099", "%C%y",
        "3001", "%C%y",

        "20060806", "%Y",
        "20060806", "%Y ",
        "20060806", "%Y%m%d",
        "2006908906", "%Y9%m9%d",
        "12006 08 06", "%Y %m %d",
        "12006-08-06", "%Y-%m-%d",
        "200608 6", "%Y%m%e",

        "2006333", "%Y%j",
        "20069333", "%Y9%j",
        "12006 333", "%Y %j",
        "12006-333", "%Y-%j",

        "232425", "%H%M%S",
        "23924925", "%H9%M9%S",
        "23 24 25", "%H %M %S",
        "23:24:25", "%H:%M:%S",
        " 32425", "%k%M%S",
        " 32425", "%l%M%S",

        "FriAug", "%a%b",
        "FriAug", "%A%B",
        "FridayAugust", "%A%B",
        "FridayAugust", "%a%b",

        "2001.", "%Y.",
        "2001. ", "%Y.",
        "2001.", "%Y. ",
        "2001. ", "%Y. ",

        "2001", "%Y.",
        "2001 ", "%Y.",
        "2001", "%Y. ",
        "2001 ", "%Y. ",

        "2001-13-31", "%Y-%m-%d",
        "2001-12-00", "%Y-%m-%d",
        "2001-12-32", "%Y-%m-%d",
        "2001-12-00", "%Y-%m-%e",
        "2001-12-32", "%Y-%m-%e",
        "2001-12-31", "%y-%m-%d",

        "2004-000", "%Y-%j",
        "2004-367", "%Y-%j",
        "2004-366", "%y-%j",

        "24:59:59", "%H:%M:%S",
        "24:59:59", "%k:%M:%S",
        "24:59:60", "%H:%M:%S",
        "24:59:60", "%k:%M:%S",

        "24:60:59", "%H:%M:%S",
        "24:60:59", "%k:%M:%S",
        "24:59:61", "%H:%M:%S",
        "24:59:61", "%k:%M:%S",
        "00:59:59", "%I:%M:%S",
        "13:59:59", "%I:%M:%S",
        "00:59:59", "%l:%M:%S",
        "13:59:59", "%l:%M:%S",

        "0", "%U",
        "54", "%U",
        "0", "%W",
        "54", "%W",
        "0", "%V",
        "54", "%V",
        "0", "%u",
        "7", "%u",
        "0", "%w",
        "7", "%w",

        "Sanday", "%A",
        "Jenuary", "%B",
        "Sundai", "%A",
        "Januari", "%B",
        "Sundai,", "%A,",
        "Januari,", "%B,",

        "2002-03-14T11:22:33Z", "%Y-%m-%dT%H:%M:%S%Z",
        "2002-03-14T11:22:33+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2002-03-14T11:22:33-09:00", "%FT%T%Z",
        "2002-03-14T11:22:33-09:00", "%FT%X%Z",
        "2002-03-14T11:22:33.123456789-09:00", "%FT%T.%N%Z",

        "-1", "%s",
        "-86400", "%s",

        "-999", "%Q",
        "-1000", "%Q",

        "073", "%j",
        "13", "%d",

        "Mar", "%b",
        "2004", "%Y",

        "Mar 13", "%b %d",
        "Mar 2004", "%b %Y",
        "23:55", "%H:%M",
        "23:55:30", "%H:%M:%S",

        "Sun 23:55", "%a %H:%M",
        "Aug 23:55", "%b %H:%M",

        "2004", "%G",
        "11", "%V",
        "6", "%u",

        "11-6", "%V-%u",
        "2004-11", "%G-%V",

        "11-6", "%U-%w",
        "2004-11", "%Y-%U",

        "11-6", "%W-%w",
        "2004-11", "%Y-%W",

        "", "",
        "2001-02-29", "%F",
        "2001-02-29T23:59:60", "%FT%T",
        "2001-03-01T23:59:60", "%FT%T",
        "2001-03-01T23:59:61", "%FT%T",
        "23:55", "%H:%M",
        "01-31-2011", "%m/%d/%Y",

        "2001-02-03T04:05:06Z", "%FT%T%Z",

        "0 -0200", "%s %z",
        "9 +0200", "%s %z",

        "0 -0200", "%Q %z",
        "9000 +0200", "%Q %z",

        // Added originally in embulk-util-rubytime.
        "123456789 12849124", "%Q %s",
        "123456789 12849124", "%s %Q",
        "2016-02-29T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-02-29T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2016-02-31T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-11-31T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-10-32T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-13-01T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-00-01T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2018-01-00T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "1500000000.123456789", "%s.%N",
        "1500000000456.111111111", "%Q.%N",
        "1500000000.123", "%s.%L",
        "1500000000456.111", "%Q.%L",
        "1.5", "%s.%N",
        "-1.5", "%s.%N",
        "1.000000001", "%s.%N",
        "-1.000000001", "%s.%N",
        "2008-12-31T23:56:00", "%Y-%m-%dT%H:%M:%S",
        "2008-12-31T23:59:00", "%Y-%m-%dT%H:%M:%S",
        "2008-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S",
        "2008-12-31T23:59:60", "%Y-%m-%dT%H:%M:%S",
        "2009-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "2009-01-01T00:00:01", "%Y-%m-%dT%H:%M:%S",
        "2009-01-01T00:01:00", "%Y-%m-%dT%H:%M:%S",
        "2009-01-01T00:03:00", "%Y-%m-%dT%H:%M:%S",
        "-999999999-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "-1000000000-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S",
        "999999999-12-31T23:59:59", "%Y-%m-%dT%H:%M:%S",
        "1000000000-01-01T00:00:00", "%Y-%m-%dT%H:%M:%S",
        "9223372036854775", "%s",
        "9223372036854776", "%s",
        "31556889832780799", "%s",
        "31556889832780800", "%s",
        "31556889864403199", "%s",
        "31556889864403200", "%s",
        "-9223372036854775", "%s",
        "-9223372036854776", "%s",
        "-31556889832780799", "%s",
        "-31556889832780800", "%s",
        "-31556889864403199", "%s",
        "-31556889864403200", "%s",
        "-31557014135596799", "%s",
        "-31557014167219200", "%s",
        "-31557014167219201", "%s",
        "9223372036854775807", "%Q",
        "9223372036854775808", "%Q",
        "-9223372036854775807", "%Q",
        "-9223372036854775808", "%Q",
        "2007-08-01T00:00:00.", "%Y-%m-%dT%H:%M:%S.%N",
        "2007-08-01T00:00:00.-777777777", "%Y-%m-%dT%H:%M:%S.%N",
        "2007-08-01T00:00:00.777777777", "%Y-%m-%dT%H:%M:%S.%N",
        "2007-08-01T00:00:00.77777777777777", "%Y-%m-%dT%H:%M:%S.%N",
        "1500000000456.111111111", "%Q.%N",
        "2019-05-03T00:00:00-00:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T00:00:00-5", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T00:15:24-01:23:45", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T23:00:00-01:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T01:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T23:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-05-03T01:14:19+07:49:12", "%Y-%m-%dT%H:%M:%S%Z",
        "2000-02-28T23:00:00-05", "%Y-%m-%dT%H:%M:%S%Z",
        "2001-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2004-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2100-02-28T23:00:00-05:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2000-03-01T05:00:00+09", "%Y-%m-%dT%H:%M:%S%Z",
        "2001-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2004-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2100-03-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-01-01T05:00:00+09:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2019-12-31T21:00:00-07:00", "%Y-%m-%dT%H:%M:%S%Z",
        "2000-03-01T05:00:00", "%Y-%m-%dT%H:%M:%S",
        "2001-03-01T05:00:00", "%Y-%m-%dT%H:%M:%S",
        "2004-03-01T05:00:00", "%Y-%m-%dT%H:%M:%S",
        "2100-03-01T05:00:00", "%Y-%m-%dT%H:%M:%S",

        // Variations from https://github.com/ruby/ruby/blob/v2_6_5/test/date/test_date_strftime.rb
        "11:33:44 PM -04:00", "%I:%M:%S %p %Z",
        "11:33:44 P.M. -04:00", "%I:%M:%S %p %Z",
        "fri1feb034pm+05", "%a%d%b%y%H%p%Z",

        // Empty.
        "Foobar", "Foobar",

        // Various formats.
        "Thu Jul 29 14:47:19 1999", "%Ec",
        "1999", "%EC%y",
        "14:47:19", "%T",
        "14:47:20", "%X",
        "14:47:21", "%EX",
        "12/30/99", "%D",
        "12/30/98", "%x",
        "12/30/97", "%Ex",
        "2001-02-03", "%EY-%m-%d",
        "06-DEC-99", "%d-%b-%Ey",
        "06-DEC-99", "%d-%b-%Oy",
        "2001-02-03", "%Y-%m-%Od",
        "2001-12-00", "%Y-%m-%Oe",
        "Thu Jul 29 16:39:41 EST 1999", "%a %b %d %OH:%M:%S %Z %Y",
        "09:02:11 AM", "%OI:%M:%S %p",
        "20060806", "%Y%Om%d",
        "2001-02-03T23:59:59", "%Y-%m-%dT%H:%OM:%S",
        "2001-02-03T23:59:58", "%Y-%m-%dT%H:%M:%OS",
        "3", "%Ou",
        "2", "%OU",
        "15", "%OV",
        "4", "%Ow",
        "24", "%OW",
        "%OY", "%OY",
        "PST", "%z",
        "PST", "%:z",
        "PST", "%::z",
        "PST", "%:::z",
        "%::::z", "%::::z",

        // Invalid format options for parsing.
        "PST", "%::::z",
        "123456789", "%^s",
        "123456789", "%#s",
        "123456789", "%-s",
        "123456789", "%_s",
        "123456789", "%0s",
        "123456789", "%7s",
        "123456789", "%07s",
        "Thu Jul 29 14:47:19 1999", "%Oc",
        "1999", "%OC%y",
        "14:47:21", "%OX",
        "12/30/97", "%Ox",
        "2001-02-03", "%OY-%m-%d",
        "2001-02-03", "%Y-%m-%Ed",
        "2001-12-00", "%Y-%m-%Ee",
        "Thu Jul 29 16:39:41 EST 1999", "%a %b %d %EH:%M:%S %Z %Y",
        "09:02:11 AM", "%EI:%M:%S %p",
        "20060806", "%Y%Em%d",
        "2001-02-03T23:59:57", "%Y-%m-%dT%H:%EM:%S",
        "2001-02-03T23:59:56", "%Y-%m-%dT%H:%M:%ES",
        "4", "%Eu",
        "5", "%EU",
        "28", "%EV",
        "1", "%Ew",
        "33", "%EW",
    };

    private static final String[] STRPTIME_ARGS_EXPECTED_TO_FAIL_IN_ARRAY = {
        "-1000000000-12-31T23:59:59",  // Cannot be parsed by embulk-util-rubytime.
        "1000000000-01-01T00:00:00",  // Cannot be parsed by embulk-util-rubytime.
        "31556889864403200",  // Cannot be parsed by embulk-util-rubytime.
        "-31556889864403200",  // Cannot be parsed by embulk-util-rubytime.
        "-31557014135596799",  // Cannot be parsed by embulk-util-rubytime.
        "-31557014167219200",  // Cannot be parsed by embulk-util-rubytime.
        "-31557014167219201",  // Cannot be parsed by embulk-util-rubytime.
        "9223372036854775808",  // Cannot be parsed by embulk-util-rubytime.
        "-9223372036854775808",  // Cannot be parsed by embulk-util-rubytime.

        // Cannot be parsed by embulk-util-rubytime -- but we may want to allow this?
        "2007-08-01T00:00:00.-777777777",

        "2007-08-01T00:00:00.77777777777777",  // Cannot be parsed by embulk-util-rubytime.

        "%::::z",  // "%::::z% should fail even if the input string is also "%::::z", but embulk-util-rubytime now accepts.
    };

    private static final Set<String> STRPTIME_ARGS_EXPECTED_TO_FAIL =
            new HashSet<>(Arrays.asList(STRPTIME_ARGS_EXPECTED_TO_FAIL_IN_ARRAY));

    private static CRubyCaller cruby;
}

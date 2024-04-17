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

/*
 * This file includes a copy of zonetab.list from Matz’s Ruby Interpreter.
 *
 * https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/zonetab.list?view=markup
 *
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 */

package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestRubyDateTimeZones {
    @Test
    public void testNormalize() {
        assertNormalize("abc", "ABC");
        assertNormalize("abc ", "ABC");
        assertNormalize("abc  ", "ABC");
        assertNormalize(" abc", "ABC");
        assertNormalize("  abc", "ABC");
        assertNormalize(" abc ", "ABC");
        assertNormalize("  abc  ", "ABC");
        assertNormalize("a bc", "A BC");
        assertNormalize("a  bc", "A BC");
        assertNormalize("  a  bc d  ef  ", "A BC D EF");
    }

    @Test
    public void testParseOffsetTooLongFraction() {
        assertThrows(
                NumberFormatException.class,
                () -> {
                    RubyDateTimeZones.parseOffsetForTesting("UTC+19.001953125");
                });
        assertThrows(
                NumberFormatException.class,
                () -> {
                    RubyDateTimeZones.parseOffsetForTesting("UTC+19.0009765625");
                });
        assertThrows(
                NumberFormatException.class,
                () -> {
                    RubyDateTimeZones.parseOffsetForTesting("UTC+19.0000111111");
                });
    }

    @Test
    public void testParseUnsignedIntUntilNonDigit() {
        assertParseUnsignedInt("abcd19381fjs", 4, 19381, 9);
        assertParseUnsignedInt("19084:219", 0, 19084, 5);
    }

    /**
     * The data source is a copy from "zonetab.list" of Ruby 2.5.0.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/zonetab.list?view=markup">zonetab.list</a>
     */
    @ParameterizedTest
    @CsvSource({
            "ut,   0*3600",
            "gmt,  0*3600",
            "est, -5*3600",
            "edt, -4*3600",
            "cst, -6*3600",
            "cdt, -5*3600",
            "mst, -7*3600",
            "mdt, -6*3600",
            "pst, -8*3600",
            "pdt, -7*3600",
            "a,    1*3600",
            "b,    2*3600",
            "c,    3*3600",
            "d,    4*3600",
            "e,    5*3600",
            "f,    6*3600",
            "g,    7*3600",
            "h,    8*3600",
            "i,    9*3600",
            "k,   10*3600",
            "l,   11*3600",
            "m,   12*3600",
            "n,   -1*3600",
            "o,   -2*3600",
            "p,   -3*3600",
            "q,   -4*3600",
            "r,   -5*3600",
            "s,   -6*3600",
            "t,   -7*3600",
            "u,   -8*3600",
            "v,   -9*3600",
            "w,  -10*3600",
            "x,  -11*3600",
            "y,  -12*3600",
            "z,    0*3600",
            "utc,  0*3600",
            "wet,  0*3600",
            "at,  -2*3600",
            "brst,-2*3600",
            "ndt, -(2*3600+1800)",
            "art, -3*3600",
            "adt, -3*3600",
            "brt, -3*3600",
            "clst,-3*3600",
            "nst, -(3*3600+1800)",
            "ast, -4*3600",
            "clt, -4*3600",
            "akdt,-8*3600",
            "ydt, -8*3600",
            "akst,-9*3600",
            "hadt,-9*3600",
            "hdt, -9*3600",
            "yst, -9*3600",
            "ahst,-10*3600",
            "cat,-10*3600",
            "hast,-10*3600",
            "hst,-10*3600",
            "nt,  -11*3600",
            "idlw,-12*3600",
            "bst,  1*3600",
            "cet,  1*3600",
            "fwt,  1*3600",
            "met,  1*3600",
            "mewt, 1*3600",
            "mez,  1*3600",
            "swt,  1*3600",
            "wat,  1*3600",
            "west, 1*3600",
            "cest, 2*3600",
            "eet,  2*3600",
            "fst,  2*3600",
            "mest, 2*3600",
            "mesz, 2*3600",
            "sast, 2*3600",
            "sst,  2*3600",
            "bt,   3*3600",
            "eat,  3*3600",
            "eest, 3*3600",
            "msk,  3*3600",
            "msd,  4*3600",
            // "zp4,  4*3600",
            // "zp5,  5*3600",
            "ist,  (5*3600+1800)",
            // "zp6,  6*3600",
            "wast, 7*3600",
            "cct,  8*3600",
            "sgt,  8*3600",
            "wadt, 8*3600",
            "jst,  9*3600",
            "kst,  9*3600",
            "east,10*3600",
            "gst, 10*3600",
            "eadt,11*3600",
            "idle,12*3600",
            "nzst,12*3600",
            "nzt, 12*3600",
            "nzdt,13*3600",
            "afghanistan,             16200",
            "alaskan,                -32400",
            "arab,                    10800",
            "arabian,                 14400",
            "arabic,                  10800",
            "atlantic,               -14400",
            "aus central,             34200",
            "aus eastern,             36000",
            "azores,                  -3600",
            "canada central,         -21600",
            "cape verde,              -3600",
            "caucasus,                14400",
            "cen. australia,          34200",
            "central america,        -21600",
            "central asia,            21600",
            "central europe,           3600",
            "central european,         3600",
            "central pacific,         39600",
            "central,                -21600",
            "china,                   28800",
            "dateline,               -43200",
            "e. africa,               10800",
            "e. australia,            36000",
            "e. europe,                7200",
            "e. south america,       -10800",
            "eastern,                -18000",
            "egypt,                    7200",
            "ekaterinburg,            18000",
            "fiji,                    43200",
            "fle,                      7200",
            "greenland,              -10800",
            "greenwich,                   0",
            "gtb,                      7200",
            "hawaiian,               -36000",
            "india,                   19800",
            "iran,                    12600",
            "jerusalem,                7200",
            "korea,                   32400",
            "mexico,                 -21600",
            // "mid-atlantic,            -7200",
            "mountain,               -25200",
            "myanmar,                 23400",
            "n. central asia,         21600",
            "nepal,                   20700",
            "new zealand,             43200",
            "newfoundland,           -12600",
            "north asia east,         28800",
            "north asia,              25200",
            "pacific sa,             -14400",
            "pacific,                -28800",
            "romance,                  3600",
            "russian,                 10800",
            "sa eastern,             -10800",
            "sa pacific,             -18000",
            "sa western,             -14400",
            "samoa,                  -39600",
            "se asia,                 25200",
            "malay peninsula,         28800",
            "south africa,             7200",
            "sri lanka,               21600",
            "taipei,                  28800",
            "tasmania,                36000",
            "tokyo,                   32400",
            "tonga,                   46800",
            "us eastern,             -18000",
            "us mountain,            -25200",
            "vladivostok,             36000",
            "w. australia,            28800",
            "w. central africa,        3600",
            "w. europe,                3600",
            "west asia,               18000",
            "west pacific,            36000",
            "yakutsk,                 32400",
    })
    public void testMatchingZoneTab(final String name, final String expectedOffsetInString) {
        final Matcher matcherHourHalf = HOUR_HALF.matcher(expectedOffsetInString);
        final Matcher matcherHour = HOUR.matcher(expectedOffsetInString);
        final Matcher matcherSecond = SECOND.matcher(expectedOffsetInString);

        final int expectedOffset;
        if (matcherHourHalf.matches()) {
            final boolean isNegative = matcherHourHalf.group("negative").equals("-");
            final int hour = Integer.parseInt(matcherHourHalf.group("hour"));
            expectedOffset = (isNegative ? -1 : 1) * (hour * 3600 + 1800);
        } else if (matcherHour.matches()) {
            final boolean isNegative = matcherHour.group("negative").equals("-");
            final int hour = Integer.parseInt(matcherHour.group("hour"));
            expectedOffset = (isNegative ? -1 : 1) * (hour * 3600);
        } else if (matcherSecond.matches()) {
            final boolean isNegative = matcherSecond.group("negative").equals("-");
            final int second = Integer.parseInt(matcherSecond.group("second"));
            expectedOffset = (isNegative ? -1 : 1) * second;
        } else {
            fail("The expected data \"" + expectedOffsetInString + "\" is in an unexpected format.");
            return;
        }

        assertEquals(expectedOffset, RubyDateTimeZones.mapZoneNameToOffsetInSecondsForTesting(name.toUpperCase()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dummy",
            "@@@@@",
            "",
            "%d",
            "\n",
            "   ",
            "\t",
            "!",
            "?",
            "*",
    })
    public void testUnmatchingZoneTab(final String name) {
        assertEquals(Integer.MIN_VALUE, RubyDateTimeZones.mapZoneNameToOffsetInSecondsForTesting(name.toUpperCase()));
    }

    private static void assertParseUnsignedInt(
            final String string,
            final int oldIndex,
            final int value,
            final int newIndex) {
        final long result = RubyDateTimeZones.parseUnsignedIntUntilNonDigitForTesting(string, oldIndex);
        assertEquals(value, (int) (result & 0xffffffffL));
        assertEquals(newIndex, (int) (result >> 32));
    }

    private static void assertNormalize(final String name, final String expected) {
        assertEquals(expected, RubyDateTimeZones.normalizeForTesting(name));
    }

    private static final Pattern HOUR_HALF = Pattern.compile("(?<negative>-?)\\((?<hour>\\d+)\\*3600\\+1800\\)");
    private static final Pattern HOUR = Pattern.compile("(?<negative>-?)(?<hour>\\d+)\\*3600");
    private static final Pattern SECOND = Pattern.compile("(?<negative>-?)(?<second>\\d+)");
}

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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests org.embulk.util.rubytime.Format.
 */
public class TestFormat {
    @Test
    public void testEmpty() {
        testFormat("");
    }

    @Test
    public void testSingles() {
        testFormat("%Y", FormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%C", FormatDirective.CENTURY.toTokens());
        testFormat("%y", FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());

        testFormat("%m", FormatDirective.MONTH_OF_YEAR.toTokens());
        testFormat("%B", FormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens());
        testFormat("%b", FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens());
        testFormat("%h", FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H.toTokens());

        testFormat("%d", FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%e", FormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens());

        testFormat("%j", FormatDirective.DAY_OF_YEAR.toTokens());

        testFormat("%H", FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens());
        testFormat("%k", FormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens());
        testFormat("%I", FormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens());
        testFormat("%l", FormatDirective.HOUR_OF_AMPM_BLANK_PADDED.toTokens());
        testFormat("%P", FormatDirective.AMPM_OF_DAY_LOWER_CASE.toTokens());
        testFormat("%p", FormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());

        testFormat("%M", FormatDirective.MINUTE_OF_HOUR.toTokens());

        testFormat("%S", FormatDirective.SECOND_OF_MINUTE.toTokens());

        testFormat("%L", FormatDirective.MILLI_OF_SECOND.toTokens());
        testFormat("%N", FormatDirective.NANO_OF_SECOND.toTokens());

        testFormat("%z", FormatDirective.TIME_OFFSET.toTokens());
        testFormat("%Z", FormatDirective.TIME_ZONE_NAME.toTokens());

        testFormat("%A", FormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens());
        testFormat("%a", FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens());
        testFormat("%u", FormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1.toTokens());
        testFormat("%w", FormatDirective.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0.toTokens());

        testFormat("%G", FormatDirective.WEEK_BASED_YEAR_WITH_CENTURY.toTokens());
        testFormat("%g", FormatDirective.WEEK_BASED_YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%V", FormatDirective.WEEK_OF_WEEK_BASED_YEAR.toTokens());

        testFormat("%U", FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY.toTokens());
        testFormat("%W", FormatDirective.WEEK_OF_YEAR_STARTING_WITH_MONDAY.toTokens());

        testFormat("%s", FormatDirective.SECOND_SINCE_EPOCH.toTokens());
        testFormat("%Q", FormatDirective.MILLISECOND_SINCE_EPOCH.toTokens());
    }

    @Test
    public void testRecurred() {
        testFormat("%c",
                   FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%D",
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%x",
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%F",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%n",
                   new FormatToken.Immediate('\n'));
        testFormat("%R",
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens());
        testFormat("%r",
                   FormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());
        testFormat("%T",
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%X",
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%t",
                   new FormatToken.Immediate('\t'));
        testFormat("%v",
                   FormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.YEAR_WITH_CENTURY.toTokens());
        testFormat("%+",
                   FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.TIME_ZONE_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.YEAR_WITH_CENTURY.toTokens());
    }

    @Test
    public void testExtended() {
        testFormat("%EC", FormatDirective.CENTURY.toTokens());
        testFormat("%Oy", FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%:z", FormatDirective.TIME_OFFSET.toTokens());
        testFormat("%::z", FormatDirective.TIME_OFFSET.toTokens());
        testFormat("%:::z", FormatDirective.TIME_OFFSET.toTokens());
    }

    @Test
    public void testPercents() {
        testFormat("%", new FormatToken.Immediate('%'));
        testFormat("%%", new FormatToken.Immediate('%'));

        // Split into two "%" tokens for some internal reasons.
        testFormat("%%%", new FormatToken.Immediate('%'), new FormatToken.Immediate('%'));
        testFormat("%%%%", new FormatToken.Immediate('%'), new FormatToken.Immediate('%'));
    }

    @Test
    public void testOrdinary() {
        testFormat("abc123", new FormatToken.Immediate("abc123"));
    }

    @Test
    public void testPercentButOrdinary() {
        testFormat("%f", new FormatToken.Immediate("%f"));
        testFormat("%Ed", new FormatToken.Immediate("%Ed"));
        testFormat("%OY", new FormatToken.Immediate("%OY"));
        testFormat("%::::z", new FormatToken.Immediate("%::::z"));
    }

    @Test
    public void testSpecifiersAndOrdinary() {
        testFormat("ab%Out%Expose",
                   new FormatToken.Immediate("ab"),
                   FormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1.toTokens(),
                   new FormatToken.Immediate("t"),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('/'),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens(),
                   new FormatToken.Immediate("pose"));
    }

    @Test
    public void testRubyTestPatterns() {
        testFormat("%Y-%m-%dT%H:%M:%S",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('T'),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%d-%b-%y",
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%A %B %d %y",
                   FormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%B %d, %y",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(", "),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%B%t%d,%n%y",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new FormatToken.Immediate('\t'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(','),
                   new FormatToken.Immediate('\n'),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens());
        testFormat("%I:%M:%S %p",
                   FormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens());
        testFormat("%I:%M:%S %p %Z",
                   FormatDirective.HOUR_OF_AMPM_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens(),
                   new FormatToken.Immediate(' '),
                   FormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%a%d%b%y%H%p%Z",
                   FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME.toTokens(),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME.toTokens(),
                   FormatDirective.YEAR_WITHOUT_CENTURY.toTokens(),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE.toTokens(),
                   FormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%Y9%m9%d",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('9'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('9'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%k%M%S",
                   FormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens(),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%l%M%S",
                   FormatDirective.HOUR_OF_AMPM_BLANK_PADDED.toTokens(),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%Y.",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('.'));
        testFormat("%Y. ",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate(". "));
        testFormat("%Y-%m-%d",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens());
        testFormat("%Y-%m-%e",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_BLANK_PADDED.toTokens());
        testFormat("%Y-%j",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_YEAR.toTokens());
        testFormat("%H:%M:%S",
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%k:%M:%S",
                   FormatDirective.HOUR_OF_DAY_BLANK_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens());
        testFormat("%A,",
                   FormatDirective.DAY_OF_WEEK_FULL_NAME.toTokens(),
                   new FormatToken.Immediate(','));
        testFormat("%B,",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME.toTokens(),
                   new FormatToken.Immediate(','));
        testFormat("%FT%T%Z",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('T'),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   FormatDirective.TIME_ZONE_NAME.toTokens());
        testFormat("%FT%T.%N%Z",
                   FormatDirective.YEAR_WITH_CENTURY.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.MONTH_OF_YEAR.toTokens(),
                   new FormatToken.Immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate('T'),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR.toTokens(),
                   new FormatToken.Immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE.toTokens(),
                   new FormatToken.Immediate('.'),
                   FormatDirective.NANO_OF_SECOND.toTokens(),
                   FormatDirective.TIME_ZONE_NAME.toTokens());
    }

    private void testFormat(final String formatString, final Object... expectedTokensInArray) {
        final List<FormatToken> expectedTokens = new ArrayList<>();
        for (final Object expectedElement : expectedTokensInArray) {
            if (expectedElement instanceof FormatToken) {
                expectedTokens.add((FormatToken) expectedElement);
            } else if (expectedElement instanceof List) {
                for (final Object expectedElement2 : (List) expectedElement) {
                    if (expectedElement2 instanceof FormatToken) {
                        expectedTokens.add((FormatToken) expectedElement2);
                    } else {
                        fail("Not Token");
                    }
                }
            } else {
                fail("Neither Token nor List");
            }
        }
        final Format expectedFormat = Format.createForTesting(expectedTokens);
        final Format actualFormat = Format.compile(formatString);
        assertEquals(expectedFormat, actualFormat);
    }
}

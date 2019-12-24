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
        testFormat("%Y", FormatDirective.YEAR_WITH_CENTURY);
        testFormat("%C", FormatDirective.CENTURY);
        testFormat("%y", FormatDirective.YEAR_WITHOUT_CENTURY);

        testFormat("%m", FormatDirective.MONTH_OF_YEAR);
        testFormat("%B", FormatDirective.MONTH_OF_YEAR_FULL_NAME);
        testFormat("%b", FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME);
        testFormat("%h", FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H);

        testFormat("%d", FormatDirective.DAY_OF_MONTH_ZERO_PADDED);
        testFormat("%e", FormatDirective.DAY_OF_MONTH_BLANK_PADDED);

        testFormat("%j", FormatDirective.DAY_OF_YEAR);

        testFormat("%H", FormatDirective.HOUR_OF_DAY_ZERO_PADDED);
        testFormat("%k", FormatDirective.HOUR_OF_DAY_BLANK_PADDED);
        testFormat("%I", FormatDirective.HOUR_OF_AMPM_ZERO_PADDED);
        testFormat("%l", FormatDirective.HOUR_OF_AMPM_BLANK_PADDED);
        testFormat("%P", FormatDirective.AMPM_OF_DAY_LOWER_CASE);
        testFormat("%p", FormatDirective.AMPM_OF_DAY_UPPER_CASE);

        testFormat("%M", FormatDirective.MINUTE_OF_HOUR);

        testFormat("%S", FormatDirective.SECOND_OF_MINUTE);

        testFormat("%L", FormatDirective.MILLI_OF_SECOND);
        testFormat("%N", FormatDirective.NANO_OF_SECOND);

        testFormat("%z", FormatDirective.TIME_OFFSET);
        testFormat("%Z", FormatDirective.TIME_ZONE_NAME);

        testFormat("%A", FormatDirective.DAY_OF_WEEK_FULL_NAME);
        testFormat("%a", FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME);
        testFormat("%u", FormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1);
        testFormat("%w", FormatDirective.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0);

        testFormat("%G", FormatDirective.WEEK_BASED_YEAR_WITH_CENTURY);
        testFormat("%g", FormatDirective.WEEK_BASED_YEAR_WITHOUT_CENTURY);
        testFormat("%V", FormatDirective.WEEK_OF_WEEK_BASED_YEAR);

        testFormat("%U", FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY);
        testFormat("%W", FormatDirective.WEEK_OF_YEAR_STARTING_WITH_MONDAY);

        testFormat("%s", FormatDirective.SECONDS_SINCE_EPOCH);
        testFormat("%Q", FormatDirective.MILLISECONDS_SINCE_EPOCH);
    }

    @Test
    public void testRecurred() {
        testFormat("%c",
                   FormatDirective.RECURRED_LOWER_C);
        testFormat("%D",
                   FormatDirective.RECURRED_UPPER_D);
        testFormat("%x",
                   FormatDirective.RECURRED_LOWER_X);
        testFormat("%F",
                   FormatDirective.RECURRED_UPPER_F);
        testFormat("%n",
                   FormatDirective.IMMEDIATE_NEWLINE);
        testFormat("%R",
                   FormatDirective.RECURRED_UPPER_R);
        testFormat("%r",
                   FormatDirective.RECURRED_LOWER_R);
        testFormat("%T",
                   FormatDirective.RECURRED_UPPER_T);
        testFormat("%X",
                   FormatDirective.RECURRED_UPPER_X);
        testFormat("%t",
                   FormatDirective.IMMEDIATE_TAB);
        testFormat("%v",
                   FormatDirective.RECURRED_LOWER_V);
        testFormat("%+",
                   FormatDirective.RECURRED_PLUS);
    }

    @Test
    public void testExtended() {
        testFormat("%EC", FormatToken.directive("%EC", FormatDirective.CENTURY));
        testFormat("%Oy", FormatToken.directive("%Oy", FormatDirective.YEAR_WITHOUT_CENTURY));
        testFormat("%:z", FormatToken.directive(
                "%:z", FormatDirective.TIME_OFFSET, FormatDirectiveOptions.builder().setColons(1).build()));
        assertFalse(Format.compile("%:z").onlyForFormatter());
        testFormat("%::z", FormatToken.directive(
                "%::z", FormatDirective.TIME_OFFSET, FormatDirectiveOptions.builder().setColons(2).build()));
        assertFalse(Format.compile("%::z").onlyForFormatter());
        testFormat("%:::z", FormatToken.directive(
                "%:::z", FormatDirective.TIME_OFFSET, FormatDirectiveOptions.builder().setColons(3).build()));
        assertFalse(Format.compile("%:::z").onlyForFormatter());
        testFormat("%::::z", FormatToken.directive(
                "%::::z", FormatDirective.TIME_OFFSET, FormatDirectiveOptions.builder().setColons(4).build()));
        assertTrue(Format.compile("%::::z").onlyForFormatter());

        testFormat("%0d", FormatToken.directive(
                "%0d", FormatDirective.DAY_OF_MONTH_ZERO_PADDED, FormatDirectiveOptions.builder().setPadding('0').build()));
        assertTrue(Format.compile("%0d").onlyForFormatter());
        testFormat("%12S", FormatToken.directive(
                "%12S", FormatDirective.SECOND_OF_MINUTE, FormatDirectiveOptions.builder().setPrecision(12).build()));
        assertTrue(Format.compile("%12S").onlyForFormatter());
        testFormat("%09M", FormatToken.directive(
                "%09M", FormatDirective.MINUTE_OF_HOUR,
                FormatDirectiveOptions.builder().setPadding('0').setPrecision(9).build()));
        assertTrue(Format.compile("%09M").onlyForFormatter());
    }

    @Test
    public void testPercents() {
        testFormat("%", FormatToken.immediate('%'));
        testFormat("%%", FormatDirective.IMMEDIATE_PERCENT);

        // Split into two "%" tokens for some internal reasons.
        testFormat("%%%", FormatDirective.IMMEDIATE_PERCENT, FormatToken.immediate('%'));
        testFormat("%%%%", FormatDirective.IMMEDIATE_PERCENT, FormatDirective.IMMEDIATE_PERCENT);
    }

    @Test
    public void testOrdinary() {
        testFormat("abc123", FormatToken.immediate("abc123"));
    }

    @Test
    public void testPercentButOrdinary() {
        testFormat("%f", FormatToken.immediate("%f"));
        testFormat("%Ed", FormatToken.immediate("%Ed"));
        testFormat("%OY", FormatToken.immediate("%OY"));
    }

    @Test
    public void testSpecifiersAndOrdinary() {
        testFormat("ab%Out%Expose",
                   FormatToken.immediate("ab"),
                   FormatToken.directive("%Ou", FormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1),
                   FormatToken.immediate("t"),
                   FormatToken.directive("%Ex", FormatDirective.RECURRED_LOWER_X),
                   FormatToken.immediate("pose"));
    }

    @Test
    public void testRubyTestPatterns() {
        testFormat("%Y-%m-%dT%H:%M:%S",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('-'),
                   FormatDirective.MONTH_OF_YEAR,
                   FormatToken.immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatToken.immediate('T'),
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED,
                   FormatToken.immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatToken.immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE);
        testFormat("%d-%b-%y",
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatToken.immediate('-'),
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME,
                   FormatToken.immediate('-'),
                   FormatDirective.YEAR_WITHOUT_CENTURY);
        testFormat("%A %B %d %y",
                   FormatDirective.DAY_OF_WEEK_FULL_NAME,
                   FormatToken.immediate(' '),
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME,
                   FormatToken.immediate(' '),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatToken.immediate(' '),
                   FormatDirective.YEAR_WITHOUT_CENTURY);
        testFormat("%B %d, %y",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME,
                   FormatToken.immediate(' '),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatToken.immediate(", "),
                   FormatDirective.YEAR_WITHOUT_CENTURY);
        testFormat("%B%t%d,%n%y",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME,
                   FormatDirective.IMMEDIATE_TAB,
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatToken.immediate(','),
                   FormatDirective.IMMEDIATE_NEWLINE,
                   FormatDirective.YEAR_WITHOUT_CENTURY);
        testFormat("%I:%M:%S %p",
                   FormatDirective.HOUR_OF_AMPM_ZERO_PADDED,
                   FormatToken.immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatToken.immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE,
                   FormatToken.immediate(' '),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE);
        testFormat("%I:%M:%S %p %Z",
                   FormatDirective.HOUR_OF_AMPM_ZERO_PADDED,
                   FormatToken.immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatToken.immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE,
                   FormatToken.immediate(' '),
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE,
                   FormatToken.immediate(' '),
                   FormatDirective.TIME_ZONE_NAME);
        testFormat("%a%d%b%y%H%p%Z",
                   FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME,
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED,
                   FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME,
                   FormatDirective.YEAR_WITHOUT_CENTURY,
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED,
                   FormatDirective.AMPM_OF_DAY_UPPER_CASE,
                   FormatDirective.TIME_ZONE_NAME);
        testFormat("%Y9%m9%d",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('9'),
                   FormatDirective.MONTH_OF_YEAR,
                   FormatToken.immediate('9'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED);
        testFormat("%k%M%S",
                   FormatDirective.HOUR_OF_DAY_BLANK_PADDED,
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatDirective.SECOND_OF_MINUTE);
        testFormat("%l%M%S",
                   FormatDirective.HOUR_OF_AMPM_BLANK_PADDED,
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatDirective.SECOND_OF_MINUTE);
        testFormat("%Y.",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('.'));
        testFormat("%Y. ",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate(". "));
        testFormat("%Y-%m-%d",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('-'),
                   FormatDirective.MONTH_OF_YEAR,
                   FormatToken.immediate('-'),
                   FormatDirective.DAY_OF_MONTH_ZERO_PADDED);
        testFormat("%Y-%m-%e",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('-'),
                   FormatDirective.MONTH_OF_YEAR,
                   FormatToken.immediate('-'),
                   FormatDirective.DAY_OF_MONTH_BLANK_PADDED);
        testFormat("%Y-%j",
                   FormatDirective.YEAR_WITH_CENTURY,
                   FormatToken.immediate('-'),
                   FormatDirective.DAY_OF_YEAR);
        testFormat("%H:%M:%S",
                   FormatDirective.HOUR_OF_DAY_ZERO_PADDED,
                   FormatToken.immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatToken.immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE);
        testFormat("%k:%M:%S",
                   FormatDirective.HOUR_OF_DAY_BLANK_PADDED,
                   FormatToken.immediate(':'),
                   FormatDirective.MINUTE_OF_HOUR,
                   FormatToken.immediate(':'),
                   FormatDirective.SECOND_OF_MINUTE);
        testFormat("%A,",
                   FormatDirective.DAY_OF_WEEK_FULL_NAME,
                   FormatToken.immediate(','));
        testFormat("%B,",
                   FormatDirective.MONTH_OF_YEAR_FULL_NAME,
                   FormatToken.immediate(','));
        testFormat("%FT%T%Z",
                   FormatDirective.RECURRED_UPPER_F,
                   FormatToken.immediate('T'),
                   FormatDirective.RECURRED_UPPER_T,
                   FormatDirective.TIME_ZONE_NAME);
        testFormat("%FT%T.%N%Z",
                   FormatDirective.RECURRED_UPPER_F,
                   FormatToken.immediate('T'),
                   FormatDirective.RECURRED_UPPER_T,
                   FormatToken.immediate('.'),
                   FormatDirective.NANO_OF_SECOND,
                   FormatDirective.TIME_ZONE_NAME);
    }

    private void testFormat(final String formatString, final Object... expectedTokensInArray) {
        final List<FormatToken> expectedTokens = new ArrayList<>();
        for (final Object expectedElement : expectedTokensInArray) {
            if (expectedElement instanceof FormatToken) {
                expectedTokens.add((FormatToken) expectedElement);
            } else if (expectedElement instanceof FormatDirective) {
                expectedTokens.add(FormatToken.directive(
                        "%" + ((FormatDirective) expectedElement).getSpecifier(),
                        (FormatDirective) expectedElement,
                        FormatDirectiveOptions.EMPTY));
            } else {
                fail("Neither Token nor List");
            }
        }
        final Format expectedFormat = Format.createForTesting(expectedTokens);
        final Format actualFormat = Format.compile(formatString);
        assertEquals(expectedFormat, actualFormat);
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enumerates constants to represent Ruby-compatible date-time format directives.
 *
 * @see <a href="https://docs.ruby-lang.org/en/2.4.0/Time.html#method-i-strftime">Ruby v2.4.0's datetime format</a>
 */
enum FormatDirective {
    // Date (Year, Month, Day):

    YEAR_WITH_CENTURY(true, 'Y'),
    CENTURY(true, 'C'),
    YEAR_WITHOUT_CENTURY(true, 'y'),

    MONTH_OF_YEAR(true, 'm'),
    MONTH_OF_YEAR_FULL_NAME(false, 'B'),
    MONTH_OF_YEAR_ABBREVIATED_NAME(false, 'b'),
    MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H(false, 'h'),

    DAY_OF_MONTH_ZERO_PADDED(true, 'd'),
    DAY_OF_MONTH_BLANK_PADDED(true, 'e'),

    DAY_OF_YEAR(true, 'j'),

    // Time (Hour, Minute, Second, Subsecond):

    HOUR_OF_DAY_ZERO_PADDED(true, 'H'),
    HOUR_OF_DAY_BLANK_PADDED(true, 'k'),
    HOUR_OF_AMPM_ZERO_PADDED(true, 'I'),
    HOUR_OF_AMPM_BLANK_PADDED(true, 'l'),
    AMPM_OF_DAY_LOWER_CASE(false, 'P'),
    AMPM_OF_DAY_UPPER_CASE(false, 'p'),

    MINUTE_OF_HOUR(true, 'M'),

    SECOND_OF_MINUTE(true, 'S'),

    MILLI_OF_SECOND(true, 'L'),
    NANO_OF_SECOND(true, 'N'),

    // Time zone:

    TIME_OFFSET(false, 'z'),
    TIME_ZONE_NAME(false, 'Z'),

    // Weekday:

    DAY_OF_WEEK_FULL_NAME(false, 'A'),
    DAY_OF_WEEK_ABBREVIATED_NAME(false, 'a'),
    DAY_OF_WEEK_STARTING_WITH_MONDAY_1(true, 'u'),
    DAY_OF_WEEK_STARTING_WITH_SUNDAY_0(true, 'w'),

    // ISO 8601 week-based year and week number:
    // The first week of YYYY starts with a Monday and includes YYYY-01-04.
    // The days in the year before the first week are in the last week of
    // the previous year.

    WEEK_BASED_YEAR_WITH_CENTURY(true, 'G'),
    WEEK_BASED_YEAR_WITHOUT_CENTURY(true, 'g'),
    WEEK_OF_WEEK_BASED_YEAR(true, 'V'),

    // Week number:
    // The first week of YYYY that starts with a Sunday or Monday (according to %U
    // or %W). The days in the year before the first week are in week 0.

    WEEK_OF_YEAR_STARTING_WITH_SUNDAY(true, 'U'),
    WEEK_OF_YEAR_STARTING_WITH_MONDAY(true, 'W'),

    // Seconds since the Epoch:

    SECONDS_SINCE_EPOCH(true, 's'),
    MILLISECONDS_SINCE_EPOCH(false, 'Q'),  // TODO: Revisit this "%Q" is not a numeric pattern?

    // Recurred:

    RECURRED_UPPER_C('c', "a b e H:M:S Y"),
    RECURRED_UPPER_D('D', "m/d/y"),
    RECURRED_LOWER_X('x', "m/d/y"),
    RECURRED_UPPER_F('F', "Y-m-d"),
    RECURRED_LOWER_N('n', "\n"),
    RECURRED_UPPER_R('R', "H:M"),
    RECURRED_LOWER_R('r', "I:M:S p"),
    RECURRED_UPPER_T('T', "H:M:S"),
    RECURRED_UPPER_X('X', "H:M:S"),
    RECURRED_LOWER_T('t', "\t"),
    RECURRED_LOWER_V('v', "e-b-Y"),
    RECURRED_PLUS('+', "a b e H:M:S Z Y"),
    ;

    private FormatDirective(final boolean isNumeric, final char conversionSpecifier) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = isNumeric;
        this.isRecurred = false;
        this.recurred = null;
    }

    private FormatDirective(final char conversionSpecifier, final String recurred) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = false;
        this.isRecurred = true;
        this.recurred = recurred;
    }

    private FormatDirective() {
        this(false, '\0');
    }

    static boolean isSpecifier(final char conversionSpecifier) {
        return FROM_CONVERSION_SPECIFIER.containsKey(conversionSpecifier);
    }

    static FormatDirective of(final char conversionSpecifier) {
        return FROM_CONVERSION_SPECIFIER.get(conversionSpecifier);
    }

    @Override
    public String toString() {
        return "" + this.conversionSpecifier;
    }

    boolean isNumeric() {
        return this.isNumeric;
    }

    List<FormatToken> toTokens() {
        return TO_TOKENS.get(this);
    }

    static {
        final HashMap<Character, FormatDirective> charDirectiveMapBuilt = new HashMap<>();
        for (final FormatDirective directive : values()) {
            if (directive.conversionSpecifier != '\0') {
                charDirectiveMapBuilt.put(directive.conversionSpecifier, directive);
            }
        }
        FROM_CONVERSION_SPECIFIER = Collections.unmodifiableMap(charDirectiveMapBuilt);

        final EnumMap<FormatDirective, List<FormatToken>> directiveTokensMapBuilt =
                new EnumMap<>(FormatDirective.class);
        for (final FormatDirective directive : values()) {
            // Non-recurred directives first so that recurred directives can use tokens of non-recurred directives.
            if (!directive.isRecurred) {
                final ArrayList<FormatToken> tokensBuilt = new ArrayList<>();
                tokensBuilt.add(FormatToken.directive(directive));
                directiveTokensMapBuilt.put(directive, Collections.unmodifiableList(tokensBuilt));
            }
        }
        for (final FormatDirective directive : values()) {
            if (directive.isRecurred) {
                final ArrayList<FormatToken> tokensBuilt = new ArrayList<>();
                for (int i = 0; i < directive.recurred.length(); ++i) {
                    final FormatDirective eachDirective =
                            charDirectiveMapBuilt.get(directive.recurred.charAt(i));
                    if (eachDirective == null) {
                        tokensBuilt.add(FormatToken.immediate(directive.recurred.charAt(i)));
                    } else {
                        tokensBuilt.add(directiveTokensMapBuilt.get(eachDirective).get(0));
                    }
                }
                directiveTokensMapBuilt.put(directive, Collections.unmodifiableList(tokensBuilt));
            }
        }
        TO_TOKENS = Collections.unmodifiableMap(directiveTokensMapBuilt);
    }

    private static final Map<Character, FormatDirective> FROM_CONVERSION_SPECIFIER;
    private static final Map<FormatDirective, List<FormatToken>> TO_TOKENS;

    private final char conversionSpecifier;
    private final boolean isNumeric;
    private final boolean isRecurred;
    private final String recurred;
}

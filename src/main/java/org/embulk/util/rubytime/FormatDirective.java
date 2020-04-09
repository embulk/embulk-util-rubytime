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
 * Enumerates directives of Ruby-compatible date-time format.
 *
 * <ul>
 * <li>Directive: means the special tokens to be used to parse and format. For example, {@code "%Y"} and {@code "%^B"}.
 * <li>(Conversion) Specifier: means the last character of a directive. For example, {@code 's'} of {@code "%012s"}.
 * <li>Option: means modifiers of a directive, excluding its specifier. For example, {@code "-12"} of {@code "%-12s"}.
 * </ul>
 *
 * <p>Ruby's parser ({@code strptime}) and formatter ({@code strftime}) share almost the same format directives,
 * but they have some differences. The parser do not accept options except for {@code ":"} for {@code "%z"}.
 * The formattter accepts options, but it does not have {@code "%Q"}.
 *
 * @see <a href="https://docs.ruby-lang.org/en/2.6.0/Time.html#method-c-strptime">Time::strptime</a>
 * @see <a href="https://docs.ruby-lang.org/en/2.6.0/Time.html#method-i-strftime">Time#strftime</a>
 */
enum FormatDirective {
    // Date (Year, Month, Day):

    YEAR_WITH_CENTURY(true, 'Y'),  // '0', 4, defaultPrecision = 5 if <= 0
    CENTURY(true, 'C'),  /// '0', 2
    YEAR_WITHOUT_CENTURY(true, 'y'),  // '0', 2

    MONTH_OF_YEAR(true, 'm'),  // '0', 2
    MONTH_OF_YEAR_FULL_NAME(false, 'B'),
    MONTH_OF_YEAR_ABBREVIATED_NAME(false, 'b'),
    MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H(false, 'h'),

    DAY_OF_MONTH_ZERO_PADDED(true, 'd'),  // '0', 2
    DAY_OF_MONTH_BLANK_PADDED(true, 'e'),  // ' ', 2

    DAY_OF_YEAR(true, 'j'),  // '0', 3

    // Time (Hour, Minute, Second, Subsecond):

    HOUR_OF_DAY_ZERO_PADDED(true, 'H'),  // '0', 2
    HOUR_OF_DAY_BLANK_PADDED(true, 'k'),  // ' ', 2
    HOUR_OF_AMPM_ZERO_PADDED(true, 'I'),  // '0', 2
    HOUR_OF_AMPM_BLANK_PADDED(true, 'l'),  // ' ', 2
    AMPM_OF_DAY_LOWER_CASE(false, 'P'),
    AMPM_OF_DAY_UPPER_CASE(false, 'p'),

    MINUTE_OF_HOUR(true, 'M'),  // '0', 2

    SECOND_OF_MINUTE(true, 'S'),  // '0', 2

    MILLI_OF_SECOND(true, 'L'),
    NANO_OF_SECOND(true, 'N'),

    // Time zone:

    TIME_OFFSET(false, 'z'),
    TIME_ZONE_NAME(false, 'Z'),

    // Weekday:

    DAY_OF_WEEK_FULL_NAME(false, 'A'),
    DAY_OF_WEEK_ABBREVIATED_NAME(false, 'a'),
    DAY_OF_WEEK_STARTING_WITH_MONDAY_1(true, 'u'),  // '0', 1
    DAY_OF_WEEK_STARTING_WITH_SUNDAY_0(true, 'w'),  // '0', 1

    // ISO 8601 week-based year and week number:
    // The first week of YYYY starts with a Monday and includes YYYY-01-04.
    // The days in the year before the first week are in the last week of
    // the previous year.

    WEEK_BASED_YEAR_WITH_CENTURY(true, 'G'),  // '0', 4, defaultPrecision = 5 if <= 0
    WEEK_BASED_YEAR_WITHOUT_CENTURY(true, 'g'),  // '0', 2
    WEEK_OF_WEEK_BASED_YEAR(true, 'V'),  // '0', 2

    // Week number:
    // The first week of YYYY that starts with a Sunday or Monday (according to %U
    // or %W). The days in the year before the first week are in week 0.

    WEEK_OF_YEAR_STARTING_WITH_SUNDAY(true, 'U'),  // '0', 2
    WEEK_OF_YEAR_STARTING_WITH_MONDAY(true, 'W'),  // '0', 2

    // Seconds since the Epoch:

    SECONDS_SINCE_EPOCH(true, 's'),  // '0', 1
    MILLISECONDS_SINCE_EPOCH(false, 'Q'),

    // Immediates:

    IMMEDIATE_PERCENT(false, '%'),
    IMMEDIATE_NEWLINE(false, 'n'),
    IMMEDIATE_TAB(false, 't'),

    // Recurred:

    RECURRED_LOWER_C('c'),
    RECURRED_UPPER_D('D'),
    RECURRED_LOWER_X('x'),
    RECURRED_UPPER_F('F'),
    RECURRED_UPPER_R('R'),
    RECURRED_LOWER_R('r'),
    RECURRED_UPPER_T('T'),
    RECURRED_UPPER_X('X'),
    RECURRED_LOWER_V('v'),
    RECURRED_PLUS('+'),
    ;

    private FormatDirective(final boolean isNumeric, final char conversionSpecifier) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = isNumeric;
    }

    private FormatDirective(final char conversionSpecifier) {
        this.conversionSpecifier = conversionSpecifier;
        this.isNumeric = false;
    }

    static FormatDirective of(final char conversionSpecifier) {
        return FROM_CONVERSION_SPECIFIER.get(conversionSpecifier);
    }

    @Override
    public String toString() {
        return "" + this.conversionSpecifier;
    }

    char getSpecifier() {
        return this.conversionSpecifier;
    }

    boolean isNumeric() {
        return this.isNumeric;
    }

    static {
        final HashMap<Character, FormatDirective> charDirectiveMapBuilt = new HashMap<>();
        for (final FormatDirective directive : values()) {
            if (directive.conversionSpecifier != '\0') {
                charDirectiveMapBuilt.put(directive.conversionSpecifier, directive);
            }
        }
        FROM_CONVERSION_SPECIFIER = Collections.unmodifiableMap(charDirectiveMapBuilt);
    }

    private static final Map<Character, FormatDirective> FROM_CONVERSION_SPECIFIER;

    private final char conversionSpecifier;
    private final boolean isNumeric;
}

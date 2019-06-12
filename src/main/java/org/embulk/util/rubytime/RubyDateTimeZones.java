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

/**
 * This class contains a {@code static} method to interpret a timezone string
 * in the manner of Ruby's {@code Date} and {@code DateTime} classes.
 *
 * <p>Ruby's {@code Date} and {@code DateTime} classes accept a superset of
 * timezone strings which are accepted by Ruby's {@code Time} class. Ruby's
 * {@code Time.strptime} internally calls {@code Date._strptime} just to expand
 * a date-time string into elements, and then resolves the expanded elements by
 * {@code Time}'s own way. In other words, some timezone strings are recognized
 * once by {@code Date._strptime} at first, and then by {@code Time.strptime}
 * ignores them.
 *
 * <p>For example, {@code Date} recognizes {@code "CEST"} although {@code Time}
 * does not recognize it. On the other hand, both recognizes {@code "PST"}.
 *
 * <pre>{@code $ env TZ=UTC irb
 * irb(main):001:0> require 'date'
 * => true
 * irb(main):002:0> require 'time'
 * => true
 *
 * irb(main):003:0> Date._strptime("CEST", "%z")
 * => {:zone=>"CEST", :offset=>7200}
 *
 * irb(main):004:0> DateTime.strptime("2017-12-31 12:34:56 CEST", "%Y-%m-%d %H:%M:%S %z")
 * => #<DateTime: 2017-12-31T12:34:56+02:00 ((2458119j,38096s,0n),+7200s,2299161j)>
 *
 * irb(main):005:0> Time.strptime("2017-12-31 12:34:56 CEST", "%Y-%m-%d %H:%M:%S %z")
 * => 2017-12-31 12:34:56 +0000
 *
 * irb(main):006:0> Date._strptime("PST", "%z")
 * => {:zone=>"PST", :offset=>-28800}
 *
 * irb(main):007:0> DateTime.strptime("2017-12-31 12:34:56 PST", "%Y-%m-%d %H:%M:%S %z")
 * => #<DateTime: 2017-12-31T12:34:56-08:00 ((2458119j,74096s,0n),-28800s,2299161j)>
 *
 * irb(main):008:0> Time.strptime("2017-12-31 12:34:56 PST", "%Y-%m-%d %H:%M:%S %z")
 * => 2017-12-31 12:34:56 -0800}</pre>
 */
public final class RubyDateTimeZones {
    private RubyDateTimeZones() {
        // No instantiation.
    }

    /**
     * Converts a timezone string into a time offset integer in seconds.
     *
     * <p>Note that it has some limitations compared from Ruby's implementation.
     * <ul>
     * <li>Offset with a fraction part (e.g. {@code "UTC+10.5"}) is parsed into an
     * integer rounded down in seconds, not into a {@code Rational}.
     * <li>Offset with too long a fraction part (e.g. {@code "UTC+9.111111111111"})
     * is rejected. It throws {@link java.lang.NumberFormatException} in that case.
     * </ul>
     *
     * @param zoneName  a timezone string
     *
     * @return a time offset integer in seconds
     *
     * @throws java.lang.NumberFormatException  if failed to parse a numeric part
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_parse.c?view=markup#l353">date_zone_to_diff in Ruby</a>
     */
    public static int toOffsetInSeconds(final String zoneName) {
        if (zoneName == null) {
            return Integer.MIN_VALUE;
        }

        final String normalizedZone = normalize(zoneName);
        final int normalizedLength = normalizedZone.length();

        final String zoneMain;
        final boolean isDaylightSaving;
        if (normalizedZone.endsWith(SUFFIX_STANDARD_TIME)) {
            zoneMain = normalizedZone.substring(0, normalizedLength - LENGTH_STANDARD_TIME);
            isDaylightSaving = false;
        } else if (normalizedZone.endsWith(SUFFIX_DAYLIGHT_TIME)) {
            zoneMain = normalizedZone.substring(0, normalizedLength - LENGTH_DAYLIGHT_TIME);
            isDaylightSaving = true;
        } else if (normalizedZone.endsWith(SUFFIX_DST)) {
            zoneMain = normalizedZone.substring(0, normalizedLength - LENGTH_DST);
            isDaylightSaving = true;
        } else {
            zoneMain = normalizedZone;
            isDaylightSaving = false;
        }

        final int offset = mapZoneNameToOffsetInSeconds(zoneMain);
        if (offset != Integer.MIN_VALUE) {
            if (isDaylightSaving) {
                return offset + 3600;
            }
            return offset;
        }

        return parseOffset(zoneMain);
    }

    static String normalizeForTesting(final String zone) {
        return normalize(zone);
    }

    static int parseOffsetForTesting(final String zoneMain) {
        return parseOffset(zoneMain);
    }

    static long parseUnsignedIntUntilNonDigitForTesting(final String string, final int beginIndex) {
        return parseUnsignedIntUntilNonDigit(string, beginIndex);
    }

    static long parseUnsignedIntUntilNonDigitForTesting(final String string, final int beginIndex, final int endIndex) {
        return parseUnsignedIntUntilNonDigit(string, beginIndex, endIndex);
    }

    static int mapZoneNameToOffsetInSecondsForTesting(final String zone) {
        return mapZoneNameToOffsetInSeconds(zone);
    }

    /**
     * Normalizes a given time zone name String to be all upper-case and separated with one space.
     *
     * <p>It tries minimizing the number of instances for performance optimization. It is optimized for the cases
     * of all upper-case names separated at most one space, for example "JST", "PST DST", and "WEST ASIA".
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_parse.c?view=markup#l415">the latter part of date_zone_to_diff</a>
     */
    private static String normalize(final String original) {
        final int originalLength = original.length();
        StringBuilder normalized = null;

        boolean isPreviousSpace = false;
        boolean hasLowerCase = false;
        int beginning = -1;
        int lastNonWhitespace = -1;
        for (int i = 0; i < originalLength; ++i) {
            final char c = original.charAt(i);
            switch (c) {
            case ' ':
                if (beginning >= 0) {
                    if (isPreviousSpace) {
                        normalized = addToken(normalized, original, originalLength, beginning, lastNonWhitespace);
                        beginning = -1;
                    }
                }
                isPreviousSpace = true;
                break;
            case '\u0009':
            case '\n':
            case '\u000b':
            case '\u000c':
            case '\r':
                if (beginning >= 0) {
                    normalized = addToken(normalized, original, originalLength, beginning, lastNonWhitespace);
                    beginning = -1;
                }
                isPreviousSpace = false;
                break;
            default:
                if (beginning < 0) {
                    beginning = i;
                }
                if (Character.isLowerCase(c)) {
                    hasLowerCase = true;
                }
                lastNonWhitespace = i;
                isPreviousSpace = false;
            }
        }

        if (normalized == null) {
            if (beginning == 0 && lastNonWhitespace == originalLength - 1) {
                return toUpperCaseIfHasLowerCase(original, hasLowerCase);
            }
            return toUpperCaseIfHasLowerCase(original.substring(beginning, lastNonWhitespace + 1), hasLowerCase);
        }

        if (beginning >= 0) {
            normalized = addToken(normalized, original, originalLength, beginning, lastNonWhitespace);
        }
        return toUpperCaseIfHasLowerCase(normalized.toString(), hasLowerCase);
    }

    /**
     * Parses a time zone "offset" string into a time offset in seconds.
     *
     * <p>Time zone "offset" strings mean such as "+9", "UTC", "GMT", "UTC+9", "UTC+9:30", "UTC-102030", "UTC+10.234".
     *
     * <p>It does not expect valid non-offset time zone names other than "UTC" and "GMT", such as "PDT" and "JST".
     *
     * <p>It does not expect strings which will have "leftover" by {@code Date._strptime}.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_parse.c?view=markup#l415">the latter part of date_zone_to_diff</a>
     */
    private static int parseOffset(final String zoneMain) {
        final int length = zoneMain.length();

        int index;
        if (zoneMain.startsWith("GMT") || zoneMain.startsWith("UTC")) {
            index = 3;
        } else {
            index = 0;
        }

        if (index >= length) {
            return 0;
        }
        final char signCharacter = zoneMain.charAt(index);
        final boolean isNegative;
        if (signCharacter == '+') {
            isNegative = false;
        } else if (signCharacter == '-') {
            isNegative = true;
        } else {
            // An offset must be with a sign, such as "UTC-7", "GMT+06:30". "UTC9" is not allowed.
            return Integer.MIN_VALUE;
        }
        ++index;

        final int indexOfHour = index;
        final long hourResult = parseUnsignedIntUntilNonDigit(zoneMain, index);
        final int hour = (int) (hourResult & 0xffffffffL);
        index = (int) (hourResult >> 32);
        final char charNextToHour = (index < length ? zoneMain.charAt(index) : '\0');

        final int absoluteOffsetInSeconds;
        if (charNextToHour == ':') {
            ++index;
            final long minuteResult = parseUnsignedIntUntilNonDigit(zoneMain, index);
            final int minute = (int) (minuteResult & 0xffffffffL);
            index = (int) (minuteResult >> 32);
            final char charNextToMinute = (index < length ? zoneMain.charAt(index) : '\0');

            final int second;
            if (charNextToMinute == ':') {
                ++index;
                final long secondResult = parseUnsignedIntUntilNonDigit(zoneMain, index);
                second = (int) (secondResult & 0xffffffffL);
                index = (int) (secondResult >> 32);
            } else {
                second = 0;
            }

            absoluteOffsetInSeconds = second + minute * 60 + hour * 3600;
        } else if (charNextToHour == ',' || charNextToHour == '.') {
            ++index;
            final int beginIndex = index;
            // It throws NumberFormatException if the fraction part is too long. We accept that error case.
            final long fractionResult = parseUnsignedIntUntilNonDigit(zoneMain, index);

            final int fraction = (int) (fractionResult & 0xffffffffL);
            index = (int) (fractionResult >> 32);
            final int fractionDigits = index - beginIndex;

            // A fraction like .0000000000000000001 passes parseUnsignedIntUntilNonDigit above,
            // but too small digits are not effective because it eventually ignores subseconds.
            if (fractionDigits >= POW_10.length || fraction >= MAX_FRACTION) {
                throw new NumberFormatException("Too long a fraction part in: \"" + zoneMain + "\"");
            }

            final int fractionInSeconds = (fraction * 3600) / POW_10[fractionDigits];

            absoluteOffsetInSeconds = fractionInSeconds + hour * 3600;
        } else {
            final int digits = length - indexOfHour;
            if (digits > 2) {
                final int indexOfMinute = indexOfHour + 2 - digits % 2;
                final int indexOfSecond = indexOfHour + 4 - digits % 2;
                final int endIndex = indexOfHour + 6 - digits % 2;

                final long tempHourResult = parseUnsignedIntUntilNonDigit(zoneMain, indexOfHour, indexOfMinute);
                int offset = (int) (tempHourResult & 0xffffffffL) * 3600;
                final long tempMinuteResult = parseUnsignedIntUntilNonDigit(zoneMain, indexOfMinute, indexOfSecond);
                offset += (int) (tempMinuteResult & 0xffffffffL) * 60;
                if (digits >= 5) {
                    final long secondResult = parseUnsignedIntUntilNonDigit(zoneMain, indexOfSecond, endIndex);
                    offset += (int) (secondResult & 0xffffffffL);
                }

                absoluteOffsetInSeconds = offset;
            } else {
                absoluteOffsetInSeconds = hour * 3600;
            }
        }

        return (isNegative ? -absoluteOffsetInSeconds : absoluteOffsetInSeconds);
    }

    private static StringBuilder addToken(
            StringBuilder normalizedZoneBuilder,
            final String original,
            final int length,
            final int beginning,
            final int lastNonWhitespace) {
        if (normalizedZoneBuilder == null) {
            normalizedZoneBuilder = new StringBuilder(length);
        } else {
            normalizedZoneBuilder.append(' ');
        }
        normalizedZoneBuilder.append(original, beginning, lastNonWhitespace + 1);
        return normalizedZoneBuilder;
    }

    private static String toUpperCaseIfHasLowerCase(final String string, final boolean hasLowerCase) {
        if (hasLowerCase) {
            return string.toUpperCase();
        }
        return string;
    }

    private static long parseUnsignedIntUntilNonDigit(final String string, final int beginIndex) {
        return parseUnsignedIntUntilNonDigit(string, beginIndex, string.length());
    }

    private static long parseUnsignedIntUntilNonDigit(final String string, final int beginIndex, final int endIndex) {
        final int overflowThreshold = Integer.MAX_VALUE / 10;

        int index = beginIndex;

        int value = 0;
        for (; index < endIndex; ++index) {
            final int digit = Character.digit(string.charAt(index), 10);
            if (digit < 0 || digit >= 10) {
                break;
            }
            if (value > overflowThreshold) {
                throw new NumberFormatException("Overflown in parsing \"" + string.substring(beginIndex) + "\".");
            }
            value *= 10;
            final int temporaryValue = value;
            value += digit;
            if (value < temporaryValue) {
                throw new NumberFormatException("Overflown in parsing \"" + string.substring(beginIndex) + "\".");
            }
        }

        return (((long) index) << 32) | ((long) value);
    }

    /**
     * Maps an upper-cased zone name to its corresponding time offset in the manner of Ruby's Date / DateTime.
     *
     * <p>Upper-cased names are expected here so that it can accept typical zonetab names straightforward,
     * which are often upper-cased, for example, "GMT", "PST", and "JST".
     *
     * <p>Names with non-alphabetic characters are not recognized by Ruby's {@code Date._strptime} because of
     * the regular expression.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_strptime.c?view=markup#l571">pat_source</a>
     *
     * <p>Names with space characters must be suffixed by "standard time" or "daylight time" to be parsed
     * by {@code Date._strptime} due to the same regular expression.
     *
     * <p>Its switch-case implementation is efficient enough. It is compiled into efficient bytecode that is
     * usually based on {@code hashCode}. {@code HashMap} is not chosen because it needs boxing for integers.
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/strings-switch.html">Strings in switch Statements</a>
     * @see <a href="https://stackoverflow.com/questions/22110707/how-is-string-in-switch-statement-more-efficient-than-corresponding-if-else-stat">How is String in switch statement more efficient than corresponding if-else statement?</a>
     *
     * <p>The mapping is generated from "zonetab.list" of Ruby 2.5.0.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/zonetab.list?view=markup">zonetab.list</a>
     */
    private static int mapZoneNameToOffsetInSeconds(final String zone) {
        switch (zone) {
        case "UT":                return 0 * 3600;
        case "GMT":               return 0 * 3600;
        case "EST":               return -5 * 3600;
        case "EDT":               return -4 * 3600;
        case "CST":               return -6 * 3600;
        case "CDT":               return -5 * 3600;
        case "MST":               return -7 * 3600;
        case "MDT":               return -6 * 3600;
        case "PST":               return -8 * 3600;
        case "PDT":               return -7 * 3600;
        case "A":                 return 1 * 3600;
        case "B":                 return 2 * 3600;
        case "C":                 return 3 * 3600;
        case "D":                 return 4 * 3600;
        case "E":                 return 5 * 3600;
        case "F":                 return 6 * 3600;
        case "G":                 return 7 * 3600;
        case "H":                 return 8 * 3600;
        case "I":                 return 9 * 3600;
        case "K":                 return 10 * 3600;
        case "L":                 return 11 * 3600;
        case "M":                 return 12 * 3600;
        case "N":                 return -1 * 3600;
        case "O":                 return -2 * 3600;
        case "P":                 return -3 * 3600;
        case "Q":                 return -4 * 3600;
        case "R":                 return -5 * 3600;
        case "S":                 return -6 * 3600;
        case "T":                 return -7 * 3600;
        case "U":                 return -8 * 3600;
        case "V":                 return -9 * 3600;
        case "W":                 return -10 * 3600;
        case "X":                 return -11 * 3600;
        case "Y":                 return -12 * 3600;
        case "Z":                 return 0 * 3600;
        case "UTC":               return 0 * 3600;
        case "WET":               return 0 * 3600;
        case "AT":                return -2 * 3600;
        case "BRST":              return -2 * 3600;
        case "NDT":               return -(2 * 3600 + 1800);
        case "ART":               return -3 * 3600;
        case "ADT":               return -3 * 3600;
        case "BRT":               return -3 * 3600;
        case "CLST":              return -3 * 3600;
        case "NST":               return -(3 * 3600 + 1800);
        case "AST":               return -4 * 3600;
        case "CLT":               return -4 * 3600;
        case "AKDT":              return -8 * 3600;
        case "YDT":               return -8 * 3600;
        case "AKST":              return -9 * 3600;
        case "HADT":              return -9 * 3600;
        case "HDT":               return -9 * 3600;
        case "YST":               return -9 * 3600;
        case "AHST":              return -10 * 3600;
        case "CAT":               return -10 * 3600;
        case "HAST":              return -10 * 3600;
        case "HST":               return -10 * 3600;
        case "NT":                return -11 * 3600;
        case "IDLW":              return -12 * 3600;
        case "BST":               return 1 * 3600;
        case "CET":               return 1 * 3600;
        case "FWT":               return 1 * 3600;
        case "MET":               return 1 * 3600;
        case "MEWT":              return 1 * 3600;
        case "MEZ":               return 1 * 3600;
        case "SWT":               return 1 * 3600;
        case "WAT":               return 1 * 3600;
        case "WEST":              return 1 * 3600;
        case "CEST":              return 2 * 3600;
        case "EET":               return 2 * 3600;
        case "FST":               return 2 * 3600;
        case "MEST":              return 2 * 3600;
        case "MESZ":              return 2 * 3600;
        case "SAST":              return 2 * 3600;
        case "SST":               return 2 * 3600;
        case "BT":                return 3 * 3600;
        case "EAT":               return 3 * 3600;
        case "EEST":              return 3 * 3600;
        case "MSK":               return 3 * 3600;
        case "MSD":               return 4 * 3600;
        case "ZP4":               return 4 * 3600;
        case "ZP5":               return 5 * 3600;
        case "IST":               return (5 * 3600 + 1800);
        case "ZP6":               return 6 * 3600;
        case "WAST":              return 7 * 3600;
        case "CCT":               return 8 * 3600;
        case "SGT":               return 8 * 3600;
        case "WADT":              return 8 * 3600;
        case "JST":               return 9 * 3600;
        case "KST":               return 9 * 3600;
        case "EAST":              return 10 * 3600;
        case "GST":               return 10 * 3600;
        case "EADT":              return 11 * 3600;
        case "IDLE":              return 12 * 3600;
        case "NZST":              return 12 * 3600;
        case "NZT":               return 12 * 3600;
        case "NZDT":              return 13 * 3600;
        case "AFGHANISTAN":       return 16200;
        case "ALASKAN":           return -32400;
        case "ARAB":              return 10800;
        case "ARABIAN":           return 14400;
        case "ARABIC":            return 10800;
        case "ATLANTIC":          return -14400;
        case "AUS CENTRAL":       return 34200;
        case "AUS EASTERN":       return 36000;
        case "AZORES":            return -3600;
        case "CANADA CENTRAL":    return -21600;
        case "CAPE VERDE":        return -3600;
        case "CAUCASUS":          return 14400;
        case "CEN. AUSTRALIA":    return 34200;
        case "CENTRAL AMERICA":   return -21600;
        case "CENTRAL ASIA":      return 21600;
        case "CENTRAL EUROPE":    return 3600;
        case "CENTRAL EUROPEAN":  return 3600;
        case "CENTRAL PACIFIC":   return 39600;
        case "CENTRAL":           return -21600;
        case "CHINA":             return 28800;
        case "DATELINE":          return -43200;
        case "E. AFRICA":         return 10800;
        case "E. AUSTRALIA":      return 36000;
        case "E. EUROPE":         return 7200;
        case "E. SOUTH AMERICA":  return -10800;
        case "EASTERN":           return -18000;
        case "EGYPT":             return 7200;
        case "EKATERINBURG":      return 18000;
        case "FIJI":              return 43200;
        case "FLE":               return 7200;
        case "GREENLAND":         return -10800;
        case "GREENWICH":         return 0;
        case "GTB":               return 7200;
        case "HAWAIIAN":          return -36000;
        case "INDIA":             return 19800;
        case "IRAN":              return 12600;
        case "JERUSALEM":         return 7200;
        case "KOREA":             return 32400;
        case "MEXICO":            return -21600;
        case "MID-ATLANTIC":      return -7200;
        case "MOUNTAIN":          return -25200;
        case "MYANMAR":           return 23400;
        case "N. CENTRAL ASIA":   return 21600;
        case "NEPAL":             return 20700;
        case "NEW ZEALAND":       return 43200;
        case "NEWFOUNDLAND":      return -12600;
        case "NORTH ASIA EAST":   return 28800;
        case "NORTH ASIA":        return 25200;
        case "PACIFIC SA":        return -14400;
        case "PACIFIC":           return -28800;
        case "ROMANCE":           return 3600;
        case "RUSSIAN":           return 10800;
        case "SA EASTERN":        return -10800;
        case "SA PACIFIC":        return -18000;
        case "SA WESTERN":        return -14400;
        case "SAMOA":             return -39600;
        case "SE ASIA":           return 25200;
        case "MALAY PENINSULA":   return 28800;
        case "SOUTH AFRICA":      return 7200;
        case "SRI LANKA":         return 21600;
        case "TAIPEI":            return 28800;
        case "TASMANIA":          return 36000;
        case "TOKYO":             return 32400;
        case "TONGA":             return 46800;
        case "US EASTERN":        return -18000;
        case "US MOUNTAIN":       return -25200;
        case "VLADIVOSTOK":       return 36000;
        case "W. AUSTRALIA":      return 28800;
        case "W. CENTRAL AFRICA": return 3600;
        case "W. EUROPE":         return 3600;
        case "WEST ASIA":         return 18000;
        case "WEST PACIFIC":      return 36000;
        case "YAKUTSK":           return 32400;
        }
        return Integer.MIN_VALUE;
    }

    private static final String SUFFIX_STANDARD_TIME = " STANDARD TIME";
    private static final int LENGTH_STANDARD_TIME = SUFFIX_STANDARD_TIME.length();
    private static final String SUFFIX_DAYLIGHT_TIME = " DAYLIGHT TIME";
    private static final int LENGTH_DAYLIGHT_TIME = SUFFIX_DAYLIGHT_TIME.length();
    private static final String SUFFIX_DST = " DST";
    private static final int LENGTH_DST = SUFFIX_DST.length();
    private static final int MAX_FRACTION = Integer.MAX_VALUE / 3600;

    private static final int[] POW_10 = {
        1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };
}

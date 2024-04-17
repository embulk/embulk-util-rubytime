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

import java.time.ZoneOffset;
import java.util.Locale;

/**
 * This class contains a {@code static} method to interpret a timezone string
 * in the manner of Ruby's {@code Time} class.
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
@SuppressWarnings("checkstyle:Indentation")
public final class RubyTimeZones {
    private RubyTimeZones() {
        // No instantiation.
    }

    /**
     * Converts a timezone string into {@link java.time.ZoneOffset} in the manner
     * of Ruby's {@code Time} class.
     *
     * <p>Note that it has a difference from Ruby {@code Time.strptime}, where it
     * does not consider the local timezone. It returns {@code defaultZoneOffset}
     * if the given timezone string is invalid -- neither numerical nor predefined
     * textual timezone names. In contrast, Ruby's {@code Time.strptime} considers
     * the local timezone in that case.
     *
     * @param zoneName  a timezone string
     * @param defaultZoneOffset  a {@link java.time.ZoneOffset} for default
     *
     * @return a {@link java.time.ZoneOffset} converted
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l134">zone_offset in Ruby</a>
     */
    public static ZoneOffset toZoneOffset(final String zoneName, final ZoneOffset defaultZoneOffset) {
        if (zoneName == null || zoneName.isEmpty()) {
            return defaultZoneOffset;
        }

        if (zoneName.charAt(0) == '+' || zoneName.charAt(0) == '-') {
            if (matchesOffsetRepresentation(zoneName)) {
                return ZoneOffset.of(zoneName);  // Delegates parsing to java.time.ZoneOffset.
            }
            return defaultZoneOffset;
        }

        // NOTE: String#toUpperCase does not create a new instance when unnecessary.
        final ZoneOffset zoneOffset = mapZoneNametoZoneOffset(zoneName.toUpperCase(Locale.ENGLISH));
        if (zoneOffset != null) {
            return zoneOffset;
        }
        return defaultZoneOffset;
    }

    private static boolean matchesOffsetRepresentation(final String zone) {
        switch (zone.length()) {
        case 3:  // +HH
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2));
        case 5:  // +HHMM
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                       && Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4));
        case 6:  // +HH:MM
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                       && zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5));
        case 7:  // +HHMMSS
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                       && Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4))
                       && Character.isDigit(zone.charAt(5)) && Character.isDigit(zone.charAt(6));
        case 9:  // +HH:MM:SS
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                       && zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5))
                       && zone.charAt(6) == ':' && Character.isDigit(zone.charAt(7)) && Character.isDigit(zone.charAt(8));
        default:
            return false;
        }
    }

    private static int extractOffsetRepresentation(final String zone) {
        switch (zone.length()) {
        case 3:  // +HH
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600;
            }
            return Integer.MIN_VALUE;
        case 5:  // +HHMM
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                    && Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600
                           + (Character.digit(zone.charAt(3), 10) * 10 + Character.digit(zone.charAt(4), 10)) * 60;
            }
            return Integer.MIN_VALUE;
        case 6:  // +HH:MM
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                    && zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600
                           + (Character.digit(zone.charAt(4), 10) * 10 + Character.digit(zone.charAt(5), 10)) * 60;
            }
            return Integer.MIN_VALUE;
        case 7:  // +HHMMSS
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                    && Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4))
                    && Character.isDigit(zone.charAt(5)) && Character.isDigit(zone.charAt(6))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600
                           + (Character.digit(zone.charAt(3), 10) * 10 + Character.digit(zone.charAt(4), 10)) * 60
                           + (Character.digit(zone.charAt(5), 10) * 10 + Character.digit(zone.charAt(6), 10));
            }
            return Integer.MIN_VALUE;
        case 9:  // +HH:MM:SS
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2))
                    && zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5))
                    && zone.charAt(6) == ':' && Character.isDigit(zone.charAt(7)) && Character.isDigit(zone.charAt(8))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600
                           + (Character.digit(zone.charAt(4), 10) * 10 + Character.digit(zone.charAt(5), 10)) * 60
                           + (Character.digit(zone.charAt(7), 10) * 10 + Character.digit(zone.charAt(8), 10));
            }
            return Integer.MIN_VALUE;
        default:
            // Pass-through.
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Converts an upper-case timezone string to {@link java.time.ZoneOffset}
     * in the manner of Ruby's {@code Time}.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l97">ZoneOffset in Ruby</a>
     *
     * <p>An upper-cased timename string is expected here so that it can accept
     * typical zonetab names as-is, which are often upper-cased such as "GMT",
     * and "PST".
     *
     * <p>Its switch-case implementation is efficient enough. It is compiled to
     * efficient bytecode that is usually based on {@code hashCode}.
     * {@code HashMap} is not chosen because it needs boxing for integers.
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/strings-switch.html">Strings in switch Stateme
nts</a>
     * @see <a href="https://stackoverflow.com/questions/22110707/how-is-string-in-switch-statement-more-efficient-than-correspondi
ng-if-else-stat">How is String in switch statement more efficient than corresponding if-else statement?</a>
     */
    private static ZoneOffset mapZoneNametoZoneOffset(final String name) {
        switch (name) {
        case "UTC": return ZoneOffset.UTC;

        // ISO 8601
        case "Z":   return ZoneOffset.UTC;

        // RFC 822
        case "UT":  return ZoneOffset.UTC;
        case "GMT": return ZoneOffset.UTC;
        case "EST": return OFFSET_N_05;
        case "EDT": return OFFSET_N_04;
        case "CST": return OFFSET_N_06;
        case "CDT": return OFFSET_N_05;
        case "MST": return OFFSET_N_07;
        case "MDT": return OFFSET_N_06;
        case "PST": return OFFSET_N_08;
        case "PDT": return OFFSET_N_07;

        // Following definition of military zones is original one.
        // See RFC 1123 and RFC 2822 for the error in RFC 822.
        case "A":   return OFFSET_P_01;
        case "B":   return OFFSET_P_02;
        case "C":   return OFFSET_P_03;
        case "D":   return OFFSET_P_04;
        case "E":   return OFFSET_P_05;
        case "F":   return OFFSET_P_06;
        case "G":   return OFFSET_P_07;
        case "H":   return OFFSET_P_08;
        case "I":   return OFFSET_P_09;
        case "K":   return OFFSET_P_10;
        case "L":   return OFFSET_P_11;
        case "M":   return OFFSET_P_12;
        case "N":   return OFFSET_N_01;
        case "O":   return OFFSET_N_02;
        case "P":   return OFFSET_N_03;
        case "Q":   return OFFSET_N_04;
        case "R":   return OFFSET_N_05;
        case "S":   return OFFSET_N_06;
        case "T":   return OFFSET_N_07;
        case "U":   return OFFSET_N_08;
        case "V":   return OFFSET_N_09;
        case "W":   return OFFSET_N_10;
        case "X":   return OFFSET_N_11;
        case "Y":   return OFFSET_N_12;
        default:    // Pass-through.
        }
        return null;
    }

    private static final ZoneOffset OFFSET_N_01 = ZoneOffset.ofHours(-1);
    private static final ZoneOffset OFFSET_N_02 = ZoneOffset.ofHours(-2);
    private static final ZoneOffset OFFSET_N_03 = ZoneOffset.ofHours(-3);
    private static final ZoneOffset OFFSET_N_04 = ZoneOffset.ofHours(-4);
    private static final ZoneOffset OFFSET_N_05 = ZoneOffset.ofHours(-5);
    private static final ZoneOffset OFFSET_N_06 = ZoneOffset.ofHours(-6);
    private static final ZoneOffset OFFSET_N_07 = ZoneOffset.ofHours(-7);
    private static final ZoneOffset OFFSET_N_08 = ZoneOffset.ofHours(-8);
    private static final ZoneOffset OFFSET_N_09 = ZoneOffset.ofHours(-9);
    private static final ZoneOffset OFFSET_N_10 = ZoneOffset.ofHours(-10);
    private static final ZoneOffset OFFSET_N_11 = ZoneOffset.ofHours(-11);
    private static final ZoneOffset OFFSET_N_12 = ZoneOffset.ofHours(-12);
    private static final ZoneOffset OFFSET_P_01 = ZoneOffset.ofHours(+1);
    private static final ZoneOffset OFFSET_P_02 = ZoneOffset.ofHours(+2);
    private static final ZoneOffset OFFSET_P_03 = ZoneOffset.ofHours(+3);
    private static final ZoneOffset OFFSET_P_04 = ZoneOffset.ofHours(+4);
    private static final ZoneOffset OFFSET_P_05 = ZoneOffset.ofHours(+5);
    private static final ZoneOffset OFFSET_P_06 = ZoneOffset.ofHours(+6);
    private static final ZoneOffset OFFSET_P_07 = ZoneOffset.ofHours(+7);
    private static final ZoneOffset OFFSET_P_08 = ZoneOffset.ofHours(+8);
    private static final ZoneOffset OFFSET_P_09 = ZoneOffset.ofHours(+9);
    private static final ZoneOffset OFFSET_P_10 = ZoneOffset.ofHours(+10);
    private static final ZoneOffset OFFSET_P_11 = ZoneOffset.ofHours(+11);
    private static final ZoneOffset OFFSET_P_12 = ZoneOffset.ofHours(+12);
}

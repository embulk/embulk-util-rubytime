package org.embulk.util.rubytime;

import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Emulates parsing a zone in the manner of Ruby's Time.
 *
 * <p>{@code Time}'s parsing is different from {@code Date}'s parsing. For example,
 * {@code Date} recognizes {@code "CEST"} while {@code Time} does not recognize.
 * Both recognizes {@code "PST"}, though.
 *
 * <code>
 * $ env TZ=UTC irb
 * irb(main):001:0> require 'date'
 * => true
 * irb(main):002:0> require 'time'
 * => true
 *
 * irb(main):003:0> Date._strptime("CEST", "%z")
 * => {:zone=>"CEST", :offset=>7200}
 * irb(main):004:0> DateTime.strptime("2017-12-31 12:34:56 CEST", "%Y-%m-%d %H:%M:%S %z")
 * => #<DateTime: 2017-12-31T12:34:56+02:00 ((2458119j,38096s,0n),+7200s,2299161j)>
 * irb(main):005:0> Time.strptime("2017-12-31 12:34:56 CEST", "%Y-%m-%d %H:%M:%S %z")
 * => 2017-12-31 12:34:56 +0000
 *
 * irb(main):006:0> Date._strptime("PST", "%z")
 * => {:zone=>"PST", :offset=>-28800}
 * irb(main):007:0> DateTime.strptime("2017-12-31 12:34:56 PST", "%Y-%m-%d %H:%M:%S %z")
 * => #<DateTime: 2017-12-31T12:34:56-08:00 ((2458119j,74096s,0n),-28800s,2299161j)>
 * irb(main):008:0> Time.strptime("2017-12-31 12:34:56 PST", "%Y-%m-%d %H:%M:%S %z")
 * => 2017-12-31 12:34:56 -0800
 * </code>
 *
 * This class is public only to be called from DynamicColumnSetterFactory and DynamicPageBuilder.
 * It is not guaranteed to use this class from plugins. This class may be moved, renamed, or removed.
 *
 * A part of this class is reimplementation of Ruby v2.3.1's lib/time.rb. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup">lib/time.rb</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
final class TimeZones {
    private TimeZones() {
        // No instantiation.
    }

    /**
     * Converts a zone to an offset in seconds.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l134">zone_offset</a>
     */
    public static int toOffsetInSeconds(final String zone) {
        if (zone == null || zone.isEmpty()) {
            return Integer.MIN_VALUE;
        }

        if (zone.charAt(0) == '+' || zone.charAt(0) == '-') {
            return extractOffsetRepresentation(zone);
        }

        // NOTE: String#toUpperCase does not create a new instance when unnecessary.
        final int offsetInHours = mapZoneNametoOffsetInHours(zone.toUpperCase(Locale.ENGLISH));
        if (offsetInHours == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return offsetInHours * 3600;
    }

    /**
     * Parses a time zone ID to java.time.ZoneOffset basically in the same rule with Ruby v2.3.1's Time.strptime.
     *
     * The only difference from Ruby v2.3.1's Time.strptime is that it does not consider local time zone.
     * If the given zone is neither numerical nor predefined textual time zones, it returns defaultZoneOffset then.
     *
     * The method is reimplemented based on zone_offset from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l134">zone_offset</a>
     */
    public static ZoneOffset toZoneOffset(final String zone, final ZoneOffset defaultZoneOffset) {
        if (zone == null || zone.isEmpty()) {
            return defaultZoneOffset;
        }

        if (zone.charAt(0) == '+' || zone.charAt(0) == '-') {
            if (matchesOffsetRepresentation(zone)) {
                return ZoneOffset.of(zone);  // Delegates parsing to java.time.ZoneOffset.
            }
            return defaultZoneOffset;
        }

        // NOTE: String#toUpperCase does not create a new instance when unnecessary.
        final ZoneOffset zoneOffset = mapZoneNametoZoneOffset(zone.toUpperCase(Locale.ENGLISH));
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
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                       Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4));
        case 6:  // +HH:MM
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                       zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5));
        case 7:  // +HHMMSS
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                       Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4)) &&
                       Character.isDigit(zone.charAt(5)) && Character.isDigit(zone.charAt(6));
        case 9:  // +HH:MM:SS
            return Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                       zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5)) &&
                       zone.charAt(6) == ':' && Character.isDigit(zone.charAt(7)) && Character.isDigit(zone.charAt(8));
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
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                    Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600 +
                           (Character.digit(zone.charAt(3), 10) * 10 + Character.digit(zone.charAt(4), 10)) * 60;
            }
            return Integer.MIN_VALUE;
        case 6:  // +HH:MM
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                    zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600 +
                           (Character.digit(zone.charAt(4), 10) * 10 + Character.digit(zone.charAt(5), 10)) * 60;
            }
            return Integer.MIN_VALUE;
        case 7:  // +HHMMSS
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                    Character.isDigit(zone.charAt(3)) && Character.isDigit(zone.charAt(4)) &&
                    Character.isDigit(zone.charAt(5)) && Character.isDigit(zone.charAt(6))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600 +
                           (Character.digit(zone.charAt(3), 10) * 10 + Character.digit(zone.charAt(4), 10)) * 60 +
                           (Character.digit(zone.charAt(5), 10) * 10 + Character.digit(zone.charAt(6), 10));
            }
            return Integer.MIN_VALUE;
        case 9:  // +HH:MM:SS
            if (Character.isDigit(zone.charAt(1)) && Character.isDigit(zone.charAt(2)) &&
                    zone.charAt(3) == ':' && Character.isDigit(zone.charAt(4)) && Character.isDigit(zone.charAt(5)) &&
                    zone.charAt(6) == ':' && Character.isDigit(zone.charAt(7)) && Character.isDigit(zone.charAt(8))) {
                return (Character.digit(zone.charAt(1), 10) * 10 + Character.digit(zone.charAt(2), 10)) * 3600 +
                           (Character.digit(zone.charAt(4), 10) * 10 + Character.digit(zone.charAt(5), 10)) * 60 +
                           (Character.digit(zone.charAt(7), 10) * 10 + Character.digit(zone.charAt(8), 10));
            }
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Maps an upper-cased zone name to its corresponding time offset in the manner of Ruby's Time.
     *
     * <p>Upper-cased names are expected here so that it can accept typical zonetab names straightforward,
     * which are often upper-cased, for example, "GMT", "PST", and "JST".
     *
     * <p>Its switch-case implementation is efficient enough. It is compiled into efficient bytecode that is
     * usually based on {@code hashCode}. {@code HashMap} is not chosen because it needs boxing for integers.
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/strings-switch.html">Strings in switch Statements</a>
     * @see <a href="https://stackoverflow.com/questions/22110707/how-is-string-in-switch-statement-more-efficient-than-corresponding-if-else-stat">How is String in switch statement more efficient than corresponding if-else statement?</a>
     *
     * <p>The mapping is generated from {@code ZoneOffset} in {@code lib/time.rb} of Ruby 2.5.0.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l97">ZoneOffset</a>
     */
    private static int mapZoneNametoOffsetInHours(final String name) {
        switch (name) {
        case "UTC": return 0;

        // ISO 8601
        case "Z":   return 0;

        // RFC 822
        case "UT":  return 0;
        case "GMT": return 0;
        case "EST": return -5;
        case "EDT": return -4;
        case "CST": return -6;
        case "CDT": return -5;
        case "MST": return -7;
        case "MDT": return -6;
        case "PST": return -8;
        case "PDT": return -7;

        // Following definition of military zones is original one.
        // See RFC 1123 and RFC 2822 for the error in RFC 822.
        case "A":   return +1;
        case "B":   return +2;
        case "C":   return +3;
        case "D":   return +4;
        case "E":   return +5;
        case "F":   return +6;
        case "G":   return +7;
        case "H":   return +8;
        case "I":   return +9;
        case "K":   return +10;
        case "L":   return +11;
        case "M":   return +12;
        case "N":   return -1;
        case "O":   return -2;
        case "P":   return -3;
        case "Q":   return -4;
        case "R":   return -5;
        case "S":   return -6;
        case "T":   return -7;
        case "U":   return -8;
        case "V":   return -9;
        case "W":   return -10;
        case "X":   return -11;
        case "Y":   return -12;
        }
        return Integer.MIN_VALUE;
    }

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

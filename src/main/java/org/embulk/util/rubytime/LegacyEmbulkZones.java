package org.embulk.util.rubytime;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Emulates parsing a zone name in Embulk's legacy manner.
 *
 * <p>Embulk's legacy TimestampParser recognized "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", and "PDT"
 * with {@code org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis()} before they are looked up
 * from {@code org.joda.time.DateTimeZone.forID()} nor {@code org.joda.time.DateTimeZone.getAvailableIDs()}.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.8.20/embulk-core/src/main/java/org/embulk/spi/time/TimestampFormat.java#L40-L70">org.embulk.spi.time.TimestampFormat#parseDateTimeZone</a>
 * @see <a href="https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05">added TimestampFormatConfig, TimestampFormatter and TimestampParser - embulk/embulk@b97954a</a>
 * @see <a href="https://github.com/embulk/embulk/issues/860">Parse time zones in a consistent manner - Issue #860 - embulk/embulk</a>
 *
 * <p>However, the recognition by {@code org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis()}
 * was not "correct" unfortunately. For example, {@code "PDT"} should be {@code -07:00} as {@code "PDT"} was
 * a daylight saving time. But, {@code "PDT"} and {@code "PST"} were both recognized as {@code -08:00}.
 * It was because {@code "PDT"} and {@code "PST"} were aliases of {@code "America/Los_Angeles"}, not of a
 * fixed offset, in Joda-Time. Joda-Time originally intended to recognize {@code "2017-08-20 12:34:56 PDT"}
 * as {@code "2017-08-20 12:34:56 America/Los_Angeles"}, and it was {@code -07:00} since {@code 2017-08-20}
 * was in a daylight saving time. But, {@code DateTimeFormat.forPattern("z").parseMillis("PDT")} was aliased
 * to {@code DateTimeFormat.forPattern("z").parseMillis("America/Los_Angeles")}, and it was recognized as
 * a standard time, not a daylight saving time, because just {@code "America/Los_Angeles"} did not have any
 * date to determine the date is a standard time or a daylight saving time.
 *
 * @see <a href="https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L432-L448">org.joda.time.DateTimeUtils#buildDefaultTimeZoneNames</a>
 * @see <a href="http://www.joda.org/joda-time/apidocs/org/joda/time/DateTimeUtils.html#getDefaultTimeZoneNames--">org.joda.time.DateTimeUtils#getDefaultTimeZoneNames</a>
 *
 * <p>If {@code org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis()} did not solve the given
 * name, {@code org.joda.time.DateTimeZone.forID} was used next in legacy Embulk.
 *
 * <p>On the other hand to replace Joda-Time by {@code java.time} in this library, {@code java.time.ZoneId.of}
 * replaces {@code DateTimeZone.forID} of Joda-Time. Because {@code ZoneId.of} does not recognize {@code "HST"}
 * and {@code "ROC"} that are recognized by {@code DateTimeZone.forID}, these are additionally specified as
 * aliases for {@code ZoneId.of}. ({@code "EST"} and {@code "HST"} are not recognized by {@code ZoneId.of} as
 * well, but they are covered by the cases above.)
 *
 * @see <a href="http://joda-time.sourceforge.net/timezones.html">Joda-Time - Java date and time API - Time Zones</a>
 *
 * <p>If the given zone is not recognized above, it is fallback to {@code org.embulk.util.rubytime.DateZones}.
 */
public class LegacyEmbulkZones {
    private LegacyEmbulkZones() {
        // No instantiation.
    }

    /**
     * Converts a zone name to java.time.ZoneId in Embulk's legacy manner.
     *
     * <p>It recognizes zone names in the following priority.
     *
     * <ol>
     * <li>"Z" is always recognized as UTC in the highest priority.
     * <li>If the ID is "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", or "PDT", parsed by ZoneId.of with alias.
     * <li>If the ID is "HST", "ROC", or recognized by ZoneId.of, it is parsed by ZoneId.of with alias.
     * <li>Otherwise, the zone ID is recognized by Ruby-compatible zone tab.
     * <li>If none of the above does not recognize the zone ID, it returns null.
     * </ol>
     *
     * <p>Some of its offset transition (e.g. daylight saving time) are a little different from Embulk's
     * real legacy recognition. But, the difference basically comes from their base tz database versions.
     * The difference should be acceptable as such a difference can always happen by updating the tzdb.
     */
    public static ZoneId toZoneId(final String zone) {
        if (zone == null) {
            return null;
        }

        if (zone.equals("Z")) {
            return ZoneOffset.UTC;
        }

        try {
            return ZoneId.of(zone, ALIASES);  // Is is never null unless Exception is thrown.
        } catch (final DateTimeException ex) {
            // Fallback to Ruby's Date.
            final int offsetInSeconds = DateZones.toOffsetInSeconds(zone);
            if (offsetInSeconds != Integer.MIN_VALUE) {
                return ZoneOffset.ofTotalSeconds(offsetInSeconds);
            }
        }
        return null;
    }

    static {
        final HashMap<String, String> aliases = new HashMap<>();
        aliases.put("EST", "-05:00");
        aliases.put("EDT", "-05:00");
        aliases.put("CST", "-06:00");
        aliases.put("CDT", "-06:00");
        aliases.put("MST", "-07:00");
        aliases.put("MDT", "-07:00");
        aliases.put("PST", "-08:00");
        aliases.put("PDT", "-08:00");

        aliases.put("HST", "-10:00");
        aliases.put("ROC", "Asia/Taipei");

        ALIASES = Collections.unmodifiableMap(aliases);
    }

    private static final Map<String, String> ALIASES;
}

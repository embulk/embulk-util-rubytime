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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;

/**
 * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}, with a rule close to Ruby's {@code Time.strptime}.
 *
 * <p>A difference from Ruby's {@code Time.strptime} is that it does not consider "now" and local time zone.
 * If the given zone is neither numerical nor predefined textual time zones, it returns defaultZoneOffset then.
 *
 * <p>Ruby's {@code Time} class implements a proleptic Gregorian calendar and has no concept of calendar reform.
 *
 * <p>Epoch seconds (%s) and epoch milliseconds (%Q) are prioritized over calendar date/time although
 * fraction part (%L/%N) is added to the epoch seconds/milliseconds.
 *
 * <pre>{@code
 * irb(main):001:0> require 'time'
 * => true
 * irb(main):002:0> Time.strptime("123456789 12.345", "%Q %S.%N").nsec
 * => 134000000
 * irb(main):003:0> Time.strptime("12.345 123456789", "%S.%N %Q").nsec
 * => 134000000
 * irb(main):004:0> Time.strptime("12.345", "%S.%N").nsec
 * => 345000000
 * irb(main):005:0> Time.strptime("1500000000.123456789", "%s.%N").nsec
 * => 123456789
 * irb(main):006:0> Time.strptime("1500000000456.111111111", "%Q.%N").nsec
 * => 567111111
 * irb(main):007:0> Time.strptime("1500000000.123", "%s.%L").nsec
 * => 123000000
 * irb(main):008:0> Time.strptime("1500000000456.111", "%Q.%L").nsec
 * => 567000000
 * }
 * </pre>
 *
 * <p>Day of the year (yday: {@code DAY_OF_YEAR}) is not considered unlike {@code DateTime.strptime}.
 *
 * <pre>{@code
 * irb(main):002:0> Time.strptime("2001-128T23:59:59", "%Y-%jT%H:%M:%S")
 * => 2001-01-01 23:59:59 +0900
 * irb(main):004:0> DateTime.strptime("2001-128T23:59:59", "%Y-%jT%H:%M:%S")
 * => #<DateTime: 2001-05-08T23:59:59+00:00 ((2452038j,86399s,0n),+0s,2299161j)>
 * }
 * </pre>
 *
 * <p>The resolver is reimplemented based on {@code Time.strptime} of Ruby v2.5.0.
 *
 * @see <a href="https://docs.ruby-lang.org/en/2.5.0/DateTime.html#class-DateTime-label-When+should+you+use+DateTime+and+when+should+you+use+Time-3F">When should you use DateTime and when should you use Time?</a>
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l431">Time.strptime</a>
 */
final class DefaultRubyTimeResolver extends RubyDateTimeResolver {
    DefaultRubyTimeResolver(
            final boolean acceptsEmpty,
            final ZoneOffset defaultOffset,
            final int defaultYear,
            final int defaultMonthOfYear,
            final int defaultDayOfMonth,
            final int defaultHourOfDay,
            final int defaultMinuteOfHour,
            final int defaultSecondOfMinute,
            final int defaultNanoOfSecond) {
        this.acceptsEmpty = acceptsEmpty;
        this.defaultOffset = defaultOffset;
        this.defaultYear = defaultYear;
        this.defaultMonthOfYear = defaultMonthOfYear;
        this.defaultDayOfMonth = defaultDayOfMonth;
        this.defaultHourOfDay = defaultHourOfDay;
        this.defaultMinuteOfHour = defaultMinuteOfHour;
        this.defaultSecondOfMinute = defaultSecondOfMinute;
        this.defaultNanoOfSecond = defaultNanoOfSecond;
    }

    // TODO(dmikurube): Confirm whether prioritizing |original| over |resolved| is really correct.
    private class ResolvedFromOffsetDateTime implements TemporalAccessor, RubyTemporalQueryResolver {
        private ResolvedFromOffsetDateTime(
                final TemporalAccessor original,
                final OffsetDateTime resolvedDateTime) {
            this.original = original;
            this.resolvedDateTime = resolvedDateTime;
        }

        @Override
        public long getLong(final TemporalField field) {
            if (this.original.isSupported(field)) {
                return this.original.getLong(field);
            }
            return this.resolvedDateTime.getLong(field);
        }

        @Override
        public boolean isSupported(final TemporalField field) {
            if (this.original.isSupported(field)) {
                return true;
            }
            return this.resolvedDateTime.isSupported(field);
        }

        @Override
        public <R> R query(final TemporalQuery<R> query) {
            final R resultOriginal = this.original.query(query);
            if (resultOriginal != null) {
                return resultOriginal;
            }
            return this.resolvedDateTime.query(query);
        }

        @Override
        public ValueRange range(final TemporalField field) {
            if (this.original.isSupported(field)) {
                return this.original.range(field);
            }
            return this.resolvedDateTime.range(field);
        }

        @Override
        public String getOriginalText() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getOriginalText();
            }
            return null;
        }

        @Override
        public String getZone() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getZone();
            }
            return null;
        }

        @Override
        public String getLeftover() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getLeftover();
            }
            return null;
        }

        private final TemporalAccessor original;
        private final OffsetDateTime resolvedDateTime;
    }

    private class ResolvedFromInstant implements TemporalAccessor, RubyTemporalQueryResolver {
        private ResolvedFromInstant(final TemporalAccessor original, final Instant resolvedInstant) {
            this.original = original;
            this.resolvedInstant = resolvedInstant;
            this.resolvedDateTime = OffsetDateTime.ofInstant(resolvedInstant, ZoneOffset.UTC);
        }

        @Override
        public long getLong(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return this.resolvedInstant.getLong(field);
            }
            if (this.original.isSupported(field)) {
                return this.original.getLong(field);
            }
            return this.resolvedDateTime.getLong(field);
        }

        @Override
        public boolean isSupported(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return true;
            }
            if (this.original.isSupported(field)) {
                return true;
            }
            return this.resolvedDateTime.isSupported(field);
        }

        @Override
        public <R> R query(final TemporalQuery<R> query) {
            if (RubyTemporalQueries.isSpecificQuery(query)) {
                // Some special queries are prioritized.
                final R resultOriginal = this.original.query(query);
                if (resultOriginal != null) {
                    return resultOriginal;
                }
            }
            final R resultFromResolvedInstant = this.resolvedInstant.query(query);
            if (resultFromResolvedInstant != null) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return resultFromResolvedInstant;
            }
            final R resultOriginal = this.original.query(query);
            if (resultOriginal != null) {
                return resultOriginal;
            }
            return this.resolvedDateTime.query(query);
        }

        @Override
        public ValueRange range(final TemporalField field) {
            if (this.resolvedInstant.isSupported(field)) {
                // Its own Instant is intentionally prioritized so that a query for Instant does not return
                // not SECONDS_SINCE_EPOCH + NANO_OF_SECOND unintentionally.
                // Its Instant may need to be MILLISECONDS_SINCE_EPOCH + NANO_OF_SECOND instead.
                return this.resolvedInstant.range(field);
            }
            if (this.original.isSupported(field)) {
                return this.original.range(field);
            }
            return this.resolvedDateTime.range(field);
        }

        @Override
        public String getOriginalText() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getOriginalText();
            }
            return null;
        }

        @Override
        public String getZone() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getZone();
            }
            return null;
        }

        @Override
        public String getLeftover() {
            if (this.original instanceof RubyTemporalQueryResolver) {
                final RubyTemporalQueryResolver resolver = (RubyTemporalQueryResolver) this.original;
                return resolver.getLeftover();
            }
            return null;
        }

        private final TemporalAccessor original;
        private final Instant resolvedInstant;
        private final OffsetDateTime resolvedDateTime;
    }

    /**
     * Resolves {@link java.time.temporal.TemporalAccessor}, date-time data parsed by {@link RubyDateTimeFormatter}, with a rule close to Ruby's {@code Time.strptime}.
     *
     * @param original  the original date-time data parsed by {@link RubyDateTimeFormatter}, not null
     * @return the resolved temporal object, not null
     */
    @Override
    public TemporalAccessor resolve(final TemporalAccessor original) {
        final String zone = original.query(RubyTemporalQueries.zone());
        final ZoneOffset offset = RubyTimeZones.toZoneOffset(zone, defaultOffset);

        if (offset == null) {
            if (zone == null) {
                throw new DateTimeException("Empty time zone ID.");
            } else {
                throw new DateTimeException("Invalid time zone ID: '" + zone);
            }
        }

        if (original.isSupported(ChronoField.INSTANT_SECONDS) || original.isSupported(RubyChronoFields.INSTANT_MILLIS)) {
            final Instant instant;
            if (original.isSupported(RubyChronoFields.INSTANT_MILLIS)) {
                // INSTANT_MILLIS (%Q) is prioritized if exists.
                final long instantMillis = original.getLong(RubyChronoFields.INSTANT_MILLIS);
                instant = Instant.ofEpochMilli(instantMillis);
            } else {
                final long instantSeconds = original.getLong(ChronoField.INSTANT_SECONDS);
                instant = Instant.ofEpochSecond(instantSeconds);
            }

            if (original.isSupported(ChronoField.NANO_OF_SECOND)) {
                // The fraction part is "added" to the epoch second in case both are specified.
                // irb(main):002:0> Time.strptime("1500000000.123456789", "%s.%N").nsec
                // => 123456789
                // irb(main):003:0> Time.strptime("1500000000456.111111111", "%Q.%N").nsec
                // => 567111111
                //
                // If "sec_fraction" is specified, the value is used like |Time.at(seconds, sec_fraction * 1000000)|.
                // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/lib/time.rb?view=markup#l431
                //
                // |Time.at| adds "seconds" (the epoch) and "sec_fraction" (the fraction part) with scaling.
                // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/time.c?view=markup#l2405
                //
                // It behaves the same even if "seconds" is specified as a Rational, not an Integer.
                // irb(main):004:0> Time.at(Rational(1500000000789, 1000), 100123).nsec
                // => 889123000
                final int nanoOfSecond = original.get(ChronoField.NANO_OF_SECOND);
                if (!instant.isBefore(Instant.EPOCH)) {
                    return new ResolvedFromInstant(original, instant.plusNanos(nanoOfSecond));
                } else {
                    // NANO_OF_SECOND is a "literal" fraction part of a second, by definition.
                    // It is because "%N" (NANO_OF_SECOND) is used for calendar date-time, not only for seconds since epoch.
                    //
                    // Date._strptime("2019-06-08 12:34:56.789", "%Y-%m-%d %H:%M:%S.%N")
                    // => {:year=>2019, :mon=>6, :mday=>8, :hour=>12, :min=>34, :sec=>56, :sec_fraction=>(789/1000)}
                    // Date._strptime("1960-06-08 12:34:56.789", "%Y-%m-%d %H:%M:%S.%N")
                    // => {:year=>1960, :mon=>6, :mday=>8, :hour=>12, :min=>34, :sec=>56, :sec_fraction=>(789/1000)}
                    //
                    // Date._strptime( "123.789", "%s.%N")
                    // => {:seconds=>123, :sec_fraction=>(789/1000)}
                    // Date._strptime("-123.789", "%s.%N")
                    // => {:seconds=>-123, :sec_fraction=>(789/1000)}
                    //
                    // Then, if second's integer part is negative, the fraction part must be considered as negative.
                    // The Ruby interpreter does the same.
                    //
                    // See: https://git.ruby-lang.org/ruby.git/tree/lib/time.rb?id=v2_6_3#n449
                    return new ResolvedFromInstant(original, instant.minusNanos(nanoOfSecond));
                }
            } else {
                return new ResolvedFromInstant(original, instant);
            }
            // TODO: Store the zone offset information in Resolved, instead of ZoneOffset.UTC.
            //
            // INSTANT_SECONDS by itself is always a value as of UTC, but results of
            // Ruby's Time.strptime can have non-UTC offsets.
            //
            // From test/test_time.rb,
            //    t = Time.strptime('0 +0100', '%s %z')
            //    assert_equal(0, t.to_r)
            //    assert_equal(3600, t.utc_offset)
            //
            // The test is working-around it by getting zone offset from Parsed, not Resolved.
        }

        return new ResolvedFromOffsetDateTime(original, this.getOffsetAppliedDateTime(original, offset));
    }

    /**
     * Applies time zone offset to date/time information, and creates java.time.OffsetDateTime in UTC.
     *
     * The method is reimplemented based on apply_offset from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l208">apply_offset</a>
     */
    private OffsetDateTime getOffsetAppliedDateTime(final TemporalAccessor original, final ZoneOffset zoneOffset) {
        final Boolean parsedLeapSecond = original.query(DateTimeFormatter.parsedLeapSecond());
        final Period parsedExcessDays = original.query(DateTimeFormatter.parsedExcessDays());

        if (!this.acceptsEmpty
                    && !original.isSupported(ChronoField.YEAR)
                    && !original.isSupported(ChronoField.MONTH_OF_YEAR)
                    && !original.isSupported(ChronoField.DAY_OF_MONTH)
                    && !original.isSupported(ChronoField.HOUR_OF_DAY)
                    && !original.isSupported(ChronoField.MINUTE_OF_HOUR)
                    && !original.isSupported(ChronoField.SECOND_OF_MINUTE)
                    && !original.isSupported(ChronoField.NANO_OF_SECOND)) {
            throw new DateTimeException("No time information.");
        }

        int year = original.isSupported(ChronoField.YEAR)
                ? original.get(ChronoField.YEAR) : this.defaultYear;
        int monthOfYear = original.isSupported(ChronoField.MONTH_OF_YEAR)
                ? original.get(ChronoField.MONTH_OF_YEAR) : this.defaultMonthOfYear;
        int dayOfMonth = original.isSupported(ChronoField.DAY_OF_MONTH)
                ? original.get(ChronoField.DAY_OF_MONTH) : this.defaultDayOfMonth;
        int hourOfDay = original.isSupported(ChronoField.HOUR_OF_DAY)
                ? original.get(ChronoField.HOUR_OF_DAY) : this.defaultHourOfDay;
        int minuteOfHour = original.isSupported(ChronoField.MINUTE_OF_HOUR)
                ? original.get(ChronoField.MINUTE_OF_HOUR) : this.defaultMinuteOfHour;
        int secondOfMinute = original.isSupported(ChronoField.SECOND_OF_MINUTE)
                ? original.get(ChronoField.SECOND_OF_MINUTE) : this.defaultSecondOfMinute;
        int nanoOfSecond = original.isSupported(ChronoField.NANO_OF_SECOND)
                ? original.get(ChronoField.NANO_OF_SECOND) : this.defaultNanoOfSecond;

        int offset = zoneOffset.getTotalSeconds();

        // Processing leap seconds using time offsets in a bit tricky manner.
        //
        // Leap seconds are considered as the next second in Time.strptime.
        //
        // irb(main):002:0> Time.strptime("2001-02-03T23:59:60", "%Y-%m-%dT%H:%M:%S")
        // => 2001-02-04 00:00:00 +0900
        if (parsedLeapSecond != null && (boolean) parsedLeapSecond) {
            secondOfMinute = 59;
            offset -= 1;
        }

        // Processing 24h in clock hours using time offsets in a bit tricky manner.
        //
        // 24h is considered as 0h in the next day in Time.strptime (if non-UTC time zone is specified).
        //
        // irb(main):002:0> Time.strptime("24:59:59 PST", "%H:%M:%S %Z")
        // => 2018-01-13 00:59:59 -0800
        // irb(main):003:0> Time.strptime("24:59:59 UTC", "%H:%M:%S %Z")
        // ArgumentError: min out of range
        // ...
        // irb(main):004:0> Time.strptime("24:59:59", "%H:%M:%S")
        // ArgumentError: min out of range
        // ...
        //
        // 24h is always handled as 0h in the next day in Embulk as if non-UTC time zone is specified.
        if (parsedExcessDays != null) {
            if (parsedExcessDays.getDays() == 1
                        && parsedExcessDays.getMonths() == 0
                        && parsedExcessDays.getYears() == 0) {
                offset -= 24 * 60 * 60;
                hourOfDay = 0;  // TODO: Better to be hourOfDay -= 24 ?
            } else if (!parsedExcessDays.isZero()) {
                throw new DateTimeException("Hour is not in the range of 0-24.");
            }
        }

        if (offset < 0) {
            offset = -offset;

            final int offsetSecond = offset % 60;
            offset = offset / 60;
            if (offsetSecond != 0) {
                secondOfMinute += offsetSecond;
                offset += secondOfMinute / 60;
                secondOfMinute %= 60;
            }

            final int offsetMinute = offset % 60;
            offset = offset / 60;
            if (offsetMinute != 0) {
                minuteOfHour += offsetMinute;
                offset += minuteOfHour / 60;
                minuteOfHour %= 60;
            }

            final int offsetHour = offset % 24;
            offset = offset / 24;
            if (offsetHour != 0) {
                hourOfDay += offsetHour;
                offset += hourOfDay / 24;
                hourOfDay %= 24;
            }

            if (offset != 0) {
                final int days = monthDays(year, monthOfYear);
                if (monthOfYear == 2 && days == 28 && dayOfMonth == 29) {
                    // Check for a leap year before applying a leap second.
                    // Without this, "2001-02-29T23:59:60" successfully goes to "2001-03-01T00:00:00" inappropriately.
                    throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
                }
                dayOfMonth += offset;
                if (days < dayOfMonth) {
                    monthOfYear += 1;
                    if (12 < monthOfYear) {
                        monthOfYear = 1;
                        year += 1;
                    }
                    dayOfMonth = 1;
                }
            }

        } else if (0 < offset) {
            final int offsetSecond = offset % 60;
            offset /= 60;
            if (offsetSecond != 0) {
                secondOfMinute -= offsetSecond;
                offset -= secondOfMinute / 60;
                secondOfMinute %= 60;
            }

            final int offsetMinute = offset % 60;
            offset /= 60;
            if (offsetMinute != 0) {
                minuteOfHour -= offsetMinute;
                offset -= minuteOfHour / 60;
                minuteOfHour %= 60;
            }

            final int offsetHour = offset % 24;
            offset /= 24;
            if (offsetHour != 0) {
                hourOfDay -= offsetHour;
                offset -= hourOfDay / 24;
                hourOfDay %= 24;
            }

            if (offset != 0) {
                dayOfMonth -= offset;
                if (dayOfMonth < 1) {
                    monthOfYear -= 1;
                    if (monthOfYear < 1) {
                        year -= 1;
                        monthOfYear = 12;
                    }
                    dayOfMonth = monthDays(year, monthOfYear);
                }
            }
        }

        return OffsetDateTime.of(
                year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, ZoneOffset.UTC);
    }

    private int getWithDefaultOrThrow(final TemporalAccessor original, final TemporalField field, final int defaultValue) {
        if (original.isSupported(field)) {
            return original.get(field);
        } else {
            if (this.acceptsEmpty) {
                return defaultValue;
            } else {
                throw new DateTimeException("No time information enough.");
            }
        }
    }

    /**
     * Returns the number of days in the given month of the given year.
     *
     * The method is reimplemented based on month_days from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l199">month_days</a>
     */
    private static int monthDays(final int year, final int monthOfYear) {
        if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) {
            return leapYearMonthDays[monthOfYear - 1];
        } else {
            return commonYearMonthDays[monthOfYear - 1];
        }
    }

    /**
     * Numbers of days per month and year.
     *
     * The constants are imported from LeapYearMonthDays and CommonYearMonthDays in Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l197">LeapYearMonthDays</a>
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l198">CommonYearMonthDays</a>
     */
    private static final int[] leapYearMonthDays = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    private static final int[] commonYearMonthDays = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private final boolean acceptsEmpty;
    private final ZoneOffset defaultOffset;
    private final int defaultYear;
    private final int defaultMonthOfYear;
    private final int defaultDayOfMonth;
    private final int defaultHourOfDay;
    private final int defaultMinuteOfHour;
    private final int defaultSecondOfMinute;
    private final int defaultNanoOfSecond;
}

/*
 * Copyright 2020 The Embulk project
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

final class FormatterWithContext {
    FormatterWithContext(final TemporalAccessor temporal) {
        this.temporal = temporal;
    }

    String format(final Format format) {
        return this.format(format, RubyDateTimeFormatter.ZoneNameStyle.NONE);
    }

    String format(final Format format, final RubyDateTimeFormatter.ZoneNameStyle zoneNameStyle) {
        final StringBuilder builder = new StringBuilder();

        for (final Format.TokenWithNext tokenWithNext : format) {
            final FormatToken token = tokenWithNext.getToken();

            if (token.isImmediate()) {
                builder.append(token.getImmediate().get());
            } else {
                final FormatDirective directive = token.getFormatDirective().get();
                final FormatDirectiveOptions options = token.getDirectiveOptions().get();

                switch (directive) {
                    // %% - "%"
                    case IMMEDIATE_PERCENT:
                        this.fillPadding(builder, options, ' ', 0, 1);
                        builder.append('%');
                        break;

                    // %n - "\n"
                    case IMMEDIATE_NEWLINE:
                        this.fillPadding(builder, options, ' ', 0, 1);
                        builder.append('\n');
                        break;

                    // %t - "\t"
                    case IMMEDIATE_TAB:
                        this.fillPadding(builder, options, ' ', 0, 1);
                        builder.append('\t');
                        break;

                    // %a - The abbreviated name (``Sun'')
                    case DAY_OF_WEEK_ABBREVIATED_NAME:
                        this.appendDayOfWeekName(builder, options, true);
                        break;

                    // %A - The full weekday name (``Sunday'')
                    case DAY_OF_WEEK_FULL_NAME:
                        this.appendDayOfWeekName(builder, options, false);
                        break;

                    // %b, %h - The abbreviated month name (``Jan'')
                    case MONTH_OF_YEAR_ABBREVIATED_NAME:
                    case MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H:
                        this.appendMonthOfYearName(builder, options, true);
                        break;

                    // %B - The full month name (``January'')
                    case MONTH_OF_YEAR_FULL_NAME:
                        this.appendMonthOfYearName(builder, options, false);
                        break;

                    // %C - year / 100 (round down.  20 in 2009)
                    case CENTURY:
                        this.appendCentury(builder, options);
                        break;

                    // %d, %Od - Day of the month, zero-padded (01..31)
                    case DAY_OF_MONTH_ZERO_PADDED:
                        this.appendDayOfMonth(builder, options, '0');
                        break;

                    // %e, %Oe - Day of the month, blank-padded ( 1..31)
                    case DAY_OF_MONTH_BLANK_PADDED:
                        this.appendDayOfMonth(builder, options, ' ');
                        break;

                    // %G - The week-based year
                    case WEEK_BASED_YEAR_WITH_CENTURY:
                        this.appendWeekBasedYear(builder, options, true);
                        break;

                    // %g - The last 2 digits of the week-based year (00..99)
                    case WEEK_BASED_YEAR_WITHOUT_CENTURY:
                        this.appendWeekBasedYear(builder, options, false);
                        break;

                    // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    case HOUR_OF_DAY_ZERO_PADDED:
                        this.appendHourOfDay(builder, options, '0');
                        break;

                    // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                    case HOUR_OF_DAY_BLANK_PADDED:
                        this.appendHourOfDay(builder, options, ' ');
                        break;

                    // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    case HOUR_OF_AMPM_ZERO_PADDED:
                        this.appendHourOfAmPm(builder, options, '0');
                        break;

                    // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                    case HOUR_OF_AMPM_BLANK_PADDED:
                        this.appendHourOfAmPm(builder, options, ' ');
                        break;

                    // %j - Day of the year (001..366)
                    case DAY_OF_YEAR:
                        this.appendDayOfYear(builder, options);
                        break;

                    // %L - Millisecond of the second (000..999)
                    case MILLI_OF_SECOND:
                        this.appendSubsecond(builder, options, 3);
                        break;

                    // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                    case NANO_OF_SECOND:
                        this.appendSubsecond(builder, options, 9);
                        break;

                    // %M, %OM - Minute of the hour (00..59)
                    case MINUTE_OF_HOUR:
                        this.appendMinuteOfHour(builder, options);
                        break;

                    // %m, %Om - Month of the year, zero-padded (01..12)
                    case MONTH_OF_YEAR:
                        this.appendMonthOfYear(builder, options);
                        break;

                    // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                    case AMPM_OF_DAY_UPPER_CASE:
                        this.appendAmPmOfDay(builder, options, true);
                        break;

                    // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    case AMPM_OF_DAY_LOWER_CASE:
                        this.appendAmPmOfDay(builder, options, false);
                        break;

                    // %Q - Number of milliseconds since 1970-01-01 00:00:00 UTC.
                    case MILLISECONDS_SINCE_EPOCH:
                        // %Q is not supported in formatter. %Q is only for parser.
                        builder.append(token.getImmediate().orElse(""));
                        break;

                    // %S - Second of the minute (00..59)
                    case SECOND_OF_MINUTE:
                        this.appendSecondOfMinute(builder, options);
                        break;

                    // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                    case SECONDS_SINCE_EPOCH:
                        this.appendSecondsSinceEpoch(builder, options);
                        break;

                    // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    case WEEK_OF_YEAR_STARTING_WITH_SUNDAY:
                        this.appendWeekOfYear(builder, options, DayOfWeek.SUNDAY);
                        break;

                    // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                    case WEEK_OF_YEAR_STARTING_WITH_MONDAY:
                        this.appendWeekOfYear(builder, options, DayOfWeek.MONDAY);
                        break;

                    // %u, %Ou - Day of the week (Monday is 1, 1..7)
                    case DAY_OF_WEEK_STARTING_WITH_MONDAY_1:
                        this.appendDayOfWeek(builder, options, DayOfWeek.MONDAY);
                        break;

                    // %V, %OV - Week number of the week-based year (01..53)
                    case WEEK_OF_WEEK_BASED_YEAR:
                        this.appendWeekOfWeekBasedYear(builder, options);
                        break;

                    // %w - Day of the week (Sunday is 0, 0..6)
                    case DAY_OF_WEEK_STARTING_WITH_SUNDAY_0:
                        this.appendDayOfWeek(builder, options, DayOfWeek.SUNDAY);
                        break;

                    // %Y, %EY - Year with century (can be negative, 4 digits at least)
                    //           -0001, 0000, 1995, 2009, 14292, etc.
                    case YEAR_WITH_CENTURY:
                        this.appendYearWithCentury(builder, options);
                        break;

                    // %y, %Ey, %Oy - year % 100 (00..99)
                    case YEAR_WITHOUT_CENTURY:
                        this.appendYearWithoutCentury(builder, options);
                        break;

                    // %Z - Time zone abbreviation name
                    case TIME_ZONE_NAME:
                        this.appendTimeZoneName(builder, options, zoneNameStyle);
                        break;

                    // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                    //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                    //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                    //      %:::z - hour, minute and second offset from UTC (e.g. +09, +09:30, +09:30:30)
                    case TIME_OFFSET:
                        this.appendTimeOffset(builder, token, options);
                        break;

                    // %c - "%a %b %e %H:%M:%S %Y"
                    case RECURRED_LOWER_C:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendDayOfWeekName(innerBuilder, DEFAULT, true);
                            innerBuilder.append(" ");
                            this.appendMonthOfYearName(innerBuilder, DEFAULT, true);
                            innerBuilder.append(" ");
                            this.appendDayOfMonth(innerBuilder, DEFAULT, ' ');
                            innerBuilder.append(" ");
                            this.appendHourOfDay(innerBuilder, DEFAULT, '0');
                            innerBuilder.append(":");
                            this.appendMinuteOfHour(innerBuilder, DEFAULT);
                            innerBuilder.append(":");
                            this.appendSecondOfMinute(innerBuilder, DEFAULT);
                            innerBuilder.append(" ");
                            this.appendYearWithCentury(innerBuilder, DEFAULT);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            if (options.isUpper()) {
                                builder.append(innerBuilder.toString().toUpperCase(Locale.ROOT));
                            } else {
                                builder.append(innerBuilder.toString());
                            }
                        }
                        break;

                    // %D - "%m/%d/%y"
                    // %x - "%m/%d/%y"
                    case RECURRED_UPPER_D:
                    case RECURRED_LOWER_X:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendMonthOfYear(innerBuilder, DEFAULT);
                            innerBuilder.append("/");
                            this.appendDayOfMonth(innerBuilder, DEFAULT, '0');
                            innerBuilder.append("/");
                            this.appendYearWithoutCentury(innerBuilder, DEFAULT);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            builder.append(innerBuilder.toString());
                        }
                        break;

                    // %F - "%Y-%m-%d"
                    case RECURRED_UPPER_F:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendYearWithCentury(innerBuilder, DEFAULT);
                            innerBuilder.append("-");
                            this.appendMonthOfYear(innerBuilder, DEFAULT);
                            innerBuilder.append("-");
                            this.appendDayOfMonth(innerBuilder, DEFAULT, '0');

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            builder.append(innerBuilder.toString());
                        }
                        break;

                    // %R - "%H:%M"
                    case RECURRED_UPPER_R:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendHourOfDay(innerBuilder, DEFAULT, '0');
                            innerBuilder.append(":");
                            this.appendMinuteOfHour(innerBuilder, DEFAULT);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            builder.append(innerBuilder.toString());
                        }
                        break;

                    // %r - "%I:%M:%S %p"
                    case RECURRED_LOWER_R:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendHourOfAmPm(innerBuilder, DEFAULT, '0');
                            innerBuilder.append(":");
                            this.appendMinuteOfHour(innerBuilder, DEFAULT);
                            innerBuilder.append(":");
                            this.appendSecondOfMinute(innerBuilder, DEFAULT);
                            innerBuilder.append(" ");
                            this.appendAmPmOfDay(innerBuilder, DEFAULT, true);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            builder.append(innerBuilder.toString());
                        }
                        break;

                    // %T - "%H:%M:%S"
                    // %X - "%H:%M:%S"
                    case RECURRED_UPPER_T:
                    case RECURRED_UPPER_X:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendHourOfDay(innerBuilder, DEFAULT, '0');
                            innerBuilder.append(":");
                            this.appendMinuteOfHour(innerBuilder, DEFAULT);
                            innerBuilder.append(":");
                            this.appendSecondOfMinute(innerBuilder, DEFAULT);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            builder.append(innerBuilder.toString());
                        }
                        break;

                    // %V - "%e-%b-%Y"
                    case RECURRED_LOWER_V:
                        {
                            final StringBuilder innerBuilder = new StringBuilder();
                            this.appendDayOfMonth(innerBuilder, DEFAULT, ' ');
                            innerBuilder.append(":");
                            this.appendMonthOfYearName(innerBuilder, DEFAULT, true);
                            innerBuilder.append(":");
                            this.appendYearWithCentury(innerBuilder, DEFAULT);

                            this.fillPadding(builder, options, ' ', 0, innerBuilder.length());
                            if (options.isUpper()) {
                                builder.append(innerBuilder.toString().toUpperCase(Locale.ROOT));
                            } else {
                                builder.append(innerBuilder.toString());
                            }
                        }
                        break;

                    // %+ - "%a %b %e %H:%M:%S %Z %Y"
                    case RECURRED_PLUS:
                        // %+ is not supported in formatter. %+ is only for parser.
                        builder.append(token.getImmediate().orElse(""));
                        break;
                }
            }
        }

        return builder.toString();
    }

    private void fillPadding(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final char defaultPadding,
            final int defaultPrecision,
            final int digits) {
        final int precision = options.getPrecision(defaultPrecision);
        if ((!options.isLeft()) && precision > digits) {
            for (int i = 0; i < precision - digits; i++) {
                builder.append(options.getPadding(defaultPadding));
            }
        }
    }

    private void appendDayOfWeekName(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final boolean abbreviated) {
        final int dayOfWeekNumber = this.temporal.get(ChronoField.DAY_OF_WEEK);
        final String dayOfWeek;
        if (dayOfWeekNumber < 1 || dayOfWeekNumber > 7) {
            dayOfWeek = "?";
        } else {
            final String dayOfWeekBeforeCase;
            if (abbreviated) {
                dayOfWeekBeforeCase = DAY_OF_WEEK_ABBREVIATED_NAMES.get(DayOfWeek.of(dayOfWeekNumber));
            } else {
                dayOfWeekBeforeCase = DAY_OF_WEEK_FULL_NAMES.get(DayOfWeek.of(dayOfWeekNumber));
            }

            if (options.isUpper() || options.isChCase()) {
                dayOfWeek = dayOfWeekBeforeCase.toUpperCase(Locale.ROOT);
            } else {
                dayOfWeek = dayOfWeekBeforeCase;
            }
        }
        this.fillPadding(builder, options, ' ', 0, dayOfWeek.length());
        builder.append(dayOfWeek);
    }

    private void appendMonthOfYearName(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final boolean abbreviated) {
        final int monthNumber = this.temporal.get(ChronoField.MONTH_OF_YEAR);
        final String month;
        if (monthNumber < 1 || monthNumber > 12) {
            month = "?";
        } else {
            final String monthBeforeCase;
            if (abbreviated) {
                monthBeforeCase = MONTH_ABBREVIATED_NAMES.get(Month.of(monthNumber));
            } else {
                monthBeforeCase = MONTH_FULL_NAMES.get(Month.of(monthNumber));
            }

            if (options.isUpper() || options.isChCase()) {
                month = monthBeforeCase.toUpperCase(Locale.ROOT);
            } else {
                month = monthBeforeCase;
            }
        }
        this.fillPadding(builder, options, ' ', 0, month.length());
        builder.append(month);
    }

    private void appendCentury(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendLongFormatted(builder, this.temporal.getLong(ChronoField.YEAR) / 100L, options, '0', 2);
    }

    private void appendDayOfMonth(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final char defaultPadding) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.DAY_OF_MONTH), options, defaultPadding, 2);
    }

    private void appendWeekBasedYear(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final boolean withCentury) {
        final int iso8601WeekNumber = calculateIso8601WeekNumber(this.temporal);

        final long year;
        if (this.temporal.get(ChronoField.MONTH_OF_YEAR) == 12 && iso8601WeekNumber == 1) {
            year = this.temporal.getLong(ChronoField.YEAR) + 1L;
        } else if (this.temporal.get(ChronoField.MONTH_OF_YEAR) == 1 && iso8601WeekNumber >= 52) {
            year = this.temporal.getLong(ChronoField.YEAR) - 1L;
        } else {
            year = this.temporal.getLong(ChronoField.YEAR);
        }

        if (withCentury) {
            this.appendLongFormatted(builder, year, options, '0', (0L <= year ? 4 : 5));
        } else {
            this.appendLongFormatted(builder, year / 100L, options, '0', 2);
        }
    }

    private void appendHourOfDay(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final char defaultPadding) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.HOUR_OF_DAY), options, defaultPadding, 2);
    }

    private void appendHourOfAmPm(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final char defaultPadding) {
        final int hourOfDay = this.temporal.get(ChronoField.HOUR_OF_DAY);
        if (hourOfDay == 0) {
            this.appendIntegerFormatted(builder, 12, options, defaultPadding, 2);
        } else if (hourOfDay > 12) {
            this.appendIntegerFormatted(builder, hourOfDay - 12, options, defaultPadding, 2);
        } else {
            this.appendIntegerFormatted(builder, hourOfDay, options, defaultPadding, 2);
        }
    }

    private void appendDayOfYear(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.DAY_OF_YEAR), options, '0', 3);
    }

    private void appendSubsecond(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final int defaultPrecision) {
        final int nanoOfSecond = this.temporal.get(ChronoField.NANO_OF_SECOND);
        final int precision = options.getPrecision(defaultPrecision);
        if (9 < precision) {
            builder.append(String.format(Locale.ROOT, "%09d", nanoOfSecond));
            builder.append(String.format(Locale.ROOT, "%0" + (precision - 9) + "d", 0));
        } else {
            builder.append(String.format(Locale.ROOT, "%0" + precision + "d", nanoOfSecond / POW10[9 - precision]));
        }
    }

    private void appendMinuteOfHour(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.MINUTE_OF_HOUR), options, '0', 2);
    }

    private void appendMonthOfYear(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.MONTH_OF_YEAR), options, '0', 2);
    }

    private void appendAmPmOfDay(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final boolean isUpperCase) {
        this.fillPadding(builder, options, ' ', 0, 2);

        final boolean lower;
        if ((isUpperCase && options.isChCase()) || ((!isUpperCase) && !(options.isChCase() || options.isUpper()))) {
            lower = true;
        } else {
            lower = false;
        }

        final int hourOfDay = this.temporal.get(ChronoField.HOUR_OF_DAY);
        if (hourOfDay < 12) {
            builder.append(lower ? "am" : "AM");
        } else {
            builder.append(lower ? "pm" : "PM");
        }
    }

    private void appendSecondOfMinute(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.SECOND_OF_MINUTE), options, '0', 2);
    }

    private void appendSecondsSinceEpoch(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendLongFormatted(builder, this.temporal.getLong(ChronoField.INSTANT_SECONDS), options, '0', 0);
    }

    private void appendWeekOfYear(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final DayOfWeek starting) {
        this.appendIntegerFormatted(builder, calculateWeekNumber(this.temporal, starting), options, '0', 2);
    }

    private void appendDayOfWeek(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final DayOfWeek starting) {
        if (starting == DayOfWeek.MONDAY) {
            this.appendIntegerFormatted(builder, this.temporal.get(ChronoField.DAY_OF_WEEK), options, '0', 1);
        } else {  // SUNDAY
            this.appendIntegerFormatted(builder, getDayOfWeekInRuby(this.temporal), options, '0', 1);
        }
    }

    private void appendWeekOfWeekBasedYear(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        this.appendIntegerFormatted(builder, calculateIso8601WeekNumber(this.temporal), options, '0', 2);
    }

    private void appendYearWithCentury(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        final long year = this.temporal.getLong(ChronoField.YEAR);
        this.appendLongFormatted(builder, year, options, '0', (0L <= year ? 4 : 5));
    }

    private void appendYearWithoutCentury(
            final StringBuilder builder,
            final FormatDirectiveOptions options) {
        final long year = this.temporal.getLong(ChronoField.YEAR);
        this.appendIntegerFormatted(builder, (int) (year % 100L), options, '0', 2);
    }

    /**
     * Appends a time zone name for {@code "%Z"}.
     *
     * <h3>{@code zoneNameStyle}: {@code NONE}</h3>
     *
     * <p>If {@code zoneNameStyle} is {@code NONE}, {@code "%Z"} outputs only {@code "UTC"} only
     * when the offset is +00:00. It outputs {@code ""} otherwise. It follows the basic behavior
     * of Ruby's {@code Time.strftime} like below:
     *
     * <ol>
     * <li>MRI's {@code Time.zone} (the name of the time zone) cannot be set arbitrary.
     * It is set almost only from {@code localtime(3)} from the computer's environment,
     * such as the TZ environment variable.
     * @see <a href="https://twitter.com/shyouhei/status/1245616200874209283">Tweet from @shyouhei (in Japanese)</a>
     * @see <a href="https://twitter.com/nalsh/status/1245689453315670022">Tweet from @nalsh (in Japanese)</a>
     * <li>MRI's {@code Time#strptime} never sets the name of the time zone from its input.
     * The name of the time zone is set only from {@code Time.utc} or {@code Time.local}.
     * @see <a href="https://docs.ruby-lang.org/en/2.7.0/Time.html#method-c-local">Time.local</a>
     * @see <a href="https://docs.ruby-lang.org/en/2.7.0/Time.html#method-c-utc">Time.utc</a>
     * <li>Embulk uses only {@link java.time.Instant} as its internal timestamp representation.
     * </ol>
     *
     * <p>Also, JRuby (9.1.15.0), which Embulk has used for a long time, behaves like below :
     *
     * <pre>{@code
     * $ env TZ=UTC irb  (JRuby 9.1.15.0)
     * irb(main):001:0> require 'time'
     * => true
     *
     * irb(main):002:0> a = Time.strptime("2020-04-01T12:34:56 JST", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 +0000
     * irb(main):003:0> a.zone
     * => "UTC"
     * irb(main):004:0> a.strftime("%Z")
     * => "UTC"
     *
     * irb(main):005:0> b = Time.strptime("2020-04-01T12:34:56 PST", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 -0800
     * irb(main):006:0> b.zone
     * => ""
     * irb(main):007:0> b.strftime("%Z")
     * => ""
     *
     * irb(main):008:0> c = Time.strptime("2020-04-01T12:34:56 UTC", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 UTC
     * irb(main):009:0> c.zone
     * => "UTC"
     * irb(main):010:0> c.strftime("%Z")
     * => "UTC"
     *
     * irb(main):011:0> d = Time.strptime("2020-04-01T12:34:56 GMT", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 +0000
     * irb(main):012:0> d.zone
     * => "UTC"
     * irb(main):013:0> d.strftime("%Z")
     * => "UTC"
     *
     * irb(main):014:0> e = Time.strptime("2020-04-01T12:34:56 +00:00", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 +0000
     * irb(main):015:0> e.strftime("%Z")
     * => "UTC"
     * irb(main):016:0> e.zone
     * => "UTC"
     *
     * irb(main):017:0> f = Time.strptime("2020-04-01T12:34:56 +07:00", "%Y-%m-%dT%H:%M:%S %Z")
     * => 2020-04-01 12:34:56 +0700
     * irb(main):018:0> f.strftime("%Z")
     * => ""
     * irb(main):019:0> f.zone
     * => ""
     * }</pre>
     *
     * <h3>{@code zoneNameStyle}: {@code SHORT}</h3>
     *
     * <p>On the other hand, Embulk's legacy {@code TimestampFormatter} has used {@code org.jruby.util.RubyDateFormat}
     * directly. Unlike just {@code Time.strptime}, {@code RubyDateFormat} formats {@code "%Z"} into short names.
     * @see <a href="https://github.com/jruby/jruby/blob/9.1.15.0/core/src/main/java/org/jruby/util/RubyDateFormat.java#L411-L419">RubyDateFormat#compilePattern</a>
     * @see <a href="https://github.com/jruby/jruby/blob/9.1.15.0/core/src/main/java/org/jruby/util/RubyDateFormat.java#L622-L624">RubyDateFormat#format</a>
     *
     * To emulate this behavior, when {@code zoneNameStyle} is {@code SHORT}, {@code "%Z"} is formatted into short
     * names with {@link java.util.TimeZone#getDisplayName(boolean, int, java.util.Locale)}.
     */
    private void appendTimeZoneName(
            final StringBuilder builder,
            final FormatDirectiveOptions options,
            final RubyDateTimeFormatter.ZoneNameStyle zoneNameStyle) {
        final int offset = this.temporal.get(ChronoField.OFFSET_SECONDS);
        final Optional<ZoneId> zoneId = getZoneId(this.temporal);

        switch (zoneNameStyle) {
            case NONE:
                if (offset == 0) {
                    this.fillPadding(builder, options, ' ', 0, 3);
                    if (options.isChCase()) {
                        builder.append("utc");
                    } else {
                        builder.append("UTC");
                    }
                } else {
                    this.fillPadding(builder, options, ' ', 0, 0);
                    builder.append("");
                }
                break;
            case SHORT:
                if (zoneId.isPresent() && zoneId.get() instanceof ZoneOffset) {
                    final String shortName;
                    if (((ZoneOffset) (zoneId.get())).getTotalSeconds() == 0) {
                        shortName = "UTC";
                    } else {
                        shortName = zoneId.get().getDisplayName(TextStyle.SHORT, Locale.ROOT);
                    }
                    this.fillPadding(builder, options, ' ', 0, shortName.length());
                    builder.append(shortName);
                } else if (zoneId.isPresent()) {
                    // Trying to emulate short zone names in JRuby / Joda-Time with older java.util.TimeZone.
                    // It does almost the same job with org.jruby.util.RubyDateFormat#format.
                    final TimeZone legacyTimeZone = TimeZone.getTimeZone(zoneId.get());
                    final boolean inDaylightTime = isInDaylightTime(legacyTimeZone, this.temporal);
                    final String shortName = legacyTimeZone.getDisplayName(inDaylightTime, TimeZone.SHORT, Locale.ROOT);
                    this.fillPadding(builder, options, ' ', 0, shortName.length());
                    builder.append(shortName);
                } else {
                    this.fillPadding(builder, options, ' ', 0, 0);
                    builder.append("");
                }
                break;
        }
    }

    private void appendTimeOffset(
            final StringBuilder builder,
            final FormatToken token,
            final FormatDirectiveOptions options) {
        final int offsetSigned = this.temporal.get(ChronoField.OFFSET_SECONDS);
        final char padding = options.getPadding('0');
        final int offset;
        final int sign;
        if (offsetSigned < 0) {
            offset = -offsetSigned;
            sign = -1;
        } else if (offsetSigned > 0) {
            offset = offsetSigned;
            sign = +1;
        } else {
            offset = 0;
            sign = 0;
        }

        final int colons = options.getColons();
        final int precisionSpecified = options.getPrecision(0);
        final int precision;

        switch (colons) {
            case 0:  // %z -> +hhmm
                precision = precisionSpecified <= 5 ? 2 : (precisionSpecified - 3);
                break;

            case 1:  // %:z -> +hh:mm
                precision = precisionSpecified <= 6 ? 2 : (precisionSpecified - 4);
                break;

            case 2:  // %::z -> +hh:mm:ss
                precision = precisionSpecified <= 9 ? 2 : (precisionSpecified - 7);
                break;

            case 3:  // %:::z -> +hh[:mm[:ss]]
                if (offset % 3600 == 0) {
                    precision = precisionSpecified <= 3 ? 2 : (precisionSpecified - 1);
                } else if (offset % 60 == 0) {
                    precision = precisionSpecified <= 6 ? 2 : (precisionSpecified - 4);
                } else {
                    precision = precisionSpecified <= 9 ? 2 : (precisionSpecified - 7);
                }
                break;

            default:
                // %::::z (or more colons) is not supported even in formatter.
                builder.append(token.getImmediate().orElse(""));
                return;
        }

        final int hour = offset / 3600;
        if (padding == ' ') {
            final String hourInString = (sign < 0 ? "-" : "+") + Integer.toString(hour);
            for (int i = 0; i < precision + 1 - hourInString.length(); ++i) {
                builder.append(padding);
            }
            builder.append(hourInString);
        } else {  // padding == '0'
            builder.append(sign < 0 ? "-" : "+");
            builder.append(String.format(Locale.ROOT, "%0" + precision + "d", hour));
        }

        final int minuteInSeconds = offset % 3600;
        if (colons == 3 && minuteInSeconds == 0) {
            return;
        }
        if (1 <= colons) {
            builder.append(":");
        }
        builder.append(String.format(Locale.ROOT, "%02d", minuteInSeconds / 60));

        final int second = minuteInSeconds % 60;

        if (colons == 3 && second == 0) {
            return;
        }
        if (2 <= colons) {
            builder.append(":");
            builder.append(String.format(Locale.ROOT, "%02d", second));
        }
    }

    private void appendIntegerFormatted(
            final StringBuilder builder,
            final int integer,
            final FormatDirectiveOptions options,
            final char defaultPadding,
            final int defaultPrecision) {
        final char padding = options.getPadding(defaultPadding);

        if (padding == '0' && integer < 0) {
            final String integerInString = Integer.toString(-integer);
            builder.append("-");
            this.fillPadding(builder, options, defaultPadding, defaultPrecision, integerInString.length() + 1);
            builder.append(integerInString);
        } else {
            final String integerInString = Integer.toString(integer);
            this.fillPadding(builder, options, defaultPadding, defaultPrecision, integerInString.length());
            builder.append(integerInString);
        }
    }

    private void appendLongFormatted(
            final StringBuilder builder,
            final long integer,
            final FormatDirectiveOptions options,
            final char defaultPadding,
            final int defaultPrecision) {
        final char padding = options.getPadding(defaultPadding);

        if (padding == '0' && integer < 0) {
            final String integerInString = Long.toString(-integer);
            builder.append("-");
            this.fillPadding(builder, options, defaultPadding, defaultPrecision, integerInString.length() + 1);
            builder.append(integerInString);
        } else {
            final String integerInString = Long.toString(integer);
            this.fillPadding(builder, options, defaultPadding, defaultPrecision, integerInString.length());
            builder.append(integerInString);
        }
    }


    /**
     * Calculates a "week number" according to ISO 8601.
     *
     * <p>This method is reimplemented based on iso8601wknum from Ruby v2.6.3's strftime.c.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/strftime.c#L985-L1092">iso8601wknum</a>
     */
    private static int calculateIso8601WeekNumber(final TemporalAccessor temporal) {
        final int jan1DayOfWeek = calculateJan1DayOfWeek(temporal);

        final int weekNumber;
        switch (jan1DayOfWeek) {
            case 1:  // Monday
                weekNumber = calculateWeekNumber(temporal, DayOfWeek.MONDAY);
                break;
            case 2:  // Tuesday
            case 3:  // Wednesday
            case 4:  // Thursday
                weekNumber = calculateWeekNumber(temporal, DayOfWeek.MONDAY) + 1;
                break;
            case 5:  // Friday
            case 6:  // Saturday
            case 0:  // Sunday
                {
                    final int temporaryWeekNumber = calculateWeekNumber(temporal, DayOfWeek.MONDAY);
                    if (temporaryWeekNumber == 0) {
                        final LocalDate dec31LastYear = LocalDate.of(temporal.get(ChronoField.YEAR) - 1, 12, 31);
                        if (getDayOfWeekInRuby(dec31LastYear) != ((jan1DayOfWeek == 0) ? 6 : jan1DayOfWeek - 1)) {
                            throw new DateTimeException(
                                    "Unexpected: wday of Dec 31, " + temporal.get(ChronoField.YEAR) + " is "
                                    + getDayOfWeekInRuby(dec31LastYear) + ", not "
                                    + ((jan1DayOfWeek == 0) ? 6 : jan1DayOfWeek - 1) + ".");
                        }
                        if ((dec31LastYear.get(ChronoField.DAY_OF_YEAR) - 1) != (dec31LastYear.isLeapYear() ? 365 : 364)) {
                            throw new DateTimeException(
                                    "Unexpected: yday of Dec 31, " + temporal.get(ChronoField.YEAR) + " is "
                                    + (dec31LastYear.get(ChronoField.DAY_OF_YEAR) - 1) + ", not "
                                    + (dec31LastYear.isLeapYear() ? 365 : 364) + ".");
                        }
                        weekNumber = calculateIso8601WeekNumber(dec31LastYear);
                    } else {
                        weekNumber = temporaryWeekNumber;
                    }
                }
                break;
            default:
                throw new DateTimeException("Unexpected wday: " + jan1DayOfWeek);
        }

        if (temporal.get(ChronoField.MONTH_OF_YEAR) == 12) {
            final int wday = getDayOfWeekInRuby(temporal);
            final int mday = temporal.get(ChronoField.DAY_OF_MONTH);
            if ((wday == 1 && (mday >= 29 && mday <= 31))
                    || (wday == 2 && (mday == 30 || mday == 31))
                    || (wday == 3 &&  mday == 31)) {
                return 1;
            }
        }

        return weekNumber;
    }

    /**
     * Calculates a "week number".
     *
     * <p>This method is reimplemented based on weeknumber from Ruby v2.6.3's strftime.c.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_3/strftime.c#L1104-L1124">weeknumber</a>
     */
    private static int calculateWeekNumber(final TemporalAccessor temporal, final DayOfWeek firstDayOfWeek) {
        final int dayOfWeekNumberInRuby = getDayOfWeekInRuby(temporal);

        final int dayOfWeekNumberAdjustedInRuby;
        if (firstDayOfWeek == DayOfWeek.MONDAY) {
            if (dayOfWeekNumberInRuby == 0) {  // Sunday
                dayOfWeekNumberAdjustedInRuby = 6;  // Saturday
            } else {
                dayOfWeekNumberAdjustedInRuby = dayOfWeekNumberInRuby - 1;
            }
        } else {
            dayOfWeekNumberAdjustedInRuby = dayOfWeekNumberInRuby;
        }

        final int dayOfYearInRuby = temporal.get(ChronoField.DAY_OF_YEAR) - 1;

        final int result = (dayOfYearInRuby + 7 - dayOfWeekNumberAdjustedInRuby) / 7;

        if (result < 0) {
            // Must not happen?
            return 0;
        }
        return result;
    }

    private static int calculateJan1DayOfWeek(final TemporalAccessor temporal) {
        final int jan1DayOfWeek = getDayOfWeekInRuby(temporal) - ((temporal.get(ChronoField.DAY_OF_YEAR) - 1) % 7);
        if (jan1DayOfWeek < 0) {
            return jan1DayOfWeek + 7;
        }
        return jan1DayOfWeek;
    }

    private static int getDayOfWeekInRuby(final TemporalAccessor temporal) {
        final int dayOfWeekNumberInJava = temporal.get(ChronoField.DAY_OF_WEEK);
        if (dayOfWeekNumberInJava < 1 || dayOfWeekNumberInJava > 7) {
            throw new DateTimeException("Illegal day of week.");
        }

        if (dayOfWeekNumberInJava == 7) {
            return 0;
        } else {
            return dayOfWeekNumberInJava;
        }
    }

    private static Optional<ZoneId> getZoneId(final TemporalAccessor temporal) {
        try {
            final ZoneId zoneId = temporal.query(TemporalQueries.zoneId());
            if (zoneId != null) {
                return Optional.of(zoneId.normalized());
            }
        } catch (final DateTimeException ex) {
            // Pass-through.
        }

        try {
            final int offset = temporal.get(ChronoField.OFFSET_SECONDS);
            return Optional.of(ZoneOffset.ofTotalSeconds(offset));
        } catch (final DateTimeException ex) {
            return Optional.empty();
        }
    }

    private static boolean isInDaylightTime(final TimeZone legacyTimeZone, final TemporalAccessor temporalAsOf) {
        final Instant instant;
        try {
            instant = Instant.from(temporalAsOf);
        } catch (final DateTimeException ex) {
            return false;
        }

        return legacyTimeZone.inDaylightTime(Date.from(instant));
    }

    static {
        final EnumMap<DayOfWeek, String> weekdayFullNames = new EnumMap<>(DayOfWeek.class);
        weekdayFullNames.put(DayOfWeek.SUNDAY, "Sunday");
        weekdayFullNames.put(DayOfWeek.MONDAY, "Monday");
        weekdayFullNames.put(DayOfWeek.TUESDAY, "Tuesday");
        weekdayFullNames.put(DayOfWeek.WEDNESDAY, "Wednesday");
        weekdayFullNames.put(DayOfWeek.THURSDAY, "Thursday");
        weekdayFullNames.put(DayOfWeek.FRIDAY, "Friday");
        weekdayFullNames.put(DayOfWeek.SATURDAY, "Saturday");
        DAY_OF_WEEK_FULL_NAMES = Collections.unmodifiableMap(weekdayFullNames);

        final EnumMap<DayOfWeek, String> weekdayAbbreviatedNames = new EnumMap<>(DayOfWeek.class);
        weekdayAbbreviatedNames.put(DayOfWeek.SUNDAY, "Sun");
        weekdayAbbreviatedNames.put(DayOfWeek.MONDAY, "Mon");
        weekdayAbbreviatedNames.put(DayOfWeek.TUESDAY, "Tue");
        weekdayAbbreviatedNames.put(DayOfWeek.WEDNESDAY, "Wed");
        weekdayAbbreviatedNames.put(DayOfWeek.THURSDAY, "Thu");
        weekdayAbbreviatedNames.put(DayOfWeek.FRIDAY, "Fri");
        weekdayAbbreviatedNames.put(DayOfWeek.SATURDAY, "Sat");
        DAY_OF_WEEK_ABBREVIATED_NAMES = Collections.unmodifiableMap(weekdayAbbreviatedNames);

        final EnumMap<Month, String> monthFullNames = new EnumMap<>(Month.class);
        monthFullNames.put(Month.JANUARY, "January");
        monthFullNames.put(Month.FEBRUARY, "February");
        monthFullNames.put(Month.MARCH, "March");
        monthFullNames.put(Month.APRIL, "April");
        monthFullNames.put(Month.MAY, "May");
        monthFullNames.put(Month.JUNE, "June");
        monthFullNames.put(Month.JULY, "July");
        monthFullNames.put(Month.AUGUST, "August");
        monthFullNames.put(Month.SEPTEMBER, "September");
        monthFullNames.put(Month.OCTOBER, "October");
        monthFullNames.put(Month.NOVEMBER, "November");
        monthFullNames.put(Month.DECEMBER, "December");
        MONTH_FULL_NAMES = Collections.unmodifiableMap(monthFullNames);

        final EnumMap<Month, String> monthAbbreviatedNames = new EnumMap<>(Month.class);
        monthAbbreviatedNames.put(Month.JANUARY, "Jan");
        monthAbbreviatedNames.put(Month.FEBRUARY, "Feb");
        monthAbbreviatedNames.put(Month.MARCH, "Mar");
        monthAbbreviatedNames.put(Month.APRIL, "Apr");
        monthAbbreviatedNames.put(Month.MAY, "May");
        monthAbbreviatedNames.put(Month.JUNE, "Jun");
        monthAbbreviatedNames.put(Month.JULY, "Jul");
        monthAbbreviatedNames.put(Month.AUGUST, "Aug");
        monthAbbreviatedNames.put(Month.SEPTEMBER, "Sep");
        monthAbbreviatedNames.put(Month.OCTOBER, "Oct");
        monthAbbreviatedNames.put(Month.NOVEMBER, "Nov");
        monthAbbreviatedNames.put(Month.DECEMBER, "Dec");
        MONTH_ABBREVIATED_NAMES = Collections.unmodifiableMap(monthAbbreviatedNames);
    }

    private static final FormatDirectiveOptions DEFAULT = FormatDirectiveOptions.EMPTY;

    private static final int[] POW10 = {
        1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000
    };

    private static final Map<DayOfWeek, String> DAY_OF_WEEK_FULL_NAMES;
    private static final Map<DayOfWeek, String> DAY_OF_WEEK_ABBREVIATED_NAMES;
    private static final Map<Month, String> MONTH_FULL_NAMES;
    private static final Map<Month, String> MONTH_ABBREVIATED_NAMES;

    private final TemporalAccessor temporal;
}

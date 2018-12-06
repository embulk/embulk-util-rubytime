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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains date/time information parsed from a String.
 *
 * <p>It stores "as-is" parsed date/time information like Ruby's {@code Date._strptime}. In other words,
 * It does not "resolve" nor "compliment" any unspecified field from other specified fields or default values.
 *
 * @see <a href="http://ruby-doc.org/stdlib-2.5.0/libdoc/date/rdoc/Date.html#method-c-_strptime">Date._strptime</a>
 */
final class Parsed implements TemporalAccessor {
    private Parsed(
            final String originalString,

            final int dayOfMonth,
            final int weekBasedYear,
            final int hour,
            final int dayOfYear,
            final int nanoOfSecond,
            final int minuteOfHour,
            final int monthOfYear,
            final long secondsSinceEpoch,
            final int nanoOfSecondsSinceEpoch,
            final int secondOfMinute,
            final int weekOfYearStartingWithSunday,
            final int weekOfYearStartingWithMonday,
            final int dayOfWeekStartingWithMonday1,
            final int weekOfWeekBasedYear,
            final int dayOfWeekStartingWithSunday0,
            final int year,

            final String timeZoneName,

            final String leftover,

            final Period parsedExcessDays,
            final boolean parsedLeapSecond) {
        this.originalString = originalString;

        this.chronoFieldValues = new EnumMap<>(ChronoField.class);
        this.rubyChronoFieldValues = new EnumMap<>(RubyChronoFields.Field.class);

        if (dayOfMonth > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.DAY_OF_MONTH, (long) dayOfMonth);
        }
        if (weekBasedYear > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.WEEK_BASED_YEAR, (long) weekBasedYear);
        }
        if (hour > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.HOUR_OF_DAY, (long) hour);
        }
        if (dayOfYear > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.DAY_OF_YEAR, (long) dayOfYear);
        }
        if (nanoOfSecond > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.NANO_OF_SECOND, (long) nanoOfSecond);
        }
        if (minuteOfHour > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.MINUTE_OF_HOUR, (long) minuteOfHour);
        }
        if (monthOfYear > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.MONTH_OF_YEAR, (long) monthOfYear);
        }
        if (secondsSinceEpoch > Long.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.INSTANT_SECONDS, secondsSinceEpoch);
        }
        if (nanoOfSecondsSinceEpoch > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.NANO_OF_INSTANT_SECONDS, (long) nanoOfSecondsSinceEpoch);
        }
        if (secondOfMinute > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.SECOND_OF_MINUTE, (long) secondOfMinute);
        }
        if (weekOfYearStartingWithSunday > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.WEEK_OF_YEAR_STARTING_WITH_SUNDAY,
                                           (long) weekOfYearStartingWithSunday);
        }
        if (weekOfYearStartingWithMonday > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.WEEK_OF_YEAR_STARTING_WITH_MONDAY,
                                           (long) weekOfYearStartingWithMonday);
        }
        if (dayOfWeekStartingWithMonday1 > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.DAY_OF_WEEK_STARTING_WITH_MONDAY_1,
                                           (long) dayOfWeekStartingWithMonday1);
        }
        if (weekOfWeekBasedYear > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.WEEK_OF_WEEK_BASED_YEAR,
                                           (long) weekOfWeekBasedYear);
        }
        if (dayOfWeekStartingWithSunday0 > Integer.MIN_VALUE) {
            this.rubyChronoFieldValues.put(RubyChronoFields.Field.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0,
                                           (long) dayOfWeekStartingWithSunday0);
        }
        if (year > Integer.MIN_VALUE) {
            this.chronoFieldValues.put(ChronoField.YEAR, (long) year);  // Not YEAR_OF_ERA.
        }

        // TODO: Parsed should contain "offset" by itself to be consistent with Date._strptime.
        // This is now done in ParsedElementsQuery to create a Map compatible with Date._strptime.
        this.timeZoneName = timeZoneName;

        this.leftover = leftover;

        this.parsedExcessDays = parsedExcessDays;
        this.parsedLeapSecond = parsedLeapSecond;
    }

    static class Builder {
        Builder(final String originalString) {
            this.originalString = originalString;

            this.century = Integer.MIN_VALUE;
            this.dayOfMonth = Integer.MIN_VALUE;
            this.weekBasedYear = Integer.MIN_VALUE;
            this.hour = Integer.MIN_VALUE;
            this.dayOfYear = Integer.MIN_VALUE;
            this.nanoOfSecond = Integer.MIN_VALUE;
            this.minuteOfHour = Integer.MIN_VALUE;
            this.monthOfYear = Integer.MIN_VALUE;
            this.ampmOfDay = Integer.MIN_VALUE;
            this.secondsSinceEpoch = Long.MIN_VALUE;
            this.nanoOfSecondsSinceEpoch = Integer.MIN_VALUE;
            this.secondOfMinute = Integer.MIN_VALUE;
            this.weekOfYearStartingWithSunday = Integer.MIN_VALUE;
            this.weekOfYearStartingWithMonday = Integer.MIN_VALUE;
            this.dayOfWeekStartingWithMonday1 = Integer.MIN_VALUE;
            this.weekOfWeekBasedYear = Integer.MIN_VALUE;
            this.dayOfWeekStartingWithSunday0 = Integer.MIN_VALUE;
            this.year = Integer.MIN_VALUE;

            this.timeZoneName = null;

            this.leftover = null;

            this.fail = false;
        }

        Parsed build() {
            // Merge week-based year and century as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l676
            final int weekBasedYearWithCentury;
            if (this.century != Integer.MIN_VALUE && this.weekBasedYear != Integer.MIN_VALUE) {
                // It is the right behavior in Ruby.
                // Date._strptime('13 1234', '%C %G') => {:cwyear=>2534}
                weekBasedYearWithCentury = this.century * 100 + this.weekBasedYear;
            } else {
                weekBasedYearWithCentury = this.weekBasedYear;
            }

            // Merge year and century as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l679
            final int yearWithCentury;
            if (this.century != Integer.MIN_VALUE && this.year != Integer.MIN_VALUE) {
                // It is the right behavior in Ruby.
                // Date._strptime('13 1234', '%C %Y') => {:year=>2534}
                yearWithCentury = this.century * 100 + this.year;
            } else {
                yearWithCentury = this.year;
            }

            // Merge hour and ampmOfDay as MRI (Matz' Ruby Implementation) does before generating a hash.
            // See: https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l685
            final int hourWithAmPm;
            final Period parsedExcessDays;
            if (this.hour != Integer.MIN_VALUE && this.ampmOfDay != Integer.MIN_VALUE) {
                hourWithAmPm = (this.hour % 12) + this.ampmOfDay;
                parsedExcessDays = null;
            } else if (this.hour == 24) {
                hourWithAmPm = 0;
                parsedExcessDays = Period.ofDays(1);;
            } else {
                hourWithAmPm = this.hour;
                parsedExcessDays = null;
            }

            return new Parsed(
                    this.originalString,

                    this.dayOfMonth,
                    weekBasedYearWithCentury,
                    hourWithAmPm,
                    this.dayOfYear,
                    this.nanoOfSecond,
                    this.minuteOfHour,
                    this.monthOfYear,
                    this.secondsSinceEpoch,
                    this.nanoOfSecondsSinceEpoch,
                    (this.secondOfMinute == 60 ? 59 : this.secondOfMinute),
                    this.weekOfYearStartingWithSunday,
                    this.weekOfYearStartingWithMonday,
                    this.dayOfWeekStartingWithMonday1,
                    this.weekOfWeekBasedYear,
                    this.dayOfWeekStartingWithSunday0,
                    yearWithCentury,

                    this.timeZoneName,

                    this.leftover,

                    parsedExcessDays,
                    this.secondOfMinute == 60);
        }

        /**
         * Sets day of the week by name.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wday
         * <li> Ruby strptime directive specifier related: %A, %a
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekByName(final String dayOfWeekByName) {
            final Integer dayOfWeek = DAY_OF_WEEK_NAMES.get(dayOfWeekByName);
            if (dayOfWeek == null) {
                fail = true;
            } else {
                this.dayOfWeekStartingWithSunday0 = dayOfWeek;
            }
            return this;
        }

        /**
         * Sets month of the year by name.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mon
         * <li> Ruby strptime directive specifier related: %B, %b, %h
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setMonthOfYearByName(final String monthOfYearByName) {
            final Integer monthOfYear = MONTH_OF_YEAR_NAMES.get(monthOfYearByName);
            if (monthOfYear == null) {
                fail = true;
            } else {
                this.monthOfYear = monthOfYear;
            }
            return this;
        }

        /**
         * Sets century.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: _cent
         * <li> Ruby strptime directive specifier related: %C
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setCentury(final int century) {
            this.century = century;
            return this;
        }

        /**
         * Sets day of the month.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mday
         * <li> Ruby strptime directive specifier related: %d, %e
         * <li> java.time.temporal: ChronoField.DAY_OF_MONTH
         * </ul>
         */
        Builder setDayOfMonth(final int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }

        /**
         * Sets week-based year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwyear
         * <li> Ruby strptime directive specifier related: %G
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setWeekBasedYear(final int weekBasedYear) {
            this.weekBasedYear = weekBasedYear;
            return this;
        }

        /**
         * Sets week-based year without century.
         *
         * If century is not set before by setCentury (%C), it tries to set default century as well.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwyear
         * <li> Ruby strptime directive specifier related: %g
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setWeekBasedYearWithoutCentury(final int weekBasedYearWithoutCentury) {
            this.weekBasedYear = weekBasedYearWithoutCentury;
            if (this.century == Integer.MIN_VALUE) {
                this.century = (weekBasedYearWithoutCentury >= 69 ? 19 : 20);
            }
            return this;
        }

        /**
         * Sets hour.
         *
         * The hour can be either in 12-hour or 24-hour. It is considered 12-hour if setAmPmOfDay (%P/%p) is set.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: hour
         * <li> Ruby strptime directive specifier related: %H, %k, %I, %l
         * <li> java.time.temporal: ChronoField.HOUR_OF_AMPM or ChronoField.HOUR_OF_DAY
         * </ul>
         */
        Builder setHour(final int hour) {
            this.hour = hour;
            return this;
        }

        /**
         * Sets day of the year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: yday
         * <li> Ruby strptime directive specifier related: %j
         * <li> java.time.temporal: ChronoField.DAY_OF_YEAR
         * </ul>
         */
        Builder setDayOfYear(final int dayOfYear) {
            this.dayOfYear = dayOfYear;
            return this;
        }

        /**
         * Sets fractional part of the second.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: sec_fraction
         * <li> Ruby strptime directive specifier related: %L, %N
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setNanoOfSecond(final int nanoOfSecond) {
            this.nanoOfSecond = nanoOfSecond;
            return this;
        }

        /**
         * Sets minute of the hour.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: min
         * <li> Ruby strptime directive specifier related: %M
         * <li> java.time.temporal: ChronoField.MINUTE_OF_HOUR
         * </ul>
         */
        Builder setMinuteOfHour(final int minuteOfHour) {
            this.minuteOfHour = minuteOfHour;
            return this;
        }

        /**
         * Sets month of the year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: mon
         * <li> Ruby strptime directive specifier related: %m
         * <li> java.time.temporal: ChronoField.MONTH_OF_YEAR
         * </ul>
         */
        Builder setMonthOfYear(final int monthOfYear) {
            this.monthOfYear = monthOfYear;
            return this;
        }

        /**
         * Sets am/pm of the day.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: _merid
         * <li> Ruby strptime directive specifier related: %P, %p
         * <li> java.time.temporal: ChronoField.AMPM_OF_DAY
         * </ul>
         */
        Builder setAmPmOfDay(final int ampmOfDay) {
            this.ampmOfDay = ampmOfDay;
            return this;
        }

        /**
         * Sets seconds since the epoch.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: seconds
         * <li> Ruby strptime directive specifier related: %s
         * <li> java.time.temporal: ChronoField.INSTANT_SECONDS
         * </ul>
         */
        Builder setSecondsSinceEpoch(final long secondsSinceEpoch) {
            this.secondsSinceEpoch = secondsSinceEpoch;
            this.nanoOfSecondsSinceEpoch = 0;
            return this;
        }

        /**
         * Sets milliseconds since the epoch.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: seconds
         * <li> Ruby strptime directive specifier related: %Q
         * <li> java.time.temporal: ChronoField.INSTANT_SECONDS, RubyChronoFields.MILLI_OF_INSTANT_SECONDS
         * </ul>
         */
        Builder setMillisecondsSinceEpoch(final long millisecondsSinceEpoch) {
            if (millisecondsSinceEpoch >= 0) {
                this.secondsSinceEpoch = millisecondsSinceEpoch / 1000L;
                this.nanoOfSecondsSinceEpoch = ((int) (millisecondsSinceEpoch % 1000L)) * 1000_000;
            } else {
                this.secondsSinceEpoch = millisecondsSinceEpoch / 1000L - 1;
                this.nanoOfSecondsSinceEpoch = ((int) (millisecondsSinceEpoch % 1000L) + 1000) * 1000_000;
            }
            return this;
        }

        /**
         * Sets second of the minute.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: sec
         * <li> Ruby strptime directive specifier related: %S
         * <li> java.time.temporal: ChronoField.SECOND_OF_MINUTE
         * </ul>
         */
        Builder setSecondOfMinute(final int secondOfMinute) {
            this.secondOfMinute = secondOfMinute;
            return this;
        }

        /**
         * Sets week number with weeks starting from Sunday.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wnum0
         * <li> Ruby strptime directive specifier related: %U
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfYearStartingWithSunday(final int weekOfYearStartingWithSunday) {
            this.weekOfYearStartingWithSunday = weekOfYearStartingWithSunday;
            return this;
        }

        /**
         * Sets week number with weeks starting from Monday.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wnum1
         * <li> Ruby strptime directive specifier related: %W
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfYearStartingWithMonday(final int weekOfYearStartingWithMonday) {
            this.weekOfYearStartingWithMonday = weekOfYearStartingWithMonday;
            return this;
        }

        /**
         * Sets day of week starting from Monday with 1.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cwday
         * <li> Ruby strptime directive specifier related: %u
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekStartingWithMonday1(final int dayOfWeekStartingWithMonday1) {
            this.dayOfWeekStartingWithMonday1 = dayOfWeekStartingWithMonday1;
            return this;
        }

        /**
         * Sets week number of week-based year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: cweek
         * <li> Ruby strptime directive specifier related: %V
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setWeekOfWeekBasedYear(final int weekOfWeekBasedYear) {
            this.weekOfWeekBasedYear = weekOfWeekBasedYear;
            return this;
        }

        /**
         * Sets day of week starting from Sunday with 0.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: wday
         * <li> Ruby strptime directive specifier related: %w
         * <li> java.time.temporal:
         * </ul>
         */
        Builder setDayOfWeekStartingWithSunday0(final int dayOfWeekStartingWithSunday0) {
            this.dayOfWeekStartingWithSunday0 = dayOfWeekStartingWithSunday0;
            return this;
        }

        /**
         * Sets year.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: year
         * <li> Ruby strptime directive specifier related: %Y
         * <li> java.time.temporal: ChronoField.YEAR
         * </ul>
         */
        Builder setYear(final int year) {
            this.year = year;
            return this;
        }

        /**
         * Sets year without century.
         *
         * If century is not set before by setCentury (%C), it tries to set default century as well.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: year
         * <li> Ruby strptime directive specifier related: %y
         * <li> java.time.temporal: ChronoField.YEAR
         * </ul>
         */
        Builder setYearWithoutCentury(final int yearWithoutCentury) {
            this.year = yearWithoutCentury;
            if (this.century == Integer.MIN_VALUE) {
                this.century = (yearWithoutCentury >= 69 ? 19 : 20);
            }
            return this;
        }

        /**
         * Sets time offset.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: zone, offset
         * <li> Ruby strptime directive specifier related: %Z, %z
         * <li> java.time.temporal: ChronoField.OFFSET_SECONDS (not exactly the same)
         * </ul>
         */
        Builder setTimeOffset(final String timeZoneName) {
            this.timeZoneName = timeZoneName;
            return this;
        }

        /**
         * Sets leftover.
         *
         * <ul>
         * <li> Ruby Date._strptime hash key corresponding: leftover
         * <li> Ruby strptime directive specifier related: N/A
         * <li> java.time.temporal: N/A
         * </ul>
         */
        Builder setLeftover(final String leftover) {
            this.leftover = leftover;
            return this;
        }

        private static final Map<String, Integer> DAY_OF_WEEK_NAMES;
        private static final Map<String, Integer> MONTH_OF_YEAR_NAMES;

        private static final String[] DAY_OF_WEEK_FULL_NAMES = new String[] {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

        private static final String[] DAY_OF_WEEK_ABBREVIATED_NAMES = new String[] {
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        };

        private static final String[] MONTH_OF_YEAR_FULL_NAMES = new String[] {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        private static final String[] MONTH_OF_YEAR_ABBREVIATED_NAMES = new String[] {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        static {
            final HashMap<String, Integer> dayOfWeekNamesBuilt = new HashMap<>();
            for (int i = 0; i < DAY_OF_WEEK_FULL_NAMES.length; ++i) {
                dayOfWeekNamesBuilt.put(DAY_OF_WEEK_FULL_NAMES[i], i);
            }
            for (int i = 0; i < DAY_OF_WEEK_ABBREVIATED_NAMES.length; ++i) {
                dayOfWeekNamesBuilt.put(DAY_OF_WEEK_ABBREVIATED_NAMES[i], i);
            }
            DAY_OF_WEEK_NAMES = Collections.unmodifiableMap(dayOfWeekNamesBuilt);

            final HashMap<String, Integer> monthOfYearNamesBuilt = new HashMap<>();
            for (int i = 0; i < MONTH_OF_YEAR_FULL_NAMES.length; ++i) {
                monthOfYearNamesBuilt.put(MONTH_OF_YEAR_FULL_NAMES[i], i + 1);
            }
            for (int i = 0; i < MONTH_OF_YEAR_ABBREVIATED_NAMES.length; ++i) {
                monthOfYearNamesBuilt.put(MONTH_OF_YEAR_ABBREVIATED_NAMES[i], i + 1);
            }
            MONTH_OF_YEAR_NAMES = Collections.unmodifiableMap(monthOfYearNamesBuilt);
        }

        private final String originalString;

        private int century;
        private int dayOfMonth;
        private int weekBasedYear;
        private int hour;
        private int dayOfYear;
        private int nanoOfSecond;
        private int minuteOfHour;
        private int monthOfYear;
        private int ampmOfDay;
        private long secondsSinceEpoch;
        private int nanoOfSecondsSinceEpoch;
        private int secondOfMinute;
        private int weekOfYearStartingWithSunday;
        private int weekOfYearStartingWithMonday;
        private int dayOfWeekStartingWithMonday1;
        private int weekOfWeekBasedYear;
        private int dayOfWeekStartingWithSunday0;
        private int year;

        private String timeZoneName;

        private String leftover;

        private boolean fail;
    }

    static Builder builder(final String originalString) {
        return new Builder(originalString);
    }

    @Override
    public long getLong(final TemporalField field) {
        if (field instanceof ChronoField) {
            final Long value = this.chronoFieldValues.get(field);
            if (value == null) {
                throw new UnsupportedTemporalTypeException("");
            }
            return (long) value;
        } else if (field instanceof RubyChronoFields.Field) {
            final Long value = this.rubyChronoFieldValues.get(field);
            if (value == null) {
                throw new UnsupportedTemporalTypeException("");
            }
            return (long) value;
        }
        throw new UnsupportedTemporalTypeException("");
    }

    @Override
    public boolean isSupported(final TemporalField field) {
        if (field instanceof ChronoField) {
            return this.chronoFieldValues.containsKey(field);
        } else if (field instanceof RubyChronoFields.Field) {
            return this.rubyChronoFieldValues.containsKey(field);
        }
        throw new UnsupportedTemporalTypeException("");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R query(final TemporalQuery<R> query) {
        if (query == DateTimeFormatter.parsedExcessDays()) {
            return (R) this.parsedExcessDays;
        } else if (query == DateTimeFormatter.parsedLeapSecond()) {
            return (R) ((Boolean) this.parsedLeapSecond);
        } else if (query == RubyTemporalQueries.rubyTimeZone()) {
            return (R) this.timeZoneName;
        } else if (query == RubyTemporalQueries.leftover()) {
            return (R) this.leftover;
        } else {
            return TemporalAccessor.super.query(query);
        }
    }

    @Override
    public ValueRange range(final TemporalField field) {
        if (field instanceof RubyChronoFields.Field && isSupported(field)) {
            return field.range();
        }
        return TemporalAccessor.super.range(field);
    }

    private final String originalString;

    private final EnumMap<ChronoField, Long> chronoFieldValues;
    private final EnumMap<RubyChronoFields.Field, Long> rubyChronoFieldValues;

    private final String timeZoneName;

    private final String leftover;

    private final Period parsedExcessDays;
    private final boolean parsedLeapSecond;
}

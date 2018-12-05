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

import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.Locale;

/**
 * A Ruby-specific set of temporal fields.
 */
public final class RubyChronoField {
    private RubyChronoField() {
        // No instantiation.
    }

    /**
     * The ISO 8601 week-based year.
     */
    public static final TemporalField WEEK_BASED_YEAR = Field.WEEK_BASED_YEAR;

    /**
     * The instant epoch-milliseconds.
     */
    public static final TemporalField INSTANT_MILLIS = Field.INSTANT_MILLIS;

    /**
     * The week number of the year, with the week starting with Sunday (00..53).
     */
    public static final TemporalField WEEK_OF_YEAR_STARTING_WITH_SUNDAY = Field.WEEK_OF_YEAR_STARTING_WITH_SUNDAY;

    /**
     * The week number of the year, with the week starting with Monday (00..53).
     */
    public static final TemporalField WEEK_OF_YEAR_STARTING_WITH_MONDAY = Field.WEEK_OF_YEAR_STARTING_WITH_MONDAY;

    /**
     * The day of the week (Monday is 1, 1..7).
     */
    public static final TemporalField DAY_OF_WEEK_STARTING_WITH_MONDAY_1 = Field.DAY_OF_WEEK_STARTING_WITH_MONDAY_1;

    /**
     * The week number of the ISO 8601 week-based year (01..53).
     */
    public static final TemporalField WEEK_OF_WEEK_BASED_YEAR = Field.WEEK_OF_WEEK_BASED_YEAR;

    /**
     * The day of the week (Sunday is 0, 0..6).
     */
    public static final TemporalField DAY_OF_WEEK_STARTING_WITH_SUNDAY_0 = Field.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0;

    static enum Field implements TemporalField {
        WEEK_BASED_YEAR(
                "WeekBasedYear",
                ChronoUnit.YEARS,
                ChronoUnit.FOREVER,
                ValueRange.of(Year.MIN_VALUE, Year.MAX_VALUE),
                true,
                false),
        INSTANT_MILLIS(
                "InstantMillis",
                ChronoUnit.MILLIS,
                ChronoUnit.FOREVER,
                ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE),
                false,
                true),
        WEEK_OF_YEAR_STARTING_WITH_SUNDAY(
                "WeekOfYearStartingWithSunday",
                ChronoUnit.WEEKS,
                ChronoUnit.YEARS,
                ValueRange.of(0, 53),
                true,
                false),
        WEEK_OF_YEAR_STARTING_WITH_MONDAY(
                "WeekOfYearStartingWithMonday",
                ChronoUnit.WEEKS,
                ChronoUnit.YEARS,
                ValueRange.of(0, 53),
                true,
                false),
        DAY_OF_WEEK_STARTING_WITH_MONDAY_1(
                "DayOfWeekStartingWithMonday1",
                ChronoUnit.DAYS,
                ChronoUnit.WEEKS,
                ValueRange.of(1, 7),
                true,
                false),
        WEEK_OF_WEEK_BASED_YEAR(
                "WeekOfWeekBasedYear",
                ChronoUnit.WEEKS,
                ChronoUnit.YEARS,
                ValueRange.of(1, 53),
                true,
                false),
        DAY_OF_WEEK_STARTING_WITH_SUNDAY_0(
                "DayOfWeekStartingWithSunday0",
                ChronoUnit.DAYS,
                ChronoUnit.WEEKS,
                ValueRange.of(0, 6),
                true,
                false),
        ;

        private Field(
                final String name,
                final TemporalUnit baseUnit,
                final TemporalUnit rangeUnit,
                final ValueRange range,
                final boolean isDateBased,
                final boolean isTimeBased) {
            this.name = name;
            this.baseUnit = baseUnit;
            this.rangeUnit = rangeUnit;
            this.range = range;
            this.isDateBased = isDateBased;
            this.isTimeBased = isTimeBased;
        }

        @Override
        public String getDisplayName(final Locale locale) {
            return this.name;
        }

        @Override
        public TemporalUnit getBaseUnit() {
            return this.baseUnit;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return this.rangeUnit;
        }

        @Override
        public ValueRange range() {
            return this.range;
        }

        @Override
        public boolean isDateBased() {
            return this.isDateBased;
        }

        @Override
        public boolean isTimeBased() {
            return this.isTimeBased;
        }

        public long checkValidValue(final long value) {  // Not from TemporalField.
            return this.range().checkValidValue(value, this);
        }

        public int checkValidIntValue(final long value) {  // Not from TemporalField.
            return this.range().checkValidIntValue(value, this);
        }

        @Override
        public boolean isSupportedBy(final TemporalAccessor temporal) {
            return temporal.isSupported(this);
        }

        @Override
        public ValueRange rangeRefinedBy(final TemporalAccessor temporal) {
            return temporal.range(this);
        }

        @Override
        public long getFrom(final TemporalAccessor temporal) {
            return temporal.getLong(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R adjustInto(final R temporal, final long newValue) {
            return (R) temporal.with(this, newValue);
        }

        @Override
        public String toString() {
            return this.name;
        }

        private final String name;
        private final TemporalUnit baseUnit;
        private final TemporalUnit rangeUnit;
        private final ValueRange range;
        private final boolean isDateBased;
        private final boolean isTimeBased;
    }
}

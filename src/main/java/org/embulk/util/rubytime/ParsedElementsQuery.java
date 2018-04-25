package org.embulk.util.rubytime;

import java.math.BigDecimal;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Queries to provide Maps of parsed elements, which are analogous to Ruby hashes by Date._strptime.
 */
public class ParsedElementsQuery<T> implements TemporalQuery<Map<T, Object>> {
    public static interface FractionConverter {
        Object convertFraction(int seconds, int nanoOfSecond);
    }

    public static interface HashKeyConverter<T> {
        T convertHashKey(String hashKey);
    }

    private ParsedElementsQuery(
            final FractionConverter fractionConverter,
            final HashKeyConverter<T> hashKeyConverter) {
        this.fractionConverter = fractionConverter;
        this.hashKeyConverter = hashKeyConverter;
    }

    public static ParsedElementsQuery<String> withFractionInBigDecimal() {
        return new ParsedElementsQuery<String>(FRACTION_TO_BIG_DECIMAL, STRING_AS_IS);
    }

    public static ParsedElementsQuery<String> of(final FractionConverter fractionConverter) {
        return new ParsedElementsQuery<String>(fractionConverter, STRING_AS_IS);
    }

    public static <U> ParsedElementsQuery<U> of(
            final FractionConverter fractionConverter,
            final HashKeyConverter<U> hashKeyConverter) {
        return new ParsedElementsQuery<U>(fractionConverter, hashKeyConverter);
    }

    @Override
    public Map<T, Object> queryFrom(final TemporalAccessor temporal) {
        final Builder<T> builder = new Builder<T>(temporal, this.fractionConverter, this.hashKeyConverter);

        builder.put("mday", ChronoField.DAY_OF_MONTH);
        builder.put("cwyear", RubyChronoField.WEEK_BASED_YEAR);
        builder.putHourOfDay();
        builder.put("yday", ChronoField.DAY_OF_YEAR);
        builder.putSecFraction();
        builder.put("min", ChronoField.MINUTE_OF_HOUR);
        builder.put("mon", ChronoField.MONTH_OF_YEAR);
        builder.putInstantMillisecond();
        builder.putSecondOfMinute();
        builder.put("wnum0", RubyChronoField.WEEK_OF_YEAR_STARTING_WITH_SUNDAY);
        builder.put("wnum1", RubyChronoField.WEEK_OF_YEAR_STARTING_WITH_MONDAY);
        builder.put("cwday", RubyChronoField.DAY_OF_WEEK_STARTING_WITH_MONDAY_1);
        builder.put("cweek", RubyChronoField.WEEK_OF_WEEK_BASED_YEAR);
        builder.put("wday", RubyChronoField.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0);
        builder.put("year", ChronoField.YEAR);

        builder.putTimeZone();
        builder.putLeftover();

        return builder.build();
    }

    private static class Builder<T> {
        private Builder(
                final TemporalAccessor temporal,
                final FractionConverter fractionConverterInner,
                final HashKeyConverter<T> hashKeyConverter) {
            this.temporal = temporal;
            this.fractionConverterInner = fractionConverterInner;
            this.hashKeyConverter = hashKeyConverter;
            this.built = new HashMap<>();
        }

        private void put(final String key, final TemporalField field) {
            if (this.temporal.isSupported(field)) {
                this.built.put(this.hashKeyConverter.convertHashKey(key), this.temporal.get(field));
            }
        }

        private void putHourOfDay() {
            if (this.temporal.isSupported(ChronoField.HOUR_OF_DAY)) {
                final Period parsedExcessDays = this.temporal.query(DateTimeFormatter.parsedExcessDays());
                if (parsedExcessDays == null || parsedExcessDays.isZero()) {
                    this.built.put(this.hashKeyConverter.convertHashKey("hour"),
                                   this.temporal.get(ChronoField.HOUR_OF_DAY));
                } else if (parsedExcessDays.getDays() == 1
                                   && parsedExcessDays.getMonths() == 0
                                   && parsedExcessDays.getYears() == 0) {
                    this.built.put(this.hashKeyConverter.convertHashKey("hour"), 24);
                }
            }
        }

        private void putSecondOfMinute() {
            if (this.temporal.isSupported(ChronoField.SECOND_OF_MINUTE)) {
                if (this.temporal.query(DateTimeFormatter.parsedLeapSecond())) {
                    this.built.put(this.hashKeyConverter.convertHashKey("sec"), 60);
                } else {
                    this.built.put(this.hashKeyConverter.convertHashKey("sec"),
                                   this.temporal.get(ChronoField.SECOND_OF_MINUTE));
                }
            }
        }

        private void putSecFraction() {
            if (this.temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
                this.built.put(this.hashKeyConverter.convertHashKey("sec_fraction"),
                               this.fractionConverterInner.convertFraction(
                                       0, this.temporal.get(ChronoField.NANO_OF_SECOND)));
            }
        }

        private void putInstantMillisecond() {
            if (this.temporal.isSupported(RubyChronoField.INSTANT_MILLIS)) {
                final long instantMillisecond = this.temporal.getLong(RubyChronoField.INSTANT_MILLIS);
                final int instantSecond = (int) (instantMillisecond / 1000);
                final int nanoOfInstantSecond = (int) (instantMillisecond % 1000) * 1_000_000;
                if (nanoOfInstantSecond == 0) {
                    this.built.put(this.hashKeyConverter.convertHashKey("seconds"), instantSecond);
                } else {
                    this.built.put(this.hashKeyConverter.convertHashKey("seconds"),
                                   this.fractionConverterInner.convertFraction(
                                           instantSecond, nanoOfInstantSecond));
                }
            }
        }

        private void putTimeZone() {
            final String zone = temporal.query(RubyTemporalQueries.rubyTimeZone());
            if (zone != null) {
                final int offset = RubyTimeZoneTab.dateZoneToDiff(zone);
                if (offset != Integer.MIN_VALUE) {
                    this.built.put(this.hashKeyConverter.convertHashKey("offset"), offset);
                }
                this.built.put(this.hashKeyConverter.convertHashKey("zone"), zone);
            }
        }

        private void putLeftover() {
            final String leftover = temporal.query(RubyTemporalQueries.leftover());
            if (leftover != null) {
                this.built.put(this.hashKeyConverter.convertHashKey("leftover"), leftover);
            }
        }

        private Map<T, Object> build() {
            return Collections.unmodifiableMap(this.built);
        }

        private final TemporalAccessor temporal;
        private final FractionConverter fractionConverterInner;
        private final HashKeyConverter<T> hashKeyConverter;
        private final HashMap<T, Object> built;
    }

    private static final FractionToBigDecimalConverter FRACTION_TO_BIG_DECIMAL;
    private static final HashKeyConverter<String> STRING_AS_IS;

    private final static class FractionToBigDecimalConverter implements FractionConverter {
        @Override
        public Object convertFraction(final int seconds, final int nanoOfSecond) {
            return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanoOfSecond, 9));
        }
    }

    private final static class StringAsIs implements HashKeyConverter<String> {
        @Override
        public String convertHashKey(final String hashKey) {
            return hashKey;
        }
    }

    static {
        FRACTION_TO_BIG_DECIMAL = new FractionToBigDecimalConverter();
        STRING_AS_IS = new StringAsIs();
    }

    private final FractionConverter fractionConverter;
    private final HashKeyConverter<T> hashKeyConverter;
}

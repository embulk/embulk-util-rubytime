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
 * A query to retrieve a {@link java.util.Map} of parsed elements.
 *
 * <p>The {@link java.util.Map} of parsed elements is analogous to a hash returned from Ruby's {@code Date._strptime} like below.
 *
 * <pre>{@code
 * {:year=>2001, :mon=>2, :mday=>3}
 * }</pre>
 *
 * @see <a href="https://ruby-doc.org/stdlib-2.5.1/libdoc/date/rdoc/Date.html#method-c-_strptime">Ruby's Date._strptime</a>
 */
public final class RubyDateTimeParsedElementsQuery<T> implements TemporalQuery<Map<T, Object>> {
    /**
     * A converter for a decimal fraction to be stored as the required type in the result {@link java.util.Map}.
     *
     * <p>For example, the following implementation converts a decimal fraction into Ruby's {@code Rational} object in JRuby.
     *
     * <pre>{@code
     * import org.jruby.Ruby;
     * import org.jruby.RubyRational;
     *
     * public class DecimalFractionToRationalConverter implements RubyDateTimeParsedElementsQuery.DecimalFractionConverter {
     *     public DecimalFractionToRationalConverter(final Ruby ruby) {
     *         this.ruby = ruby;
     *     }
     *
     *     public Object convertDecimalFraction(final long integer, final int nano) {
     *         return RubyRational.newRational(this.ruby, ((long) integer * 1_000_000_000L) + (long) nano, 1_000_000_000L);
     *     }
     *
     *     private final Ruby ruby;
     * }
     * }
     * </pre>
     */
    public static interface DecimalFractionConverter {
        /**
         * Converts a decimal fraction, a pair of an integer part and a fraction part, into an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map} of {@link RubyDateTimeParsedElementsQuery}.
         *
         * @param integer  the integer part
         * @param nano  the fraction part in nano
         * @return an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map}, not null
         */
        Object convertDecimalFraction(long integer, int nano);
    }

    /**
     * A converter for keys of the result {@link java.util.Map} from {@link java.lang.String} to the required type.
     *
     * <p>For example, the following implementation converts into Ruby's {@code Symbol} object in JRuby.
     *
     * <pre>{@code
     * import org.jruby.Ruby;
     * import org.jruby.RubySymbol;
     *
     * public class MapKeyToJRubySymbolConverter implements RubyDateTimeParsedElementsQuery.MapKeyConverter<RubySymbol> {
     *     public MapKeyToJRubySymbolConverter(final Ruby ruby) {
     *         this.ruby = ruby;
     *     }
     *
     *     public RubySymbol convertMapKey(final String mapKey) {
     *         return RubySymbol.newSymbol(this.ruby, mapKey);
     *     }
     *
     *     private final Ruby ruby;
     * }
     * }
     * </pre>
     *
     * @param <T>  the target type to be converted into
     */
    public static interface MapKeyConverter<T> {
        /**
         * Converts a {@link java.lang.String} into the required type {@code <T>} as keys in the result {@link java.util.Map} of {@link RubyDateTimeParsedElementsQuery}.
         *
         * @param mapKey  the map key, not null
         * @return the converted map key object, not null
         */
        T convertMapKey(String mapKey);
    }

    private RubyDateTimeParsedElementsQuery(
            final DecimalFractionConverter decimalFractionConverter,
            final MapKeyConverter<T> mapKeyConverter) {
        this.decimalFractionConverter = decimalFractionConverter;
        this.mapKeyConverter = mapKeyConverter;
    }

    /**
     * Creates a query with seconds represented in {@link java.math.BigDecimal}.
     *
     * @return the query, not null
     */
    public static RubyDateTimeParsedElementsQuery<String> withDecimalFractionInBigDecimal() {
        return new RubyDateTimeParsedElementsQuery<String>(FRACTION_TO_BIG_DECIMAL, STRING_AS_IS);
    }

    /**
     * Creates a query with seconds converted by the {@code decimalFractionConverter}.
     *
     * @param decimalFractionConverter  the converter to convert decimal fractions, not null
     * @return the query, not null
     */
    public static RubyDateTimeParsedElementsQuery<String> with(final DecimalFractionConverter decimalFractionConverter) {
        return new RubyDateTimeParsedElementsQuery<String>(decimalFractionConverter, STRING_AS_IS);
    }

    /**
     * Creates a query with seconds converted by the {@code decimalFractionConverter}, and map keys converted by the {@code mapKeyConverter}.
     *
     * @param <U>  the key type of the result {@link java.util.Map}
     * @param decimalFractionConverter  the converter to convert decimal fractions, not null
     * @param mapKeyConverter  the converter to convert map keys, not null
     * @return the query, not null
     */
    public static <U> RubyDateTimeParsedElementsQuery<U> with(
            final DecimalFractionConverter decimalFractionConverter,
            final MapKeyConverter<U> mapKeyConverter) {
        return new RubyDateTimeParsedElementsQuery<U>(decimalFractionConverter, mapKeyConverter);
    }

    /**
     * Queries the specified temporal object.
     *
     * @param temporal  the temporal object to query, not null
     * @return the queried {@link java.util.Map}, not null
     */
    @Override
    public Map<T, Object> queryFrom(final TemporalAccessor temporal) {
        final Builder<T> builder = new Builder<T>(temporal, this.decimalFractionConverter, this.mapKeyConverter);

        builder.put("mday", ChronoField.DAY_OF_MONTH);
        builder.put("cwyear", RubyChronoFields.WEEK_BASED_YEAR);
        builder.putHourOfDay();
        builder.put("yday", ChronoField.DAY_OF_YEAR);
        builder.putSecFraction();
        builder.put("min", ChronoField.MINUTE_OF_HOUR);
        builder.put("mon", ChronoField.MONTH_OF_YEAR);
        builder.putInstantSeconds();
        builder.putSecondOfMinute();
        builder.put("wnum0", RubyChronoFields.WEEK_OF_YEAR_STARTING_WITH_SUNDAY);
        builder.put("wnum1", RubyChronoFields.WEEK_OF_YEAR_STARTING_WITH_MONDAY);
        builder.put("cwday", RubyChronoFields.DAY_OF_WEEK_STARTING_WITH_MONDAY_1);
        builder.put("cweek", RubyChronoFields.WEEK_OF_WEEK_BASED_YEAR);
        builder.put("wday", RubyChronoFields.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0);
        builder.put("year", ChronoField.YEAR);

        builder.putTimeZone();
        builder.putLeftover();

        return builder.build();
    }

    private static class Builder<T> {
        private Builder(
                final TemporalAccessor temporal,
                final DecimalFractionConverter decimalFractionConverterInner,
                final MapKeyConverter<T> mapKeyConverter) {
            this.temporal = temporal;
            this.decimalFractionConverterInner = decimalFractionConverterInner;
            this.mapKeyConverter = mapKeyConverter;
            this.built = new HashMap<>();
        }

        private void put(final String key, final TemporalField field) {
            if (this.temporal.isSupported(field)) {
                this.built.put(this.mapKeyConverter.convertMapKey(key), this.temporal.get(field));
            }
        }

        private void putHourOfDay() {
            if (this.temporal.isSupported(ChronoField.HOUR_OF_DAY)) {
                final Period parsedExcessDays = this.temporal.query(DateTimeFormatter.parsedExcessDays());
                if (parsedExcessDays == null || parsedExcessDays.isZero()) {
                    this.built.put(this.mapKeyConverter.convertMapKey("hour"),
                                   this.temporal.get(ChronoField.HOUR_OF_DAY));
                } else if (parsedExcessDays.getDays() == 1
                                   && parsedExcessDays.getMonths() == 0
                                   && parsedExcessDays.getYears() == 0) {
                    this.built.put(this.mapKeyConverter.convertMapKey("hour"), 24);
                }
            }
        }

        private void putSecondOfMinute() {
            if (this.temporal.isSupported(ChronoField.SECOND_OF_MINUTE)) {
                if (this.temporal.query(DateTimeFormatter.parsedLeapSecond())) {
                    this.built.put(this.mapKeyConverter.convertMapKey("sec"), 60);
                } else {
                    this.built.put(this.mapKeyConverter.convertMapKey("sec"),
                                   this.temporal.get(ChronoField.SECOND_OF_MINUTE));
                }
            }
        }

        private void putSecFraction() {
            if (this.temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
                this.built.put(this.mapKeyConverter.convertMapKey("sec_fraction"),
                               this.decimalFractionConverterInner.convertDecimalFraction(
                                       0, this.temporal.get(ChronoField.NANO_OF_SECOND)));
            }
        }

        private void putInstantSeconds() {
            if (this.temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                final long instantSeconds = this.temporal.getLong(ChronoField.INSTANT_SECONDS);
                final int nanoOfInstantSeconds;
                if (this.temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                    nanoOfInstantSeconds = this.temporal.get(RubyChronoFields.NANO_OF_INSTANT_SECONDS);
                } else {
                    nanoOfInstantSeconds = 0;
                }
                if (nanoOfInstantSeconds == 0) {
                    this.built.put(this.mapKeyConverter.convertMapKey("seconds"), instantSeconds);
                } else {
                    this.built.put(this.mapKeyConverter.convertMapKey("seconds"),
                                   this.decimalFractionConverterInner.convertDecimalFraction(
                                           instantSeconds, nanoOfInstantSeconds));
                }
            }
        }

        private void putTimeZone() {
            final String zone = temporal.query(RubyTemporalQueries.rubyTimeZone());
            if (zone != null) {
                final int offset = RubyDateTimeZones.toOffsetInSeconds(zone);
                if (offset != Integer.MIN_VALUE) {
                    this.built.put(this.mapKeyConverter.convertMapKey("offset"), offset);
                }
                this.built.put(this.mapKeyConverter.convertMapKey("zone"), zone);
            }
        }

        private void putLeftover() {
            final String leftover = temporal.query(RubyTemporalQueries.leftover());
            if (leftover != null) {
                this.built.put(this.mapKeyConverter.convertMapKey("leftover"), leftover);
            }
        }

        private Map<T, Object> build() {
            return Collections.unmodifiableMap(this.built);
        }

        private final TemporalAccessor temporal;
        private final DecimalFractionConverter decimalFractionConverterInner;
        private final MapKeyConverter<T> mapKeyConverter;
        private final HashMap<T, Object> built;
    }

    private static final DecimalFractionToBigDecimalConverter FRACTION_TO_BIG_DECIMAL;
    private static final StringAsIs STRING_AS_IS;

    private final static class DecimalFractionToBigDecimalConverter implements DecimalFractionConverter {
        @Override
        public Object convertDecimalFraction(final long integer, final int nano) {
            return BigDecimal.valueOf(integer).add(BigDecimal.valueOf(nano, 9));
        }
    }

    private final static class StringAsIs implements MapKeyConverter<String> {
        @Override
        public String convertMapKey(final String mapKey) {
            return mapKey;
        }
    }

    static {
        FRACTION_TO_BIG_DECIMAL = new DecimalFractionToBigDecimalConverter();
        STRING_AS_IS = new StringAsIs();
    }

    private final DecimalFractionConverter decimalFractionConverter;
    private final MapKeyConverter<T> mapKeyConverter;
}

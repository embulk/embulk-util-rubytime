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
     * A converter for fractional second to be stored as the required type in the result {@link java.util.Map}.
     *
     * <p>For example, the following implementation converts fractional second into Ruby's {@code Rational} object in JRuby.
     *
     * <pre>{@code
     * import org.jruby.Ruby;
     * import org.jruby.RubyRational;
     *
     * public class FractionalSecondToRationalConverter implements RubyDateTimeParsedElementsQuery.FractionalSecondConverter {
     *     public FractionalSecondToRationalConverter(final Ruby ruby) {
     *         this.ruby = ruby;
     *     }
     *
     *     public Object convertFractionalSecond(final long integer, final int nano) {
     *         return RubyRational.newRationalCanonicalize(
     *                        this.ruby.getCurrentContext(), ((long) integer * 1_000_000_000L) + (long) nano, 1_000_000_000L);
     *     }
     *
     *     private final Ruby ruby;
     * }
     * }
     * </pre>
     */
    public static interface FractionalSecondConverter {
        /**
         * Converts fractional second, a pair of an integer part and a fraction part, into an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map} of {@link RubyDateTimeParsedElementsQuery}.
         *
         * @param integer  the integer part
         * @param nano  the fraction part in nano
         * @return an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map}, not null
         */
        Object convertFractionalSecond(long integer, int nano);
    }

    /**
     * A converter for millisecond to be stored as the required type in the result {@link java.util.Map}.
     *
     * <p>For example, the following implementation converts millisecond into Ruby's {@code Rational} object in JRuby.
     *
     * <pre>{@code
     * import org.jruby.Ruby;
     * import org.jruby.RubyRational;
     *
     * public class MillisecondToRationalConverter implements RubyDateTimeParsedElementsQuery.MillisecondConverter {
     *     public MillisecondToRationalConverter(final Ruby ruby) {
     *         this.ruby = ruby;
     *     }
     *
     *     public Object convertMillisecond(final long millisecond) {
     *         return RubyRational.newRationalCanonicalize(this.ruby.getCurrentContext(), millisecond, 1000L);
     *     }
     *
     *     private final Ruby ruby;
     * }
     * }
     * </pre>
     */
    public static interface MillisecondConverter {
        /**
         * Converts millisecond in long into an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map} of {@link RubyDateTimeParsedElementsQuery}.
         *
         * @param millisecond  millisecond
         * @return an arbitrary {@link java.lang.Object} to be stored in the result {@link java.util.Map}, not null
         */
        Object convertMillisecond(long millisecond);
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
            final FractionalSecondConverter fractionalSecondConverter,
            final MillisecondConverter millisecondConverter,
            final MapKeyConverter<T> mapKeyConverter) {
        this.fractionalSecondConverter = fractionalSecondConverter;
        this.millisecondConverter = millisecondConverter;
        this.mapKeyConverter = mapKeyConverter;
    }

    /**
     * Creates a query with seconds represented in {@link java.math.BigDecimal}.
     *
     * @return the query, not null
     */
    public static RubyDateTimeParsedElementsQuery<String> withBigDecimal() {
        return new RubyDateTimeParsedElementsQuery<String>(
                FRACTIONAL_SECOND_TO_BIG_DECIMAL, MILLISECOND_TO_BIG_DECIMAL, STRING_AS_IS);
    }

    /**
     * Creates a query with seconds converted by {@code fractionalSecondConverter} and {@code millisecondConverter}.
     *
     * @param fractionalSecondConverter  the converter to convert fractional second, not null
     * @param millisecondConverter  the converter to convert millisecond, not null
     * @return the query, not null
     */
    public static RubyDateTimeParsedElementsQuery<String> with(
            final FractionalSecondConverter fractionalSecondConverter,
            final MillisecondConverter millisecondConverter) {
        return new RubyDateTimeParsedElementsQuery<String>(fractionalSecondConverter, millisecondConverter, STRING_AS_IS);
    }

    /**
     * Creates a query with seconds converted by {@code fractionalSecondConverter} and {@code millisecondConverter}, and map keys converted by the {@code mapKeyConverter}.
     *
     * @param <U>  the key type of the result {@link java.util.Map}
     * @param fractionalSecondConverter  the converter to convert fractional second, not null
     * @param millisecondConverter  the converter to convert millisecond, not null
     * @param mapKeyConverter  the converter to convert map keys, not null
     * @return the query, not null
     */
    public static <U> RubyDateTimeParsedElementsQuery<U> with(
            final FractionalSecondConverter fractionalSecondConverter,
            final MillisecondConverter millisecondConverter,
            final MapKeyConverter<U> mapKeyConverter) {
        return new RubyDateTimeParsedElementsQuery<U>(fractionalSecondConverter, millisecondConverter, mapKeyConverter);
    }

    /**
     * Queries the specified temporal object.
     *
     * @param temporal  the temporal object to query, not null
     * @return the queried {@link java.util.Map}, not null
     */
    @Override
    public Map<T, Object> queryFrom(final TemporalAccessor temporal) {
        final Builder<T> builder = new Builder<T>(
                temporal, this.fractionalSecondConverter, this.millisecondConverter, this.mapKeyConverter);

        builder.put("mday", ChronoField.DAY_OF_MONTH);
        builder.put("cwyear", RubyChronoFields.WEEK_BASED_YEAR);
        builder.putHourOfDay();
        builder.put("yday", ChronoField.DAY_OF_YEAR);
        builder.putSecFraction();
        builder.put("min", ChronoField.MINUTE_OF_HOUR);
        builder.put("mon", ChronoField.MONTH_OF_YEAR);
        builder.putSinceEpoch();
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
                final FractionalSecondConverter fractionalSecondConverterInner,
                final MillisecondConverter millisecondConverterInner,
                final MapKeyConverter<T> mapKeyConverter) {
            this.temporal = temporal;
            this.fractionalSecondConverterInner = fractionalSecondConverterInner;
            this.millisecondConverterInner = millisecondConverterInner;
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
                               this.fractionalSecondConverterInner.convertFractionalSecond(
                                       0, this.temporal.get(ChronoField.NANO_OF_SECOND)));
            }
        }

        private void putSinceEpoch() {
            if (this.temporal.isSupported(RubyChronoFields.INSTANT_MILLIS)) {
                // INSTANT_MILLIS (%Q) is prioritized over INSTANT_SECONDS (%s) "if exists".
                //
                // Once %Q is specified, both INSTANT_MILLIS and INSTANT_SECONDS are set.
                // Once %s is specified, INSTANT_SECONDS is set, and INSTANT_MILLIS is cleared.
                // The later overrides the earlier.
                final long instantMillis = this.temporal.getLong(RubyChronoFields.INSTANT_MILLIS);
                this.built.put(this.mapKeyConverter.convertMapKey("seconds"),
                               this.millisecondConverterInner.convertMillisecond(instantMillis));
            } else if (this.temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                final long instantSeconds = this.temporal.getLong(ChronoField.INSTANT_SECONDS);
                this.built.put(this.mapKeyConverter.convertMapKey("seconds"), instantSeconds);
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
        private final FractionalSecondConverter fractionalSecondConverterInner;
        private final MillisecondConverter millisecondConverterInner;
        private final MapKeyConverter<T> mapKeyConverter;
        private final HashMap<T, Object> built;
    }

    private static final FractionalSecondToBigDecimalConverter FRACTIONAL_SECOND_TO_BIG_DECIMAL;
    private static final MillisecondToBigDecimalConverter MILLISECOND_TO_BIG_DECIMAL;
    private static final StringAsIs STRING_AS_IS;

    private final static class FractionalSecondToBigDecimalConverter implements FractionalSecondConverter {
        @Override
        public Object convertFractionalSecond(final long integer, final int nano) {
            return BigDecimal.valueOf(integer).add(BigDecimal.valueOf(nano, 9));
        }
    }

    private final static class MillisecondToBigDecimalConverter implements MillisecondConverter {
        @Override
        public Object convertMillisecond(final long millisecond) {
            return BigDecimal.valueOf(millisecond, 3);
        }
    }

    private final static class StringAsIs implements MapKeyConverter<String> {
        @Override
        public String convertMapKey(final String mapKey) {
            return mapKey;
        }
    }

    static {
        FRACTIONAL_SECOND_TO_BIG_DECIMAL = new FractionalSecondToBigDecimalConverter();
        MILLISECOND_TO_BIG_DECIMAL = new MillisecondToBigDecimalConverter();
        STRING_AS_IS = new StringAsIs();
    }

    private final FractionalSecondConverter fractionalSecondConverter;
    private final MillisecondConverter millisecondConverter;
    private final MapKeyConverter<T> mapKeyConverter;
}

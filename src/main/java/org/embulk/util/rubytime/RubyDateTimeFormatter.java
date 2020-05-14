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

import java.time.temporal.TemporalAccessor;

/**
 * Formatter for printing and parsing date-time objects.
 *
 * <p>Methods in this class are designed to be similar to {@link java.time.format.DateTimeFormatter}.
 *
 * <p>Parsing is implemented as a two-phase operation like {@link java.time.format.DateTimeFormatter} does.
 */
public final class RubyDateTimeFormatter {
    private RubyDateTimeFormatter(final Format format, final RubyDateTimeResolver resolver) {
        this.format = format;
        this.resolver = resolver;
    }

    /**
     * Enumeration of the style of zone name formatting.
     */
    public enum ZoneNameStyle {
        /**
         * Formats into {@code "UTC"} when the offset is +00:00. Otherwise {@code ""}.
         */
        NONE,

        /**
         * Formats into short zone names such as {@code "PST"}, {@code "PDT"}, {@code "JST"}, and else.
         */
        SHORT,
        ;
    }

    /**
     * Creates a formatter using the specified pattern with the default resolver similar to Ruby's {@code Time.strptime}.
     *
     * @param pattern  the pattern to use, not null
     *
     * @return the formatter based on the pattern, not null
     */
    public static RubyDateTimeFormatter ofPattern(final String pattern) {
        return new RubyDateTimeFormatter(Format.compile(pattern), RubyDateTimeResolver.ofDefault());
    }

    /**
     * Formats a date-time object using this formatter.
     *
     * @param temporal  the temporal object to format, not null
     *
     * @return the formatted string, not null
     *
     * @throws RubyDateTimeParseException  if the parse results in an error
     */
    public String format(final TemporalAccessor temporal) {
        return (new FormatterWithContext(temporal)).format(this.format);
    }

    /**
     * Formats a date-time object using this formatter with the zone name style specified.
     *
     * <p>If {@code zoneNameStyle} is {@link ZoneNameStyle#NONE}, {@code "%Z"} is formatted into {@code "UTC"}
     * only when the offset is +00:00. Otherwise, formatted into {@code ""}. It follows the basic behavior of
     * Ruby's {@code Time.strftime}.
     *
     * <p>If {@code zoneNameStyle} is {@link ZoneNameStyle#SHORT}, {@code "%Z"} is formatted into short zone names
     * such as {@code "PST"}, {@code "PDT"}, {@code "JST"}, and else.
     *
     * @param temporal  the temporal object to format, not null
     * @param zoneNameStyle  the style of zone name formatting
     *
     * @return the formatted string, not null
     *
     * @throws RubyDateTimeParseException  if the parse results in an error
     */
    public String formatWithZoneNameStyle(final TemporalAccessor temporal, final ZoneNameStyle zoneNameStyle) {
        return (new FormatterWithContext(temporal)).format(this.format, zoneNameStyle);
    }

    /**
     * Parses the text using this formatter, without resolving the result, intended for advanced use cases.
     *
     * @param text  the text to parse, not null
     *
     * @return the parsed text
     *
     * @throws RubyDateTimeParseException  if the parse results in an error
     */
    public TemporalAccessor parseUnresolved(final String text) {
        return (new ParserWithContext(text)).parse(this.format);
    }

    /**
     * Parses the text using this formatter and the registered resolver.
     *
     * @param text  the text to parse, not null
     *
     * @return the parsed temporal object, not null
     *
     * @throws RubyDateTimeParseException  if unable to parse the requested result
     */
    public TemporalAccessor parse(final String text) {
        try {
            return this.resolver.resolve(this.parseUnresolved(text));
        } catch (final RubyDateTimeParseException ex) {
            throw ex;
        } catch (final RuntimeException ex) {
            throw new RubyDateTimeParseException("Text '" + text + "' could not be parsed: " + ex.getMessage(), text, 0, ex);
        }
    }

    /**
     * Returns a copy of this formatter with a new resolver.
     *
     * @param resolver  the new resolver, not null
     *
     * @return a formatter based on this formatter with the requested resolver, not null
     */
    public RubyDateTimeFormatter withResolver(final RubyDateTimeResolver resolver) {
        return new RubyDateTimeFormatter(this.format, resolver);
    }

    private final Format format;
    private final RubyDateTimeResolver resolver;
}

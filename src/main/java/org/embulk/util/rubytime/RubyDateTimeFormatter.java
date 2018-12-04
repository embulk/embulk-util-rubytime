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
 * Formatter for printing and parsing date-time objects in a way similar to Ruby's {@code Time.strptime}.
 *
 * <p>Methods in this class are designed to be similar to {@link java.time.format.DateTimeFormatter}.
 */
public final class RubyDateTimeFormatter {
    private RubyDateTimeFormatter(final Format format) {
        this.format = format;
    }

    /**
     * Creates a formatter using the specified pattern.
     *
     * @param pattern  the pattern to use, not null
     *
     * @return the formatter based on the pattern, not null
     */
    public static RubyDateTimeFormatter ofPattern(final String pattern) {
        return new RubyDateTimeFormatter(Format.compile(pattern));
    }

    /**
     * Parses the text using this formatter, without resolving the result, intended for advanced use cases.
     *
     * <p>Parsing is implemented as a two-phase operation as {@link java.time.format.DateTimeFormatter#parseUnresolved} does.
     *
     * <p>Note that epoch milliseconds (%Q) and epoch seconds (%s) are considered equally.
     *
     * <pre>{@code
     * irb(main):002:0> Date._strptime("123456789 12849124", "%Q %s")
     * => {:seconds=>12849124}
     * irb(main):003:0> Date._strptime("123456789 12849124", "%s %Q")
     * => {:seconds=>(3212281/250)}
     * }</pre>
     *
     * @param text  the text to parse, not null
     *
     * @return the parsed text
     *
     * @throws RubyDateTimeParseException  if the parse results in an error
     */
    public TemporalAccessor parseUnresolved(final String text) {
        return new ParserWithContext(text).parse(this.format);
    }

    private final Format format;
}

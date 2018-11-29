package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * Formatter for printing and parsing date-time objects in a way similar to Ruby's Time.strptime.
 *
 * <p>Its interface is designed to be similar to {@code java.time.DateTimeFormatter}.
 *
 * <p>Note that epoch milliseconds (%Q) and epoch seconds (%s) are considered equally.
 *
 * <code>
 * irb(main):002:0> Date._strptime("123456789 12849124", "%Q %s")
 * => {:seconds=>12849124}
 * irb(main):003:0> Date._strptime("123456789 12849124", "%s %Q")
 * => {:seconds=>(3212281/250)}
 * </code>
 */
public final class RubyDateTimeFormatter {
    private RubyDateTimeFormatter(final RubyTimeFormat format) {
        this.format = format;
    }

    public static RubyDateTimeFormatter ofPattern(final String pattern) {
        return new RubyDateTimeFormatter(RubyTimeFormat.compile(pattern));
    }

    public TemporalAccessor parseUnresolved(final String text) {
        return new ParserWithContext(text).parse(this.format);
    }

    private final RubyTimeFormat format;
}

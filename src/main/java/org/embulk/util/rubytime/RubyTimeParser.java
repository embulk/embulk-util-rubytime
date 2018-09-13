package org.embulk.util.rubytime;

import java.time.temporal.TemporalAccessor;

/**
 * Parses a date/time String almost in the same way with Ruby's {@code Date._strptime}.
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
public class RubyTimeParser {
    public RubyTimeParser(final RubyTimeFormat format) {
        this.format = format;
    }

    public TemporalAccessor parse(final String text) {
        return new ParserWithContext(text).parse(this.format);
    }

    private final RubyTimeFormat format;
}

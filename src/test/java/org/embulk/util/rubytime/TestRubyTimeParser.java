package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.temporal.TemporalAccessor;
import org.junit.jupiter.api.Test;

/**
 * Tests RubyTimeParser.
 */
public class TestRubyTimeParser {
    @Test
    public void testMultipleEpochs() {
        final TemporalAccessor parsed1 = strptime("123456789 12849124", "%Q %s");
        assertEquals(1000L * 12849124L, parsed1.getLong(RubyChronoField.Field.INSTANT_MILLIS));

        final TemporalAccessor parsed2 = strptime("123456789 12849124", "%s %Q");
        assertEquals(1000L * 3212281L / 250L, parsed2.getLong(RubyChronoField.Field.INSTANT_MILLIS));
    }

    private static TemporalAccessor strptime(final String string, final String format) {
        final RubyTimeParser parser = new RubyTimeParser(RubyTimeFormat.compile(format));
        return parser.parse(string);
    }
}

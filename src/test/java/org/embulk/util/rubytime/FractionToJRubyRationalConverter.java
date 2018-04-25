package org.embulk.util.rubytime;

import org.jruby.Ruby;
import org.jruby.RubyRational;

public class FractionToJRubyRationalConverter implements ParsedElementsQuery.FractionConverter {
    public FractionToJRubyRationalConverter(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public Object convertFraction(final int seconds, final int nanoOfSecond) {
        return RubyRational.newRational(
                this.ruby, ((long) seconds * 1_000_000_000L) + (long) nanoOfSecond, 1_000_000_000L);
    }

    private final Ruby ruby;
}

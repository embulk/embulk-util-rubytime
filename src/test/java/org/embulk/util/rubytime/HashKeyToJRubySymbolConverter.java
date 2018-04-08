package org.embulk.util.rubytime;

import org.jruby.Ruby;
import org.jruby.RubySymbol;

public class HashKeyToJRubySymbolConverter implements ParsedElementsHashQuery.HashKeyConverter<RubySymbol> {
    public HashKeyToJRubySymbolConverter(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public RubySymbol convertHashKey(final String hashKey) {
        return RubySymbol.newSymbol(this.ruby, hashKey);
    }

    private final Ruby ruby;
}

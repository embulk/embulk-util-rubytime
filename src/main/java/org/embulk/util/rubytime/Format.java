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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a Ruby-compatible date-time format.
 */
final class Format implements Iterable<Format.TokenWithNext> {
    private Format(final List<FormatToken> compiledPattern) {
        this.compiledPattern = Collections.unmodifiableList(compiledPattern);
    }

    public static Format compile(final String formatString) {
        return new Format(CompilerForParser.compile(formatString));
    }

    static Format createForTesting(final List<FormatToken> compiledPattern) {
        return new Format(compiledPattern);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof Format)) {
            return false;
        }
        final Format other = (Format) otherObject;
        return this.compiledPattern.equals(other.compiledPattern);
    }

    @Override
    public Iterator<TokenWithNext> iterator() {
        return new TokenIterator(this.compiledPattern.iterator());
    }

    static class TokenWithNext {
        private TokenWithNext(final FormatToken token, final FormatToken nextToken) {
            this.token = token;
            this.nextToken = nextToken;
        }

        FormatToken getToken() {
            return this.token;
        }

        FormatToken getNextToken() {
            return this.nextToken;
        }

        private final FormatToken token;
        private final FormatToken nextToken;
    }

    private static class CompilerForParser {
        private CompilerForParser(final String formatString) {
            this.formatString = formatString;
        }

        public static List<FormatToken> compile(final String formatString) {
            return new CompilerForParser(formatString).compileInitial();
        }

        private List<FormatToken> compileInitial() {
            this.index = 0;
            this.resultTokens = new ArrayList<>();
            this.rawStringBuffer = new StringBuilder();

            while (this.index < this.formatString.length()) {
                final char cur = this.formatString.charAt(this.index);
                switch (cur) {
                    case '%':
                        if (this.rawStringBuffer.length() > 0) {
                            this.resultTokens.add(new FormatToken.Immediate(this.rawStringBuffer.toString()));
                        }
                        this.rawStringBuffer = new StringBuilder();
                        this.index++;
                        if (!this.compileDirective(this.index)) {
                            this.rawStringBuffer.append(cur);  // Add '%', and go next ordinarily.
                        }
                        break;
                    default:
                        this.rawStringBuffer.append(cur);
                        this.index++;
                }
            }
            if (this.rawStringBuffer.length() > 0) {
                this.resultTokens.add(new FormatToken.Immediate(this.rawStringBuffer.toString()));
            }

            return Collections.unmodifiableList(this.resultTokens);
        }

        private boolean compileDirective(final int beginningIndex) {
            if (beginningIndex >= this.formatString.length()) {
                return false;
            }
            final char cur = this.formatString.charAt(beginningIndex);
            switch (cur) {
                case 'E':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "cCxXyY".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case 'O':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "deHImMSuUVwWy".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case ':':
                    for (int i = 1; i <= 3; ++i) {
                        if (beginningIndex + i >= this.formatString.length()) {
                            return false;
                        }
                        if (this.formatString.charAt(beginningIndex + i) == 'z') {
                            return this.compileDirective(beginningIndex + i);
                        }
                        if (this.formatString.charAt(beginningIndex + i) != ':') {
                            return false;
                        }
                    }
                    return false;
                case '%':
                    this.resultTokens.add(new FormatToken.Immediate("%"));
                    this.index = beginningIndex + 1;
                    return true;
                default:
                    if (FormatDirective.isSpecifier(cur)) {
                        this.resultTokens.addAll(FormatDirective.of(cur).toTokens());
                        this.index = beginningIndex + 1;
                        return true;
                    } else {
                        return false;
                    }
            }
        }

        private final String formatString;

        private int index;
        private List<FormatToken> resultTokens;
        private StringBuilder rawStringBuffer;
    }

    private static class TokenIterator implements Iterator<TokenWithNext> {
        private TokenIterator(final Iterator<FormatToken> initialIterator) {
            this.internalIterator = initialIterator;
            if (initialIterator.hasNext()) {
                this.next = initialIterator.next();
            } else {
                this.next = null;
            }
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public TokenWithNext next() {
            final TokenWithNext tokenWithNext;
            if (this.internalIterator.hasNext()) {
                tokenWithNext = new TokenWithNext(this.next, this.internalIterator.next());
            } else {
                tokenWithNext = new TokenWithNext(this.next, null);
            }
            this.next = tokenWithNext.getNextToken();
            return tokenWithNext;
        }

        private final Iterator<FormatToken> internalIterator;
        private FormatToken next;
    }

    private final List<FormatToken> compiledPattern;
}

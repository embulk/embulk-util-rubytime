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
import java.util.Objects;

/**
 * Represents a Ruby-compatible date-time format.
 */
final class Format implements Iterable<Format.TokenWithNext> {
    private Format(final List<FormatToken> compiledPattern) {
        this.compiledPattern = Collections.unmodifiableList(compiledPattern);

        boolean onlyForFormatter = false;
        for (final FormatToken token : compiledPattern) {
            if (token.onlyForFormatter()) {
                onlyForFormatter = true;
            }
        }
        this.onlyForFormatter = onlyForFormatter;
    }

    public static Format compile(final String formatString) {
        return new Format(Compiler.compile(formatString));
    }

    static Format createForTesting(final List<FormatToken> compiledPattern) {
        return new Format(compiledPattern);
    }

    boolean onlyForFormatter() {
        return this.onlyForFormatter;
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
    public int hashCode() {
        return Objects.hash(this.compiledPattern);
    }

    @Override
    public String toString() {
        return this.compiledPattern.toString();
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

    /**
     * Compiles a date-time format string into internal representation, which is available both for parsing and formatting.
     *
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_5/ext/date/date_strptime.c#L164-L651">date__strptime_internal</a>
     * @see <a href="https://github.com/ruby/ruby/blob/v2_6_5/strftime.c#L222-L907">rb_strftime_with_timespec</a>
     */
    private static class Compiler {
        private Compiler(final String formatString) {
            this.formatString = formatString;
        }

        public static List<FormatToken> compile(final String formatString) {
            return new Compiler(formatString).compileInitial();
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
                            this.resultTokens.add(FormatToken.immediate(this.rawStringBuffer.toString()));
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
                this.resultTokens.add(FormatToken.immediate(this.rawStringBuffer.toString()));
            }

            return Collections.unmodifiableList(this.resultTokens);
        }

        /**
         * Compiles {@code formatString} from {@code [beginningIndex]} as a directive.
         *
         * <p>The compiled directives are pushed into {@code resultTokens} if recognized as a directive.
         *
         * @param beginningIndex  index of the character next to {@code '%'} in {@code formatString}.
         * @return {@code true} if recognized as a directive, {@code false} otherwise.
         */
        @SuppressWarnings("checkstyle:LeftCurly")
        private boolean compileDirective(final int beginningIndex) {
            final FormatDirectiveOptions.Builder optionsBuilder = FormatDirectiveOptions.builder();

            for (int cursorIndex = beginningIndex; cursorIndex < this.formatString.length(); cursorIndex++) {
                final char cur = this.formatString.charAt(cursorIndex);
                switch (cur) {
                    case '%':  // FormatDirective.IMMEDIATE_PERCENT:
                    case 'n':  // FormatDirective.IMMEDIATE_NEWLINE
                    case 't':  // FormatDirective.IMMEDIATE_TAB
                        // They are compiled as directives so that options can work for formatting. For example,
                        //
                        //    irb(main):001:0> Time.now.strftime("%10%")
                        //    => "         %"
                        //    irb(main):002:0> Time.now.strftime("%010n")
                        //    => "000000000\n"

                    case 'a':  // FormatDirective.DAY_OF_WEEK_ABBREVIATED_NAME
                    case 'A':  // FormatDirective.DAY_OF_WEEK_FULL_NAME
                    case 'h':  // FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H
                    case 'b':  // FormatDirective.MONTH_OF_YEAR_ABBREVIATED_NAME
                    case 'B':  // FormatDirective.MONTH_OF_YEAR_FULL_NAME

                    case 'P':  // FormatDirective.AMPM_OF_DAY_LOWER_CASE
                    case 'p':  // FormatDirective.AMPM_OF_DAY_UPPER_CASE

                    case 'c':  // FormatDirective.RECURRED_UPPER_C
                    case 'x':  // FormatDirective.RECURRED_LOWER_X
                    case 'X':  // FormatDirective.RECURRED_UPPER_X
                    case 'D':  // FormatDirective.RECURRED_UPPER_D
                    case 'r':  // FormatDirective.RECURRED_LOWER_R
                    case 'R':  // FormatDirective.RECURRED_UPPER_R
                    case 'T':  // FormatDirective.RECURRED_UPPER_T
                    case 'v':  // FormatDirective.RECURRED_LOWER_V
                    case 'F':  // FormatDirective.RECURRED_UPPER_F
                    case '+':  // FormatDirective.RECURRED_PLUS

                    case 'd':  // FormatDirective.DAY_OF_MONTH_ZERO_PADDED
                    case 'H':  // FormatDirective.HOUR_OF_DAY_ZERO_PADDED
                    case 'I':  // FormatDirective.HOUR_OF_AMPM_ZERO_PADDED
                    case 'j':  // FormatDirective.DAY_OF_YEAR
                    case 'm':  // FormatDirective.MONTH_OF_YEAR
                    case 'M':  // FormatDirective.MINUTE_OF_HOUR
                    case 's':  // FormatDirective.SECONDS_SINCE_EPOCH
                    case 'S':  // FormatDirective.SECOND_OF_MINUTE
                    case 'U':  // FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY
                    case 'W':  // FormatDirective.WEEK_OF_YEAR_STARTING_WITH_MONDAY
                    case 'w':  // FormatDirective.DAY_OF_WEEK_STARTING_WITH_SUNDAY_0
                    case 'y':  // FormatDirective.YEAR_WITHOUT_CENTURY
                    case 'Y':  // FormatDirective.YEAR_WITH_CENTURY
                    case 'e':  // FormatDirective.DAY_OF_MONTH_BLANK_PADDED
                    case 'k':  // FormatDirective.HOUR_OF_DAY_BLANK_PADDED
                    case 'l':  // FormatDirective.HOUR_OF_AMPM_BLANK_PADDED
                    case 'C':  // FormatDirective.CENTURY -- FMTV
                    case 'V':  // FormatDirective.WEEK_OF_WEEK_BASED_YEAR
                    case 'u':  // FormatDirective.DAY_OF_WEEK_STARTING_WITH_MONDAY_1

                    case 'Q':  // FormatDirective.MILLISECONDS_SINCE_EPOCH
                        // %Q is only for parsing, not for formatting. Then, %Q never takes any option.
                        // So, a token of "%Q" can always be stringified straightforward to "%Q".

                    case 'z':  // FormatDirective.TIME_OFFSET

                    case 'Z':  // FormatDirective.TIME_ZONE_NAME

                    case 'G':  // FormatDirective.WEEK_BASED_YEAR_WITH_CENTURY
                    case 'g':  // FormatDirective.WEEK_BASED_YEAR_WITHOUT_CENTURY

                    case 'L':  // FormatDirective.MILLI_OF_SECOND
                    case 'N':  // FormatDirective.NANO_OF_SECOND
                        this.resultTokens.add(FormatToken.directive(
                                "%" + this.formatString.substring(beginningIndex, cursorIndex + 1),
                                FormatDirective.of(cur),
                                optionsBuilder.build()));
                        this.index = cursorIndex + 1;
                        return true;

                    case 'E':
                        if (cursorIndex + 1 < this.formatString.length()
                                && "cCxXyY".indexOf(this.formatString.charAt(cursorIndex + 1)) >= 0) {
                            break;
                        } else {
                            return false;
                        }
                    case 'O':
                        if (cursorIndex + 1 < this.formatString.length()
                                && "deHImMSuUVwWy".indexOf(this.formatString.charAt(cursorIndex + 1)) >= 0) {
                            break;
                        } else {
                            return false;
                        }

                    case '-':
                        if (optionsBuilder.isPrecisionSpecified()) {
                            return false;
                        }
                        optionsBuilder.setLeft();
                        break;

                    case '^':
                        if (optionsBuilder.isPrecisionSpecified()) {
                            return false;
                        }
                        optionsBuilder.setUpper();
                        break;

                    case '#':
                        if (optionsBuilder.isPrecisionSpecified()) {
                            return false;
                        }
                        optionsBuilder.setChCase();
                        break;

                    case '_':
                        if (optionsBuilder.isPrecisionSpecified()) {
                            return false;
                        }
                        optionsBuilder.setPadding(' ');
                        break;

                    case ':':
                        // strptime accepts only 3 colons at maximum.
                        // strftime accepts unlimited number of colons.
                        for (int i = 1; ; i++) {
                            if (cursorIndex + i >= this.formatString.length()) {
                                return false;
                            }
                            if (this.formatString.charAt(cursorIndex + i) == 'z') {
                                optionsBuilder.setColons(i);
                                cursorIndex += (i - 1);
                                break;
                            }
                            if (this.formatString.charAt(cursorIndex + i) != ':') {
                                return false;
                            }
                        }
                        break;

                    case '0':
                        optionsBuilder.setPadding('0');
                        // fall-through

                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        {
                            final String digits = this.consumeDigits(cursorIndex);
                            if (digits == null) {
                                return false;
                            }
                            optionsBuilder.setPrecision(Integer.parseInt(digits));
                            cursorIndex += digits.length() - 1;
                        }
                        break;

                    default:
                        return false;
                }
            }
            return false;
        }

        /**
         * Consumes digits in a format directive token.
         *
         * <pre>{@code
         * Time.now.strftime("%-2147483647s")
         * => "1576139531"
         * Time.now.strftime("%-2147483648s")
         * => "%-2147483648s"
         * }</pre>
         */
        private String consumeDigits(final int start) {
            int startNonZero = start;
            for (; startNonZero < this.formatString.length() && this.formatString.charAt(startNonZero) == '0'; startNonZero++) {
            }

            int endDigit = startNonZero;
            for (; endDigit < this.formatString.length() && isDigit(this.formatString.charAt(endDigit)); endDigit++) {
            }

            if (endDigit - startNonZero > 10) {
                return null;
            }

            final String digits = this.formatString.substring(startNonZero, endDigit);
            if (endDigit > startNonZero) {
                final long parsedLong = Long.parseLong(digits);
                if (parsedLong > Integer.MAX_VALUE) {
                    return null;
                }
            }

            return this.formatString.substring(start, endDigit);
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

    /**
     * Determines if the specified character is a digit.
     *
     * <p>It accepts only ASCII digit characters intentionally.
     */
    private static boolean isDigit(final char c) {
        return '0' <= c && c <= '9';
    }

    private final List<FormatToken> compiledPattern;
    private final boolean onlyForFormatter;
}

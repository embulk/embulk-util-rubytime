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

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a token, a part of a Ruby-compatible date-time format string.
 *
 * <p>A token can be an immediate string (e.g. "foo", "T", or else), or a directive.
 * A directive is a special string piece, starting with {@code '%'}, to be used for
 * parsing and formatting.
 *
 * <p>Even in a directive, it keeps its "immediate string" in it. For example for
 * {@code "%-000124s"}, it means seconds since epoch, in 124-digit precision,
 * without padding for a numerical output, but zero-padded (, zero-padded, zero-padded).
 * So, it means just the same with {@code "%s"}, but the immediate string keeps
 * the original string {@code "%-000124s"}.
 */
final class FormatToken {
    private FormatToken(final String immediate, final FormatDirective directive, final FormatDirectiveOptions options) {
        this.immediate = immediate;
        this.directive = directive;
        this.options = options;
    }

    static FormatToken directive(
            final String immediate,
            final FormatDirective directive,
            final FormatDirectiveOptions options) {
        return new FormatToken(immediate, directive, options);
    }

    static FormatToken directive(final String immediate, final FormatDirective directive) {
        return new FormatToken(immediate, directive, FormatDirectiveOptions.EMPTY);
    }

    static FormatToken immediate(final char character) {
        return new FormatToken("" + character, null, null);
    }

    static FormatToken immediate(final String string) {
        return new FormatToken(string, null, null);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof FormatToken)) {
            return false;
        }
        final FormatToken other = (FormatToken) otherObject;
        return Objects.equals(this.immediate, other.immediate)
                && Objects.equals(this.directive, other.directive)
                && Objects.equals(this.options, other.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.immediate, this.directive, this.options);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (this.directive != null) {
            builder.append("%");
            if (this.options != null) {
                builder.append(this.options.toString());
            }
            builder.append(this.directive.toString());
            if (this.immediate != null) {
                builder.append("[\"").append(this.immediate).append("\"]");
            }
        } else {
            builder.append("\"").append(this.immediate).append("\"");
        }
        return builder.toString();
    }

    boolean isImmediate() {
        return this.directive == null;
    }

    boolean isDirective() {
        return this.directive != null;
    }

    boolean onlyForFormatter() {
        if (this.isImmediate()) {
            return false;
        }
        if (this.options.onlyForFormatter()) {
            return true;
        }
        return false;
    }

    Optional<FormatDirective> getFormatDirective() {
        return Optional.ofNullable(this.directive);
    }

    Optional<String> getImmediate() {
        return Optional.ofNullable(this.immediate);
    }

    Optional<FormatDirectiveOptions> getDirectiveOptions() {
        return Optional.ofNullable(this.options);
    }

    private final String immediate;
    private final FormatDirective directive;
    private final FormatDirectiveOptions options;
}

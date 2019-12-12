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
 * Represents a token instance in Ruby-compatible date-time format strings.
 */
final class FormatToken {
    private FormatToken(final String immediate, final FormatDirective directive) {
        this.immediate = immediate;
        this.directive = directive;
    }

    static FormatToken directive(final FormatDirective directive) {
        return new FormatToken(null, directive);
    }

    static FormatToken immediate(final char character) {
        return new FormatToken("" + character, null);
    }

    static FormatToken immediate(final String string) {
        return new FormatToken(string, null);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof FormatToken)) {
            return false;
        }
        final FormatToken other = (FormatToken) otherObject;
        return Objects.equals(this.immediate, other.immediate)
                && Objects.equals(this.directive, other.directive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.immediate, this.directive);
    }

    @Override
    public String toString() {
        if (this.immediate == null) {
            return "<%" + this.directive.toString() + ">";
        } else {
            return "<\"" + this.immediate + "\">";
        }
    }

    boolean isImmediate() {
        return this.immediate != null;
    }

    boolean isDirective() {
        return this.directive != null;
    }

    Optional<FormatDirective> getFormatDirective() {
        return Optional.ofNullable(this.directive);
    }

    Optional<String> getImmediate() {
        return Optional.ofNullable(this.immediate);
    }

    private final String immediate;
    private final FormatDirective directive;
}

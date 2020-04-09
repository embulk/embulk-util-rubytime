/*
 * Copyright 2019 The Embulk project
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

/**
 * Represents a token instance in Ruby-compatible date-time format strings.
 *
 * <p>It represents explicitly-specified options. Default values are not considered in this class.
 */
final class FormatDirectiveOptions {
    private FormatDirectiveOptions(
            final boolean onlyForFormatter,
            final int precision,
            final boolean isLeft,
            final boolean isUpper,
            final boolean isChCase,
            final char padding,
            final int colons) {
        this.onlyForFormatter = onlyForFormatter;
        this.precision = precision;
        this.isLeft = isLeft;
        this.isUpper = isUpper;
        this.isChCase = isChCase;
        this.padding = padding;
        this.colons = colons;
    }

    static class Builder {
        Builder() {
            this.onlyForFormatter = false;
            this.precision = 0;
            this.isLeft = false;
            this.isUpper = false;
            this.isChCase = false;
            this.padding = '\0';
            this.colons = 0;
        }

        Builder setColons(final int colons) {
            if (colons <= 0) {
                throw new IllegalArgumentException();
            }
            if (this.colons > 0) {  // Colons must be consequent -- no double sets.
                throw new IllegalArgumentException();
            }
            if (colons > 3) {
                this.onlyForFormatter = true;
            }
            this.colons = colons;
            return this;
        }

        Builder setPadding(final char padding) {
            this.onlyForFormatter = true;
            this.padding = padding;
            return this;
        }

        Builder setLeft() {
            this.onlyForFormatter = true;
            this.isLeft = true;
            return this;
        }

        Builder setUpper() {
            this.onlyForFormatter = true;
            this.isUpper = true;
            return this;
        }

        Builder setChCase() {
            this.onlyForFormatter = true;
            this.isChCase = true;
            return this;
        }

        Builder setPrecision(final int precision) {
            this.onlyForFormatter = true;
            this.precision = precision;
            return this;
        }

        boolean isPrecisionSpecified() {
            return this.precision > 0;
        }

        FormatDirectiveOptions build() {
            return new FormatDirectiveOptions(
                    this.onlyForFormatter,
                    this.precision,
                    this.isLeft,
                    this.isUpper,
                    this.isChCase,
                    this.padding,
                    this.colons);
        }

        private boolean onlyForFormatter;

        private int precision;
        private boolean isLeft;
        private boolean isUpper;
        private boolean isChCase;
        private char padding;
        private int colons;
    }

    static Builder builder() {
        return new Builder();
    }

    boolean onlyForFormatter() {
        return this.onlyForFormatter;
    }

    int getPrecision(final int defaultPrecision) {
        if (this.precision <= 0) {
            return defaultPrecision;
        }
        return this.precision;
    }

    boolean isLeft() {
        return this.isLeft;
    }

    boolean isUpper() {
        return this.isUpper;
    }

    boolean isChCase() {
        return this.isChCase;
    }

    char getPadding(final char defaultPadding) {
        if (this.padding == '\0') {
            return defaultPadding;
        }
        return this.padding;
    }

    int getColons() {
        return this.colons;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof FormatDirectiveOptions)) {
            return false;
        }
        final FormatDirectiveOptions other = (FormatDirectiveOptions) otherObject;
        return Objects.equals(this.onlyForFormatter, other.onlyForFormatter)
                && Objects.equals(this.precision, other.precision)
                && Objects.equals(this.isLeft, other.isLeft)
                && Objects.equals(this.isUpper, other.isUpper)
                && Objects.equals(this.isChCase, other.isChCase)
                && Objects.equals(this.padding, other.padding)
                && Objects.equals(this.colons, other.colons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.onlyForFormatter,
                this.precision,
                this.isLeft,
                this.isUpper,
                this.isChCase,
                this.padding,
                this.colons);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (this.isLeft) {
            builder.append('-');
        }
        if (this.isUpper) {
            builder.append('^');
        }
        if (this.isChCase) {
            builder.append('#');
        }
        if (this.padding != '\0') {
            builder.append(padding);
        }
        if (this.colons > 0) {
            for (int i = 0; i < colons; i++) {
                builder.append(':');
            }
        }
        if (this.precision > 0) {
            builder.append(String.valueOf(this.precision));
        }
        return builder.toString();
    }

    static final FormatDirectiveOptions EMPTY = new FormatDirectiveOptions(false, 0, false, false, false, '\0', 0);

    private boolean onlyForFormatter;

    private final int precision;
    private final boolean isLeft;
    private final boolean isUpper;
    private final boolean isChCase;
    private final char padding;
    private final int colons;
}

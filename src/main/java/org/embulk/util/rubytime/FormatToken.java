package org.embulk.util.rubytime;

/**
 * Represents a token instance in Ruby-compatible date-time format strings.
 */
abstract class FormatToken {
    abstract boolean isDirective();

    static class Directive extends FormatToken {
        Directive(final FormatDirective formatDirective) {
            this.formatDirective = formatDirective;
        }

        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof Directive)) {
                return false;
            }
            final Directive other = (Directive) otherObject;
            return this.formatDirective.equals(other.formatDirective);
        }

        @Override
        public String toString() {
            return "<%" + this.formatDirective.toString() + ">";
        }

        @Override
        boolean isDirective() {
            return true;
        }

        FormatDirective getFormatDirective() {
            return this.formatDirective;
        }

        private final FormatDirective formatDirective;
    }

    static class Immediate extends FormatToken {
        Immediate(final char character) {
            this.string = "" + character;
        }

        Immediate(final String string) {
            this.string = string;
        }

        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof Immediate)) {
                return false;
            }
            final Immediate other = (Immediate) otherObject;
            return this.string.equals(other.string);
        }

        @Override
        public String toString() {
            return "<\"" + this.string + "\">";
        }

        @Override
        boolean isDirective() {
            return false;
        }

        String getContent() {
            return this.string;
        }

        private final String string;
    }
}

package org.embulk.util.rubytime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Year;

final class ParserWithContext {
    ParserWithContext(final CharSequence text) {
        this.text = text.toString();

        this.pos = 0;
    }

    Parsed parse(final Format format) {
        final Parsed.Builder builder = Parsed.builder(this.text);

        for (final Format.TokenWithNext tokenWithNext : format) {
            final FormatToken token = tokenWithNext.getToken();

            if (!token.isDirective()) {
                this.consumeImmediateString(((FormatToken.Immediate) token).getContent());
            } else {
                switch (((FormatToken.Directive) token).getFormatDirective()) {
                    // %A - The full weekday name (``Sunday'')
                    // %a - The abbreviated name (``Sun'')
                    case DAY_OF_WEEK_FULL_NAME:
                    case DAY_OF_WEEK_ABBREVIATED_NAME:
                        this.consumeWeekName(builder);
                        break;

                    // %B - The full month name (``January'')
                    // %b, %h - The abbreviated month name (``Jan'')
                    case MONTH_OF_YEAR_FULL_NAME:
                    case MONTH_OF_YEAR_ABBREVIATED_NAME:
                    case MONTH_OF_YEAR_ABBREVIATED_NAME_ALIAS_SMALL_H:
                        this.consumeMonthOfYearName(builder);
                        break;

                    // %C - year / 100 (round down.  20 in 2009)
                    case CENTURY:
                        this.consumeCentury(builder, tokenWithNext.getNextToken());
                        break;

                    // %d, %Od - Day of the month, zero-padded (01..31)
                    // %e, %Oe - Day of the month, blank-padded ( 1..31)
                    case DAY_OF_MONTH_ZERO_PADDED:
                    case DAY_OF_MONTH_BLANK_PADDED:
                        this.consumeDayOfMonth(builder);
                        break;

                    // %G - The week-based year
                    case WEEK_BASED_YEAR_WITH_CENTURY:
                        this.consumeWeekBasedYearWithCentury(builder, tokenWithNext.getNextToken());
                        break;

                    // %g - The last 2 digits of the week-based year (00..99)
                    case WEEK_BASED_YEAR_WITHOUT_CENTURY:
                        this.consumeWeekBasedYearWithoutCentury(builder);
                        break;

                    // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                    case HOUR_OF_DAY_ZERO_PADDED:
                    case HOUR_OF_DAY_BLANK_PADDED:
                        this.consumeHourOfDay(builder);
                        break;

                    // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                    case HOUR_OF_AMPM_ZERO_PADDED:
                    case HOUR_OF_AMPM_BLANK_PADDED:
                        this.consumeHourOfAmPm(builder);
                        break;

                    // %j - Day of the year (001..366)
                    case DAY_OF_YEAR:
                        this.consumeDayOfYear(builder);
                        break;

                    // %L - Millisecond of the second (000..999)
                    // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                    case MILLI_OF_SECOND:
                    case NANO_OF_SECOND:
                        this.consumeSubsecond(builder, token, tokenWithNext.getNextToken());
                        break;

                    // %M, %OM - Minute of the hour (00..59)
                    case MINUTE_OF_HOUR:
                        this.consumeMinuteOfHour(builder);
                        break;

                    // %m, %Om - Month of the year, zero-padded (01..12)
                    case MONTH_OF_YEAR:
                        this.consumeMonthOfYear(builder);
                        break;

                    // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                    case AMPM_OF_DAY_UPPER_CASE:
                    case AMPM_OF_DAY_LOWER_CASE:
                        this.consumeAmPmOfDay(builder);
                        break;

                    // %Q - Number of milliseconds since 1970-01-01 00:00:00 UTC.
                    case MILLISECOND_SINCE_EPOCH:
                        this.consumeMillisecondSinceEpoch(builder);
                        break;

                    // %S - Second of the minute (00..59)
                    case SECOND_OF_MINUTE:
                        this.consumeSecondOfMinute(builder);
                        break;

                    // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                    case SECOND_SINCE_EPOCH:
                        this.consumeSecondSinceEpoch(builder);
                        break;

                    // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                    case WEEK_OF_YEAR_STARTING_WITH_SUNDAY:
                    case WEEK_OF_YEAR_STARTING_WITH_MONDAY:
                        this.consumeWeekOfYear(builder, token);
                        break;

                    // %u, %Ou - Day of the week (Monday is 1, 1..7)
                    case DAY_OF_WEEK_STARTING_WITH_MONDAY_1:
                        this.consumeDayOfWeekStartingWithMonday1(builder);
                        break;

                    // %V, %OV - Week number of the week-based year (01..53)
                    case WEEK_OF_WEEK_BASED_YEAR:
                        this.consumeWeekOfWeekBasedYear(builder);
                        break;

                    // %w - Day of the week (Sunday is 0, 0..6)
                    case DAY_OF_WEEK_STARTING_WITH_SUNDAY_0:
                        this.consumeDayOfWeekStartingWithSunday0(builder);
                        break;

                    // %Y, %EY - Year with century (can be negative, 4 digits at least)
                    //           -0001, 0000, 1995, 2009, 14292, etc.
                    case YEAR_WITH_CENTURY:
                        this.consumeYearWithCentury(builder, tokenWithNext.getNextToken());
                        break;

                    // %y, %Ey, %Oy - year % 100 (00..99)
                    case YEAR_WITHOUT_CENTURY:
                        this.consumeYearWithoutCentury(builder);
                        break;

                    // %Z - Time zone abbreviation name
                    // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                    //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                    //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                    //      %:::z - hour, minute and second offset from UTC (e.g. +09, +09:30, +09:30:30)
                    case TIME_ZONE_NAME:
                    case TIME_OFFSET:
                        this.consumeTimeZone(builder);
                        break;

                    default:  // Do nothing, and just pass through.
                }
            }
        }

        if (this.text.length() > this.pos) {
            builder.setLeftover(this.text.substring(this.pos, this.text.length()));
        }

        return builder.build();
    }

    private void consumeImmediateString(final String immediateString) {
        for (int i = 0; i < immediateString.length(); i++) {
            final char c = immediateString.charAt(i);
            if (isSpace(c)) {
                while (!isEndOfText(this.text, this.pos) && isSpace(this.text.charAt(this.pos))) {
                    this.pos++;
                }
            } else {
                if (isEndOfText(this.text, this.pos) || c != this.text.charAt(this.pos)) {
                    throw new RubyDateTimeParseException(
                            "Text '" + this.text + "' could not be parsed at index " + this.pos,
                            this.text,
                            this.pos);
                }
                this.pos++;
            }
        }
    }

    private void consumeWeekName(final Parsed.Builder builder) {
        final int dayIndex = findIndexInPatterns(DAY_NAMES);
        if (dayIndex < 0) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos,
                    this.text,
                    this.pos);
        }

        builder.setDayOfWeekStartingWithSunday0(dayIndex % 7);
        this.pos += DAY_NAMES[dayIndex].length();
    }

    private void consumeMonthOfYearName(final Parsed.Builder builder) {
        final int monIndex = findIndexInPatterns(MONTH_NAMES);
        if (monIndex < 0) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos,
                    this.text,
                    this.pos);
        }

        builder.setMonthOfYear(monIndex % 12 + 1);
        this.pos += MONTH_NAMES[monIndex].length();
    }

    private void consumeCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        if (isNumberPattern(nextToken)) {
            builder.setCentury(this.consumeDigitsInInt(2, 0, 99, "invalid century"));
        } else {
            // JSR-310 accepts only [-999_999_999, 999_999_999] as a year, not [Integer.MIN_VALUE, Integer.MAX_VALUE].
            // It is different from Ruby's Date._strptime / Time.strptime.
            builder.setCentury(this.consumeDigitsInInt(Integer.MAX_VALUE, 0, Year.MAX_VALUE / 100, "invalid century"));
        }
    }

    private void consumeDayOfMonth(final Parsed.Builder builder) {
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            builder.setDayOfMonth(this.consumeDigitsInInt(1, 1, 31, "invalid day of month"));
        } else {
            builder.setDayOfMonth(this.consumeDigitsInInt(2, 1, 31, "invalid day of month"));
        }
    }

    private void consumeWeekBasedYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        if (isNumberPattern(nextToken)) {
            builder.setWeekBasedYear(this.consumeDigitsInInt(4, 0, 9999, "invalid year"));
        } else {
            // JSR-310 accepts only [-999_999_999, 999_999_999] as a year, not [Integer.MIN_VALUE, Integer.MAX_VALUE].
            // It is different from Ruby's Date._strptime / Time.strptime.
            builder.setWeekBasedYear(this.consumeDigitsInInt(Integer.MAX_VALUE, 0, Year.MAX_VALUE, "invalid year"));
        }
    }

    private void consumeWeekBasedYearWithoutCentury(final Parsed.Builder builder) {
        builder.setWeekBasedYearWithoutCentury(this.consumeDigitsInInt(2, 0, 99, "invalid year"));
    }

    private void consumeHourOfDay(final Parsed.Builder builder) {
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            builder.setHour(this.consumeDigitsInInt(1, 0, 24, "invalid hour of day"));
        } else {
            builder.setHour(this.consumeDigitsInInt(2, 0, 24, "invalid hour of day"));
        }
    }

    private void consumeHourOfAmPm(final Parsed.Builder builder) {
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            builder.setHour(this.consumeDigitsInInt(1, 1, 12, "invalid hour of am/pm"));
        } else {
            builder.setHour(this.consumeDigitsInInt(2, 1, 12, "invalid hour of am/pm"));
        }
    }

    private void consumeDayOfYear(final Parsed.Builder builder) {
        builder.setDayOfYear(this.consumeDigitsInInt(3, 1, 366, "invalid day of year"));
    }

    private void consumeSubsecond(final Parsed.Builder builder, final FormatToken thisToken, final FormatToken nextToken) {
        final boolean negative;
        if (isSign(this.text, this.pos)) {
            negative = (this.text.charAt(this.pos) == '-');
            this.pos++;
        } else {
            negative = false;
        }

        final long value;
        if (isNumberPattern(nextToken)) {
            if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.MILLI_OF_SECOND) {
                value = this.consumeFractionalPartInInt(3, 9, "invalid fraction part of second");
            } else {
                value = this.consumeFractionalPartInInt(9, 9, "invalid fraction part of second");
            }
        } else {
            value = this.consumeFractionalPartInInt(Integer.MAX_VALUE, 9, "invalid fraction part of second");
        }

        builder.setNanoOfSecond((int) (!negative ? value : -value));
    }

    private void consumeMinuteOfHour(final Parsed.Builder builder) {
        builder.setMinuteOfHour(this.consumeDigitsInInt(2, 0, 59, "invalid minute of hour"));
    }

    private void consumeMonthOfYear(final Parsed.Builder builder) {
        builder.setMonthOfYear(this.consumeDigitsInInt(2, 1, 12, "invalid month of year"));
    }

    private void consumeAmPmOfDay(final Parsed.Builder builder) {
        final int meridIndex = findIndexInPatterns(MERID_NAMES);
        if (meridIndex < 0) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos,
                    this.text,
                    this.pos);
        }

        builder.setAmPmOfDay(meridIndex % 2 == 0 ? 0 : 12);
        this.pos += MERID_NAMES[meridIndex].length();
    }

    private void consumeMillisecondSinceEpoch(final Parsed.Builder builder) {
        final boolean negative;
        if (isMinus(this.text, this.pos)) {
            negative = true;
            this.pos++;
        } else {
            negative = false;
        }

        // JSR-310 accepts only [-1000000000-01-01T00:00Z, 1000000000-12-31T23:59:59.999999999Z] as an Instant.
        // It is different from Ruby's Date._strptime / Time.strptime.
        //
        // Note that Instant.MAX.toEpochMillis() > Long.MAX_VALUE. Long.MAX_VALUE is considered as its maximum.
        final long absoluteMillisecondSinceEpoch = this.consumeDigitsInLong(
                Integer.MAX_VALUE, 0, Long.MAX_VALUE, "invalid millisecond since epoch");
        builder.setInstantMilliseconds(negative ? -absoluteMillisecondSinceEpoch : absoluteMillisecondSinceEpoch);
    }

    private void consumeSecondOfMinute(final Parsed.Builder builder) {
        builder.setSecondOfMinute(this.consumeDigitsInInt(2, 0, 60, "invalid second of minute"));
    }

    private void consumeSecondSinceEpoch(final Parsed.Builder builder) {
        final boolean negative;
        if (isMinus(this.text, this.pos)) {
            negative = true;
            this.pos++;
        } else {
            negative = false;
        }

        // JSR-310 accepts only [-1000000000-01-01T00:00Z, 1000000000-12-31T23:59:59.999999999Z] as an Instant.
        // It is different from Ruby's Date._strptime / Time.strptime.
        //
        // Note that Instant.MAX.toEpochMillis() > Long.MAX_VALUE. Long.MAX_VALUE / 1000 is considered as its maximum
        // because it stores second since epoch internally as millisecond.
        final long absoluteSecondSinceEpoch = this.consumeDigitsInLong(
                Integer.MAX_VALUE, 0, Long.MAX_VALUE / 1000L, "invalid second since epoch");
        builder.setInstantMilliseconds((negative ? -absoluteSecondSinceEpoch : absoluteSecondSinceEpoch) * 1000L);
    }

    private void consumeWeekOfYear(final Parsed.Builder builder, final FormatToken thisToken) {
        if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY) {
            builder.setWeekOfYearStartingWithSunday(this.consumeDigitsInInt(2, 0, 53, "invalid week of year"));
        } else {
            builder.setWeekOfYearStartingWithMonday(this.consumeDigitsInInt(2, 0, 53, "invalid week of year"));
        }
    }

    private void consumeDayOfWeekStartingWithMonday1(final Parsed.Builder builder) {
        builder.setDayOfWeekStartingWithMonday1(this.consumeDigitsInInt(1, 1, 7, "invalid day of week"));
    }

    private void consumeWeekOfWeekBasedYear(final Parsed.Builder builder) {
        builder.setWeekOfWeekBasedYear(this.consumeDigitsInInt(2, 1, 53, "invalid week of year"));
    }

    private void consumeDayOfWeekStartingWithSunday0(final Parsed.Builder builder) {
        builder.setDayOfWeekStartingWithSunday0(this.consumeDigitsInInt(1, 0, 6, "invalid day of week"));
    }

    private void consumeYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        final boolean negative;
        if (isSign(this.text, this.pos)) {
            negative = (this.text.charAt(this.pos) == '-');
            this.pos++;
        } else {
            negative = false;
        }

        final int yearWithCentury;
        if (isNumberPattern(nextToken)) {
            yearWithCentury = this.consumeDigitsInInt(4, 0, 9999, "invalid year");
        } else {
            // JSR-310 accepts only [-999_999_999, 999_999_999] as a year, not [Integer.MIN_VALUE, Integer.MAX_VALUE].
            // It is different from Ruby's Date._strptime / Time.strptime.
            yearWithCentury = this.consumeDigitsInInt(Integer.MAX_VALUE, 0, Year.MAX_VALUE, "invalid year");
        }

        builder.setYear((int) (!negative ? yearWithCentury : -yearWithCentury));
    }

    private void consumeYearWithoutCentury(final Parsed.Builder builder) {
        builder.setYearWithoutCentury(this.consumeDigitsInInt(2, 0, 99, "invalid year"));
    }

    private void consumeTimeZone(final Parsed.Builder builder) {
        if (isEndOfText(this.text, this.pos)) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos,
                    this.text,
                    this.pos);
        }

        final Matcher matcher = ZONE_PARSE_REGEX.matcher(this.text.substring(this.pos));
        if (!matcher.find()) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos,
                    this.text,
                    this.pos);
        }

        final String zone = this.text.substring(this.pos, this.pos + matcher.end());
        builder.setTimeOffset(zone);
        this.pos += zone.length();
    }

    private long consumeDigitsInternal(
            final int digitsToConsume,
            final boolean isFraction,
            final int exponentInFraction,
            final long lowerLimit,
            final long upperLimit,
            final String messageOutOfRange) {
        long result = 0L;

        int digitsConsumed = 0;
        for (digitsConsumed = 0; digitsConsumed < digitsToConsume; digitsConsumed++) {
            if (isEndOfText(this.text, this.pos)) {
                break;
            }

            final char digitChar = text.charAt(this.pos);
            if (!isDigit(digitChar)) {
                break;
            }

            if (isFraction) {
                if (digitsConsumed < exponentInFraction) {
                    final int digit = toInt(digitChar);
                    result = result * 10 + digit;
                }
            } else {
                final int digit = toInt(digitChar);
                if (result > (Long.MAX_VALUE - digit) / 10) {
                    throw new RubyDateTimeParseException(
                            "Text '" + this.text + "' could not be parsed at index " + this.pos + ": " + messageOutOfRange,
                            this.text,
                            this.pos);
                }
                result = result * 10 + digit;
            }
            this.pos++;
        }

        if (digitsConsumed == 0) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos + ": no digits",
                    this.text,
                    this.pos);
        }

        if (isFraction && exponentInFraction > digitsConsumed) {
            result *= POW10[exponentInFraction - digitsConsumed];
        }

        if (lowerLimit > result || result > upperLimit) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos + ": " + messageOutOfRange,
                    this.text,
                    this.pos);
        }

        return result;
    }

    private int consumeDigitsInInt(
            final int digitsToConsume,
            final int lowerLimit,
            final int upperLimit,
            final String messageOutOfRange) {
        return (int) this.consumeDigitsInternal(digitsToConsume, false, 0, lowerLimit, upperLimit, messageOutOfRange);
    }

    private long consumeDigitsInLong(
            final int digitsToConsume,
            final long lowerLimit,
            final long upperLimit,
            final String messageOutOfRange) {
        return this.consumeDigitsInternal(digitsToConsume, false, 0, lowerLimit, upperLimit, messageOutOfRange);
    }

    private int consumeFractionalPartInInt(
            final int digitsToConsume,
            final int exponentInFraction,
            final String messageOutOfRange) {
        return (int) this.consumeDigitsInternal(
                digitsToConsume,
                true,
                exponentInFraction,
                0L,
                POW10[exponentInFraction] - 1,
                messageOutOfRange);
    }

    private int findIndexInPatterns(final String[] patterns) {
        if (isEndOfText(text, this.pos)) {
            return -1;
        }

        for (int i = 0; i < patterns.length; i++) {
            final String pattern = patterns[i];
            final int length = pattern.length();
            if (!isEndOfText(this.text, this.pos + length - 1)
                        && pattern.equalsIgnoreCase(this.text.substring(this.pos, this.pos + length))) {
                return i;
            }
        }

        return -1;  // Case that text doesn't match at any patterns.
    }

    private static boolean isNumberPattern(final FormatToken token) {
        if (token == null) {
            return false;
        } else if ((!token.isDirective()) && isDigit(((FormatToken.Immediate) token).getContent().charAt(0))) {
            return true;
        } else if (token.isDirective() && ((FormatToken.Directive) token).getFormatDirective().isNumeric()) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSpace(final char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000b' || c == '\f' || c == '\r';
    }

    private static boolean isDigit(final char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isEndOfText(final String text, final int pos) {
        return pos >= text.length();
    }

    private static boolean isSign(final String text, final int pos) {
        return !isEndOfText(text, pos) && (text.charAt(pos) == '+' || text.charAt(pos) == '-');
    }

    private static boolean isMinus(final String text, final int pos) {
        return !isEndOfText(text, pos) && text.charAt(pos) == '-';
    }

    private static boolean isBlank(final String text, final int pos) {
        return !isEndOfText(text, pos) && text.charAt(pos) == ' ';
    }

    private static int toInt(final char c) {
        return c - '0';
    }

    /**
     * Regular expression that matches time zones.
     *
     * <p>It comes from Ruby v2.5.1.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_1/ext/date/date_strptime.c?view=markup#l571">ext/date/date_strptime.c</a>
     */
    private static final Pattern ZONE_PARSE_REGEX =
        Pattern.compile("\\A("
                                + "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?"
                                + "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b"
                                + "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b"
                                + ")",
                        Pattern.CASE_INSENSITIVE);

    private static final String[] DAY_NAMES = new String[] {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static final String[] MONTH_NAMES = new String[] {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static final String[] MERID_NAMES = new String[] {
        "am", "pm", "a.m.", "p.m."
    };

    private static final long[] POW10 = {
        1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L,
        10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L,
        1_000_000_000_000_000L, 10_000_000_000_000_000L, 100_000_000_000_000_000L, 1_000_000_000_000_000_000L,
    };

    private final String text;

    private int pos;
}

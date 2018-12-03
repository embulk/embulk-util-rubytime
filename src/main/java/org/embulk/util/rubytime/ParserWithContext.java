package org.embulk.util.rubytime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParserWithContext {
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
            builder.setCentury((int) this.consumeDigits(2));
        } else {
            builder.setCentury((int) this.consumeDigitsMax());
        }
    }

    private void consumeDayOfMonth(final Parsed.Builder builder) {
        final long dayOfMonth;
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            dayOfMonth = this.consumeDigits(1);
        } else {
            dayOfMonth = this.consumeDigits(2);
        }

        this.throwIfOutOfRange(dayOfMonth, 1, 31, "invalid day of month");
        builder.setDayOfMonth((int) dayOfMonth);
    }

    private void consumeWeekBasedYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        if (isNumberPattern(nextToken)) {
            builder.setWeekBasedYear((int) this.consumeDigits(4));
        } else {
            builder.setWeekBasedYear((int) this.consumeDigitsMax());
        }
    }

    private void consumeWeekBasedYearWithoutCentury(final Parsed.Builder builder) {
        final long weekBasedYearWithoutCentury = this.consumeDigits(2);

        this.throwIfOutOfRange(weekBasedYearWithoutCentury, 0, 99, "invalid year");
        builder.setWeekBasedYearWithoutCentury((int) weekBasedYearWithoutCentury);
    }

    private void consumeHourOfDay(final Parsed.Builder builder) {
        final long hourOfDay;
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            hourOfDay = this.consumeDigits(1);
        } else {
            hourOfDay = this.consumeDigits(2);
        }

        this.throwIfOutOfRange(hourOfDay, 0, 24, "invalid hour of day");
        builder.setHour((int) hourOfDay);
    }

    private void consumeHourOfAmPm(final Parsed.Builder builder) {
        final long hourOfAmPm;
        if (isBlank(this.text, this.pos)) {
            this.pos += 1;  // Blank
            hourOfAmPm = this.consumeDigits(1);
        } else {
            hourOfAmPm = this.consumeDigits(2);
        }

        this.throwIfOutOfRange(hourOfAmPm, 1, 12, "invalid hour of am/pm");
        builder.setHour((int) hourOfAmPm);
    }

    private void consumeDayOfYear(final Parsed.Builder builder) {
        final long dayOfYear = this.consumeDigits(3);

        this.throwIfOutOfRange(dayOfYear, 1, 365, "invalid day of year");
        builder.setDayOfYear((int) dayOfYear);
    }

    private void consumeSubsecond(final Parsed.Builder builder, final FormatToken thisToken, final FormatToken nextToken) {
        final boolean negative;
        if (isSign(this.text, this.pos)) {
            negative = (this.text.charAt(this.pos) == '-');
            this.pos++;
        } else {
            negative = false;
        }

        final int initialPosition = this.pos;

        final long value;
        if (isNumberPattern(nextToken)) {
            if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.MILLI_OF_SECOND) {
                value = this.consumeDigits(3);
            } else {
                value = this.consumeDigits(9);
            }
        } else {
            value = this.consumeDigitsMax();
        }

        // TODO: Fix cases of subseconds longer than 9.
        // TODO: Stop using Math.pow(double, double).
        builder.setNanoOfSecond((int) (!negative ? value : -value) * (int) Math.pow(10, 9 - (this.pos - initialPosition)));
    }

    private void consumeMinuteOfHour(final Parsed.Builder builder) {
        final long minuteOfHour = this.consumeDigits(2);

        this.throwIfOutOfRange(minuteOfHour, 0, 59, "invalid minute of hour");
        builder.setMinuteOfHour((int) minuteOfHour);
    }

    private void consumeMonthOfYear(final Parsed.Builder builder) {
        final long monthOfYear = this.consumeDigits(2);

        this.throwIfOutOfRange(monthOfYear, 1, 12, "invalid month of year");
        builder.setMonthOfYear((int) monthOfYear);
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

        final long absoluteMillisecondSinceEpoch = this.consumeDigitsMax();
        builder.setInstantMilliseconds(negative ? -absoluteMillisecondSinceEpoch : absoluteMillisecondSinceEpoch);
    }

    private void consumeSecondOfMinute(final Parsed.Builder builder) {
        final long secondOfMinute = this.consumeDigits(2);

        this.throwIfOutOfRange(secondOfMinute, 0, 60, "invalid second of minute");
        builder.setSecondOfMinute((int) secondOfMinute);
    }

    private void consumeSecondSinceEpoch(final Parsed.Builder builder) {
        final boolean negative;
        if (isMinus(this.text, this.pos)) {
            negative = true;
            this.pos++;
        } else {
            negative = false;
        }

        final long absoluteSecondSinceEpoch = this.consumeDigitsMax();
        builder.setInstantMilliseconds((negative ? -absoluteSecondSinceEpoch : absoluteSecondSinceEpoch) * 1000);
    }

    private void consumeWeekOfYear(final Parsed.Builder builder, final FormatToken thisToken) {
        final long weekOfYear = this.consumeDigits(2);
        this.throwIfOutOfRange(weekOfYear, 0, 53, "invalid week of year");

        if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY) {
            builder.setWeekOfYearStartingWithSunday((int) weekOfYear);
        } else {
            builder.setWeekOfYearStartingWithMonday((int) weekOfYear);
        }
    }

    private void consumeDayOfWeekStartingWithMonday1(final Parsed.Builder builder) {
        final long dayOfWeek = this.consumeDigits(1);

        this.throwIfOutOfRange(dayOfWeek, 1, 7, "invalid day of week");
        builder.setDayOfWeekStartingWithMonday1((int) dayOfWeek);
    }

    private void consumeWeekOfWeekBasedYear(final Parsed.Builder builder) {
        final long weekOfWeekBasedYear = this.consumeDigits(2);

        this.throwIfOutOfRange(weekOfWeekBasedYear, 1, 53, "invalid week of year");
        builder.setWeekOfWeekBasedYear((int) weekOfWeekBasedYear);
    }

    private void consumeDayOfWeekStartingWithSunday0(final Parsed.Builder builder) {
        final long dayOfWeek = this.consumeDigits(1);

        this.throwIfOutOfRange(dayOfWeek, 0, 6, "invalid week of year");
        builder.setDayOfWeekStartingWithSunday0((int) dayOfWeek);
    }

    private void consumeYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        final boolean negative;
        if (isSign(this.text, this.pos)) {
            negative = (this.text.charAt(this.pos) == '-');
            this.pos++;
        } else {
            negative = false;
        }

        final long yearWithCentury;
        if (isNumberPattern(nextToken)) {
            yearWithCentury = this.consumeDigits(4);
        } else {
            yearWithCentury = this.consumeDigitsMax();
        }

        builder.setYear((int) (!negative ? yearWithCentury : -yearWithCentury));
    }

    private void consumeYearWithoutCentury(final Parsed.Builder builder) {
        final long yearWithoutCentury = this.consumeDigits(2);

        this.throwIfOutOfRange(yearWithoutCentury, 0, 99, "invalid year");
        builder.setYearWithoutCentury((int) yearWithoutCentury);
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

    private long consumeDigits(final int length) {
        final int initialPosition = this.pos;

        long result = 0L;
        for (int i = 0; i < length; i++) {
            if (isEndOfText(this.text, this.pos)) {
                break;
            }

            final char c = text.charAt(this.pos);
            if (!isDigit(c)) {
                break;
            }
            result = result * 10 + toInt(c);
            this.pos += 1;
        }

        if (this.pos == initialPosition) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos + ": no digits",
                    this.text,
                    this.pos);
        }

        return result;
    }

    private long consumeDigitsMax() {
        return this.consumeDigits(Integer.MAX_VALUE);
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

    private void throwIfOutOfRange(final long value, final int lower, final int upper, final String message) {
        if (lower > value || value > upper) {
            throw new RubyDateTimeParseException(
                    "Text '" + this.text + "' could not be parsed at index " + this.pos + ": " + message,
                    this.text,
                    this.pos);
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

    private final String text;

    private int pos;
}

package org.embulk.util.rubytime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParserWithContext {
    ParserWithContext(final CharSequence text) {
        this.text = text.toString();

        this.pos = 0;
        this.fail = false;
    }

    Parsed parse(final Format format) {
        final Parsed.Builder builder = Parsed.builder(this.text);

        for (final Format.TokenWithNext tokenWithNext : format) {
            final FormatToken token = tokenWithNext.getToken();

            if (!token.isDirective()) {
                final FormatToken.Immediate stringToken = (FormatToken.Immediate) token;
                final String str = stringToken.getContent();
                for (int i = 0; i < str.length(); i++) {
                    final char c = str.charAt(i);
                    if (isSpace(c)) {
                        while (!isEndOfText(text, pos) && isSpace(text.charAt(pos))) {
                            pos++;
                        }
                    } else {
                        if (isEndOfText(text, pos) || c != text.charAt(pos)) {
                            fail = true;
                        }
                        pos++;
                    }
                }
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

        if (fail) {
            return null;
        }

        if (text.length() > pos) {
            builder.setLeftover(text.substring(pos, text.length()));
        }

        return builder.build();
    }

    private void consumeWeekName(final Parsed.Builder builder) {
        final int dayIndex = findIndexInPatterns(DAY_NAMES);
        if (dayIndex >= 0) {
            builder.setDayOfWeekStartingWithSunday0(dayIndex % 7);
            pos += DAY_NAMES[dayIndex].length();
        } else {
            fail = true;
        }
    }

    private void consumeMonthOfYearName(final Parsed.Builder builder) {
        final int monIndex = findIndexInPatterns(MONTH_NAMES);
        if (monIndex >= 0) {
            builder.setMonthOfYear(monIndex % 12 + 1);
            pos += MONTH_NAMES[monIndex].length();
        } else {
            fail = true;
        }
    }

    private void consumeCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        final long cent;
        if (isNumberPattern(nextToken)) {
            cent = readDigits(2);
        } else {
            cent = readDigitsMax();
        }
        builder.setCentury((int) cent);
    }

    private void consumeDayOfMonth(final Parsed.Builder builder) {
        final long day;
        if (isBlank(text, pos)) {
            pos += 1;  // blank
            day = readDigits(1);
        } else {
            day = readDigits(2);
        }

        if (!validRange(day, 1, 31)) {
            fail = true;
        }
        builder.setDayOfMonth((int) day);
    }

    private void consumeWeekBasedYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        final long year;
        if (isNumberPattern(nextToken)) {
            year = readDigits(4);
        } else {
            year = readDigitsMax();
        }
        builder.setWeekBasedYear((int) year);
    }

    private void consumeWeekBasedYearWithoutCentury(final Parsed.Builder builder) {
        final long v = readDigits(2);
        if (!validRange(v, 0, 99)) {
            fail = true;
        }
        builder.setWeekBasedYearWithoutCentury((int) v);
    }

    private void consumeHourOfDay(final Parsed.Builder builder) {
        final long hour;
        if (isBlank(text, pos)) {
            pos += 1;  // blank
            hour = readDigits(1);
        } else {
            hour = readDigits(2);
        }

        if (!validRange(hour, 0, 24)) {
            fail = true;
        }
        builder.setHour((int) hour);
    }

    private void consumeHourOfAmPm(final Parsed.Builder builder) {
        final long hour;
        if (isBlank(text, pos)) {
            pos += 1; // blank
            hour = readDigits(1);
        } else {
            hour = readDigits(2);
        }

        if (!validRange(hour, 1, 12)) {
            fail = true;
        }
        builder.setHour((int) hour);
    }

    private void consumeDayOfYear(final Parsed.Builder builder) {
        final long day = readDigits(3);
        if (!validRange(day, 1, 365)) {
            fail = true;
        }
        builder.setDayOfYear((int) day);
    }

    private void consumeSubsecond(final Parsed.Builder builder, final FormatToken thisToken, final FormatToken nextToken) {
        boolean negative = false;
        if (isSign(text, pos)) {
            negative = text.charAt(pos) == '-';
            pos++;
        }

        final long v;
        final int initPos = pos;
        if (isNumberPattern(nextToken)) {
            if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.MILLI_OF_SECOND) {
                v = readDigits(3);
            } else {
                v = readDigits(9);
            }
        } else {
            v = readDigitsMax();
        }

        builder.setNanoOfSecond((int) (!negative ? v : -v) * (int) Math.pow(10, 9 - (pos - initPos)));
    }

    private void consumeMinuteOfHour(final Parsed.Builder builder) {
        final long min = readDigits(2);
        if (!validRange(min, 0, 59)) {
            fail = true;
        }
        builder.setMinuteOfHour((int) min);
    }

    private void consumeMonthOfYear(final Parsed.Builder builder) {
        final long mon = readDigits(2);
        if (!validRange(mon, 1, 12)) {
            fail = true;
        }
        builder.setMonthOfYear((int) mon);
    }

    private void consumeAmPmOfDay(final Parsed.Builder builder) {
        final int meridIndex = findIndexInPatterns(MERID_NAMES);
        if (meridIndex >= 0) {
            builder.setAmPmOfDay(meridIndex % 2 == 0 ? 0 : 12);
            pos += MERID_NAMES[meridIndex].length();
        } else {
            fail = true;
        }
    }

    private void consumeMillisecondSinceEpoch(final Parsed.Builder builder) {
        boolean negative = false;
        if (isMinus(text, pos)) {
            negative = true;
            pos++;
        }

        final long sec = (negative ? -readDigitsMax() : readDigitsMax());

        builder.setInstantMilliseconds(sec);
    }

    private void consumeSecondOfMinute(final Parsed.Builder builder) {
        final long sec = readDigits(2);
        if (!validRange(sec, 0, 60)) {
            fail = true;
        }
        builder.setSecondOfMinute((int) sec);
    }

    private void consumeSecondSinceEpoch(final Parsed.Builder builder) {
        boolean negative = false;
        if (isMinus(text, pos)) {
            negative = true;
            pos++;
        }

        final long sec = readDigitsMax();
        builder.setInstantMilliseconds((!negative ? sec : -sec) * 1000);
    }

    private void consumeWeekOfYear(final Parsed.Builder builder, final FormatToken thisToken) {
        final long week = readDigits(2);
        if (!validRange(week, 0, 53)) {
            fail = true;
        }

        if (((FormatToken.Directive) thisToken).getFormatDirective() == FormatDirective.WEEK_OF_YEAR_STARTING_WITH_SUNDAY) {
            builder.setWeekOfYearStartingWithSunday((int) week);
        } else {
            builder.setWeekOfYearStartingWithMonday((int) week);
        }
    }

    private void consumeDayOfWeekStartingWithMonday1(final Parsed.Builder builder) {
        final long day = readDigits(1);
        if (!validRange(day, 1, 7)) {
            fail = true;
        }
        builder.setDayOfWeekStartingWithMonday1((int) day);
    }

    private void consumeWeekOfWeekBasedYear(final Parsed.Builder builder) {
        final long week = readDigits(2);
        if (!validRange(week, 1, 53)) {
            fail = true;
        }
        builder.setWeekOfWeekBasedYear((int) week);
    }

    private void consumeDayOfWeekStartingWithSunday0(final Parsed.Builder builder) {
        final long day = readDigits(1);
        if (!validRange(day, 0, 6)) {
            fail = true;
        }
        builder.setDayOfWeekStartingWithSunday0((int) day);
    }

    private void consumeYearWithCentury(final Parsed.Builder builder, final FormatToken nextToken) {
        boolean negative = false;
        if (isSign(text, pos)) {
            negative = text.charAt(pos) == '-';
            pos++;
        }

        final long year;
        if (isNumberPattern(nextToken)) {
            year = readDigits(4);
        } else {
            year = readDigitsMax();
        }

        builder.setYear((int) (!negative ? year : -year));
    }

    private void consumeYearWithoutCentury(final Parsed.Builder builder) {
        final long y = readDigits(2);
        if (!validRange(y, 0, 99)) {
            fail = true;
        }
        builder.setYearWithoutCentury((int) y);
    }

    private void consumeTimeZone(final Parsed.Builder builder) {
        if (isEndOfText(text, pos)) {
            fail = true;
            return;
        }

        final Matcher m = ZONE_PARSE_REGEX.matcher(text.substring(pos));
        if (m.find()) {
            // zone
            String zone = text.substring(pos, pos + m.end());
            builder.setTimeOffset(zone);
            pos += zone.length();
        } else {
            fail = true;
        }
    }

    /**
     * The method is reimplemented based on read_digits from Ruby v2.3.1's ext/date/date_strptime.c.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l77">read_digits</a>
     */
    private long readDigits(final int len) {
        char c;
        long v = 0;
        final int initPos = pos;

        for (int i = 0; i < len; i++) {
            if (isEndOfText(text, pos)) {
                break;
            }

            c = text.charAt(pos);
            if (!isDigit(c)) {
                break;
            } else {
                v = v * 10 + toInt(c);
            }
            pos += 1;
        }

        if (pos == initPos) {
            fail = true;
        }

        return v;
    }

    /**
     * The method is reimplemented based on READ_DIGITS_MAX from Ruby v2.3.1's ext/date/date_strptime.c.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l137">READ_DIGITS_MAX</a>
     */
    private long readDigitsMax() {
        return readDigits(Integer.MAX_VALUE);
    }

    /**
     * Returns -1 if text doesn't match with patterns.
     */
    private int findIndexInPatterns(final String[] patterns) {
        if (isEndOfText(text, pos)) {
            return -1;
        }

        for (int i = 0; i < patterns.length; i++) {
            final String pattern = patterns[i];
            final int len = pattern.length();
            if (!isEndOfText(text, pos + len - 1)
                    && pattern.equalsIgnoreCase(text.substring(pos, pos + len))) { // strncasecmp
                return i;
            }
        }

        return -1; // text doesn't match at any patterns.
    }

    /**
     * The method is reimplemented based on num_pattern_p from Ruby v2.3.1's ext/date/date_strptime.c.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l58">num_pattern_p</a>
     */
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

    /**
     * The method is reimplemented based on valid_range_p from Ruby v2.3.1's ext/date/date_strptime.c.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l139">valid_range_p</a>
     */
    private static boolean validRange(long v, int lower, int upper) {
        return lower <= v && v <= upper;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000b' || c == '\f' || c == '\r';
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isEndOfText(String text, int pos) {
        return pos >= text.length();
    }

    private static boolean isSign(String text, int pos) {
        return !isEndOfText(text, pos) && (text.charAt(pos) == '+' || text.charAt(pos) == '-');
    }

    private static boolean isMinus(String text, int pos) {
        return !isEndOfText(text, pos) && text.charAt(pos) == '-';
    }

    private static boolean isBlank(String text, int pos) {
        return !isEndOfText(text, pos) && text.charAt(pos) == ' ';
    }

    private static int toInt(char c) {
        return c - '0';
    }

    /**
     * The regular expression is reimplemented from Ruby v2.3.1's ext/date/date_strptime.c.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/ext/date/date_strptime.c?view=markup#l571">pat_source</a>
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
    private boolean fail;
}

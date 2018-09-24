package org.embulk.util.rubytime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRational;
import org.jruby.RubySymbol;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestDateZones {
    @ParameterizedTest
    @MethodSource("provideToOffsetInSecondsParams")
    public void testToOffsetInSecondsWithCRuby(final String value) throws Exception {
        assertToOffsetInSecondsCRuby(value);
    }

    @Test
    public void testNormalize() {
        assertNormalize("abc", "ABC");
        assertNormalize("abc ", "ABC");
        assertNormalize("abc  ", "ABC");
        assertNormalize(" abc", "ABC");
        assertNormalize("  abc", "ABC");
        assertNormalize(" abc ", "ABC");
        assertNormalize("  abc  ", "ABC");
        assertNormalize("a bc", "A BC");
        assertNormalize("a  bc", "A BC");
        assertNormalize("  a  bc d  ef  ", "A BC D EF");
    }

    @ParameterizedTest
    @MethodSource("provideParseOffsetParams")
    public void testParseOffsetWithCRuby(final String value) throws Exception {
        assertParseOffsetCRuby(value);
    }

    @ParameterizedTest
    @MethodSource("provideParseOffsetParams")
    public void testParseOffsetWithJRuby(final String value) {
        // Offsets in OFFSETS_IGNORED_JRUBY are incompliant in JRuby.
        assumeTrue(!OFFSETS_IGNORED_JRUBY.contains(value));
        assertParseOffsetJRuby(value);
    }

    @Test
    public void testParseOffsetTooLongFraction() {
        assertThrows(
                NumberFormatException.class,
                () -> { DateZones.parseOffsetForTesting("UTC+19.001953125"); });
        assertThrows(
                NumberFormatException.class,
                () -> { DateZones.parseOffsetForTesting("UTC+19.0009765625"); });
        assertThrows(
                NumberFormatException.class,
                () -> { DateZones.parseOffsetForTesting("UTC+19.0000111111"); });
    }

    @Test
    public void testParseUnsignedIntUntilNonDigit() {
        assertParseUnsignedInt("abcd19381fjs", 4, 19381, 9);
        assertParseUnsignedInt("19084:219", 0, 19084, 5);
    }

    /**
     * The data source is a copy from "zonetab.list" of Ruby 2.5.0.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/zonetab.list?view=markup">zonetab.list</a>
     */
    @ParameterizedTest
    @CsvSource({
            "ut,   0*3600",
            "gmt,  0*3600",
            "est, -5*3600",
            "edt, -4*3600",
            "cst, -6*3600",
            "cdt, -5*3600",
            "mst, -7*3600",
            "mdt, -6*3600",
            "pst, -8*3600",
            "pdt, -7*3600",
            "a,    1*3600",
            "b,    2*3600",
            "c,    3*3600",
            "d,    4*3600",
            "e,    5*3600",
            "f,    6*3600",
            "g,    7*3600",
            "h,    8*3600",
            "i,    9*3600",
            "k,   10*3600",
            "l,   11*3600",
            "m,   12*3600",
            "n,   -1*3600",
            "o,   -2*3600",
            "p,   -3*3600",
            "q,   -4*3600",
            "r,   -5*3600",
            "s,   -6*3600",
            "t,   -7*3600",
            "u,   -8*3600",
            "v,   -9*3600",
            "w,  -10*3600",
            "x,  -11*3600",
            "y,  -12*3600",
            "z,    0*3600",
            "utc,  0*3600",
            "wet,  0*3600",
            "at,  -2*3600",
            "brst,-2*3600",
            "ndt, -(2*3600+1800)",
            "art, -3*3600",
            "adt, -3*3600",
            "brt, -3*3600",
            "clst,-3*3600",
            "nst, -(3*3600+1800)",
            "ast, -4*3600",
            "clt, -4*3600",
            "akdt,-8*3600",
            "ydt, -8*3600",
            "akst,-9*3600",
            "hadt,-9*3600",
            "hdt, -9*3600",
            "yst, -9*3600",
            "ahst,-10*3600",
            "cat,-10*3600",
            "hast,-10*3600",
            "hst,-10*3600",
            "nt,  -11*3600",
            "idlw,-12*3600",
            "bst,  1*3600",
            "cet,  1*3600",
            "fwt,  1*3600",
            "met,  1*3600",
            "mewt, 1*3600",
            "mez,  1*3600",
            "swt,  1*3600",
            "wat,  1*3600",
            "west, 1*3600",
            "cest, 2*3600",
            "eet,  2*3600",
            "fst,  2*3600",
            "mest, 2*3600",
            "mesz, 2*3600",
            "sast, 2*3600",
            "sst,  2*3600",
            "bt,   3*3600",
            "eat,  3*3600",
            "eest, 3*3600",
            "msk,  3*3600",
            "msd,  4*3600",
            // "zp4,  4*3600",
            // "zp5,  5*3600",
            "ist,  (5*3600+1800)",
            // "zp6,  6*3600",
            "wast, 7*3600",
            "cct,  8*3600",
            "sgt,  8*3600",
            "wadt, 8*3600",
            "jst,  9*3600",
            "kst,  9*3600",
            "east,10*3600",
            "gst, 10*3600",
            "eadt,11*3600",
            "idle,12*3600",
            "nzst,12*3600",
            "nzt, 12*3600",
            "nzdt,13*3600",
            "afghanistan,             16200",
            "alaskan,                -32400",
            "arab,                    10800",
            "arabian,                 14400",
            "arabic,                  10800",
            "atlantic,               -14400",
            "aus central,             34200",
            "aus eastern,             36000",
            "azores,                  -3600",
            "canada central,         -21600",
            "cape verde,              -3600",
            "caucasus,                14400",
            "cen. australia,          34200",
            "central america,        -21600",
            "central asia,            21600",
            "central europe,           3600",
            "central european,         3600",
            "central pacific,         39600",
            "central,                -21600",
            "china,                   28800",
            "dateline,               -43200",
            "e. africa,               10800",
            "e. australia,            36000",
            "e. europe,                7200",
            "e. south america,       -10800",
            "eastern,                -18000",
            "egypt,                    7200",
            "ekaterinburg,            18000",
            "fiji,                    43200",
            "fle,                      7200",
            "greenland,              -10800",
            "greenwich,                   0",
            "gtb,                      7200",
            "hawaiian,               -36000",
            "india,                   19800",
            "iran,                    12600",
            "jerusalem,                7200",
            "korea,                   32400",
            "mexico,                 -21600",
            // "mid-atlantic,            -7200",
            "mountain,               -25200",
            "myanmar,                 23400",
            "n. central asia,         21600",
            "nepal,                   20700",
            "new zealand,             43200",
            "newfoundland,           -12600",
            "north asia east,         28800",
            "north asia,              25200",
            "pacific sa,             -14400",
            "pacific,                -28800",
            "romance,                  3600",
            "russian,                 10800",
            "sa eastern,             -10800",
            "sa pacific,             -18000",
            "sa western,             -14400",
            "samoa,                  -39600",
            "se asia,                 25200",
            "malay peninsula,         28800",
            "south africa,             7200",
            "sri lanka,               21600",
            "taipei,                  28800",
            "tasmania,                36000",
            "tokyo,                   32400",
            "tonga,                   46800",
            "us eastern,             -18000",
            "us mountain,            -25200",
            "vladivostok,             36000",
            "w. australia,            28800",
            "w. central africa,        3600",
            "w. europe,                3600",
            "west asia,               18000",
            "west pacific,            36000",
            "yakutsk,                 32400",
    })
    public void testMatchingZoneTab(final String name, final String expectedOffsetInString) {
        final Matcher matcherHourHalf = HOUR_HALF.matcher(expectedOffsetInString);
        final Matcher matcherHour = HOUR.matcher(expectedOffsetInString);
        final Matcher matcherSecond = SECOND.matcher(expectedOffsetInString);

        final int expectedOffset;
        if (matcherHourHalf.matches()) {
            final boolean isNegative = matcherHourHalf.group("negative").equals("-");
            final int hour = Integer.parseInt(matcherHourHalf.group("hour"));
            expectedOffset = (isNegative ? -1 : 1) * (hour * 3600 + 1800);
        } else if (matcherHour.matches()) {
            final boolean isNegative = matcherHour.group("negative").equals("-");
            final int hour = Integer.parseInt(matcherHour.group("hour"));
            expectedOffset = (isNegative ? -1 : 1) * (hour * 3600);
        } else if (matcherSecond.matches()) {
            final boolean isNegative = matcherSecond.group("negative").equals("-");
            final int second = Integer.parseInt(matcherSecond.group("second"));
            expectedOffset = (isNegative ? -1 : 1) * second;
        } else {
            fail("The expected data \"" + expectedOffsetInString + "\" is in an unexpected format.");
            return;
        }

        assertEquals(expectedOffset, DateZones.mapZoneNameToOffsetInSecondsForTesting(name.toUpperCase()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dummy",
            "@@@@@",
            "",
            "%d",
            "\n",
            "   ",
            "\t",
            "!",
            "?",
            "*",
        })
    public void testUnmatchingZoneTab(final String name) {
        assertEquals(Integer.MIN_VALUE, DateZones.mapZoneNameToOffsetInSecondsForTesting(name.toUpperCase()));
    }

    private static void assertToOffsetInSecondsCRuby(final String value) throws Exception {
        // A fraction part of :offset is rounded down.
        final String oneLiner = String.format(
                "require 'date'; " +
                "hashDateTime = DateTime._strptime(\"%s\", \"%%z\"); " +
                "hashDateTime[:offset] = hashDateTime[:offset].to_i if hashDateTime&.fetch(:offset).is_a?(Rational); " +
                "puts hashDateTime&.fetch(:offset, nil).inspect; " +
                "puts hashDateTime&.fetch(:leftover, nil).inspect",
                value);
        final List<String> result = cruby.callOneLiner(oneLiner);
        assertTrue(result.size() == 2);

        final int actual = DateZones.toOffsetInSeconds(value);
        if (result.get(0).equals("nil") || !result.get(1).equals("nil")) {
            assertEquals(Integer.MIN_VALUE, actual);
        } else {
            assertEquals(Integer.parseInt(result.get(0)), actual);
        }
    }

    private static void assertParseOffsetCRuby(final String value) throws Exception {
        // A fraction part of :offset is rounded down.
        final String oneLiner = String.format(
                "require 'date'; " +
                "hashDateTime = DateTime._strptime(\"%s\", \"%%z\"); " +
                "hashDateTime[:offset] = hashDateTime[:offset].to_i if hashDateTime&.fetch(:offset).is_a?(Rational); " +
                "puts hashDateTime&.fetch(:offset, nil).inspect; " +
                "puts hashDateTime&.fetch(:leftover, nil).inspect",
                value);
        final List<String> result = cruby.callOneLiner(oneLiner);
        assertTrue(result.size() == 2);

        // We don't test parseOffset against strings which will have leftover.
        // The typical case is a string ending with a colon, such as "UTC+13:".
        // In such cases, the colon is filtered by the regular expression
        // before the string is passed to CRuby's date_zone_to_diff.
        // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_strptime.c?view=markup#l571
        assertTrue(result.get(1).equals("nil"));

        final int actual = DateZones.parseOffsetForTesting(value);
        if (result.get(0).equals("nil")) {
            assertEquals(Integer.MIN_VALUE, actual);
        } else {
            assertEquals(Integer.parseInt(result.get(0)), actual);
        }
    }

    private static void assertParseOffsetJRuby(final String value) {
        final String scriptlet = String.format("DateTime._strptime(\"%s\", \"%%z\")", value);
        final Object result = jruby.runScriptlet(scriptlet);

        final int actual = DateZones.parseOffsetForTesting(value);
        if (result == null) {
            assertEquals(Integer.MIN_VALUE, actual);
        } else {
            assertTrue(result instanceof RubyHash);
            final RubyHash parsedRubyHash = (RubyHash) result;
            assumeTrue(!parsedRubyHash.has_key_p(leftoverSymbol).isTrue());
            if (parsedRubyHash.has_key_p(offsetSymbol).isTrue()) {
                final IRubyObject offsetObject = parsedRubyHash.fastARef(offsetSymbol);
                assertTrue(offsetObject instanceof RubyFixnum);
                final RubyFixnum offsetFixnum = (RubyFixnum) offsetObject;
                final int offsetExpectedJRuby = (int) offsetFixnum.getLongValue();
                assertEquals(offsetExpectedJRuby, actual);
            } else {
                assertEquals(Integer.MIN_VALUE, actual);
            }
        }
    }

    private static void assertParseUnsignedInt(
            final String string,
            final int oldIndex,
            final int value,
            final int newIndex) {
        final long result = DateZones.parseUnsignedIntUntilNonDigitForTesting(string, oldIndex);
        assertEquals(value, (int) (result & 0xffffffffL));
        assertEquals(newIndex, (int) (result >> 32));
    }

    private static void assertNormalize(final String name, final String expected) {
        assertEquals(expected, DateZones.normalizeForTesting(name));
    }

    @BeforeAll
    public static void loadJRubyScriptingContainerVanilla() {
        cruby = new CRubyCaller();

        jruby = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        jruby.runScriptlet("require 'date'");
        leftoverSymbol = RubySymbol.newSymbol(jruby.getProvider().getRuntime(), "leftover");
        offsetSymbol = RubySymbol.newSymbol(jruby.getProvider().getRuntime(), "offset");
    }

    private static Stream<String> provideParseOffsetParams() {
        return Arrays.stream(OFFSETS);
    }

    private static Stream<String> provideToOffsetInSecondsParams() {
        return Stream.concat(Arrays.stream(OFFSETS), Arrays.stream(ZONES));
    }

    private static final String[] OFFSETS = {
        "DUMMY",
        "9",
        "13",
        "78",
        "127",
        "19248",
        "482844",
        "9184812",
        "19184812",
        "+9",
        "+13",
        "+78",
        "+127",
        "+19248",
        "+482844",
        "+9184812",
        "+19184812",
        "-9",
        "-13",
        "-78",
        "-127",
        "-19248",
        "-482844",
        "-9184812",
        "-19184812",
        "UTC",
        "GMT",
        "UTC+9",
        "UTC+9:2",
        "UTC+9:2:3",
        "UTC+12",
        "UTC+12:13",
        "UTC+12:13:14",
        "UTC+99",
        "UTC+99:13",
        "UTC+99:13:14",
        "UTC+912",
        "UTC+912:42",
        "UTC+912:42:49",
        "UTC+9129",
        "UTC+91294",
        "UTC+912942",
        "UTC+9129424",
        "UTC+19129424",
        "UTC+19.5",
        "UTC+19.25",
        "UTC+19.125",
        "UTC+19.0625",
        "UTC+19.03125",
        "UTC+19.015625",
        "UTC+19.0078125",
        "UTC+19.00390625",
        "UTC+19.111111",
        "UTC+19.0111111",
        "UTC+19.00111111",
        "UTC+19.000111111",
        "UTC-9",
        "UTC-9:2",
        "UTC-9:2:3",
        "UTC-12",
        "UTC-12:13",
        "UTC-12:13:14",
        "UTC-99",
        "UTC-99:13",
        "UTC-99:13:14",
        "UTC-912",
        "UTC-912:42",
        "UTC-912:42:49",
        "UTC-9129",
        "UTC-91294",
        "UTC-912942",
        "UTC-9129424",
        "UTC-19129424",
        "UTC-19.5",
        "UTC-19.25",
        "UTC-19.125",
        "UTC-19.0625",
        "UTC-19.03125",
        "UTC-19.015625",
        "UTC-19.0078125",
        "UTC-19.00390625",
        "UTC-19.111111",
        "UTC-19.0111111",
        "UTC-19.00111111",
        "UTC-19.000111111",
    };

    // A zone name with a space character is not parsed by itself by Ruby's Date._strptime for the regular expression.
    // https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_5_0/ext/date/date_strptime.c?view=markup#l571
    //
    // Such a zone name is parsed only when suffixed by "standard time" or "daylight time".
    private static final String[] ZONES = {
        "dummy",
        "DUMMY",
        "DuMmY",
        "DuMmY DsT",
        // "dummy zone",
        // "DUMMY zone",
        // "DuMMy ZOnE",
        "dumMY zOne StandarD TimE",
        "ut",
        "UT",
        "Ut",
        "uT DayliGHt \t TimE",
        "gmt",
        "GMT",
        "gmT",
        "gMt sTANDaRd\t\tTIMe",
        "est",
        "EST",
        "EsT",
        "eST\t dST",
        "edt",
        "EDT",
        "edT",
        "Edt  dst",
        "cst",
        "CST",
        "cst",
        "csT\tDummy  TIME",
        "cdt",
        "CDT",
        "CDT",
        "cdt \tsTAnDaRd \tTiME",
        "mst",
        "MST",
        "mSt",
        "msT\tdsT",
        "mdt",
        "MDT",
        "MdT",
        "mdT \n\nDst",
        "pst",
        "PST",
        "PST",
        "PST\tdst",
        "pdt",
        "PDT",
        "PDT",
        "Pdt dAYLigHT\t  TImE",
        "a",
        "A",
        "a  DST",
        "b",
        "B",
        "b DAyligHT  tImE",
        "c",
        "C",
        "C  StanDarD   tImE",
        "d",
        "D",
        "d Dst",
        "e",
        "E",
        "e\tdst",
        "f",
        "F",
        "f \tdUMmY\t\t\ttiME",
        "g",
        "G",
        "g DUMMy\ttiME",
        "h",
        "H",
        "H\tDumMY  TiMe",
        "i",
        "I",
        "i stAnDarD\t tIME",
        "k",
        "K",
        "k\t duMMY\ntIME",
        "l",
        "L",
        "L  sTAndaRd\tTIMe",
        "m",
        "M",
        "m\n daYLIgHT \ttIME",
        "n",
        "N",
        "N  DuMMy\nTIME",
        "o",
        "O",
        "o  DUMmY TIMe",
        "p",
        "P",
        "p DsT",
        "q",
        "Q",
        "q\ndSt",
        "r",
        "R",
        "r\n\tdUMmy TimE",
        "s",
        "S",
        "s dST",
        "t",
        "T",
        "t\tdAYlIGhT\n tIMe",
        "u",
        "U",
        "u\nDST",
        "v",
        "V",
        "V  \tSTANDarD\n\n tiMe",
        "w",
        "W",
        "W\tDuMmy\ntIme",
        "x",
        "X",
        "X\tDummY\n\nTIME",
        "y",
        "Y",
        "Y DAylIGhT Time",
        "z",
        "Z",
        "Z  DST",
        "utc",
        "UTC",
        "UTC",
        "UTC  DAylIGht tIME",
        "wet",
        "WET",
        "Wet",
        "WEt Dst",
        "at",
        "AT",
        "aT",
        "aT dsT",
        "brst",
        "BRST",
        "brST",
        "BrST\t sTANDArD   timE",
        "ndt",
        "NDT",
        "ndt",
        "ndt  sTANdArD TIMe",
        "art",
        "ART",
        "aRt",
        "art  DUmMY tiMe",
        "adt",
        "ADT",
        "adt",
        "aDT\ndUMmY\nTimE",
        "brt",
        "BRT",
        "brt",
        "bRT\t\n\nDaYLighT Time",
        "clst",
        "CLST",
        "ClsT",
        "clsT\n STANdARD\t\ntime",
        "nst",
        "NST",
        "nsT",
        "nST  dsT",
        "ast",
        "AST",
        "AST",
        "AsT sTANDard\n tiMe",
        "clt",
        "CLT",
        "cLT",
        "ClT  DsT",
        "akdt",
        "AKDT",
        "akDt",
        "aKdT  StANDARD tIME",
        "ydt",
        "YDT",
        "ydt",
        "YDT DAyLIghT tiME",
        "akst",
        "AKST",
        "Akst",
        "aksT \tDuMmY \n TiMe",
        "hadt",
        "HADT",
        "HaDt",
        "hADT  sTaNdarD \tTiMe",
        "hdt",
        "HDT",
        "HdT",
        "hDT\n\n\nstaNdArD tImE",
        "yst",
        "YST",
        "YsT",
        "YST\ndaYliGht TImE",
        "ahst",
        "AHST",
        "ahSt",
        "aHSt\n DUMmY TiME",
        "cat",
        "CAT",
        "Cat",
        "CAt\n DAyLiGHt\ntiME",
        "hast",
        "HAST",
        "HASt",
        "hasT DayLighT timE",
        "hst",
        "HST",
        "HsT",
        "hST\t dUmMY tImE",
        "nt",
        "NT",
        "Nt",
        "NT\nSTaNdArd\ntime",
        "idlw",
        "IDLW",
        "idlw",
        "idlw   Dst",
        "bst",
        "BST",
        "BsT",
        "BST DUmmY\t tImE",
        "cet",
        "CET",
        "CET",
        "Cet DAYliGHT\ttIME",
        "fwt",
        "FWT",
        "Fwt",
        "fWt DaYLIGhT\t\ntiMe",
        "met",
        "MET",
        "MeT",
        "MET\ndaylIGhT\ttiMe",
        "mewt",
        "MEWT",
        "mEwt",
        "mEwT\tSTaNdard  tiMe",
        "mez",
        "MEZ",
        "meZ",
        "MeZ\t\tduMMY\t TimE",
        "swt",
        "SWT",
        "SWt",
        "swT \nstanDard  \ttIME",
        "wat",
        "WAT",
        "wAT",
        "WAt   DUMMy \ntiME",
        "west",
        "WEST",
        "WEsT",
        "WeST\nDuMMy\tTimE",
        "cest",
        "CEST",
        "CESt",
        "CEsT \nSTanDArd TImE",
        "eet",
        "EET",
        "eet",
        "eET DuMMY\t\n\ttime",
        "fst",
        "FST",
        "FST",
        "fSt  DAyLigHT  time",
        "mest",
        "MEST",
        "mESt",
        "meST\n DumMy \tTiME",
        "mesz",
        "MESZ",
        "Mesz",
        "MEsz\nduMMY\t\tTiME",
        "sast",
        "SAST",
        "sasT",
        "Sast \tDuMMy \nTimE",
        "sst",
        "SST",
        "sSt",
        "sst\n dUmMy time",
        "bt",
        "BT",
        "BT",
        "Bt\tDaYliGhT\tTImE",
        "eat",
        "EAT",
        "EAt",
        "Eat\tSTandaRd tIME",
        "eest",
        "EEST",
        "EEst",
        "EeSt\n\ndUmMy\nTIMe",
        "msk",
        "MSK",
        "MSk",
        "Msk dAYLiGHT  \ntime",
        "msd",
        "MSD",
        "MSD",
        "MSd\n\n DAYLigHt\ttiME",
        // "zp4",
        // "ZP4",
        // "ZP4",
        // "ZP4\nDsT",
        // "zp5",
        // "ZP5",
        // "zp5",
        // "zP5  stAndaRD tIMe",
        "ist",
        "IST",
        "Ist",
        "Ist\tDayLigHT\tTIME",
        // "zp6",
        // "ZP6",
        // "Zp6",
        // "Zp6  DsT",
        "wast",
        "WAST",
        "wASt",
        "wast \tdSt",
        "cct",
        "CCT",
        "cct",
        "Cct  dAYlighT Time",
        "sgt",
        "SGT",
        "sgt",
        "Sgt \tDUmmY\n TImE",
        "wadt",
        "WADT",
        "WaDT",
        "wADT \ndST",
        "jst",
        "JST",
        "Jst",
        "jst  DsT",
        "kst",
        "KST",
        "KST",
        "KSt\n\tDUmMy TiMe",
        "east",
        "EAST",
        "eAst",
        "eAsT DUMmY\n\ntIme",
        "gst",
        "GST",
        "Gst",
        "GSt DumMY \n TimE",
        "eadt",
        "EADT",
        "EAdT",
        "eadT\n\nduMMY  TimE",
        "idle",
        "IDLE",
        "idLe",
        "IdlE\t dUMmY\n\t TIMe",
        "nzst",
        "NZST",
        "nZst",
        "NZSt DSt",
        "nzt",
        "NZT",
        "nZT",
        "nzT Dst",
        "nzdt",
        "NZDT",
        "nZDt",
        "NzDT\tDST",
        "afghanistan",
        "AFGHANISTAN",
        "afgHAniStaN",
        "AFGHANIsTAN\n DAyLighT\nTime",
        "alaskan",
        "ALASKAN",
        "alaskan",
        "AlASKAn DST",
        "arab",
        "ARAB",
        "aRAb",
        "aRaB  \tSTAndARD  Time",
        "arabian",
        "ARABIAN",
        "aRabIan",
        "aRaBIAn  DUMMY\ttImE",
        "arabic",
        "ARABIC",
        "ARABIc",
        "ARabiC\n \tdSt",
        "atlantic",
        "ATLANTIC",
        "AtlANTic",
        "atlanTic DAyLigHT\ttiMe",
        // "aus central",
        // "AUS CENTRAL",
        // "AUS   CeNTRAl",
        "AUS CenTraL  DAyLIGht\tTimE",
        // "aus eastern",
        // "AUS EASTERN",
        // "auS  eAsTeRN",
        "aus\nEastErn\t\nsTaNDarD  Time",
        "azores",
        "AZORES",
        "AzorES",
        "azores  dUmmy\nTImE",
        // "canada central",
        // "CANADA CENTRAL",
        // "cANADA \nCENTRAl",
        "CANAda CenTrAl  sTaNdArd\n Time",
        // "cape verde",
        // "CAPE VERDE",
        // "cApe  VERDe",
        "CAPe  veRde sTandard tIME",
        "caucasus",
        "CAUCASUS",
        "CAuCasUS",
        "CaucasUs\tdst",
        // "cen. australia",
        // "CEN. AUSTRALIA",
        // "ceN.\t\tausTralIa",
        "cEN. aUstRALiA StaNdArd  TIMe",
        // "central america",
        // "CENTRAL AMERICA",
        // "centrAL  AmerIca",
        "CentRal   ameRICA\t\t STandArd tiMe",
        // "central asia",
        // "CENTRAL ASIA",
        // "CeNtRaL  asIa",
        "CEnTRaL \nasIA dAYLighT  TiME",
        // "central europe",
        // "CENTRAL EUROPE",
        // "CentRaL\n\tEUrOpe",
        "CEntraL\nEUrOPE STaNDArD TimE",
        // "central european",
        // "CENTRAL EUROPEAN",
        // "CENtrAl \tEurOpeaN",
        "CeNtral euroPEAN  DAYLiGhT tIMe",
        // "central pacific",
        // "CENTRAL PACIFIC",
        // "CEntral PaCIFic",
        "CenTRal PACIfic daYlIGHT TiME",
        "central",
        "CENTRAL",
        "Central",
        "cenTRAL DaYLiGHT TIme",
        "china",
        "CHINA",
        "chiNA",
        "chIna\t\ndaylIGht\t\ntImE",
        "dateline",
        "DATELINE",
        "dATeLINE",
        "datElInE DaYLIghT\n  TiME",
        // "e. africa",
        // "E. AFRICA",
        // "E. AfrIcA",
        "e. AfriCa\nStANDarD time",
        // "e. australia",
        // "E. AUSTRALIA",
        // "e. AUStraLIa",
        "E.\t\taUSTRALIa\t\tDAylIGHt\ttIME",
        // "e. europe",
        // "E. EUROPE",
        // "E. \t\tEuroPe",
        "E. \n EUrope\tDummY  TIme",
        // "e. south america",
        // "E. SOUTH AMERICA",
        // "E. SouTH  AMERICA",
        "e.\t\t SOuTh AmeRIca DAyliGht tImE",
        "eastern",
        "EASTERN",
        "easteRn",
        "eAsTERN dAyLight TIme",
        "egypt",
        "EGYPT",
        "eGYpT",
        "EGyPT\t\nstanDARD\nTIME",
        "ekaterinburg",
        "EKATERINBURG",
        "EKATerINBurg",
        "ekAtERINbURg \tDuMmy\nTIMe",
        "fiji",
        "FIJI",
        "fIJi",
        "fiji DaYlIght \ttImE",
        "fle",
        "FLE",
        "fLe",
        "FLe StaNDArD\n Time",
        "greenland",
        "GREENLAND",
        "gREenLAnd",
        "GReENlAnD \n STAndArd  TimE",
        "greenwich",
        "GREENWICH",
        "gReeNwIcH",
        "grEeNWIch   Dst",
        "gtb",
        "GTB",
        "gTB",
        "gtB \tSTAndard \ntiMe",
        "hawaiian",
        "HAWAIIAN",
        "HAWaiIAn",
        "HawaIiAN\n \tdumMY  tIMe",
        "india",
        "INDIA",
        "India",
        "inDia\ndaYligHt\tTiME",
        "iran",
        "IRAN",
        "IRan",
        "Iran\tDummY \ttIME",
        "jerusalem",
        "JERUSALEM",
        "JeRUsaleM",
        "JerUsALEM\n \nDst",
        "korea",
        "KOREA",
        "KOReA",
        "kOrEA Dst",
        "mexico",
        "MEXICO",
        "MeXico",
        "mexICo\tdaYLIGhT\t timE",
        // "mid-atlantic",
        // "MID-ATLANTIC",
        // "mid-aTlANtIC",
        // "mId-AtLanTIC\tStAnDarD\t TImE",
        "mountain",
        "MOUNTAIN",
        "mOUntaiN",
        "moUnTaiN \nstaNDarD \ttImE",
        "myanmar",
        "MYANMAR",
        "mYaNmar",
        "mYaNMAR  stanDard \tTiME",
        // "n. central asia",
        // "N. CENTRAL ASIA",
        // "n.\tCEnTral \n\taSia",
        "N.  cEnTrAL\nasIA\ndAYliGht TIME",
        "nepal",
        "NEPAL",
        "nepal",
        "nepal  StAnDARd\tTIME",
        // "new zealand",
        // "NEW ZEALAND",
        // "neW \nZEalAnD",
        "nEw \t\nZEaLaNd  dummy\t tIME",
        "newfoundland",
        "NEWFOUNDLAND",
        "newFoUnDlAnD",
        "NEWfounDLAnD daYLIGht  TIME",
        // "north asia east",
        // "NORTH ASIA EAST",
        // "norTH\n aSIa  EaSt",
        "nOrth \naSiA\n eaST  stAndArd\nTime",
        // "north asia",
        // "NORTH ASIA",
        // "NORth\tAsia",
        "nORTh\nasIa dUmMy\nTIme",
        // "pacific sa",
        // "PACIFIC SA",
        // "pACifIC Sa",
        "PACIfIc\tSA\n sTANDarD tIME",
        "pacific",
        "PACIFIC",
        "pAcIFiC",
        "pAcIFic   dAyLIgHT  tiMe",
        "romance",
        "ROMANCE",
        "rOmancE",
        "ROMANce DayLIghT\ntiME",
        "russian",
        "RUSSIAN",
        "russiaN",
        "RUSSian\tDSt",
        // "sa eastern",
        // "SA EASTERN",
        // "sa  easTERN",
        "Sa\n EAstERN dummY tImE",
        // "sa pacific",
        // "SA PACIFIC",
        // "sa\n paCIFIC",
        "SA \t PAciFIC  \nSTAndArd\ntiMe",
        // "sa western",
        // "SA WESTERN",
        // "SA westeRN",
        "Sa  wEstern  DaYLigHT\ntiME",
        "samoa",
        "SAMOA",
        "samOA",
        "SAMoa\t  DSt",
        // "se asia",
        // "SE ASIA",
        // "se \tASiA",
        "sE\n\t ASia\tSTANdARD tIME",
        // "malay peninsula",
        // "MALAY PENINSULA",
        // "MALaY PENInSUlA",
        "MAlay PEnInSuLa\tdayLIgHt\n\ntIme",
        // "south africa",
        // "SOUTH AFRICA",
        // "south afriCa",
        "SOuTh AFRicA DAYlIGht\t\tTIME",
        // "sri lanka",
        // "SRI LANKA",
        // "SrI lAnKA",
        "sri lanKa\tDuMmY\n  tImE",
        "taipei",
        "TAIPEI",
        "tAiPei",
        "taipEi dAyLIght TIMe",
        "tasmania",
        "TASMANIA",
        "tAsMaNIA",
        "taSMaNIA sTaNDARD time",
        "tokyo",
        "TOKYO",
        "ToKYo",
        "ToKYO\tDUmmY\ttIme",
        "tonga",
        "TONGA",
        "Tonga",
        "TONGa  dUMMy \ntimE",
        // "us eastern",
        // "US EASTERN",
        // "Us \nEASterN",
        "us EaSTerN daYliGHt\n\tTimE",
        // "us mountain",
        // "US MOUNTAIN",
        // "us  MOUNTaIN",
        "uS MOuNtAin\n DuMMy\n\ttiMe",
        "vladivostok",
        "VLADIVOSTOK",
        "vladIvosTOk",
        "vLADIVOSToK  dsT",
        // "w. australia",
        // "W. AUSTRALIA",
        // "w.\naUsTrAliA",
        "w.\n aUstRalIa \tDUMMY  TImE",
        // "w. central africa",
        // "W. CENTRAL AFRICA",
        // "W. \ncENtrAl  AFrIca",
        "w. CentrAL\tafriCa DAYligHt TIME",
        // "w. europe",
        // "W. EUROPE",
        // "W. EuRopE",
        "W.  EUrope\n  duMMY  tiMe",
        // "west asia",
        // "WEST ASIA",
        // "WeSt asIa",
        "wesT  AsIa\n\tSTanDarD TimE",
        // "west pacific",
        // "WEST PACIFIC",
        // "wEST pacIFIc",
        "wESt\t PAciFIC dUMmY\ntimE",
        "yakutsk",
        "YAKUTSK",
        "YaKuTSk",
        "YaKutSK  dAyLIgHT  TIMe",
    };

    private static final String[] OFFSETS_IGNORED_JRUBY_IN_ARRAY = {
        "UTC+19.125",
        "UTC+19.0625",
        "UTC+19.03125",
        "UTC+19.015625",
        "UTC+19.0078125",
        "UTC+19.00390625",
        "UTC+19.111111",
        "UTC+19.0111111",
        "UTC+19.00111111",
        "UTC+19.000111111",
        "UTC-19.125",
        "UTC-19.0625",
        "UTC-19.03125",
        "UTC-19.015625",
        "UTC-19.0078125",
        "UTC-19.00390625",
        "UTC-19.111111",
        "UTC-19.0111111",
        "UTC-19.00111111",
        "UTC-19.000111111",
    };

    private static final Set<String> OFFSETS_IGNORED_JRUBY = new HashSet(Arrays.asList(OFFSETS_IGNORED_JRUBY_IN_ARRAY));

    private static final Pattern HOUR_HALF = Pattern.compile("(?<negative>-?)\\((?<hour>\\d+)\\*3600\\+1800\\)");
    private static final Pattern HOUR = Pattern.compile("(?<negative>-?)(?<hour>\\d+)\\*3600");
    private static final Pattern SECOND = Pattern.compile("(?<negative>-?)(?<second>\\d+)");

    private static CRubyCaller cruby;
    private static ScriptingContainer jruby;
    private static RubySymbol leftoverSymbol;
    private static RubySymbol offsetSymbol;
}

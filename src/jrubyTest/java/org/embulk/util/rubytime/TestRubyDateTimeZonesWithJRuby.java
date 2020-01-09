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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
import org.junit.jupiter.params.provider.MethodSource;

public class TestRubyDateTimeZonesWithJRuby {
    @ParameterizedTest
    @MethodSource("provideParseOffsetParams")
    public void testParseOffsetWithJRuby(final String value) {
        // Offsets in OFFSETS_IGNORED_JRUBY are incompliant in JRuby.
        assumeTrue(!OFFSETS_IGNORED_JRUBY.contains(value));
        if (JRUBY_VERSION < V9_2_0_0) {
            assumeTrue(!OFFSETS_IGNORED_JRUBY_9_1.contains(value));
        }
        assertParseOffsetJRuby(value);
    }

    private static void assertParseOffsetJRuby(final String value) {
        final String scriptlet = String.format("DateTime._strptime(\"%s\", \"%%z\")", value);
        final Object result = jruby.runScriptlet(scriptlet);

        final int actual = RubyDateTimeZones.parseOffsetForTesting(value);
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

    @BeforeAll
    public static void loadJRubyScriptingContainerVanilla() {
        jruby = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
        jruby.runScriptlet("require 'date'");
        leftoverSymbol = RubySymbol.newSymbol(jruby.getProvider().getRuntime(), "leftover");
        offsetSymbol = RubySymbol.newSymbol(jruby.getProvider().getRuntime(), "offset");
    }

    private static Stream<String> provideParseOffsetParams() {
        return Arrays.stream(OFFSETS);
    }

    private static long encodeVersion(final String jrubyVersion) {
        final String[] splitVersion = jrubyVersion.split("\\.");
        if (splitVersion.length != 4) {
            throw new RuntimeException("Version " + jrubyVersion + " is not 4-digits.");
        }
        return Long.parseLong(splitVersion[0]) * 1_000_000_000L
                + Long.parseLong(splitVersion[1]) * 1_000_000L
                + Long.parseLong(splitVersion[2]) * 1_000L
                + Long.parseLong(splitVersion[3]);
    }

    private static long JRUBY_VERSION = encodeVersion(org.jruby.runtime.Constants.VERSION);
    private static long V9_2_0_0 = encodeVersion("9.2.0.0");

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

    // They don't work before JRuby 9.2.0.
    private static final String[] OFFSETS_IGNORED_JRUBY_9_1_IN_ARRAY = {
        "+19248",
        "+9184812",
        "-19248",
        "-9184812",
        "UTC+91294",
        "UTC+9129424",
        "UTC-91294",
        "UTC-9129424",
    };

    private static final Set<String> OFFSETS_IGNORED_JRUBY = new HashSet<>(Arrays.asList(OFFSETS_IGNORED_JRUBY_IN_ARRAY));
    private static final Set<String> OFFSETS_IGNORED_JRUBY_9_1 = new HashSet<>(Arrays.asList(OFFSETS_IGNORED_JRUBY_9_1_IN_ARRAY));

    private static ScriptingContainer jruby;
    private static RubySymbol leftoverSymbol;
    private static RubySymbol offsetSymbol;
}

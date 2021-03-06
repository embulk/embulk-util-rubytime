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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;

/**
 * Tests parsing by RubyDateTimeFormatter with JRuby.
 */
public class TestRubyDateTimeFormatterParseWithJRuby {
    @Test
    public void testDateStrptime_test__strptime() throws IOException {
        assertJRubyTest("TestDateStrptime", "test__strptime");
    }

    @Test
    public void testDateStrptime_test__strptime__2() throws IOException {
        assertJRubyTest("TestDateStrptime", "test__strptime__2");
    }

    @Test
    public void testDateStrptime_test__strptime__3() throws IOException {
        assertJRubyTest("TestDateStrptime", "test__strptime__3");
    }

    @Test
    public void testDateStrptime_test__strptime__width() throws IOException {
        assertJRubyTest("TestDateStrptime", "test__strptime__width");
    }

    @Test
    public void testDateStrptime_test__strptime__fail() throws IOException {
        assertJRubyTest("TestDateStrptime", "test__strptime__fail");
    }

    @Test
    public void testDateStrptime_test_strptime() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime");
    }

    @Test
    public void testDateStrptime_test_strptime__2() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime__2");
    }

    @Test
    public void testDateStrptime_test_strptime__minus() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime__minus");
    }

    @Test
    public void testDateStrptime_test_strptime__comp() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime__comp");
    }

    @Test
    public void testDateStrptime_test_strptime__d_to_s() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime__d_to_s");
    }

    @Test
    public void testDateStrptime_test_strptime__ex() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_strptime__ex");
    }

    @Test
    public void testDateStrptime_test_given_string() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_given_string");
    }

    @Disabled("\"test_sz\" is intentionally skipped because it fails on JRuby even without the monkey patch.")
    @Test
    public void testDateStrptime_test_sz() throws IOException {
        assertJRubyTest("TestDateStrptime", "test_sz");
    }

    @Test
    public void testTestTimeExtension_test_strptime() throws IOException {
        assertJRubyTest("TestTimeExtension", "test_strptime");
    }

    @Test
    public void testTestTimeExtension_test_strptime_empty() throws IOException {
        assertJRubyTest("TestTimeExtension", "test_strptime_empty");
    }

    @Test
    public void testTestTimeExtension_test_strptime_s_z() throws IOException {
        assertJRubyTest("TestTimeExtension", "test_strptime_s_z");
    }

    @Disabled("\"strptime_s_N\" is intentionally skipped because it fails on JRuby even without the monkey patch.")
    @Test
    public void testTestTimeExtension_test_strptime_s_N() throws IOException {
        // It tests fractions finer than nanoseconds.
        assertJRubyTest("TestTimeExtension", "test_strptime_s_N");
    }

    @Test
    public void testTestTimeExtension_test_strptime_Ymd_z() throws IOException {
        assertJRubyTest("TestTimeExtension", "test_strptime_Ymd_z");
    }

    private void assertJRubyTest(final String testcase, final String name) {
        assertTrue(runJRubyTest(testcase, name));
    }

    private boolean runJRubyTest(final String testcase, final String name) {
        final String scriptlet = String.format(
                "Test::Unit::AutoRunner.run(false, nil, ['--testcase=%s', '--name=%s'])",
                testcase,
                name);

        System.out.println(scriptlet);
        return (Boolean) jruby.runScriptlet(scriptlet);
    }

    @BeforeAll
    public static void loadJRubyScriptingContainerWithMonkeyPatch() throws IOException, URISyntaxException {
        jruby = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);

        final RubyInstanceConfig config = jruby.getProvider().getRubyInstanceConfig();
        final String[] arguments = { "--debug" };
        config.processArguments(arguments);

        final URL gemsDirUrl = TestRubyDateTimeFormatterParseWithJRuby.class.getClassLoader().getResource("third_party/gems");
        final URI gemsDirUri = gemsDirUrl.toURI();
        final String gemsDir = gemsDirUri.getPath();

        jruby.runScriptlet("ENV['TZ'] = 'UTC'");
        jruby.runScriptlet("Gem.use_paths('" + gemsDir + "')");
        jruby.runScriptlet("require 'date'");
        jruby.runScriptlet("require 'time'");
        jruby.runScriptlet("require 'test/unit'");

        jruby.runScriptlet(
                TestRubyDateTimeFormatterParseWithJRuby.class.getClassLoader().getResourceAsStream("date_monkey_patch.rb"),
                "date_monkey_patch.rb");
        jruby.runScriptlet(
                TestRubyDateTimeFormatterParseWithJRuby.class.getClassLoader().getResourceAsStream("time_monkey_patch.rb"),
                "time_monkey_patch.rb");

        // Require test files from Java resource.
        jruby.runScriptlet("require 'ruby/test/date/test_date_strptime.rb'");
        jruby.runScriptlet("require 'ruby/test/test_time.rb'");
    }

    private static ScriptingContainer jruby;
}

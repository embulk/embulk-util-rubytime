# Copyright 2018 The Embulk project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

module TimeMonkeyPatch
  require 'java'

  java_package 'org.embulk.util.rubytime'

  def self.included base
    base.instance_eval do
      def strptime(date, format)
        formatter = Java::org.embulk.util.rubytime.RubyDateTimeFormatter.ofPattern(format)

        begin
          parsedResolved = formatter.parse(date)
        rescue Java::org.embulk.util.rubytime.RubyDateTimeParseException
          raise ArgumentError
        end

        instant_seconds = parsedResolved.getLong(Java::java.time.temporal.ChronoField::INSTANT_SECONDS)
        nano = parsedResolved.get(Java::java.time.temporal.ChronoField::NANO_OF_SECOND)

        # TODO: Get the zone offset directly from the resolved object, not from the parsed object.
        # Querying rubyTimeZone for the resolved object may fail as of now.
        begin
          parsed = formatter.parseUnresolved(date)
        rescue Java::org.embulk.util.rubytime.RubyDateTimeParseException
          return nil
        end
        if parsed.nil?
          raise 'RubyDateTimeFormatter#parseUnresolved returned null unexpectedly.'
        end
        zone_string = parsed.query(Java::org.embulk.util.rubytime.RubyTemporalQueries.rubyTimeZone())

        offset = Java::org.embulk.util.rubytime.RubyTimeZones.toZoneOffset(
          zone_string, Java::java.time.ZoneOffset::UTC).getTotalSeconds()

        # Workaround against difference in handling "UTC" between Matz' Ruby Implementation (MRI) and JRuby.
        #
        # In MRI 2.5.0:
        #   irb(main):002:0> mri_time = Time.new(2018, 1, 2, 3, 4, 5, 0)
        #   => 2018-01-02 03:04:05 +0000
        #   irb(main):003:0> mri_time.utc?
        #   => false
        #
        # In JRuby (as of 9.1.15.0):
        #   irb(main):002:0> jruby_time = Time.new(2018, 1, 2, 3, 4, 5, 0)
        #   => 2018-01-02 03:04:05 UTC
        #   irb(main):003:0> jruby_time.utc?
        #   => true
        #
        # Due to this difference, without this workaround, TestTimeExtension::test_strptime fails at:
        #     assert_equal(false, Time.strptime('0', '%s').utc?)
        if offset == 0 && zone_string != "UTC"
          ruby_time = Java::org.jruby.RubyTime.newTime(
            JRuby.runtime,
            Java::org.joda.time.DateTime.new((instant_seconds * 1000) + (nano / 1000000),
                                             Java::org.joda.time.DateTimeZone::forID("Etc/UTC")),
            nano % 1000000)
        else
          ruby_time = Java::org.jruby.RubyTime.newTime(
            JRuby.runtime,
            Java::org.joda.time.DateTime.new((instant_seconds * 1000) + (nano / 1000000)),
            nano % 1000000
          ).localtime(offset)
        end

        return ruby_time
      end
    end
  end
end

Time.send(:include, TimeMonkeyPatch)

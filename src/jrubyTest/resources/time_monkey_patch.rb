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
        # Querying RubyTemporalQueries.zone() for the resolved object may fail as of now.
        begin
          parsed = formatter.parseUnresolved(date)
        rescue Java::org.embulk.util.rubytime.RubyDateTimeParseException
          return nil
        end
        if parsed.nil?
          raise 'RubyDateTimeFormatter#parseUnresolved returned null unexpectedly.'
        end
        zone_string = parsed.query(Java::org.embulk.util.rubytime.RubyTemporalQueries.zone())

        offset = Java::org.embulk.util.rubytime.RubyTimeZones.toZoneOffset(
          zone_string, Java::java.time.ZoneOffset::UTC).getTotalSeconds()

        jruby_version = TimeMonkeyPatch::version_to_i(JRUBY_VERSION)
        v9_2_0_0 = TimeMonkeyPatch::version_to_i("9.2.0.0")

        # To make JRuby's `Time` instance to respond with `true` against `utc?`,
        # it must be initialized with `org.joda.time.DateTimeZone.UTC`.
        #
        # Ruby's `Time` instance should respond with `true` when and only when the zone
        # matches with /\A(?:-00:00|-0000|-00|UTC|Z|UT)\z/i. See `zone_utc?` below.
        #
        # Note that JRuby has been fixed to follow CRuby's behavior between 9.2.0.0 and 9.2.9.0.
        # For example :
        # https://github.com/jruby/jruby/pull/5402
        # https://github.com/jruby/jruby/commit/71f7b5ae8f90b53eb72e4b0c6c928653fb6a2928
        # https://github.com/jruby/jruby/pull/5589
        # ...
        if jruby_version > v9_2_0_0 && TimeMonkeyPatch::zone_utc?(zone_string)
          ruby_time = Java::org.jruby.RubyTime.newTime(
            JRuby.runtime,
            Java::org.joda.time.DateTime.new((instant_seconds * 1000) + (nano / 1000000),
                                             Java::org.joda.time.DateTimeZone::UTC),
            nano % 1000000)
        elsif jruby_version <= v9_2_0_0 && offset == 0 && zone_string != "UTC"
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

  # This method is imported from MRI for testing.
  # https://github.com/ruby/ruby/blob/v2_6_5/lib/time.rb#L95-L117
  def self.zone_utc?(zone)
    # * +0000
    #   In RFC 2822, +0000 indicate a time zone at Universal Time.
    #   Europe/Lisbon is "a time zone at Universal Time" in Winter.
    #   Atlantic/Reykjavik is "a time zone at Universal Time".
    #   Africa/Dakar is "a time zone at Universal Time".
    #   So +0000 is a local time such as Europe/London, etc.
    # * GMT
    #   GMT is used as a time zone abbreviation in Europe/London,
    #   Africa/Dakar, etc.
    #   So it is a local time.
    #
    # * -0000, -00:00
    #   In RFC 2822, -0000 the date-time contains no information about the
    #   local time zone.
    #   In RFC 3339, -00:00 is used for the time in UTC is known,
    #   but the offset to local time is unknown.
    #   They are not appropriate for specific time zone such as
    #   Europe/London because time zone neutral,
    #   So -00:00 and -0000 are treated as UTC.
    if zone.nil?
      return false
    else
      zone.match?(/\A(?:-00:00|-0000|-00|UTC|Z|UT)\z/i)
    end
  end

  def self.version_to_i(jruby_version)
    split_version = jruby_version.split(".")
    if split_version.length != 4
      raise "Version #{jruby_version} is not 4-digits."
    end
    (split_version[0].to_i * 1_000_000_000) + (split_version[1].to_i * 1_000_000) + (split_version[2].to_i * 1_000) + split_version[3].to_i
  end
end

Time.send(:include, TimeMonkeyPatch)

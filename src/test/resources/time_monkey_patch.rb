module TimeMonkeyPatch
  require 'java'

  java_package 'org.embulk.util.rubytime'

  def self.included base
    base.instance_eval do
      def strptime(date, format)
        formatter = Java::org.embulk.util.rubytime.RubyDateTimeFormatter.ofPattern(format)

        parsed = formatter.parseUnresolved(date)
        if parsed.nil?
          return nil
        end

        resolver = Java::org.embulk.util.rubytime.DefaultRubyTimeResolver.of()

        begin
          resolved = resolver.resolve(parsed)
        rescue Java::org.embulk.util.rubytime.RubyTimeResolveException
          raise ArgumentError, "no time information in #{date.inspect}"
        end

        instant_seconds = resolved.getLong(Java::java.time.temporal.ChronoField::INSTANT_SECONDS)
        nano = resolved.get(Java::java.time.temporal.ChronoField::NANO_OF_SECOND)

        # TODO: Get the zone offset directly from the resolved object, not from the parsed object.
        zone_string = parsed.query(Java::org.embulk.util.rubytime.RubyTemporalQueries.rubyTimeZone())
        offset = Java::org.embulk.util.rubytime.TimeZones.toZoneOffset(
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

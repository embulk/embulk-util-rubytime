module DateMonkeyPatch
  require 'java'

  java_package 'org.embulk.util.rubytime'

  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        format = Java::org.embulk.util.rubytime.RubyTimeFormat.compile(fmt)
        parser = Java::org.embulk.util.rubytime.RubyTimeParser.new(format)

        parsed = parser.parse(str)
        if parsed.nil?
          return nil
        end

        map = Java::org.embulk.util.rubytime.ParsedElementsHashQuery.of(
                  Java::org.embulk.util.rubytime.FractionToJRubyRationalConverter.new(JRuby.runtime),
                  Java::org.embulk.util.rubytime.HashKeyToJRubySymbolConverter.new(JRuby.runtime))
                .queryFrom(parsed)

        return map.nil? ? nil : map.to_hash
      end
    end
  end
end

Date.send(:include, DateMonkeyPatch)

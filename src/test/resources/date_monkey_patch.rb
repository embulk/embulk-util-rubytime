module DateMonkeyPatch
  require 'java'

  java_package 'org.embulk.util.rubytime'

  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        formatter = Java::org.embulk.util.rubytime.RubyDateTimeFormatter.ofPattern(fmt)

        parsed = formatter.parseUnresolved(str)
        if parsed.nil?
          return nil
        end

        map = parsed.query(Java::org.embulk.util.rubytime.ParsedElementsQuery.of(
                             Java::org.embulk.util.rubytime.FractionToJRubyRationalConverter.new(JRuby.runtime),
                             Java::org.embulk.util.rubytime.HashKeyToJRubySymbolConverter.new(JRuby.runtime)))

        return map.nil? ? nil : map.to_hash
      end
    end
  end
end

Date.send(:include, DateMonkeyPatch)

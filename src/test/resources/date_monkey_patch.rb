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

module DateMonkeyPatch
  require 'java'

  java_package 'org.embulk.util.rubytime'

  def self.included base
    base.instance_eval do
      def _strptime(str, fmt='%F')
        formatter = Java::org.embulk.util.rubytime.RubyDateTimeFormatter.ofPattern(fmt)

        begin
          parsed = formatter.parseUnresolved(str)
        rescue Java::org.embulk.util.rubytime.RubyDateTimeParseException
          return nil
        end
        if parsed.nil?
          raise 'RubyDateTimeFormatter#parseUnresolved returned null unexpectedly.'
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

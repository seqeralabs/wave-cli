/*
 * Copyright 2023-2025, Seqera Labs
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
 *
 */


package io.seqera.wave.cli.util;

import picocli.CommandLine;

import java.time.Duration;
/**
 * Converter to convert cli argument to duration
 *
 * @author Munish Chouhan <munish.chouhan@seqera.io>
 */
public class DurationConverter implements CommandLine.ITypeConverter<Duration> {
    @Override
    public Duration convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Duration.ofMinutes(15);
        }
        return Duration.parse("PT" + value.toUpperCase());
    }
}

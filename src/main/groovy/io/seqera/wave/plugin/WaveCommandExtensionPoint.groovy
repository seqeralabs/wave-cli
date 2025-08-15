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

package io.seqera.wave.plugin

import nextflow.cli.CommandExtensionPoint
import org.pf4j.Extension

/**
 * Wave command extension point for Nextflow
 *
 * Provides the 'wave' command as a first-class Nextflow CLI command
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Extension
class WaveCommandExtensionPoint implements CommandExtensionPoint {

    @Override
    String getCommandName() {
        return "wave"
    }

    @Override
    String getCommandDescription() {
        return "Wave container provisioning and management"
    }

    @Override
    int getPriority() {
        return 100  // Standard priority for plugin commands
    }

    @Override
    nextflow.cli.CmdBase createCommand() {
        return new WaveCommand()
    }
}
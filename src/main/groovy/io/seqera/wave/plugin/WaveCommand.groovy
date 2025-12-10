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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import nextflow.cli.CmdBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Wave command implementation for Nextflow CLI
 *
 * Integrates Wave CLI functionality as a first-class Nextflow command
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Parameters(commandDescription = "Wave container provisioning and management")
class WaveCommand extends CmdBase {

    static final String NAME = "wave"
    private static final Logger log = LoggerFactory.getLogger(WaveCommand.class)

    @Parameter(description = "Wave CLI arguments", variableArity = true)
    List<String> args = []

    @Parameter(names = ['--help'], description = "Show this help message", help = true)
    boolean help

    @Override
    String getName() {
        return NAME
    }

    @Override
    void run() {
        log.debug "Executing Wave command with args: ${args}"
        
        try {
            // Create Wave CLI wrapper instance
            final WaveCommandExtension waveExtension = new WaveCommandExtension()
            
            // Convert args to array and execute
            final String[] argArray = args as String[]
            final int exitCode = waveExtension.exec(argArray)
            
            // Handle exit code
            if (exitCode != 0) {
                System.exit(exitCode)
            }
        }
        catch (Exception e) {
            log.error("Failed to execute Wave command: ${e.message}", e)
            System.err.println("Error executing Wave command: ${e.message}")
            System.exit(1)
        }
    }
}
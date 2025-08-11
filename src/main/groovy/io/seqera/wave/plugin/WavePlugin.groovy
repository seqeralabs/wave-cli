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

import nextflow.plugin.BasePlugin
import nextflow.cli.PluginAbstractExec
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Wave plugin for Nextflow
 *
 * Enables Wave container provisioning directly from Nextflow CLI
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WavePlugin extends BasePlugin implements PluginAbstractExec {

    private static final Logger log = LoggerFactory.getLogger(WavePlugin.class)

    WavePlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Wave plugin started")
    }

    @Override
    void stop() {
        log.debug("Wave plugin stopped")
    }

    @Override
    List<String> getCommands() {
        return ['wave']
    }

    @Override
    int exec(String cmd, List<String> args) {
        if (cmd == 'wave') {
            log.debug("Executing Wave command with args: ${args}")
            try {
                def extension = new WaveCommandExtension()
                return extension.exec(args as String[])
            } catch (Exception e) {
                System.err.println("Wave command failed: ${e.message}")
                log.error("Wave command execution failed", e)
                return 1
            }
        } else {
            System.err.println("Invalid command: ${cmd}")
            return 1
        }
    }
}
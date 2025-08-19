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
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Wave CLI plugin for Nextflow
 *
 * Provides first-class Wave CLI commands through CommandExtensionPoint system
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WavePlugin extends BasePlugin {

    private static final Logger log = LoggerFactory.getLogger(WavePlugin.class)

    WavePlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        log.debug("Wave CLI plugin started")
    }

    @Override
    void stop() {
        log.debug("Wave CLI plugin stopped")
    }
}
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

import io.seqera.wave.cli.App
import io.seqera.wave.cli.exception.BadClientResponseException
import io.seqera.wave.cli.exception.ClientConnectionException
import io.seqera.wave.cli.exception.IllegalCliArgumentException
import io.seqera.wave.cli.exception.ReadyTimeoutException
import io.seqera.wave.cli.util.DurationConverter
import picocli.CommandLine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Wave command extension for Nextflow plugin
 *
 * Wraps the existing Wave CLI App functionality to be executed within Nextflow plugin context
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WaveCommandExtension {

    private static final Logger log = LoggerFactory.getLogger(WaveCommandExtension.class)

    /**
     * Execute Wave command with the provided arguments
     *
     * @param args Command line arguments 
     * @return Exit code (0 for success, non-zero for failure)
     */
    int exec(String[] args) {
        try {
            // Create Wave App instance
            final App app = new App()
            final CommandLine cli = new CommandLine(app)

            // Register duration converter
            cli.registerConverter(Duration.class, new DurationConverter())

            // Add examples in help
            cli
                .getCommandSpec()
                .usageMessage()
                .footer(readExamples("usage-examples.txt"))

            // Parse arguments with stdin support
            final CommandLine.ParseResult result = cli.parseArgs(App.makeArgs(args))
            if (!result.originalArgs().contains("--")) {
                // Reset prompt if `--` was not entered
                app.setPrompt(null)
            }

            // Handle help and version requests
            if (result.matchedArgs().size() == 0 || result.isUsageHelpRequested()) {
                cli.usage(System.out)
                return 0
            }
            else if (result.isVersionHelpRequested()) {
                System.out.println(io.seqera.wave.cli.util.BuildInfo.getFullVersion())
                return 0
            }

            // Execute the command
            app.setLogLevel()
            app.defaultArgs()
            if (app.isInfo()) {
                app.printInfo()
            }
            else if (app.isInspect()) {
                app.inspect()
            }
            else {
                app.run()
            }

            return 0
        }
        catch (IllegalCliArgumentException | CommandLine.ParameterException | BadClientResponseException |
               ReadyTimeoutException | ClientConnectionException e) {
            System.err.println(e.getMessage())
            return 1
        }
        catch (Throwable e) {
            log.error("Unexpected error during Wave command execution", e)
            e.printStackTrace(System.err)
            return 1
        }
    }

    /**
     * Read usage examples from resource file
     */
    private static String readExamples(String exampleFile) {
        try {
            def stream = WaveCommandExtension.class.getResourceAsStream("/io/seqera/wave/cli/${exampleFile}")
            if (stream) {
                return new String(stream.readAllBytes())
            }
            return ""
        }
        catch (Exception e) {
            log.warn("Unable to read usage examples: ${e.message}")
            return ""
        }
    }
}
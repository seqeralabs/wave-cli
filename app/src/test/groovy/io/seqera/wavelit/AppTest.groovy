package io.seqera.wavelit

import io.seqera.wave.api.ContainerConfig
import io.seqera.wavelit.exception.IllegalCliArgumentException
import picocli.CommandLine
import spock.lang.Specification

class AppTest extends Specification {

    def "test valid no entrypoint"() {
        given:
        def app = new App()
        String[] args = []
        def cli = new CommandLine(app)

        when:
        cli.parseArgs(args)
        then:
        app.@entrypoint == null

        when:
        def config = app.prepareConfig()
        then:
        config == null
    }


    def "test valid entrypoint"() {
        given:
        def app = new App()
        String[] args = ["--config-entrypoint", "entryPoint"]
        def cli = new CommandLine(app)

        when:
        cli.parseArgs(args)
        then:
        app.@entrypoint == "entryPoint"

        when:
        def config = app.prepareConfig()
        then:
        config == new ContainerConfig(entrypoint: ['entryPoint'])
    }

    def "test invalid entrypoint"() {
        given:
        def app = new App()
        String[] args = ["--config-entrypoint"]

        when:
        new CommandLine(app).parseArgs(args)

        then:
        thrown(CommandLine.MissingParameterException)
    }

    def "test valid command"() {
        given:
        def app = new App()
        String[] args = ["--config-cmd", "/some/command"]

        when:
        new CommandLine(app).parseArgs(args)
        then:
        app.@command == "/some/command"

        when:
        def config = app.prepareConfig()
        then:
        config == new ContainerConfig(cmd: ['/some/command'])
    }

    def "test invalid command"() {
        given:
        def app = new App()
        String[] args = ["--config-cmd", ""]

        when:
        new CommandLine(app).parseArgs(args)
        app.prepareConfig()
        then:
        thrown(IllegalCliArgumentException)

    }

    def "test valid working directory"() {
        given:
        def app = new App()
        String[] args = ["--config-working-dir", "/work/dir"]

        when:
        new CommandLine(app).parseArgs(args)
        then:
        app.@workingDir == "/work/dir"

        when:
        def config = app.prepareConfig()
        then:
        config == new ContainerConfig(workingDir: '/work/dir')
    }

    def "test invalid working directory"() {
        given:
        def app = new App()
        String[] args = ["--config-working-dir", "  "]

        when:
        new CommandLine(app).parseArgs(args)
        app.prepareConfig()
        then:
        thrown(IllegalCliArgumentException)

    }
}

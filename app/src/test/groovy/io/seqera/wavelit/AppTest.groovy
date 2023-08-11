package io.seqera.wavelit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.seqera.wave.api.ContainerConfig
import io.seqera.wavelit.exception.IllegalCliArgumentException
import picocli.CommandLine
import spock.lang.Specification

class AppTest extends Specification {

    WireMockServer wireMockServer
    def setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080))
        wireMockServer.start()

        def jsonContent = getResourceFileContent("config.json")
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/api/data"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(jsonContent)
                        )
        )
    }

    def cleanup() {
        wireMockServer.stop()
    }

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
    def "test valid config file from a path"() {
        given:
        def app = new App()
        String[] args = ["--config-file", getResourceFilePath("config.json")]

        when:
        new CommandLine(app).parseArgs(args)
        then:
        app.@configFile == getResourceFilePath("config.json")

        when:
        def config = app.prepareConfig()
        then:
        config.entrypoint == ['entrypoint']
        def layer = config.layers[0]
        layer.location == "https://location"
        layer.gzipDigest == "sha256:gzipDigest"
        layer.tarDigest == "sha256:tarDigest"
        layer.gzipSize == 100
    }

    def "test valid config file from a URL"() {
        given:
        def app = new App()
        String[] args = ["--config-file", "http://localhost:8080/api/data"]

        when:
        new CommandLine(app).parseArgs(args)
        then:
        app.@configFile == "http://localhost:8080/api/data"

        when:
        def config = app.prepareConfig()
        then:
        config.entrypoint == ['entrypoint']
        def layer = config.layers[0]
        layer.location == "https://location"
        layer.gzipDigest == "sha256:gzipDigest"
        layer.tarDigest == "sha256:tarDigest"
        layer.gzipSize == 100
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

    String getResourceFilePath(String resourceName) {
        getClass().getClassLoader().getResource(resourceName).file
    }

    String getResourceFileContent(String resourceName) {
        new File(getClass().getClassLoader().getResource(resourceName).toURI()).text
    }
}

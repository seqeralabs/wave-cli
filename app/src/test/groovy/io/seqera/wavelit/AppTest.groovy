package io.seqera.wavelit


import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import java.nio.file.Files

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

    def 'should fail when passing both conda file and packages' () {
        given:
        def app = new App()
        String[] args = ["--conda-file", "foo", "--conda-package", "bar"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        app.validateArgs()

        then:
        thrown(IllegalCliArgumentException)
    }

    def 'should fail when passing both conda file and image' () {
        given:
        def app = new App()
        String[] args = ["--conda-file", "foo", "--image", "bar"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        app.validateArgs()
        then:
        thrown(IllegalCliArgumentException)
    }

    def 'should fail when passing both conda file and conmtainer file' () {
        given:
        def app = new App()
        String[] args = ["--conda-file", "foo", "--containerfile", "bar"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        app.validateArgs()
        then:
        thrown(IllegalCliArgumentException)
    }

    def 'should fail when passing both conda package and image' () {
        given:
        def app = new App()
        String[] args = ["--conda-package", "foo", "--image", "bar"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        app.validateArgs()
        then:
        thrown(IllegalCliArgumentException)
    }

    def 'should fail when passing both conda package and conmtainer file' () {
        given:
        def app = new App()
        String[] args = ["--conda-package", "foo", "--containerfile", "bar"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        app.validateArgs()
        then:
        thrown(IllegalCliArgumentException)
    }

    def 'should create container file from conda file' () {
        given:
        def folder = Files.createTempDirectory('test')
        def condaFile = folder.resolve('conda.yml');
        condaFile.text = 'MY CONDA FILE'
        and:
        def app = new App()
        String[] args = ["--conda-file", condaFile.toString()]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        def req = app.createRequest()
        then:
        new String(req.containerFile.decodeBase64()) == '''\
                FROM mambaorg/micromamba:1.4.9
                COPY --chown=$MAMBA_USER:$MAMBA_USER conda.yml /tmp/conda.yml
                RUN micromamba install -y -n base -f /tmp/conda.yml \\
                    && micromamba clean -a -y
                '''.stripIndent()
        and:
        new String(req.condaFile.decodeBase64()) == 'MY CONDA FILE'

        cleanup:
        folder?.deleteDir()
    }


    def 'should create container file from conda package' () {
        given:
        def app = new App()
        String[] args = ["--conda-package", "foo"]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        def req = app.createRequest()
        then:
        new String(req.containerFile.decodeBase64()) == '''\
                FROM mambaorg/micromamba:1.4.9
                RUN \\
                    micromamba install -y -n base -c seqera -c bioconda -c conda-forge -c defaults \\
                    foo \\
                    && micromamba clean -a -y
                '''.stripIndent()
        and:
        req.condaFile == null
    }

    def 'should create container file from conda package and custom options' () {
        given:
        def app = new App()
        String[] args = [
                "--conda-package", "foo",
                "--conda-package", "bar",
                "--conda-base-image", "my/mamba:latest",
                "--conda-channels", "alpha,beta",
                "--conda-run-command", "RUN one",
                "--conda-run-command", "RUN two",

        ]

        when:
        new CommandLine(app).parseArgs(args)
        and:
        def req = app.createRequest()
        then:
        new String(req.containerFile.decodeBase64()) == '''\
                FROM my/mamba:latest
                RUN \\
                    micromamba install -y -n base -c alpha -c beta \\
                    foo bar \\
                    && micromamba clean -a -y
                RUN one
                RUN two    
                '''.stripIndent()

        and:
        req.condaFile == null
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

    String getResourceFilePath(String resourceName) {
        getClass().getClassLoader().getResource(resourceName).file
    }

    String getResourceFileContent(String resourceName) {
        new File(getClass().getClassLoader().getResource(resourceName).toURI()).text
    }
}

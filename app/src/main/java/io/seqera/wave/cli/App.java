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

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.seqera.wave.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.seqera.wave.api.*;
import io.seqera.wave.cli.exception.BadClientResponseException;
import io.seqera.wave.cli.exception.ClientConnectionException;
import io.seqera.wave.cli.exception.IllegalCliArgumentException;
import io.seqera.wave.cli.exception.ReadyTimeoutException;
import io.seqera.wave.cli.json.JsonHelper;
import io.seqera.wave.cli.model.ContainerInspectResponseEx;
import io.seqera.wave.cli.model.ContainerSpecEx;
import io.seqera.wave.cli.model.SubmitContainerTokenResponseEx;
import io.seqera.wave.cli.util.BuildInfo;
import io.seqera.wave.cli.util.CliVersionProvider;
import io.seqera.wave.cli.util.DurationConverter;
import io.seqera.wave.cli.util.GptHelper;
import io.seqera.wave.cli.util.YamlHelper;
import io.seqera.wave.config.CondaOpts;
import io.seqera.wave.util.DockerIgnoreFilter;
import io.seqera.wave.util.Packer;
import org.apache.commons.lang3.StringUtils;
import org.semver4j.Semver;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import static io.seqera.wave.cli.util.Checkers.isEmpty;
import static io.seqera.wave.cli.util.Checkers.isEnvVar;
import static io.seqera.wave.cli.util.StreamHelper.tryReadStdin;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Wave cli main class
 */
@Command(name = "wave",
        description = "Wave command line tool",
        mixinStandardHelpOptions = true,
        versionProvider = CliVersionProvider.class,
        usageHelpAutoWidth = true)
public class App implements Runnable {

    static public final String DEFAULT_CONDA_CHANNELS = "conda-forge,bioconda";

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(App.class);

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final String DEFAULT_TOWER_ENDPOINT = "https://api.cloud.seqera.io";

    private static final List<String> VALID_PLATFORMS = List.of("amd64", "x86_64", "linux/amd64", "linux/x86_64", "arm64", "linux/arm64");

    private static final long _1MB = 1024 * 1024;

    @Option(names = {"-i", "--image"}, paramLabel = "''", description = "Container image name to be provisioned e.g alpine:latest.")
    private String image;

    @Option(names = {"-f", "--containerfile"}, paramLabel = "''", description = "Container file to be used to build the image e.g. ./Dockerfile.")
    private String containerFile;

    @Option(names = {"--tower-token"}, paramLabel = "''", description = "Tower service access token.")
    private String towerToken;

    @Option(names = {"--tower-endpoint"}, paramLabel = "''", description = "Tower service endpoint e.g. https://api.cloud.seqera.io.")
    private String towerEndpoint;

    @Option(names = {"--tower-workspace-id"}, paramLabel = "''", description = "Tower service workspace ID e.g. 1234567890.")
    private Long towerWorkspaceId;

    @Option(names = {"--build-repo", "--build-repository"}, paramLabel = "''", description = "The container repository where image build by Wave will stored e.g. docker.io/user/build.")
    private String buildRepository;

    @Option(names = {"--cache-repo", "--cache-repository"}, paramLabel = "''", description = "The container repository where image layer created by Wave will stored e.g. docker.io/user/cache.")
    private String cacheRepository;

    @Option(names = {"--wave-endpoint"}, paramLabel = "''", description = "Wave service endpoint e.g. https://wave.seqera.io.")
    private String waveEndpoint;

    @Option(names = {"--freeze", "-F"}, paramLabel = "false",  description = "Request a container freeze.")
    private boolean freeze;

    @Option(names = {"--platform"}, paramLabel = "''", description = "Platform to be used for the container build. One of: linux/amd64, linux/arm64.")
    private String platform;

    @Option(names = {"--await"}, paramLabel = "false", arity = "0..1", description = "Await the container build to be available. you can provide a timeout like --await 10m or 2s, by default its 15 minutes.")
    private Duration await;

    @Option(names = {"--context"}, paramLabel = "''",  description = "Directory path where the build context is stored e.g. /some/context/path.")
    private String contextDir;

    @Option(names = {"--layer"}, paramLabel = "''", description = "Directory path where a layer content is stored e.g. /some/layer/path")
    private List<String> layerDirs;

    @Option(names = {"--config-env"}, paramLabel = "''",  description = "Overwrite the environment of the image e.g. NAME=VALUE")
    private List<String> environment;

    @Option(names = {"--config-cmd"}, paramLabel = "''", description = "Overwrite the default CMD (command) of the image.")
    private String command;

    @Option(names = {"--config-entrypoint"}, paramLabel = "''", description = "Overwrite the default ENTRYPOINT of the image.")
    private String entrypoint;

    @Option(names = {"--config-file"}, paramLabel = "''", description = "Configuration file in JSON format to overwrite the default configuration of the image.")
    private String configFile;

    @Option(names = {"--config-working-dir"}, paramLabel = "''", description = "Overwrite the default WORKDIR of the image e.g. /some/work/dir.")
    private String workingDir;

    @Option(names = {"--conda-file"}, paramLabel = "''", description = "A Conda file used to build the container e.g. /some/path/conda.yaml.")
    private String condaFile;

    @Option(names = {"--conda-package", "--conda"}, paramLabel = "''", description = "One or more Conda packages used to build the container e.g. bioconda::samtools=1.17.")
    private List<String> condaPackages;

    @Option(names = {"--conda-base-image"}, paramLabel = "''", description = "Conda base image used to to build the container (default: ${DEFAULT-VALUE}).")
    private String condaBaseImage = CondaOpts.DEFAULT_MAMBA_IMAGE;

    @Option(names = {"--conda-run-command"}, paramLabel = "''", description = "Dockerfile RUN commands used to build the container.")
    private List<String> condaRunCommands;

    @Option(names = {"--conda-channels"}, paramLabel = "''", description = "Conda channels used to build the container (default: ${DEFAULT-VALUE}).")
    private String condaChannels = DEFAULT_CONDA_CHANNELS;

    @Option(names = {"--log-level"}, paramLabel = "''", description = "Set the application log level. One of: OFF, ERROR, WARN, INFO, DEBUG, TRACE and ALL")
    private String logLevel;

    @Option(names = {"-o","--output"}, paramLabel = "json|yaml",  description = "Output format. One of: json, yaml.")
    private String outputFormat;

    @Option(names = {"-s","--singularity"}, paramLabel = "false", description = "Enable Singularity build (experimental)")
    private boolean singularity;

    @Option(names = {"--dry-run"}, paramLabel = "false", description = "Simulate a request switching off the build container images")
    private boolean dryRun;

    @Option(names = {"--preserve-timestamp"}, paramLabel = "false", description = "Preserve timestamp of files in the build context and layers created by Wave")
    private boolean preserveTimestamp;

    @Option(names = {"--info"}, paramLabel = "false", description = "Show Wave client & service information")
    private boolean info;

    private BuildContext buildContext;

    private ContainerConfig containerConfig;

    @Option(names = {"--inspect"}, paramLabel = "false", description = "Inspect specified container image")
    private boolean inspect;

    @Option(names = {"--include"}, paramLabel = "''", description = "Include one or more containers in the specified base image")
    private List<String> includes;

    @Option(names = {"--name-strategy"}, paramLabel = "<value>", description = "Specify the name strategy for the container name, it can be 'none' or 'tagPrefix' or 'imageSuffix'")
    private ImageNameStrategy nameStrategy;

    @Option(names = {"-m","--mirror"}, paramLabel = "false", description = "Enable container mirror mode'")
    private boolean mirror;

    @Option(names = {"--scan-mode"}, paramLabel = "<value>", description = "Specify container security scan mode, it can be 'none', 'async' or 'required'")
    private ScanMode scanMode;

    @Option(names = {"--scan-level"}, paramLabel = "<value>", description = "Specify one or more security scan vulnerabilities level allowed in the container e.g. low,medium,high,critical")
    private List<ScanLevel> scanLevels;

    @Option(names = {"--build-compression"}, paramLabel = "<value>", description = "Specify the compression algorithm to be used for the build context, it can be 'gzip', 'zstd' or 'estargz'")
    private BuildCompression.Mode buildCompression;


    @CommandLine.Parameters
    List<String> prompt;

    static private String[] makeArgs(String[] args) {
        String stdin = tryReadStdin();
        if( stdin==null )
            return args;

        List<String> result = new ArrayList<>(Arrays.asList(args));
        result.add("--");
        result.add(stdin);
        return result.toArray(new String[args.length+2]);
    }

    public static void main(String[] args) {
        try {
            final App app = new App();
            final CommandLine cli = new CommandLine(app);

            //register duration converter
            cli.registerConverter(Duration.class, new DurationConverter());

            // add examples in help
            cli
                .getCommandSpec()
                .usageMessage()
                .footer(readExamples("usage-examples.txt"));

            final CommandLine.ParseResult result = cli.parseArgs(makeArgs(args));
            if( !result.originalArgs().contains("--") ) {
                // reset prompt if `-- was not entered
                app.prompt=null;
            }

            if( result.matchedArgs().size()==0 || result.isUsageHelpRequested() ) {
                cli.usage(System.out);
            }
            else if( result.isVersionHelpRequested() ) {
                System.out.println(BuildInfo.getFullVersion());
                return;
            }

            app.setLogLevel();
            app.defaultArgs();
            if( app.info ) {
                app.printInfo();
            }
            else if( app.inspect ) {
                app.inspect();
            }
            else {
                app.run();
            }
        }
        catch (IllegalCliArgumentException | CommandLine.ParameterException | BadClientResponseException |
               ReadyTimeoutException | ClientConnectionException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String readExamples(String exampleFile) {
        try(InputStream stream = App.class.getResourceAsStream(exampleFile)) {
            return new String(stream.readAllBytes());
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to read usge examples", e);
        }
    }

    protected void setLogLevel() {
        if( !isEmpty(logLevel) ) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.valueOf(logLevel));
        }
    }

    protected void defaultArgs() {
        if( "null".equals(towerEndpoint) )  {
            towerEndpoint = null;
        }
        else if( isEmpty(towerEndpoint) && System.getenv().containsKey("TOWER_API_ENDPOINT") ) {
            towerEndpoint = System.getenv("TOWER_API_ENDPOINT");
        }
        else if( isEmpty(towerEndpoint) ) {
            towerEndpoint = DEFAULT_TOWER_ENDPOINT;
        }

        if( "null".equals(towerToken) ) {
            towerToken = null;
        }
        else if( isEmpty(towerToken) && System.getenv().containsKey("TOWER_ACCESS_TOKEN") ) {
            towerToken = System.getenv("TOWER_ACCESS_TOKEN");
        }
        if( towerWorkspaceId==null && System.getenv().containsKey("TOWER_WORKSPACE_ID") ) {
            towerWorkspaceId = Long.valueOf(System.getenv("TOWER_WORKSPACE_ID"));
        }

        if( isEmpty(waveEndpoint) && System.getenv().containsKey("WAVE_ENDPOINT") ) {
            waveEndpoint = System.getenv("WAVE_ENDPOINT");
        }
        else if( isEmpty(waveEndpoint) ) {
            waveEndpoint = Client.DEFAULT_ENDPOINT;
        }

    }

    protected void validateArgs() {
        if( !isEmpty(image) && !isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Argument --image and --containerfile conflict each other - Specify an image name or a container file for the container to be provisioned");

        if( isEmpty(image) && isEmpty(containerFile) && isEmpty(condaFile) && condaPackages==null && isEmpty(prompt) )
            throw new IllegalCliArgumentException("Provide either a image name or a container file for the Wave container to be provisioned");

        if( isEmpty(towerToken) && !isEmpty(buildRepository) )
            throw new IllegalCliArgumentException("Specify the Tower access token required to authenticate the access to the build repository either by using the --tower-token option or the TOWER_ACCESS_TOKEN environment variable");

        // -- check conda options
        if( !isEmpty(condaFile) && condaPackages!=null )
            throw new IllegalCliArgumentException("Option --conda-file and --conda-package conflict each other");

        if( !isEmpty(condaFile) && !isEmpty(image) )
            throw new IllegalCliArgumentException("Option --conda-file and --image conflict each other");

        if( !isEmpty(condaFile) && !isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Option --conda-file and --containerfile conflict each other");

        if( condaPackages!=null && !isEmpty(image) )
            throw new IllegalCliArgumentException("Option --conda-package and --image conflict each other");

        if( condaPackages!=null && !isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Option --conda-package and --containerfile conflict each other");

        if( !isEmpty(outputFormat) && !List.of("json","yaml").contains(outputFormat) ) {
            final String msg = String.format("Invalid output format: '%s' - expected value: json, yaml", outputFormat);
            throw new IllegalCliArgumentException(msg);
        }

        if( !isEmpty(condaFile) && !Files.exists(Path.of(condaFile)) )
            throw new IllegalCliArgumentException("The specified Conda file path cannot be accessed - offending file path: " + condaFile);

        if( !isEmpty(contextDir) && isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Option --context requires the use of a container file");

        if( singularity && !freeze )
            throw new IllegalCliArgumentException("Singularity build requires enabling freeze mode");

        if( !isEmpty(contextDir) ) {
            // check that a container file has been provided
            if( isEmpty(containerFile) )
                throw new IllegalCliArgumentException("Container context directory is only allowed when a build container file is provided");
            Path location = Path.of(contextDir);
            // check it exist
            if( !Files.exists(location) )
                throw new IllegalCliArgumentException("Context path does not exists - offending value: " + contextDir);
            // check it's a directory
            if( !Files.isDirectory(location) )
                throw new IllegalCliArgumentException("Context path is not a directory - offending value: " + contextDir);
        }

        if( mirror && !isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Argument --mirror and --containerfile conflict each other");

        if( mirror && !isEmpty(condaFile) )
            throw new IllegalCliArgumentException("Argument --mirror and --conda-file conflict each other");

        if( mirror && !isEmpty(condaPackages) )
            throw new IllegalCliArgumentException("Argument --mirror and --conda-package conflict each other");

        if( mirror && !isEmpty(contextDir) )
            throw new IllegalCliArgumentException("Argument --mirror and --context conflict each other");

        if( mirror && freeze )
            throw new IllegalCliArgumentException("Argument --mirror and --freeze conflict each other");

        if( mirror && isEmpty(buildRepository) )
            throw new IllegalCliArgumentException("Option --mirror and requires the use of a build repository");

        if( dryRun && await != null )
            throw new IllegalCliArgumentException("Options --dry-run and --await conflicts each other");

        if( !isEmpty(platform) && !VALID_PLATFORMS.contains(platform) )
            throw new IllegalCliArgumentException(String.format("Unsupported container platform: '%s'", platform));

    }

    protected Client client() {
        return new Client().withEndpoint(waveEndpoint);
    }

    protected SubmitContainerTokenRequest createRequest() {
        return new SubmitContainerTokenRequest()
                .withContainerImage(image)
                .withContainerFile(containerFileBase64())
                .withPackages(packagesSpec())
                .withContainerPlatform(platform)
                .withTimestamp(OffsetDateTime.now())
                .withBuildRepository(buildRepository)
                .withCacheRepository(cacheRepository)
                .withBuildContext(buildContext)
                .withContainerConfig(containerConfig)
                .withTowerAccessToken(towerToken)
                .withTowerWorkspaceId(towerWorkspaceId)
                .withTowerEndpoint(towerEndpoint)
                .withFormat( singularity ? "sif" : null )
                .withFreezeMode(freeze)
                .withDryRun(dryRun)
                .withContainerIncludes(includes)
                .withNameStrategy(nameStrategy)
                .withMirror(mirror)
                .withScanMode(scanMode)
                .withScanLevels(scanLevels)
                .withBuildCompression(compression(buildCompression))
                ;
    }

    BuildCompression compression(BuildCompression.Mode mode) {
        if( mode==null )
            return null;
        return new BuildCompression().withMode(mode);
    }

    public void inspect() {
        final Client client = client();
        final ContainerInspectRequest req = new ContainerInspectRequest()
                .withContainerImage(image)
                .withTowerAccessToken(towerToken)
                .withTowerWorkspaceId(towerWorkspaceId)
                .withTowerEndpoint(towerEndpoint)
                ;

        final ContainerInspectResponse resp = client.inspect(req);
        final ContainerSpecEx spec = new ContainerSpecEx(resp.getContainer());
        System.out.println(dumpOutput(new ContainerInspectResponseEx(spec)));
    }

    @Override
    public void run() {
        // validate the command line args
        validateArgs();
        // prepare the request
        buildContext = prepareContext();
        containerConfig = prepareConfig();
        // create the wave request
        SubmitContainerTokenRequest request = createRequest();
        // creat the client
        final Client client = client();
        // submit it
        SubmitContainerTokenResponse resp = client.submit(request);
        // await container request to be completed
        if( await != null && resp.requestId!=null && resp.succeeded==null ) {
            ContainerStatusResponse status = client.awaitCompletion(resp.requestId, await);
            // print the wave container name
            System.out.println(dumpOutput(new SubmitContainerTokenResponseEx(resp, status)));
        }
        else {
            // print the wave container name
            System.out.println(dumpOutput(resp));
        }
    }

    private String encodePathBase64(String value) {
        try {
            if( isEmpty(value) )
                return null;
            // read the text from a URI resource and encode to base64
            if( value.startsWith("file:/") || value.startsWith("http://") || value.startsWith("https://")) {
                try(InputStream stream=new URI(value).toURL().openStream()) {
                    return Base64.getEncoder().encodeToString(stream.readAllBytes());
                }
            }
            // read the text from a local file and encode to base64
            return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(value)));
        }
        catch (URISyntaxException e) {
            throw new IllegalCliArgumentException("Invalid container file URI path - offending value: " + value, e);
        }
        catch (NoSuchFileException | FileNotFoundException e) {
            throw new IllegalCliArgumentException("File not found: " + value, e);
        }
        catch (IOException e) {
            String msg = String.format("Unable to read resource: %s - reason: %s" + value, e.getMessage());
            throw new IllegalCliArgumentException(msg, e);
        }
    }

    private String encodeStringBase64(String value) {
        if( isEmpty(value) )
            return null;
        else
            return Base64.getEncoder().encodeToString(value.getBytes());
    }

    protected BuildContext prepareContext()  {
        if( isEmpty(contextDir) )
            return null;
        BuildContext result;
        try {
            if( isWindows )
                log.warn("Build context file permission may not be honoured when using Windows OS");

            //check for .dockerignore file in context directory
            final Path dockerIgnorePath = Path.of(contextDir).resolve(".dockerignore");
            final DockerIgnoreFilter filter = Files.exists(dockerIgnorePath)
                    ? DockerIgnoreFilter.fromFile(dockerIgnorePath)
                    : null;
            final Packer packer = new Packer()
                            .withFilter(filter)
                            .withPreserveFileTimestamp(preserveTimestamp);
            result = BuildContext.of(packer.layer(Path.of(contextDir)));
        }
        catch (IOException e) {
            throw new IllegalCliArgumentException("Unexpected error while preparing build context - cause: "+e.getMessage(), e);
        }
        if( result.gzipSize > 5*_1MB )
            throw new IllegalCliArgumentException("Build context cannot be bigger of 5 MiB");
        return result;
    }

    protected ContainerConfig prepareConfig() {
        ContainerConfig result = new ContainerConfig();

        // add configuration from config file if specified
        if( configFile != null ){
            if( "".equals(configFile.trim()) ) throw new IllegalCliArgumentException("The specified config file is an empty string");
            result = readConfig(configFile);
        }

        // add the entrypoint if specified
        if( entrypoint!=null )
            result.entrypoint = List.of(entrypoint);

        // add the command if specified
        if( command != null ){
            if( "".equals(command.trim()) ) throw new IllegalCliArgumentException("The command cannot be an empty string");
            result.cmd = List.of(command);
        }

        //add environment variables if specified
        if( environment!=null ) {
            for( String it : environment ) {
                if( !isEnvVar(it) ) throw new IllegalCliArgumentException("Invalid environment variable syntax - offending value: " + it);
            }
            result.env = environment;
        }

        //add the working directory if specified
        if( workingDir != null ){
            if( "".equals(workingDir.trim()) ) throw new IllegalCliArgumentException("The working directory cannot be empty string");
            result.workingDir = workingDir;
        }

        // add the layers to the resulting config if specified
        if( layerDirs!=null ) for( String it : layerDirs ) {
            final Path loc = Path.of(it);
            if( !Files.isDirectory(loc) ) throw new IllegalCliArgumentException("Not a valid container layer directory - offering path: "+loc);
            ContainerLayer layer;
            try {
                layer = new Packer()
                        .withPreserveFileTimestamp(preserveTimestamp)
                        .layer(loc);
                result.layers.add(layer);
            }
            catch (IOException e ) {
                throw new RuntimeException("Unexpected error while packing container layer at path: " + loc, e);
            }
            if( layer.gzipSize > _1MB )
                throw new RuntimeException("Container layer cannot be bigger of 1 MiB - offending path: " + loc);
        }

        // check all size
        long size = 0;
        for(ContainerLayer it : result.layers ) {
            if( it.location.startsWith("data:"))
                size += it.gzipSize;
        }
        if( size>=10 * _1MB )
            throw new RuntimeException("Compressed container layers cannot exceed 10 MiB");

        // return the result
        return !result.empty() ? result : null;
    }

    private ContainerInspectRequest inspectRequest(String image) {
        return new ContainerInspectRequest()
                .withContainerImage(image)
                .withTowerEndpoint(towerEndpoint)
                .withTowerAccessToken(towerToken)
                .withTowerWorkspaceId(towerWorkspaceId);
    }

    private CondaOpts condaOpts() {
        return new CondaOpts()
                .withMambaImage(condaBaseImage)
                .withCommands(condaRunCommands)
                ;
    }

    protected String containerFileBase64() {
        return !isEmpty(containerFile)
                ? encodePathBase64(containerFile)
                : null;
    }

    protected PackagesSpec packagesSpec() {
        if( !isEmpty(condaFile) ) {
            return new PackagesSpec()
                    .withType(PackagesSpec.Type.CONDA)
                    .withCondaOpts(condaOpts())
                    .withEnvironment(encodePathBase64(condaFile))
                    .withChannels(condaChannels())
                    ;
        }

        if( !isEmpty(condaPackages) ) {
            return new PackagesSpec()
                    .withType(PackagesSpec.Type.CONDA)
                    .withCondaOpts(condaOpts())
                    .withEntries(condaPackages)
                    .withChannels(condaChannels())
                    ;
        }

        if( !isEmpty(prompt) ) {
            return GptHelper.grabPackages(prompt.stream().collect(Collectors.joining(" ")));
        }

        return null;
    }

    protected String dumpOutput(SubmitContainerTokenResponseEx resp) {
        if( outputFormat==null && !resp.succeeded ) {
            String message = "Container provisioning did not complete successfully";
            if( !StringUtils.isEmpty(resp.reason) )
                message += "\n- Reason: " + resp.reason;
            if( !StringUtils.isEmpty(resp.detailsUri) )
                message += "\n- Find out more here: " + resp.detailsUri;
            throw new BadClientResponseException(message);
        }
        return dumpOutput((SubmitContainerTokenResponse)resp);
    }

    protected String dumpOutput(SubmitContainerTokenResponse resp) {
        if( "yaml".equals(outputFormat) ) {
            return YamlHelper.toYaml(resp);
        }
        if( "json".equals(outputFormat) ) {
            return JsonHelper.toJson(resp);
        }
        if( outputFormat!=null )
            throw new IllegalArgumentException("Unexpected output format: "+outputFormat);

        return resp.targetImage;
    }

    protected String dumpOutput(ContainerInspectResponseEx resp) {
        if( "json".equals(outputFormat) || outputFormat==null ) {
            return JsonHelper.toJson(resp);
        }
        if( "yaml".equals(outputFormat) ) {
            return YamlHelper.toYaml(resp);
        }
        throw new IllegalArgumentException("Unexpected output format: "+outputFormat);
    }

    protected ContainerConfig readConfig(String path) {
        try {
            if( path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file:/")) {
                try (InputStream stream=new URL(path).openStream()) {
                    return JsonHelper.fromJson(new String(stream.readAllBytes()), ContainerConfig.class);
                }
            }
            else {
                return JsonHelper.fromJson(Files.readString(Path.of(path)), ContainerConfig.class);
            }
        }
        catch (FileNotFoundException | NoSuchFileException e) {
            throw new IllegalCliArgumentException("Invalid container config file - File not found: " + path);
        }
        catch (IOException e) {
            String msg = String.format("Unable to read container config file: %s - Cause: %s", path, e.getMessage());
            throw new IllegalCliArgumentException(msg, e);
        }
    }

    protected List<String> condaChannels() {
        if( condaChannels==null )
            return null;
        // parse channels
        return Arrays.stream(condaChannels.split("[, ]"))
                .map(String::trim)
                .filter(it -> !isEmpty(it))
                .collect(Collectors.toList());
    }

    void printInfo() {
        System.out.println(String.format("Client:"));
        System.out.println(String.format(" Version   : %s", BuildInfo.getVersion()));
        System.out.println(String.format(" System    : %s", System. getProperty("os.name")));

        System.out.println(String.format("Server:"));
        System.out.println(String.format(" Version   : %s", serviceVersion()));
        System.out.println(String.format(" Endpoint  : %s", waveEndpoint));
    }

    protected String serviceVersion() {
        return serviceVersion0(getServiceVersion(), "1.13.0");
    }

    protected String serviceVersion0(String current, String required) {
        Semver current0 = new Semver(current);
        Semver required0 = new Semver(required);
        return current0.compareTo(required0) >= 0
                ? current
                : current + " (required: " + required0 + ")";
    }

    protected String getServiceVersion() {
        fixServiceInfoConstructor();
        try {
            return client().serviceInfo().version;
        }
        catch (Throwable e) {
            log.debug("Unexpected error while retrieving Wave service info", e);
            return "-";
        }
    }

    private void fixServiceInfoConstructor() {
        /*
         dirty hack to force Graal compiler to recognise the access via reflection of the
         ServiceInfo constructor
         */
        try {
            Class<?> myClass = ServiceInfo.class;
            Constructor<?> constructor = myClass.getConstructor(String.class, String.class);
            constructor.newInstance("Foo", "Bar");
        }
        catch (Exception e) {
            log.debug("Unable to load constructor", e);
        }

    }
}

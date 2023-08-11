/*
 *  Copyright (c) 2023, Seqera Labs.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *  This Source Code Form is "Incompatible With Secondary Licenses", as
 *  defined by the Mozilla Public License, v. 2.0.
 */

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.seqera.wavelit;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import io.seqera.wave.api.BuildContext;
import io.seqera.wave.api.ContainerConfig;
import io.seqera.wave.api.ContainerLayer;
import io.seqera.wave.api.SubmitContainerTokenRequest;
import io.seqera.wave.api.SubmitContainerTokenResponse;
import io.seqera.wave.util.Packer;
import io.seqera.wavelit.exception.IllegalCliArgumentException;
import io.seqera.wavelit.util.ConfigFileProcessor;
import io.seqera.wavelit.util.CliVersionProvider;
import picocli.CommandLine;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Wavelit entrypoint class
 */
@Command(name = "wavelit", description = "Wave command line tool", mixinStandardHelpOptions = true, versionProvider = CliVersionProvider.class)
public class App implements Runnable {

    private static final long _1MB = 1024 * 1024;

    @Option(names = {"-i", "--image"}, description = "Container image name to be provisioned.")
    private String image;

    @Option(names = {"-c", "--containerfile"}, description = "Container file (i.e. Dockerfile) to be used to build the image.")
    private String containerFile;

    @Option(names = {"--tower-token"}, description = "Tower service access token.")
    private String towerToken;

    @Option(names = {"--tower-endpoint"}, description = "Tower service endpoint (default: ${DEFAULT-VALUE}).")
    private String towerEndpoint = "https://api.tower.nf";

    private Long towerWorkspaceId;

    @Option(names = {"--build-repo"}, description = "The container repository where image build by Wave will stored.")
    private String buildRepository;

    @Option(names = {"--cache-repo"}, description = "The container repository where image layer created by Wave will stored.")
    private String cacheRepository;

    @Option(names = {"--wave-endpoint"}, description = "Wave service endpoint (default: ${DEFAULT-VALUE}).")
    private String waveEndpoint = Client.DEFAULT_ENDPOINT;

    @Option(names = {"--freeze"}, description = "Request a container freeze.")
    private boolean freeze;

    @Option(names = {"--platform"}, description = "Platform to be used for the container build e.g. linux/amd64, linux/arm64.")
    private String platform;

    @Option(names = {"--await"}, description = "Await the container build to be available.")
    private boolean await;

    @Option(names = {"--context"}, description = "Directory path where the build context is stored.")
    private String contextDir;

    @Option(names = {"--layer"})
    private List<String> layerDirs;

    @Option(names = {"--config-cmd"}, description = "Overwrite the default CMD (command) of the image.")
    private String command;

    @Option(names = {"--config-entrypoint"}, description = "Overwrite the default ENTRYPOINT of the image.")
    private String entrypoint;

    @Option(names = {"--config-file"}, description = "Configuration file in JSON format to overwrite the default configuration of the image.")
    private String configFile;

    private BuildContext buildContext;

    private ContainerConfig containerConfig;

    public static void main(String[] args) {
        try {
            CommandLine.run(new App(), args);
        }
        catch (CommandLine.ExecutionException e) {
            Throwable err = e.getCause()!=null ? e.getCause() : e;
            System.err.println(err.getMessage());
            System.exit(1);
        }
        catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected void defaultArgs() {
        if( "null".equals(towerEndpoint) )  {
            towerEndpoint = null;
        }
        else if( isEmpty(towerEndpoint) && System.getenv().containsKey("TOWER_API_ENDPOINT") ) {
            towerEndpoint = System.getenv("TOWER_API_ENDPOINT");
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
    }

    protected void validateArgs() {
        if( !isEmpty(image) && !isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Argument --image and --containerfile conflict each other - Specify an image name or a container file for the container to be provisioned");

        if( isEmpty(image) && isEmpty(containerFile) )
            throw new IllegalCliArgumentException("Provide either a image name or a container file for the Wave container to be provisioned");

        if( freeze && isEmpty(buildRepository) )
            throw new IllegalCliArgumentException("Specify the build repository where the freeze container will be pushed by using the --build-repo option");

        if( isEmpty(towerToken) && !isEmpty(buildRepository) )
            throw new IllegalCliArgumentException("Specify the Tower access token required to authenticate the access to the build repository either by using the --tower-token option or the TOWER_ACCESS_TOKEN environment variable");

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
    }

    protected Client client() {
        return new Client().withEndpoint(waveEndpoint);
    }

    protected SubmitContainerTokenRequest createRequest() {
        return new SubmitContainerTokenRequest()
                .withContainerImage(image)
                .withContainerFile(encodeBase64(containerFile))
                .withContainerPlatform(platform)
                .withTimestamp(OffsetDateTime.now())
                .withBuildRepository(buildRepository)
                .withCacheRepository(cacheRepository)
                .withBuildContext(buildContext)
                .withContainerConfig(containerConfig)
                .withTowerAccessToken(towerToken)
                .withTowerWorkspaceId(towerWorkspaceId)
                .withTowerEndpoint(towerEndpoint)
                .withFreezeMode(freeze);
    }

    @Override
    public void run() {
        // default Args
        defaultArgs();
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
        // await build to be completed
        if( await && !isEmpty(resp.buildId) )
            client.awaitImage(resp.targetImage);
        // print the wave container name
        System.out.println( freeze
                ? resp.containerImage
                : resp.targetImage );
    }

    private String encodeBase64(String value) {
        try {
            if( isEmpty(value) )
                return null;
            // check if it's a file path
            if( value.startsWith("/") || value.startsWith("./") ) {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(value)));
            }
            if( value.startsWith("file:/") || value.startsWith("http://") || value.startsWith("https://")) {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(new URI(value))));
            }
            // parse a plain dockerfile string
            return Base64.getEncoder().encodeToString(value.getBytes());
        }
        catch (URISyntaxException e) {
            throw new IllegalCliArgumentException("Invalid container file URI path - offending value: " + value, e);
        }
        catch (IOException e) {
            throw new IllegalCliArgumentException("Unable to read container file - reason: " + e.getMessage(), e);
        }
    }

    protected BuildContext prepareContext()  {
        if( isEmpty(contextDir) )
            return null;
        BuildContext result = null;
        try {
            result = BuildContext.of(new Packer().layer(Path.of(contextDir)));
        }
        catch (IOException e) {
            throw new RuntimeException("Unexpected error while preparing build context - cause: "+e.getMessage(), e);
        }
        if( result.gzipSize > 5*_1MB )
            throw new RuntimeException("Build context cannot be bigger of 5 MiB");
        return result;
    }

    protected ContainerConfig prepareConfig() {
        ContainerConfig result = new ContainerConfig();

        // add configuration from config file if specified
        if( configFile != null ){
            if( "".equals(configFile.trim()) ) throw new IllegalCliArgumentException("The specified config file is an empty string");
            try {
                result = ConfigFileProcessor.process(configFile);
            } catch (IOException e) {
                throw new RuntimeException("Unexpected error while setting the configuration from config file: "+e);
            }
        }

        // add the entrypoint if specified
        if( entrypoint!=null )
            result.entrypoint = List.of(entrypoint);

        // add the command if specified
        if( command != null ){
            if( "".equals(command) ) throw new IllegalCliArgumentException("The specified command is an empty string");
            result.cmd = List.of(command);
        }

        // add the layers to the resulting config if specified
        if( layerDirs!=null ) for( String it : layerDirs ) {
            final Path loc = Path.of(it);
            if( !Files.isDirectory(loc) ) throw new IllegalCliArgumentException("Not a valid container layer directory - offering path: "+loc);
            ContainerLayer layer;
            try {
                result.layers.add( layer=new Packer().layer(loc) );
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
            size += it.gzipSize;
        }
        if( size>=10 * _1MB )
            throw new RuntimeException("Compressed container layers cannot exceed 10 MiB");

        // return the result
        return !result.empty() ? result : null;
    }
}

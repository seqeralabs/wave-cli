# wave-cli

Nextflow plugin providing [Wave CLI](https://github.com/seqeralabs/wave-cli) functionality.

This plugin enables Wave container provisioning via the `nextflow plugin wave` command, providing all the CLI features of the standalone Wave CLI tool directly within Nextflow.

### Summary 

Wave allows augmenting existing containers and building containers on demand so
that it can be used in your Docker (replace-with-your-own-fav-container-engine) workflow.

### Features

* Build container images on-demand for a given container file (aka Dockerfile);
* Build container images on-demand based on one or more [Conda](https://conda.io/) packages;
* Build container images for a specified target platform (currently linux/amd64 and linux/arm64);
* Push and cache built containers to a user-provided container repository;
* Push Singularity native container images to OCI-compliant registries;
* Mirror (ie. copy) container images on-demand to a given registry;
* Scan container images on-demand for security vulnerabilities;
  
### Installation 

Install the wave-cli plugin in your Nextflow environment:

```bash
nextflow plugin install wave-cli
```

Alternatively, add the plugin to your `nextflow.config`:

```groovy
plugins {
    id 'wave-cli'
}
```

### Get started

1. Create a basic Dockerfile file (or use an existing one)
   
    ```bash
    cat << EOF > ./Dockerfile
    FROM alpine 

    RUN apk update && apk add bash cowsay \
            --update-cache \
            --repository https://alpine.global.ssl.fastly.net/alpine/edge/community \
            --repository https://alpine.global.ssl.fastly.net/alpine/edge/main \
            --repository https://dl-3.alpinelinux.org/alpine/edge/testing
    EOF
    ```

2. Run it provisioning the container on-the-fly


    ```bash
    docker run --rm $(nextflow plugin wave -f ./Dockerfile) cowsay "Hello world"
    ```


### Examples 

#### Augment a container image 

1. Create a directory holding the files to be added to your container:

    ```bash
    mkdir -p new-layer/usr/local/bin
    printf 'echo Hello world!' > new-layer/usr/local/bin/hello.sh 
    chmod +x new-layer/usr/local/bin/hello.sh
    ```

2. Augment the container with the local layer and run with Docker:

    ```bash
    container=$(nextflow plugin wave -i alpine --layer new-layer)
    docker run $container sh -c hello.sh
    ```

#### Build a container with Dockerfile 

1. Create a Dockerfile for your container image: 

    ```bash
    cat << EOF > ./Dockerfile
    FROM alpine 
    ADD hello.sh /usr/local/bin/
    EOF
    ```

2. Create the build context directory:

    ```bash
    mkdir -p build-context/
    printf 'echo Hello world!' > build-context/hello.sh 
    chmod +x build-context/hello.sh 
    ```

3. Build and run the container on the fly:

    ```bash
    container=$(nextflow plugin wave -f Dockerfile --context build-context)
    docker run $container sh -c hello.sh
    ```

#### Build a Conda multi-packages container 

```bash
container=$(nextflow plugin nextflow plugin wave --conda-package bamtools=2.5.2 --conda-package samtools=1.17)
docker run $container sh -c 'bamtools --version && samtools --version'
```

#### Build a container by using a Conda environment file

1. Create the Conda environment file:

    ```bash
    cat << EOF > ./conda.yaml
    name: my-conda
    channels:
    - bioconda
    - conda-forge
    dependencies:
    - bamtools=2.5.2
    - samtools=1.17
    EOF
    ```

2. Build and run the container using the Conda environment:

    ```bash
    container=$(nextflow plugin wave --conda-file ./conda.yaml)
    docker run $container sh -c 'bamtools --version'
    ```


#### Build a container by using a Conda lock file

```bash
container=$(nextflow plugin nextflow plugin wave --conda-package https://prefix.dev/envs/pditommaso/wave/6x60arx3od13/conda-lock.yml)
docker run $container cowpy 'Hello, world!'
```


#### Build a Conda package container arm64 architecture

```bash
container=$(nextflow plugin nextflow plugin wave --conda-package fastp --platform linux/arm64)
docker run --platform linux/arm64 $container sh -c 'fastp --version'
```

#### Build a Singularity container using a Conda package and pushing to a OCI registry

```bash
container=$(nextflow plugin wave --singularity --conda-package bamtools=2.5.2 --build-repo docker.io/user/repo --freeze --await)
singularity exec $container bamtools --version
```

#### Mirror (aka copy) a container to another registry

```bash
container=$(nextflow plugin wave -i ubuntu:latest --mirror --build-repo <YOUR REGISTRY> --tower-token <YOUR ACCESS TOKEN> --await)
docker pull $container
```

#### Build a container and scan it for vulnerabilities

```bash
nextflow plugin wave --conda-package bamtools=2.5.2 --scan-mode required --await -o yaml
```

### Development

1. Install Java 21 

    ```bash
    sdk install java 21.0.1-graal
    ```

    or if it's already installed

   ```bash
   sdk use java 21.0.1-graal
   ```

2. Compile & run tests 

    ```bash
    ./gradlew check
    ```

3. Build and install plugin locally for development

    ```bash
    ./gradlew installPlugin
    ```

4. Test the plugin with Nextflow

    ```bash
    nextflow plugin wave --version
    ```

### Usage in Workflows

You can use Wave directly in your Nextflow workflows by installing the plugin and enabling Wave container provisioning in your `nextflow.config`:

```groovy
plugins {
    id 'wave-cli'
}
```

Note: This plugin provides CLI functionality via `nextflow plugin wave`. For workflow-level Wave integration, use the official `nf-wave` plugin instead.

# Wavelit 

Command line tool for Wave container provisioning service

### Summary 

Wavelit allows augmenting existing containers and building containers on-the-fly so
that it can be used in your Docker (replace-with-your-own-fav-container-engine) workflow.

### Features

- Dynamically add one or more container layers to existing images;
- Build container images on-demand for a given container file (aka Dockerfile);
- Build container images on-demand starting one or more [Conda](https://conda.io/) packages;
- Build container images on-demand starting one or more [Spack](https://spack.io/) packages;
- Build container images for a specified target platform (currently `linux/amd64` and `linux/arm64`);
- Push and cache built containers to a user provider container repository;

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
    docker run --rm $(wavelit -f ./Dockerfile) cowsay "Hello world"
    ```


### Examples 

#### Augment a container image 

1. Create a directory holding the files to be added to your container:

    ```bash
    mkdir -p new-layer/usr/local/bin
    printf 'echo Hello world!' > new-layer/usr/local/bin/hello.sh 
    chmod +x new-layer/usr/local/bin/hello.sh
    ```

2. Run the container via Wave 

    ```bash
    docker run $(wavelit -i alpine --layer new-layer) sh -c hello.sh
    ```

#### Build a container with Dockerfile 

1. Create a Dockerfile for your container image: 

    ```bash
    cat << EOF > ./Dockerfile
    FROM alpine 
    ADD hello.sh /usr/local/bin/
    EOF
    ```

1. Create the build context directory:

    ```bash
    mkdir -p build-context/
    printf 'echo Hello world!' > build-context/hello.sh 
    chmod +x build-context/hello.sh 
    ```

2. Build and run the container on the fly:

    ```bash
    docker run $(wavelit -f Dockerfile --context build-context) sh -c hello.sh
    ```

#### Build a Conda multi-packages container 

```bash
docker run $(wavelit --conda-package bamtools=2.5.2 --conda-package samtools=1.17) sh -c 'bamtools --version && samtools --version'
```

#### Build a Conda package container arm64 architecture

```bash
docker run --platform linux/arm64 $(wavelit --conda-package fastp --platform linux/arm64) sh -c 'fastp --version'
```

#### Build a Spack package container

```bash
docker run $(wavelit --spack-package cowsay) sh -c 'cowsay Hello world!'
```


### Development

1. Install GraalVM-Java 20.0.1 

    ```bash
    sdk install java  20.0.1-graal
    ```

2. Compile & run tests 

    ```bash
    ./gradlew check
    ```

3. Native compile

    ```bash
    ./gradlew app:nativeCompile
    ```

4. Run the native binary 

    ```bash
    ./app/build/native/nativeCompile/wavelit --version
    ```

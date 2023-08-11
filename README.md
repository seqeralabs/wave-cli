# Wavelit 

Command line tool for Wave container provisioning service

### Summary 

Wavelit allows augmenting existing containers and building containers on-the-fly so
that it can be used in your Docker (replace-with-your-own-fav-container-engine) workflow.

### Get started

1. Create a basic Dockefile file (or use an existing one)
   
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


### Development 

1. Install GraalVM 22.3.1 + Java 19 

    ```bash
    sdk install java 22.3.1.r19-grl
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

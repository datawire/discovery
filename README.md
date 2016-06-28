# Datawire Discovery

[![Kotlin](https://img.shields.io/badge/Kotlin-1.0.2-blue.svg)](https://kotlinlang.org/)

Datawire Discovery is an eventually consistent service discovery server by [Datawire](https://datawire.io) that is designed for running in the cloud.

# Running Locally (Testing/Experimentation User)

These instructions are for users that do not want to setup a development environment but are instead interested in just testing the Discovery server locally.

1. Build the Docker image:

```bash
./gradlew clean build dockerize
```

2. Modify the Discovery configuration file for single-tenancy:

Normally Discovery uses an HMAC to verify incoming JSON Web Tokens ("JWT") and enable multi-tenancy. In order to minimize friction the Discovery server supports a no-auth single tenant mode. To turn this feature on open the configuration file located at `discovery-web/config/discovery.json` and set the `authHandler.type` to "none".

3. Modify the Discovery cluster configuration file:

Discovery is usually run on AWS and we keep the AWS configuration as the default setup for the `cluster.xml` file used by Vert.x. For local development and testing this file need to be updated to use the Multicast TCP joiner mechanism. Open the `discovery-web/config/cluster.xml` file and then find `<multicast enabled="false" />` and change it to `<multicast enabled="true" />`. Similarly find `<aws enabled="true">` and set it to `<aws enabled="false">`.

4. Run the Docker image as a container:

```bash
./gradlew runDockerized
```

# Running Locally (Development; IGNORE THIS FOR NOW)

These instructions are a WIP. Use a recent version of Docker before attempting to run. It is known to work with >= 1.8.3, build f4bf5c7

1. Build the Docker Image

```bash
make discoball
```

2. Run the container

```bash
make discostart
```

3. To shut down the container before running a new build:

```bash
make discostop
```

# License

Datawire Discovery is open-source software licensed under **Apache 2.0**. Please see [LICENSE](LICENSE) for further details.

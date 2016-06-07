# Datawire Discovery

[![Kotlin](https://img.shields.io/badge/Kotlin-1.0.2-blue.svg)](https://kotlinlang.org/)

Datawire Discovery is an eventually consistent service discovery server by [Datawire](https://datawire.io) that is designed for running in the cloud.

# Running Locally (Development)

These instructions are a WIP. Use a recent version of Docker before attempting to run. It is known to work with >= 1.8.3, build f4bf5c7

1. Build the Docker Image

```bash
./gradlew clean build :discovery-web:buildDockerImage
```

2. Run the container

```bash
docker run --name disco -p 52689:52689 -v $(pwd)/discovery-web/config:/opt/discovery/config datawire/discovery:2.0.0`
```

# License

Datawire Discovery is open-source software licensed under **Apache 2.0**. Please see [LICENSE](LICENSE) for further details.
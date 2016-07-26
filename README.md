# Datawire discovery"

Datawire Discovery

# User Testing Instructions

A Docker container can be started by executing `make docker`

# Developer Instructions

Developers will likely be using a familiar Java IDE to interact with the codebase rather than relying on the Docker image.

## IntelliJ IDEA

Add a new `Application` Run/Debug Configuration `Run > Edit Configurations`. Then click the plus symbol to create a new Application configuration.

```text
Main-Class: io.vertx.core.Launcher
VM Options:
  -Dlogback.configurationFile=config/logback.xml
  -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
Program Arguments: run io.datawire.discovery.ServiceVerticle -conf config/discovery-develop.json
Working Directory: $MODULE_DIR$
Use Classpath of Module: discovery-web
```

# LicenseDatawire discovery is open-source software licensed under **Apache 2.0**. Please see [LICENSE](LICENSE) for further details.
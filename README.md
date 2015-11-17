# Boston # 

Boston is a service registration and discovery service built by (Datawire.io) named after "The Hub of The Universe": Boston, Massachusetts. Boston can also be used as general purpose broadcast notification service. Boston is written in Kotlin and therefore requires a JVM to run.

Note: "Datawire Hub" might actually be a better name long term... but I like Boston right now and I'm kinda on a Massachusetts naming kick.

# Features #

Boston is simple to run and interact with:

1. Single JAR or container deployment. High availability ("HA") support is still in development.
2. Native language clients for Java, Python, and Node.JS which are build by our protocol bridging language Quark!
3. Simple web socket or HTTP API for publishers and subscribers.

# Getting Started #

1. Download Boston from:
2. Ensure Java 8 is installed on the host machine.
3. Run Boston:

`java -jar boston-0.0.1.jar`



# Theory of Operation #

Boston is expected to run as a middleware component in a distributed system and uses web sockets to send and receive notifications about services that are joining and leaving the distributed system. When services connect to Boston they send a registration message containing information about the service such as its name and network address. Boston then broadcasts that message to all interested connected parties.

# Contributing #

TBD.

# License #

Apache 2.0
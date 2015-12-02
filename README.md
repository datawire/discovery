# Datawire Hub

[![Build Status](https://travis-ci.org/datawire/hub.svg?branch=master)](https://travis-ci.org/datawire/hub)
 
The Hub is a service registration and discovery service built by (Datawire.io) named after "The Hub of The Universe": Boston, Massachusetts. The Hub can also be used as general purpose broadcast notification service. Datawire Hub is written in Kotlin and therefore requires a JVM to run.

# Features

Boston is simple to run and interact with:

1. Single JAR or container deployment. High availability ("HA") support is still in development.
2. Native language clients for Java, Python, and Node.JS which are build by our protocol bridging language Quark!
3. Simple web socket or HTTP API for publishers and subscribers.

# Getting Started Quickly

We provide a Docker image for getting started with using Datawire Hub quickly. The Hub image can be acquired by invoking the following command:

`docker pull quay.io/datawire/hub:v1`

To run The Hub just boot the container with Docker:

`docker run datawire/hub:v1`

# Theory of Operation

Boston is expected to run as a middleware component in a distributed system and uses web sockets to send and receive notifications about services that are joining and leaving the distributed system. When services connect to Boston they send a registration message containing information about the service such as its name and network address. Boston then broadcasts that message to all interested connected parties.

# Contributing

## Developer Instructions

You need Quark for the Client.

# FAQ

Q: **Why Kotlin?**
A: We wanted to use the JVM for this important component in the Datawire stack because of the JVM's performance and its wide array of available networking libraries. However, Java itself is not a perfect language and in larger projects can become difficult to work with due to a combination of language deficencies (such as nullable types) and syntactic verbosity. Kotlin provides a more powerful and safer type system as well as compact and intuitive syntax that we believe will make extending, maintaining and delivering a high quality product quicker and easier.

# License

Datawire Hub is open-source software licensed under **Apache 2.0**. Please see [LICENSE](LICENSE) for further details.
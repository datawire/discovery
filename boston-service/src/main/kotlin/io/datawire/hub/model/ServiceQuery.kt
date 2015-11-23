package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonProperty


class ServiceQuery(@JsonProperty("service") val service: String): Message

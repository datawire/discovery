package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonProperty


class ServiceQuery(override val sender: String, @JsonProperty("service") val service: String): Message

package io.datawire.discovery.registry.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable
import java.net.URI


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Endpoint constructor(
    @JsonProperty("scheme") val scheme: String,
    @JsonProperty("host") val host: String,
    @JsonProperty("port") val port: Int
): Serializable {
    @JsonGetter("uri")
    fun toURI() = URI(scheme, null, host, port, null, null, null)
}
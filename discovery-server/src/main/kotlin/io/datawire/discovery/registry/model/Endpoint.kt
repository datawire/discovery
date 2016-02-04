package io.datawire.discovery.registry.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.net.URI


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Endpoint constructor(
    //@JsonProperty("name") val name: String,
    @JsonProperty("scheme") val scheme: String,
    @JsonProperty("host") val host: String,
    @JsonProperty("port") val port: Int
) {
    @JsonGetter("uri")
    fun toURI() = URI(scheme, null, host, port, null, null, null)
}
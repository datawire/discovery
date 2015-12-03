package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HubConfig(
    @JsonProperty("server")
    val server: ServerConfig
)

data class ServerConfig(
    @JsonProperty("port")
    val port: Int,

    @JsonProperty("bindHost")
    val bindHost: String
)
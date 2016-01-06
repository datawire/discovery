package io.datawire.hub.gateway

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.gateway.jwt.JwtProviderFactory


data class Configuration(
    @JsonProperty("jwt") val jwtFactory: JwtProviderFactory
)
package io.datawire.hub.auth

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.auth.jwt.JwtProviderFactory


data class Configuration(@JsonProperty("jwt") val jwtFactory: JwtProviderFactory)
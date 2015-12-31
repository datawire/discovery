package io.datawire.hub.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.service.ServiceRegistryFactory


data class Configuration(
    @JsonProperty
    val tenant: String,

    @JsonProperty
    val serviceRegistry: ServiceRegistryFactory,

    @JsonProperty
    val jwt: JwtProviderFactory
)
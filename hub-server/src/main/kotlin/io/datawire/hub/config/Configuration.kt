package io.datawire.hub.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.service.ServiceRegistryFactory
import io.datawire.hub.tenant.TenantResolver


data class Configuration(
    @JsonProperty
    val tenantResolver: TenantResolver,

    @JsonProperty
    val serviceRegistry: ServiceRegistryFactory,

    @JsonProperty
    val jwt: JwtProviderFactory
)
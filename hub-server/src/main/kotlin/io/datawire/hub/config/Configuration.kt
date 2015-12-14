package io.datawire.hub.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.service.ServiceRegistryFactory
import io.datawire.hub.tenant.TenantStoreFactory


data class Configuration(
    @JsonProperty
    val serviceRegistry: ServiceRegistryFactory,

    @JsonProperty
    val tenantStore: TenantStoreFactory
)
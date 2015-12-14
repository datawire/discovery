package io.datawire.hub.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.datawire.hub.tenant.TenantStore
import io.datawire.hub.vertx.VerticleBundle
import io.vertx.core.Verticle


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = SimpleServiceRegistryFactory::class)
)
interface ServiceRegistryFactory {
  fun build(tenantStore: TenantStore): VerticleBundle
}
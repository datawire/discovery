package io.datawire.hub.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.datawire.hub.config.JwtProviderFactory
import io.datawire.hub.tenant.model.TenantId
import io.datawire.hub.vertx.VerticleBundle
import io.vertx.core.Vertx


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = SimpleServiceRegistryFactory::class)
)
interface ServiceRegistryFactory {
  fun build(vertx: Vertx, tenant: TenantId, jwt: JwtProviderFactory): VerticleBundle
}
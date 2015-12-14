package io.datawire.hub.service

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.tenant.TenantStore
import io.datawire.hub.vertx.VerticleBundle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject


data class SimpleServiceRegistryFactory(
    @JsonProperty
    val bindHost: String,

    @JsonProperty
    val port: Int
): ServiceRegistryFactory {

  override fun build(tenantStore: TenantStore): VerticleBundle {
    val verticle = LocalServiceRegistryVerticle(tenantStore)
    val options = DeploymentOptions()
    val config = JsonObject()
    config.put("bindHost", bindHost)
    config.put("port", port)
    options.setConfig(config)

    return VerticleBundle(verticle, options)
  }
}
package io.datawire.hub.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.config.JwtProviderFactory
import io.datawire.hub.tenant.model.TenantId
import io.datawire.hub.vertx.VerticleBundle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


data class SimpleServiceRegistryFactory(
    @JsonProperty
    val bindHost: String,

    @JsonProperty
    val port: Int
): ServiceRegistryFactory {

  override fun build(vertx: Vertx, tenant: TenantId, jwt: JwtProviderFactory): VerticleBundle {
    val mapper = ObjectMapper().registerKotlinModule()

    val verticle = LocalServiceRegistryVerticle(jwt.build(vertx), mapper, LocalServiceRegistry(), tenant)
    val options = DeploymentOptions()
    val config = JsonObject()
    config.put("bindHost", bindHost)
    config.put("port", port)
    options.setConfig(config)

    return VerticleBundle(verticle, options)
  }
}
package io.datawire.discovery.registry

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.discovery.auth.QueryJWTAuthHandler
import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth


@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleServiceRegistryFactory(
    @JsonProperty("bindHost")
    val bindHost: String,

    @JsonProperty("port")
    val port: Int
): ServiceRegistryFactory {

  override fun build(vertx: Vertx, jwt: JWTAuth, tenant: String): ServiceRegistryVerticle {
    val jsonMapper = ObjectMapper().registerKotlinModule()
    val jwtHandler = QueryJWTAuthHandler(jwt, "/health").setAudience(listOf(tenant))

    val verticle = ServiceRegistryVerticle(jwtHandler, jsonMapper, LocalServiceRegistry(), tenant)

//    val options = DeploymentOptions()
//    val config = JsonObject()
//    config.put("bindHost", bindHost)
//    config.put("port", port)
//    config.put("open", open)
//    options.setConfig(config)

    return verticle
  }

  override fun deploy(vertx: Vertx, jwt: JWTAuth, tenant: String, onCompletion: Handler<AsyncResult<String>>) {
    val verticle = build(vertx, jwt, tenant)
    vertx.deployVerticle(verticle, DeploymentOptions().setConfig(JsonObject().put("port", port)), onCompletion)
  }
}
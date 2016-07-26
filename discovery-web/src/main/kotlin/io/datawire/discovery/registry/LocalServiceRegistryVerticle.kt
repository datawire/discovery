package io.datawire.discovery.registry

import io.datawire.discovery.registry.ServiceId
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject


class LocalServiceRegistryVerticle : AbstractVerticle() {

  override fun start() {




    vertx.eventBus().consumer<JsonObject>("discovery.service") { msg ->

    }
  }

  fun broadcastExpiration(service: ServiceId) {
    val msg = JsonObject()

    vertx.eventBus().publish("discovery.service-registry:expire", msg)
  }

  fun broadcastAddition(service: ServiceId) {
    val msg = JsonObject()

    vertx.eventBus().publish("discovery.service-registry:add", msg)
  }
}
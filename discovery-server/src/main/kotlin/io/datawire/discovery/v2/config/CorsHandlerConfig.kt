package io.datawire.discovery.v2.config

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.CorsHandler


class CorsHandlerConfig(json: JsonObject) {

  val allowedOrigin = json.getString("allowedOrigin", "*")
  val path          = json.getString("path", "/*")

  fun createCorsHandler(): CorsHandler = CorsHandler.create(allowedOrigin)
}
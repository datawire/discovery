package io.datawire.discovery.config

import io.datawire.discovery.auth.DiscoveryAuthHandler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.handler.AuthHandler


class AuthHandlerConfig(private val json: JsonObject) {

  private val logger = LoggerFactory.getLogger(javaClass)

  val type        = json.getString("type", "none").toLowerCase()
  val protectPath = json.getString("protectPath", "/*")
  val skipPath    = json.getString("skipPath", "/health")

  fun createJwtAuthHandler(vertx: Vertx): AuthHandler {
    logger.info("Using JWT authentication (protects: {})", protectPath)
    val keystoreConfig = json.getJsonObject("keyStore")
    val jwt = JWTAuth.create(vertx, keystoreConfig)
    return DiscoveryAuthHandler(jwt, skipPath).setIgnoreExpiration(true)
  }
}
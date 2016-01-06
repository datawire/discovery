package io.datawire.hub.gateway.jwt

import com.fasterxml.jackson.annotation.JsonProperty
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth


data class JwtProviderFactory(
    @JsonProperty private val type: String,
    @JsonProperty private val path: String,
    @JsonProperty private val password: String
) {

  fun build(vertx: Vertx): JWTAuth {
    return JWTAuth.create(vertx, JsonObject(mapOf("keyStore" to buildKeyStoreConfig())))
  }

  private fun buildKeyStoreConfig(): JsonObject {
    return JsonObject(mapOf(
        "type" to type,
        "path" to path,
        "password" to password
    ))
  }
}
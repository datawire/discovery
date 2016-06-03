package io.datawire.discovery

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth


abstract class DiscoveryTest() {

  protected fun buildJsonWebTokenAuthProvider(vertx: Vertx, config: JsonObject): JWTAuth {
    return JWTAuth.create(vertx, config)
  }

  protected fun buildJsonWebTokenKeyStoreConfig(path: String,
                                                password: String = "notasecret",
                                                type: String = "jceks"): JsonObject {
    return JsonObject(mapOf(
        "keyStore" to mapOf(
            "path" to path,
            "password" to password,
            "type" to type
        )
    ))
  }

  protected fun resourcePath(resource: String): String {
    return this.javaClass.classLoader.getResource(resource).path
  }
}
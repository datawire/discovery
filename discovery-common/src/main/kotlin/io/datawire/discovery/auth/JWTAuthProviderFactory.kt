/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.discovery.auth


import com.fasterxml.jackson.annotation.JsonProperty
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth

/**
 * Constructs instances of [JWTAuth] for use within a Vert.x verticle.
 */

data class JWTAuthProviderFactory(
    @JsonProperty("keyStorePath") private val keyStorePath: String,
    @JsonProperty("keyStoreType") private val keyStoreType: String,
    @JsonProperty("keyStorePassword") private val keyStorePassword: String
) {

  private val log = LoggerFactory.getLogger(JWTAuthProviderFactory::class.java)

  /**
   * Constructs a new instance of [JWTAuth] for the given [Vertx] instance and with the current objects internal
   * parameters.
   *
   * @param vertx the Vert.x instance to associate the created [JWTAuth] object with.
   */
  fun build(vertx: Vertx): JWTAuth {
    log.debug("Building JWT AuthProvider (keystore: {0})", keyStorePath)
    return JWTAuth.create(vertx, buildKeyStoreConfig())
  }

  /**
   * Constructs a [JsonObject] that contains configuration for [JWTAuth].
   */
  private fun buildKeyStoreConfig(): JsonObject {
    val result = JsonObject().put("keyStore", JsonObject(mapOf(
        "path" to keyStorePath,
        "type" to keyStoreType,
        "password" to keyStorePassword
    )))

    return result
  }
}
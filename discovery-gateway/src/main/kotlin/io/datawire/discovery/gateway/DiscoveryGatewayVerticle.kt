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

package io.datawire.discovery.gateway


import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.handler.JWTAuthHandler

class DiscoveryGatewayVerticle(): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(DiscoveryGatewayVerticle::class.java)

  private val jsonContentType = "application/json; charset=utf-8"

  override fun start() {
    log.info("Starting Discovery Gateway")

    val router = Router.router(vertx)

    registerHealthCheck(router)
    registerConnectHandler(router)

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(config().getInteger("port"))

    // todo(plombardi): replace with {} syntax once this bug fix is released
    //
    // https://github.com/eclipse/vert.x/pull/1282
    log.info("Running server on ${config().getInteger("port")}")
  }

  private fun registerHealthCheck(router: Router) {
    log.info("Registering health check (path: /health)")
    router.get("/health").handler { rc ->
      rc.response().setStatusCode(200).end()
    }
  }

  private fun registerConnectHandler(router: Router) {
    log.info("Registering JWT handler")

    val jwtAuth = JWTAuth.create(vertx, config().getJsonObject("jsonWebToken"))
    val jwt = JWTAuthHandler.create(jwtAuth, "/health")

    router.post("/v1/connect").handler(jwt)

    log.info("Registering connector URL")
    router.post("/v1/connect").produces("application/json").handler { rc ->
      val user = rc.user()

      val audience = user.principal().getValue("aud")
      val tenant = if (audience is String) audience else (audience as JsonArray).getString(0)

      val response = rc.response()!!

      vertx.eventBus().send<String>("discovery-resolver", tenant) {
        if (it.succeeded()) {
          val address = it.result().body()

          val connectOptions = JsonObject(mapOf(
              "url" to "ws://$address/v1/messages"
          ))
          response.setStatusCode(200).putHeader("content-type", jsonContentType).end(connectOptions.encodePrettily())
        } else {
          response.setStatusCode(500).end()
        }
      }
    }
  }
}
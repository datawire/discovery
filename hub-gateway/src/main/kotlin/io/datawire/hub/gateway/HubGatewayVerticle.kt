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

package io.datawire.hub.gateway


import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.handler.JWTAuthHandler

class HubGatewayVerticle(private val jwt: JWTAuth): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(HubGatewayVerticle::class.java)

  private val jsonContentType = "application/json; charset=utf-8"

  override fun start() {
    log.info("Starting Hub Gateway")

    val router = Router.router(vertx)
    val server = vertx.createHttpServer()

    registerHealthCheck(router)
    registerConnectHandler(router)

    log.info("Accepting connections (port: {0})", config().getInteger("port"))
    server.requestHandler { router.accept(it) }.listen(8080)
  }

  private fun registerHealthCheck(router: Router) {
    log.info("Registering health check (path: /health)")
    router.get("/health").handler { rc ->
      rc.response().setStatusCode(200).end()
    }
  }

  private fun registerConnectHandler(router: Router) {
    log.info("Registering JWT handler")
    JWTAuthHandler.create(jwt, "/health")
    router.post("/v1/connect").handler(JWTAuthHandler.create(jwt))

    log.info("Registering connector URL")
    router.post("/v1/connect").produces(jsonContentType).handler { rc ->
      val user = rc.user()
      val tenant = user.principal().getString("aud")

      val response = rc.response()!!

      vertx.eventBus().send<String>("hub-resolver", tenant) {
        if (it.succeeded()) {
          val connectOptions = JsonObject(mapOf(
              "hubUrl" to it.result().body()
          ))
          response.setStatusCode(200).end(connectOptions.encodePrettily())
        } else {
          response.setStatusCode(500)
        }
      }
    }
  }
}